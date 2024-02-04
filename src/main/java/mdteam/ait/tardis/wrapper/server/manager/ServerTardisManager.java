package mdteam.ait.tardis.wrapper.server.manager;

import com.google.gson.GsonBuilder;
import mdteam.ait.AITMod;
import mdteam.ait.compat.DependencyChecker;
import mdteam.ait.compat.immersive.PortalsHandler;
import mdteam.ait.core.AITDimensions;
import mdteam.ait.core.managers.DeltaTimeManager;
import mdteam.ait.tardis.*;
import mdteam.ait.tardis.exterior.ExteriorCategory;
import mdteam.ait.tardis.util.NetworkUtil;
import mdteam.ait.tardis.util.TardisUtil;
import mdteam.ait.tardis.util.AbsoluteBlockPos;
import mdteam.ait.tardis.util.SerialDimension;
import mdteam.ait.tardis.variant.exterior.ExteriorVariantSchema;
import mdteam.ait.tardis.wrapper.client.manager.ClientTardisManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import mdteam.ait.tardis.wrapper.server.ServerTardis;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerTardisManager extends TardisManager<ServerTardis> implements TardisTickable {

    public static final Identifier SEND = new Identifier("ait", "send_tardis");
    public static final Identifier UPDATE = new Identifier("ait", "update_tardis");
    private static ServerTardisManager instance;
    // Changed from MultiMap to HashMap to fix some concurrent issues, maybe
    private final ConcurrentHashMap<UUID, List<UUID>> subscribers = new ConcurrentHashMap<>(); // fixme most of the issues with tardises on client when the world gets reloaded is because the subscribers dont get readded so the client stops getting informed, either save this somehow or make sure the client reasks on load.
    private final ConcurrentHashMap<UUID, List<UUID>> buffers = new ConcurrentHashMap<>(); // buffer for sending tardises

    public ServerTardisManager() {
        this.loadTardises();

        ServerPlayNetworking.registerGlobalReceiver(
                ClientTardisManager.ASK, (server, player, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    if (player == null) return; // todo why does creativious have trust issues with player being null
                    this.sendTardis(player, uuid);
                    addSubscriberToTardis(player, uuid);
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ClientTardisManager.LET_KNOW_UNLOADED, (server, player, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    if (player == null) return;
                    removeSubscriberToTardis(player, uuid);
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ClientTardisManager.ASK_POS, (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = AbsoluteBlockPos.fromNbt(buf.readNbt());
                    UUID uuid = null;
                    for (Tardis tardis : this.getLookup().values()) {
                        if (!tardis.getTravel().getPosition().equals(pos)) continue;

                        uuid = tardis.getUuid();
                    }
                    if (uuid == null)
                        return;
                    this.sendTardis(player, uuid);
                    addSubscriberToTardis(player, uuid);
                }
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // todo clean up this
            // force all dematting to go flight and all matting to go land
            for (Tardis tardis : this.getLookup().values()) {
                if (tardis.getTravel().getState() == TardisTravel.State.DEMAT) {
                    tardis.getTravel().toFlight();
                } else if (tardis.getTravel().getState() == TardisTravel.State.MAT) {
                    tardis.getTravel().forceLand();
                }

                tardis.getDoor().closeDoors();

                if (DependencyChecker.hasPortals())
                    PortalsHandler.removePortals(tardis);
            }

            this.reset();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> this.reset());

        // ServerLifecycleEvents.SERVER_STARTED.register(server -> this.loadTardises());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // fixme would this cause lag?
            for (Tardis tardis : ServerTardisManager.getInstance().getLookup().values()) {
                tardis.tick(server);
            }

            tick(server);
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            // fixme lag?
            for (Tardis tardis : ServerTardisManager.getInstance().getLookup().values()) {
                tardis.tick(world);
            }
            tick(world);
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (Tardis tardis : ServerTardisManager.getInstance().getLookup().values()) {
                tardis.startTick(server);
            }
            startTick(server);
        });
    }

    /**
     * Adds a subscriber to the Tardis
     * @param serverPlayerEntity PLAYER
     * @param tardisUUID TARDIS UUID
     */
    private void addSubscriberToTardis(ServerPlayerEntity serverPlayerEntity, UUID tardisUUID) {
        if (this.subscribers.containsKey(tardisUUID)) {
            this.subscribers.get(tardisUUID).add(serverPlayerEntity.getUuid());
        } else {
            List<UUID> subscriber_list = new CopyOnWriteArrayList<>();
            subscriber_list.add(serverPlayerEntity.getUuid());
            this.subscribers.put(tardisUUID, subscriber_list);
        }
    }

    /**
     * Removes a subscriber from the TARDIS
     * @param serverPlayerEntity the player to remove from the subscribers list
     * @param tardisUUID the UUID of the TARDIS
     */
    private void removeSubscriberToTardis(ServerPlayerEntity serverPlayerEntity, UUID tardisUUID) {
        if (!this.subscribers.containsKey(tardisUUID)) return; // If the Tardis does not have any subscribers ignore this

        List<UUID> old_uuids = this.subscribers.get(tardisUUID);
        int i_to_remove = -1;

        for (int i = 0; i < old_uuids.size(); i++) {
            if (old_uuids.get(i).equals(serverPlayerEntity.getUuid())) {
                i_to_remove = i;
                break;
            }
        }

        if (i_to_remove == -1) return; // If the player is not in the list ignore this

        old_uuids.remove(i_to_remove);
        if (old_uuids.isEmpty()) {
            this.subscribers.remove(tardisUUID);
        } else {
            this.subscribers.put(tardisUUID, old_uuids); // update the subscriber list in case any other subscriber was added or removed during this operation
        }
    }

    /**
     * Removes all subscribers from the TARDIS
     * @param tardisUUID the TARDIS UUID
     */
    private void removeAllSubscribersFromTardis(UUID tardisUUID) {
        this.subscribers.replace(tardisUUID, new CopyOnWriteArrayList<>());
    }

    public ServerTardis create(AbsoluteBlockPos.Directed pos, ExteriorCategory exteriorType, ExteriorVariantSchema variantType, TardisDesktopSchema schema, boolean locked) {
        UUID uuid = UUID.randomUUID();

        ServerTardis tardis = new ServerTardis(uuid, pos, schema, exteriorType, variantType, locked); // todo removed "locked" param
        // tardis.setFuelCount(1000); // Default fuel count is 100 - cant be set here causes issues. set in PropertiesHandler instead
        //this.saveTardis(tardis);
        this.lookup.put(uuid, tardis);

        // todo this can be moved to init
        tardis.getTravel().placeExterior();
        tardis.getTravel().runAnimations();

        tardis.getHandlers().getStats().markCreationDate();

        this.saveTardis(tardis);

        return tardis;
    }

    public Tardis getTardis(UUID uuid) {
        if (uuid == null) return null; // ugh

        if (this.lookup.containsKey(uuid))
            return this.lookup.get(uuid);

        return this.loadTardis(uuid);
    }

    @Override
    public void loadTardis(UUID uuid, Consumer<ServerTardis> consumer) {
        consumer.accept(this.loadTardis(uuid));
    }


    @Override
    public GsonBuilder getGsonBuilder(GsonBuilder builder) {
        builder.registerTypeAdapter(SerialDimension.class, SerialDimension.serializer());
        return builder;
    }

    public static void init() {
        instance = new ServerTardisManager();
    }


    public void sendToSubscribers(Tardis tardis) {
        // todo this likely needs refactoring
//        if (tardis == null) return;
//        if (!this.subscribers.containsKey(tardis.getUuid())) return;
//        MinecraftServer mc = TardisUtil.getServer();
//
//        Map<UUID, List<UUID>> subscribersCopy = new HashMap<>(this.subscribers);
//        List<UUID> tardisSubscribers = new CopyOnWriteArrayList<>(subscribersCopy.getOrDefault(tardis.getUuid(), Collections.emptyList()));
//
//        for (UUID uuid : tardisSubscribers) {
//            ServerPlayerEntity player = mc.getPlayerManager().getPlayer(uuid);
//            if (player == null) continue;
//            this.sendTardis(player, tardis);
//        }
        for (ServerPlayerEntity player : NetworkUtil.getNearbyTardisPlayers(tardis)) {
            this.sendTardis(player, tardis);
        }
    }

    // TODO - yes this is much better than sending the entire tardis class, but it still sends the entire component class. If everything is saved in a PropertiesHolder then this is a non-issue though.
    public void sendToSubscribers(AbstractTardisComponent component) {

        if (component.getTardis().isEmpty()) return;

        for (ServerPlayerEntity player : NetworkUtil.getNearbyTardisPlayers(this.getTardis(component.getTardis().get().getUuid()))) {
            this.updateTardis(player, component.getTardis().get().getUuid(), component);
        }
    }

    /**
     * For sending changes to do with the PropertiesHandler
     * @param uuid The TARDIS UUID to sync to
     * @param key The key where the value is stored in the PropertiesHolder
     * @param type The class of the variable, serialised so it can be read later
     *             eg a double would be "double", boolean would be "boolean", etc
     * @param value The new value to be synced to client
     */
    public void sendToSubscribers(UUID uuid, String key, String type, String value) {
        for (ServerPlayerEntity player : NetworkUtil.getNearbyTardisPlayers(this.getTardis(uuid))) {
            this.updateTardisProperty(player, uuid, key, type, value);
        }
    }
    public void updateTardisProperty(@NotNull ServerPlayerEntity player, UUID tardis, String key, String type, String value) {
        PacketByteBuf data = PacketByteBufs.create();

        data.writeUuid(tardis);
        data.writeString("properties");

        data.writeString(key);
        data.writeString(type);
        data.writeString(value);

        ServerPlayNetworking.send(player, UPDATE, data);

        checkForceSync(player, tardis);
    }
    private void updateTardis(@NotNull ServerPlayerEntity player, UUID uuid, AbstractTardisComponent component) {
        this.updateTardis(player, uuid, component.getId(), this.gson.toJson(component));
    }

    private void updateTardis(@NotNull ServerPlayerEntity player, UUID uuid, String header, String json) {
        PacketByteBuf data = PacketByteBufs.create();
        data.writeUuid(uuid);
        data.writeString(header);
        data.writeString(json);

        ServerPlayNetworking.send(player, UPDATE, data);

        checkForceSync(player, uuid);
    }

    private void sendTardis(@NotNull ServerPlayerEntity player, UUID uuid) {
        this.sendTardis(player, this.getTardis(uuid));
    }

    private void sendTardis(@NotNull ServerPlayerEntity player, Tardis tardis) {
        this.sendTardis(player, tardis.getUuid(), this.gson.toJson(tardis, ServerTardis.class));
    }

    private void sendTardis(@NotNull ServerPlayerEntity player, UUID uuid, String json) {
        if (this.isInBuffer(player, uuid)) {
            return;
        }
        if (isAskOnDelay(player)) {
            this.addToBuffer(player, uuid);
            return;
        }

        // Is this really necessary? On servers it results in unnecessary console spam. - Loqor
        //AITMod.LOGGER.info("SENDING TARDIS " + uuid + " TO " + player.getName().getString());

        PacketByteBuf data = PacketByteBufs.create();
        data.writeUuid(uuid);
        data.writeString(json);

        ServerPlayNetworking.send(player, SEND, data);

        createAskDelay(player);
        createForceSyncDelay(player);
    }

    /**
     * A delay to stop the client getting overloaded with tons of tardises all at once, splitting it up over a few seconds to save server performance.
     */
    private void createAskDelay(ServerPlayerEntity player) {
        DeltaTimeManager.createDelay(player.getUuidAsString() + "-ask-delay", (long) ((AITMod.AIT_CUSTOM_CONFIG.SERVER.ASK_DELAY) * 1000L)); // A delay between asking for tardises to be synced
    }
    private boolean isAskOnDelay(ServerPlayerEntity player) {
        return DeltaTimeManager.isStillWaitingOnDelay(player.getUuidAsString() + "-ask-delay");
    }

    /**
     * A delay to force resync the server when its been a while since theyve seen a tardis to fix sync issues
     */
    private void createForceSyncDelay(ServerPlayerEntity player) {
        DeltaTimeManager.createDelay(player.getUuidAsString() + "-force-sync-delay", (long) ((AITMod.AIT_CUSTOM_CONFIG.SERVER.FORCE_SYNC_DELAY) * 1000L)); // A delay between asking for tardises to be synced
    }
    private boolean isForceSyncOnDelay(ServerPlayerEntity player) {
        return DeltaTimeManager.isStillWaitingOnDelay(player.getUuidAsString() + "-force-sync-delay");
    }
    private void checkForceSync(ServerPlayerEntity player, UUID tardis) {
        if (!isForceSyncOnDelay(player)) {
            this.sendTardis(player, tardis);
        }
        createForceSyncDelay(player);
    }
    public void onPlayerJoin(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() == AITDimensions.TARDIS_DIM_WORLD) {
            // if the player is a tardis already, sync the one at their location
            Tardis found = TardisUtil.findTardisByInterior(player.getBlockPos(), true);
            if (found == null) return;

            this.sendTardis(player, found);
        }
    }

    @Override
    public void reset() {
        this.subscribers.clear();

        this.saveTardis();
        super.reset();
    }

    private static File getSavePath() {
        return new File(TardisUtil.getSavePath() + "ait/");
    }

    private static File getSavePath(UUID uuid) {
        return new File(getSavePath(), uuid + ".json");
    }

    private static File getSavePath(Tardis tardis) {
        return ServerTardisManager.getSavePath(tardis.getUuid());
    }

    public static ServerTardisManager getInstance() {
        return instance;
    }

    public void loadTardises() {
        File[] saved = ServerTardisManager.getSavePath().listFiles();

        if (saved == null)
            return;

        for (String name : Stream.of(saved)
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet())
        ) {
            if (!name.substring(name.lastIndexOf(".") + 1).equalsIgnoreCase("json"))
                continue;

            UUID uuid = UUID.fromString(name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".")));
            this.loadTardis(uuid);
            this.backupTardis(uuid);
        }
    }
    private ServerTardis loadTardis(UUID uuid) {
        File file = ServerTardisManager.getSavePath(uuid);
        file.getParentFile().mkdirs();

        try {
            if (!file.exists())
                throw new IOException("Tardis file " + file + " doesn't exist!");

            String json = Files.readString(file.toPath());

            ServerTardis tardis = this.gson.fromJson(json, ServerTardis.class);
            tardis.init(true);
            this.lookup.put(tardis.getUuid(), tardis);

            return tardis;
        } catch (IOException e) {
            AITMod.LOGGER.warn("Failed to load tardis with uuid {}!", file);
            AITMod.LOGGER.warn(e.getMessage());
        }

        return null;
    }
    public void backupTardis(UUID uuid) {
        File file = ServerTardisManager.getSavePath(uuid);
        file.getParentFile().mkdirs();

        File backup = new File(getSavePath(), uuid + ".old");
        backup.getParentFile().mkdirs();

        try {
            if (!file.exists())
                throw new IOException("Tardis file " + file + " doesn't exist!");

            String json = Files.readString(file.toPath());

            Files.writeString(backup.toPath(), json);
        } catch (IOException e) {
            AITMod.LOGGER.warn("Failed to backup tardis with uuid {}!", file);
            AITMod.LOGGER.warn(e.getMessage());
        }
    }

    public void saveTardis() {
        for (ServerTardis tardis : this.lookup.values()) {
            this.saveTardis(tardis);
        }

        // this might fix server crash bugs
        if (this.lookup.isEmpty()) {
            this.loadTardises();

            if (!this.lookup.isEmpty())
                this.saveTardis();
        }
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveTardis(ServerTardis tardis) {
        File savePath = ServerTardisManager.getSavePath(tardis);
        savePath.getParentFile().mkdirs();

        try {
            Files.writeString(savePath.toPath(), this.gson.toJson(tardis, ServerTardis.class));
        } catch (IOException e) {
            AITMod.LOGGER.warn("Couldn't save Tardis {}", tardis.getUuid());
            AITMod.LOGGER.warn(e.getMessage());
        }
    }

    private boolean isInBuffer(ServerPlayerEntity player, UUID tardis) {
        if (!this.buffers.containsKey(player.getUuid())) return false;

        return this.buffers.get(player.getUuid()).contains(tardis);
    }

    private void addToBuffer(ServerPlayerEntity player, UUID tardis) {
        if (this.buffers.containsKey(player.getUuid())) {
            this.buffers.get(player.getUuid()).add(tardis);
            return;
        }

        this.buffers.put(player.getUuid(), new ArrayList<>(Collections.singletonList(tardis)));
    }

    private void tickBuffer(MinecraftServer server) {
        if (this.buffers.isEmpty()) return;

        for (Iterator<UUID> it = this.buffers.keys().asIterator(); it.hasNext(); ) {
            UUID playerId = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);

            if (player == null
                    || !this.buffers.containsKey(playerId)
                    || isAskOnDelay(player)) continue;

            List<UUID> tardisIds = this.buffers.get(playerId);

            if (tardisIds == null || tardisIds.isEmpty()) continue;

            List<UUID> copyOfTardisIds = new CopyOnWriteArrayList<>(tardisIds);

            for (UUID tardisId : copyOfTardisIds) {
                tardisIds.remove(tardisId);
                this.sendTardis(player, tardisId);
            }

            if (tardisIds.isEmpty())
                this.buffers.remove(playerId);
        }
    }

    @Override
    public void tick(MinecraftServer server) {
        this.tickBuffer(server);
    }

    @Override
    public void tick(ServerWorld world) {

    }

    @Override
    public void tick(MinecraftClient client) {
        // this will never be called
    }

    @Override
    public void startTick(MinecraftServer server) {

    }
}
