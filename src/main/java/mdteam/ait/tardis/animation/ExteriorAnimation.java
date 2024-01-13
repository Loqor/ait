package mdteam.ait.tardis.animation;

import mdteam.ait.core.blockentities.ExteriorBlockEntity;
import mdteam.ait.network.ServerAITNetworkManager;
import mdteam.ait.tardis.Tardis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.joml.Math;
import mdteam.ait.tardis.TardisTravel;

public abstract class ExteriorAnimation {
    private static final double MAX_CLOAK_DISTANCE = 5d;
    protected float alpha = 1;
    protected ExteriorBlockEntity exterior;
    protected int timeLeft, maxTime, startTime;

    public ExteriorAnimation(ExteriorBlockEntity exterior) {
        this.exterior = exterior;
    }

    // fixme bug that sometimes happens where server doesnt have animation
    protected void runAlphaChecks(TardisTravel.State state) {
        if (this.exterior.getWorld().isClient())
            return;

        if (alpha <= 0f && state == TardisTravel.State.DEMAT) {
            exterior.getTardis().getTravel().toFlight();
        }
        if (alpha >= 1f && state == TardisTravel.State.MAT) {
            exterior.getTardis().getTravel().forceLand(this.exterior);
        }
    }

    public float getAlpha() {
        if (this.timeLeft < 0) {
            this.setupAnimation(exterior.getTardis().getTravel().getState()); // fixme is a jank fix for the timeLeft going negative on client
            return 1f;
        }
        if (this.exterior.getTardis().getTravel().getState() == TardisTravel.State.LANDED && this.exterior.getTardis().getHandlers().getCloak().isEnabled()) {
            if (isServer())
                return 0.105f;
            else if (isNearTardis(MinecraftClient.getInstance().player, exterior.getTardis(), MAX_CLOAK_DISTANCE)) {
                return 1f - (float) (distanceFromTardis(MinecraftClient.getInstance().player, exterior.getTardis()) / MAX_CLOAK_DISTANCE);
            }
            return 0f;
        }

        return Math.clamp(0.0F, 1.0F, this.alpha);
    }

    private boolean isServer() {
        return !this.exterior.getWorld().isClient();
    }

    private static boolean isNearTardis(PlayerEntity player, Tardis tardis, double radius) {
        return radius >= distanceFromTardis(player, tardis);
    }
    private static double distanceFromTardis(PlayerEntity player, Tardis tardis) {
        BlockPos pPos = player.getBlockPos();
        BlockPos tPos = tardis.position();
        double distance = Math.sqrt(tPos.getSquaredDistance(pPos));
        return distance;
    }

    public abstract void tick();

    public abstract void setupAnimation(TardisTravel.State state);

    public void setAlpha(float alpha) {
        this.alpha = Math.clamp(0.0F, 1.0F, alpha);
    }

    public boolean hasAnimationStarted() {
        return this.timeLeft < this.startTime;
    }

    public void tellClientsToSetup(TardisTravel.State state) {
        if (exterior.getWorld() == null || exterior.getWorld().isClient() || exterior.getTardis() == null) return;
        ServerAITNetworkManager.sendExteriorAnimationUpdateSetup(exterior.getTardis().getUuid(), state);
    }

}