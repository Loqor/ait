package loqor.ait.core.item;

import loqor.ait.core.blockentities.ConsoleBlockEntity;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.TardisTravel;
import loqor.ait.tardis.data.FlightData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.Random;

public class HammerItem extends SwordItem {
	public HammerItem(int attackDamage, float attackSpeed, Settings settings) {
		super(ToolMaterials.IRON, attackDamage, attackSpeed, settings);
	}

	public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
		if (state.isOf(Blocks.IRON_BLOCK)) {
			return 15.0F;
		} else {
			return state.isIn(BlockTags.SWORD_EFFICIENT) ? 1.5F : 1.0F;
		}
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		if (world.getBlockEntity(pos) instanceof ConsoleBlockEntity consoleBlockEntity) {
			if (player == null)
				return ActionResult.PASS;

			if (consoleBlockEntity.tardis().isEmpty())
				return ActionResult.PASS;

			Tardis tardis = consoleBlockEntity.tardis().get();
			if (!(tardis.travel().getState() == TardisTravel.State.FLIGHT)) {
				world.playSound(null, consoleBlockEntity.getPos(),
						SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1f, 1.0f);
				return ActionResult.SUCCESS;
			}

			FlightData flightData = tardis.flight();
			int targetTicks = flightData.getTargetTicks();
			int current_flight_ticks = flightData.getFlightTicks();
			int added_flight_ticks = 500 * tardis.flight().speed().get();
			double current_fuel = tardis.fuel().getCurrentFuel();
			double max_fuel = tardis.fuel().getMaxFuel();

			if (tardis.tardisHammerAnnoyance > 0)
				added_flight_ticks -= (int) Math.round(added_flight_ticks * 0.1 * tardis.tardisHammerAnnoyance);

			double estimated_fuel_cost_for_hit = added_flight_ticks / 5.0;
			if (tardis.tardisHammerAnnoyance > 0)
				estimated_fuel_cost_for_hit += (150 * tardis.flight().speed().get() * tardis.tardisHammerAnnoyance) / 7.0;

			if (!world.isClient() && current_fuel + estimated_fuel_cost_for_hit > max_fuel) {
				tardis.travel().crash();
				tardis.fuel().setCurrentFuel(0.0);
				return ActionResult.SUCCESS;
			}

            flightData.setFlightTicks(Math.min(current_flight_ticks + added_flight_ticks, targetTicks));
			tardis.fuel().setCurrentFuel(current_fuel - estimated_fuel_cost_for_hit);
			tardis.tardisHammerAnnoyance++;

			if (!world.isClient() && shouldCrashTardis(tardis.tardisHammerAnnoyance)) {
				tardis.travel().crash();
			} else {
				world.playSound(null, consoleBlockEntity.getPos(),
						SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.25f * tardis.tardisHammerAnnoyance, 1.0f);
			}

			if (world.isClient())
				return ActionResult.PASS;

			((ServerWorld) world).spawnParticles(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5f, pos.getY() + 1.25,
					pos.getZ() + 0.5f, 5 * tardis.tardisHammerAnnoyance, 0, 0, 0, 0.1f * tardis.tardisHammerAnnoyance);
			((ServerWorld) world).spawnParticles(new DustColorTransitionParticleEffect(
							new Vector3f(0.75f, 0.75f, 0.75f), new Vector3f(0.1f, 0.1f, 0.1f), 1), pos.getX() + 0.5f, pos.getY() + 1.25,
					pos.getZ() + 0.5f, 5 * tardis.tardisHammerAnnoyance, 0, 0, 0, 0.1f * tardis.tardisHammerAnnoyance);

			world.playSound(null, consoleBlockEntity.getPos(),
					SoundEvents.ENTITY_GLOW_ITEM_FRAME_BREAK, SoundCategory.BLOCKS, 0.25f * tardis.tardisHammerAnnoyance, 1.0f);

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	public boolean shouldCrashTardis(int annoyance) {
		Random random = new Random();
		if (annoyance <= 3) {
			return false;
		}
		for (int i = 0; i < annoyance; i++) {
			if (random.nextInt(0, 10) == 1) {
				return true;
			}
		}
		return false;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		world.playSoundFromEntity(null, user, SoundEvents.ENTITY_DOLPHIN_HURT, SoundCategory.PLAYERS, 1f, 1f);
		return super.use(world, user, hand);
	}

	@Override
	public boolean isSuitableFor(BlockState state) {
		return state.isOf(Blocks.IRON_BLOCK);
	}
}
