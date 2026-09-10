package dev.amble.ait.core.entities;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import io.netty.handler.codec.EncoderException;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.tardis.ControlAnimationState;
import dev.amble.ait.core.AITBlocks;
import dev.amble.ait.core.AITEntityTypes;
import dev.amble.ait.core.AITItems;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.entities.base.LinkableDummyEntity;
import dev.amble.ait.core.item.RepairToolItem;
import dev.amble.ait.core.item.SonicItem;
import dev.amble.ait.core.item.control.ControlBlockItem;
import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.TardisManager;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.ControlTypes;
import dev.amble.ait.core.tardis.control.impl.HammerHangerControl;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.ait.registry.impl.ControlRegistry;

public class ConsoleControlEntity extends LinkableDummyEntity {
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Vector3f> OFFSET = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Boolean> PART_OF_SEQUENCE = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SEQUENCE_INDEX = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.INTEGER); // <--->
    private static final TrackedData<Integer> SEQUENCE_LENGTH = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> WAS_SEQUENCED = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ON_DELAY = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<BlockPos> CONSOLE_BLOCK_POS = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.BLOCK_POS);
    private static final TrackedData<String> CONTROL_ID = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.STRING);
    /**
     * This control's slot in its console's {@code getControlTypes()}. -1 until set, because 0 is a
     * real slot and an unset index must not drive another control's animation.
     */
    private static final TrackedData<Integer> CONTROL_INDEX = DataTracker.registerData(ConsoleControlEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    public static final float MAX_DURABILITY = 1.0f;

    /** Whether the last cooldown pushed to the console was set, so release can be pushed too. */
    private boolean pushedOnDelay;
    private Control control;
    private ControlTypes controlType;

    public ConsoleControlEntity(EntityType<? extends Entity> entityType, World world) {
        super(entityType, world);
    }

    private ConsoleControlEntity(World world, Tardis tardis) {
        this(AITEntityTypes.CONTROL_ENTITY_TYPE, world);
        this.link(tardis);
    }

    public static ConsoleControlEntity create(World world, Tardis tardis) {
        return new ConsoleControlEntity(world, tardis);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();

        // No unset case to guard: CONSOLE_BLOCK_POS defaults to BlockPos.ORIGIN and is never
        // null, so a control with no console simply finds no block entity there. The old guard
        // meant super was never reached on the live path.
        if (this.getWorld().getBlockEntity(this.getConsoleBlockPos()) instanceof ConsoleBlockEntity console)
            console.markNeedsControl();
    }

    @Override
    public void initDataTracker() {
        super.initDataTracker();

        this.dataTracker.startTracking(WIDTH, 0.125f);
        this.dataTracker.startTracking(HEIGHT, 0.125f);
        this.dataTracker.startTracking(OFFSET, new Vector3f(0));
        this.dataTracker.startTracking(PART_OF_SEQUENCE, false);
        this.dataTracker.startTracking(SEQUENCE_INDEX, 0);
        this.dataTracker.startTracking(SEQUENCE_LENGTH, 0);
        this.dataTracker.startTracking(WAS_SEQUENCED, false);
        this.dataTracker.startTracking(ON_DELAY, false);
        this.dataTracker.startTracking(CONSOLE_BLOCK_POS, BlockPos.ORIGIN);
        this.dataTracker.startTracking(CONTROL_ID, "");
        this.dataTracker.startTracking(CONTROL_INDEX, -1);
    }

	@Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        nbt.put("console", NbtHelper.fromBlockPos(this.getConsoleBlockPos()));

        nbt.putFloat("width", this.getControlWidth());
        nbt.putFloat("height", this.getControlHeight());
        nbt.putFloat("offsetX", this.getOffset().x());
        nbt.putFloat("offsetY", this.getOffset().y());
        nbt.putFloat("offsetZ", this.getOffset().z());
        nbt.putBoolean("partOfSequence", this.isPartOfSequence());
        nbt.putInt("sequenceColor", this.getSequenceIndex());
        nbt.putBoolean("wasSequenced", this.wasSequenced());
        nbt.putFloat("durability", this.getDurability());
        nbt.putBoolean("sticky", this.isSticky());
        nbt.putInt("controlIndex", this.getControlIndex());

	    // write control type via codec
	    if (this.controlType == null) {
		    AITMod.LOGGER.error("Control type is null for control entity at {}", this.getPos());
		    return;
	    }
	    DataResult<NbtElement> result = ControlTypes.CODEC.encodeStart(NbtOps.INSTANCE, this.controlType);
	    result.resultOrPartial(AITMod.LOGGER::error).ifPresent(nbtResult -> nbt.put("controlType", nbtResult));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        NbtCompound console = nbt.getCompound("console");

        if (nbt.contains("console")) {
            this.setConsolePos(NbtHelper.toBlockPos(console));
        }

        if (nbt.contains("width") && nbt.contains("height")) {
            this.setControlWidth(nbt.getFloat("width"));
            this.setControlHeight(nbt.getFloat("height"));
            this.calculateDimensions();
        }

        if (nbt.contains("offsetX") && nbt.contains("offsetY") && nbt.contains("offsetZ"))
            this.setOffset(new Vector3f(nbt.getFloat("offsetX"), nbt.getFloat("offsetY"), nbt.getFloat("offsetZ")));

        if (nbt.contains("partOfSequence"))
            this.setPartOfSequence(nbt.getBoolean("partOfSequence"));

        if (nbt.contains("sequenceColor"))
            this.setSequenceIndex(nbt.getInt("sequenceColor"));

        if (nbt.contains("wasSequenced"))
            this.setWasSequenced(nbt.getBoolean("wasSequenced"));

        if (nbt.contains("controlIndex"))
            this.setControlIndex(nbt.getInt("controlIndex"));

        if (nbt.contains("durability"))
            this.setDurability(nbt.getFloat("durability"));
        if (nbt.contains("sticky"))
            this.setSticky(nbt.getBoolean("sticky"));

	    if (nbt.contains("controlType")) {
		    DataResult<ControlTypes> result = ControlTypes.CODEC.parse(NbtOps.INSTANCE, nbt.get("controlType"));
		    result.resultOrPartial(AITMod.LOGGER::error).ifPresent(controlType -> {
			    this.controlType = controlType;
			    this.control = controlType.getControl();
			    this.dataTracker.set(CONTROL_ID, this.control.id().toString());

		    });
	    }
    }

    public void setConsolePos(BlockPos consoleBlockPos) {
        this.dataTracker.set(CONSOLE_BLOCK_POS, consoleBlockPos);
    }

    @Override
    public void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> dataEntries) {
        this.setScaleAndCalculate(this.getDataTracker().get(WIDTH), this.getDataTracker().get(HEIGHT));
    }

    @Override
    public boolean canHit() {
        return !isRemoved();
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack handStack = player.getStackInHand(hand);

        if (handStack.isOf(AITItems.REPAIR_TOOL) && this.getDurability() < MAX_DURABILITY) {
            return ActionResult.PASS;
        }

        if (player.getOffHandStack().isOf(Items.COMMAND_BLOCK)) {
            controlEditorHandler(player);
            return ActionResult.SUCCESS;
        }

        if (handStack.isOf(AITBlocks.REDSTONE_CONTROL_BLOCK.asItem()) && this.getControl() != null) {
            NbtCompound nbt = handStack.getOrCreateNbt();
            nbt.putString(ControlBlockItem.CONTROL_ID_KEY, this.getControl().id().toString());
            ConsoleBlockEntity consoleBlockEntity = this.getConsole();
            nbt.putString(ControlBlockItem.CONSOLE_TYPE_ID_KEY, consoleBlockEntity.getTypeSchema().id().toString());
            return ActionResult.SUCCESS;
        }

        if (hand == Hand.MAIN_HAND && !this.run(player, player.getWorld(), false))
            this.playFailFx();

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getSource() instanceof TntEntity)
            return false;

        if (source.getAttacker() instanceof PlayerEntity player) {
            if (source.getSource() instanceof ProjectileEntity)
                source.getSource().discard();

            if (player.getOffHandStack().isOf(Items.COMMAND_BLOCK))
                controlEditorHandler(player);
            else if (!this.run(player, player.getWorld(), true))
                this.playFailFx();
        }

        return false;
    }

    private void playFailFx() {
        if (this.getWorld().isClient())
            return;

        ServerWorld world = (ServerWorld) this.getWorld();

        // spawn particle above the control - change from the ait particle to crit - Loqor
        world.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.25, this.getZ(), 1, 0.05, 0.05, 0.05, 0.025);

        // Changed from that horrible, ear-grating, nausea-inducing SHIELD BREAK to a much softer sound - Loqor
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.5F, AITMod.RANDOM.nextFloat(0.5F, 1F));
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        if (this.getDataTracker().containsKey(WIDTH) && this.getDataTracker().containsKey(HEIGHT))
            return EntityDimensions.changing(this.getControlWidth(), this.getControlHeight());

        return super.getDimensions(pose);
    }

    @Override
    public void tick() {
	    if (this.getWorld().isClient()) {
		    this.pushCooldown();
            return;
	    }

        if (this.control == null && this.getConsoleBlockPos() != null)
            this.discard();

        switch (this.getDurabilityState(this.getDurability())) {
            case SPARKY -> this.spark();
            case OCCASIONALY_JAM -> this.smoke();
            case CATCH_FIRE -> this.onFire();
            // Don't have the COMPLETELY BROKEN one show anything, this is the player's fault
            default -> {}
        }
    }

    @Override
    public boolean shouldRenderName() {
        return true;
    }

    public float getControlWidth() {
        return this.dataTracker.get(WIDTH);
    }

    public float getControlHeight() {
        return this.dataTracker.get(HEIGHT);
    }

    public void setControlWidth(float width) {
        this.dataTracker.set(WIDTH, width);
    }

    public void setControlHeight(float height) {
        this.dataTracker.set(HEIGHT, height);
    }

    public Control getControl() {
        // The control field is only assigned server-side (see setControlData), so on the client we
        // resolve it lazily from the synced id.
        if (this.control == null) {
            String id = this.dataTracker.get(CONTROL_ID);

            if (!id.isEmpty())
                this.control = ControlRegistry.REGISTRY.get(Identifier.tryParse(id));
        }

        return control;
    }
    public Vector3f getOffset() {
        return this.dataTracker.get(OFFSET);
    }

    public void setOffset(Vector3f offset) {
        this.dataTracker.set(OFFSET, offset);
    }

    public int getSequenceIndex() {
        return this.dataTracker.get(SEQUENCE_INDEX);
    }

    public void setSequenceIndex(int i) {
        this.dataTracker.set(SEQUENCE_INDEX, i);
    }

    public int getSequenceLength() {
        return this.dataTracker.get(SEQUENCE_LENGTH);
    }

    public void setSequenceLength(int n) {
        this.dataTracker.set(SEQUENCE_LENGTH, n);
    }

    public float getSequencePercentage() {
        return (this.getSequenceIndex() + 1f) / this.getSequenceLength();
    }

    public boolean wasSequenced() {
        return this.dataTracker.get(WAS_SEQUENCED);
    }

    public void setWasSequenced(boolean sequenced) {
        this.dataTracker.set(WAS_SEQUENCED, sequenced);
    }

    public void setPartOfSequence(boolean partOfSequence) {
        this.dataTracker.set(PART_OF_SEQUENCE, partOfSequence);
    }

    public boolean isPartOfSequence() {
        return this.dataTracker.get(PART_OF_SEQUENCE);
    }

    public boolean isOnDelay() {
        return this.dataTracker.get(ON_DELAY);
    }

    public float getDurability() {
        if (this.getControl() == null || this.getConsole() == null) return MAX_DURABILITY;
        return this.getConsole().controlStateMap.getOrDefault(this.getControl(), new Control.ControlState()).damage();//this.dataTracker.get(DURABILITY);
    }

    public boolean isSticky() {
        if (this.getControl() == null || this.getConsole() == null) return false;
        return this.getConsole().controlStateMap.getOrDefault(this.getControl(), new Control.ControlState()).sticky();//this.dataTracker.get(DURABILITY);
    }

    public DurabilityStates getDurabilityState(float durability) {
        return DurabilityStates.get(durability);
    }

    public void setDurability(float durability) {
        ConsoleBlockEntity console = this.getConsole();
        if (console != null && this.getControl() != null) {
            console.updateDurability(this.getControl(), durability);
            console.markDirty();
        }
    }

    public void setSticky(boolean sticky) {
        ConsoleBlockEntity console = this.getConsole();
        if (console != null && this.getControl() != null) {
            console.updateStickiness(this.getControl(), sticky);
            console.markDirty();
        }
    }

    public void addDurability(float durability) {
        this.setDurability(Math.min(this.getDurability() + durability, MAX_DURABILITY));
    }

    public void subtractDurability(float durability) {
        this.setDurability(Math.max(this.getDurability() - durability, 0));
    }

    public boolean run(PlayerEntity player, World world, boolean leftClick) {
        if (isSticky()) {
            if (player.getMainHandStack().isOf(Items.SHEARS)) {
                this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1, 1);
                this.setSticky(false);
                return true;
            }

            this.playSound(SoundEvents.BLOCK_SLIME_BLOCK_BREAK, 0.4f, 1);
            player.getWorld().addParticle(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.getDefaultState()),
                    this.getX(), this.getY(), this.getZ(),
                    0.2, 0.5, -0.1
            );

            return true;
        } else if (player.getMainHandStack().isOf(Items.SLIME_BALL)) {
            this.playSound(SoundEvents.BLOCK_SLIME_BLOCK_PLACE, 1, 1);
            this.setSticky(true);
            return true;
        }

        if (world.isClient())
            return false;

        if (player.getMainHandStack().isOf(AITItems.TARDIS_ITEM))
            this.discard();

        if (!this.isLinked()) {
            AITMod.LOGGER.warn("Discarding invalid control entity at {}; console pos: {}", this.getPos(),
                    this.getConsoleBlockPos());

            this.discard();
            return false;
        }

        Tardis tardis = this.tardis().get();

        ItemStack stack = player.getMainHandStack();
        if (tardis.travel().isLanded() && ((stack.isOf(AITItems.SONIC_SCREWDRIVER) && SonicItem.mode(stack) == SonicMode.Modes.TARDIS) || stack.getItem() instanceof RepairToolItem) && this.getDurability() < 1.0f) {
            Vec3d pos = this.getPos();
            this.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            ((ServerWorld) this.getEntityWorld()).spawnParticles(ParticleTypes.WAX_ON,
                    pos.getX(), pos.getY(), pos.getZ(), 2, 0.2, 0.4, 0.2, 0.02);
            this.addDurability(0.1f);
            return true;
        }

        control.runAnimation(tardis, (ServerPlayerEntity) player, (ServerWorld) world);

        DurabilityStates state = this.getDurabilityState(this.getDurability());
        boolean hasMallet = player.getMainHandStack().isOf(AITItems.HAMMER);

        if (state == DurabilityStates.CATCH_FIRE && !hasMallet) {
            player.setFireTicks(random.nextBetween(20*2, 20*6));
            player.damage(world.getDamageSources().hotFloor(), 4);
        }

        if (this.isOnDelay())
            return false;

        if (!this.control.canRun(tardis, (ServerPlayerEntity) player))
            return false;

        if (world.getRandom().nextBetween(1, 10_000) == 72)
            this.getWorld().playSound(null, this.getBlockPos(), AITSounds.EVEN_MORE_SECRET_MUSIC, SoundCategory.MASTER,
                    1F, 1F);

        if (hasMallet) {
            this.playSound(AITSounds.KNOCK, 1, 0.25f);
            Vec3d pos = this.getPos();
            ((ServerWorld) world).spawnParticles(ParticleTypes.SCRAPE,
                    pos.getX(), pos.getY(), pos.getZ(), 2, 0.2, 0.4, 0.2, 0.02);

            if (!(this.getControl() instanceof HammerHangerControl)) {
                this.subtractDurability(random.nextBetween(1, 4) / 25f * this.getDurability());
            }
        }

        if (state == DurabilityStates.JAMMED) {
            if (hasMallet && random.nextBetween(0, 5) < 2)
                tardis.subsystems().engine().removeDurability(random.nextBetween(0, 7));

            return false;
        }

        if (state == DurabilityStates.SPARKY && random.nextBetween(0, 20) < 10) {
            if (hasMallet) {
                this.setDurability(state.next().durability);
            } else {
                return false;
            }
        }

        if (state == DurabilityStates.OCCASIONALY_JAM && random.nextBetween(0, 25) < 5) {
            if (!hasMallet) return false;
        }

        if (this.control.shouldHaveDelay(tardis) && !this.isOnDelay()) {
            this.dataTracker.set(ON_DELAY, true);

            Scheduler.get().runTaskLater(() -> this.dataTracker.set(ON_DELAY, false),
                    TaskStage.END_SERVER_TICK, TimeUnit.TICKS, this.control.getDelayLength(tardis));
        }

        Control.Result result = this.control.handleRun(tardis, (ServerPlayerEntity) player, (ServerWorld) world, this.getConsoleBlockPos(), leftClick);

        if (result == Control.Result.SEQUENCE) {
            if (random.nextBetween(0, 8) == 5) {
                // Clamping the random variance to avoid extreme swings
                float variance = (float) random.nextGaussian() * 0.15f + 0.5f;
                this.subtractDurability(MathHelper.clamp(variance, 0.1f, 0.9f));
            }
        }

        ConsoleBlockEntity console = this.getConsole();
        if (console != null) {
            this.getWorld().playSound(null, this.getBlockPos(), this.control.getSound(console.getTypeSchema(), result), SoundCategory.BLOCKS, 0.7f,
                    1f);
        }

        return result.isSuccess();
    }

    /**
     * Reports this control's cooldown to its console, which is what the renderer reads.
     *
     * <p>The console owns the animation state; this entity owns nothing but the cooldown flag,
     * which is server authoritative and reaches the client only as tracked data. Pushing rather
     * than being read means no block entity reference is held across ticks, so a console that
     * goes away without warning cannot be observed stale.
     *
     * <p>Pushes while on cooldown and once more on release, then goes quiet. A control with
     * nothing to report costs nothing, so a console with thirty controls does not pay a block
     * entity lookup per control per tick; and because release is pushed rather than inferred, a
     * cooldown ends when it ends instead of when the console stops trusting the last push.
     */
    private void pushCooldown() {
        boolean onDelay = this.isOnDelay();

        if (!onDelay && !this.pushedOnDelay)
            return;

        int index = this.getControlIndex();

        if (index < 0)
            return;

        // Deliberately not getConsole(): that warns when the block entity has not loaded yet,
        // which is the normal case for the first few ticks after a control is synced.
        if (!(this.getWorld().getBlockEntity(this.getConsoleBlockPos()) instanceof ConsoleBlockEntity console)) {
            // Nothing to deliver a release to. Forget it rather than retrying every tick: the
            // console expires an unrenewed cooldown on its own, and a console that comes back
            // comes back with fresh state anyway.
            this.pushedOnDelay = false;
            return;
        }

        ControlAnimationState state = console.controlAnimation(index);

        if (state == null) {
            // Not a slot on this console any more, so this control has nothing to drive.
            this.pushedOnDelay = false;
            return;
        }

        state.push(onDelay, console.getAge());
        this.pushedOnDelay = onDelay;
    }

    public ConsoleBlockEntity getConsole() {
        if (this.getConsoleBlockPos() == null)
            return null;

        BlockEntity blockEntity = this.getWorld().getBlockEntity(this.getConsoleBlockPos());
        if (blockEntity instanceof ConsoleBlockEntity console)
            return console;

        AITMod.LOGGER.warn("Control entity at {} has no console block entity at {}", this.getPos(), this.getConsoleBlockPos());
        return null;
    }

    private void spark() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        Vec3d pos = this.getPos();
        if (serverWorld.getServer().getTicks() % 15 == 0 && random.nextBoolean()) {
            this.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 0.1f, random.nextBoolean() ? 1f : 2f);
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX(), pos.getY(), pos.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
            serverWorld.spawnParticles(ParticleTypes.LAVA, pos.getX(), pos.getY(), pos.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void smoke() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        Vec3d pos = this.getPos();
        if (serverWorld.getServer().getTicks() % 10 == 0)
            serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0.0f);
    }

    private void onFire() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        Vec3d pos = this.getPos();
        serverWorld.spawnParticles(ParticleTypes.SMOKE, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0.0f);
        if (serverWorld.getServer().getTicks() % 10 == 0)
            serverWorld.spawnParticles(ParticleTypes.SMALL_FLAME, pos.getX(), pos.getY() + 0.2f, pos.getZ(), 1, 0, 0.075f, 0, 0);
    }

    public int getControlIndex() {
        return this.dataTracker.get(CONTROL_INDEX);
    }

    public void setControlIndex(int index) {
        this.dataTracker.set(CONTROL_INDEX, index);
    }

    public BlockPos getConsoleBlockPos() {
        return this.dataTracker.get(CONSOLE_BLOCK_POS);
    }

    public void setScaleAndCalculate(float width, float height) {
        this.setControlWidth(width);
        this.setControlHeight(height);
        this.calculateDimensions();
    }

    public void setControlData(ConsoleTypeSchema consoleType, ControlTypes type, BlockPos consoleBlockPosition, float durability, boolean sticky) {
        this.setConsolePos(consoleBlockPosition);
        this.control = type.getControl();
        this.controlType = type;
        this.dataTracker.set(CONTROL_ID, this.control.id().toString());

        super.setCustomName(this.control.getName(this.tardis().get()));

        if (consoleType != null) {
            this.setControlWidth(type.getScale().width);
            this.setControlHeight(type.getScale().height);
            this.setOffset(type.getOffset());
            this.setDurability(durability);
            this.setSticky(sticky);
        }
    }

    public void logConsoleJson() {
        // convert all controls into json
        JsonArray root = new JsonArray();

        for (ConsoleControlEntity entity : this.getConsole().controlEntities) {
            if (entity == null) continue;

            ControlTypes type = new ControlTypes(entity.getControl(), entity.getDimensions(EntityPose.STANDING), entity.getOffset());
            DataResult<JsonElement> dataResult = ControlTypes.CODEC.encodeStart(JsonOps.INSTANCE, type);
            JsonElement typeData = Util.getResult(dataResult, error -> new EncoderException("Failed to encode: " + error + " " + type));

            root.add(typeData);
        }

        AITMod.LOGGER.info(TardisManager.getInstance(this).getFileGson().toJson(root));
    }

    public void controlEditorHandler(PlayerEntity player) {
        if (!player.isCreativeLevelTwoOp()) return;

        float increment = 0.0125f;

        if (player.getMainHandStack().getItem() == Items.PAPER && !player.getWorld().isClient()) {
            logConsoleJson();

            player.sendMessage(Text.translatable("message.ait.console_control.json_logged"));

            return;
        }

        if (player.getMainHandStack().getItem() == Items.EMERALD_BLOCK)
            this.setPosition(this.getPos().add(player.isSneaking() ? -increment : increment, 0, 0));

        if (player.getMainHandStack().getItem() == Items.DIAMOND_BLOCK)
            this.setPosition(this.getPos().add(0, player.isSneaking() ? -increment : increment, 0));

        if (player.getMainHandStack().getItem() == Items.REDSTONE_BLOCK)
            this.setPosition(this.getPos().add(0, 0, player.isSneaking() ? -increment : increment));

        if (player.getMainHandStack().getItem() == Items.COD)
            this.setScaleAndCalculate(player.isSneaking()
                    ? this.getDataTracker().get(WIDTH) - increment
                    : this.getDataTracker().get(WIDTH) + increment, this.getDataTracker().get(HEIGHT));

        if (player.getMainHandStack().getItem() == Items.COOKED_COD)
            this.setScaleAndCalculate(this.getDataTracker().get(WIDTH),
                    player.isSneaking()
                            ? this.getDataTracker().get(HEIGHT) - increment
                            : this.getDataTracker().get(HEIGHT) + increment);

        if (this.getConsoleBlockPos() != null) {
            Vec3d centered = this.getPos().subtract(this.getConsoleBlockPos().toCenterPos());
            this.setOffset(centered.toVector3f());
            if (this.control != null)
                player.sendMessage(Text.literal("EntityDimensions.changing(" + this.getControlWidth() + "f, "
                        + this.getControlHeight() + "f), new Vector3f(" + centered.getX() + "f, " + centered.getY()
                        + "f, " + centered.getZ() + "f)),"));
        }
    }

    @Override
    public void setCustomName(@Nullable Text name) {}

    public enum DurabilityStates {
        JAMMED(0.0f),
        CATCH_FIRE(0.25f),
        OCCASIONALY_JAM(0.5f),
        SPARKY(0.75f),
        FULL(ConsoleControlEntity.MAX_DURABILITY);
        public final float durability;
        DurabilityStates(float durabilityLevel) {
            this.durability = durabilityLevel;
        }

        public static DurabilityStates get(String id) {
            return DurabilityStates.valueOf(id.toUpperCase());
        }

        public static DurabilityStates get(float level) {
            level = DurabilityStates.normalize(level);

            for (int i = 0; i < values().length - 1; i++) {
                DurabilityStates current = values()[i];
                DurabilityStates next = values()[i + 1];

                if (current.durability <= level && level < next.durability)
                    return current;
            }

            return DurabilityStates.FULL;
        }

        public static float normalize(float durability) {
            return Math.min(Math.max(durability, DurabilityStates.JAMMED.durability), DurabilityStates.FULL.durability);
        }

        public DurabilityStates next() {
            return switch (this) {
                case JAMMED -> CATCH_FIRE;
                case CATCH_FIRE -> OCCASIONALY_JAM;
                case OCCASIONALY_JAM -> SPARKY;
                case SPARKY -> FULL;
                case FULL -> JAMMED;
            };
        }
    }
}
