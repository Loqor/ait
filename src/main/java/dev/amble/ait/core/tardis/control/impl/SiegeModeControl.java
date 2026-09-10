package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class SiegeModeControl extends Control {
    public static final Identifier ID = AITMod.id("protocol_1913");

    private static final Text ENABLED = Text.translatable("tardis.message.control.siege.enabled");
    private static final Text DISABLED = Text.translatable("tardis.message.control.siege.disabled");

    public SiegeModeControl() {
        super(ID);
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        if (tardis.travel().isCrashing() || tardis.travel().getState() != TravelHandlerBase.State.LANDED)
            return Result.FAILURE;

        tardis.siege().setActive(!tardis.siege().isActive());
        tardis.alarm().disable();
        player.sendMessage(tardis.siege().isActive() ? ENABLED : DISABLED, true);

        return tardis.siege().isActive() ? Result.SUCCESS : Result.SUCCESS_ALT;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.SIEGE;
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
    protected SubSystem.IdLike requiredSubSystem() {
        return SubSystem.Id.DESPERATION;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		return tardis.siege().isActive() ? 1.0f : 0.0f;
	}
}
