package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.advancement.TardisCriterions;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PowerControl extends Control {

    public PowerControl() {
        super(AITMod.id("power"));
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);
        tardis.fuel().togglePower();

        if (tardis.fuel().hasPower()) {
            int power = (int) tardis.fuel().getCurrentFuel();
            boolean inRange = power >= 1500 && power <= 2017;
            boolean doorLocked = tardis.door().locked();
            boolean refueling = !tardis.isRefueling();

            if (inRange && doorLocked && refueling) {
                TardisCriterions.ATTACK_EYEBROWS.trigger(player);
                world.playSound(null, console, AITSounds.MAD_MAN_MUSIC, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }

        return tardis.fuel().hasPower() ? Result.SUCCESS : Result.SUCCESS_ALT;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.POWER_FLICK;
    }

    @Override
    public boolean requiresPower() {
        return false;
    }

    @Override
    public long getDelayLength(Tardis tardis) {
        return 200;
    }

    @Override
    public boolean shouldHaveDelay(Tardis tardis) {
        return !tardis.fuel().hasPower() && super.shouldHaveDelay();
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		return tardis.fuel().hasPower() ? 1.0f : 0.0f;
	}
}
