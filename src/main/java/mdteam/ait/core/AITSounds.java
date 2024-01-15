package mdteam.ait.core;

import com.neptunedevelopmentteam.neptunelib.core.init_handlers.NeptuneSoundEventInit;
import mdteam.ait.AITMod;
import mdteam.ait.core.sounds.MatSound;
import mdteam.ait.core.sounds.SoundRegistryContainer;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class AITSounds implements NeptuneSoundEventInit {
    public static final SoundEvent SECRET_MUSIC = SoundEvent.of(new Identifier(AITMod.MOD_ID, "secret_music"));
    public static final SoundEvent EVEN_MORE_SECRET_MUSIC = SoundEvent.of(new Identifier(AITMod.MOD_ID, "even_more_secret_music"));

    // TARDIS
    public static final SoundEvent DEMAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/demat"));
    public static final SoundEvent MAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/mat"));
    public static final SoundEvent HOP_DEMAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/hop_takeoff"));
    public static final SoundEvent HOP_MAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/hop_land"));
    public static final SoundEvent FAIL_DEMAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/fail_takeoff"));
    public static final SoundEvent FAIL_MAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/fail_land"));
    public static final SoundEvent EMERG_MAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/emergency_land"));
    public static final SoundEvent FLIGHT_LOOP = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/flight_loop"));
    public static final SoundEvent LAND_THUD = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/land_thud"));
    public static final SoundEvent SHUTDOWN = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/console_shutdown"));
    public static final SoundEvent POWERUP = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/console_powerup"));

    public static final SoundEvent SIEGE_ENABLE = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/siege_enable"));
    public static final SoundEvent SIEGE_DISABLE = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/siege_disable"));

    public static final SoundEvent EIGHT_DEMAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/eighth_demat"));
    public static final SoundEvent EIGHT_MAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/eighth_mat"));
    public static final SoundEvent GHOST_MAT = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/ghost_mat"));

    // Controls
    public static final SoundEvent DEMAT_LEVER_PULL = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/demat_lever_pull"));
    public static final SoundEvent HANDBRAKE_LEVER_PULL = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/handbrake_lever_pull"));
    public static final SoundEvent HANDBRAKE_UP = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/handbrake_up"));
    public static final SoundEvent HANDBRAKE_DOWN = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/handbrake_down"));
    public static final SoundEvent CRANK = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/crank"));
    public static final SoundEvent KNOCK = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/knock"));
    public static final SoundEvent SNAP = SoundEvent.of(new Identifier(AITMod.MOD_ID, "controls/snap"));

    // Hums
    public static final SoundEvent TOYOTA_HUM = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/hums/toyota_hum"));
    public static final SoundEvent CORAL_HUM = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/hums/coral_hum"));

    public static final SoundEvent CLOISTER = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/cloister"));

    // Creaks
    public static final SoundEvent CREAK_ONE = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/creaks/creak_one"));
    public static final SoundEvent CREAK_TWO = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/creaks/creak_two"));
    public static final SoundEvent CREAK_THREE = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/creaks/creak_three"));
    public static final SoundEvent WHISPER = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/creaks/whisper"));

    // Tools
    public static final SoundEvent DING = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tools/goes_ding"));


    // FIXME: move somwehre else + these values suck
    public static final MatSound DEMAT_ANIM = new MatSound(DEMAT, 240, 240, 240, 0.2f, 0.4f); // fixme especially this one it flickers bad
    public static final MatSound MAT_ANIM = new MatSound(MAT, 460, 240, 240, 0.2f, 0.4f);
    public static final MatSound FLIGHT_ANIM = new MatSound(FLIGHT_LOOP, 120, 60, 60, 0, 0);
    public static final MatSound EIGHT_DEMAT_ANIM = new MatSound(EIGHT_DEMAT, 8 * 20, 8 * 20, 8 * 20, 0.1f, 0.3f);
    public static final MatSound EIGHT_MAT_ANIM = new MatSound(EIGHT_MAT, 11 * 20, 11 * 20, 9 * 20, 0.2f, 0.4f);
    public static final MatSound GHOST_MAT_ANIM = new MatSound(GHOST_MAT, 590, 320, 320, 0.2f, 0.4f);
    public static final MatSound LANDED_ANIM = new MatSound(null, 0, 0, 0, 0, 0);

    // Secret
    public static final SoundEvent DOOM_DOOR_OPEN = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/secret/doom_door_open"));
    public static final SoundEvent DOOM_DOOR_CLOSE = SoundEvent.of(new Identifier(AITMod.MOD_ID, "tardis/secret/doom_door_close"));
}
