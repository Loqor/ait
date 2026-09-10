package dev.amble.ait.registry.impl.console.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import org.joml.Vector3f;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.util.Transformations;
import dev.amble.ait.data.datapack.DatapackConsole;
import dev.amble.ait.data.datapack.DatapackExterior;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.ait.data.schema.console.ConsoleVariantSchema;
import dev.amble.ait.data.schema.console.variant.alnico.AlnicoVariant;
import dev.amble.ait.data.schema.console.variant.alnico.BlueAlnicoVariant;
import dev.amble.ait.data.schema.console.variant.copper.CopperVariant;
import dev.amble.ait.data.schema.console.variant.coral.*;
import dev.amble.ait.data.schema.console.variant.crystalline.CrystallineVariant;
import dev.amble.ait.data.schema.console.variant.crystalline.CrystallineZeitonVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.HartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.KeltHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.MintHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hartnell.WoodenHartnellVariant;
import dev.amble.ait.data.schema.console.variant.hudolin.*;
import dev.amble.ait.data.schema.console.variant.renaissance.*;
import dev.amble.ait.data.schema.console.variant.steam.*;
import dev.amble.ait.data.schema.console.variant.toyota.ToyotaBlueVariant;
import dev.amble.ait.data.schema.console.variant.toyota.ToyotaLegacyVariant;
import dev.amble.ait.data.schema.console.variant.toyota.ToyotaVariant;
import dev.amble.lib.register.unlockable.UnlockableRegistry;

public class ConsoleVariantRegistry extends UnlockableRegistry<ConsoleVariantSchema> {
    private static ConsoleVariantRegistry INSTANCE;

    protected ConsoleVariantRegistry() {
        super(DatapackConsole::fromInputStream, null, "console_variants", "console", true, AITMod.MOD_ID);
    }

    public static ConsoleVariantSchema registerStatic(ConsoleVariantSchema schema) {
        return ConsoleVariantRegistry.getInstance().register(schema);
    }

    @Override
    public ConsoleVariantSchema fallback() {
        return null;
    }

    @Override
    public void syncToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(REGISTRY.size());

        for (ConsoleVariantSchema schema : REGISTRY.values()) {
            if (schema instanceof DatapackConsole variant) {
                buf.encodeAsJson(DatapackConsole.CODEC, variant);
                continue;
            }

            buf.encodeAsJson(DatapackConsole.CODEC, new DatapackConsole(schema.id(), Optional.of(schema.parentId()),
		            DatapackExterior.DEFAULT_TEXTURE, DatapackExterior.DEFAULT_TEXTURE, List.of(), new Vector3f(), List.of(), new Vector3f(), Optional.empty(), Transformations.DEFAULT, null, Optional.empty(), false));
        }

        ServerPlayNetworking.send(player, this.packet, buf);
    }

    @Override
    public void onCommonInit() {
        super.onCommonInit();
        this.defaults();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this);
    }

    @Override
    public void readFromServer(PacketByteBuf buf) {
        PacketByteBuf copy = PacketByteBufs.copy(buf);
        ClientConsoleVariantRegistry.getInstance().readFromServer(copy);

        REGISTRY.clear();

        this.defaults();
        int size = buf.readInt();

        for (int i = 0; i < size; i++) {
            DatapackConsole variant = buf.decodeAsJson(DatapackConsole.CODEC);
            if (!variant.wasDatapack())
                continue;

            this.register(variant);
        }

        AITMod.LOGGER.info("Read {} console variants from server", size);
    }

    public static ConsoleVariantRegistry getInstance() {
        if (INSTANCE == null) {
            AITMod.LOGGER.debug("ConsoleVariantRegistry was not initialized, Creating a new instance");
            INSTANCE = new ConsoleVariantRegistry();
        }

        return INSTANCE;
    }

    public static List<ConsoleVariantSchema> withParent(ConsoleTypeSchema parent) {
        List<ConsoleVariantSchema> list = new ArrayList<>();

        for (ConsoleVariantSchema schema : ConsoleVariantRegistry.getInstance().REGISTRY.values()) {
            if (schema.parent() == null) {
                AITMod.LOGGER.warn("Console variant {} has no parent, skipping", schema.id());
                continue;
            }

            if (schema.parent().equals(parent))
                list.add(schema);
        }

        return list;
    }

    public static ConsoleVariantSchema HARTNELL;
    public static ConsoleVariantSchema HARTNELL_WOOD;
    public static ConsoleVariantSchema HARTNELL_KELT;
    public static ConsoleVariantSchema HARTNELL_MINT;
    public static ConsoleVariantSchema CORAL;
    public static ConsoleVariantSchema CORAL_BLUE;
    public static ConsoleVariantSchema CORAL_WHITE;
    public static ConsoleVariantSchema CORAL_SITH;
    public static ConsoleVariantSchema CORAL_DECAYED;
    public static ConsoleVariantSchema TOYOTA;
    public static ConsoleVariantSchema TOYOTA_BLUE;
    public static ConsoleVariantSchema TOYOTA_LEGACY;
    public static ConsoleVariantSchema ALNICO;
    public static ConsoleVariantSchema ALNICO_BLUE;
    public static ConsoleVariantSchema STEAM;
    public static ConsoleVariantSchema STEAM_CHERRY;
    public static ConsoleVariantSchema STEAM_STEEL;
    public static ConsoleVariantSchema STEAM_GILDED;
    public static ConsoleVariantSchema STEAM_COPPER;
    public static ConsoleVariantSchema STEAM_PLAYPAL;
    public static ConsoleVariantSchema HUDOLIN;
    public static ConsoleVariantSchema HUDOLIN_NATURE;
    public static ConsoleVariantSchema HUDOLIN_TALL;
    public static ConsoleVariantSchema HUDOLIN_SHORT;
    public static ConsoleVariantSchema HUDOLIN_SHALKA;
    public static ConsoleVariantSchema COPPER;
    public static ConsoleVariantSchema BOREALIS;
    public static ConsoleVariantSchema CRYSTALLINE;
    public static ConsoleVariantSchema CRYSTALLINE_ZEITON;
    public static ConsoleVariantSchema RENAISSANCE;
    public static ConsoleVariantSchema RENAISSANCE_TOKAMAK;
    public static ConsoleVariantSchema RENAISSANCE_FIRE;
    public static ConsoleVariantSchema RENAISSANCE_IDENTITY;
    public static ConsoleVariantSchema RENAISSANCE_INDUSTRIOUS;
    //public static ConsoleVariantSchema HOURGLASS;


    @Override
    protected void defaults() {
        // Hartnell variants
        HARTNELL = registerStatic(new HartnellVariant());
        HARTNELL_KELT = registerStatic(new KeltHartnellVariant());
        HARTNELL_MINT = registerStatic(new MintHartnellVariant());
        HARTNELL_WOOD = registerStatic(new WoodenHartnellVariant()); // fixme this texture is awful - make tright remake
                                                                        // it

        // Coral variants
        CORAL = registerStatic(new CoralVariant());
        CORAL_BLUE = registerStatic(new BlueCoralVariant());
        CORAL_WHITE = registerStatic(new WhiteCoralVariant());
        CORAL_SITH = registerStatic(new CoralSithVariant());
        CORAL_DECAYED = registerStatic(new CoralDecayedVariant());

        // Toyota variants
        TOYOTA = registerStatic(new ToyotaVariant());
        TOYOTA_BLUE = registerStatic(new ToyotaBlueVariant());
        TOYOTA_LEGACY = registerStatic(new ToyotaLegacyVariant());

        // Alnico variants
        ALNICO = registerStatic(new AlnicoVariant());
        ALNICO_BLUE = registerStatic(new BlueAlnicoVariant());

        // Steam variants
        STEAM = registerStatic(new SteamVariant());
        STEAM_CHERRY = registerStatic(new SteamCherryVariant());
        STEAM_STEEL = registerStatic(new SteamSteelVariant());
        STEAM_GILDED = registerStatic(new SteamGildedVariant());
        STEAM_PLAYPAL = registerStatic(new SteamPlaypalVariant());
        STEAM_COPPER = registerStatic(new SteamCopperVariant());

        // Hudolin variants
        HUDOLIN = registerStatic(new HudolinVariant());
        HUDOLIN_NATURE = registerStatic(new HudolinNatureVariant());
        HUDOLIN_TALL = registerStatic(new HudolinTallVariant());
        HUDOLIN_SHORT = registerStatic(new HudolinShortVariant());
        HUDOLIN_SHALKA = registerStatic(new HudolinShalkaVariant());

        // Copper variants
        COPPER = registerStatic(new CopperVariant());

        // Borealis variants
        // BOREALIS = registerStatic(new BorealisVariant());

        // Crystalline variants
        CRYSTALLINE = registerStatic(new CrystallineVariant());
        CRYSTALLINE_ZEITON = registerStatic(new CrystallineZeitonVariant());

        // Renaissance variants
        RENAISSANCE = registerStatic(new RenaissanceVariant());
        RENAISSANCE_TOKAMAK = registerStatic(new RenaissanceTokamakVariant());
        RENAISSANCE_FIRE = registerStatic(new RenaissanceFireVariant());
        RENAISSANCE_IDENTITY = registerStatic(new RenaissanceIdentityVariant());
        RENAISSANCE_INDUSTRIOUS = registerStatic(new RenaissanceIndustriousVariant());

        // Hourglass variants
        //HOURGLASS = registerStatic(new HourglassVariant());

    }
}
