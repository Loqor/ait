package dev.loqor.portal.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Supplier;

public class ClientWorldAnalog extends ClientWorld {
    public ClientWorldAnalog(ClientPlayNetworkHandler networkHandler, Properties properties, RegistryKey<World> registryRef,
                             RegistryEntry<DimensionType> dimensionTypeEntry, int loadDistance, int simulationDistance, Supplier<Profiler> profiler,
                             WorldRenderer worldRenderer, boolean debugWorld, long seed) {
        super(networkHandler, properties, registryRef, dimensionTypeEntry, loadDistance, simulationDistance, profiler, worldRenderer, debugWorld, seed);
    }
}
