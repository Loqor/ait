package dev.amble.ait.core.blockentities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.amble.lib.animation.EffectProvider;
import org.jetbrains.annotations.Nullable;
import dev.amble.ait.AITMod;
import dev.amble.ait.client.tardis.ControlAnimationState;
import dev.amble.ait.api.ArtronHolderItem;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.core.AITBlockEntityTypes;
import dev.amble.ait.core.AITBlocks;
import dev.amble.ait.core.entities.ConsoleControlEntity;
import dev.amble.ait.core.item.ChargedZeitonCrystalItem;
import dev.amble.ait.core.item.SonicItem;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.TardisDesktop;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.ControlTypes;
import dev.amble.ait.core.tardis.control.sequences.SequenceHandler;
import dev.amble.ait.core.tardis.handler.FuelHandler;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import dev.amble.ait.core.world.RiftChunkManager;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.ait.data.schema.console.ConsoleVariantSchema;
import dev.amble.ait.registry.impl.ControlRegistry;
import dev.amble.ait.registry.impl.console.ConsoleRegistry;
import dev.amble.ait.registry.impl.console.variant.ConsoleVariantRegistry;
import dev.amble.lib.util.ServerLifecycleHooks;

public class ConsoleBlockEntity extends AbstractConsoleBlockEntity
        implements BlockEntityTicker<ConsoleBlockEntity>, ArtronHolderItem, EffectProvider {

    private ItemStack sonicScrewdriver = ItemStack.EMPTY;
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(54, ItemStack.EMPTY);

    public final List<ConsoleControlEntity> controlEntities = new ArrayList<>();
    public final Map<Control, Control.ControlState> controlStateMap = new HashMap<>();
    public final AnimationState ANIM_STATE = new AnimationState();

    private boolean needsControls = true;

    private ConsoleTypeSchema type;
    private ConsoleVariantSchema variant;

    /**
     * Client side animation state, one slot per entry of {@link #controlTypes()}. Null server
     * side, where nothing animates.
     */
    private ControlAnimationState[] controlAnimations;

    /**
     * The type schema's controls, held because the renderer reads them every frame and
     * {@link dev.amble.ait.data.datapack.DatapackConsole.SimpleType#getControlTypes()} builds a
     * fresh array on each call. Datapack consoles are exactly the ones on that path.
     */
    private ControlTypes[] controlTypes;

    public int age;

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(AITBlockEntityTypes.CONSOLE_BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    public void onLinked() {
        if (this.getWorld() == null || !TardisServerWorld.isTardisDimension(this.getWorld())) return;
        if (this.tardis().isEmpty())
            return;

        Tardis tardis = this.tardis().get();

        if (tardis instanceof ClientTardis)
            return;

        tardis.getDesktop().getConsolePos().add(this.pos);
        tardis.asServer().markDirty(tardis.getDesktop());
        this.markNeedsControl();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        nbt.putString("type", this.getTypeSchema().id().toString());
        nbt.putString("variant", this.getVariant().id().toString());
        Inventories.writeNbt(nbt, this.inventory);
        if (this.sonicScrewdriver != null) {
            nbt.put("sonic_screwdriver", this.sonicScrewdriver.writeNbt(new NbtCompound()));
        }

        NbtCompound controlNbt = new NbtCompound();

        this.controlStateMap.forEach((control, state) -> {
            controlNbt.put(control.id().toString(), state.writeNbt());
        });

        nbt.put("ControlStates", controlNbt);
    }

    public void updateDurability(Control control, float durability) {
        if (control == null) return;
        if (durability >= ConsoleControlEntity.MAX_DURABILITY) {
            Control.ControlState state = this.controlStateMap.get(control);
            if (state != null && state.sticky()) {
                state.setDamage(durability);
            } else {
                this.controlStateMap.remove(control);
            }
            this.markDirty();
            return;
        }

        this.getOrCreateState(control).setDamage(durability);
        this.markDirty();
    }

    public void updateStickiness(Control control, boolean sticky) {
        if (control == null) return;

        if (!sticky) {
            Control.ControlState state = this.controlStateMap.get(control);
            if (state == null) {
                return;
            }
            state.setSticky(false);
            if (state.damage() >= ConsoleControlEntity.MAX_DURABILITY) {
                this.controlStateMap.remove(control);
            }
            this.markDirty();
            return;
        }

        this.getOrCreateState(control).setSticky(true);
        this.markDirty();
    }

    @Override
    protected Text getContainerName() {
        return tardis().isPresent()
                ? Text.literal(tardis().get().stats().getName())
                : Text.translatable("ait.console.inventory");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, this);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        ConsoleTypeSchema schema = ConsoleRegistry.getInstance().get(Identifier.tryParse(nbt.getString("type")));

        // Registry lookups are nullable. Assigning the miss would re-null the type on every
        // resend, so the fallback in getTypeSchema would warn again and clear the control caches
        // twice each time, snapping every lever animation.
        if (schema != null)
            this.setType(schema);

        this.setVariant(ConsoleVariantRegistry.getInstance().get(Identifier.tryParse(nbt.getString("variant"))));

        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
        if (nbt.contains("sonic_screwdriver")) {
            this.sonicScrewdriver = ItemStack.fromNbt(nbt.getCompound("sonic_screwdriver"));
        }

        if (!nbt.contains("ControlStates", NbtCompound.COMPOUND_TYPE)) return;

        NbtCompound controlStatesNbt = nbt.getCompound("ControlStates");

        for (String key : controlStatesNbt.getKeys()) {
            Identifier id = Identifier.tryParse(key);

            if (id == null) continue;

            Control control = ControlRegistry.REGISTRY.get(id);

            if (control == null) continue;

            if (!controlStatesNbt.contains(id.toString(), NbtCompound.COMPOUND_TYPE)) return;

            NbtCompound compound1 = controlStatesNbt.getCompound(id.toString());
            Control.ControlState existingState = controlStateMap.get(control);

            if (existingState != null) {
                existingState.readNbt(compound1);
            } else {
                controlStateMap.put(control, new Control.ControlState().readNbt(compound1));
            }
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        if (world.getRegistryKey().equals(World.OVERWORLD)) {
            return super.toInitialChunkDataNbt();
        }
        this.markNeedsControl();
        return super.toInitialChunkDataNbt();
    }

    public ConsoleTypeSchema getTypeSchema() {
        if (type == null) {
            // Worth saying: control animation is driven off this schema, and no hardcoded type
            // carries animation references, so a console that lands here renders its controls
            // completely static. Logs once, because setType fills the field in.
            AITMod.LOGGER.warn("Console at {} has no type schema, falling back to Hartnell; its controls will not animate", this.pos);
            this.setType(ConsoleRegistry.HARTNELL);
        }

        return type;
    }

    public void setType(ConsoleTypeSchema schema) {
        // Guarded because readNbt calls this on every full console resend, and a resend happens
        // for unrelated things like inserting a sonic. Clearing unconditionally would snap and
        // replay every lever animation on the console.
        //
        // Compared by identity rather than equals: schema equality is id based, and a datapack
        // reload hands out a new schema object with a different control list under the same id.
        // The registry returns one instance per id otherwise, so a plain resend still matches.
        if (schema != this.type) {
            this.controlTypes = null;
            this.controlAnimations = null;
        }

        this.type = schema;
        this.markDirty();
    }

    public void setVariant(ConsoleVariantSchema schema) {
        this.variant = schema;
        this.markDirty();
    }

    public ConsoleVariantSchema getVariant() {
        if (variant == null)
            this.setVariant(this.getTypeSchema().getDefaultVariant());

        return variant;
    }

    public int getAge() {
        return age;
    }

    /** The type schema's controls. Do not mutate: this is the schema's own array for most types. */
    public ControlTypes[] controlTypes() {
        if (this.controlTypes == null)
            this.controlTypes = this.getTypeSchema().getControlTypes();

        return this.controlTypes;
    }

    /**
     * Animation state for one control slot, or null if the index is not a slot on this console.
     *
     * <p>Out of range is reachable: a control entity carries the index it was spawned with, and
     * its console can already be holding a different type schema.
     */
    public @Nullable ControlAnimationState controlAnimation(int index) {
        ControlTypes[] types = this.controlTypes();

        if (index < 0 || index >= types.length)
            return null;

        if (this.controlAnimations == null || this.controlAnimations.length != types.length)
            this.controlAnimations = new ControlAnimationState[types.length];

        ControlAnimationState state = this.controlAnimations[index];

        if (state == null)
            this.controlAnimations[index] = state = new ControlAnimationState();

        return state;
    }

    public void useOn(World world, boolean sneaking, PlayerEntity player) {
        if (world.isClient())
            return;

        if (this.tardis().isEmpty())
            return;

        ItemStack itemStack = player.getMainHandStack();
        if (itemStack.getItem() == AITBlocks.ZEITON_CLUSTER.asItem()) {
            this.tardis().get().addFuel(15);

            if (!player.isCreative())
                itemStack.decrement(1);

            return;
        }

        if (itemStack.getItem() instanceof ChargedZeitonCrystalItem) {
            NbtCompound nbt = itemStack.getOrCreateNbt();

            if (!nbt.contains(ChargedZeitonCrystalItem.FUEL_KEY))
                return;

            this.tardis().get().addFuel(nbt.getDouble(ChargedZeitonCrystalItem.FUEL_KEY));
            nbt.putDouble(ChargedZeitonCrystalItem.FUEL_KEY, 0);
        }
    }

    @Override
    public void markRemoved() {
        this.killControls();
        super.markRemoved();
    }

    public void onBroken() {
        this.killControls();
        if (this.tardis().isEmpty())
            return;

        Tardis tardis = this.tardis().get();
        TardisDesktop desktop = tardis.getDesktop();

        desktop.getConsolePos().remove(this.pos);
        tardis.asServer().markDirty(desktop);
    }

    public void killControls() {
        // Server authoritative: it writes control state back and discards the entities. It reaches
        // the client through markRemoved, where the list is always empty, so keep it a no-op rather
        // than leaving a path that could discard client side control entities.
        if (this.world == null || this.world.isClient())
            return;

        for (ConsoleControlEntity entity : controlEntities) {
            Control control = entity.getControl();
            if (control != null) {
                this.updateStateFromEntity(control, entity.getDurability(), entity.isSticky());
            }
        }

        controlEntities.forEach(Entity::discard);
        controlEntities.clear();
        this.markDirty();
    }

    public void spawnControls() {
        BlockPos current = this.getPos();

        if (!(this.world instanceof ServerWorld serverWorld))
            return;

        if (!TardisServerWorld.isTardisDimension((ServerWorld) this.getWorld()))
            return;

        this.killControls();
        ConsoleTypeSchema consoleType = this.getTypeSchema();
        ControlTypes[] controls = consoleType.getControlTypes();

        for (int index = 0; index < controls.length; index++) {
            ControlTypes control = controls[index];
            ConsoleControlEntity controlEntity = ConsoleControlEntity.create(this.world, this.tardis().get());

            Vector3f position = current.toCenterPos().toVector3f().add(control.getOffset().x(), control.getOffset().y(),
                    control.getOffset().z());
            controlEntity.setPosition(position.x(), position.y(), position.z());
            controlEntity.setYaw(0.0f);
            controlEntity.setPitch(0.0f);

            Control controlKey = control.getControl();
            Control.ControlState state = controlKey == null ? null : this.controlStateMap.get(controlKey);
            float durability = state == null ? ConsoleControlEntity.MAX_DURABILITY : state.damage();
            boolean sticky = state != null && state.sticky();

            controlEntity.setControlData(consoleType, control, this.getPos(), durability, sticky);
            controlEntity.setControlIndex(index);

            serverWorld.spawnEntity(controlEntity);
            this.controlEntities.add(controlEntity);
        }

	    this.markDirty();
        this.needsControls = false;
    }

    // EffectProvider, so control animations can play their keyframe effects with the console as
    // the source rather than a control entity. Implemented directly rather than through modkit's
    // AnimatedBlockEntity, which supplies these same defaults but also drags in AnimatedInstance
    // and BedrockModelProvider, neither of which this block entity wants.

    @Override
    public boolean isSilent() {
        return false;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.BLOCKS;
    }

    @Override
    public float getHeadYaw() {
        return 0;
    }

    @Override
    public float getBodyYaw() {
        return 0;
    }

    @Override
    public float getPitch() {
        return 0;
    }

    @Override
    public Vec3d getEffectPosition(float tickDelta) {
        return Vec3d.ofCenter(this.getPos());
    }

    public void markNeedsControl() {
        this.needsControls = true;
    }

    /** Keeps control animations advancing while this console is culled and so never drawn. */
    private void tickControlAnimations() {
        if (this.controlAnimations == null)
            return;

        for (ControlAnimationState state : this.controlAnimations) {
            if (state != null)
                state.tick();
        }
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state, ConsoleBlockEntity blockEntity) {
        if (!TardisServerWorld.isTardisDimension(world)) {
            return;
        }

        if (this.needsControls)
            this.spawnControls();

        if (!(world instanceof ServerWorld serverWorld)) {
            this.age++;

            ANIM_STATE.startIfNotRunning(this.age);
            this.tickControlAnimations();
            return;
        }

        if (!TardisServerWorld.isTardisDimension((ServerWorld) this.getWorld()))
            this.markRemoved();

        if (!this.isLinked())
            return;

        SequenceHandler handler = this.tardis().get().sequence();
        TravelHandlerBase.State travelState = this.tardis().get().travel().getState();

        if (travelState == TravelHandlerBase.State.FLIGHT || travelState == TravelHandlerBase.State.MAT) {
            if (handler.hasActiveSequence() && handler.getActiveSequence() != null) {
                List<Control> sequence = handler.getActiveSequence().getControls();

                this.controlEntities.forEach(entity -> {
                    int index = sequence.indexOf(entity.getControl());

                    Control control = entity.getControl();
                    entity.setPartOfSequence(index != -1);
                    entity.setWasSequenced(handler.doesControlIndexMatch(control));
                    entity.setSequenceIndex(index);
                    entity.setSequenceLength(sequence.size());
                });
            } else {
                this.controlEntities.forEach(entity -> entity.setPartOfSequence(false));
            }
        }

        world.updateNeighborsAlways(pos, state.getBlock());

        ServerTardis tardis = (ServerTardis) this.tardis().get();
        boolean isRiftChunk = RiftChunkManager.isRiftChunk(tardis.travel().position());

        boolean moreThanAFew = this.controlEntities.stream()
                .filter(controlEntity -> controlEntity.getDurability() <
                        ConsoleControlEntity.DurabilityStates.FULL.durability)
                .count() > 5;

        if (tardis.travel().isCrashing() || moreThanAFew) {
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5f, pos.getY() + 1.25,
                    pos.getZ() + 0.5f, 5, 0, 0, 0, 0.025f);

            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5f, pos.getY() + 1.85,
                    pos.getZ() + 0.5f, 2, 0.2f, 0.5f, 0.2f, 0.01f);

            serverWorld.spawnParticles(
                    new DustColorTransitionParticleEffect(new Vector3f(0.75f, 0.75f, 0.75f),
                            new Vector3f(0.1f, 0.1f, 0.1f), 1),
                    pos.getX() + 0.5f, pos.getY() + 1.25, pos.getZ() + 0.5f, 1, 0, 0, 0, 0.01f);
        }

        if (tardis.crash().isToxic() || tardis.crash().isUnstable()) {
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5f, pos.getY() + 1.25,
                    pos.getZ() + 0.5f, 5, 0, 0, 0, 0.025f);

            serverWorld.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5f, pos.getY() + 1.25, pos.getZ() + 0.5f, 1,
                    0, 0.05f, 0, 0.025f);
        }

        if (tardis.crash().isToxic()) {
            serverWorld.spawnParticles(
                    new DustColorTransitionParticleEffect(new Vector3f(0.75f, 0.85f, 0.75f),
                            new Vector3f(0.15f, 0.25f, 0.15f), 1),
                    pos.getX() + 0.5f, pos.getY() + 1.25, pos.getZ() + 0.5f, 1,
                    AITMod.RANDOM.nextBoolean() ? 0.5f : -0.5f, 3f,
                    AITMod.RANDOM.nextBoolean() ? 0.5f : -0.5f, 0.025f);
        }

        if (tardis.isRefueling() && tardis.getFuel() < FuelHandler.TARDIS_MAX_FUEL) {
            serverWorld.spawnParticles((isRiftChunk) ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD,
                    pos.getX() + 0.5f, pos.getY() + 1.25, pos.getZ() + 0.5f, 1, 0, 0, 0,
                    (isRiftChunk) ? 0.05f : 0.025f);
        }

        if (ServerLifecycleHooks.get().getTicks() % 10 != 0)
            return;

        if (sonicScrewdriver != null) {
            if (this.hasMaxFuel(sonicScrewdriver))
                return;

            if (!tardis.fuel().hasPower())
                return;

            this.addFuel(10, sonicScrewdriver);
            tardis.fuel().removeFuel(10);
        }
    }

    public static ConsoleTypeSchema previousConsole(ConsoleTypeSchema current) {
        List<ConsoleTypeSchema> list = ConsoleRegistry.getInstance().toList();
        int idx = list.indexOf(current);
        int size = list.size();

        return list.get((size + idx - 1) % size);
    }

    public static ConsoleTypeSchema nextConsole(ConsoleTypeSchema current) {
        List<ConsoleTypeSchema> list = ConsoleRegistry.getInstance().toList();
        int idx = list.indexOf(current);
        int size = list.size();

        return list.get((idx + 1) % size);
    }

    public static ConsoleVariantSchema previousVariant(ConsoleVariantSchema current) {
        List<ConsoleVariantSchema> list = ConsoleVariantRegistry.withParent(current.parent());
        int idx = list.indexOf(current);
        int size = list.size();

        return list.get((size + idx - 1) % size);
    }

    public static ConsoleVariantSchema nextVariant(ConsoleVariantSchema current) {
        List<ConsoleVariantSchema> list = ConsoleVariantRegistry.withParent(current.parent());
        int idx = list.indexOf(current);
        int size = list.size();

        return list.get((idx + 1) % size);
    }

    @Override
    public int size() {
        return this.getInventory().size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.getInventory()) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    public DefaultedList<ItemStack> getInventory() {
        return this.inventory;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.getInventory(), slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return this.getInventory().remove(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.getInventory().set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    @Override
    public boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player) && !this.isEmpty();
    }

    @Override
    public void clear() {
        this.getInventory().clear();
    }

    public void setSonicScrewdriver(ItemStack stack) {
        this.sonicScrewdriver = stack;
        this.sync();
        this.markDirty();
    }

    public ItemStack getSonicScrewdriver() {
        return this.sonicScrewdriver;
    }

    @Override
    public double getMaxFuel(ItemStack stack) {
        return SonicItem.MAX_FUEL;
    }

    private Control.ControlState getOrCreateState(Control control) {
        return this.controlStateMap.computeIfAbsent(control,
                entry -> new Control.ControlState());
    }

    private void updateStateFromEntity(Control control, float damage, boolean sticky) {
        if (damage >= ConsoleControlEntity.MAX_DURABILITY && !sticky) {
            this.controlStateMap.remove(control);
            return;
        }

        this.controlStateMap.put(control, new Control.ControlState().setDamage(damage).setSticky(sticky));
    }

    @Override
    public void sync() {
        this.markDirty();
        if (this.world != null && !this.world.isClient)
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), net.minecraft.block.Block.NOTIFY_ALL);
    }
}