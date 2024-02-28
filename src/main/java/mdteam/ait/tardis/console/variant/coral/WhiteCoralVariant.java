package mdteam.ait.tardis.console.variant.coral;

import mdteam.ait.AITMod;
import mdteam.ait.tardis.console.type.CoralType;
import mdteam.ait.tardis.console.variant.ConsoleVariantSchema;
import net.minecraft.util.Identifier;

public class WhiteCoralVariant extends ConsoleVariantSchema {
	public static final Identifier REFERENCE = new Identifier(AITMod.MOD_ID, "console/coral_white");

	public WhiteCoralVariant() {
		super(CoralType.REFERENCE, REFERENCE);
	}
}
