package dev.loqor.portal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import dev.amble.ait.AITMod;
import dev.amble.ait.client.boti.PortalParticleManager;
import dev.amble.ait.core.blockentities.DoorBlockEntity;
import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Renders the slice of the exterior world a TARDIS is standing in, as seen through the interior door.
 * <p>
 * The exterior surroundings live in a {@link ClientWorld shadow world} (see {@link PortalData}); this class turns
 * the blocks/entities/particles in front of the exterior door into geometry and draws them inside the doorway's
 * stencil region. Everything is drawn through a single {@linkplain #buildPortalView portal view matrix} so terrain,
 * block entities, entities, particles and the sky all share one camera - which is what was previously broken
 * (terrain used an identity view while entities were flushed with the interior camera's matrix, so they never
 * lined up).
 */
public class WorldGeometryRenderer {
    /** How many sections to (re)build per dispatch. Keeps each off-thread batch small so a hitch never stalls. */
    private static final int BUILD_BUDGET = 6;

    /** Big stack so deep block-model / biome-colour recursion can't overflow the build thread (the old cause of the
     * silently-swallowed StackOverflowError that left chunks unbuilt). */
    private static final long BUILD_THREAD_STACK = 32L * 1024 * 1024;

    private final Map<ChunkSectionPos, Map<RenderLayer, VertexBuffer>> sectionBuffers = new HashMap<>();
    private final Map<ChunkSectionPos, List<BlockEntity>> sectionBlockEntities = new HashMap<>();

    // Sections whose geometry changed since the last build. Drained a budget at a time so a single block/light/chunk
    // update only rebuilds the affected sections instead of the whole render volume.
    private final Set<ChunkSectionPos> dirtySections = ConcurrentHashMap.newKeySet();
    private boolean needsFullRebuild = true;

    // Per-section failure counter. A build that throws (usually a transient race with chunk streaming) is retried a
    // few times, then dropped so a permanently-bad section can't spin the builder or spam the log forever. Fresh
    // data for a section (markSectionDirty) clears its count so it gets a clean shot.
    private static final int MAX_BUILD_ATTEMPTS = 3;
    private final Map<ChunkSectionPos, Integer> buildAttempts = new ConcurrentHashMap<>();

    private CompletableFuture<Void> buildFuture = null;

    /** Set once {@link #close()} has run so a build still in flight can't upload (and leak) VBOs into a dead renderer. */
    private volatile boolean closed = false;

    /**
     * Reused per-section {@link BufferBuilder} sets (one entry per in-flight batch slot, each a full set of block
     * layers), confined to the single builder thread. Reusing them is the fix for the "leave the door open and memory
     * overloads" OOM: the old code allocated a fresh set of seven layer buffers - several MB of direct memory, since
     * SOLID's expected size alone is 2 MiB - for <em>every</em> section on <em>every</em> rebuild, and left them for
     * the GC's Cleaner to reclaim. A busy exterior (flowing water, redstone, mobs) rebuilds sections constantly, so
     * that direct-memory churn outran the Cleaner and exhausted the off-heap buffer pool. Reuse is safe because the
     * build → apply pipeline is serialised (see {@link #dispatchBuild}): {@link #buildFuture} only completes after
     * {@code applySection} has uploaded - and thereby released - every {@link BufferBuilder.BuiltBuffer}, so a slot's
     * builders are always idle before the next batch calls {@code begin()} on them again.
     */
    private final List<Map<RenderLayer, BufferBuilder>> builderPool = new ArrayList<>();

    /** Dedicated single-thread builder with a large stack; serialised so our builds never race each other. */
    private final ExecutorService buildExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(null, runnable, "BOTI-Geometry-Builder", BUILD_THREAD_STACK);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private final int renderDistance;

    private Direction doorFacing = Direction.NORTH;
    private Direction lastDoorFacing = null;

    // Frame-local view state, set at the top of every render() so the cull helpers and draw passes agree.
    private BlockPos centerPos = BlockPos.ORIGIN;
    private BlockPos lastBuiltCenter = null;
    private Matrix4f portalView = new Matrix4f();
    private Matrix4f portalProjection = new Matrix4f();
    private Frustum frustum = null;

    // Shared, reused immediate for the block-entity / entity / particle passes. Allocating a new BufferBuilder per
    // pass per door per frame churned off-heap direct memory (each grows well past its initial size, then is left for
    // the GC's Cleaner to reclaim) - the main cause of the "leave the door open and memory overloads" spikes. The
    // passes run sequentially on the render thread, each ending in draw(), so one reused buffer is safe and keeps its
    // grown capacity across frames instead of re-allocating.
    private final VertexConsumerProvider.Immediate immediate =
            VertexConsumerProvider.immediate(new BufferBuilder(256));

    /**
     * While the portal sky pass runs, the portal eye's position in the EXTERIOR world. Read by
     * {@code WorldRendererBotiMixin}: vanilla {@code renderSky} decides whether to draw its black below-horizon
     * "void plane" from {@code client.player}'s Y (the interior player sits below the overworld's y=63 darkness
     * line, which painted a black band along the doorway's horizon) and samples the sky colour's biome at the real
     * camera's position (interior coordinates that don't exist in the shadow world). The mixin substitutes this
     * position for both while it is non-null. Only touched on the render thread.
     */
    private static Vec3d portalSkyCameraPos = null;

    /** Exterior fog colour computed by the most recent {@link #render}; the doorway background is painted with it. */
    private Vec3d lastExteriorFogColor = null;

    /** Periodic counter for the particle diagnostic log line (see {@link #renderParticles}). Temporary. */
    // private static int PARTICLE_DIAG_TICK = 0;

    /** One-shot guard for the cloud diagnostic log line (see {@link #renderSky}). Temporary. */
    // private static final AtomicBoolean CLOUD_DIAG = new AtomicBoolean(false);

    public WorldGeometryRenderer(int renderDistance) {
        this.renderDistance = renderDistance;
    }

    /** Forces a full rebuild of the whole render volume (used for the first build, dimension reset, facing change). */
    public void markDirty() {
        this.needsFullRebuild = true;
    }

    /** Queues just one section for rebuilding - the cheap path for block/light/chunk updates. */
    public void markSectionDirty(ChunkSectionPos pos) {
        this.buildAttempts.remove(pos); // fresh data - give it a clean set of retries
        this.dirtySections.add(pos);
    }

    public void setDoorFacing(Direction facing) {
        if (this.lastDoorFacing != facing) {
            markDirty();
        }
        this.doorFacing = facing;
        this.lastDoorFacing = facing;
    }

    /** The portal centre (exterior/interior block the volume is baked around), as of the last {@link #render}. */
    public BlockPos centerPos() {
        return this.centerPos;
    }

    /** Block radius of the baked volume - used to scope the ambient particle display ticks to what is visible. */
    public int renderDistance() {
        return this.renderDistance;
    }

    /**
     * @param eyeRelToCenter the portal camera's eye position, expressed relative to {@code centerPos} (the exterior
     *                       block). This is the player's eye mapped through the doorway into the exterior world.
     * @param portalYaw      the portal camera's yaw (the player's yaw rotated by the door's turn through the portal)
     * @param portalPitch    the portal camera's pitch (matches the player's pitch)
     */
    public void render(UUID id, ClientWorld portalWorld, BlockPos centerPos, Vec3d eyeRelToCenter,
                       float portalYaw, float portalPitch, float tickDelta, boolean checkBehindPortal) {
        this.centerPos = centerPos;

        // Geometry is stored relative to centerPos, so if the exterior block moved (e.g. the TARDIS re-landed) the
        // whole volume has to be rebuilt around the new origin or it would draw offset.
        if (!centerPos.equals(this.lastBuiltCenter)) {
            this.lastBuiltCenter = centerPos.toImmutable();
            markDirty();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        GameRenderer gameRenderer = client.gameRenderer;

        // Match the doorway's view bobbing. In 1.20.1 GameRenderer.renderWorld folds view bobbing (plus the
        // hurt-tilt and nausea warp) into the *projection* matrix - `projection.mul(bobMatrix)` - and leaves the
        // model-view as pure camera rotation. The doorway's stencil mask and frame are drawn during the world
        // render with that bobbed projection bound, so they wobble with the head bob. Building a fresh
        // getBasicProjectionMatrix() here would have NO bob, so the interior would sit rigid while its frame
        // wobbles around it - the "interior swims in the doorway" wobble. Reusing the live projection gives the
        // interior the identical bob (and FOV), so frame and contents move as one.
        this.portalProjection = new Matrix4f(RenderSystem.getProjectionMatrix());

        Matrix4f portalRot = buildPortalRotation(portalYaw, portalPitch);
        this.portalView = buildPortalView(portalRot, eyeRelToCenter);

        // Vanilla's Frustum convention: planes from a rotation-only view, boxes offset by the camera position. Our
        // boxes are expressed relative to centerPos, so the camera position is eyeRelToCenter.
        this.frustum = new Frustum(portalRot, portalProjection);
        this.frustum.setPosition(eyeRelToCenter.x, eyeRelToCenter.y, eyeRelToCenter.z);

        pumpBuilds(portalWorld, checkBehindPortal);

        // A throwaway camera parked at centerPos, facing the way the portal looks. Used for particle billboarding,
        // entity name-tag orientation and the sky pass. Keeping its position at centerPos (not the real eye) makes
        // particles centerPos-relative, exactly like terrain and entities, so one model-view matrix covers them all.
        Camera portalCamera = new Camera();
        portalCamera.setPos(centerPos.getX(), centerPos.getY(), centerPos.getZ());
        portalCamera.setRotation(portalYaw, portalPitch);

        Matrix4f originalProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.setProjectionMatrix(portalProjection, VertexSorter.BY_DISTANCE);

        // The portal eye in exterior-world coordinates - the position the doorway's sky should be "seen from".
        Vec3d eyeWorldPos = new Vec3d(centerPos.getX() + eyeRelToCenter.x, centerPos.getY() + eyeRelToCenter.y,
                centerPos.getZ() + eyeRelToCenter.z);

        // Swap the frame's fog over to the exterior dimension before the sky pass. Setting the shader fog colour
        // directly is NOT enough: renderSky and renderClouds both call BackgroundRenderer.setFogBlack() mid-pass,
        // which re-applies the fog colour BackgroundRenderer computed at the start of the frame - the INTERIOR
        // dimension's (typically near-black for the TARDIS), fogging the dome's horizon to black no matter what
        // colour was bound here. BackgroundRenderer.render recomputes that cached colour from the shadow world
        // (biome fog colour, time of day, weather, sunrise tint), exactly like vanilla does at the start of a real
        // dimension's world render. Everything is restored in the finally below.
        float[] previousFogColor = RenderSystem.getShaderFogColor().clone();
        float previousFogStart = RenderSystem.getShaderFogStart();
        float previousFogEnd = RenderSystem.getShaderFogEnd();
        FogShape previousFogShape = RenderSystem.getShaderFogShape();
        try {
            this.lastExteriorFogColor = updateExteriorFog(portalWorld, eyeWorldPos, portalYaw, portalPitch, tickDelta, Math.min(client.options.getClampedViewDistance(), this.renderDistance()));
        } catch (Exception e) {
            AITMod.LOGGER.error("BOTI: failed to compute exterior fog", e);
        }

        // Sky sits at infinity, so it only takes the rotation (no eye translation) and never writes depth.
        renderSky(id, portalWorld, portalRot, portalCamera, eyeWorldPos, tickDelta);

        // The lightmap (light coord -> final RGB; it bakes in sky darkness, time of day, the dimension's ambient
        // light and gamma) is rebuilt once per frame by GameRenderer from client.world - the *interior* dimension -
        // before this door ever renders. Our terrain and entities carry light coordinates sampled from the shadow
        // world, but with the interior's ramp they get shaded as if they were inside the TARDIS (typically a flat,
        // day/night-less ramp), which reads as "lighting is broken / entities are invisible". Rebuild the ramp for
        // the exterior dimension for the duration of the portal passes, then restore it (below) so the rest of the
        // frame - main-world particles, weather, the hand - keeps the interior ramp.
        LightmapTextureManager lightmap = gameRenderer.getLightmapTextureManager();
        ClientWorld previousLightmapWorld = client.world;
        client.world = portalWorld;
        lightmap.tick();            // GameRenderer already consumed this frame's dirty flag - re-arm it
        lightmap.update(tickDelta); // recompute the ramp from the shadow world's dimension + (synced) time of day
        client.world = previousLightmapWorld;

        MatrixStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        try {
            modelViewStack.peek().getPositionMatrix().set(portalView);
            modelViewStack.peek().getNormalMatrix().set(new Matrix3f(portalView));
            RenderSystem.applyModelViewMatrix();

            // Each pass is isolated: a single throwing block-entity/entity renderer must not abort the others. The
            // old single try/catch (up in TardisDoorBOTI) meant one bad entity killed terrain's siblings *and* leaked
            // the model-view push below - which is exactly why entities and particles silently never appeared.

            if (!sectionBuffers.isEmpty()) {
                runPass("terrain", this::renderTerrain);
            }

            runPass("block entities", () -> renderBlockEntities(portalWorld, tickDelta, portalCamera));
            runPass("entities", () -> renderEntities(portalWorld, tickDelta, portalCamera));
            runPass("particles", () -> renderParticles(id, portalCamera, tickDelta));
        } finally {
            // Always unwind - otherwise the leaked push corrupts the main game's model-view matrix next frame.
            modelViewStack.pop();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(originalProjection, VertexSorter.BY_DISTANCE);

            // Restore both dispatcher configurations to the interior world and the real game camera.
            // renderBlockEntities/renderEntities reconfigure the shared BlockEntityRenderDispatcher and
            // EntityRenderDispatcher to point at portalWorld; any interior block entities rendered after
            // this door in the same frame's block-entity loop would then query light from the exterior
            // dimension and get the wrong (dark/transparent) lighting. Re-configure here so the
            // remaining interior block entities in the loop see the correct world and camera.
            Camera mainCamera = client.gameRenderer.getCamera();
            client.getBlockEntityRenderDispatcher().configure(previousLightmapWorld, mainCamera, client.crosshairTarget);
            client.getEntityRenderDispatcher().configure(previousLightmapWorld, mainCamera, client.targetedEntity);


            // Restore the interior dimension's lightmap for the rest of this frame (client.world is the interior
            // again here). Without this, main-world particles/weather drawn after AFTER_ENTITIES would be shaded
            // with the exterior ramp.
            lightmap.tick();
            lightmap.update(tickDelta);

            // Recompute BackgroundRenderer's cached fog colour for the interior (anything later in the frame that
            // calls setFogBlack must get the interior colour back; this also restores the GL clear colour), then put
            // back the exact fog uniforms the interior render had - including any overrides AIT's FoggyUtils applied
            // for alarm/power-off effects, which a recompute alone would lose.
            try {
                BackgroundRenderer.render(mainCamera, tickDelta, previousLightmapWorld,
                        client.options.getClampedViewDistance(), gameRenderer.getSkyDarkness(tickDelta));
            } catch (Exception e) {
                AITMod.LOGGER.error("BOTI: failed to restore interior fog", e);
            }
            RenderSystem.setShaderFogColor(previousFogColor[0], previousFogColor[1], previousFogColor[2], previousFogColor[3]);
            RenderSystem.setShaderFogStart(previousFogStart);
            RenderSystem.setShaderFogEnd(previousFogEnd);
            RenderSystem.setShaderFogShape(previousFogShape);
        }
    }

    /** Runs one draw pass, swallowing (and logging once) any failure so the remaining passes still render. */
    private void runPass(String name, Runnable pass) {
        try {
            pass.run();
        } catch (Throwable t) {
            AITMod.LOGGER.error("BOTI: '{}' pass failed", name, t);
        }
    }

    /** The world-relative rotation part of the portal camera (pitch then yaw, vanilla camera convention). */
    private static Matrix4f buildPortalRotation(float yaw, float pitch) {
        return new Matrix4f()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw + 180.0f));
    }

    /** The full portal view matrix: rotation, then translate the world so the eye sits at the origin. */
    private static Matrix4f buildPortalView(Matrix4f portalRotation, Vec3d eyeRelToCenter) {
        return new Matrix4f(portalRotation).translate(
                (float) -eyeRelToCenter.x, (float) -eyeRelToCenter.y, (float) -eyeRelToCenter.z);
    }

    // ===== Geometry build scheduling =====

    private void pumpBuilds(World world, boolean checkBehindPortal) {
        boolean idle = buildFuture == null || buildFuture.isDone();
        if (!idle)
            return;

        if (needsFullRebuild) {
            needsFullRebuild = false;
            enqueueVolume();
        }

        if (dirtySections.isEmpty())
            return;

        List<ChunkSectionPos> batch = drainBatch(BUILD_BUDGET);
        if (!batch.isEmpty())
            dispatchBuild(world, batch, checkBehindPortal);
    }

    /** Adds every section in the current render volume to the dirty set and drops buffers that fell out of it. */
    private void enqueueVolume() {
        buildAttempts.clear(); // a full rebuild gives every section a fresh set of retries
        Set<ChunkSectionPos> volume = computeVolumeSections();

        Iterator<Map.Entry<ChunkSectionPos, Map<RenderLayer, VertexBuffer>>> it = sectionBuffers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkSectionPos, Map<RenderLayer, VertexBuffer>> entry = it.next();
            if (!volume.contains(entry.getKey())) {
                for (VertexBuffer vbo : entry.getValue().values())
                    vbo.close();
                sectionBlockEntities.remove(entry.getKey());
                it.remove();
            }
        }

        dirtySections.addAll(volume);
    }

    /** Pulls up to {@code budget} in-volume sections from the dirty set, nearest first; drops the rest. */
    private List<ChunkSectionPos> drainBatch(int budget) {
        List<ChunkSectionPos> candidates = new ArrayList<>();

        Iterator<ChunkSectionPos> it = dirtySections.iterator();
        while (it.hasNext()) {
            ChunkSectionPos pos = it.next();
            if (!isSectionInVolume(pos)) {
                it.remove(); // never going to be visible - forget about it
                continue;
            }
            candidates.add(pos);
        }

        candidates.sort(Comparator.comparingDouble(this::sectionDistanceSq));

        List<ChunkSectionPos> batch = new ArrayList<>(Math.min(budget, candidates.size()));
        for (ChunkSectionPos pos : candidates) {
            if (batch.size() >= budget)
                break;
            batch.add(pos);
            dirtySections.remove(pos);
        }

        return batch;
    }

    private void dispatchBuild(World world, List<ChunkSectionPos> batch, boolean checkBehindPortal) {
        if (closed)
            return;

        // Lighting flood-fill pre-pass - MUST run on this (the render) thread, never on the async builder below.
        // The shadow world's light engine is backed by thread-unsafe fastutil sets and is also mutated on this
        // thread by PortalData as chunk/light packets arrive. Touching it from the builder thread corrupted those
        // sets (builder-thread NPEs that blanked the terrain, then a fatal LongOpenHashSet.rehash AIOOBE on the
        // render thread). Doing it here keeps every light mutation single-threaded; the off-thread mesh then only
        // reads light. doLightUpdates() also commits the light PortalData staged via enqueueSectionData/setStatus.
        LightingProvider lightingProvider = world.getLightingProvider();
        boolean queuedAny = false;
        BlockPos.Mutable lightPos = new BlockPos.Mutable();
        for (ChunkSectionPos sectionPos : batch) {
            if (world.getChunk(sectionPos.getX(), sectionPos.getZ(), ChunkStatus.FULL, false) == null)
                continue; // not streamed yet - the async loop re-queues it; nothing to light

            int startX = sectionPos.getMinX(), startY = sectionPos.getMinY(), startZ = sectionPos.getMinZ();
            for (int x = startX; x <= startX + 15; x++)
                for (int y = startY; y <= startY + 15; y++)
                    for (int z = startZ; z <= startZ + 15; z++)
                        lightingProvider.checkBlock(lightPos.set(x, y, z));
            queuedAny = true;
        }
        if (queuedAny)
            lightingProvider.doLightUpdates();

        // Serialised pipeline: this future only completes once the results have been uploaded (or discarded) on the
        // render thread. Because pumpBuilds waits for it before dispatching the next batch, the reusable builder pool
        // is never touched by the builder thread while the render thread is still reading (uploading/releasing) the
        // previous batch's built buffers - the invariant that makes reusing the builders safe (see builderPool).
        CompletableFuture<Void> applied = new CompletableFuture<>();
        buildFuture = applied;

        buildExecutor.execute(() -> {
            BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
            Random random = Random.create();

            List<SectionResult> results = new ArrayList<>(batch.size());
            for (int slot = 0; slot < batch.size(); slot++) {
                ChunkSectionPos sectionPos = batch.get(slot);

                // Don't build a section whose column hasn't streamed into the shadow world yet: reading it would
                // return all-air, and applySection would then *replace* the section's last good geometry with
                // nothing - the chunk flashes in, then vanishes ("yeeted"). Re-queue and try again once it loads.
                // (An explicit unload drops geometry via dropSection, so a genuinely gone chunk still disappears.)
                if (world.getChunk(sectionPos.getX(), sectionPos.getZ(), ChunkStatus.FULL, false) == null) {
                    dirtySections.add(sectionPos);
                    continue;
                }

                try {
                    results.add(buildSection(world, sectionPos, builderSet(slot), blockRenderManager, random, checkBehindPortal));
                } catch (Throwable t) {
                    // One bad section (e.g. boundary data not streamed yet) must not sink the whole batch, and a
                    // StackOverflowError is a Throwable, not an Exception - catch it so it can't be swallowed by the
                    // CompletableFuture and quietly leave the doorway blank. The pooled builders for this slot may be
                    // left mid-build, so throw them away and let the slot re-allocate a clean set next batch.
                    resetBuilderSet(slot);

                    int attempts = buildAttempts.merge(sectionPos, 1, Integer::sum);
                    if (attempts == 1)
                        AITMod.LOGGER.error("BOTI: failed to build section {} (attempt {})", sectionPos, attempts, t);

                    if (attempts < MAX_BUILD_ATTEMPTS)
                        dirtySections.add(sectionPos); // transient - retry on a later frame
                }
            }

            MinecraftClient.getInstance().execute(() -> {
                try {
                    if (closed) {
                        // Renderer was torn down while this batch was building - drop the built buffers rather than
                        // uploading them into VBOs that nothing would ever close (a GL-buffer leak per teardown).
                        for (SectionResult result : results)
                            for (BufferBuilder.BuiltBuffer built : result.buffers().values())
                                built.release();
                        return;
                    }
                    for (SectionResult result : results)
                        applySection(result);
                } finally {
                    applied.complete(null); // frees the pipeline (and the builder pool) for the next batch
                }
            });
        });
    }

    /**
     * Returns the reusable {@link BufferBuilder} set for the given batch slot, allocating it (and any lower slots)
     * on first use. Called only on the builder thread; safe to reuse across batches because of the serialised
     * pipeline (see {@link #builderPool}).
     */
    private Map<RenderLayer, BufferBuilder> builderSet(int slot) {
        while (builderPool.size() <= slot)
            builderPool.add(newBuilderSet());
        return builderPool.get(slot);
    }

    private static Map<RenderLayer, BufferBuilder> newBuilderSet() {
        Map<RenderLayer, BufferBuilder> set = new HashMap<>();
        for (RenderLayer layer : RenderLayer.getBlockLayers())
            set.put(layer, new BufferBuilder(layer.getExpectedBufferSize()));
        return set;
    }

    /** Drops a slot's builders (they may be left mid-{@code begin()} after a failed build) so it re-allocates clean. */
    private void resetBuilderSet(int slot) {
        if (slot < builderPool.size())
            builderPool.set(slot, newBuilderSet());
    }

    // ===== Volume / culling helpers (all in centerPos-relative space) =====

    private Set<ChunkSectionPos> computeVolumeSections() {
        Set<ChunkSectionPos> sections = new HashSet<>();

        int minSectionX = (centerPos.getX() - renderDistance) >> 4;
        int minSectionY = (centerPos.getY() - renderDistance) >> 4;
        int minSectionZ = (centerPos.getZ() - renderDistance) >> 4;
        int maxSectionX = (centerPos.getX() + renderDistance) >> 4;
        int maxSectionY = (centerPos.getY() + renderDistance) >> 4;
        int maxSectionZ = (centerPos.getZ() + renderDistance) >> 4;

        for (int x = minSectionX; x <= maxSectionX; x++)
            for (int y = minSectionY; y <= maxSectionY; y++)
                for (int z = minSectionZ; z <= maxSectionZ; z++) {
                    ChunkSectionPos pos = ChunkSectionPos.from(x, y, z);
                    if (isSectionInVolume(pos))
                        sections.add(pos);
                }

        return sections;
    }

    /** A section is in the volume if it is within render distance and not entirely behind the door plane. */
    private boolean isSectionInVolume(ChunkSectionPos pos) {
        double dx = pos.getMinX() + 8 - centerPos.getX();
        double dy = pos.getMinY() + 8 - centerPos.getY();
        double dz = pos.getMinZ() + 8 - centerPos.getZ();

        double reach = renderDistance + 16.0;
        if (dx * dx + dy * dy + dz * dz > reach * reach)
            return false;

        Vec3i normal = doorFacing.getVector();
        double inFront = dx * normal.getX() + dy * normal.getY() + dz * normal.getZ();
        return inFront > -16.0; // keep sections straddling the door plane
    }

    private double sectionDistanceSq(ChunkSectionPos pos) {
        double dx = pos.getMinX() + 8 - centerPos.getX();
        double dy = pos.getMinY() + 8 - centerPos.getY();
        double dz = pos.getMinZ() + 8 - centerPos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** Frustum test against the portal camera so only sections actually visible through the doorway are drawn. */
    private boolean isSectionVisible(ChunkSectionPos pos) {
        if (frustum == null)
            return true;

        double minX = pos.getMinX() - centerPos.getX();
        double minY = pos.getMinY() - centerPos.getY();
        double minZ = pos.getMinZ() - centerPos.getZ();

        return frustum.isVisible(new Box(minX, minY, minZ, minX + 16, minY + 16, minZ + 16));
    }

    // ===== Draw passes =====

    private void renderTerrain() {
        // Frustum-cull once per frame, then reuse the survivors for every render layer (instead of re-testing each
        // section seven times over).
        List<Map<RenderLayer, VertexBuffer>> visible = new ArrayList<>();
        for (Map.Entry<ChunkSectionPos, Map<RenderLayer, VertexBuffer>> entry : sectionBuffers.entrySet()) {
            if (isSectionVisible(entry.getKey()))
                visible.add(entry.getValue());
        }

        if (visible.isEmpty())
            return;

        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            if (layer == RenderLayer.getTranslucent())
                continue;

            drawLayer(layer, visible);
        }

        drawLayer(RenderLayer.getTranslucent(), visible);

        RenderSystem.disableBlend();
    }

    private void drawLayer(RenderLayer layer, List<Map<RenderLayer, VertexBuffer>> visible) {
        layer.startDrawing();

        for (Map<RenderLayer, VertexBuffer> layerBuffers : visible) {
            VertexBuffer vbo = layerBuffers.get(layer);
            if (vbo != null) {
                vbo.bind();
                vbo.draw(portalView, portalProjection, RenderSystem.getShader());
            }
        }

        VertexBuffer.unbind();
        layer.endDrawing();
    }

    /**
     * Draws the exterior dimension's sky into the doorway so it shows the sky for wherever the TARDIS actually is.
     * <p>
     * The doorway is already cleared to the exterior dimension's real sky colour (see {@code TardisDoorBOTI}); on top
     * of that we ask the shadow world's own {@link WorldRenderer} (it mirrors the right dimension) to draw the
     * celestial bodies. Vanilla {@code renderSky} reads the sun/moon angle, sky colour and star brightness from the
     * renderer's {@code world} (already the shadow world) but decides the sky <em>type</em> - overworld sun/moon/stars
     * vs. End starfield vs. nether/no sky - from {@code client.world}, the <em>interior</em> dimension. The TARDIS
     * interior has no vanilla sky type, so nothing celestial ever drew. We momentarily point {@code client.world} at
     * the shadow world for the duration of the sky pass so the type matches the target dimension too; the swap and the
     * GL state are restored in a {@code finally} (which does not swallow exceptions - they propagate as normal).
     */
    private void renderSky(UUID id, ClientWorld portalWorld, Matrix4f portalRotation, Camera portalCamera,
                           Vec3d eyeWorldPos, float tickDelta) {
        PortalData data = PortalDataManager.get(id);
        if (data == null || data.renderer() == null)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld previousWorld = client.world;

        // Vanilla renderSky draws the sun, moon and sunrise/sunset glow with BufferRenderer.drawWithGlobalProgram,
        // which transforms them by the GLOBAL model-view matrix - on top of the rotation baked into the skyStack we
        // pass below. Vanilla runs its whole sky pass with that global matrix at IDENTITY (the camera rotation lives
        // only in the matrices argument), so the celestial bodies are rotated exactly once.
        //
        // The global model-view still holds the interior camera's full view (rotation + eye translation) from the
        // main world render, which would fling the celestial vertices hundreds of blocks off-screen, so we must
        // override it - but to IDENTITY, not portalRotation. skyStack already carries portalRotation, so putting
        // portalRotation here too rotates the sun/moon TWICE (portalRotation squared) and throws them off-screen,
        // leaving only the dome (drawn via VertexBuffer.draw with an explicit matrix, which ignores the global
        // model-view) visible. That was the "dome shows but there's no sun" bug. Identity makes the sun/moon agree
        // with the dome. The dome, stars and clouds all use explicit matrices, so this value doesn't affect them.
        MatrixStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        modelViewStack.peek().getPositionMatrix().identity();
        modelViewStack.peek().getNormalMatrix().identity();
        RenderSystem.applyModelViewMatrix();

        try {
            client.world = portalWorld;
            portalSkyCameraPos = eyeWorldPos; // WorldRendererBotiMixin reads this inside renderSky

            MatrixStack skyStack = new MatrixStack();
            skyStack.multiplyPositionMatrix(portalRotation);

            RenderSystem.depthMask(false);

            // Bind the position program BEFORE renderSky. Vanilla draws the upper sky dome (lightSkyBuffer, a
            // POSITION-format VBO) with whatever shader RenderSystem.getShader() happens to hold - it only sets an
            // explicit shader later, for the horizon glow / sun. In the portal path the leftover shader is the
            // stencil mask's position_color program, whose vertex layout doesn't match the dome's, so the dome's
            // colour attribute is unbound and it renders black - leaving only the (separately-shaded) horizon band
            // visible. Binding the matching position program here makes the dome paint the real sky colour again.
            RenderSystem.setShader(GameRenderer::getPositionProgram);

            // Vanilla sky fog (FOG_SKY: start 0, end view-distance, cylinder). This fog IS the sky gradient: the
            // dome is drawn in the zenith sky colour and linear fog fades its lower fragments into the horizon fog
            // colour (computed for the shadow world by updateExteriorFog, re-applied inside renderSky via
            // setFogBlack). The old "push the fog past all sky geometry" workaround killed the fade, which is why
            // the doorway sky was one flat colour with no horizon gradient.
            float viewDistanceBlocks = Math.max(client.gameRenderer.getViewDistance(), 32.0f);
            data.renderer().renderSky(skyStack, portalProjection, tickDelta, portalCamera, false, () -> {
                RenderSystem.setShaderFogStart(0.0f);
                RenderSystem.setShaderFogEnd(viewDistanceBlocks);
                RenderSystem.setShaderFogShape(FogShape.CYLINDER);
            });

            // Match vanilla's terrain fog for everything drawn after the dome (the clouds below, then the terrain/
            // entity/particle passes back in render()): clear until just short of the view distance, then a quick
            // fade. The portal volume is far smaller than the fog start so the doorway's blocks stay unfogged, but
            // the fog COLOUR stays the exterior's - renderClouds re-applies it via setFogBlack for the cloud sheet.
            RenderSystem.setShaderFogStart(viewDistanceBlocks - MathHelper.clamp(viewDistanceBlocks / 10.0f, 4.0f, 64.0f));
            RenderSystem.setShaderFogEnd(viewDistanceBlocks);
            RenderSystem.setShaderFogShape(FogShape.CYLINDER);

            // Clouds are a separate pass in vanilla (WorldRenderer.renderClouds), so the doorway never drew them.
            // Draw them now, inside the same client.world swap. renderClouds builds its sheet relative to the camera
            // coords passed here and draws it through cloudStack (rotation ONLY - no eye translation, unlike the
            // portalView that terrain/entities/particles use). So the camera coords MUST be the portal eye's world
            // position, not centerPos: pass centerPos and the whole cloud layer is shifted by eyeRelToCenter, which is
            // large whenever the player stands away from the interior door - the clouds slide right out of the doorway
            // ("no clouds"). Passing eyeWorldPos puts the eye at the rotation origin, exactly matching the terrain
            // (which is R * (blockPos - eyeWorldPos)), so the clouds line up above the visible ground.
            CloudRenderMode cloudMode = client.options.getCloudRenderModeValue();
            //if (CLOUD_DIAG.compareAndSet(false, true))
            //    AITMod.LOGGER.info("BOTI cloud diag: mode={} cloudsHeight={} eyeY={} centerY={}",
            //            cloudMode, portalWorld.getDimensionEffects().getCloudsHeight(), eyeWorldPos.y, centerPos.getY());
            if (cloudMode != CloudRenderMode.OFF) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                MatrixStack cloudStack = new MatrixStack();
                cloudStack.multiplyPositionMatrix(portalRotation);
                data.renderer().renderClouds(cloudStack, portalProjection, tickDelta,
                        eyeWorldPos.x, eyeWorldPos.y, eyeWorldPos.z);
            }
        } finally {
            portalSkyCameraPos = null;
            client.world = previousWorld;
            modelViewStack.pop();
            RenderSystem.applyModelViewMatrix();
            // Vanilla renderSky leaves these in various states; reset to sane terrain defaults.
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setProjectionMatrix(portalProjection, VertexSorter.BY_DISTANCE);
        }
    }

    /**
     * Recomputes the frame fog for the exterior dimension, exactly like vanilla does at the start of a world render:
     * {@code BackgroundRenderer.render} derives the fog colour from the shadow world's biomes / time of day /
     * weather as seen by the portal camera, and {@code setFogBlack()} applies it as the live shader fog colour
     * (and primes the cached value that renderSky/renderClouds re-apply mid-pass).
     *
     * @return the computed fog colour - the colour the sky fades into at the horizon, which is also what the
     *         doorway background should be painted with so the hand-off is seamless
     */
    public static Vec3d updateExteriorFog(ClientWorld portalWorld, Vec3d eyePos, float yaw, float pitch,
                                          float tickDelta, int renderDistance) {
        MinecraftClient client = MinecraftClient.getInstance();

        Camera fogCamera = new Camera();
        fogCamera.setPos(eyePos.x, eyePos.y, eyePos.z);
        fogCamera.setRotation(yaw, pitch);

        BackgroundRenderer.render(fogCamera, tickDelta, portalWorld, renderDistance,
                client.gameRenderer.getSkyDarkness(tickDelta));
        BackgroundRenderer.setFogBlack();

        float[] fog = RenderSystem.getShaderFogColor();
        return new Vec3d(fog[0], fog[1], fog[2]);
    }

    /** The exterior fog colour from this renderer's most recent frame, or {@code null} before the first one. */
    public Vec3d exteriorFogColor() {
        return this.lastExteriorFogColor;
    }

    /** The portal eye's exterior-world position while the portal sky pass is running, else {@code null}. */
    public static Vec3d getPortalSkyCameraPos() {
        return portalSkyCameraPos;
    }

    private void renderBlockEntities(ClientWorld portalWorld, float tickDelta, Camera portalCamera) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();

        dispatcher.configure(portalWorld, portalCamera, client.crosshairTarget);

        MatrixStack matrices = new MatrixStack();
        List<BlockEntity> snapshot = new ArrayList<>();
        for (List<BlockEntity> sectionEntities : sectionBlockEntities.values())
            snapshot.addAll(sectionEntities);

        for (BlockEntity blockEntity : snapshot) {
            BlockPos blockPos = blockEntity.getPos();

            if (!isWithinRenderBounds(blockPos))
                continue;
            if ((blockEntity instanceof DoorBlockEntity || blockEntity instanceof ExteriorBlockEntity) && blockEntity.getPos() == portalCamera.getBlockPos())
                continue;

            matrices.push();
            matrices.translate(
                    blockPos.getX() - centerPos.getX(),
                    blockPos.getY() - centerPos.getY(),
                    blockPos.getZ() - centerPos.getZ());

            try {
                dispatcher.render(blockEntity, tickDelta, matrices, immediate);
            } catch (Throwable t) {
                AITMod.LOGGER.error("BOTI: failed to render block entity {}", blockEntity, t);
            } finally {
                matrices.pop();
            }
        }

        immediate.draw();
    }

    private void renderEntities(ClientWorld portalWorld, float tickDelta, Camera portalCamera) {
        MinecraftClient client = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        dispatcher.configure(portalWorld, portalCamera, client.targetedEntity);

        // Bias the whole entity pass toward the camera in depth. This is what keeps the entities' fake contact shadow
        // (the SHADOW_LAYER) from z-fighting the block top it lies on. Vanilla relies on that layer's
        // VIEW_OFFSET_Z_LAYERING, which pulls the decal toward the camera by scaling the model-view about its origin -
        // but that only works under camera-relative rendering, where the origin IS the eye. We render centerPos-
        // relative, so that scale biases toward the door centre and, once the player steps a few blocks from the door,
        // no longer clears the surface (the "shadow glitches into the block" report). A depth-only polygon offset is
        // camera-relative regardless of the model-view origin, so it fixes the shadow at any distance; entities aren't
        // coplanar with terrain so the tiny bias is invisible on them. It must stay enabled for the whole loop, not
        // just the final draw(): the shared Immediate flushes the SHADOW_LAYER mid-loop whenever the next entity asks
        // for a different layer, so those draws need the offset live too. Matches vanilla's POLYGON_OFFSET_LAYERING.
        RenderSystem.polygonOffset(-1.0f, -10.0f);
        RenderSystem.enablePolygonOffset();
        try {
            MatrixStack matrices = new MatrixStack();

            for (Entity entity : portalWorld.getEntities()) {
                if (entity == null || !isWithinRenderBounds(entity.getBlockPos()))
                    continue;

                // Interpolated render position, expressed relative to the portal centre (same convention as terrain
                // and block entities). The dispatcher translates the matrix stack by these coordinates internally; the
                // shared portal view matrix (set as the global model-view) then maps them onto the doorway.
                double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - centerPos.getX();
                double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - centerPos.getY();
                double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - centerPos.getZ();
                float yaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());

                try {
                    int light = dispatcher.getLight(entity, tickDelta);
                    dispatcher.render(entity, x, y, z, yaw, tickDelta, matrices, immediate, light);
                } catch (Throwable t) {
                    // A half-synced mob (missing tracked data, etc.) must not blank the whole entity pass.
                    AITMod.LOGGER.error("BOTI: failed to render entity {}", entity, t);
                }
            }

            immediate.draw();
        } finally {
            RenderSystem.polygonOffset(0.0f, 0.0f);
            RenderSystem.disablePolygonOffset();
        }
    }

    private void renderParticles(UUID id, Camera portalCamera, float tickDelta) {
        PortalParticleManager manager = PortalDataManager.particles(id);

        // DIAGNOSTIC (periodic, ~every 100 renders): the previous one-shot version only caught the expected first
        // frame race (the manager is created lazily the tick after the first render sets centerPos), so it couldn't
        // report steady state. This reports whether the manager exists and how many particles it holds over time,
        // distinguishing a spawn failure (never created / count 0) from a render failure (count > 0, nothing shows).
        /*if (PARTICLE_DIAG_TICK++ % 100 == 0)
            AITMod.LOGGER.info("BOTI particle diag: manager={} count=[{}] for {}",
                    manager != null, manager == null ? "-" : manager.getDebugString(), id);
*/
        if (manager == null)
            return;

        MinecraftClient client = MinecraftClient.getInstance();

        // Particles bill-board relative to the camera; with the camera parked at centerPos they come out
        // centerPos-relative, matching the terrain and entities under the shared portal view matrix.
        manager.renderParticles(new MatrixStack(), immediate, client.gameRenderer.getLightmapTextureManager(),
                portalCamera, tickDelta);
        immediate.draw();
    }

    private boolean isWithinRenderBounds(BlockPos blockPos) {
        return blockPos.getX() >= centerPos.getX() - renderDistance
                && blockPos.getX() <= centerPos.getX() + renderDistance
                && blockPos.getY() >= centerPos.getY() - renderDistance
                && blockPos.getY() <= centerPos.getY() + renderDistance
                && blockPos.getZ() >= centerPos.getZ() - renderDistance
                && blockPos.getZ() <= centerPos.getZ() + renderDistance;
    }

    // ===== Section building (off-thread; no GL here) =====

    private SectionResult buildSection(World world, ChunkSectionPos sectionPos, Map<RenderLayer, BufferBuilder> builders,
                                       BlockRenderManager blockRenderManager, Random random, boolean checkBehindPortal) {

        int startX = sectionPos.getMinX();
        int startY = sectionPos.getMinY();
        int startZ = sectionPos.getMinZ();
        int endX = startX + 15;
        int endY = startY + 15;
        int endZ = startZ + 15;

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // NOTE: the lighting flood-fill pre-pass used to run here, on this builder thread. That raced PortalData's
        // render-thread light mutations and corrupted the shadow world's (thread-unsafe) light engine - a fatal
        // crash. The pre-pass now runs on the render thread in dispatchBuild() before this build is queued, so this
        // off-thread mesh only READS light (renderBlock below), which is safe.

        // builders come from the reusable pool (see builderPool); (re)begin each for this section's build.
        Set<RenderLayer> usedLayers = new HashSet<>();

        for (RenderLayer layer : RenderLayer.getBlockLayers())
            builders.get(layer).begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

        MatrixStack matrices = new MatrixStack();
        List<BlockEntity> foundBlockEntities = new ArrayList<>();

        // renderFluid() ignores the matrix stack and bakes its vertices at section-local coords (x&15, y&15, z&15).
        // Everything else here is baked relative to centerPos, so wrap the fluid buffers to add the constant
        // per-section shift (sectionMin - centerPos) that maps section-local space into centerPos-relative space.
        double fluidOffsetX = startX - centerPos.getX();
        double fluidOffsetY = startY - centerPos.getY();
        double fluidOffsetZ = startZ - centerPos.getZ();
        Map<RenderLayer, OffsetVertexConsumer> fluidConsumers = new HashMap<>();

        boolean hasBlocks = false;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    mutablePos.set(x, y, z);

                    BlockState state = world.getBlockState(mutablePos);

                    if (state.isAir())
                        continue;

                    double relX = x - centerPos.getX();
                    double relY = y - centerPos.getY();
                    double relZ = z - centerPos.getZ();

                    if (checkBehindPortal && isBehindPortal(relX, relY, relZ))
                        continue;

                    if (isFullySurrounded(world, mutablePos))
                        continue;

                    hasBlocks = true;

                    if (state.hasBlockEntity()) {
                        BlockEntity blockEntity = world.getBlockEntity(mutablePos);
                        if (blockEntity != null)
                            foundBlockEntities.add(blockEntity);
                    }

                    FluidState fluidState = state.getFluidState();
                    if (!fluidState.isEmpty()) {
                        RenderLayer fluidLayer = RenderLayers.getFluidLayer(fluidState);
                        usedLayers.add(fluidLayer);

                        OffsetVertexConsumer fluidConsumer = fluidConsumers.computeIfAbsent(fluidLayer,
                                layer -> new OffsetVertexConsumer(builders.get(layer),
                                        fluidOffsetX, fluidOffsetY, fluidOffsetZ));

                        blockRenderManager.renderFluid(mutablePos, world, fluidConsumer, state, fluidState);
                    }

                    if (state.getRenderType() != BlockRenderType.INVISIBLE) {
                        RenderLayer blockLayer = RenderLayers.getBlockLayer(state);
                        BufferBuilder builder = builders.get(blockLayer);
                        usedLayers.add(blockLayer);

                        matrices.push();
                        matrices.translate(relX, relY, relZ);

                        // Because we ran doLightUpdates() above, this will now receive true flood-fill data
                        blockRenderManager.renderBlock(state, mutablePos, world, matrices, builder, true, random);

                        matrices.pop();
                    }
                }
            }
        }

        Map<RenderLayer, BufferBuilder.BuiltBuffer> builtBuffers = new HashMap<>();

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            BufferBuilder.BuiltBuffer built = builders.get(layer).end();

            if (hasBlocks && usedLayers.contains(layer))
                builtBuffers.put(layer, built);
            else
                built.release();
        }

        return new SectionResult(sectionPos, builtBuffers, foundBlockEntities);
    }

    /** True for blocks on the far side of the door plane - they can never be seen through the doorway. */
    private boolean isBehindPortal(double relX, double relY, double relZ) {
        Vec3i normal = doorFacing.getVector();
        return relX * normal.getX() + relY * normal.getY() + relZ * normal.getZ() < 0.0;
    }

    /** Uploads one section's freshly-built buffers, replacing (or removing) whatever was there before. */
    private void applySection(SectionResult result) {
        ChunkSectionPos pos = result.pos();
        buildAttempts.remove(pos); // built cleanly

        Map<RenderLayer, VertexBuffer> old = sectionBuffers.remove(pos);
        if (old != null) {
            for (VertexBuffer vbo : old.values())
                vbo.close();
        }

        if (result.buffers().isEmpty()) {
            sectionBlockEntities.remove(pos);
            return;
        }

        Map<RenderLayer, VertexBuffer> layerBuffers = new HashMap<>();
        for (Map.Entry<RenderLayer, BufferBuilder.BuiltBuffer> entry : result.buffers().entrySet()) {
            VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vbo.bind();
            vbo.upload(entry.getValue());
            VertexBuffer.unbind();

            layerBuffers.put(entry.getKey(), vbo);
        }

        sectionBuffers.put(pos, layerBuffers);

        if (result.blockEntities().isEmpty())
            sectionBlockEntities.remove(pos);
        else
            sectionBlockEntities.put(pos, result.blockEntities());
    }

    /**
     * Immediately drops a section's geometry. Called when the shadow world unloads a chunk: the blocks are genuinely
     * gone, so we remove the buffers directly instead of scheduling a rebuild (which would now be skipped, because
     * the streaming guard in {@link #dispatchBuild} won't build an unloaded column). Must run on the render thread.
     */
    public void dropSection(ChunkSectionPos pos) {
        dirtySections.remove(pos);
        buildAttempts.remove(pos);

        Map<RenderLayer, VertexBuffer> old = sectionBuffers.remove(pos);
        if (old != null) {
            for (VertexBuffer vbo : old.values())
                vbo.close();
        }

        sectionBlockEntities.remove(pos);
    }

    private void clearBuffers() {
        for (Map<RenderLayer, VertexBuffer> layerMap : sectionBuffers.values()) {
            for (VertexBuffer vbo : layerMap.values())
                vbo.close();
        }

        sectionBuffers.clear();
        sectionBlockEntities.clear();
    }

    private boolean isFullySurrounded(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.offset(dir);
            BlockState adjacentState = world.getBlockState(adjacent);
            if (!adjacentState.isOpaqueFullCube(world, adjacent))
                return false;
        }
        return true;
    }

    public void close() {
        closed = true;
        clearBuffers();
        buildExecutor.shutdownNow();
    }

    public int getSectionCount() {
        return sectionBuffers.size();
    }

    public int getBlockEntityCount() {
        int count = 0;
        for (List<BlockEntity> sectionEntities : sectionBlockEntities.values())
            count += sectionEntities.size();
        return count;
    }

    private record SectionResult(ChunkSectionPos pos, Map<RenderLayer, BufferBuilder.BuiltBuffer> buffers,
                                 List<BlockEntity> blockEntities) {
    }

    private class HybridRenderView implements BlockRenderView {
        private final World fakeWorld; // Provides the blocks
        private final World realWorld; // Provides the light

        public HybridRenderView(World fakeWorld) {
            this.fakeWorld = fakeWorld;
            this.realWorld = MinecraftClient.getInstance().world;
        }

        // ==========================================
        // LIGHTING: Fetch from the real ClientWorld
        // ==========================================

        @Override
        public int getLightLevel(LightType type, BlockPos pos) {
            if (realWorld == null) return 15;
            return realWorld.getLightLevel(type, pos);
        }

        @Override
        public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
            if (realWorld == null) return 15728880;
            return realWorld.getBaseLightLevel(pos, ambientDarkness);
        }

        @Override
        public float getBrightness(Direction direction, boolean shaded) {
            if (realWorld == null) return 1.0f;
            return realWorld.getBrightness(direction, shaded);
        }

        @Override
        public LightingProvider getLightingProvider() {
            return realWorld != null ? realWorld.getLightingProvider() : fakeWorld.getLightingProvider();
        }

        // ==========================================
        // BLOCKS: Fetch from your copied fake world
        // ==========================================

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return fakeWorld.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return fakeWorld.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return fakeWorld.getFluidState(pos);
        }

        @Override
        public int getColor(BlockPos pos, ColorResolver colorResolver) {
            return fakeWorld.getColor(pos, colorResolver);
        }

        @Override
        public int getHeight() { return fakeWorld.getHeight(); }

        @Override
        public int getBottomY() { return fakeWorld.getBottomY(); }
    }
}
