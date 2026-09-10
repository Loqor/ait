package dev.amble.ait.registry.impl;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.*;
import dev.amble.ait.core.tardis.control.impl.pos.IncrementControl;
import dev.amble.ait.core.tardis.control.impl.pos.XControl;
import dev.amble.ait.core.tardis.control.impl.pos.YControl;
import dev.amble.ait.core.tardis.control.impl.pos.ZControl;
import dev.amble.ait.core.tardis.control.impl.waypoint.LoadWaypointControl;
import dev.amble.ait.core.tardis.control.impl.waypoint.SaveWaypointControl;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ControlRegistry {

    public static final SimpleRegistry<Control> REGISTRY = FabricRegistryBuilder
            .createSimple(RegistryKey.<Control>ofRegistry(AITMod.id("control"))).buildAndRegister();

    public static Control register(Control control) {
        return Registry.register(REGISTRY, control.id(), control);
    }

    /**
     * Finds a control by its ID ( name ) NOT its Identifier
     *
     * @param id
     *            the id to look for
     * @return the control found
     */
    public static Optional<Control> fromId(Identifier id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

	/** What a control id that resolves to nothing falls back to, before the first registered. */
	private static final Identifier FALLBACK_ID = AITMod.id("throttle");

	public static Control fallback() {
		Control fallback = fromId(FALLBACK_ID).orElse(REGISTRY.get(0));

		if (fallback == null)
			throw new IllegalStateException("ControlRegistry is empty; cannot provide a fallback control");

		return fallback;
	}

    public static void init() {
        // Pain.

        register(new AntiGravsControl());
        register(new AutoPilotControl());
        register(new CloakControl());
        register(new DimensionControl());
        register(new DirectionControl());
        register(new DoorControl());
        register(new DoorLockControl());
        register(new FastReturnControl());
        register(new HADSControl());
        register(new HailMaryControl());
        register(new HandBrakeControl());
        register(new LandTypeControl());
        register(new MonitorControl());
        register(new PowerControl());
        register(new RandomiserControl());
        register(new RefuelerControl());
        register(new SecurityControl());
        register(new SiegeModeControl());
        register(new SonicPortControl());
        register(new TelepathicControl());
        register(new ThrottleControl());
        register(new VisualiserControl());
        register(new EngineOverloadControl());
        register(new ElectricalDischargeControl());
        register(new ShieldsControl());

        // Waypoints
        register(new ConsolePortControl());
        register(new SaveWaypointControl());
        register(new LoadWaypointControl());

        // Pos
        register(new IncrementControl());
        register(new XControl());
        register(new YControl());
        register(new ZControl());
    }
}
