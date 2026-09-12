package dev.loqor.portal;

import net.minecraft.network.packet.Packet;

public interface ProxyPacketListener {
    void onPacket(Packet<?> packet);
}
