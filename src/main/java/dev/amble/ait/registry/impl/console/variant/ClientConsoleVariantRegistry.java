package dev.amble.ait.registry.impl.console.variant;

import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.consoles.BedrockConsoleModel;
import dev.amble.ait.client.models.consoles.ConsoleModel;
import dev.amble.ait.data.datapack.DatapackConsole;
import dev.amble.ait.data.schema.console.ClientConsoleVariantSchema;
import dev.amble.ait.data.schema.console.ConsoleVariantSchema;
import dev.amble.ait.data.schema.console.variant.alnico.client.ClientAlnicoVariant;
import dev.amble.ait.data.schema.console.variant.alnico.client.ClientBlueAlnicoVariant;
import dev.amble.ait.data.schema.console.variant.copper.client.ClientCopperVariant;
import dev.amble.ait.data.schema.console.variant.coral.client.*;
import dev.amble.ait.data.schema.console.variant.crystalline.client.ClientCrystallineVariant;
import dev.amble.ait.data.schema.console.variant.crystalline.client.ClientCrystallineZeitonVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.HartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.client.ClientHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.client.ClientKeltHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.client.ClientMintHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.client.ClientWoodenHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hudolin.client.*;
import dev.amble.ait.data.schema.console.variant.renaissance.client.*;
import dev.amble.ait.data.schema.console.variant.steam.client.*;
import dev.amble.ait.data.schema.console.variant.toyota.client.ClientToyotaBlueVariant;
import dev.amble.ait.data.schema.console.variant.toyota.client.ClientToyotaLegacyVariant;
import dev.amble.ait.data.schema.console.variant.toyota.client.ClientToyotaVariant;
import dev.amble.lib.client.bedrock.BedrockModelRegistry;
import dev.amble.lib.register.datapack.DatapackRegistry;

public class ClientConsoleVariantRegistry extends DatapackRegistry<ClientConsoleVariantSchema> {
    private static ClientConsoleVariantRegistry INSTANCE;

    public static DatapackRegistry<ClientConsoleVariantSchema> getInstance() {
        if (INSTANCE == null) {
            AITMod.LOGGER.info("ClientConsoleVariantRegistry was not initialized, Creating a new instance");
            INSTANCE = new ClientConsoleVariantRegistry();
        }

        return INSTANCE;
    }

    /**
     * Will return the clients version of a servers door variant
     *
     * @return the first variant found as there should only be one client version
     */
    public static ClientConsoleVariantSchema withParent(ConsoleVariantSchema parent) {
        for (ClientConsoleVariantSchema schema : ClientConsoleVariantRegistry.getInstance().toList()) {
            if (schema.parent() == null)
                continue;

            if (schema.parent().id().equals(parent.id()))
                return schema;
        }

        return null;
    }

    public static ClientConsoleVariantSchema withSameParent(ClientConsoleVariantSchema schema) {
        for (ClientConsoleVariantSchema s : ClientConsoleVariantRegistry.getInstance().toList()) {
            if (s.parent() == null)
                continue;

            if (schema.equals(s))
                continue;

            if (s.parent().parentId().equals(schema.parent().parentId()))
                return s;
        }

        return null;
    }

    @Override
    public ClientConsoleVariantSchema fallback() {
        return REGISTRY.get(HartnellVariant.REFERENCE);
    }

    @Override
    public void syncToClient(ServerPlayerEntity player) {
    }

    @Override
    public void readFromServer(PacketByteBuf buf) {
        int size = buf.readInt();

        for (int i = 0; i < size; i++) {
            this.register(convertDatapack(buf.decodeAsJson(DatapackConsole.CODEC)));
        }

        AITMod.LOGGER.info("Read {} client console variants from server", size);
    }

    public static ClientConsoleVariantSchema convertDatapack(DatapackConsole variant) {
        if (!variant.wasDatapack())
            return convertNonDatapack(variant);

        return new ClientConsoleVariantSchema(variant.id()) {
            private ClientConsoleVariantSchema sameParent; // a variant with the same parent as this one, so we have the same models n that
	        private @Nullable Identifier texture = null;
	        private @Nullable Identifier emission = null;

            private ClientConsoleVariantSchema getSameParent() {
                if (sameParent == null) {
                    sameParent = withSameParent(this);
                }

                return sameParent;
            }

	        private ClientConsoleVariantSchema getSameParent(Predicate<ClientConsoleVariantSchema> predicate) {
		        ConsoleVariantSchema thisParent = this.parent();
		        if (thisParent == null)
			        return null;

		        for (ClientConsoleVariantSchema schema : ClientConsoleVariantRegistry.getInstance().toList()) {
			        if (schema == this)
				        continue;

			        if (schema.parent() == null)
				        continue;

			        if (schema.parent().parentId().equals(thisParent.parentId()) && predicate.test(schema))
				        return schema;
		        }

		        return null;
	        }

            @Override
            public Identifier texture() {
	            if (texture == null) {
		            if (variant.texture().equals(DatapackConsole.EMPTY)) {
			            ClientConsoleVariantSchema parent = getSameParent(val -> val.parent() instanceof DatapackConsole dp && !dp.texture().equals(DatapackConsole.EMPTY));
			            texture = parent != null ? parent.texture() : variant.texture();
		            } else {
			            texture = variant.texture();
		            }
	            }

	            return texture;
            }

            @Override
            public Identifier emission() {
	            if (emission == null) {
		            if (variant.emission().equals(DatapackConsole.EMPTY)) {
			            ClientConsoleVariantSchema parent = getSameParent(val -> val.parent() instanceof DatapackConsole dp && !dp.emission().equals(DatapackConsole.EMPTY));
			            emission = parent != null ? parent.emission() : variant.emission();
		            } else {
			            emission = variant.emission();
		            }
	            }

	            return emission;
            }

            @Override
            public ConsoleModel model() {
                if (variant.model().isPresent()) {
                    return new BedrockConsoleModel(BedrockModelRegistry.getInstance().get(variant.model().get()));
                }

	            ClientConsoleVariantSchema parent = getSameParent(val -> val.parent() instanceof DatapackConsole dp && dp.model().isPresent());
	            if (parent != null) {
		            return parent.model();
	            }

                AITMod.LOGGER.warn("Datapack console variant {} has no model set, falling back to Hartnell model", variant.id());

	            return HARTNELL.model();
            }

            @Override
            public float[] sonicItemRotations() {
                if (variant.sonicRotation().isEmpty()) {
                    if (getSameParent() == null) return new float[]{0, 0};

                    return getSameParent().sonicItemRotations();
                }

                float[] result = new float[2];

                for (int i = 0; i < 2; i++) {
                    result[i] = variant.sonicRotation().get(i);
                }

                return result;
            }

            @Override
            public Vector3f sonicItemTranslations() {
                if (variant.sonicTranslation().equals(0,0,0)) {
                    if (getSameParent() == null) return new Vector3f(0,0,0);

                    return getSameParent().sonicItemTranslations();
                }

                return variant.sonicTranslation();
            }

            @Override
            public float[] handlesRotations() {
                if (variant.handlesRotation().isEmpty()) {
                    if (getSameParent() == null) return new float[]{0, 0};

                    return getSameParent().handlesRotations();
                }

                float[] result = new float[2];

                for (int i = 0; i < 2; i++) {
                    result[i] = variant.handlesRotation().get(i);
                }

                return result;
            }

            @Override
            public Vector3f handlesTranslations() {
                if (variant.handlesTranslation().equals(0,0,0)) {
                    if (getSameParent() == null) return new Vector3f(0,0,0);

                    return getSameParent().handlesTranslations();
                }

                return variant.handlesTranslation();
            }
        };
    }

    private static ClientConsoleVariantSchema convertNonDatapack(DatapackConsole variant) {
        if (variant.wasDatapack())
            return convertDatapack(variant);

        return getInstance().get(variant.id());
    }

    public static ClientConsoleVariantSchema HARTNELL;
    public static ClientConsoleVariantSchema HARTNELL_WOOD;
    public static ClientConsoleVariantSchema HARTNELL_KELT;
    public static ClientConsoleVariantSchema HARTNELL_MINT;
    public static ClientConsoleVariantSchema CORAL_GREEN;
    public static ClientConsoleVariantSchema CORAL_BLUE;
    public static ClientConsoleVariantSchema CORAL_WHITE;
    public static ClientConsoleVariantSchema CORAL_DECAYED;
    public static ClientConsoleVariantSchema CORAL_SITH;
    public static ClientConsoleVariantSchema TOYOTA;
    public static ClientConsoleVariantSchema TOYOTA_BLUE;
    public static ClientConsoleVariantSchema TOYOTA_LEGACY;
    public static ClientConsoleVariantSchema ALNICO;
    public static ClientConsoleVariantSchema ALNICO_BLUE;
    public static ClientConsoleVariantSchema STEAM;
    public static ClientConsoleVariantSchema STEAM_CHERRY;
    public static ClientConsoleVariantSchema STEAM_STEEL;
    public static ClientConsoleVariantSchema STEAM_GILDED;
    public static ClientConsoleVariantSchema STEAM_PLAYPAL;
    public static ClientConsoleVariantSchema STEAM_COPPER;
    public static ClientConsoleVariantSchema HUDOLIN;
    public static ClientConsoleVariantSchema HUDOLIN_NATURE;
    public static ClientConsoleVariantSchema HUDOLIN_TALL;
    public static ClientConsoleVariantSchema HUDOLIN_SHORT;
    public static ClientConsoleVariantSchema HUDOLIN_SHALKA;
    public static ClientConsoleVariantSchema COPPER;
    public static ClientConsoleVariantSchema CRYSTALLINE;
    public static ClientConsoleVariantSchema CRYSTALLINE_ZEITON;
    public static ClientConsoleVariantSchema RENAISSANCE;
    public static ClientConsoleVariantSchema RENAISSANCE_FIRE;
    public static ClientConsoleVariantSchema RENAISSANCE_TOKAMAK;
    public static ClientConsoleVariantSchema RENAISSANCE_IDENTITY;
    public static ClientConsoleVariantSchema RENAISSANCE_INDUSTRIOUS;
    //public static ClientConsoleVariantSchema HOURGLASS;



    @Override
    public void onClientInit() {
        // Hartnell variants
        HARTNELL = register(new ClientHartnellVariant());
        HARTNELL_KELT = register(new ClientKeltHartnellVariant());
        HARTNELL_MINT = register(new ClientMintHartnellVariant());
        HARTNELL_WOOD = register(new ClientWoodenHartnellVariant());

        // Coral variants
        CORAL_GREEN = register(new ClientGreenCoralVariant());
        CORAL_BLUE = register(new ClientBlueCoralVariant());
        CORAL_WHITE = register(new ClientWhiteCoralVariant());
        CORAL_DECAYED = register(new ClientCoralDecayedVariant());
        CORAL_SITH = register(new ClientCoralSithVariant());

        // Toyota variants
        TOYOTA = register(new ClientToyotaVariant());
        TOYOTA_BLUE = register(new ClientToyotaBlueVariant());
        TOYOTA_LEGACY = register(new ClientToyotaLegacyVariant());

        // Alnico variants
        ALNICO = register(new ClientAlnicoVariant());
        ALNICO_BLUE = register(new ClientBlueAlnicoVariant());

        // Steam variants
        STEAM = register(new ClientSteamVariant());
        STEAM_CHERRY = register(new ClientSteamCherryVariant());
        STEAM_STEEL = register(new ClientSteamSteelVariant());
        STEAM_GILDED = register(new ClientSteamGildedVariant());
        STEAM_COPPER = register(new ClientSteamCopperVariant());
        STEAM_PLAYPAL = register(new ClientSteamPlaypalVariant());

        // Hudolin variants
        HUDOLIN = register(new ClientHudolinVariant());
        HUDOLIN_NATURE = register(new ClientHudolinNatureVariant());
        HUDOLIN_TALL = register(new ClientHudolinTallVariant());
        HUDOLIN_SHORT = register(new ClientHudolinShortVariant());
        HUDOLIN_SHALKA = register(new ClientHudolinShalkaVariant());

        // Copper variants
        COPPER = register(new ClientCopperVariant());

        // Crystalline variants
        CRYSTALLINE = register(new ClientCrystallineVariant());
        CRYSTALLINE_ZEITON = register(new ClientCrystallineZeitonVariant());

        // Renaissance variants
        RENAISSANCE = register(new ClientRenaissanceVariant());
        RENAISSANCE_TOKAMAK = register(new ClientRenaissanceTokamakVariant());
        RENAISSANCE_FIRE = register(new ClientRenaissanceFireVariant());
        RENAISSANCE_IDENTITY = register(new ClientRenaissanceIdentityVariant());
        RENAISSANCE_INDUSTRIOUS = register(new ClientRenaissanceIndustriousVariant());

        // Hourglass variants
        //HOURGLASS = register(new ClientHourglassVariant());
    }
}
