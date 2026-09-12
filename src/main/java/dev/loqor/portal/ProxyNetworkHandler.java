package dev.loqor.portal;

import net.fabricmc.fabric.impl.networking.UntrackedNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class ProxyNetworkHandler extends ServerPlayNetworkHandler implements UntrackedNetworkHandler {

    private static final ClientConnection FAKE_CONNECTION = new ClientConnection(NetworkSide.CLIENTBOUND);

    private ProxyPacketListener packetListener;

    public ProxyNetworkHandler(ServerPlayerEntity player) {
        super(player.getServer(), FAKE_CONNECTION, player);
    }

    public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        if (this.packetListener != null && !(packet instanceof ProxiedPacket))
            this.packetListener.onPacket(packet);
    }

    public void setListener(ProxyPacketListener listener) {
        this.packetListener = listener;
    }
}
