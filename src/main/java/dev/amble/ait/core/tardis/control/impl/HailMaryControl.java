package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class HailMaryControl extends Control {
    public static final Identifier ID = AITMod.id("protocol_813");

    public HailMaryControl() {
        // ♡ ?
        super(ID);
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        tardis.stats().hailMary().set(!tardis.stats().hailMary().get());

        player.sendMessage(tardis.stats().hailMary().get()
                ? Text.translatable("tardis.message.control.hail_mary.engaged")
                : Text.translatable("tardis.message.control.hail_mary.disengaged"), true);

        return tardis.stats().hailMary().get() ? Result.SUCCESS_ALT : Result.SUCCESS;
    }

    @Override
    protected SubSystem.IdLike requiredSubSystem() {
        return SubSystem.Id.DESPERATION;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.HAIL_MARY;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		return tardis.stats().hailMary().get() ? 1.0f : 0.0f;
	}
}
