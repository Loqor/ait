package loqor.ait.tardis.console.variant.steam;

import loqor.ait.AITMod;
import loqor.ait.tardis.console.type.SteamType;
import loqor.ait.core.data.schema.console.ConsoleVariantSchema;
import loqor.ait.tardis.data.loyalty.Loyalty;
import net.minecraft.util.Identifier;

public class SteamCherryVariant extends ConsoleVariantSchema {
	public static final Identifier REFERENCE = new Identifier(AITMod.MOD_ID, "console/steam_cherry");

	public SteamCherryVariant() {
		super(SteamType.REFERENCE, REFERENCE, new Loyalty(Loyalty.Type.PILOT));
	}
}
