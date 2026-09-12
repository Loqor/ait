package dev.loqor.portal.client;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.boti.PortalParticleManager;
import dev.loqor.portal.PortalInitS2CPacket;
import dev.loqor.portal.WrappedPacketS2CPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;

public class PortalDataManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    // TODO: replace with array or intmap maybe
    private static final Map<UUID, PortalData> map = new HashMap<>();

    // Per-portal particle managers, kept here (rather than on the PortalData record) so the shadow world's
    // particles tick and render in the doorway without touching the main world's particle manager.
    private static final Map<UUID, PortalParticleManager> particles = new HashMap<>();
    private static final Random random = Random.create();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(WrappedPacketS2CPacket.TYPE, (wrapped, player, packetSender) -> {
            handle(wrapped);
        });

        ClientPlayNetworking.registerGlobalReceiver(PortalInitS2CPacket.TYPE, (packet, player, packetSender) -> {
            handleInit(packet.id(), packet.dimension(), packet.dimensionType());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> {
            // reset() -> PortalData.close() deletes GL buffers, which must happen on the render thread. This
            // DISCONNECT callback fires on the netty IO thread, so marshal the teardown across - otherwise every
            // disconnect throws "RenderSystem called from wrong thread" and leaks the shadow-world VBOs.
            if (minecraftClient.isOnThread())
                reset();
            else
                minecraftClient.execute(PortalDataManager::reset);
        });

        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            for (PortalData data : new ArrayList<>(map.values())) {
                // Each per-tick step is isolated so a failure in one can't skip the ones after it. In particular
                // entity ticking must run every tick regardless of the chunk/time steps: if it were skipped on the
                // ticks one of those threw, entity age and interpolation would advance unevenly and every animation
                // (head turn, item spin, walk) would stutter.

                // The shadow world isn't part of the client tick loop, so nothing else drains the queue that
                // onChunkData fills - the work that applies a chunk's light data and marks its sections dirty for
                // rebuilding. Without this the doorway only ever shows the initial (empty) build and stays blank.
                step(data, "chunk updates", d -> d.world().runQueuedChunkUpdates());

                // Advance the shadow world's clock every client tick, exactly like the real ClientWorld does in
                // MinecraftClient#tick. Time-driven block-entity animations (banners waving, etc.) read
                // world.getTime()/getTimeOfDay() each frame; without a per-tick advance the clock only moves when a
                // WorldTimeUpdate packet arrives from the server (once per refresh), so those animations stutter,
                // jumping forward once a second. The periodic packet stays authoritative and corrects any drift.
                step(data, "clock", d -> d.world().tickTime());

                step(data, "entities", PortalData::tickEntities);
                step(data, "particles", PortalDataManager::spawnDisplayParticles);
            }

            for (PortalParticleManager manager : new ArrayList<>(particles.values())) {
                try {
                    manager.tick();
                } catch (Exception e) {
                    AITMod.LOGGER.error("BOTI: failed to tick portal particles", e);
                }
            }
        });
    }

    /** Runs one isolated per-tick step for a portal, logging (but not rethrowing) any failure so later steps run. */
    private static void step(PortalData data, String name, java.util.function.Consumer<PortalData> action) {
        try {
            action.accept(data);
        } catch (Exception e) {
            AITMod.LOGGER.error("BOTI: portal '{}' step failed", name, e);
        }
    }

    /**
     * Runs one tick of the shadow world's ambient display-tick particles for a portal, routing them into that
     * portal's {@link PortalParticleManager}. {@link ClientWorld#addParticle} (which {@code randomDisplayTick} calls)
     * always targets {@code MinecraftClient#particleManager}, so the field is swapped to the portal's manager for the
     * duration and restored in a finally - otherwise the particles would spawn in the interior the player is standing
     * in instead of the doorway. Skipped until the portal has rendered at least once (so its centre is known).
     */
    private static void spawnDisplayParticles(PortalData data) {
        BlockPos center = data.geometry().centerPos();
        if (center == null || center.equals(BlockPos.ORIGIN))
            return;

        PortalParticleManager manager = particles.computeIfAbsent(data.id(),
                uuid -> new PortalParticleManager(data.world(), client));

        ParticleManager previous = client.particleManager;
        client.particleManager = manager;
        try {
            data.spawnDisplayParticles(center.getX(), center.getY(), center.getZ(), data.geometry().renderDistance());
        } finally {
            client.particleManager = previous;
        }
    }

    /** Rebuilds the shadow world for a TARDIS in the dimension the server says its exterior now occupies. */
    public static void handleInit(UUID id, RegistryKey<World> dimension, RegistryKey<DimensionType> dimensionType) {
        if (!client.isOnThread()) {
            client.executeSync(() -> handleInit(id, dimension, dimensionType));
            return;
        }

        free(id);
        map.put(id, PortalData.create(id, dimension, dimensionType));
        // The fresh PortalData owns a geometry renderer that already starts needing a full rebuild.
    }

    public static void reset() {
        for (PortalData data : map.values())
            data.close();

        map.clear();
        particles.clear();
    }

    public static void free(UUID id) {
        PortalData data = map.remove(id);
        if (data != null)
            data.close();

        particles.remove(id);
    }

    /** The shadow particle manager for a TARDIS's doorway, or {@code null} if none has spawned particles yet. */
    public static PortalParticleManager particles(UUID id) {
        return particles.get(id);
    }

    public static PortalData getOrCreate(UUID id) {
        // FIXED: Lambda ensures no signature mismatch if fromCurrent doesn't take a UUID
        return map.computeIfAbsent(id, uuid -> PortalData.fromCurrent(id));
    }

    public static PortalData get(UUID id) {
        return map.get(id);
    }

    private static void handle(WrappedPacketS2CPacket packet) {
        handle(packet.id(), packet.packet());
    }

    public static void handle(UUID id, Packet<?> packet) {
        if (!client.isOnThread()) {
            client.executeSync(() -> handle(id, packet));
            return;
        }

        try {
            PortalData data = handle0(id, packet);
            PortalEvents.UPDATE.invoker().onPortalUpdate(data);
        } catch (Exception var3) {
            AITMod.LOGGER.error("Failed to handle packet {}, suppressing error", packet, var3);
        }
    }

    private static PortalData handle0(UUID id, Packet<?> packet) {
        // FIXED: Prevent NullPointerException when receiving packets for a new portal
        PortalData data = getOrCreate(id);

        if (packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> otherPacket : bundle.getPackets()) {
                handle0(data, otherPacket);
            }

            return data;
        }

        handle0(data, packet);
        return data;
    }

    private static void handle0(PortalData data, Packet<?> packet) {
        if (packet instanceof ChunkRenderDistanceCenterS2CPacket render) {
            data.onChunkRenderDistanceCenter(render);
        } else if (packet instanceof WorldTimeUpdateS2CPacket time) {
            data.onWorldTime(time);
        } else if (packet instanceof ChunkDataS2CPacket chunkData) {
            data.onChunkData(chunkData);
        } else if (packet instanceof ChunkDeltaUpdateS2CPacket update) {
            data.onChunkDeltaUpdate(update);
        } else if (packet instanceof BlockUpdateS2CPacket update) {
            data.onBlockUpdate(update);
        } else if (packet instanceof UnloadChunkS2CPacket unload) {
            data.onUnloadChunk(unload);
        } else if (packet instanceof EntitySpawnS2CPacket spawn) {
            data.onEntitySpawn(spawn);
        } else if (packet instanceof PlayerSpawnS2CPacket spawn) {
            data.onPlayerSpawn(spawn);
        } else if (packet instanceof GameStateChangeS2CPacket state) {
            data.onGameStateChange(state);
        } else if (packet instanceof EntityPositionS2CPacket position) {
            data.onEntityPosition(position);
        } else if (packet instanceof EntityS2CPacket move) {
            data.onEntityMove(move);
        } else if (packet instanceof EntityVelocityUpdateS2CPacket velocity) {
            data.onEntityVelocity(velocity);
        } else if (packet instanceof EntitySetHeadYawS2CPacket headYaw) {
            data.onEntitySetHeadYaw(headYaw);
        } else if (packet instanceof EntityAnimationS2CPacket animation) {
            data.onEntityAnimation(animation);
        } else if (packet instanceof EntityTrackerUpdateS2CPacket tracker) {
            data.onEntityTrackerUpdate(tracker);
        } else if (packet instanceof EntityEquipmentUpdateS2CPacket equipment) {
            data.onEntityEquipment(equipment);
        } else if (packet instanceof EntitiesDestroyS2CPacket destroy) {
            data.onEntitiesDestroy(destroy);
        } else if (packet instanceof ParticleS2CPacket particle) {
            onParticle(data, particle);
        } else if (packet instanceof ChunkBiomeDataS2CPacket biome) {
//          this.onChunkBiomeData(biome); // - uncomment if it breaks everything
        }
    }

    /** Mirrors ClientPlayNetworkHandler#onParticle, spawning into the shadow world's particle manager. */
    private static void onParticle(PortalData data, ParticleS2CPacket packet) {
        PortalParticleManager manager = particles.computeIfAbsent(data.id(),
                uuid -> new PortalParticleManager(data.world(), client));

        if (packet.getCount() == 0) {
            // "exact" particle: the offset fields carry the velocity.
            double vx = packet.getSpeed() * packet.getOffsetX();
            double vy = packet.getSpeed() * packet.getOffsetY();
            double vz = packet.getSpeed() * packet.getOffsetZ();
            manager.addParticle(packet.getParameters(),
                    packet.getX(), packet.getY(), packet.getZ(), vx, vy, vz);
        } else {
            for (int i = 0; i < packet.getCount(); i++) {
                double ox = random.nextGaussian() * (double) packet.getOffsetX();
                double oy = random.nextGaussian() * (double) packet.getOffsetY();
                double oz = random.nextGaussian() * (double) packet.getOffsetZ();
                double vx = random.nextGaussian() * (double) packet.getSpeed();
                double vy = random.nextGaussian() * (double) packet.getSpeed();
                double vz = random.nextGaussian() * (double) packet.getSpeed();
                manager.addParticle(packet.getParameters(),
                        packet.getX() + ox, packet.getY() + oy, packet.getZ() + oz, vx, vy, vz);
            }
        }
    }
}