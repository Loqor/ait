package mdteam.ait.tardis.exterior;

import mdteam.ait.AITMod;
import net.minecraft.util.Identifier;

public class CapsuleExterior extends ExteriorSchema {
    public static final Identifier REFERENCE = new Identifier(AITMod.MOD_ID, "exterior/capsule");

    public CapsuleExterior() {
        super(REFERENCE, "capsule");
    }

    @Override
    public boolean hasPortals() {
        return true;
    }
}
