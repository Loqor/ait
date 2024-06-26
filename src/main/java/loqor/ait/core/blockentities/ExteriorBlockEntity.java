package loqor.ait.core.blockentities;

import loqor.ait.compat.DependencyChecker;
import loqor.ait.core.AITBlockEntityTypes;
import loqor.ait.core.blocks.ExteriorBlock;
import loqor.ait.core.item.KeyItem;
import loqor.ait.core.item.SiegeTardisItem;
import loqor.ait.core.item.SonicItem;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.TardisTravel;
import loqor.ait.tardis.animation.ExteriorAnimation;
import loqor.ait.tardis.base.TardisComponent;
import loqor.ait.tardis.data.DoorData;
import loqor.ait.tardis.data.InteriorChangingHandler;
import loqor.ait.tardis.data.SonicHandler;
import loqor.ait.tardis.link.v2.AbstractLinkableBlockEntity;
import loqor.ait.tardis.link.v2.TardisRef;
import loqor.ait.tardis.util.TardisUtil;
import loqor.ait.tardis.wrapper.server.ServerTardis;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static loqor.ait.tardis.TardisTravel.State.*;

public class ExteriorBlockEntity extends AbstractLinkableBlockEntity implements BlockEntityTicker<ExteriorBlockEntity> {
	public int animationTimer = 0;
	public final AnimationState DOOR_STATE = new AnimationState();
	private ExteriorAnimation animation;

	public ExteriorBlockEntity(BlockPos pos, BlockState state) {
		super(AITBlockEntityTypes.EXTERIOR_BLOCK_ENTITY_TYPE, pos, state);
	}

	public ExteriorBlockEntity(BlockPos pos, BlockState state, Tardis tardis) {
		this(pos, state);
		this.link(tardis);
	}

	public void useOn(ServerWorld world, boolean sneaking, PlayerEntity player) {
		if (this.tardis().isEmpty() || player == null)
			return;

		ServerTardis tardis = (ServerTardis) this.tardis().get();

		if (tardis.isGrowth())
			return;

		SonicHandler handler = this.tardis().get().sonic();
		boolean hasSonic = handler.hasSonic(SonicHandler.HAS_EXTERIOR_SONIC);
		boolean shouldEject = player.isSneaking();

		if (player.getMainHandStack().getItem() instanceof KeyItem
				&& !tardis.siege().isActive()
				&& !tardis.<InteriorChangingHandler>handler(TardisComponent.Id.INTERIOR).isGenerating()) {
			ItemStack key = player.getMainHandStack();
			NbtCompound tag = key.getOrCreateNbt();
			if (!tag.contains("tardis")) {
				return;
			}
			if (Objects.equals(tardis.getUuid().toString(), tag.getString("tardis"))) {
				DoorData.toggleLock(tardis, (ServerPlayerEntity) player);
			} else {
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.BLOCKS, 1F, 0.2F);
				player.sendMessage(Text.translatable("tardis.key.identity_error"), true); //TARDIS does not identify with key
			}
			return;
		}

		if (hasSonic) {
			if (shouldEject) {
				player.giveItemStack(handler.get(SonicHandler.HAS_EXTERIOR_SONIC));
				handler.clear(false, SonicHandler.HAS_EXTERIOR_SONIC, null);
				handler.clearSonicMark(SonicHandler.HAS_EXTERIOR_SONIC);
				world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1F, 0.2F);
				return;
			}

			player.sendMessage(Text.translatable("tardis.exterior.sonic.repairing").append(Text.literal(": " + tardis.crash().getRepairTicksAsSeconds() + "s").formatted(Formatting.BOLD, Formatting.GOLD)), true);
			return;
		}

		if (player.getMainHandStack().getItem() instanceof SonicItem &&
				!tardis.siege().isActive() &&
				!tardis.<InteriorChangingHandler>handler(TardisComponent.Id.INTERIOR).isGenerating()
				&& tardis.getDoor().isClosed()
				&& tardis.crash().getRepairTicks() > 0) {
			ItemStack sonic = player.getMainHandStack();
			NbtCompound tag = sonic.getOrCreateNbt();
			if (!tag.contains("tardis"))
				return;

			if (Objects.equals(tardis.getUuid().toString(), tag.getString("tardis"))) {
				ItemStack stack = player.getMainHandStack();

				if (!(stack.getItem() instanceof SonicItem))
					return;

				handler.set(stack, true, SonicHandler.HAS_EXTERIOR_SONIC, this.pos);
				handler.markHasSonic(SonicHandler.HAS_EXTERIOR_SONIC);

				player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
				world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1F, 0.2F);
			} else {
				world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1F, 0.2F);
				player.sendMessage(Text.translatable("tardis.tool.cannot_repair"), true); //Unable to repair TARDIS with current tool!
			}

			return;
		}

		if (sneaking && tardis.siege().isActive() && !tardis.isSiegeBeingHeld()) {
			SiegeTardisItem.pickupTardis(tardis, (ServerPlayerEntity) player);
			return;
		}

		if ((tardis.travel().getState() == LANDED
				|| tardis.travel().getState() == CRASH)) {
			DoorData.useDoor(tardis, (ServerWorld) this.getWorld(), this.getPos(), (ServerPlayerEntity) player);
		}
	}

	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
	}

	public void onEntityCollision(Entity entity) {
		TardisRef optional = this.tardis();

		if (optional.isEmpty())
			return;

		Tardis tardis = optional.get();
		boolean previouslyLocked = tardis.getDoor().previouslyLocked();

		if (!previouslyLocked && tardis.travel().getState() == MAT && this.getAlpha() >= 0.9f)
			TardisUtil.teleportInside(tardis, entity);

		if (!tardis.getDoor().isOpen())
			return;

		if (!tardis.getLockedTardis() && (!DependencyChecker.hasPortals() || !tardis.getExterior().getVariant().hasPortals()))
			TardisUtil.teleportInside(tardis, entity);
	}

	@Override
	public void tick(World world, BlockPos pos, BlockState blockState, ExteriorBlockEntity blockEntity) {
		TardisRef optional = this.tardis();

		if (optional == null || optional.isEmpty())
			return;

		Tardis tardis = optional.get();

		TardisTravel travel = tardis.travel();
		TardisTravel.State state = travel.getState();

		/*if (state == LANDED && tardis.getExteriorPos() != null && !world.isClient()) {
			((ExteriorBlock) blockState.getBlock()).tryFall(blockState, (ServerWorld) world, pos);
		}*/

		if (state != LANDED)
			this.getAnimation().tick();

		if (world.isClient()) {
			this.checkAnimations();
			this.exteriorLightBlockState(tardis);
			return;
		}

		// ensures we don't exist during flight
		if (state == FLIGHT)
			world.removeBlock(this.getPos(), false);
	}

	public void verifyAnimation() {
		TardisRef optional = this.tardis();

		if (this.animation != null || optional == null || optional.isEmpty())
			return;

		Tardis tardis = optional.get();

		this.animation = tardis.getExterior().getVariant().animation(this);
		this.animation.setupAnimation(tardis.travel().getState());

		if (this.getWorld() != null && !this.getWorld().isClient()) {
			this.animation.tellClientsToSetup(tardis.travel().getState());
		}
	}

	public void checkAnimations() {
		// DO NOT RUN THIS ON SERVER!!
		if (this.tardis().isEmpty())
			return;

		animationTimer++;
		Tardis tardis = this.tardis().get();

		DoorData door = tardis.getDoor();

		DoorData.DoorStateEnum doorState = door.getDoorState();
		DoorData.DoorStateEnum animState = door.getAnimationExteriorState();

		if (animState == null)
			return;

		if (animState != doorState) {
			DOOR_STATE.start(animationTimer);
			door.tempExteriorState = doorState;
		}
	}

	public ExteriorAnimation getAnimation() {
		this.verifyAnimation();
		return this.animation;
	}

	public float getAlpha() {
		return this.getAnimation().getAlpha();
	}

	private void exteriorLightBlockState(Tardis tardis) {
		TardisTravel.State state = tardis.travel().getState();

		if (state == TardisTravel.State.DEMAT || state == TardisTravel.State.MAT) {
			int light = (int) Math.max(1, Math.min(this.getAlpha() * 9.0f, 9));
			this.getWorld().setBlockState(pos, this.getCachedState().with(ExteriorBlock.LEVEL_9, light));
		}
	}

	public void onBroken() {
		this.tardis().ifPresent(tardis -> tardis.travel().setState(TardisTravel.State.FLIGHT));
	}
}