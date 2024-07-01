package loqor.ait.tardis.data.travel;

import loqor.ait.AITMod;
import loqor.ait.core.AITSounds;
import loqor.ait.core.data.DirectedGlobalPos;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.base.TardisTickable;
import loqor.ait.tardis.control.sequences.SequenceHandler;
import loqor.ait.tardis.data.TardisCrashData;
import loqor.ait.tardis.data.properties.PropertiesHandler;
import loqor.ait.tardis.data.properties.v2.bool.BoolProperty;
import loqor.ait.tardis.data.properties.v2.bool.BoolValue;
import loqor.ait.tardis.data.properties.v2.integer.IntProperty;
import loqor.ait.tardis.data.properties.v2.integer.IntValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public abstract class ProgressiveTravelHandler extends TravelHandlerBase implements TardisTickable {

    private static final Random random = Random.create();

    private static final IntProperty FLIGHT_TICKS = new IntProperty("flight_ticks");
    private static final IntProperty TARGET_TICKS = new IntProperty("target_ticks");

    private static final BoolProperty HANDBRAKE = new BoolProperty("handbrake", true);
    private static final BoolProperty AUTOPILOT = new BoolProperty("autopilot", false);

    private final IntValue flightTicks = FLIGHT_TICKS.create(this);
    private final IntValue targetTicks = TARGET_TICKS.create(this);

    protected final BoolValue handbrake = HANDBRAKE.create(this);
    protected final BoolValue autopilot = AUTOPILOT.create(this);

    public ProgressiveTravelHandler(Id id) {
        super(id);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        flightTicks.of(this, FLIGHT_TICKS);
        targetTicks.of(this, TARGET_TICKS);

        handbrake.of(this, HANDBRAKE);
        autopilot.of(this, AUTOPILOT);
    }

    private boolean isInFlight() {
        return this.getState() == State.FLIGHT || this.getState() == State.MAT;
    }

    private boolean isFlightTicking() {
        return this.tardis().travel().getState() == State.FLIGHT && this.getTargetTicks() != 0;
    }

    public boolean hasFinishedFlight() {
        return (this.getFlightTicks() >= this.getTargetTicks() || tardis.travel().isCrashing()) &&
                !PropertiesHandler.getBool(tardis().properties(), PropertiesHandler.IS_IN_REAL_FLIGHT);
    }

    @Override
    public void forceDestination(DirectedGlobalPos.Cached cached) {
        super.forceDestination(cached);
        this.recalculate();
    }

    // TODO inline
    private void onFlightFinished() {
        this.tardis.getDesktop().playSoundAtEveryConsole(SoundEvents.BLOCK_BELL_RESONATE);
        this.resetFlight();

        if (this.autopilot.get() && !PropertiesHandler.getBool(this.tardis.properties(), PropertiesHandler.IS_IN_REAL_FLIGHT))
            this.tardis().travel().rematerialize();
    }

    public void increaseFlightTime(int ticks) {
        this.setTargetTicks(this.getTargetTicks() + ticks);
    }

    public void decreaseFlightTime(int ticks) {
        this.setTargetTicks(this.getTargetTicks() - ticks);
    }

    public int getDurationAsPercentage() {
        if (this.getTargetTicks() == 0 || this.getFlightTicks() == 0)
            return this.getState() == TravelHandlerBase.State.DEMAT ? 0 : 100;

        int target = this.getTargetTicks();
        int flightTicksClamped = MathHelper.clamp(this.getFlightTicks(), 1, target);
        int percentage = (flightTicksClamped * 100) / target;

        return Math.max(0, percentage);
    }

    public DirectedGlobalPos.Cached getProgress() {
        if (this.isServer() && this.position().getWorld().getServer().getTicks() % 5 == 0) {

            System.out.println("to: " + this.destination().getPos() + "; from: " + this.position().getPos() + "; %=" + this.getDurationAsPercentage() +
                    "; alt: " + TravelUtil.getPositionFromPercentage(
                    this.position(), this.destination(), 100 - this.getDurationAsPercentage()
            ).getPos());
        }
        return TravelUtil.getPositionFromPercentage(
                this.position(), this.destination(), this.getDurationAsPercentage()
        );
    }

    public void recalculate() {
        this.setTargetTicks(TravelUtil.getFlightDuration(this.position(), this.destination()));
        this.setFlightTicks(this.isInFlight() ? MathHelper.clamp(this.getFlightTicks(), 0, this.getTargetTicks()) : 0);
    }

    protected void startFlight() {
        this.setFlightTicks(0);
        this.setTargetTicks(TravelUtil.getFlightDuration(
                this.position(), this.destination())
        );

        AITMod.LOGGER.info("Started flight: ");
    }

    protected void resetFlight() {
        this.setFlightTicks(0);
        this.setTargetTicks(0);

        AITMod.LOGGER.info("Reset flight");
    }

    public int getFlightTicks() {
        return this.flightTicks.get();
    }

    public void setFlightTicks(int ticks) {
        this.flightTicks.set(ticks);
    }

    public int getTargetTicks() {
        return this.targetTicks.get();
    }

    protected void setTargetTicks(int ticks) {
        this.targetTicks.set(ticks);
    }

    public void handbrake(boolean value) {
        if (this.getState() == TravelHandlerBase.State.DEMAT && value)
            this.tardis.travel().cancelDemat();

        handbrake.set(value);
    }

    public boolean handbrake() {
        return handbrake.get();
    }

    public boolean autopilot() {
        return autopilot.get();
    }

    public void autopilot(boolean value) {
        this.autopilot.set(value);

        int speed = this.speed.get();
        int expectedSpeed = this.clampSpeed(speed);

        if (expectedSpeed != speed)
            this.speed.set(expectedSpeed);
    }

    public void increaseSpeed() {
        this.speed(this.speed.get() + 1);
    }

    public void decreaseSpeed() {
        if (this.getState() == State.LANDED && this.speed.get() == 1)
            this.tardis.getDesktop().playSoundAtEveryConsole(AITSounds.LAND_THUD, SoundCategory.AMBIENT);

        this.speed(this.speed.get() - 1);
    }

    @Override
    protected int clampSpeed(int value) {
        int max = this.autopilot() ? 1 : this.maxSpeed.get();
        return MathHelper.clamp(value, 0, max);
    }

    @Override
    public void tick(MinecraftServer server) {
        Tardis tardis = this.tardis();

        TardisCrashData crash = tardis.crash();
        TravelHandler travel = tardis.travel();

        if (crash.getState() != TardisCrashData.State.NORMAL)
            crash.addRepairTicks(2 * travel.speed());

        if ((this.getTargetTicks() > 0 || this.getFlightTicks() > 0) && travel.getState() == TravelHandlerBase.State.LANDED)
            this.recalculate();

        if (this.isInFlight() && !travel.isCrashing() && this.getTargetTicks() == 0 && this.getFlightTicks() < this.getTargetTicks())
            this.recalculate();

        if (server.getTicks() % 2 == 0)
            this.triggerSequencingDuringFlight(tardis);

        if (this.isFlightTicking()) {
            if (this.hasFinishedFlight()) {
                this.onFlightFinished();
                return;
            }

            this.setFlightTicks(this.getFlightTicks() + (Math.max(this.speed() / 2, 1)));
        }

        this.getProgress();
    }

    public void triggerSequencingDuringFlight(Tardis tardis) {
        SequenceHandler sequences = tardis.sequence();

        if (!this.autopilot.get()
                && this.getDurationAsPercentage() < 100
                && this.getState() == TravelHandlerBase.State.FLIGHT
                && !sequences.hasActiveSequence()
                && !this.position().equals(this.destination())
                && this.getTargetTicks() > 100
                && random.nextBetween(0, 230 / (this.speed() == 0 ? 1 : this.speed())) == 7) {
            sequences.triggerRandomSequence(true);
        }
    }
}
