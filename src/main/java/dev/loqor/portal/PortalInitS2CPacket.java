package dev.loqor.portal;

import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import dev.amble.ait.AITMod;

/**
 * Sent just before a TARDIS's exterior chunks start streaming, telling the client which dimension the shadow
 * world for this TARDIS should mirror.
 * <p>
 * The client (re)builds its {@link dev.loqor.portal.client.PortalData} with the matching dimension + dimension
 * type and drops any stale geometry from a previous landing, so the doorway shows the correct world (including
 * Nether/End lighting) and never bleeds chunks across a relocation.
 */
public record PortalInitS2CPacket(UUID id, RegistryKey<World> dimension,
                                  RegistryKey<DimensionType> dimensionType) implements FabricPacket {

    public static final PacketType<PortalInitS2CPacket> TYPE =
            PacketType.create(AITMod.id("portal_init"), PortalInitS2CPacket::read);

    private static PortalInitS2CPacket read(PacketByteBuf buf) {
        return new PortalInitS2CPacket(buf.readUuid(),
                buf.readRegistryKey(RegistryKeys.WORLD),
                buf.readRegistryKey(RegistryKeys.DIMENSION_TYPE));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.id);
        buf.writeRegistryKey(this.dimension);
        buf.writeRegistryKey(this.dimensionType);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
