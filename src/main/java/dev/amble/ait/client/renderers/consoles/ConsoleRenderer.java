package dev.amble.ait.client.renderers.consoles;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import dev.amble.ait.client.AITModClient;
import dev.amble.ait.client.models.consoles.BedrockConsoleModel;
import dev.amble.ait.client.models.consoles.ConsoleModel;
import dev.amble.ait.client.models.consoles.HartnellConsoleModel;
import dev.amble.ait.client.models.consoles.SimpleConsoleModel;
import dev.amble.ait.client.models.items.HandlesModel;
import dev.amble.ait.client.renderers.AITRenderLayers;
import dev.amble.ait.client.renderers.EmissiveGeometry;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.client.util.ClientRenderPass;
import dev.amble.ait.client.util.OffScreenCull;
import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.item.HandlesItem;
import dev.amble.ait.data.datapack.DatapackConsole;
import dev.amble.ait.data.schema.console.ClientConsoleVariantSchema;
import dev.amble.ait.data.schema.console.variant.crystalline.client.ClientCrystallineVariant;
import dev.amble.ait.registry.impl.console.variant.ClientConsoleVariantRegistry;
public class ConsoleRenderer<T extends ConsoleBlockEntity> implements BlockEntityRenderer<T> {

    private ClientConsoleVariantSchema variant;
    private ConsoleModel model;

    public ConsoleRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        if (entity.getWorld() == null) return;

        Profiler profiler = entity.getWorld().getProfiler();
        profiler.visit("ait_console_dispatched");

        // Ahead of the duplicate guard so the two counters partition the dispatches.
        //
        // Origin is the block centre a block and a half up: the renderer flips 180 degrees about X and
        // the models then translate by (0.5, -1.5, -0.5), which the flip turns into this. Slop covers
        // what is drawn outside the model root, the largest being monitor text at an anchor up to 1.86
        // blocks out reaching a further 1.5.
        if (OffScreenCull.boxBehindCamera(entity, this.rootFor(entity), 0.5, 1.5, 0.5, 3.0)) {
            profiler.visit("ait_console_offscreen_skipped");
            return;
        }

        // Called twice a pass: once from the chunk's block entity list, once from the global no-cull
        // list. Both draws are identical, so only the first does the work. When the section is culled
        // the first call never arrives and the global one draws instead, which is the point of being
        // on that list at all.
        if (!ClientRenderPass.shouldDraw(entity)) {
            profiler.visit("ait_console_duplicate_skipped");
            return;
        }

        if (!entity.isLinked()) {
            profiler.visit("ait_console_unlinked");
            profiler.visit("ait_model_build");
            matrices.push();
            matrices.translate(0.5, 1.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));
            HartnellConsoleModel model = new HartnellConsoleModel(HartnellConsoleModel.getTexturedModelData().createModel());
            model.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(ClientConsoleVariantRegistry.HARTNELL.texture())),
                    light, overlay, 1, 1, 1, 1);
            RenderLayer layer = AITRenderLayers.tardisEmissiveCullZOffset(ClientConsoleVariantRegistry.HARTNELL.emission());
            model.render(matrices, vertexConsumers.getBuffer(layer),
                    0xf000f0, overlay, 1, 1, 1, 1);
            matrices.pop();
            return;
        }

        ClientTardis tardis = entity.tardis().get().asClient();

        profiler.visit("ait_console_drawn");

        profiler.push("console");
        this.renderConsole(profiler, tardis, entity, matrices, vertexConsumers, light, overlay, tickDelta);

        if (variant instanceof ClientCrystallineVariant)
            this.renderPanes(tardis, entity, matrices, vertexConsumers, light, overlay);
        profiler.pop();
    }

    private void renderPanes(ClientTardis tardis, T entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!tardis.fuel().hasPower()) return;
        matrices.push();
        matrices.translate(1, 2 + entity.getWorld().random.nextFloat() * 0.02, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MinecraftClient.getInstance().getTickDelta() % 180));
        matrices.translate(0.58, 0.1, -0.25);
        matrices.scale(0.9f, 0.9f, 0.9f);

        MinecraftClient.getInstance().getItemRenderer().
                renderItem(new ItemStack(Items.ORANGE_STAINED_GLASS_PANE),
                        ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
        matrices.translate(0 + entity.getWorld().random.nextFloat() * 0.02, 0 + entity.getWorld().random.nextFloat() * 0.02, 0 + entity.getWorld().random.nextFloat() * 0.02);
        MinecraftClient.getInstance().getItemRenderer().
                renderItem(new ItemStack(Items.ORANGE_STAINED_GLASS_PANE),
                        ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
        matrices.pop();

        matrices.push();
        matrices.translate(-1, 2 + entity.getWorld().random.nextFloat() * 0.02, -0.5);
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MinecraftClient.getInstance().getTickDelta() % 180));
        matrices.translate(0.78, 0.15, -0.11);
        matrices.scale(0.9f, 0.9f, 0.9f);

        MinecraftClient.getInstance().getItemRenderer().
                renderItem(new ItemStack(Items.ORANGE_STAINED_GLASS_PANE),
                        ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
        matrices.translate(0 - entity.getWorld().random.nextFloat() * 0.02, 0 + entity.getWorld().random.nextFloat() * 0.02, 0 - entity.getWorld().random.nextFloat() * 0.02);
        MinecraftClient.getInstance().getItemRenderer().
                renderItem(new ItemStack(Items.ORANGE_STAINED_GLASS_PANE),
                        ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
        matrices.pop();
    }

    private void renderConsole(Profiler profiler, ClientTardis tardis, T entity, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int light, int overlay, float tickDelta) {
        profiler.push("model");

        this.updateModel(entity);

        boolean hasPower = tardis.fuel().hasPower();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));

        profiler.swap("animate");
        model.animateBlockEntity(entity, tardis.travel().getState(), hasPower);

        profiler.swap("base_buffer");
        profiler.visit("ait_console_layer_switch");

        // Cutout for every variant, copper included. Copper used to ask for the translucent layer,
        // which pays a CPU quad sort on every flush, on the largest model in the mod: it cost 0.575 ms
        // a frame in the zone that flushes this layer. Its base textures carry no partial alpha at all,
        // so alpha testing and alpha blending produce the same pixels and the sort bought nothing.
        VertexConsumer baseBuffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(variant.texture()));

        profiler.swap("render");
        model.renderWithAnimations(tardis, entity, model.getPart(),
                matrices, baseBuffer, light, overlay,
                1, 1, 1, 1, tickDelta);

        this.renderEmissions(profiler, matrices, vertexConsumers, tardis, entity, hasPower, light, overlay, tickDelta);

        matrices.pop();
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));

        matrices.pop();

        profiler.swap("monitor");

        if (hasPower && AITModClient.CONFIG.showConsoleMonitorText && model instanceof SimpleConsoleModel simplez) {
            simplez.renderMonitorText(tardis, entity, matrices, vertexConsumers, light, overlay);
        }

        profiler.swap("sonic_port"); // } emission / sonic {

        ItemStack stack = entity.getSonicScrewdriver() == null || entity.getSonicScrewdriver().isEmpty() ? tardis.butler().getHandles() : entity.getSonicScrewdriver();

        if (stack == null) {
            profiler.pop(); // } sonic
            return;
        }

        if (stack.getItem() instanceof HandlesItem) {
            matrices.push();
            matrices.translate(variant.handlesTranslations().x(), variant.handlesTranslations().y(),
                    variant.handlesTranslations().z());
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(variant.handlesRotations()[0]));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(variant.handlesRotations()[1]));
            matrices.scale(0.6f, 0.6f, 0.6f);
            HandlesModel handlesModel = new HandlesModel(HandlesModel.getTexturedModelData().createModel());
            //handlesModel.setAngles(matrices, ModelTransformationMode.GROUND, false);
            handlesModel.handles.getChild("stalk").pitch = 45f;
            handlesModel.handles.getChild("stalk").getChild("head").pitch = -0.25f;
            handlesModel.render(null, MinecraftClient.getInstance().player, stack, matrices, vertexConsumers, light, overlay, 0);
            matrices.pop();
        } else {
            matrices.push();
            matrices.translate(variant.sonicItemTranslations().x(), variant.sonicItemTranslations().y(),
                    variant.sonicItemTranslations().z());
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(variant.sonicItemRotations()[0]));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(variant.sonicItemRotations()[1]));
            matrices.scale(0.9f, 0.9f, 0.9f);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light,
                    overlay, matrices, vertexConsumers, entity.getWorld(), 0);
            matrices.pop();
        }

        profiler.pop(); // } sonic
    }

    private void renderEmissions(Profiler profiler, MatrixStack matrices, VertexConsumerProvider vertexConsumers, ClientTardis tardis, T entity, boolean hasPower, int light, int overlay, float tickDelta) {
        if (!hasPower) return;

        profiler.swap("emission");

        matrices.push();
        if (variant.emission() != null && !variant.emission().equals(DatapackConsole.EMPTY)) {
            profiler.swap("emission_buffer");
            profiler.visit("ait_console_layer_switch");
            VertexConsumer emissive = vertexConsumers
                    .getBuffer(AITRenderLayers.tardisEmissiveCullZOffset(variant.emission()));

            profiler.swap("emission_geometry");

            // Geometry only. The control state was applied before the base layer was drawn and does not
            // depend on the layer, so re-running it here would only pose the model differently under
            // the glow than under the geometry it sits on.
            //
            // The model instance is shared by every console of this variant, so the hidden parts have
            // to be put back before anything else draws it.
            EmissiveGeometry.Scope unlit = EmissiveGeometry.hideUnlit(model.getPart(), variant.emission());

            model.renderGeometryOnly(tardis, entity, model.getPart(),
                    matrices, emissive, 0xf000f0, overlay,
                    1, 1, 1, 1, tickDelta);

            unlit.restore();
        }
        matrices.pop();
    }

    /**
     * The model root to bound, or null when there is nothing to bound yet.
     *
     * <p>Read straight from the variant rather than from {@link #updateModel}, because the cull runs
     * before the draw does and must not be the thing that decides which model is cached.
     */
    private ModelPart rootFor(T entity) {
        if (!entity.isLinked() || entity.getVariant() == null)
            return null;

        ClientConsoleVariantSchema schema = entity.getVariant().getClient();

        if (schema == null)
            return null;

        ConsoleModel cached = schema.getCachedModel();

        // A datapack console applies its own offset and scale inside renderWithAnimations, from
        // fields with no bound on them, so there is nothing here that could bound it. Never culled.
        if (cached instanceof BedrockConsoleModel)
            return null;

        return cached == null ? null : cached.getPart();
    }

    private void updateModel(T entity) {
        ClientConsoleVariantSchema variant = entity.getVariant().getClient();

        if (this.variant != variant) {
            this.variant = variant;
            this.model = variant.getCachedModel();
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(ConsoleBlockEntity consoleBlockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }

    @Override
    public boolean isInRenderDistance(ConsoleBlockEntity consoleBlockEntity, Vec3d vec3d) {
        return Vec3d.ofCenter(consoleBlockEntity.getPos()).multiply(1.0, 0.0, 1.0).isInRange(vec3d.multiply(1.0, 0.0, 1.0), this.getRenderDistance());
    }
}
