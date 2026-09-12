package dev.loqor.portal.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.telemetry.WorldSession;
import net.minecraft.network.ClientConnection;
import org.jetbrains.annotations.Nullable;

public class ClientPlayNetworkHanderAnalog extends ClientPlayNetworkHandler {
    public ClientPlayNetworkHanderAnalog(MinecraftClient client, Screen screen, ClientConnection connection, @Nullable ServerInfo serverInfo, GameProfile profile, WorldSession worldSession) {
        super(client, screen, connection, serverInfo, profile, worldSession);
    }
}
