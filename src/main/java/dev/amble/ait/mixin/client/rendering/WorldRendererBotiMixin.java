package dev.amble.ait.mixin.client.rendering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;

import dev.loqor.portal.client.WorldGeometryRenderer;

/**
 * While the BOTI doorway draws the exterior dimension's sky through a shadow {@link WorldRenderer}, vanilla
 * {@code renderSky} still reads two positions from the real session - i.e. the INTERIOR:
 * <ul>
 *   <li>{@code client.player.getCameraPosVec(...)} decides whether to draw the black below-horizon "void plane"
 *   (drawn whenever the eye is below the sky darkness height, y=63 in the overworld). TARDIS interiors sit below
 *   that, so the doorway always showed a black band along the horizon even with the exterior on the surface.</li>
 *   <li>{@code gameRenderer.getCamera().getPos()} chooses the biome the zenith sky colour is sampled from;
 *   interior coordinates aren't loaded in the shadow world, so it fell back to the default biome's colour.</li>
 * </ul>
 * During the portal sky pass {@link WorldGeometryRenderer} publishes the portal eye's exterior position; these
 * redirects substitute it so both decisions are made with exterior coordinates. Outside the portal pass the
 * override is null and vanilla behaviour is untouched.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererBotiMixin {

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCameraPosVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d ait$botiVoidPlaneEye(ClientPlayerEntity player, float tickDelta) {
        Vec3d portalEye = WorldGeometryRenderer.getPortalSkyCameraPos();
        return portalEye != null ? portalEye : player.getCameraPosVec(tickDelta);
    }

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;getPos()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d ait$botiSkyColorPos(Camera camera) {
        Vec3d portalEye = WorldGeometryRenderer.getPortalSkyCameraPos();
        return portalEye != null ? portalEye : camera.getPos();
    }
}
