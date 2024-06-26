package loqor.ait.tardis.data;

import loqor.ait.AITMod;
import loqor.ait.core.util.DeltaTimeManager;
import loqor.ait.registry.impl.DesktopRegistry;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.TardisDesktopSchema;
import loqor.ait.tardis.TardisTravel;
import loqor.ait.tardis.base.TardisComponent;
import loqor.ait.tardis.base.TardisTickable;
import loqor.ait.tardis.data.properties.PropertiesHandler;
import loqor.ait.tardis.util.TardisUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Random;

public class InteriorChangingHandler extends TardisComponent implements TardisTickable {
    public static final String IS_REGENERATING = "is_regenerating";
	public static final String QUEUED_INTERIOR = "queued_interior";
	public static final Identifier CHANGE_DESKTOP = new Identifier(AITMod.MOD_ID, "change_desktop");
	private static Random random;
	private int ticks;

	public InteriorChangingHandler() {
		super(Id.INTERIOR);
	}

	private void setGenerating(boolean var) {
		PropertiesHandler.set(this.tardis(), IS_REGENERATING, var);
	}

	public boolean isGenerating() {
		return PropertiesHandler.getBool(this.tardis().properties(), IS_REGENERATING);
	}

	private void setTicks(int var) {
		this.ticks = var;
	}

	public int getTicks() {
		return this.ticks;
	}

	private void setQueuedInterior(TardisDesktopSchema schema) {
		PropertiesHandler.set(this.tardis(), QUEUED_INTERIOR, schema.id());
	}

	public TardisDesktopSchema getQueuedInterior() {
		return DesktopRegistry.getInstance().get(PropertiesHandler.getIdentifier(this.tardis().properties(), QUEUED_INTERIOR));
	}

	public void queueInteriorChange(TardisDesktopSchema schema) {
		Tardis tardis = this.tardis();

		if (!this.canQueue())
			return;

		if (tardis.fuel().getCurrentFuel() < 5000) {
			for (PlayerEntity player : TardisUtil.getPlayersInsideInterior(tardis)) {
				player.sendMessage(Text.translatable("tardis.message.interiorchange.not_enough_fuel").formatted(Formatting.RED), true);
				return;
			}
		}

		AITMod.LOGGER.info("Queueing interior change for {} to {}", this.tardis, schema);

		setQueuedInterior(schema);
		setTicks(0);
		setGenerating(true);
		DeltaTimeManager.createDelay("interior_change-" + tardis.getUuid().toString(), 100L);
		tardis.alarm().enable();

		tardis.getDesktop().getConsolePos().clear();

		if (!tardis.hasGrowthDesktop())
			tardis.removeFuel(5000 * (tardis.tardisHammerAnnoyance + 1));
	}

	private void onCompletion() {
		Tardis tardis = this.tardis();

        this.setGenerating(false);
		clearedOldInterior = false;

		tardis.alarm().disable();

		boolean previouslyLocked = tardis.getDoor().previouslyLocked();
		DoorData.lockTardis(previouslyLocked, tardis, null, false);

		tardis.engine().hasEngineCore().set(false);

		if (tardis.hasGrowthExterior()) {
			TardisTravel travel = tardis.travel();

			tardis.flight().handbrake().set(false);
			tardis.flight().autoLand().set(true);

			travel.dematerialise(true, true);
		}
	}

	private void warnPlayers() {
		for (PlayerEntity player : TardisUtil.getPlayersInsideInterior(this.tardis())) {
			player.sendMessage(Text.translatable("tardis.message.interiorchange.warning").formatted(Formatting.RED), true);
		}
	}

	private boolean isInteriorEmpty() {
		return TardisUtil.getPlayersInsideInterior(this.tardis()).isEmpty();
	}

	public static Random random() {
		if (random == null)
			random = new Random();

		return random;
	}

	private boolean clearedOldInterior = false;

	@Override
	public void tick(MinecraftServer server) {

		if (!isGenerating())
			return;

		if (DeltaTimeManager.isStillWaitingOnDelay("interior_change-" + this.tardis().getUuid().toString()))
			return;

		TardisTravel travel = this.tardis().travel();

		if (travel.getState() == TardisTravel.State.FLIGHT)
			travel.crash();

		if (this.isGenerating()) {
			if (!this.tardis().alarm().isEnabled())
				this.tardis().alarm().enable();
		}

		if (!this.canQueue()) {
			this.setGenerating(false);
			this.tardis().alarm().disable();
			return;
		}

		if (!isInteriorEmpty()) {
			warnPlayers();
			return;
		}

		if (isInteriorEmpty() && !this.tardis().getDoor().locked()) {
			DoorData.lockTardis(true, this.tardis(), null, true);
		}
		if (isInteriorEmpty() && !clearedOldInterior) {
			this.tardis().getDesktop().clearOldInterior(getQueuedInterior());
			DeltaTimeManager.createDelay("interior_change-" + this.tardis().getUuid().toString(), 15000L);
			clearedOldInterior = true;
			return;
		}
		if (isInteriorEmpty() && clearedOldInterior) {
			this.tardis().getDesktop().changeInterior(getQueuedInterior());
			onCompletion();
		}
	}

	private boolean canQueue() {
		return tardis.isGrowth() || tardis.engine().hasPower() || tardis.crash().isToxic();
	}
}
