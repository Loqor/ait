package loqor.ait.tardis.exterior.variant.doom.client;

import loqor.ait.AITMod;
import loqor.ait.client.models.exteriors.DoomExteriorModel;
import loqor.ait.client.models.exteriors.ExteriorModel;
import loqor.ait.core.data.schema.exterior.ClientExteriorVariantSchema;
import loqor.ait.client.renderers.exteriors.DoomConstants;
import loqor.ait.tardis.data.BiomeHandler;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public class ClientDoomVariant extends ClientExteriorVariantSchema {
	public ClientDoomVariant() {
		super(new Identifier(AITMod.MOD_ID, "exterior/doom"));
	}


	@Override
	public ExteriorModel model() {
		return new DoomExteriorModel(DoomExteriorModel.getTexturedModelData().createModel());
	}

	@Override
	public Vector3f sonicItemTranslations() {
		return new Vector3f(0.5f, 1.5f, 0f);
	}

	@Override
	public Identifier texture() {
		return DoomConstants.DOOM_FRONT_BACK;
	}

	@Override
	public Identifier emission() {
		return DoomConstants.DOOM_TEXTURE_EMISSION;
	}

	@Override
	public Identifier getBiomeTexture(BiomeHandler.BiomeType biomeType) {
		/*return switch(biomeType) {
			case DEFAULT -> null;
			case SNOWY -> BiomeHandler.BiomeType.SNOWY.getTextureFromKey(CATEGORY_IDENTIFIER);
			case SCULK -> BiomeHandler.BiomeType.SCULK.getTextureFromKey(CATEGORY_IDENTIFIER);
			case SANDY -> BiomeHandler.BiomeType.SANDY.getTextureFromKey(CATEGORY_IDENTIFIER);
			case RED_SANDY -> BiomeHandler.BiomeType.RED_SANDY.getTextureFromKey(CATEGORY_IDENTIFIER);
			case MUDDY -> BiomeHandler.BiomeType.MUDDY.getTextureFromKey(CATEGORY_IDENTIFIER);
			case CHORUS -> BiomeHandler.BiomeType.CHORUS.getTextureFromKey(CATEGORY_IDENTIFIER);
			case CHERRY -> BiomeHandler.BiomeType.CHERRY.getTextureFromKey(CATEGORY_IDENTIFIER);
		};*/
		return null;
	}
}