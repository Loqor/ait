package mdteam.ait.core.blockentities;

import mdteam.ait.AITMod;
import mdteam.ait.core.AITBlockEntityTypes;
import mdteam.ait.core.AITDimensions;
import mdteam.ait.core.AITEntityTypes;
import mdteam.ait.core.blocks.types.HorizontalDirectionalBlock;
import mdteam.ait.core.entities.ConsoleControlEntity;
import mdteam.ait.network.ServerAITNetworkManager;
import mdteam.ait.registry.ConsoleRegistry;
import mdteam.ait.registry.ConsoleVariantRegistry;
import mdteam.ait.tardis.console.ConsoleSchema;
import mdteam.ait.tardis.control.ControlTypes;
import mdteam.ait.tardis.util.TardisUtil;
import mdteam.ait.tardis.util.AbsoluteBlockPos;
import mdteam.ait.tardis.variant.console.ConsoleVariantSchema;
import mdteam.ait.tardis.wrapper.server.manager.ServerTardisManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.TardisDesktop;

import java.util.*;

import static mdteam.ait.tardis.util.TardisUtil.isClient;

public class ConsoleBlockEntity extends BlockEntity implements BlockEntityTicker<ConsoleBlockEntity> {
    public final AnimationState ANIM_FLIGHT = new AnimationState();
    public int animationTimer = 0;
    public final List<ConsoleControlEntity> controlEntities = new ArrayList<>();
    private boolean needsControls = true;
    private boolean needsSync = true;
    private UUID tardisId;
    private Identifier type;
    private Identifier variant;
    private boolean wasPowered = false;
    private boolean needsReloading = true; // this is to ensure we get properly synced when reloaded yup ( does not work for multipalery : (

    public static final Identifier SYNC_TYPE = new Identifier(AITMod.MOD_ID, "sync_console_type");
    public static final Identifier SYNC_VARIANT = new Identifier(AITMod.MOD_ID, "sync_console_variant");
    public static final Identifier ASK = new Identifier(AITMod.MOD_ID, "client_ask_console");

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(AITBlockEntityTypes.CONSOLE_BLOCK_ENTITY_TYPE, pos, state);
        Tardis found = TardisUtil.findTardisByPosition(pos);
        if (found != null)
            this.setTardis(found);
    }
    public UUID getTardisId() {
        return tardisId;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        if (this.getTardis() == null) {
            AITMod.LOGGER.error("this.getTardis() is null! Is " + this + " invalid? BlockPos: " + "(" + this.getPos().toShortString() + ")");
        }

        if (type != null)
            nbt.putString("type", type.toString());
        if (variant != null)
            nbt.putString("variant", variant.toString());

        super.writeNbt(nbt);
        if (this.tardisId != null)
            nbt.putString("tardis", this.tardisId.toString());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("tardis")) {
            this.setTardis(UUID.fromString(nbt.getString("tardis")));
        }

        if (nbt.contains("type"))
            setType(Identifier.tryParse(nbt.getString("type")));
        if (nbt.contains("variant")) {
            setVariant(Identifier.tryParse(nbt.getString("variant")));
        }

        spawnControls();
        markNeedsSyncing();
        markDirty();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = super.toInitialChunkDataNbt();
        if (nbt.contains("type"))
            setType(ConsoleRegistry.REGISTRY.get(Identifier.tryParse(nbt.getString("type"))));
        if (nbt.contains("variant")) {
            setVariant(Identifier.tryParse(nbt.getString("variant")));
        }

        if (type != null)
            nbt.putString("type", type.toString());
        if (variant != null)
            nbt.putString("variant", variant.toString());
        markNeedsControl();
        markNeedsSyncing();
        markDirty();
        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public Tardis getTardis() {
        if (this.tardisId == null) {
            AITMod.LOGGER.warn("Console at " + this.getPos() + " is finding TARDIS!");
            this.findTardis();
        }

        if (isClient()) {
            AITMod.LOGGER.error("Client side tardis should not be accessed!");
            return null;
        }

        return ServerTardisManager.getInstance().getTardis(this.tardisId);
    }

    private void findTardis() {
        this.setTardis(TardisUtil.findTardisByInterior(pos));
        markDirty();
    }

    public void ask() {
        if (!getWorld().isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.getPos());
        ClientPlayNetworking.send(ASK, buf);
    }

    public void sync() {
        if (isClient()) return;

        // ServerTardisManager.getInstance().sendToSubscribers(this.getTardis());
        // getTardis().markDirty();
        syncType();
        syncVariant();
        /*getWorld().updateListeners(getPos(), getWorld().getBlockState(getPos()), getWorld().getBlockState(getPos()), Block.NOTIFY_ALL);*/
        needsSync = false;
    }

    private void syncType() {
        if (!hasWorld() || world.isClient()) return;

        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(getConsoleSchema().id().toString());
        buf.writeBlockPos(getPos());

        for (PlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send((ServerPlayerEntity) player, SYNC_TYPE, buf); // safe cast as we know its server
        }
    }

    private void syncVariant() {
        if (!hasWorld() || world.isClient()) return;

        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(getVariant().id().toString());
        buf.writeBlockPos(getPos());

        for (PlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send((ServerPlayerEntity) player, SYNC_VARIANT, buf); // safe cast as we know its server
        }
    }

    public void setTardis(Tardis tardis) {
        if (tardis == null) {
            AITMod.LOGGER.error("Tardis was null in ConsoleBlockEntity at " + this.getPos());
            return;
        }

        this.tardisId = tardis.getUuid();
        // force re-link a desktop if it's not null
        this.linkDesktop();
    }

    public void setTardis(UUID uuid) {
        this.tardisId = uuid;

        this.linkDesktop();
    }

    public void linkDesktop() {
        if (this.getTardis() == null)
            return;
        if (this.getTardis() != null)
            this.setDesktop(this.getDesktop());
    }

    public TardisDesktop getDesktop() {
        return this.getTardis().getDesktop();
    }

    public ConsoleSchema getConsoleSchema() {
        if (type == null) setType(ConsoleRegistry.HARTNELL);

        return ConsoleRegistry.REGISTRY.get(type);
    }

    public void setType(Identifier var) {
        type = var;

        syncType();
        markDirty();
    }
    public void setType(ConsoleSchema schema) {
        setType(schema.id());
    }


    public ConsoleVariantSchema getVariant() {
        if (variant == null) {
            // oh no : (
            // lets just pick any
            setVariant(ConsoleVariantRegistry.withParent(getConsoleSchema()).stream().findAny().get());
        }

        return ConsoleVariantRegistry.REGISTRY.get(variant);
    }
    public void setVariant(Identifier var) {
        variant = var;

        if (!(getVariant().parent().id().equals(type))) {
            AITMod.LOGGER.warn("Variant was set and it doesnt match this consoles type!");
            AITMod.LOGGER.warn(variant + " | " + type);

            if (hasWorld() && getWorld().isClient()) ask();
        }

        syncVariant();
        markDirty();
    }
    public void setVariant(ConsoleVariantSchema schema) {
        setVariant(schema.id());
    }

    /**
     * Sets the new {@link ConsoleSchema} and refreshes the console entities
     */
    private void changeConsole(ConsoleSchema var) {
        changeConsole(var, ConsoleVariantRegistry.withParent(var).stream().findAny().get());
    }

    private void changeConsole(ConsoleSchema var, ConsoleVariantSchema variant) {
        setType(var);
        setVariant(variant);

        if (!world.isClient() && world == TardisUtil.getTardisDimension())
            redoControls();
    }

    private void redoControls() {
        killControls();
        markNeedsControl();
    }

    public static ConsoleSchema nextConsole(ConsoleSchema current) {
        List<ConsoleSchema> list = ConsoleRegistry.REGISTRY.stream().toList();

        int idx = list.indexOf(current);
        if (idx < 0 || idx+1 == list.size()) return list.get(0);
        return list.get(idx + 1);
    }
    public static ConsoleVariantSchema nextVariant(ConsoleVariantSchema current) {
        List<ConsoleVariantSchema> list = ConsoleVariantRegistry.withParent(current.parent());

        int idx = list.indexOf(current);
        if (idx < 0 || idx+1 == list.size()) return list.get(0);
        return list.get(idx + 1);
    }

    public void useOn(World world, boolean sneaking, PlayerEntity player) {

    }

    @Override
    public void markRemoved() {
        this.killControls();
        super.markRemoved();
    }

    public void setDesktop(TardisDesktop desktop) {
        if (isClient()) return;

        desktop.setConsolePos(new AbsoluteBlockPos.Directed(
                this.pos, TardisUtil.getTardisDimension(), this.getCachedState().get(HorizontalDirectionalBlock.FACING))
        );
    }

    public boolean wasPowered() {
        if(this.getTardis() == null) return false;
        return this.wasPowered ^ this.getTardis().hasPower();
    }

    public void checkAnimations() {
        // DO NOT RUN THIS ON SERVER!!

        animationTimer++;

        ANIM_FLIGHT.startIfNotRunning(animationTimer);
    }


    public void onBroken() {
        this.killControls();
    }

    public void killControls() {
        controlEntities.forEach(Entity::discard);
        controlEntities.clear();
        sync();
    }

    public void spawnControls() {
        BlockPos current = getPos();

        if (!(getWorld() instanceof ServerWorld server))
            return;
        if (getWorld().getRegistryKey() != AITDimensions.TARDIS_DIM_WORLD)
            return;

        killControls();
        ConsoleSchema consoleType = getConsoleSchema();
        ControlTypes[] controls = consoleType.getControlTypes();
        Arrays.stream(controls).toList().forEach(control -> {

            ConsoleControlEntity controlEntity = new ConsoleControlEntity(AITEntityTypes.CONTROL_ENTITY_TYPE, getWorld());

            Vector3f position = current.toCenterPos().toVector3f().add(control.getOffset().x(), control.getOffset().y(), control.getOffset().z());
            controlEntity.setPosition(position.x(), position.y(), position.z());
            controlEntity.setYaw(0.0f);
            controlEntity.setPitch(0.0f);

            controlEntity.setControlData(consoleType, control, this.getPos());

            server.spawnEntity(controlEntity);
            this.controlEntities.add(controlEntity);
        });

        this.needsControls = false;
        //System.out.println("SpawnControls(): I'm getting run :) somewhere..");
    }

    public void markNeedsControl() {
        this.needsControls = true;
        if (TardisUtil.isClient()) return;
        ServerAITNetworkManager.sendTardisConsoleBlockPosToSubscribers(this.getPos(), this.getTardis());
    }
    public void markNeedsSyncing() {
        this.needsSync = true;
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state, ConsoleBlockEntity blockEntity) {
        if (this.needsControls) {
            spawnControls();
        }
        if (needsSync)
            sync();
        if (needsReloading) {
            markNeedsSyncing();
            needsReloading = false;
        }

        /*List<ConsoleControlEntity> entitiesNeedingControl = new ArrayList<>();
        Box entityBox = new Box(pos.north(2).east(2).up(2), pos.south(2).west(2).down(2));
        List<ConsoleControlEntity> entities = TardisUtil.getTardisDimension().getEntitiesByClass(ConsoleControlEntity.class, entityBox, (e) -> true);

        for (ConsoleControlEntity entity : controlEntities) {
            if (entities.isEmpty()) {
                entitiesNeedingControl.add(entity);
            }
        }

        controlEntities.removeAll(entitiesNeedingControl);

        if (!entitiesNeedingControl.isEmpty()) {
            markNeedsControl();
        }*/

        if (world.getRegistryKey() != AITDimensions.TARDIS_DIM_WORLD) {
            this.markRemoved();
        }

        // idk
        if (world.isClient()) {
            this.checkAnimations();
        }
    }

}
