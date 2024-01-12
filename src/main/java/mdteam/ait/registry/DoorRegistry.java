package mdteam.ait.registry;

import mdteam.ait.AITMod;
import mdteam.ait.tardis.variant.door.*;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

public class DoorRegistry {
    public static final SimpleRegistry<DoorSchema> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<DoorSchema>ofRegistry(new Identifier(AITMod.MOD_ID, "door"))).buildAndRegister();
    public static DoorSchema register(DoorSchema schema) {
        return Registry.register(REGISTRY, schema.id(), schema);
    }

    public static DoorSchema TARDIM;
    public static DoorSchema CLASSIC;
    public static DoorSchema BOOTH;
    public static DoorSchema CAPSULE;
    public static DoorSchema BOX;
    public static DoorSchema BOX_CORAL;
    public static DoorSchema BOX_TOKAMAK;
    public static DoorSchema HEAD;
    public static DoorSchema GROWTH;
    public static DoorSchema DOOM;

    public static void init() {
        TARDIM = register(new TardimDoorVariant());
        CLASSIC = register(new ClassicDoorVariant());
        BOOTH = register(new BoothDoorVariant());
        CAPSULE = register(new CapsuleDoorVariant());
        BOX = register(new PoliceBoxDoorVariant());
        BOX_CORAL = register(new PoliceBoxCoralDoorVariant());
        BOX_TOKAMAK = register(new PoliceBoxTokamakDoorVariant());
        HEAD = register(new EasterHeadDoorVariant());
        GROWTH = register(new CoralGrowthDoorVariant());
        DOOM = register(new DoomDoorVariant());
    }
}
