package dev.amble.ait.data.schema.exterior.variant.exclusive.wanderer.client;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.exteriors.BedrockExteriorModel;
import dev.amble.ait.client.models.exteriors.ExteriorModel;
import dev.amble.ait.data.datapack.exterior.BiomeOverrides;
import dev.amble.ait.data.schema.exterior.ClientExteriorVariantSchema;
import dev.amble.lib.client.bedrock.BedrockModelRegistry;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public class ClientBoothWandererVariant extends ClientExteriorVariantSchema {

    protected static final String CATEGORY_PATH = "textures/blockentities/exteriors/exclusive/wanderer";
    protected static final String TEXTURE_PATH = CATEGORY_PATH + "/wanderer.png";
    protected static final String EMISSIVE_TEXTURE_PATH = CATEGORY_PATH + "/wanderer_emission.png";

    public ClientBoothWandererVariant() {
        super(AITMod.id("exterior/exclusive/wanderer"));
    }

    @Override
    public ExteriorModel model() {
        return new BedrockExteriorModel(BedrockModelRegistry.getInstance().get(AITMod.id("booth")));
    }

    @Override
    public Identifier texture() {
        return AITMod.id(TEXTURE_PATH);
    }

    @Override
    public Identifier emission() {
        return AITMod.id(EMISSIVE_TEXTURE_PATH);
    }

    @Override
    public Vector3f sonicItemTranslations() {
        return new Vector3f(0.845f, 1.125f, 1.05f);
    }

    @Override
    public BiomeOverrides overrides() {
        return null;
    }

    @Override
    public boolean hasTransparentDoors() {
        return true;
    }
}
