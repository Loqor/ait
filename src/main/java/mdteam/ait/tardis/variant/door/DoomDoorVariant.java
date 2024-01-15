package mdteam.ait.tardis.variant.door;

import mdteam.ait.AITMod;
import mdteam.ait.core.AITSounds;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class DoomDoorVariant extends DoorSchema {

    public static final Identifier REFERENCE = new Identifier(AITMod.MOD_ID, "door/doom");

    public DoomDoorVariant() {
        super(REFERENCE);
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public SoundEvent openSound() {
        return AITSounds.DOOM_DOOR_OPEN;
    }

    @Override
    public SoundEvent closeSound() {
        return AITSounds.DOOM_DOOR_CLOSE;
    }
}
