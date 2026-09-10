package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.api.tardis.TardisComponent;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.handler.CloakHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CloakControl extends Control {
    public static final Identifier ID = AITMod.id("protocol_3");

    public CloakControl() {
        // ⬚ ?
        super(ID);
    }

    @Override
    public Text getName(Tardis tardis) {
        return Text.translatable(tardis.cloak().silent().get()
                ? "control.ait.protocol_3_silent_active"
                : "control.ait.protocol_3_silent_inactive");
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        CloakHandler cloak = tardis.handler(TardisComponent.Id.CLOAK);
        boolean wasCloaked = cloak.cloaked().get();

        if (leftClick && wasCloaked) {
            boolean wasSilent = cloak.silent().get();
            cloak.silent().set(!wasSilent);

            player.sendMessage(Text.translatable("control.ait.protocol_3_silent_" + (wasSilent ? "deactivated" : "activated")), true);
        } else {
            cloak.cloaked().set(!wasCloaked);

            cloak.silent().set(false);
        }

        return cloak.cloaked().get() ? Result.SUCCESS : Result.SUCCESS_ALT;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return SoundEvents.INTENTIONALLY_EMPTY;
    }

    @Override
    protected SubSystem.IdLike requiredSubSystem() {
        return SubSystem.Id.CHAMELEON;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		CloakHandler cloak = tardis.handler(TardisComponent.Id.CLOAK);
		return cloak.cloaked().get() ? 1.0f : 0.0f;
	}
}
