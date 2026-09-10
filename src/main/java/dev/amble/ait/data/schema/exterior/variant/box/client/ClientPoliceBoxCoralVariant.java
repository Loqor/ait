package dev.amble.ait.data.schema.exterior.variant.box.client;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.exteriors.BedrockExteriorModel;
import dev.amble.ait.client.models.exteriors.ExteriorModel;
import dev.amble.ait.core.tardis.handler.BiomeHandler;
import dev.amble.ait.data.datapack.exterior.BiomeOverrides;
import dev.amble.lib.client.bedrock.BedrockModelRegistry;
import net.minecraft.util.Identifier;

public class ClientPoliceBoxCoralVariant extends ClientPoliceBoxVariant {
    protected static final Identifier BIOME_IDENTIFIER = new Identifier(AITMod.MOD_ID, CATEGORY_PATH + "/biome" + "/police_box_coral.png");
    public ClientPoliceBoxCoralVariant() {
        super("coral");
    }

    private final BiomeOverrides OVERRIDES = BiomeOverrides.builder(ClientPoliceBoxVariant.OVERRIDES)
            .with(type -> type.getTexture(BIOME_IDENTIFIER), BiomeHandler.BiomeType.SANDY).build();

    @Override
    public ExteriorModel model() {
        return new BedrockExteriorModel(BedrockModelRegistry.getInstance().get(AITMod.id("coral_exterior")));
    }

    @Override
    public BiomeOverrides overrides() {
        return OVERRIDES;
    }
}
