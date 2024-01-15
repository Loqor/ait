package mdteam.ait.tardis.handler;

import mdteam.ait.AITMod;
import mdteam.ait.tardis.util.TardisUtil;
import mdteam.ait.tardis.util.AbsoluteBlockPos;
import mdteam.ait.tardis.util.SerialDimension;
import mdteam.ait.tardis.wrapper.server.manager.ServerTardisManager;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.TardisTickable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.UUID;

// todo rename all "Handler" to "Data" - eg FuelHander -> FuelData, makes more sense, no?
public abstract class TardisLink implements TardisTickable {

    protected UUID tardisId;

    public TardisLink(UUID tardisId) {
        this.tardisId = tardisId;
    }

    public Tardis getTardis() {
        if (isClient()) {
            AITMod.LOGGER.error("Client side tardis should not be accessed!");
            throw new RuntimeException("Client side tardis should not be accessed!");
        }
        return ServerTardisManager.getInstance().getTardis(tardisId);
    }

    @Override
    public void tick(ServerWorld world) {
        // Implementation of the server-side tick logic
    }

    @Override
    public void tick(MinecraftServer server) {
        // Implementation of the server-side tick logic
    }

    @Override
    public void tick(MinecraftClient client) {
        // Implementation of the client-side tick logic
    }

    @Override
    public void startTick(MinecraftServer server) {
        // Implementation of the server-side tick logic when it starts
    }

    public AbsoluteBlockPos.Directed getDoorPos() {
        Tardis tardis = getTardis();
        return tardis != null && tardis.getDesktop() != null ?
                tardis.getDesktop().getInteriorDoorPos() :
                new AbsoluteBlockPos.Directed(0, 0, 0, new SerialDimension(World.OVERWORLD.getValue().toString()), Direction.NORTH);
    }

    public AbsoluteBlockPos.Directed getExteriorPos() {
        Tardis tardis = getTardis();
        return tardis != null && tardis.getTravel() != null ?
                tardis.getTravel().getPosition() :
                new AbsoluteBlockPos.Directed(0, 0, 0, new SerialDimension(World.OVERWORLD.getValue().toString()), Direction.NORTH);
    }

    public static boolean isClient() {
        return TardisUtil.isClient();
    }

    public static boolean isServer() {
        return TardisUtil.isServer();
    }
}
