package dev.amble.ait.data.schema.exterior.variant.tardim.client;

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
public abstract class ClientTardimVariant extends ClientExteriorVariantSchema {
    private final String name;
    protected static final String CATEGORY_PATH = "textures/blockentities/exteriors/tardim";
    protected static final Identifier CATEGORY_IDENTIFIER = new Identifier(AITMod.MOD_ID,
            CATEGORY_PATH + "/tardim.png");
    protected static final Identifier BIOME_IDENTIFIER = new Identifier(AITMod.MOD_ID,CATEGORY_PATH + "/biome" + "/tardim.png");
    protected static final String TEXTURE_PATH = CATEGORY_PATH + "/tardim_";

    protected static final BiomeOverrides OVERRIDES = BiomeOverrides.builder()
            .with(type -> type.getTexture(BIOME_IDENTIFIER), BiomeHandler.BiomeType.SNOWY,
                    BiomeHandler.BiomeType.SCULK, BiomeHandler.BiomeType.CHORUS, BiomeHandler.BiomeType.CHERRY,
                    BiomeHandler.BiomeType.SANDY, BiomeHandler.BiomeType.RED_SANDY, BiomeHandler.BiomeType.MUDDY)
            .build();

    protected ClientTardimVariant(String name) {
        super(AITMod.id("exterior/tardim/" + name));

        this.name = name;
    }

    @Override
    public ExteriorModel model() {
        return new BedrockExteriorModel(BedrockModelRegistry.getInstance().get(AITMod.id("tardim")));
    }

    @Override
    public Identifier texture() {
        return AITMod.id(TEXTURE_PATH + name + ".png");
    }

    @Override
    public Identifier emission() {
        return AITMod.id(TEXTURE_PATH + name + "_emission" + ".png");
    }

    @Override
    public Vector3f sonicItemTranslations() {
        return new Vector3f(0.53f, 0.94f, 1.2f);
    }

    @Override
    public BiomeOverrides overrides() {
        return OVERRIDES;
    }
}
