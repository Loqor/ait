package dev.amble.ait.data.schema.exterior.variant.classic.client;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.exteriors.ClassicHudolinExteriorModel;
import dev.amble.ait.client.models.exteriors.ExteriorModel;
import dev.amble.ait.client.models.exteriors.SimpleExteriorModel;
import dev.amble.ait.core.tardis.handler.BiomeHandler;
import dev.amble.ait.data.datapack.exterior.BiomeOverrides;
import net.minecraft.util.Identifier;

public class ClientClassicBoxHudolinVariant extends ClientClassicBoxVariant {
    protected static final Identifier BIOME_IDENTIFIER = new Identifier(AITMod.MOD_ID, CATEGORY_PATH + "/biome" + "/classic_hudolin.png");

    private final BiomeOverrides OVERRIDES = BiomeOverrides.builder(ClientClassicBoxVariant.OVERRIDES)
            .with(type -> type.getTexture(BIOME_IDENTIFIER),
                    BiomeHandler.BiomeType.SANDY,
                    BiomeHandler.BiomeType.RED_SANDY,
                    BiomeHandler.BiomeType.MUDDY,
                    BiomeHandler.BiomeType.CHERRY,
                    BiomeHandler.BiomeType.CHORUS,
                    BiomeHandler.BiomeType.SNOWY,
                    BiomeHandler.BiomeType.SCULK)
            .build();

    public ClientClassicBoxHudolinVariant() {
        super("hudolin");
    }

    @Override
    public ExteriorModel model() {
        return super.model();
    }

    @Override
    public BiomeOverrides overrides() {
        return OVERRIDES;
    }
}
