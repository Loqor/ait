package dev.amble.ait.core.tardis.control.impl;

import dev.amble.ait.core.engine.SubSystem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import dev.amble.ait.AITMod;
import dev.amble.ait.api.tardis.link.LinkableItem;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.item.HandlesItem;
import dev.amble.ait.core.item.SonicItem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.sequences.SequenceHandler;
import dev.amble.ait.core.tardis.handler.ButlerHandler;

public class SonicPortControl extends Control {

    public SonicPortControl() {
        super(AITMod.id("sonic_port"));
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        ButlerHandler butler = tardis.butler();
        if (!(world.getBlockEntity(console) instanceof ConsoleBlockEntity consoleBlockEntity)) return Result.FAILURE;

        boolean hasSonicStored = !consoleBlockEntity.getSonicScrewdriver().isEmpty();
        boolean hasHandlesStored = butler.getHandles() != null;

        if ((leftClick || player.isSneaking()) && (hasSonicStored || hasHandlesStored)) {
            ItemStack item;
            if (hasSonicStored) {
                item = consoleBlockEntity.getSonicScrewdriver();
                consoleBlockEntity.setSonicScrewdriver(ItemStack.EMPTY);
            } else {
                item = butler.takeHandles();
            }

            if (item == null)
                return Result.FAILURE;

            player.getInventory().offerOrDrop(item);
            return Result.SUCCESS;
        }

        ItemStack stack = player.getMainHandStack();
        if (!((stack.getItem() instanceof SonicItem) || (stack.getItem() instanceof HandlesItem)))
            return Result.FAILURE;

        LinkableItem linker = (LinkableItem) stack.getItem();
        if (!linker.isLinked(stack) || player.isSneaking()) {
            linker.link(stack, tardis);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS,
                    1.0F, 1.0F);
            SequenceHandler.spawnControlParticles(world, Vec3d.ofBottomCenter(console).add(0.0, 1.2f, 0.0));
        }

        if (stack.getItem() instanceof HandlesItem) {
            if (hasHandlesStored || hasSonicStored)
                return Result.FAILURE;

            butler.insertHandles(stack, console);
            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        } else if (stack.getItem() instanceof SonicItem) {
            if (hasSonicStored || hasHandlesStored)
                return Result.FAILURE;

            consoleBlockEntity.setSonicScrewdriver(stack);
            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        }

        boolean hasSonic = !consoleBlockEntity.getSonicScrewdriver().isEmpty() || butler.getHandles() != null;
        return hasSonic ? Result.SUCCESS : Result.SUCCESS_ALT;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.SONIC_PORT;
    }

    @Override
    public boolean requiresPower() {
        return false;
    }

    @Override
    public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
        if (cooldown) return 1.0F;

        ButlerHandler butler = tardis.butler();

        boolean hasSonic = (console != null && console.getSonicScrewdriver() != null && !console.getSonicScrewdriver().isEmpty()) || butler.getHandles() != null;

        return hasSonic ? 1.0f : 0.0f;
    }

    @Override
    protected SubSystem.IdLike requiredSubSystem() {
        return null;
    }
}
