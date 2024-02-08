package mdteam.ait.tardis.control.impl;

import mdteam.ait.tardis.control.Control;
import mdteam.ait.tardis.control.impl.pos.IncrementManager;
import mdteam.ait.tardis.util.AbsoluteBlockPos;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.TardisTravel;

public class RandomiserControl extends Control {

    public RandomiserControl() {
        super("randomiser");
    }

    @Override
    public boolean runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world) {
        TardisTravel travel = tardis.getTravel();

        randomiseDestination(tardis, 10);
        tardis.removeFuel((0.1d * IncrementManager.increment(tardis)) * (tardis.tardisHammerAnnoyance + 1));

        messagePlayer(player, travel);

        return true;
    }

    // fixme this is LAGGYYY @TODO
    public static AbsoluteBlockPos.Directed randomiseDestination(Tardis tardis, int limit) {
        TardisTravel travel = tardis.getTravel();
        int increment = IncrementManager.increment(tardis);
        AbsoluteBlockPos.Directed dest = travel.getDestination();
        ServerWorld world = (ServerWorld) dest.getWorld();

        BlockPos pos;
        int x, z;

        for (int i = 0; i <= limit; i++) {
            x = dest.getX() + ((world.random.nextBoolean()) ? world.random.nextInt(increment) : -world.random.nextInt(increment));
            z = dest.getZ() + ((world.random.nextBoolean()) ? world.random.nextInt(increment) : -world.random.nextInt(increment));
            pos = new BlockPos(x, dest.getY(), z);

            travel.setDestination(new AbsoluteBlockPos.Directed(pos, dest.getWorld(), dest.getDirection()), false);
            if (travel.checkDestination()) return travel.getDestination();
        }

        return travel.getPosition();
    }

    @Override
    public long getDelayLength() {
        return 2000L;
    }

    private void messagePlayer(ServerPlayerEntity player, TardisTravel travel) {
        AbsoluteBlockPos.Directed dest = travel.getDestination();
        Text text = Text.translatable("tardis.message.control.randomiser.destination").append(Text.literal(dest.getX() + " | " + dest.getY() + " | " + dest.getZ()));
        player.sendMessage(text, true);
    }
}
