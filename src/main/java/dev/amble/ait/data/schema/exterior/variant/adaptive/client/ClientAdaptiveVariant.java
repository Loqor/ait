package dev.amble.ait.data.schema.exterior.variant.adaptive.client;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.exteriors.BedrockExteriorModel;
import dev.amble.ait.client.models.exteriors.ExteriorModel;
import dev.amble.ait.data.datapack.exterior.BiomeOverrides;
import dev.amble.ait.data.schema.exterior.ClientExteriorVariantSchema;
import dev.amble.lib.client.bedrock.BedrockModelRegistry;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public class ClientAdaptiveVariant extends ClientExteriorVariantSchema {

    protected static final String CATEGORY_PATH = "textures/blockentities/exteriors/adaptive";

    protected static final BiomeOverrides OVERRIDES = BiomeOverrides.EMPTY;

    public ClientAdaptiveVariant() {
        super(AITMod.id("exterior/adaptive"));
    }

    @Override
    public ExteriorModel model() {
        return new BedrockExteriorModel(BedrockModelRegistry.getInstance().get(AITMod.id("tt_capsule")));
    }

    @Override
    public Identifier texture() {
        return AITMod.id(CATEGORY_PATH + ".png");
    }

    @Override
    public Identifier emission() {
        return null;
    }

    @Override
    public Vector3f sonicItemTranslations() {
        return new Vector3f(0.5f, 1.2f, 1.15f);
    }

    @Override
    public BiomeOverrides overrides() {
        return OVERRIDES;
    }
}
