package mdteam.ait.registry;

import mdteam.ait.AITMod;
import mdteam.ait.tardis.control.impl.HandBrakeControl;
import mdteam.ait.tardis.control.impl.RefuelerControl;
import mdteam.ait.tardis.control.impl.pos.XControl;
import mdteam.ait.tardis.control.sequences.Sequence;
import mdteam.ait.tardis.util.FlightUtil;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

public class SequenceRegistry {
    public static final SimpleRegistry<Sequence> REGISTRY = FabricRegistryBuilder.createSimple(RegistryKey.<Sequence>ofRegistry(new Identifier(AITMod.MOD_ID, "sequence"))).buildAndRegister();
    public static Sequence register(Sequence schema) {
        return Registry.register(REGISTRY, schema.id(), schema);
    }

    public static void init() {
        register(Sequence.Builder.create(new Identifier(AITMod.MOD_ID, "takeoff"), (tardis -> {
            FlightUtil.travelTo(tardis, tardis.destination());
        }), new HandBrakeControl(), new RefuelerControl(), new XControl()));
    }
}
