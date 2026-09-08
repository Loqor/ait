package dev.amble.ait.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.decoration.TardisStarModel;
import dev.amble.ait.compat.DependencyChecker;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.world.TardisServerWorld;

public class TardisStar {

    public static final Identifier TARDIS_STAR_TEXTURE = new Identifier(AITMod.MOD_ID,
            "textures/environment/eye_of_harmony.png");
    private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0) / 2.0);

    public static void render(WorldRenderContext context, Tardis tardis) {
        if (DependencyChecker.hasPortals() && !TardisServerWorld.isTardisDimension(context.world()))
            return;

        renderShine(context, tardis);

        renderStar(context, tardis);
        if (!tardis.isGrowth() && !tardis.alarm().isEnabled() && tardis.fuel().hasPower())
            RenderSystem.setShaderFogColor(1, 1, 1, 0);
    }

    public static void renderStar(WorldRenderContext context, Tardis tardis) {
        Camera camera = context.camera();
        VertexConsumerProvider provider = context.consumers();

        Vec3d cameraPos = camera.getPos();
        if (tardis.getDesktop() == null) return;

        Profiler profiler = context.world().getProfiler();
        profiler.push("ait:tardis_star");
        // Two full model builds every frame, unconditionally, whenever the player is in an interior.
        profiler.visit("ait_model_build", 2);

        Vec3d targetPos = new Vec3d(camera.getPos().getX(),
                context.world().getBottomY() - (tardis.isGrowth() ? 150 : 120), camera.getPos().getZ());

        Vec3d diff = targetPos.subtract(cameraPos);

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrixStack.translate(0, diff.y, 0);
        matrixStack.scale(40f, 40f, 40f);

        float delta = MinecraftClient.getInstance().getTickDelta() + MinecraftClient.getInstance().player.age;
        matrixStack.multiply(RotationAxis.POSITIVE_Y
                .rotationDegrees(delta));

        TardisStarModel.getTexturedModelData().createModel().render(matrixStack,
                provider.getBuffer(AITRenderLayers.tardisEmissiveCullZOffset(TARDIS_STAR_TEXTURE)),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, tardis.isGrowth() ? 0.1f : 1,
                tardis.isGrowth() ? 0.1f : 1, tardis.isGrowth() ? 0.1f : 1, 0.5f);

        matrixStack.scale(0.9f, 0.9f, 0.9f);
        TardisStarModel.getTexturedModelData().createModel().render(matrixStack,
                provider.getBuffer(AITRenderLayers.tardisEmissiveCullZOffset(TARDIS_STAR_TEXTURE)),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1, tardis.isGrowth() ? 0.2f : 1,
                tardis.isGrowth() ? 0f : 1, 1f);

        profiler.pop();
    }

    public static void renderShine(WorldRenderContext context, Tardis tardis) {
        if (tardis.getExterior() == null) return;

        if (tardis.isGrowth())
            return;

        Profiler profiler = context.world().getProfiler();
        profiler.push("ait:tardis_star_shine");

        MatrixStack matrixStack = new MatrixStack();
        VertexConsumerProvider provider = context.consumers();

        Vec3d cameraPos = context.camera().getPos();
        Vec3d targetPos = new Vec3d(cameraPos.getX(),
                context.world().getBottomY() - (tardis.isGrowth() ? 150 : 120), cameraPos.getZ());

        Vec3d diff = targetPos.subtract(cameraPos);

        float l = (MinecraftClient.getInstance().getTickDelta() / 50120L);
        float delta = MinecraftClient.getInstance().getTickDelta() + MinecraftClient.getInstance().player.age;
        float sinFunc = (float) Math.sin((delta * (tardis.travel().speed() + 1)) * 0.2f + 0.2f);
        Random random = Random.create(432L);
        VertexConsumer vertexConsumer4 = provider.getBuffer(AITRenderLayers.getLightning());
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(context.camera().getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(context.camera().getYaw() + 180.0F));
        matrixStack.translate(0, diff.y, 0);
        if (!tardis.isRefueling())
            matrixStack.scale(8, 8, 8);
        else
            matrixStack.scale(8 + sinFunc, 8 + sinFunc, 8 + sinFunc);

        matrixStack.multiply(RotationAxis.POSITIVE_Y
                .rotationDegrees((-delta * (tardis.travel().speed() + 1))));

        float m = Math.min(l > 0.8f ? (l - 0.8f) / 0.2f : 0.0f, 1.0f);

        for (int n = 0; n < 30; n++) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((random.nextFloat() * 360.0f)));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((random.nextFloat() * 360.0f)));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((random.nextFloat() * 360.0f)));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((random.nextFloat() * 360.0f)));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((random.nextFloat() * 360.0f)));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((random.nextFloat() * 360.0f + l * 90.0f)));

            float o = random.nextFloat() * 10.0f + 10.0f + m * 10.0f;
            float p = random.nextFloat() * 0.5f + 1.0f + m * 2.0f;

            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            int q = (int) (255f * (1.0f - m));

            TardisStar.putDeathLightSourceVertex(tardis, vertexConsumer4, matrix4f, q);
            TardisStar.putDeathLightNegativeXTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightPositiveXTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightSourceVertex(tardis, vertexConsumer4, matrix4f, q);
            TardisStar.putDeathLightPositiveXTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightPositiveZTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightSourceVertex(tardis, vertexConsumer4, matrix4f, q);
            TardisStar.putDeathLightPositiveZTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightNegativeXTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightSourceVertex(tardis, vertexConsumer4, matrix4f, q);
            TardisStar.putDeathLightPositiveZTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
            TardisStar.putDeathLightPositiveZTerminalVertex(tardis, vertexConsumer4, matrix4f, o, p);
        }

        profiler.visit("ait_star_shine_vertices", 30 * 12);
        profiler.pop();
    }

    public static void putDeathLightSourceVertex(Tardis tardis, VertexConsumer buffer, Matrix4f matrix, int alpha) {
        buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(255, 255, 255, alpha).next();
    }

    public static void putDeathLightNegativeXTerminalVertex(Tardis tardis, VertexConsumer buffer, Matrix4f matrix,
                                                            float radius, float width) {
        buffer.vertex(matrix, -HALF_SQRT_3 * width, radius, -0.5f * width)
                .color(255, tardis.isGrowth() ? 30 : 154, 0, 0).next();
    }

    public static void putDeathLightPositiveXTerminalVertex(Tardis tardis, VertexConsumer buffer, Matrix4f matrix,
                                                            float radius, float width) {
        buffer.vertex(matrix, HALF_SQRT_3 * width, radius, -0.5f * width).color(255, tardis.isGrowth() ? 30 : 154, 0, 0)
                .next();
    }

    public static void putDeathLightPositiveZTerminalVertex(Tardis tardis, VertexConsumer buffer, Matrix4f matrix,
                                                            float radius, float width) {
        buffer.vertex(matrix, 0.0f, radius, width).color(255, tardis.isGrowth() ? 30 : 154, 0, 0).next();
    }
}
