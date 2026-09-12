package dev.loqor.portal;

import dev.amble.ait.AITMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record WrappedPacketS2CPacket(UUID id, Packet<?> packet) implements FabricPacket, ProxiedPacket {

    public static final PacketType<WrappedPacketS2CPacket> TYPE = PacketType.create(AITMod.id("wrapped"), WrappedPacketS2CPacket::read);

    private static WrappedPacketS2CPacket read(PacketByteBuf buf) {
        UUID id = buf.readUuid();
        Packet<?> packet = readPacket(buf);
        return new WrappedPacketS2CPacket(id, packet);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(id);

        if (this.packet instanceof BundleS2CPacket bundle) {
            List<Packet<ClientPlayPacketListener>> packets = (List<Packet<ClientPlayPacketListener>>) bundle.getPackets();

            buf.writeVarInt(-1);
            buf.writeVarInt(packets.size());

            for (Packet<ClientPlayPacketListener> packet : packets) {
                writePacket(packet, buf);
            }
        } else {
            writePacket(this.packet, buf);
        }
    }

    private static int getPacketId(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket)
            return -1;

        int packetId = NetworkState.PLAY.getPacketId(NetworkSide.CLIENTBOUND, packet);

        if (packetId < 0)
            throw new IllegalStateException("Bad packet id: " + packetId + " for packet " + packet);

        return packetId;
    }

    private static void writePacket(Packet<?> packet, PacketByteBuf buf) {
        buf.writeVarInt(getPacketId(packet));
        packet.write(buf);
    }

    private static Packet<ClientPlayPacketListener> readPacket(PacketByteBuf buf) {
        int packetId = buf.readVarInt();

        if (packetId == -1) {
            int size = buf.readVarInt();
            List<Packet<ClientPlayPacketListener>> packets = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                packets.add(readPacket(buf));
            }

            return new BundleS2CPacket(packets);
        } else {
            //noinspection unchecked - source: trust me bro.
            return (Packet<ClientPlayPacketListener>) NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, packetId, buf);
        }
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}