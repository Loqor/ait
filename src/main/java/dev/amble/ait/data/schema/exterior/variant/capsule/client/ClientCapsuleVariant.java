package dev.amble.ait.data.schema.exterior.variant.capsule.client;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.exteriors.BedrockExteriorModel;
import dev.amble.ait.client.models.exteriors.ExteriorModel;
import dev.amble.ait.core.tardis.handler.BiomeHandler;
import dev.amble.ait.data.datapack.exterior.BiomeOverrides;
import dev.amble.ait.data.schema.exterior.ClientExteriorVariantSchema;
import dev.amble.lib.client.bedrock.BedrockModelRegistry;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

// a useful class for creating tardim variants as they all have the same filepath you know
public abstract class ClientCapsuleVariant extends ClientExteriorVariantSchema {
    private final String name;
    protected static final String CATEGORY_PATH = "textures/blockentities/exteriors/capsule";
    protected static final Identifier CATEGORY_IDENTIFIER = new Identifier(AITMod.MOD_ID, CATEGORY_PATH + "/capsule.png");
    protected static final Identifier BIOME_IDENTIFIER = new Identifier(AITMod.MOD_ID, CATEGORY_PATH + "/biome" + "/capsule.png");
    protected static final String TEXTURE_PATH = CATEGORY_PATH + "/capsule_";

    protected static final BiomeOverrides OVERRIDES = BiomeOverrides.builder()
            .with(type -> type.getTexture(BIOME_IDENTIFIER), BiomeHandler.BiomeType.SNOWY,
                    BiomeHandler.BiomeType.CHORUS, BiomeHandler.BiomeType.CHERRY,
                    BiomeHandler.BiomeType.SANDY, BiomeHandler.BiomeType.RED_SANDY, BiomeHandler.BiomeType.MUDDY,
                    BiomeHandler.BiomeType.SCULK)
            .build();

    protected ClientCapsuleVariant(String name) {
        super(AITMod.id("exterior/capsule/" + name));

        this.name = name;
    }

    @Override
    public ExteriorModel model() {
        return new BedrockExteriorModel(BedrockModelRegistry.getInstance().get(AITMod.id("tt_capsule")));
    }

    @Override
    public Identifier texture() {
        return AITMod.id(TEXTURE_PATH + name + ".png");
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
