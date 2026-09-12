package dev.loqor.portal;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class PacketProxyPlayer extends FakePlayer {

    private static final GameProfile DEFAULT_PROFILE = new GameProfile(UUID.randomUUID(), "[Ptl Packet Proxy]");

    public PacketProxyPlayer(ServerWorld world) {
        super(world, DEFAULT_PROFILE);
        this.networkHandler = new ProxyNetworkHandler(this);
    }

    public void setPacketListener(ProxyPacketListener listener) {
        ((ProxyNetworkHandler) this.networkHandler).setListener(listener);
    }

    public void onChunkEntered() {
        this.getServerWorld().getChunkManager().updatePosition(this);
    }
}
