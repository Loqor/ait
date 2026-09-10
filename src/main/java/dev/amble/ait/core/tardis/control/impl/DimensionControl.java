package dev.amble.ait.core.tardis.control.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.lock.LockedDimensionRegistry;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.pos.PosType;
import dev.amble.ait.core.tardis.handler.travel.TravelHandler;
import dev.amble.ait.core.tardis.util.AsyncLocatorUtil;
import dev.amble.ait.core.util.WorldUtil;
import dev.amble.lib.data.CachedDirectedGlobalPos;

public class DimensionControl extends Control {

    public static final Identifier ID = AITMod.id("dimension");

    public DimensionControl() {
        super(ID);
    }

    @Override
    public Result runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world, BlockPos console, boolean leftClick) {
        super.runServer(tardis, player, world, console, leftClick);

        TravelHandler travel = tardis.travel();
        CachedDirectedGlobalPos dest = travel.destination();
        List<ServerWorld> dims = WorldUtil.getTravelWorlds();

        if (dims.isEmpty()) {
            player.sendMessage(Text.translatableWithFallback("message.ait.tardis.control.dimension.none_available",
                    "No travel dimensions are currently allowed."), true);
            return Result.FAILURE;
        }

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            int index = Math.max(0, WorldUtil.travelWorldIndex(dest.getWorld()));

            if (leftClick) {
                index = (dims.size() + index - 1) % dims.size();
            } else {
                index = (index + 1) % dims.size();
            }

            return dims.get(index);
        }).thenAccept(destWorld -> {
            travel.destination(cached -> {
                CachedDirectedGlobalPos cachedPos = cached.world(destWorld);
                BlockPos clampedPos = PosType.clamp(cachedPos.getPos(), 0, destWorld);
                return cachedPos.pos(clampedPos);
            });
            messagePlayer(player, destWorld, LockedDimensionRegistry.getInstance().isUnlocked(tardis, destWorld));
        });

        AsyncLocatorUtil.LOCATING_EXECUTOR_SERVICE.submit(() -> future);
        return Result.SUCCESS;
    }

    private void messagePlayer(ServerPlayerEntity player, ServerWorld world, boolean unlocked) {
        MutableText message = Text.translatable("message.ait.tardis.control.dimension.info")
                .append(WorldUtil.worldText(world.getRegistryKey(), false)).formatted(unlocked ? Formatting.WHITE : Formatting.GRAY);

        if (!unlocked) message.append(Text.literal(" \uD83D\uDD12"));

        player.sendMessage(message, true);
    }

    @Override
    public SoundEvent getFallbackSound() {
        return AITSounds.DIMENSION;
    }

	@Override
	public float getTargetProgress(Tardis tardis, boolean cooldown, @Nullable ConsoleBlockEntity console) {
		// return selected dim / all dims
		CachedDirectedGlobalPos dest = tardis.travel().destination();
		int index = Math.max(0, WorldUtil.travelWorldIndex(dest.getWorld()));
		int total = WorldUtil.getTravelWorlds().size();
		if (total <= 1) return 0.0f;
		return (float) index / (float) (total - 1);
	}
}
