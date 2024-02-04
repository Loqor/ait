package mdteam.ait.registry;

import mdteam.ait.AITMod;
import mdteam.ait.core.AITSounds;
import mdteam.ait.tardis.TardisDesktopSchema;
import mdteam.ait.tardis.sound.CreakSound;
import mdteam.ait.tardis.sound.HumSound;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Random;

// do i really need a registry for this?? no, but also YES.
public class CreakRegistry {
    public static final SimpleRegistry<CreakSound> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<CreakSound>ofRegistry(new Identifier(AITMod.MOD_ID, "creak"))).buildAndRegister();
    public static CreakSound register(CreakSound schema) {
        return Registry.register(REGISTRY, schema.id(), schema);
    }
    public static CreakSound getRandomCreak() {
        Random rnd = new Random();
        int randomized = rnd.nextInt(Math.abs(REGISTRY.size()));
        return (CreakSound) REGISTRY.stream().toArray()[randomized];
    }

    public static CreakSound ONE;
    public static CreakSound TWO;
    public static CreakSound THREE;
    // Concerned that 4-7 may be too quiet
    public static CreakSound FOUR;
    public static CreakSound FIVE;
    public static CreakSound SIX;
    public static CreakSound SEVEN;
    public static CreakSound CAVE;
    public static CreakSound WHISPER;

    public static void init() {
        ONE = register(CreakSound.create(AITMod.MOD_ID, "one", AITSounds.CREAK_ONE));
        TWO = register(CreakSound.create(AITMod.MOD_ID, "two", AITSounds.CREAK_TWO));
        THREE = register(CreakSound.create(AITMod.MOD_ID, "three", AITSounds.CREAK_THREE));
        FOUR = register(CreakSound.create(AITMod.MOD_ID, "four", AITSounds.CREAK_FOUR));
        FIVE = register(CreakSound.create(AITMod.MOD_ID, "five", AITSounds.CREAK_FIVE));
        SIX = register(CreakSound.create(AITMod.MOD_ID, "six", AITSounds.CREAK_SIX));
        SEVEN = register(CreakSound.create(AITMod.MOD_ID, "seven", AITSounds.CREAK_SEVEN));

        CAVE = register(CreakSound.create(AITMod.MOD_ID, "cave", SoundEvents.AMBIENT_CAVE.value()));
        WHISPER = register(CreakSound.create(AITMod.MOD_ID, "whisper", AITSounds.WHISPER));
    }
}
