package loqor.ait.tardis.control.impl;

import io.wispforest.owo.ops.WorldOps;
import loqor.ait.core.AITSounds;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.base.TardisComponent;
import loqor.ait.tardis.control.Control;
import loqor.ait.tardis.data.ShieldData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class ShieldsControl extends Control {

	private SoundEvent soundEvent = AITSounds.HANDBRAKE_LEVER_PULL;

	public ShieldsControl() {
		super("shields");
	}

	@Override
	public boolean runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console) {
		if (tardis.sequence().hasActiveSequence() && tardis.sequence().controlPartOfSequence(this)) {
			this.addToControlSequence(tardis, player, console);
			return false;
		}

		ShieldData shields = tardis.handler(TardisComponent.Id.SHIELDS);

		if (player.isSneaking()) {
			if (shields.areShieldsActive())
				shields.toggleVisuals();
		} else {
			shields.toggle();

			if (shields.areVisualShieldsActive())
				shields.disableVisuals();
		}

		this.soundEvent = player.isSneaking() ? SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME : AITSounds.HANDBRAKE_LEVER_PULL;

		if (tardis.getExteriorPos() != null)
			WorldOps.updateIfOnServer(world, tardis.getExteriorPos());

		return true;
	}

	@Override
	public SoundEvent getSound() {
		return this.soundEvent;
	}
}
