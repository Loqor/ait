package mdteam.ait.registry;

import mdteam.ait.AITMod;
import mdteam.ait.tardis.exterior.ExteriorSchema;
import mdteam.ait.tardis.variant.exterior.ExteriorVariantSchema;
import mdteam.ait.tardis.variant.exterior.booth.*;
import mdteam.ait.tardis.variant.exterior.box.*;
import mdteam.ait.tardis.variant.exterior.capsule.CapsuleDefaultVariant;
import mdteam.ait.tardis.variant.exterior.capsule.CapsuleFireVariant;
import mdteam.ait.tardis.variant.exterior.capsule.CapsuleSoulVariant;
import mdteam.ait.tardis.variant.exterior.classic.ClassicBoxDefinitiveVariant;
import mdteam.ait.tardis.variant.exterior.classic.ClassicBoxPrimeVariant;
import mdteam.ait.tardis.variant.exterior.classic.ClassicBoxPtoredVariant;
import mdteam.ait.tardis.variant.exterior.classic.ClassicBoxYetiVariant;
import mdteam.ait.tardis.variant.exterior.doom.DoomVariant;
import mdteam.ait.tardis.variant.exterior.easter_head.EasterHeadDefaultVariant;
import mdteam.ait.tardis.variant.exterior.easter_head.EasterHeadFireVariant;
import mdteam.ait.tardis.variant.exterior.easter_head.EasterHeadSoulVariant;
import mdteam.ait.tardis.variant.exterior.growth.CoralGrowthVariant;
import mdteam.ait.tardis.variant.exterior.plinth.PlinthDefaultVariant;
import mdteam.ait.tardis.variant.exterior.plinth.PlinthFireVariant;
import mdteam.ait.tardis.variant.exterior.plinth.PlinthSoulVariant;
import mdteam.ait.tardis.variant.exterior.tardim.TardimDefaultVariant;
import mdteam.ait.tardis.variant.exterior.tardim.TardimFireVariant;
import mdteam.ait.tardis.variant.exterior.tardim.TardimSoulVariant;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ExteriorVariantRegistry {
    public static final SimpleRegistry<ExteriorVariantSchema> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<ExteriorVariantSchema>ofRegistry(new Identifier(AITMod.MOD_ID, "exterior_variant"))).buildAndRegister();
    public static ExteriorVariantSchema register(ExteriorVariantSchema schema) {
        return Registry.register(REGISTRY, schema.id(), schema);
    }

    public static Collection<ExteriorVariantSchema> withParent(ExteriorSchema parent) {
        List<ExteriorVariantSchema> list = new ArrayList<>();

        for (Iterator<ExteriorVariantSchema> it = REGISTRY.iterator(); it.hasNext(); ) {
            ExteriorVariantSchema schema = it.next();
            //AITExteriors.iterator().forEach((System.out::println));

            if (schema.parent().equals(parent)) list.add(schema);
        }

        return list;
    }
    public static List<ExteriorVariantSchema> withParentToList(ExteriorSchema parent) {
        List<ExteriorVariantSchema> list = new ArrayList<>();

        for (Iterator<ExteriorVariantSchema> it = REGISTRY.iterator(); it.hasNext(); ) {
            ExteriorVariantSchema schema = it.next();
            if (schema.parent().equals(parent)) list.add(schema);
        }

        return list;
    }

    public static ExteriorVariantSchema TARDIM_DEFAULT;
    public static ExteriorVariantSchema TARDIM_FIRE;
    public static ExteriorVariantSchema TARDIM_SOUL;
    public static ExteriorVariantSchema BOX_DEFAULT;
    public static ExteriorVariantSchema BOX_FIRE;
    public static ExteriorVariantSchema BOX_SOUL;
    public static ExteriorVariantSchema BOX_FUTURE;
    public static ExteriorVariantSchema BOX_CORAL;
    public static ExteriorVariantSchema BOX_TOKAMAK;
    public static ExteriorVariantSchema PRIME;
    public static ExteriorVariantSchema YETI;
    public static ExteriorVariantSchema DEFINITIVE;
    public static ExteriorVariantSchema PTORED;
    public static ExteriorVariantSchema CAPSULE_DEFAULT;
    public static ExteriorVariantSchema CAPSULE_SOUL;
    public static ExteriorVariantSchema CAPSULE_FIRE;
    public static ExteriorVariantSchema BOOTH_DEFAULT;
    public static ExteriorVariantSchema BOOTH_FIRE;
    public static ExteriorVariantSchema BOOTH_SOUL;
    public static ExteriorVariantSchema BOOTH_VINTAGE;
    public static ExteriorVariantSchema BOOTH_BLUE;
    public static ExteriorVariantSchema COOB; // dont use : (
    public static ExteriorVariantSchema HEAD_DEFAULT;
    public static ExteriorVariantSchema HEAD_SOUL;
    public static ExteriorVariantSchema HEAD_FIRE;
    public static ExteriorVariantSchema CORAL_GROWTH;
    public static ExteriorVariantSchema DOOM;
    public static ExteriorVariantSchema PLINTH_DEFAULT;
    public static ExteriorVariantSchema PLINTH_SOUL;
    public static ExteriorVariantSchema PLINTH_FIRE;

    // AAAAAAAAAAAAAAAAAAAAAAAAAAA SO MANY VARIABLE
    public static void init() {
        // TARDIM
        TARDIM_DEFAULT = register(new TardimDefaultVariant());
        TARDIM_FIRE = register(new TardimFireVariant());
        TARDIM_SOUL = register(new TardimSoulVariant());

        // Police Box
        BOX_DEFAULT = register(new PoliceBoxDefaultVariant());
        BOX_SOUL = register(new PoliceBoxSoulVariant());
        BOX_FIRE = register(new PoliceBoxFireVariant());
        BOX_FUTURE = register(new PoliceBoxFuturisticVariant());
        BOX_CORAL = register(new PoliceBoxCoralVariant());
        BOX_TOKAMAK = register(new PoliceBoxTokamakVariant());

        // Classic Box
        PRIME = register(new ClassicBoxPrimeVariant());
        YETI = register(new ClassicBoxYetiVariant());
        DEFINITIVE = register(new ClassicBoxDefinitiveVariant());
        PTORED = register(new ClassicBoxPtoredVariant());

        // Capsule
        CAPSULE_DEFAULT = register(new CapsuleDefaultVariant());
        CAPSULE_SOUL = register(new CapsuleSoulVariant());
        CAPSULE_FIRE = register(new CapsuleFireVariant());

        // Booth
        BOOTH_DEFAULT = register(new BoothDefaultVariant());
        BOOTH_FIRE = register(new BoothFireVariant());
        BOOTH_SOUL = register(new BoothSoulVariant());
        BOOTH_VINTAGE = register(new BoothVintageVariant());
        BOOTH_BLUE = register(new BoothBlueVariant());

        // funny
        // COOB = register(new RedCoobVariant()); // fixme CUBE HAS BEEN REMOVED, REPEAT, CUBE HAS BEEN REMOVED. DO NOT PANIC!!

        // Easter Head
        HEAD_DEFAULT = register(new EasterHeadDefaultVariant());
        HEAD_SOUL = register(new EasterHeadSoulVariant());
        HEAD_FIRE = register(new EasterHeadFireVariant());

        // Coral Growth
        CORAL_GROWTH = register(new CoralGrowthVariant());

        // Doom
        DOOM = register(new DoomVariant());

        // Plinth
        PLINTH_DEFAULT = register(new PlinthDefaultVariant());
        PLINTH_SOUL = register(new PlinthSoulVariant());
        PLINTH_FIRE = register(new PlinthFireVariant());
    }
}
