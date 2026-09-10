package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.util.SafePosSearch;
import dev.amble.ait.data.schema.console.variant.coral.*;
import dev.amble.ait.data.schema.console.variant.renaissance.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class LandTypeControl extends Control {
    public LandTypeControl() {
        super(AITMod.id("land_type"));
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        if (leftClick) {
            tardis.travel().horizontalSearch().flatMap(value -> {
                value = !value;
                messageXPlayer(player, value);
                return value;
            });

            return Result.SUCCESS_ALT;
        }

        tardis.travel().verticalSearch().flatMap(value -> {
            value = value.next();
            messageYPlayer(player, value);
            return value;
        });

        return Result.SUCCESS;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.LAND_TYPE;
    }

    public void messageYPlayer(ServerPlayerEntity player, SafePosSearch.Kind value) {
        player.sendMessage(Text.translatable("message.ait.control.ylandtype", value.text()), true);
    }

    public void messageXPlayer(ServerPlayerEntity player, boolean var) {
        Text on = Text.translatable("message.ait.control.xlandtype.on");
        Text off = Text.translatable("message.ait.control.xlandtype.off");
        player.sendMessage(var ? on : off, true);
    }

    private boolean isRenaissanceVariant(ConsoleBlockEntity consoleBlockEntity) {
        return consoleBlockEntity.getVariant() instanceof RenaissanceTokamakVariant ||
                consoleBlockEntity.getVariant() instanceof RenaissanceVariant ||
                consoleBlockEntity.getVariant() instanceof RenaissanceIdentityVariant ||
                consoleBlockEntity.getVariant() instanceof RenaissanceIndustriousVariant ||
                consoleBlockEntity.getVariant() instanceof RenaissanceFireVariant;
    }

    private boolean isCoralVariant(ConsoleBlockEntity consoleBlockEntity) {
        return consoleBlockEntity.getVariant() instanceof CoralVariant ||
                consoleBlockEntity.getVariant() instanceof WhiteCoralVariant ||
                consoleBlockEntity.getVariant() instanceof CoralSithVariant ||
                consoleBlockEntity.getVariant() instanceof BlueCoralVariant ||
                consoleBlockEntity.getVariant() instanceof CoralDecayedVariant;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		boolean horizontal = tardis.travel().horizontalSearch().get();
		SafePosSearch.Kind vertical = tardis.travel().verticalSearch().get();

		float horizontalProgress = horizontal ? 1.0f : 0.0f;
		float verticalProgress = (float) vertical.ordinal() / (float) (SafePosSearch.Kind.values().length - 1);
		return (horizontalProgress + verticalProgress) / 2.0f;
	}
}
