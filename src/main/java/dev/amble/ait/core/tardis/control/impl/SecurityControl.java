package dev.amble.ait.core.tardis.control.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.manager.old.DeprecatedServerTardisManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITItems;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.item.KeyItem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.util.TardisUtil;

public class SecurityControl extends Control {

    public SecurityControl() {
        // ⨷ ?
        super(AITMod.id("protocol_19"));
    }

    public static boolean cannotAccess(ServerTardis tardis, ServerPlayerEntity player) {
        if (!tardis.hasWorld() || tardis.world() != player.getServerWorld())
            return true; // To verify the packet is coming from a player in the TARDIS' dimension

        return tardis.stats().security().get() && !SecurityControl.hasMatchingKey(player, tardis);
    }

    public static DeprecatedServerTardisManager.Receiver withLoyaltyCheck(DeprecatedServerTardisManager.Receiver receiver) {
        return (tardis, server, player, handler, buf, sender) -> {
              if (cannotAccess(tardis, player)) return;
              receiver.receive(tardis, server, player, handler, buf, sender);
        };
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        if (!hasMatchingKey(player, tardis))
            return Result.FAILURE;

        boolean security = tardis.stats().security().get();
        tardis.stats().security().set(!security);
        return security ? Result.SUCCESS : Result.SUCCESS_ALT;
    }

    public static void runSecurityProtocols(Tardis tardis) {
        boolean security = tardis.stats().security().get();
        boolean isDiscShouldLeave = tardis.waypoint().isDisc() && !tardis.waypoint().canContainPlayers();
        boolean leaveBehind = tardis.travel().leaveBehind().get() || isDiscShouldLeave;

        if (!security && !isDiscShouldLeave)
            return;

        List<ServerPlayerEntity> forRemoval = new ArrayList<>();

        if (leaveBehind) {
            for (ServerPlayerEntity player : tardis.asServer().world().getPlayers()) {
                if (isDiscShouldLeave || !hasMatchingKey(player, tardis)) {
                    forRemoval.add(player);
                }
            }

            for (ServerPlayerEntity player : forRemoval) {
                TardisUtil.teleportOutside(tardis, player);
            }
        }
    }

    public static boolean hasMatchingKey(ServerPlayerEntity player, Tardis tardis) {
        if (player.hasPermissionLevel(2))
            return true;

        if (!tardis.loyalty().get(player).isOf(tardis.permissions().p19Loyalty().get()))
            return false;

        if (!KeyItem.isKeyInInventory(player))
            return false;

        Collection<ItemStack> keys = KeyItem.getKeysInInventory(player);

        for (ItemStack stack : keys) {
            Tardis found = KeyItem.getTardisStatic(player.getWorld(), stack);

            if (stack.getItem() == AITItems.SKELETON_KEY)
                return true;

            if (found == tardis)
                return true;
        }

        return false;
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.PROTOCOL_19;
    }

    @Override
    public long getDelayLength(Tardis tardis) {
        return 50;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		return tardis.stats().security().get() ? 1.0f : 0.0f;
	}
}
