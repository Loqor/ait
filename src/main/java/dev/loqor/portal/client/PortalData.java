package dev.loqor.portal.client;

import com.mojang.datafixers.util.Pair;

import dev.amble.ait.AITMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public record PortalData(UUID id, WorldRenderer renderer, ClientWorld world, WorldGeometryRenderer geometry) {

    /**
     * How many blocks of the mirrored world to bake around the portal centre, derived from the single configurable
     * chunk radius {@code AITMod.CONFIG.botiRenderDistance} (so one knob drives both the server's chunk streaming and
     * the client's bake). The bake volume is a cube of this radius in every axis, so the vertical extent collected
     * scales with the horizontal chunk radius rather than being a separate fixed number.
     */
    private static int renderDistanceBlocks() {
        return AITMod.CONFIG.botiRenderDistance * 16;
    }

    public void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet) {
        packet.visitUpdates(this::handleBlockUpdate);
    }

    public void onBlockUpdate(BlockUpdateS2CPacket packet) {
        handleBlockUpdate(packet.getPos(), packet.getState());
    }

    private void handleBlockUpdate(BlockPos pos, BlockState state) {
        this.world.handleBlockUpdate(pos, state, Block.FORCE_STATE | Block.NOTIFY_LISTENERS | Block.NOTIFY_NEIGHBORS);
        markSectionsDirty(pos);
    }

    /**
     * Marks the section containing {@code pos} dirty - plus any section the block borders - so a single block change
     * only rebuilds the affected sections instead of the whole render volume. Neighbours are included when the block
     * sits on a section face/edge/corner so cross-section face culling stays correct.
     */
    private void markSectionsDirty(BlockPos pos) {
        WorldGeometryRenderer renderer = this.geometry;

        int sectionX = pos.getX() >> 4;
        int sectionY = pos.getY() >> 4;
        int sectionZ = pos.getZ() >> 4;

        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        for (int dx = (localX == 0 ? -1 : 0); dx <= (localX == 15 ? 1 : 0); dx++)
            for (int dy = (localY == 0 ? -1 : 0); dy <= (localY == 15 ? 1 : 0); dy++)
                for (int dz = (localZ == 0 ? -1 : 0); dz <= (localZ == 15 ? 1 : 0); dz++)
                    renderer.markSectionDirty(ChunkSectionPos.from(sectionX + dx, sectionY + dy, sectionZ + dz));
    }

    public void onChunkData(ChunkDataS2CPacket chunkDataS2CPacket) {
        int i = chunkDataS2CPacket.getX();
        int j = chunkDataS2CPacket.getZ();

        this.world.getChunkManager().setChunkMapCenter(i, j);

        this.loadChunk(i, j, chunkDataS2CPacket.getChunkData());
        LightData lightData = chunkDataS2CPacket.getLightData();

        this.world.enqueueChunkUpdate(() -> {
            this.readLightData(i, j, lightData);
            WorldChunk worldChunk = this.world.getChunkManager().getWorldChunk(i, j, false);
            if (worldChunk != null) {
                this.scheduleRenderChunk(worldChunk, i, j);
            }
        });
    }

    private void readLightData(int x, int z, LightData data) {
        LightingProvider lightingProvider = this.world.getChunkManager().getLightingProvider();
        BitSet bitSet = data.getInitedSky();
        BitSet bitSet2 = data.getUninitedSky();
        Iterator<byte[]> iterator = data.getSkyNibbles().iterator();
        this.updateLighting(x, z, lightingProvider, LightType.SKY, bitSet, bitSet2, iterator);
        BitSet bitSet3 = data.getInitedBlock();
        BitSet bitSet4 = data.getUninitedBlock();
        Iterator<byte[]> iterator2 = data.getBlockNibbles().iterator();
        this.updateLighting(x, z, lightingProvider, LightType.BLOCK, bitSet3, bitSet4, iterator2);
        lightingProvider.setColumnEnabled(new ChunkPos(x, z), true);
    }

    private void updateLighting(int chunkX, int chunkZ, LightingProvider provider, LightType type, BitSet inited, BitSet uninited, Iterator<byte[]> nibbles) {
        WorldGeometryRenderer renderer = this.geometry;

        for (int i = 0; i < provider.getHeight(); ++i) {
            int j = provider.getBottomY() + i;
            boolean bl = inited.get(i);
            boolean bl2 = uninited.get(i);
            if (!bl && !bl2) continue;
            provider.enqueueSectionData(type, ChunkSectionPos.from(chunkX, j, chunkZ), bl ? new ChunkNibbleArray(nibbles.next().clone()) : new ChunkNibbleArray());

            if (renderer != null)
                renderer.markSectionDirty(ChunkSectionPos.from(chunkX, j, chunkZ));
        }
    }

    private void loadChunk(int x, int z, ChunkData chunkData) {
        this.world.getChunkManager().loadChunkFromPacket(x, z, chunkData.getSectionsDataBuf(), chunkData.getHeightmap(), chunkData.getBlockEntities(x, z));
    }

    public void onChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket packet) {
        this.world.getChunkManager().setChunkMapCenter(packet.getChunkX(), packet.getChunkZ());
    }

    /**
     * Syncs the exterior world's clock onto the shadow world. The shadow world isn't part of the tick loop and never
     * receives the normal time broadcast (the proxy isn't a real player), so without this its sky angle is frozen and
     * the doorway shows a fixed time of day regardless of the real one. Mirrors ClientPlayNetworkHandler#onWorldTimeUpdate.
     */
    public void onWorldTime(WorldTimeUpdateS2CPacket packet) {
        this.world.setTime(packet.getTime());
        this.world.setTimeOfDay(packet.getTimeOfDay());
    }

    /**
     * Mirrors the rain/thunder cases of {@code ClientPlayNetworkHandler#onGameStateChange} onto the shadow world so
     * the doorway's sky colour, fog and lighting darken when it's raining/storming where the TARDIS actually is. The
     * server pushes the exterior's gradients each refresh (see {@code ExampleMod#broadcastWeather}); the shadow world
     * isn't ticked, so we set the gradient directly rather than relying on the usual client-side interpolation. Other
     * game-state reasons (game-mode changes, demo messages, etc.) are irrelevant to a render-only mirror and ignored.
     */
    public void onGameStateChange(GameStateChangeS2CPacket packet) {
        GameStateChangeS2CPacket.Reason reason = packet.getReason();
        float value = packet.getValue();

        if (reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED)
            this.world.setRainGradient(value);
        else if (reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED)
            this.world.setThunderGradient(value);
    }

    public void onChunkBiomeData(ChunkBiomeDataS2CPacket packet) {
        for (ChunkBiomeDataS2CPacket.Serialized serialized : packet.chunkBiomeData()) {
            this.world.getChunkManager().onChunkBiomeData(serialized.pos().x, serialized.pos().z, serialized.toReadingBuf());
        }
        for (ChunkBiomeDataS2CPacket.Serialized serialized : packet.chunkBiomeData()) {
            this.world.resetChunkColor(new ChunkPos(serialized.pos().x, serialized.pos().z));
        }
        for (ChunkBiomeDataS2CPacket.Serialized serialized : packet.chunkBiomeData()) {
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    for (int k = this.world.getBottomSectionCoord(); k < this.world.getTopSectionCoord(); ++k) {
                        this.renderer.scheduleBlockRender(serialized.pos().x + i, k, serialized.pos().z + j);
                    }
                }
            }
        }
    }

    private void scheduleRenderChunk(WorldChunk chunk, int x, int z) {
        LightingProvider lightingProvider = this.world.getChunkManager().getLightingProvider();
        ChunkSection[] chunkSections = chunk.getSectionArray();
        ChunkPos chunkPos = chunk.getPos();
        WorldGeometryRenderer renderer = this.geometry;

        for (int i = 0; i < chunkSections.length; ++i) {
            ChunkSection chunkSection = chunkSections[i];
            int j = this.world.sectionIndexToCoord(i);
            lightingProvider.setSectionStatus(ChunkSectionPos.from(chunkPos, j), chunkSection.isEmpty());

            if (renderer != null)
                renderer.markSectionDirty(ChunkSectionPos.from(chunkPos, j));
        }
    }

    public void onUnloadChunk(UnloadChunkS2CPacket packet) {
        this.world.getChunkManager().unload(packet.getX(), packet.getZ());

        WorldGeometryRenderer renderer = this.geometry;
        if (renderer == null)
            return;

        // The chunk's blocks are gone now. Drop its sections directly rather than scheduling a rebuild: the build
        // path skips unloaded columns (so it can't blank good geometry mid-stream), which would otherwise leave the
        // now-unloaded geometry stuck on screen forever.
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++)
            renderer.dropSection(ChunkSectionPos.from(packet.getX(), y, packet.getZ()));
    }

    // ===== Entities =====
    // These mirror ClientPlayNetworkHandler's entity handling, but target the shadow world so the doorway shows
    // the mobs, items, projectiles, etc. tracked around the exterior.

    public void onEntitySpawn(EntitySpawnS2CPacket packet) {
        EntityType<?> type = packet.getEntityType();
        Entity entity = type.create(this.world);

        if (entity == null)
            return;

        entity.onSpawnPacket(packet);
        this.world.addEntity(packet.getId(), entity);
    }

    /**
     * Mirrors {@code ClientPlayNetworkHandler#onPlayerSpawn} onto the shadow world so other players standing around
     * the exterior show through the doorway. (In 1.20.1 players still spawn via their own packet, not the unified
     * entity-spawn packet.) Building the player needs their profile from the real connection's tab list; if they
     * aren't in it there's no skin/profile to render from, so we skip them rather than spawn a broken entity.
     */
    public void onPlayerSpawn(PlayerSpawnS2CPacket packet) {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null)
            return;

        PlayerListEntry entry = handler.getPlayerListEntry(packet.getPlayerUuid());
        if (entry == null)
            return;

        OtherClientPlayerEntity player = new OtherClientPlayerEntity(this.world, entry.getProfile());
        int id = packet.getId();
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float yaw = packet.getYaw() * 360 / 256.0F;
        float pitch = packet.getPitch() * 360 / 256.0F;

        player.setId(id);
        player.updateTrackedPosition(x, y, z);
        player.updatePositionAndAngles(x, y, z, yaw, pitch);
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
        this.world.addEntity(id, player);
    }

    public void onEntityPosition(EntityPositionS2CPacket packet) {
        Entity entity = this.world.getEntityById(packet.getId());
        if (entity == null)
            return;

        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        entity.getTrackedPosition().setPos(pos);
        // interpolate = true, matching ClientPlayNetworkHandler#onEntityPosition: an absolute teleport is smoothed
        // over the interpolation steps (the entity's tick consumes them), so the body moves - and its walk/limb
        // animation is driven off that per-tick position delta - instead of snapping and staying visually idle.
        entity.updateTrackedPositionAndAngles(pos.x, pos.y, pos.z,
                packet.getYaw() * 360 / 256.0F, packet.getPitch() * 360 / 256.0F, 3, true);
        entity.setOnGround(packet.isOnGround());
    }

    public void onEntityMove(EntityS2CPacket packet) {
        Entity entity = packet.getEntity(this.world);
        if (entity == null)
            return;

        if (packet.isPositionChanged()) {
            TrackedPosition tracked = entity.getTrackedPosition();
            Vec3d pos = tracked.withDelta(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
            tracked.setPos(pos);

            float yaw = packet.hasRotation() ? packet.getYaw() * 360 / 256.0F : entity.getYaw();
            float pitch = packet.hasRotation() ? packet.getPitch() * 360 / 256.0F : entity.getPitch();
            entity.updateTrackedPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch, 3, false);
        } else if (packet.hasRotation()) {
            entity.updateTrackedPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(),
                    packet.getYaw() * 360 / 256.0F, packet.getPitch() * 360 / 256.0F, 3, false);
        }

        entity.setOnGround(packet.isOnGround());
    }

    public void onEntityVelocity(EntityVelocityUpdateS2CPacket packet) {
        Entity entity = this.world.getEntityById(packet.getId());
        if (entity == null)
            return;

        entity.setVelocityClient(packet.getVelocityX() / 8000.0,
                packet.getVelocityY() / 8000.0, packet.getVelocityZ() / 8000.0);
    }

    public void onEntitySetHeadYaw(EntitySetHeadYawS2CPacket packet) {
        Entity entity = packet.getEntity(this.world);
        if (entity == null)
            return;

        // Mirror ClientPlayNetworkHandler: feed the head yaw into the interpolation the entity's own tick consumes
        // (serverHeadYaw + headTrackingIncrements), NOT setHeadYaw. Setting it directly fights the per-tick head/body
        // turn logic - the head snaps to each packet then the tick drags it back toward the (unset) tracked target,
        // which reads as the head endlessly spinning.
        entity.updateTrackedHeadRotation(packet.getHeadYaw() * 360 / 256.0F, 3);
    }

    /**
     * Mirrors the hand-swing / wake-up cases of {@code ClientPlayNetworkHandler#onEntityAnimation} onto the shadow
     * world. Arm swinging is driven purely by this packet (not by any tracked data or movement), so without it the
     * mobs and players seen through the doorway never swing when they attack, mine or use an item - the "player
     * animations don't transfer" report. The crit / enchanted-hit cases spawn particle emitters bound to the main
     * particle manager, which don't belong in the doorway, so they're intentionally skipped.
     */
    public void onEntityAnimation(EntityAnimationS2CPacket packet) {
        if (!(this.world.getEntityById(packet.getId()) instanceof LivingEntity living))
            return;

        if (packet.getAnimationId() == EntityAnimationS2CPacket.SWING_MAIN_HAND)
            living.swingHand(Hand.MAIN_HAND);
        else if (packet.getAnimationId() == EntityAnimationS2CPacket.SWING_OFF_HAND)
            living.swingHand(Hand.OFF_HAND);
        else if (packet.getAnimationId() == EntityAnimationS2CPacket.WAKE_UP && living instanceof PlayerEntity player)
            player.wakeUp(false, false);
    }

    public void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet) {
        Entity entity = this.world.getEntityById(packet.id());

        if (entity != null && packet.trackedValues() != null)
            entity.getDataTracker().writeUpdatedEntries(packet.trackedValues());
    }

    public void onEntityEquipment(EntityEquipmentUpdateS2CPacket packet) {
        if (this.world.getEntityById(packet.getId()) instanceof LivingEntity living) {
            for (Pair<EquipmentSlot, ItemStack> pair : packet.getEquipmentList())
                living.equipStack(pair.getFirst(), pair.getSecond());
        }
    }

    public void onEntitiesDestroy(EntitiesDestroyS2CPacket packet) {
        for (int i = 0; i < packet.getEntityIds().size(); i++) {
            int id = packet.getEntityIds().getInt(i);
            Entity entity = this.world.getEntityById(id);

            if (entity != null)
                this.world.removeEntity(id, Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * Spawns the ambient "random display" particles (torch flames, campfire smoke, water drips, dripping lava, etc.)
     * for the blocks around {@code center}, mirroring {@code ClientWorld#doRandomBlockDisplayTicks}. These particles
     * are normally produced client-side by the world's own tick loop; the shadow world isn't part of it, so without
     * this the doorway shows the terrain but none of the little environmental particles that make it feel alive.
     * <p>
     * {@code Block#randomDisplayTick} spawns via {@code world.addParticle}, which routes to
     * {@code MinecraftClient#particleManager}; the caller ({@code PortalDataManager}) temporarily swaps that field to
     * this portal's manager so the particles land in the doorway instead of the interior the player is standing in.
     */
    public void spawnDisplayParticles(int centerX, int centerY, int centerZ, int radius) {
        Random random = this.world.getRandom();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int i = 0; i < 667; i++) {
            int x = centerX + random.nextInt(radius) - random.nextInt(radius);
            int y = centerY + random.nextInt(radius) - random.nextInt(radius);
            int z = centerZ + random.nextInt(radius) - random.nextInt(radius);
            pos.set(x, y, z);

            try {
                BlockState state = this.world.getBlockState(pos);
                if (!state.isAir())
                    state.getBlock().randomDisplayTick(state, this.world, pos, random);

                FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty())
                    fluid.randomDisplayTick(this.world, pos, random);
            } catch (Exception ignored) {
                // A single block's display tick (e.g. an ait: block depending on client tardis state) must not
                // sink the whole ambient-particle pass.
            }
        }
    }

    /**
     * Steps the shadow world's entities once per client tick so their tracked positions interpolate and their
     * models animate; the shadow world is not part of the client tick loop, so nothing else advances them.
     */
    public void tickEntities() {
        List<Entity> snapshot = new ArrayList<>();
        this.world.getEntities().forEach(snapshot::add);

        for (Entity entity : snapshot) {
            if (entity == null || entity.isRemoved())
                continue;

            // Skip passengers here, exactly like ClientWorld#tickEntities: tickEntity walks the passenger list
            // itself (tickPassenger). Ticking a mounted entity from this top-level loop as well would advance it
            // twice per client tick - age and the tracked-position/head interpolation would step double-time, so
            // its spin/head/limbs render at 2x then snap back: the "heads and item spin jitter" report.
            if (entity.hasVehicle())
                continue;

            // One entity whose tick throws (a half-streamed mob, a block-entity-backed state read, etc.) must not
            // abort the rest of the loop. If it did, every entity after it would silently stop ticking that frame,
            // freezing their age/interpolation and making all their animations stutter.
            try {
                this.world.tickEntity(entity);
            } catch (Throwable t) {
                AITMod.LOGGER.error("BOTI: failed to tick shadow entity {}", entity, t);
            }
        }
    }

    /**
     * Frees this shadow world's render resources. {@code setWorld(null)} stops the dedicated chunk-builder threads
     * and releases the built-chunk storage; {@code close()} frees the entity-outline framebuffer and post shaders.
     * Without this, every dimension change / new viewer (which rebuilds the {@link PortalData}) leaks an entire
     * {@link WorldRenderer} - GL buffers, an FBO and live threads. Must run on the render thread (all callers do).
     */
    public void close() {
        try {
            this.geometry.close();      // frees this shadow world's section VBOs + builder thread
            this.renderer.setWorld(null);
            this.renderer.close();
        } catch (Exception e) {
            AITMod.LOGGER.error("Failed to close shadow world for portal {}", this.id, e);
        }
    }

    public static PortalData fromCurrent(UUID id) {
        ClientWorld old = MinecraftClient.getInstance().world;
        RegistryKey<DimensionType> type = old.getDimensionEntry().getKey().orElse(DimensionTypes.OVERWORLD);

        return create(id, old.getRegistryKey(), type);
    }

    /**
     * Builds a shadow world mirroring the given dimension. The server tells us which dimension a TARDIS's
     * exterior is in (see {@link dev.loqor.portal.PortalInitS2CPacket}) so the doorway renders with the correct
     * lighting, sky and height limits instead of the interior dimension's.
     */
    public static PortalData create(UUID id, RegistryKey<World> dimension, RegistryKey<DimensionType> dimensionType) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld old = client.world;

        RegistryEntry<DimensionType> typeEntry = old.getRegistryManager()
                .get(RegistryKeys.DIMENSION_TYPE).entryOf(dimensionType);

        WorldRenderer worldRenderer = new WorldRenderer(
                client,
                client.getEntityRenderDispatcher(),
                client.getBlockEntityRenderDispatcher(),
                client.getBufferBuilders()
        );

        ClientWorld world = new ClientWorld(client.getNetworkHandler(), new ClientWorld.Properties(Difficulty.NORMAL,
                false, false), dimension,
                typeEntry,
                12, old.getSimulationDistance(), client::getProfiler, worldRenderer,
                old.isDebugWorld(), old.getBiomeAccess().seed);

        worldRenderer.setWorld(world);

        // Each shadow world owns its geometry renderer, so multiple portals (e.g. several TARDIS exteriors on
        // screen, or the exterior-view and interior-view streams of one TARDIS) bake and draw independently.
        WorldGeometryRenderer geometry = new WorldGeometryRenderer(renderDistanceBlocks());

        return new PortalData(id, worldRenderer, world, geometry);
    }
}
