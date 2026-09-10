package dev.amble.ait.core.tardis.control;

import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.api.tardis.TardisEvents;
import dev.amble.ait.core.AITItems;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.impl.SecurityControl;
import dev.amble.ait.core.tardis.control.sound.ControlSoundRegistry;
import dev.amble.ait.core.util.WorldUtil;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.lib.api.Identifiable;


public class Control implements Identifiable {

    private final Identifier id;

    public Control(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier id() {
        return id;
    }

    protected Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console,
                             boolean leftClick) throws ControlSequencedException {
        if (this.shouldBeAddedToSequence(tardis)) {
            this.addToControlSequence(tardis, player, console);
            throw ControlSequencedException.INSTANCE;
        }

        TardisEvents.USE_CONTROL.invoker().onUse(this, tardis, player, world, console, leftClick);

        return Result.FAILURE;
    }

    public Result handleRun(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console,
                             boolean leftClick) {
        try {
            return this.runServer(tardis, player, world, console, leftClick);
        } catch (Control.ControlSequencedException e) {
            return Result.SEQUENCE;
        }
    }

    /**
     * The label shown for this control (e.g. when scanning it with the sonic). May vary with live
     * TARDIS state; the default implementation returns the static, translatable control name.
     */
    public Text getName(Tardis tardis) {
        return Text.translatable(id.toTranslationKey("control"));
    }

    protected boolean shouldBeAddedToSequence(Tardis tardis) {
        return tardis.sequence().hasActiveSequence() && tardis.sequence().controlPartOfSequence(this);
    }

    public void addToControlSequence(Tardis tardis, ServerPlayerEntity player, BlockPos pos) {
        tardis.sequence().add(this, player, pos);

        if (AITMod.RANDOM.nextInt(0, 20) == 4) {
            tardis.loyalty().addLevel(player, 1);

            player.getServerWorld().spawnParticles(ParticleTypes.HEART, pos.toCenterPos().getX(),
                    pos.toCenterPos().getY() + 1, pos.toCenterPos().getZ(), 1, 0f, 1F, 0f, 5.0F);
        }
    }


    public SoundEvent getFallbackSound() {
        return null;
    }

    /**
     * Get the sound to play when this control is used
     * @param console The console variant this control is being used on
     * @param result Result of the control
     * @return The sound to play
     */
    public SoundEvent getSound(ConsoleTypeSchema console, Result result) {
        SoundEvent sound = ControlSoundRegistry.getInstance().get(console, this).sound(result);

        if (this.getFallbackSound() != null && (sound == null || sound == AITSounds.ERROR)) {
            return this.getFallbackSound();
        }

        return sound;
    }

    public boolean requiresPower() {
        return true;
    }

    protected SubSystem.IdLike requiredSubSystem() {
        return SubSystem.Id.ENGINE;
    }

    public void runAnimation(Tardis tardis, ServerPlayerEntity player, ServerWorld world) {
        // no animation
    }

    @Override
    public String toString() {
        return "Control{" + "id='" + id + '\'' + '}';
    }

    public long getDelayLength(Tardis tardis) {
        return 5;
    }

    public boolean shouldHaveDelay() {
        return true;
    }

    public boolean shouldHaveDelay(Tardis tardis) {
        return !this.shouldBeAddedToSequence(tardis) && this.shouldHaveDelay();
    }

    // Bypass security controls when using the disc :al_clueless: - Loqorb
    public boolean ignoresSecurity(ServerPlayerEntity user) {
        return user.getMainHandStack().getItem() == AITItems.CONTROL_DISC;
    }

    public boolean canRun(Tardis tardis, ServerPlayerEntity user) {
        if (tardis.isGrowth())
            return false;

        if (this.requiresPower() && !tardis.fuel().hasPower())
            return false;

        boolean security = tardis.stats().security().get();

        if (!this.ignoresSecurity(user) && security)
            return SecurityControl.hasMatchingKey(user, tardis);

        SubSystem.IdLike dependent = this.requiredSubSystem();

        if (dependent != null && !this.shouldBeAddedToSequence(tardis)) {
            boolean enabled = tardis.subsystems().get(dependent).isEnabled();

            if (!enabled)
                user.sendMessage(Text.translatable("warning.ait.needs_subsystem", Text.literal(WorldUtil.fakeTranslate(dependent.toString())).formatted(Formatting.RED)).formatted(Formatting.WHITE), true);

            return enabled;
        }

        return true;
    }

	/**
	 * A float to decide where the animation should run to.
	 * 1 is the entire animation, 0 is none of it.
	 */
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		return cooldown ? 1.0F : 0.0F;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || this.getClass() != o.getClass())
            return false;

        Control control = (Control) o;
        return this.id.equals(control.id());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public enum Result {
        SUCCESS, FAILURE, SEQUENCE, SUCCESS_ALT;

        public boolean isSuccess() {
            return this == SUCCESS || this == SUCCESS_ALT;
        }

        public boolean isAltSound() {
            return this == SUCCESS_ALT || this == FAILURE;
        }
    }

    public static class ControlSequencedException extends RuntimeException {
        /**
         * The singleton instance, to reduce object allocations.
         */
        public static final ControlSequencedException INSTANCE = new ControlSequencedException();

        private ControlSequencedException() {
            this.setStackTrace(new StackTraceElement[0]);
        }

        public synchronized Throwable fillInStackTrace() {
            this.setStackTrace(new StackTraceElement[0]);
            return this;
        }
    }

    public static class ControlState {
        private float damage = 1.0f;
        private boolean sticky = false;

        public float damage() {
            return damage;
        }

        public boolean sticky() {
            return sticky;
        }

        public ControlState setDamage(float damage) {
            this.damage = damage;
            return this;
        }

        public ControlState setSticky(boolean sticky) {
            this.sticky = sticky;
            return this;
        }

        public NbtCompound writeNbt() {
            NbtCompound stateCompound = new NbtCompound();
            stateCompound.putFloat("Durability", this.damage);
            stateCompound.putBoolean("Sticky", this.sticky);
            return stateCompound;
        }

        public ControlState readNbt(NbtCompound nbt) {
            float durability = nbt.getFloat("Durability");
            boolean sticky = nbt.getBoolean("Sticky");
            this.setDamage(durability);
            this.setSticky(sticky);
            return this;
        }
    }
}
