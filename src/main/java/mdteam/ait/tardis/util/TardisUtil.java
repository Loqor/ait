package mdteam.ait.tardis.util;

import io.wispforest.owo.ops.WorldOps;
import mdteam.ait.AITMod;
import mdteam.ait.compat.DependencyChecker;
import mdteam.ait.core.AITDimensions;
import mdteam.ait.core.AITSounds;
import mdteam.ait.core.blockentities.ConsoleBlockEntity;
import mdteam.ait.core.blockentities.DoorBlockEntity;
import mdteam.ait.core.blockentities.ExteriorBlockEntity;
import mdteam.ait.core.events.ServerLoadEvent;
import mdteam.ait.core.item.KeyItem;
import mdteam.ait.registry.DesktopRegistry;
import mdteam.ait.registry.CategoryRegistry;
import mdteam.ait.registry.ExteriorVariantRegistry;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.TardisDesktop;
import mdteam.ait.tardis.TardisManager;
import mdteam.ait.tardis.TardisTravel;
import mdteam.ait.tardis.control.impl.pos.PosType;
import mdteam.ait.tardis.data.DoorData;
import mdteam.ait.tardis.data.properties.PropertiesHandler;
import mdteam.ait.tardis.wrapper.client.manager.ClientTardisManager;
import mdteam.ait.tardis.wrapper.server.manager.ServerTardisManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.api.PortalAPI;

import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unused")
public class TardisUtil {
    private static final Random RANDOM = new Random();
    private static MinecraftServer SERVER;
    private static Path SAVE_PATH;
    private static ServerWorld TARDIS_DIMENSION;
    public static final Identifier CHANGE_EXTERIOR = new Identifier(AITMod.MOD_ID, "change_exterior");
    public static final Identifier SNAP = new Identifier(AITMod.MOD_ID, "snap");

    public static final Identifier FIND_PLAYER = new Identifier(AITMod.MOD_ID, "find_player");

    public static void init() {
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                SERVER = null;
            }
        });

        ServerLoadEvent.LOAD.register(server -> {
            SAVE_PATH = server.getSavePath(WorldSavePath.ROOT);
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                SERVER = server;
            }

            if (world.getRegistryKey() == AITDimensions.TARDIS_DIM_WORLD) {
                TARDIS_DIMENSION = world;
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
            TARDIS_DIMENSION = server.getWorld(AITDimensions.TARDIS_DIM_WORLD);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SERVER = null;
        });
        ServerPlayNetworking.registerGlobalReceiver(CHANGE_EXTERIOR,
                (server, player, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    Identifier exteriorValue = Identifier.tryParse(buf.readString());
                    boolean variantChange = buf.readBoolean();
                    String variantValue = buf.readString();
                    Tardis tardis = ServerTardisManager.getInstance().getTardis(uuid);

                    tardis.getExterior().setType(CategoryRegistry.REGISTRY.get(exteriorValue));
                    WorldOps.updateIfOnServer(server.getWorld(tardis
                                    .getTravel().getPosition().getWorld().getRegistryKey()),
                            tardis.getDoor().getExteriorPos());
                    if (variantChange) {
                        tardis.getExterior().setVariant(ExteriorVariantRegistry.REGISTRY.get(Identifier.tryParse(variantValue)));
                        WorldOps.updateIfOnServer(server.getWorld(tardis
                                        .getTravel().getPosition().getWorld().getRegistryKey()),
                                tardis.getDoor().getExteriorPos());
                    }

                    if (tardis.isGrowth())
                        tardis.getHandlers().getInteriorChanger().queueInteriorChange(DesktopRegistry.get(new Identifier(AITMod.MOD_ID, "type_40")));

                    /*ExteriorEnum[] values = ExteriorEnum.values();
                    int nextIndex = (ServerTardisManager.getInstance().getTardis(uuid).getExterior().getType().ordinal() + 1) % values.length;
                    ServerTardisManager.getInstance().getTardis(uuid).getExterior().setType(values[nextIndex]);
                    WorldOps.updateIfOnServer(server.getWorld(ServerTardisManager.getInstance().getTardis(uuid)
                                    .getTravel().getPosition().getWorld().getRegistryKey()),
                            ServerTardisManager.getInstance().getTardis(uuid).getTravel().getPosition());*/
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(SNAP,
                (server, player, handler, buf, responseSender) -> {
                    UUID uuid = buf.readUuid();
                    Tardis tardis = ServerTardisManager.getInstance().getTardis(uuid);

                    if(tardis.getHandlers().getOvergrown().isOvergrown()) return;

                    player.getWorld().playSound(null, player.getBlockPos(), AITSounds.SNAP, SoundCategory.PLAYERS, 4f, 1f);

                    BlockPos pos = player.getWorld().getRegistryKey() ==
                            TardisUtil.getTardisDimension().getRegistryKey() ? tardis.getDoor().getDoorPos() : tardis.getDoor().getExteriorPos();
                    if ((player.squaredDistanceTo(tardis.getDoor().getExteriorPos().getX(), tardis.getDoor().getExteriorPos().getY(), tardis.getDoor().getExteriorPos().getZ())) <= 200 || TardisUtil.inBox(tardis.getDesktop().getCorners().getBox(), player.getBlockPos())) {
                        if (!player.isSneaking()) {
                            // annoying bad code

                            DoorData.DoorStateEnum state = tardis.getDoor().getDoorState();
                            if (state == DoorData.DoorStateEnum.CLOSED || state == DoorData.DoorStateEnum.FIRST) {
                                DoorData.useDoor(tardis, player.getServerWorld(), null, player);
                                if(tardis.getDoor().isDoubleDoor()) {
                                    DoorData.useDoor(tardis, player.getServerWorld(), null, player);
                                }
                            } else {
                                DoorData.useDoor(tardis, player.getServerWorld(), null, player);
                            }
                        } else {
                            DoorData.toggleLock(tardis, player);
                        }
                    }
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(FIND_PLAYER,
                (server, currentPlayer, handler, buf, responseSender) -> {
                    UUID tardisId = buf.readUuid();
                    UUID playerUuid = buf.readUuid();
                    Tardis tardis = ServerTardisManager.getInstance().getTardis(tardisId);
                    ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(playerUuid);
                    if(tardis.getDesktop().getConsolePos() == null) return;
                    if(serverPlayer == null) {
                        TardisUtil.getTardisDimension().playSound(null, tardis.getDesktop().getConsolePos(), SoundEvents.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.BLOCKS, 3f, 1f);
                        return;
                    }
                    tardis.getTravel().setDestination(new AbsoluteBlockPos.Directed(
                            serverPlayer.getBlockX(),
                                    serverPlayer.getBlockY(),
                                    serverPlayer.getBlockZ(),
                                    serverPlayer.getWorld(),
                                    serverPlayer.getMovementDirection()),
                            PropertiesHandler.getBool(tardis.getHandlers().getProperties(), PropertiesHandler.AUTO_LAND));
                    TardisUtil.getTardisDimension().playSound(null, tardis.getDesktop().getConsolePos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 3f, 1f);
                }
        );
    }

    public static MinecraftServer getServer() {
        return SERVER;
    }

    public static Path getSavePath() {
        if (SAVE_PATH == null && SERVER != null) {
            SAVE_PATH = SERVER.getSavePath(WorldSavePath.ROOT);
        }

        return SAVE_PATH;
    }

    @Deprecated
    public static boolean isClient() {
        return !TardisUtil.isServer();
    }

    @Deprecated
    public static boolean isServer() {
        return SERVER != null;
    }

    public static World getTardisDimension() {
        /*if (isClient()) {
            if (MinecraftClient.getInstance().world != null) {
                if (MinecraftClient.getInstance().world.getRegistryKey() == AITDimensions.TARDIS_DIM_WORLD) {
                    return MinecraftClient.getInstance().world;
                }
            }
        }*/
        return TARDIS_DIMENSION;
    }

    public static AbsoluteBlockPos.Directed createFromPlayer(PlayerEntity player) {
        return new AbsoluteBlockPos.Directed(player.getBlockPos(), player.getWorld(), player.getMovementDirection());
    }

    public static boolean inBox(Box box, BlockPos pos) {
        return pos.getX() <= box.maxX && pos.getX() >= box.minX &&
                pos.getZ() <= box.maxZ && pos.getZ() >= box.minZ;
    }

    public static boolean inBox(Corners corners, BlockPos pos) {
        return inBox(corners.getBox(), pos);
    }

    public static DoorBlockEntity getDoor(Tardis tardis) {
        if (!(TardisUtil.getTardisDimension().getBlockEntity(tardis.getDesktop().getInteriorDoorPos()) instanceof DoorBlockEntity door))
            return null;

        return door;
    }

    public static ExteriorBlockEntity getExterior(Tardis tardis) {
        if (!(tardis.getTravel().getPosition().getBlockEntity() instanceof ExteriorBlockEntity exterior))
            return null;

        return exterior;
    }

    public static Corners findInteriorSpot() {
        BlockPos first = findRandomPlace();

        return new Corners(
                first, first.add(256, 0, 256)
        );
    }

    public static BlockPos findRandomPlace() {
        return new BlockPos(RANDOM.nextInt(100000), 0, RANDOM.nextInt(100000));
    }

    public static BlockPos findBlockInTemplate(StructureTemplate template, BlockPos pos, Direction direction, Block targetBlock) {
        List<StructureTemplate.StructureBlockInfo> list = template.getInfosForBlock(
                pos, new StructurePlacementData().setRotation(
                        TardisUtil.directionToRotation(direction)
                ), targetBlock
        );

        if (list.isEmpty())
            return null;

        return list.get(0).pos();
    }

    public static BlockRotation directionToRotation(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockRotation.CLOCKWISE_180;
            case EAST -> BlockRotation.COUNTERCLOCKWISE_90;
            case WEST -> BlockRotation.CLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    public static BlockPos offsetInteriorDoorPosition(Tardis tardis) {
        return TardisUtil.offsetInteriorDoorPosition(tardis.getDesktop());
    }

    public static BlockPos offsetInteriorDoorPosition(TardisDesktop desktop) {
        return TardisUtil.offsetDoorPosition(desktop.getInteriorDoorPos());
    }

    public static BlockPos offsetExteriorDoorPosition(Tardis tardis) {
        return TardisUtil.offsetExteriorDoorPosition(tardis.getTravel());
    }

    public static BlockPos offsetExteriorDoorPosition(TardisTravel travel) {
        return TardisUtil.offsetExteriorDoorPosition(travel.getPosition());
    }

    public static BlockPos offsetDoorPosition(AbsoluteBlockPos.Directed pos) {
        return switch (pos.getDirection()) {
            case DOWN, UP ->
                    throw new IllegalArgumentException("Cannot adjust door position with direction: " + pos.getDirection());
            case NORTH -> new BlockPos.Mutable(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1);
            case SOUTH -> new BlockPos.Mutable(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1);
            case EAST -> new BlockPos.Mutable(pos.getX() + 1, pos.getY(), pos.getZ() + 0.5);
            case WEST -> new BlockPos.Mutable(pos.getX() - 1, pos.getY(), pos.getZ() + 0.5);
        };
    }

    public static BlockPos offsetExteriorDoorPosition(AbsoluteBlockPos.Directed pos) {
        return switch (pos.getDirection()) {
            case DOWN, UP ->
                    throw new IllegalArgumentException("Cannot adjust door position with direction: " + pos.getDirection());
            case NORTH -> new BlockPos.Mutable(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.0125);
            case SOUTH -> new BlockPos.Mutable(pos.getX() + 0.5, pos.getY(), pos.getZ() - 0.0125);
            case EAST -> new BlockPos.Mutable(pos.getX() + 0.0125, pos.getY(), pos.getZ() + 0.5);
            case WEST -> new BlockPos.Mutable(pos.getX() - 0.0125, pos.getY(), pos.getZ() + 0.5);
        };
    }
    public static void teleportOutside(Tardis tardis, Entity entity) {
        AbsoluteBlockPos.Directed pos = tardis.getTravel().getState() == TardisTravel.State.FLIGHT ? FlightUtil.getPositionFromPercentage(tardis.position(), tardis.destination(), tardis.getHandlers().getFlight().getDurationAsPercentage()) : tardis.position();
        TardisUtil.teleportWithDoorOffset(entity, tardis.getDoor().getExteriorPos());
    }

    public static void teleportInside(Tardis tardis, Entity entity) {
        TardisUtil.teleportWithDoorOffset(entity, tardis.getDoor().getDoorPos());
        TardisDesktop tardisDesktop = tardis.getDesktop();
        if(tardisDesktop.getConsolePos() != null) {
            if(tardisDesktop.getConsolePos().getBlockEntity() instanceof ConsoleBlockEntity console) {
                tardisDesktop.getConsolePos().getBlockEntity().markDirty();
                console.sync(); // maybe force sync when a player enters the tardis
            } else {
                tardisDesktop.setConsolePos(null);
            }
        }
    }

    private static void teleportWithDoorOffset(Entity entity, AbsoluteBlockPos.Directed pos) {
        Vec3d vec = TardisUtil.offsetDoorPosition(pos).toCenterPos();
        SERVER.execute(() -> {
            if(DependencyChecker.hasPortals()) {
                PortalAPI.teleportEntity(entity, (ServerWorld) pos.getWorld(), vec);
            } else {
                if(entity instanceof ServerPlayerEntity player) {
                    WorldOps.teleportToWorld(player, (ServerWorld) pos.getWorld(), vec, pos.getDirection().asRotation(), player.getPitch());
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                } else {
                    if(entity.getWorld().getRegistryKey() == pos.getWorld().getRegistryKey()) {
                        entity.refreshPositionAndAngles(vec.x, vec.y, vec.z, pos.getDirection().asRotation(), entity.getPitch());
                    } else {
                        entity.teleport((ServerWorld) pos.getWorld(), vec.x, vec.y, vec.z, Set.of(), pos.getDirection().asRotation(), entity.getPitch());
                    }
                }
            }
        });
    }

    public static Tardis findTardisByInterior(BlockPos pos, boolean isServer) {
        for (Tardis tardis : TardisManager.getInstance(isServer).getLookup().values()) {
            // System.out.println(pos);
            // System.out.println(tardis.getDesktop().getCorners());
            if (TardisUtil.inBox(tardis.getDesktop().getCorners(), pos))
                return tardis;
        }

        return null;
    }

    public static Tardis findTardisByPosition(AbsoluteBlockPos.Directed pos) {
        Map<UUID, Tardis> matchingTardises = new HashMap<>();

        for (Map.Entry<UUID, ?> entry : TardisManager.getInstance().getLookup().entrySet()) {
            Tardis tardis = (Tardis) entry.getValue();
            if (tardis.getDoor().getExteriorPos().equals(pos)) {
                matchingTardises.put(entry.getKey(), tardis);
            }
        }

        if (matchingTardises.isEmpty()) {
            if (isClient()) {
                ClientTardisManager.getInstance().askTardis(pos);
            }
            return null;
        } else {
            // Return the first Tardis object in the Map
            return matchingTardises.values().iterator().next();
        }
    }

    public static Tardis findTardisByPosition(AbsoluteBlockPos pos) {
        Map<UUID, Tardis> matchingTardises = new HashMap<>();

        for (Map.Entry<UUID, ?> entry : TardisManager.getInstance().getLookup().entrySet()) {
            Tardis tardis = (Tardis) entry.getValue();
            if (tardis.getDoor().getExteriorPos().equals(pos)) {
                matchingTardises.put(entry.getKey(), tardis);
            }
        }

        if (matchingTardises.isEmpty()) {
            if (isClient()) {
                ClientTardisManager.getInstance().askTardis(pos);
            }
            return null;
        } else {
            // Return the first Tardis object in the Map
            return matchingTardises.values().iterator().next();
        }
    }


    public static void giveEffectToInteriorPlayers(Tardis tardis, StatusEffectInstance effect) {
        for (PlayerEntity player : getPlayersInInterior(tardis)) {
            player.addStatusEffect(effect);
        }
    }

    @Nullable
    public static PlayerEntity getPlayerInsideInterior(Tardis tardis) {
        return getPlayerInsideInterior(tardis.getDesktop().getCorners());
    }

    @Nullable
    public static PlayerEntity getPlayerInsideInterior(Corners corners) {
        for (PlayerEntity player : TardisUtil.getTardisDimension().getPlayers()) {
            if (TardisUtil.inBox(corners, player.getBlockPos()))
                return player;
        }
        return null;
    }

    public static List<ServerPlayerEntity> getPlayersInInterior(Tardis tardis) {
        Tardis found;
        List<ServerPlayerEntity> list = new ArrayList<>();

        for (ServerPlayerEntity player : getServer().getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() != getTardisDimension()) continue;

            found = findTardisByInterior(player.getBlockPos(), true);
            if(found == null) continue; // fixme "Cannot invoke "..getUuid()" because "found" is null ????
            if (found.getUuid().equals(tardis.getUuid())) list.add(player);
        }

        return list;
    }

    public static List<LivingEntity> getLivingEntitiesInInterior(Tardis tardis, int area) {
        return getTardisDimension().getEntitiesByClass(LivingEntity.class, new Box(tardis.getDoor().getDoorPos().north(area).east(area).up(area), tardis.getDoor().getDoorPos().south(area).west(area).down(area)), (e) -> true);
    }

    public static List<Entity> getEntitiesInInterior(Tardis tardis, int area) {
        return getTardisDimension().getEntitiesByClass(Entity.class, new Box(tardis.getDoor().getDoorPos().north(area).east(area).up(area), tardis.getDoor().getDoorPos().south(area).west(area).down(area)), (e) -> true);
    }

    public static List<LivingEntity> getLivingEntitiesInInterior(Tardis tardis) {
        return getLivingEntitiesInInterior(tardis, 20);
    }
    public static List<PlayerEntity> getPlayersInInterior(Corners corners) {
        List<PlayerEntity> list = List.of();
        for (PlayerEntity player : TardisUtil.getTardisDimension().getPlayers()) {
            if (inBox(corners, player.getBlockPos())) list.add(player);
        }
        return list;
    }

    public static boolean isInteriorNotEmpty(Tardis tardis) {
        return TardisUtil.getPlayerInsideInterior(tardis) != null;
    }

    public static void sendMessageToPilot(Tardis tardis, Text text) {
        ServerPlayerEntity player = (ServerPlayerEntity) TardisUtil.getPlayerInsideInterior(tardis); // may not necessarily be the person piloting the tardis, but todo this can be replaced with the player with the highest loyalty in future

        if (player == null) return; // Interior is probably empty

        player.sendMessage(text, true);
    }

    public static ServerWorld findWorld(RegistryKey<World> key) {
        return TardisUtil.getTardisDimension().getServer().getWorld(key);
    }

    public static ServerWorld findWorld(Identifier identifier) {
        return TardisUtil.findWorld(RegistryKey.of(RegistryKeys.WORLD, identifier));
    }

    public static ServerWorld findWorld(String identifier) {
        return TardisUtil.findWorld(new Identifier(identifier));
    }

    @Nullable
    public static ExteriorBlockEntity findExteriorEntity(Tardis tardis) {
        if (isClient()) return null;
        return (ExteriorBlockEntity) tardis.getDoor().getExteriorPos().getWorld().getBlockEntity(tardis.getDoor().getExteriorPos());
    }

    public static BlockPos addRandomAmount(PosType type, BlockPos pos, int limit, Random random) {
        return type.add(pos, random.nextInt(limit));
    }

    public static BlockPos getRandomPos(Corners corners, Random random) {
        BlockPos temp;

        temp = addRandomAmount(PosType.X, corners.getFirst(), corners.getSecond().getX() - corners.getFirst().getX(), random);
        temp = addRandomAmount(PosType.Y, temp, 0 + corners.getSecond().getY(), random);
        temp = addRandomAmount(PosType.Z, temp, corners.getSecond().getZ() - corners.getFirst().getZ(), random);

        return temp;
    }

    public static BlockPos getRandomPosInWholeInterior(Tardis tardis, Random random) {
        return getRandomPos(tardis.getDesktop().getCorners(), random);
    }

    public static BlockPos getRandomPosInWholeInterior(Tardis tardis) {
        return getRandomPosInWholeInterior(tardis, new Random());
    }

    public static BlockPos getRandomPosInPlacedInterior(Tardis tardis, Random random) {
        return getRandomPos(getPlacedInteriorCorners(tardis), random);
    }

    public static BlockPos getRandomPosInPlacedInterior(Tardis tardis) {
        return getRandomPosInPlacedInterior(tardis, new Random());
    }

    public static Corners getPlacedInteriorCorners(Tardis tardis) {
        BlockPos centre = BlockPos.ofFloored(tardis.getDesktop().getCorners().getBox().getCenter());
        BlockPos first, second;

        if (!tardis.getDesktop().getSchema().findTemplate().isPresent()) {
            AITMod.LOGGER.warn("Could not get desktop schema! Using whole interior instead.");
            return tardis.getDesktop().getCorners();
        }

        Vec3i size = tardis.getDesktop().getSchema().findTemplate().get().getSize();

        first = PosType.X.add(centre, -size.getX() / 2);
        first = PosType.Z.add(first, -size.getZ() / 2);

        second = PosType.X.add(centre, size.getX() / 2);
        second = PosType.Y.add(second, size.getY());
        second = PosType.Z.add(second, size.getZ() / 2);

        Corners corners = new Corners(first, second);

        return corners;
    }

    public static BlockPos getPlacedInteriorCentre(Tardis tardis) {
        Corners corners = getPlacedInteriorCorners(tardis);

        if (!tardis.getDesktop().getSchema().findTemplate().isPresent()) {
            AITMod.LOGGER.warn("Could not get desktop schema! Returning bad centre instead.");
            return BlockPos.ofFloored(corners.getBox().getCenter());
        }

        Vec3i size = tardis.getDesktop().getSchema().findTemplate().get().getSize();

        return corners.getFirst().add(size.getX(), size.getY() / 2, size.getZ());
    }

    @Nullable
    public static List<PlayerEntity> findPlayerByTardisKey(ServerWorld world, Tardis tardis) {
        List<PlayerEntity> newList = new ArrayList<>();
        for(PlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if(KeyItem.isKeyInInventory(player)) {
                ItemStack key = KeyItem.getFirstKeyStackInInventory(player);
                if(key == null) return null;
                NbtCompound tag = key.getOrCreateNbt();
                if (!tag.contains("tardis")) return null;
                if (UUID.fromString(tag.getString("tardis")) == tardis.getUuid()) {
                    newList.add(player);
                }
            }
        }
        return newList;
    }
}