package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.item.HammerItem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.handler.ExtraHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class HammerHangerControl extends Control {

    public HammerHangerControl() {
        super(AITMod.id("hammer_hanger"));
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console,
                             boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        ExtraHandler handler = tardis.extra();

        if ((leftClick || player.isSneaking()) && (handler.getConsoleHammer() != null)) {
            ItemStack item;

            item = handler.consoleHammerInserted();

            player.getInventory().offerOrDrop(item);
            handler.insertConsoleHammer(null);
            return Result.SUCCESS_ALT;
        }

        ItemStack stack = player.getMainHandStack();

        if (stack.getItem() instanceof HammerItem) {
            if (handler.getConsoleHammer() == null || handler.getConsoleHammer().isEmpty()) {
                handler.insertConsoleHammer(stack.copy());
                player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        return Result.SUCCESS;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return SoundEvents.BLOCK_CHAIN_HIT;
    }

    @Override
    public boolean requiresPower() {
        return false;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		ExtraHandler handler = tardis.extra();
		return (handler.getConsoleHammer() != null && !handler.getConsoleHammer().isEmpty()) ? 1.0f : 0.0f;
	}
}
