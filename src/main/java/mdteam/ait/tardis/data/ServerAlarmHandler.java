package mdteam.ait.tardis.data;

import mdteam.ait.core.AITSounds;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.TardisTravel;
import mdteam.ait.tardis.data.properties.PropertiesHandler;
import mdteam.ait.tardis.util.TardisUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;

import java.util.UUID;

// use this as reference for starting other looping sounds on the exterior
public class ServerAlarmHandler extends TardisLink {
    // fixme this is bad bad but i cant be assed with packets and thinking so hardcoding this value will be okay for now
    public static final int CLOISTER_LENGTH_TICKS = 3 * 20;
    private int soundCounter = 0; // decides when to start the next cloister sound

    public ServerAlarmHandler(Tardis tardis) {
        super(tardis, "alarm");
    }

    public void enable() {
        this.set(true);
    }

    public void disable() {
        this.set(false);
    }
    private void set(boolean var) {
        if (this.getTardis().isEmpty()) return;
        PropertiesHandler.set(this.getTardis().get(), PropertiesHandler.ALARM_ENABLED, var);
    }

    public boolean isEnabled() {
        if (this.getTardis().isEmpty()) return false;
        return PropertiesHandler.getBool(this.getTardis().get().getHandlers().getProperties(), PropertiesHandler.ALARM_ENABLED);
    }

    public void toggle() {
        if (isEnabled()) disable();
        else enable();
    }

    @Override
    public void tick(MinecraftServer server) {
        super.tick(server);

        // @TODO make a new control that makes it (by default) detect hostile entities in the interior plus a check when it's been cleared of all hostile entities - Loqor
        if(!isEnabled() && getTardis().isPresent()) {
            for (Entity entity : TardisUtil.getEntitiesInInterior(getTardis().get(), 200)) {
                if (entity instanceof HostileEntity && !entity.hasCustomName()) {
                    this.enable();
                }
            }
            return;
        }

        if (getTardis().isEmpty()) return;
        if (getTardis().get().getTravel().getState() == TardisTravel.State.FLIGHT) return;

        soundCounter++;

        if (soundCounter >= CLOISTER_LENGTH_TICKS) {
            soundCounter = 0;
            getExteriorPos().getWorld().playSound(null, getExteriorPos(), AITSounds.CLOISTER, SoundCategory.AMBIENT, 0.5f, 0.5f);
        }
    }
}
