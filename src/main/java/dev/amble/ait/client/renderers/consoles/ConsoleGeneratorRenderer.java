package dev.amble.ait.client.renderers.consoles;

import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.profiler.Profiler;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.consoles.BedrockConsoleModel;
import dev.amble.ait.client.models.consoles.ConsoleGeneratorModel;
import dev.amble.ait.client.models.consoles.ConsoleModel;
import dev.amble.ait.core.blockentities.ConsoleGeneratorBlockEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.data.datapack.DatapackConsole;
import dev.amble.ait.data.schema.console.ClientConsoleVariantSchema;
import dev.amble.ait.data.schema.console.ConsoleVariantSchema;

public class ConsoleGeneratorRenderer<T extends ConsoleGeneratorBlockEntity> implements BlockEntityRenderer<T> {

    private final ConsoleGeneratorModel generator;
    private final EntityRenderDispatcher dispatcher;

    public static final Identifier TEXTURE = new Identifier(AITMod.MOD_ID,
            "textures/blockentities/consoles/console_generator/console_generator.png");

    private static final int VARIANT_TEXT_COLOR_TURQUOISE = ColorHelper.Argb.getArgb(1, 0, 175, 235);
    private static final int VARIANT_TEXT_COLOR_YELLOW = ColorHelper.Argb.getArgb(1, 255, 205, 0);

    public ConsoleGeneratorRenderer(BlockEntityRendererFactory.Context ctx) {
        this.dispatcher = ctx.getEntityRenderDispatcher();
        this.generator = new ConsoleGeneratorModel(ConsoleGeneratorModel.getTexturedModelData().createModel());
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, int overlay) {
        if (entity.getWorld() == null || !entity.isLinked() || entity.getConsoleVariant() == null)
            return;

        Profiler profiler = entity.getWorld().getProfiler();
        profiler.push("console_generator");
        profiler.visit("ait_generator_drawn");

        this.render0(entity, profiler, matrices, vertexConsumers, light, overlay);

        profiler.pop();
    }

    private void render0(T entity, Profiler profiler, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, int overlay) {
        profiler.push("setup");

        Tardis tardis = entity.tardis().get();

        ConsoleVariantSchema variant = entity.getConsoleVariant();
        ClientConsoleVariantSchema clientVariant = variant.getClient();

        ConsoleModel console = clientVariant.getCachedModel();
        Identifier consoleTexture = clientVariant.texture();
        Identifier consoleEmission = clientVariant.emission();

        profiler.swap("frame_model");

        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));
        matrices.translate(0.5f, -1.5f, -0.5f);

        this.generator.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), light,
                overlay, 1, 1, 1, 1);

        matrices.pop();

        profiler.swap("hologram");

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));

        matrices.translate(0.5f, -1.5f + entity.getWorld().random.nextFloat() * 0.02, -0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MinecraftClient.getInstance().getTickDelta() % 180));

        if (console instanceof BedrockConsoleModel bedrockConsoleModel) {
            matrices.translate(-0.5, 1.5, 0.5);
            bedrockConsoleModel.applyOffsets(matrices, entity.getConsoleVariant());
        }

        //if (powered) {
            if (tardis.isUnlocked(entity.getConsoleVariant())) {
                console.render(matrices,
                        vertexConsumers.getBuffer(clientVariant.hologramLayer(consoleTexture)), 0xf000f0, overlay, 0.3607843137f,
                        0.9450980392f, 1, entity.getWorld().random.nextInt(32) != 6 ? 0.4f : 0.05f);
                if (consoleEmission != null && !consoleEmission.equals(DatapackConsole.EMPTY)) {
                    console.render(matrices,
                            vertexConsumers.getBuffer(clientVariant.hologramLayer(consoleEmission)), 0xf000f0, overlay, 0.3607843137f,
                            0.9450980392f, 1, entity.getWorld().random.nextInt(32) != 6 ? 0.4f : 0.05f);
                }
            } else {
                console.render(matrices,
                        vertexConsumers.getBuffer(clientVariant.hologramLayer(consoleTexture)), light,
                        OverlayTexture.DEFAULT_UV, 0.2f, 0.2f, 0.2f,
                        entity.getWorld().random.nextInt(32) != 6 ? 0.4f : 0.05f);
            }
        //}
        matrices.pop();

        profiler.swap("label");

        matrices.push();
        matrices.translate(0.5F, 2.75F, 0.5F);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(-0.1F, -0.1F, 0.1F);

        Text type = Text.translatable("console.ait.variant_label").append(entity.getConsoleVariant().text());
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float l = (float) (-textRenderer.getWidth(type) / 2);

        if (/*powered && */!tardis.isUnlocked(entity.getConsoleVariant())) {
            Text text = Text.literal("\uD83D\uDD12");
            Text requirementLevel = entity.getConsoleVariant().requirement().isPresent()
                    ? entity.getConsoleVariant().requirement().get().type().text()
                    : Text.translatable("console.ait.generator.requirement.none");
            Text requirement = Text.translatable("console.ait.generator.requires_loyalty", requirementLevel);
            float h = (float) (-textRenderer.getWidth(text) / 2);
            float p = (float) (-textRenderer.getWidth(requirement) / 2);

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            textRenderer.draw(text, h + 0.35f, 0.0F, 0xFFFFFFFF, false, matrix4f, vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x000000, 0xf000f0);
            matrices.push();
            matrices.scale(0.2f, 0.2f, 0.2f);
            Matrix4f matrixcf = matrices.peek().getPositionMatrix();
            textRenderer.draw(type, l - 0.35f, 42.5F, VARIANT_TEXT_COLOR_TURQUOISE, false, matrixcf, vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x000000, 0xf000f0);
            matrices.pop();
            matrices.push();
            matrices.scale(0.2f, 0.2f, 0.2f);
            Matrix4f matrixdf = matrices.peek().getPositionMatrix();
            textRenderer.draw(requirement, p - 0.35f, 55F, VARIANT_TEXT_COLOR_YELLOW, false, matrixdf, vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x000000, 0xf000f0);
            matrices.pop();
            matrices.pop();
        } else {
            matrices.scale(0.2f, 0.2f, 0.2f);
            Matrix4f matrixcf = matrices.peek().getPositionMatrix();
            textRenderer.draw(type, l - 0.35f, 42.5F, VARIANT_TEXT_COLOR_YELLOW, false, matrixcf, vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x000000, 0xf000f0);
            matrices.pop();
        }

        profiler.pop();
    }
}
