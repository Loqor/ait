package loqor.ait.tardis;

import io.wispforest.owo.ops.WorldOps;
import loqor.ait.AITMod;
import loqor.ait.api.tardis.TardisEvents;
import loqor.ait.core.AITBlocks;
import loqor.ait.core.AITSounds;
import loqor.ait.core.blockentities.ExteriorBlockEntity;
import loqor.ait.core.blocks.ExteriorBlock;
import loqor.ait.core.data.AbsoluteBlockPos;
import loqor.ait.core.sounds.MatSound;
import loqor.ait.core.util.ForcedChunkUtil;
import loqor.ait.registry.impl.CategoryRegistry;
import loqor.ait.tardis.base.TardisComponent;
import loqor.ait.tardis.control.impl.DirectionControl;
import loqor.ait.tardis.control.impl.SecurityControl;
import loqor.ait.tardis.control.sequences.SequenceHandler;
import loqor.ait.tardis.data.BiomeHandler;
import loqor.ait.tardis.data.DoorData;
import loqor.ait.tardis.data.SonicHandler;
import loqor.ait.tardis.data.TardisCrashData;
import loqor.ait.tardis.data.properties.PropertiesHandler;
import loqor.ait.tardis.util.FlightUtil;
import loqor.ait.tardis.util.NetworkUtil;
import loqor.ait.tardis.util.TardisUtil;
import loqor.ait.tardis.wrapper.server.ServerTardis;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TardisTravel extends TardisComponent {

	public static final Identifier CANCEL_DEMAT_SOUND = new Identifier(AITMod.MOD_ID, "cancel_demat_sound");

	private State state = State.LANDED;
	private AbsoluteBlockPos.Directed position;
	private AbsoluteBlockPos.Directed destination;
	private AbsoluteBlockPos.Directed lastPosition;
	private boolean crashing = false;
	private static final int CHECK_LIMIT = AITMod.AIT_CONFIG.SEARCH_HEIGHT();
	private static final Random random = new Random();

	public TardisTravel(AbsoluteBlockPos.Directed pos) {
		super(Id.TRAVEL);
		this.position = pos;

		if (this.lastPosition == null)
			this.lastPosition = pos;
	}

	@Override
	public void onCreate() {
		this.placeExterior();
		this.runAnimations();
	}

	static {
		TardisEvents.LOSE_POWER.register(tardis ->
				tardis.flight().autoLand().set(false));
	}

	public boolean isCrashing() {
		return this.crashing;
	}

	public void setPosition(AbsoluteBlockPos.Directed pos) {
		this.position = pos;
	}

	public void setLastPosition(AbsoluteBlockPos.Directed position) {
		this.lastPosition = position;
	}

	public AbsoluteBlockPos.Directed getLastPosition() {
		return lastPosition;
	}

	public AbsoluteBlockPos.Directed getPosition() {
		return position;
	}

	public void tick(MinecraftServer server) {
		this.tickDemat();
		this.tickMat();

		ServerTardis tardis = (ServerTardis) this.tardis();
		int speed = this.tardis.flight().speed().get();
		State state = this.getState();

		boolean handbrake = tardis.flight().handbrake().get();
		boolean autopilot = tardis.flight().autoLand().get();

		if (speed > 0 && state == State.LANDED && !handbrake && !tardis.sonic().hasSonic(SonicHandler.HAS_EXTERIOR_SONIC)) {
			this.dematerialise(autopilot);
		}

		if (speed == 0 && state == State.FLIGHT) {
			if (tardis.crash().getState() == TardisCrashData.State.UNSTABLE) {
				int random_int = random.nextInt(0, 2);
				int up_or_down = random_int == 0 ? 1 : -1;
				int random_change = random.nextInt(1, 10) * up_or_down;
				int new_x = getDestination().getX() + random_change;
				int new_y = getDestination().getX();
				int new_z = getDestination().getZ() + random_change;

				this.setDestination(new AbsoluteBlockPos.Directed(new_x, new_y, new_z, getDestination().getWorld(), getDestination().getRotation()));

				if (getDestination().getWorld().getRegistryKey() == TardisUtil.getTardisDimension().getRegistryKey()) {
					this.setDestination(new AbsoluteBlockPos.Directed(new_x, new_y, new_z, server.getOverworld(), getDestination().getRotation()));
				}
			}
			if (!PropertiesHandler.getBool(tardis.properties(), PropertiesHandler.IS_IN_REAL_FLIGHT)) {
				this.materialise();
			}
		}

		// Should we just disable autopilot if the speed goes above 1?
		if (speed > 1 && state == State.FLIGHT && autopilot) {
			this.tardis.flight().speed().set(speed - 1);
		}
	}

	public void increaseSpeed() {
		// Stop speed from going above 1 if autopilot is enabled, and we're in flight
		if (this.tardis.flight().speed().get() > 0 && this.getState() == State.FLIGHT && tardis.flight().autoLand().get()) {
			return;
		}

		this.tardis.flight().speed().set(MathHelper.clamp(this.tardis.flight().speed().get() + 1, 0, this.tardis.flight().maxSpeed().get()));
	}

	public void decreaseSpeed() {
		if (this.getState() == State.LANDED && this.tardis.flight().speed().get() == 1)
			FlightUtil.playSoundAtEveryConsole(this.tardis().getDesktop(), AITSounds.LAND_THUD, SoundCategory.AMBIENT);

		this.tardis.flight().speed().set(MathHelper.clamp(this.tardis.flight().speed().get() - 1, 0, this.tardis.flight().maxSpeed().get()));
	}

	public boolean inFlight() {
		return this.getState() == State.FLIGHT;
	}

	/**
	 * Gets the number of ticks that the Tardis has been materialising for
	 *
	 * @return ticks
	 */
	public int getMatTicks() {
		return PropertiesHandler.getInt(this.tardis().properties(), PropertiesHandler.MAT_TICKS);
	}

	private void setMatTicks(int ticks) {
		PropertiesHandler.set(this.tardis(), PropertiesHandler.MAT_TICKS, ticks, false);
	}

	private void tickMat() {
		if (this.getState() != State.MAT) {
			if (getMatTicks() != 0) setMatTicks(0);
			return;
		}

		setMatTicks(getMatTicks() + 1);

		if (getMatTicks() > (FlightUtil.getSoundLength(getMatSoundForCurrentState()) * 40)) {
			this.forceLand();
			this.setMatTicks(0);
		}
	}

	/**
	 * Gets the number of ticks that the Tardis has been dematerialising for
	 *
	 * @return ticks
	 */
	public int getDematTicks() {
		return PropertiesHandler.getInt(this.tardis().properties(), PropertiesHandler.DEMAT_TICKS);
	}

	private void setDematTicks(int ticks) {
		PropertiesHandler.set(this.tardis(), PropertiesHandler.DEMAT_TICKS, ticks, false);
	}

	private void tickDemat() {
		if (this.getState() != State.DEMAT) {
			if (getDematTicks() != 0)
				setDematTicks(0);

			return;
		}

		setDematTicks(getDematTicks() + 1);

		if (tardis.flight().handbrake().get()) {
			// cancel materialise
			this.cancelDemat();
			return;
		}

		if (getDematTicks() > (FlightUtil.getSoundLength(getMatSoundForCurrentState()) * 40)) {
			this.toFlight();
			this.setDematTicks(0);
		}
	}

	/**
	 * Stops demat while its happening - then plays a boom sound to signify
	 */
	private void cancelDemat() {
		if (this.getState() != State.DEMAT)
			return; // rip

		if (this.getPosition() == null || this.tardis().getDesktop() == null)
			return;

		this.forceLand();
		this.playThudSound();

		NetworkUtil.sendToInterior(this.tardis(), CANCEL_DEMAT_SOUND, PacketByteBufs.empty());
	}

	public void playThudSound() {
		this.getPosition().getWorld().playSound(null, this.getPosition(), AITSounds.LAND_THUD, SoundCategory.AMBIENT);
		FlightUtil.playSoundAtEveryConsole(this.tardis().getDesktop(), AITSounds.LAND_THUD, SoundCategory.AMBIENT);
	}

	/**
	 * Performs a crash for the Tardis.
	 * If the Tardis is not in flight state, the crash will not be executed.
	 */
	public void crash() {
		if (this.getState() != State.FLIGHT || this.isCrashing())
			return;

		Tardis tardis = tardis();
		int crash_intensity = this.tardis.flight().speed().get() + tardis.tardisHammerAnnoyance + 1;

		List<Explosion> explosions = new ArrayList<>();

		tardis.getDesktop().getConsolePos().forEach(console -> {
			FlightUtil.playSoundAtConsole(console,
					SoundEvents.ENTITY_GENERIC_EXPLODE,
					SoundCategory.BLOCKS,
					3f,
					1f);

			Explosion explosion = TardisUtil.getTardisDimension().createExplosion(
					null,
					null,
					null,
					console.toCenterPos(),
					3f * crash_intensity,
					false,
					World.ExplosionSourceType.MOB
			);

			explosions.add(explosion);
		});

		if (this.tardis.sequence().hasActiveSequence()) {
			this.tardis.sequence().setActiveSequence(null, true);
		}

		Random random = new Random();
		for (ServerPlayerEntity player : TardisUtil.getPlayersInInterior(tardis)) {
			float random_X_velocity = random.nextFloat(-2f, 3f);
			float random_Y_velocity = random.nextFloat(-1f, 2f);
			float random_Z_velocity = random.nextFloat(-2f, 3f);
			player.setVelocity(random_X_velocity * crash_intensity, random_Y_velocity * crash_intensity, random_Z_velocity * crash_intensity);
			//player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * crash_intensity, (int) Math.round(0.25 * crash_intensity), true, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * crash_intensity, 1, true, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * crash_intensity, (int) Math.round(0.25 * crash_intensity), true, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * crash_intensity, (int) Math.round(0.25 * crash_intensity), true, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * crash_intensity, (int) Math.round(0.25 * crash_intensity), true, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * crash_intensity, (int) Math.round(0.75 * crash_intensity), true, false, false));
			int damage_to_player = (int) Math.round(0.5 * crash_intensity);
			if (!explosions.isEmpty()) {
				player.damage(TardisUtil.getTardisDimension().getDamageSources().explosion(explosions.get(0)), damage_to_player);
			} else {
				player.damage(TardisUtil.getTardisDimension().getDamageSources().generic(), damage_to_player);
			}
		}
		tardis.setLockedTardis(true);
		PropertiesHandler.set(tardis, PropertiesHandler.ALARM_ENABLED, true);
		PropertiesHandler.set(tardis, PropertiesHandler.ANTIGRAVS_ENABLED, false);
		this.tardis.flight().speed().set(0);
		tardis.removeFuel(500 * crash_intensity);
		tardis.tardisHammerAnnoyance = 0;
		int random_int = random.nextInt(0, 2);
		int up_or_down = random_int == 0 ? 1 : -1;
		int random_change = random.nextInt(10, 100) * crash_intensity * up_or_down;
		AbsoluteBlockPos.Directed percentageOfDestination = FlightUtil.getPositionFromPercentage(tardis.position(), tardis.destination(), tardis.flight().getDurationAsPercentage());
		int new_x = percentageOfDestination.getX() + random_change;
		int new_y = percentageOfDestination.getY();
		int new_z = percentageOfDestination.getZ() + random_change;
		this.setCrashing(true);
		this.setDestination(new AbsoluteBlockPos.Directed(new_x, new_y, new_z, getDestination().getWorld(), getDestination().getRotation()));
		if (getDestination().getWorld() != null && getDestination().getWorld().getRegistryKey() == TardisUtil.getTardisDimension().getRegistryKey()) {
			this.setDestination(new AbsoluteBlockPos.Directed(new_x, new_y, new_z, TardisUtil.getOverworld(), getDestination().getRotation()));
		}
		this.crashAndMaterialise();
		int repair_ticks = 1000 * crash_intensity;
		tardis.crash().setRepairTicks(repair_ticks);
		if (repair_ticks > TardisCrashData.UNSTABLE_TICK_START_THRESHOLD) {
			tardis.crash().setState(TardisCrashData.State.TOXIC);
		} else {
			tardis.crash().setState(TardisCrashData.State.UNSTABLE);
		}
		TardisEvents.CRASH.invoker().onCrash(tardis);
	}

	public void materialise() {
		this.materialise(false);
	}

	public void crashAndMaterialise() {
		if (this.getDestination().getWorld().isClient() || this.getState() != State.FLIGHT) {
			return;
		}
		this.setState(State.MAT);
		ServerWorld destWorld = (ServerWorld) this.getDestination().getWorld();
		ForcedChunkUtil.keepChunkLoaded(destWorld, this.getDestination());
		ExteriorBlock block = (ExteriorBlock) AITBlocks.EXTERIOR_BLOCK;
		BlockState state = block.getDefaultState().with(Properties.ROTATION, this.getDestination().getRotation()).with(ExteriorBlock.LEVEL_9, 0);
		destWorld.setBlockState(this.getDestination(), state);

		// Create and add the exterior block entity at the destination
		destWorld.addBlockEntity(new ExteriorBlockEntity(this.getDestination(), state, this.tardis));

		// Set the position of the Tardis to the destination
		this.setPosition(this.getDestination());
		WorldOps.updateIfOnServer(destWorld, this.getDestination());
	}

	public void setStateAndLand(AbsoluteBlockPos.Directed pos) {
		if (pos.getWorld().isClient())
			return;

		this.setState(State.LANDED);
		deleteExterior();

		ServerWorld destWorld = (ServerWorld) pos.getWorld();
		ForcedChunkUtil.keepChunkLoaded(destWorld, pos);
		ExteriorBlock block = (ExteriorBlock) AITBlocks.EXTERIOR_BLOCK;
		BlockState state = block.getDefaultState().with(Properties.ROTATION, pos.getRotation()).with(ExteriorBlock.LEVEL_9, 0);
		destWorld.setBlockState(pos, state);

		// Create and add the exterior block entity at the destination
		destWorld.addBlockEntity(new ExteriorBlockEntity(pos, state, this.tardis));

		// Set the position of the Tardis to the destination
		this.setPosition(pos);
		this.setDestination(pos);
		((BiomeHandler) tardis().getHandlers().get(Id.BIOME)).update();
		WorldOps.updateIfOnServer(destWorld, pos);
	}

	/**
	 * Materialises the Tardis, bringing it to the specified destination.
	 * This method handles the logic of materialization, including sound effects, locking the Tardis, and setting the Tardis state.
	 */
	public void materialise(boolean ignoreChecks) {
		// Check if running on the client side, and if so, return early
		if (this.getDestination().getWorld().isClient())
			return;

		ServerTardis tardis = (ServerTardis) this.tardis();

		if (this.getState() != State.FLIGHT)
			return;

		ignoreChecks = ignoreChecks || tardis.hasGrowthExterior();

		// Disable autopilot
		// PropertiesHandler.setAutoPilot(this.getTardis().get().properties(), false);

		this.setDestination(FlightUtil.getPositionFromPercentage(tardis.position(), tardis.destination(), tardis.flight().getDurationAsPercentage()), true);

		// Check if materialization is on cooldown and return if it is
		if (!ignoreChecks && FlightUtil.isMaterialiseOnCooldown(tardis)) {
			return;
		}

		// Check if the Tardis materialization is prevented by event listeners
		if (!ignoreChecks && TardisEvents.MAT.invoker().onMat(tardis)) {
			failToMaterialise();
			return;
		}

		// Lock the Tardis doors
		// DoorData.lockTardis(true, this.getTardis().get(), null, true);

		// Set the Tardis state to materialise
		this.setState(State.MAT);

		SequenceHandler sequences = tardis.handler(Id.SEQUENCE);

		if (sequences.hasActiveSequence()) {
			sequences.setActiveSequence(null, true);
		}

		// Get the server world of the destination
		ServerWorld destWorld = (ServerWorld) this.getDestination().getWorld();

		// Play materialize sound at the destination
		this.getDestination().getWorld().playSound(null, this.getDestination(), this.getSoundForCurrentState(), SoundCategory.BLOCKS, 1f, 1f);

		FlightUtil.playSoundAtEveryConsole(tardis.getDesktop(), this.getSoundForCurrentState(), SoundCategory.BLOCKS, 1f, 1f);

		// Set the destination block to the Tardis exterior block
		ExteriorBlock block = (ExteriorBlock) AITBlocks.EXTERIOR_BLOCK;
		BlockState state = block.getDefaultState().with(Properties.ROTATION, Math.abs(this.getDestination().getRotation())).with(ExteriorBlock.LEVEL_9, 0);
		destWorld.setBlockState(this.getDestination(), state);

		// Create and add the exterior block entity at the destination
		ExteriorBlockEntity blockEntity = new ExteriorBlockEntity(this.getDestination(), state, this.tardis);
		destWorld.addBlockEntity(blockEntity);

		// Set the position of the Tardis to the destination
		this.setPosition(this.getDestination());

		// Run animations on the block entity
		this.runAnimations(blockEntity);

		this.onMaterialise(tardis);
	}

	private void onMaterialise(ServerTardis tardis) {
		((BiomeHandler) tardis.getHandlers().get(Id.BIOME)).update();
		if (tardis.isGrowth()) {
			TardisExterior exterior = tardis.getExterior();

			exterior.setType(CategoryRegistry.CAPSULE);
			tardis.getDoor().closeDoors();
		}
	}

	public void dematerialise(boolean withRemat, boolean ignoreChecks) {
		if (this.getState() != State.LANDED)
			return;

		if (!ignoreChecks && !this.tardis.engine().hasPower())
			return; // no flying for you if you have no powa :)

		if (this.getPosition().getWorld().isClient())
			return;

		if (FlightUtil.isDematerialiseOnCooldown(tardis()))
			return; // cancelled

		if (tardis.flight().autoLand().get()) {
			// fulfill all the prerequisites
			// DoorData.lockTardis(true, tardis(), null, false);
			tardis.flight().handbrake().set(false);
			this.tardis.getDoor().closeDoors();
			this.tardis.setRefueling(false);

			if (this.tardis.flight().speed().get() == 0)
				this.increaseSpeed();
		}

		tardis.flight().autoLand().set(withRemat);
		ServerWorld world = (ServerWorld) this.getPosition().getWorld();

		if (!ignoreChecks && TardisEvents.DEMAT.invoker().onDemat(tardis())) {
			failToTakeoff();
			return;
		}

		// DoorData.lockTardis(true, this.getTardis().get(), null, true);

		this.setState(State.DEMAT);

		world.playSound(null, this.getPosition(), this.getSoundForCurrentState(), SoundCategory.BLOCKS);
		FlightUtil.playSoundAtEveryConsole(this.tardis().getDesktop(), this.getSoundForCurrentState(), SoundCategory.BLOCKS, 10f, 1f);

		this.runAnimations();
	}

	public void dematerialise(boolean withRemat) {
		this.dematerialise(withRemat, false);
	}

	public void dematerialise() {
		this.dematerialise(false);
	}

	private void failToMaterialise() {
		// Play failure sound at the current position
		this.getPosition().getWorld().playSound(null, this.getPosition(), AITSounds.FAIL_MAT, SoundCategory.BLOCKS, 1f, 1f);

		// Play failure sound at the Tardis console position if the interior is not empty
		FlightUtil.playSoundAtEveryConsole(this.tardis().getDesktop(), AITSounds.FAIL_MAT, SoundCategory.BLOCKS, 1f, 1f);

		// Create materialization delay and return
		FlightUtil.createMaterialiseDelay(this.tardis());
	}

	private void failToTakeoff() {
		// demat will be cancelled
		this.getPosition().getWorld().playSound(null, this.getPosition(), AITSounds.FAIL_DEMAT, SoundCategory.BLOCKS, 1f, 1f); // fixme can be spammed

		if (TardisUtil.isInteriorNotEmpty(tardis()))
			FlightUtil.playSoundAtEveryConsole(this.tardis().getDesktop(), AITSounds.FAIL_DEMAT, SoundCategory.BLOCKS, 1f, 1f);

		// TardisUtil.sendMessageToPilot(this.getTardis().get(), Text.literal("Unable to takeoff!")); // fixme translatable
		FlightUtil.createDematerialiseDelay(this.tardis());
	}

	/**
	 * Checks whether the destination is valid otherwise searches for a new one
	 *
	 * @param limit     how many times the search can happen (should stop hanging)
	 * @param fullCheck whether to search downwards or upwards
	 * @return whether its safe to land
	 */
	private boolean checkDestination(int limit, boolean fullCheck) {
		if (this.isClient())
			return true;

		ServerWorld world = (ServerWorld) this.getDestination().getWorld(); // this cast is fine, we know its server

		// is long line
		setDestination(new AbsoluteBlockPos.Directed(
						getDestination().getX(),
						MathHelper.clamp(getDestination().getY(), world.getBottomY(), world.getTopY() - 1),
						getDestination().getZ(),
						getDestination().getWorld(),
						getDestination().getRotation()),
				false
		);

		BlockPos.Mutable temp = this.getDestination().mutableCopy(); // loqor told me mutables were better, is this true? fixme if not

		BlockState current;
		BlockState top;
		BlockState ground;

		if (fullCheck) {
			for (int i = 0; i < limit; i++) {
				current = world.getBlockState(temp);
				top = world.getBlockState(temp.up());
				ground = world.getBlockState(temp.down());

				if (isReplaceable(current, top) && !isReplaceable(ground)) { // check two blocks cus tardis is two blocks tall yk and check for groud
					this.setDestination(new AbsoluteBlockPos.Directed(temp, world, this.getDestination().getRotation()), false);
					return true;
				}

				temp = temp.down().mutableCopy();
			}

			temp = this.getDestination().mutableCopy();

			for (int i = 0; i < limit; i++) {
				current = world.getBlockState(temp);
				top = world.getBlockState(temp.up());
				ground = world.getBlockState(temp.down());

				if (isReplaceable(current, top) && !isReplaceable(ground)) { // check two blocks cus tardis is two blocks tall yk and check for groud
					this.setDestination(new AbsoluteBlockPos.Directed(temp, world, this.getDestination().getRotation()), false);
					return true;
				}

				temp = temp.up().mutableCopy();
			}
		}

		temp = this.getDestination().mutableCopy();

		current = world.getBlockState(temp);
		top = world.getBlockState(temp.up());

		return isReplaceable(current, top);
	}

	private static boolean isReplaceable(BlockState... states) {
		for (BlockState state1 : states) {
			if (!state1.isReplaceable()) {
				return false;
			}
		}

		return true;
	}

	public boolean checkDestination() {
		return this.checkDestination(CHECK_LIMIT, PropertiesHandler.getBool(this.tardis().properties(), PropertiesHandler.FIND_GROUND));
	}

	public void toFlight() {
		this.setCrashing(false);
		this.setLastPosition(this.getPosition());
		this.setState(TardisTravel.State.FLIGHT);
		this.deleteExterior();

		if (PropertiesHandler.getBool(this.tardis().properties(), SecurityControl.SECURITY_KEY)) {
			SecurityControl.runSecurityProtocols(this.tardis());
		}
	}

	public void forceLand(@Nullable ExteriorBlockEntity blockEntity) {
		if (tardis.flight().autoLand().get() && this.tardis.flight().speed().get() > 0) {
			this.tardis.flight().speed().set(0);
		}

		this.setState(TardisTravel.State.LANDED);

		if (blockEntity == null) {
			ServerWorld world = (ServerWorld) this.getPosition().getWorld();
			BlockEntity found = world.getBlockEntity(this.getPosition());


			// If there is already a matching exterior at this position
			if (found instanceof ExteriorBlockEntity exterior
					&& exterior.tardis().isPresent()
					&& Objects.equals(exterior.tardis().get(), this.tardis())) {
				blockEntity = exterior;
			} else {
				ExteriorBlock block = (ExteriorBlock) AITBlocks.EXTERIOR_BLOCK;
				BlockState state = block.getDefaultState().with(Properties.ROTATION, this.getPosition().getRotation()).with(ExteriorBlock.LEVEL_9, 0);
				world.setBlockState(this.getPosition(), state);

				ExteriorBlockEntity newEntity = new ExteriorBlockEntity(this.getPosition(), state, this.tardis);
				world.addBlockEntity(newEntity);
				blockEntity = newEntity;
			}

		}

		this.runAnimations(blockEntity);

		if (this.isClient())
			return;

		DoorData.lockTardis(PropertiesHandler.getBool(this.tardis().properties(), PropertiesHandler.PREVIOUSLY_LOCKED), this.tardis(), null, false);
		TardisEvents.LANDED.invoker().onLanded(tardis());
	}

	public void setCrashing(boolean crashing) {
		this.crashing = crashing;
	}

	public void forceLand() {
		this.forceLand(null);
	}

	public void runAnimations() {
		ServerWorld level = (ServerWorld) this.getPosition().getWorld();
		BlockEntity entity = level.getBlockEntity(this.getPosition());
		if (entity instanceof ExteriorBlockEntity exterior) {
			if (exterior.getAnimation() == null)
				return;

			exterior.getAnimation().setupAnimation(this.state);
			exterior.getAnimation().tellClientsToSetup(this.state);
		}
	}

	public void runAnimations(ExteriorBlockEntity exterior) {
		if (exterior.getAnimation() == null) return;

		exterior.getAnimation().setupAnimation(this.state);
		exterior.getAnimation().tellClientsToSetup(this.state);
	}

	public void setDestination(AbsoluteBlockPos.Directed pos, boolean withChecks) {
		if (this.destination == null || Objects.equals(this.destination, pos)) return;

		WorldBorder border = this.destination.getWorld().getWorldBorder();

		this.destination = border.contains(this.destination)
						? pos : new AbsoluteBlockPos.Directed(border.clamp(pos.getX(), pos.getY(), pos.getZ()),
						pos.getDimension(), pos.getRotation());
		this.tardis().flight().recalculate();

		if (withChecks)
			this.checkDestination(CHECK_LIMIT, PropertiesHandler.getBool(this.tardis().properties(), PropertiesHandler.FIND_GROUND));
	}

	public void setDestination(AbsoluteBlockPos.Directed pos) {
		this.setDestination(pos, true);
	}

	/**
	 * Sets the position of the tardis based off the flight's progress to the destination
	 *
	 * @param source Where this tardis originally took off from
	 */
	public void setPositionToProgress(AbsoluteBlockPos.Directed source) {
		if (this.getState() != State.FLIGHT)
			return;

		AbsoluteBlockPos.Directed pos = FlightUtil.getPositionFromPercentage(
				source, this.getDestination(),
				this.tardis().flight()
						.getDurationAsPercentage()
		);

		this.setPosition(pos);
	}

	public void setPositionToProgress() {
		this.setPositionToProgress(this.getPosition());
	}

	public AbsoluteBlockPos.Directed getDestination() {
		if (this.destination == null) {
			if (this.getPosition() != null)
				this.destination = this.getPosition();
			else {
				AITMod.LOGGER.error("Destination error! resetting to 0 0 0 in overworld");
				this.destination = new AbsoluteBlockPos.Directed(0, 0, 0, TardisUtil.findWorld(World.OVERWORLD), 0);
			}
		}

		return destination;
	}


	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public void placeExterior() {
		this.position.setBlockState(AITBlocks.EXTERIOR_BLOCK.getDefaultState()
				.with(ExteriorBlock.ROTATION, DirectionControl.getGeneralizedRotation(this.position.getRotation()))
				.with(ExteriorBlock.LEVEL_9, 0));

		this.position.addBlockEntity(new ExteriorBlockEntity(
				this.position, this.position.getBlockState(), this.tardis
		));
	}

	public void deleteExterior() {
		this.getPosition().getWorld().removeBlock(this.getPosition(), false);

		if (this.isServer())
			ForcedChunkUtil.stopForceLoading((ServerWorld) this.getPosition().getWorld(), this.getPosition());
	}

	@NotNull
	public SoundEvent getSoundForCurrentState() {
		if (this.tardis() != null) {
			if (this.isCrashing()) {
				return AITSounds.GHOST_MAT;
			}

			return this.tardis().getExterior().getVariant().getSound(this.getState()).sound();
		}

		return SoundEvents.INTENTIONALLY_EMPTY;
	}

	public MatSound getMatSoundForCurrentState() {
		if (this.tardis() != null) {
			if (this.isCrashing()) {
				return AITSounds.GHOST_MAT_ANIM;
			}

			return this.tardis().getExterior().getVariant().getSound(this.getState());
		}

		return AITSounds.LANDED_ANIM;
	}

	public enum State {
		LANDED(true) {
			@Override
			public void onEnable(TravelContext context) {
				AITMod.LOGGER.info("ON: LANDED");
			}

			@Override
			public void onDisable(TravelContext context) {
				AITMod.LOGGER.info("OFF: LANDED");
			}

			@Override
			public State getNext() {
				return DEMAT;
			}
		},
		DEMAT {
			@Override
			public void onEnable(TravelContext context) {
				AITMod.LOGGER.info("ON: DEMAT");
			}

			@Override
			public void onDisable(TravelContext context) {
				AITMod.LOGGER.info("OFF: DEMAT");
			}

			@Override
			public State getNext() {
				return FLIGHT;
			}
		},
		FLIGHT(true) {
			@Override
			public void onEnable(TravelContext context) {
				AITMod.LOGGER.info("ON: FLIGHT");
			}

			@Override
			public void onDisable(TravelContext context) {
				AITMod.LOGGER.info("OFF: LANDED");
			}

			@Override
			public State getNext() {
				return MAT;
			}
		},
		MAT {
			@Override
			public void onEnable(TravelContext context) {
				AITMod.LOGGER.info("ON: MAT");
			}

			@Override
			public void onDisable(TravelContext context) {
				AITMod.LOGGER.info("OFF: LANDED");
			}

			@Override
			public State getNext() {
				return LANDED;
			}
		},
		CRASH(true) {
			@Override
			public void onEnable(TravelContext context) {
				AITMod.LOGGER.info("ON: CRASH");
			}

			@Override
			public void onDisable(TravelContext context) {
				AITMod.LOGGER.info("OFF: LANDED");
			}

			@Override
			public State getNext() {
				return LANDED;
			}
		};
		private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		private final boolean isStatic;

		State(boolean isStatic) {
			this.isStatic = isStatic;
		}

		State() {
			this(false);
		}

		public boolean isStatic() {
			return isStatic;
		}

		public abstract void onEnable(TravelContext context);

		public abstract void onDisable(TravelContext context);

		public abstract State getNext();

		public void next(TravelContext context) {
			this.service.shutdown();
			this.onDisable(context);

			State next = this.getNext();
			next.schedule(context);

			next.onEnable(context);
			context.travel().setState(next);
		}

		public void schedule(TravelContext context) {
		}
	}
}
