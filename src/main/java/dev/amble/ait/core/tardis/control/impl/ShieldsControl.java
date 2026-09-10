package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.api.tardis.TardisComponent;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.handler.ShieldHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ShieldsControl extends Control {
    public static final Identifier ID = AITMod.id("shields");

    public ShieldsControl() {
        super(ID);
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        ShieldHandler shields = tardis.handler(TardisComponent.Id.SHIELDS);

        if (leftClick) {
            if (shields.shielded().get())
                shields.toggleVisuals();
        } else {
            shields.toggle();
            if (shields.visuallyShielded().get())
                shields.disableVisuals();
        }
        return leftClick ? Result.SUCCESS_ALT :  Result.SUCCESS;
    }

    @Override
    protected SubSystem.IdLike requiredSubSystem() {
        return SubSystem.Id.SHIELDS;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.SHIELDS;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		ShieldHandler shields = tardis.handler(TardisComponent.Id.SHIELDS);
		float visual = shields.visuallyShielded().get() ? 1.0f : 0.0f;
		float active = shields.shielded().get() ? 1.0f : 0.0f;
		return (visual + active) / 2.0F;
	}
}
