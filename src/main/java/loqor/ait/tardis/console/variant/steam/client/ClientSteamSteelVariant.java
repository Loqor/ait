package loqor.ait.tardis.console.variant.steam.client;

import loqor.ait.AITMod;
import loqor.ait.client.models.consoles.ConsoleModel;
import loqor.ait.client.models.consoles.SteamConsoleModel;
import loqor.ait.core.data.schema.console.ClientConsoleVariantSchema;
import loqor.ait.tardis.console.variant.steam.SteamCherryVariant;
import loqor.ait.tardis.console.variant.steam.SteamSteelVariant;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public class ClientSteamSteelVariant extends ClientConsoleVariantSchema {
	public static final Identifier TEXTURE = new Identifier(AITMod.MOD_ID, ("textures/blockentities/consoles/steam_console_steel.png"));
	public static final Identifier EMISSION = new Identifier(AITMod.MOD_ID, ("textures/blockentities/consoles/steam_console_steel_emission.png"));

	public ClientSteamSteelVariant() {
		super(SteamSteelVariant.REFERENCE, SteamSteelVariant.REFERENCE);
	}

	@Override
	public Identifier texture() {
		return TEXTURE;
	}

	@Override
	public Identifier emission() {
		return EMISSION;
	}

	@Override
	public ConsoleModel model() {
		return new SteamConsoleModel(SteamConsoleModel.getTexturedModelData().createModel());
	}

	@Override
	public Vector3f sonicItemTranslations() {
		return new Vector3f(0.9f, 1.125f, -0.19f);
	}

	@Override
	public float[] sonicItemRotations() {
		return new float[]{30f, 120f};
	}
}
