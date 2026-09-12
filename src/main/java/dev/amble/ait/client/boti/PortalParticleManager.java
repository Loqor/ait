package dev.amble.ait.client.boti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;

/**
 * A {@link ParticleManager} bound to a portal's shadow world, so particles spawned around a TARDIS's exterior
 * can be ticked and rendered into the doorway without polluting the main world's particle manager.
 * <p>
 * {@code ParticleManagerMixin} stops the superclass constructor from re-registering (and thereby closing) the
 * shared particle atlas; this class then copies the main manager's already-stitched atlas and factories, so it
 * reuses them instead of owning a second, unstitched copy.
 */
public class PortalParticleManager extends ParticleManager {

    public PortalParticleManager(ClientWorld world, MinecraftClient client) {
        super(world, client.getTextureManager());

        ParticleManager main = client.particleManager;
        this.factories = main.factories;
        this.spriteAwareFactories = main.spriteAwareFactories;
        this.particleAtlasTexture = main.particleAtlasTexture;
    }
}
