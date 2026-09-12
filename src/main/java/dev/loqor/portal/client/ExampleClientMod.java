package dev.loqor.portal.client;

import net.fabricmc.api.ClientModInitializer;

public class ExampleClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Shadow-world teardown on disconnect is handled by PortalDataManager.reset() (registered in
        // PortalDataManager#init), which closes every PortalData - including its geometry renderer. Nothing to do here.
    }
}
