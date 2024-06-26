package loqor.ait.core.data.schema;

import loqor.ait.AITMod;
import net.minecraft.util.Identifier;

import static loqor.ait.AITMod.MOD_ID;

public class BuiltinSonic extends SonicSchema {

    private static final String prefix = "item/sonic/";

    private static final String[] MODES = new String[] {
            "inactive", "interaction", "overload", "scanning", "tardis"
    };

    protected BuiltinSonic(Identifier id, String name, Models models) {
        super(id, name, models, new Rendering());
    }

    public static BuiltinSonic create(String id, String name) {
        Identifier[] identifiers = new Identifier[MODES.length];

        for (int i = 0; i < MODES.length; i++) {
            identifiers[i] = new Identifier(MOD_ID, prefix + id + "/" + MODES[i]);
        }

        return new BuiltinSonic(new Identifier(AITMod.MOD_ID, id), name, new Models(
                identifiers[0], identifiers[1], identifiers[2], identifiers[3], identifiers[4]
        ));
    }
}
