package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class DoorControl extends Control {
    public static final Identifier ID = AITMod.id("door_control");

    public DoorControl() {
        super(ID);
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        tardis.door().interact(world, player.getBlockPos(), player);

        return tardis.door().isOpen() ? Result.SUCCESS : Result.SUCCESS_ALT;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.DOOR_CONTROL;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		// Door open progress
		float left = tardis.door().isLeftOpen() ? 1.0f : 0.0f;
		float right = tardis.door().isRightOpen() ? 1.0f : 0.0f;

		return (left + right) / 2.0f;
	}
}
