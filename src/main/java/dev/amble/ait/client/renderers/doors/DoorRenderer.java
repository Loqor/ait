package dev.amble.ait.client.renderers.doors;

import dev.amble.ait.client.AITModClient;
import org.joml.Vector3f;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import dev.amble.ait.api.tardis.TardisComponent;
import dev.amble.ait.client.boti.BOTI;
import dev.amble.ait.client.models.AnimatedModel;
import dev.amble.ait.client.models.doors.CapsuleDoorModel;
import dev.amble.ait.client.models.doors.exclusive.DoomDoorModel;
import dev.amble.ait.client.renderers.AITRenderLayers;
import dev.amble.ait.client.util.ClientRenderPass;
import dev.amble.ait.client.util.OffScreenCull;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.compat.DependencyChecker;
import dev.amble.ait.core.blockentities.DoorBlockEntity;
import dev.amble.ait.core.blocks.DoorBlock;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.BiomeHandler;
import dev.amble.ait.data.datapack.DatapackConsole;
import dev.amble.ait.data.schema.exterior.ClientExteriorVariantSchema;
import dev.amble.ait.registry.impl.exterior.ClientExteriorVariantRegistry;

public class DoorRenderer<T extends DoorBlockEntity> implements BlockEntityRenderer<T> {

    private ClientExteriorVariantSchema variant;
    private AnimatedModel<DoorBlockEntity> model;

    public DoorRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {
        if (entity.getWorld() == null) return;

        // Fetched per call, never held: the client swaps its profiler object out every frame.
        Profiler profiler = entity.getWorld().getProfiler();
        profiler.visit("ait_door_dispatched");

        // Called twice a pass: once from the chunk's block entity list, once from the global no-cull
        // list. Both draws are identical, so only the first does the work. When the section is culled
        // the first call never arrives and the global one draws instead, which is the point of being
        // on that list at all.
        if (!ClientRenderPass.shouldDraw(entity)) {
            profiler.visit("ait_door_duplicate_skipped");
            return;
        }

        if (!entity.isLinked()) {
            BlockState blockState = entity.getCachedState();
            float k = blockState.get(DoorBlock.FACING).asRotation();
            matrices.push();
            matrices.translate(0.5, 1.5, 0.5);
            matrices.scale(1, 1, 1);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(k + 180));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));
            CapsuleDoorModel doorModel = new CapsuleDoorModel(CapsuleDoorModel.getTexturedModelData().createModel());
            doorModel.render(matrices, vertexConsumers.getBuffer(AITRenderLayers.getEntityCutout(ClientExteriorVariantRegistry.CAPSULE_DEFAULT.texture())),
                    light, overlay, 1, 1, 1, 1);
            matrices.pop();
            return;
        }

        profiler.push("door");

        ClientTardis tardis = entity.tardis().get().asClient();
        if (!tardis.siege().isActive())
            this.renderDoor(profiler, tardis, entity, matrices, vertexConsumers, light, overlay, tickDelta);

        profiler.pop();
    }

    private void renderDoor(Profiler profiler, ClientTardis tardis, T entity, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, int light, int overlay, float tickDelta) {
        this.updateModel(tardis);

        // Same shape as the exterior, and the same reason for being here rather than in front of the
        // duplicate guard: the bound needs the stats scale. A sphere because the renderer yaws the
        // model by the door's facing. This covers the interior door's own BOTI enqueue below, which
        // for a player stood inside is the likelier cost of the two.
        double doorScale = maxScale(tardis.stats().getScale());

        if (this.model != null && OffScreenCull.sphereBehindCamera(entity, this.model.getPart(),
                0.5, 0.0, 0.5, doorScale, doorScale)) {
            profiler.visit("ait_door_offscreen_skipped");
            return;
        }

        BlockState blockState = entity.getCachedState();
        float k = blockState.get(DoorBlock.FACING).asRotation();

        Identifier texture = this.variant.texture();

        if (this.variant.equals(ClientExteriorVariantRegistry.DOOM))
            texture = tardis.door().isOpen() ? DoomDoorModel.DOOM_DOOR_OPEN : DoomDoorModel.DOOM_DOOR;

        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        Vector3f scale = tardis.stats().getScale();
        matrices.scale(scale.x(), scale.y(), scale.z());
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(k));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));

        if (!DependencyChecker.hasIris()) {
            model.renderWithAnimations(tardis, entity, model.getPart(), matrices,
                    vertexConsumers.getBuffer(AITRenderLayers.getEntityTranslucentCull(texture)), light, overlay, 1, 1,
                    1, 1, tickDelta);
        }

        /*if (tardis.overgrown().overgrown().get())
            model.renderWithAnimations(entity, model.getPart(), matrices,
                    vertexConsumers.getBuffer(AITRenderLayers.getEntityTranslucentCull(
                            tardis.overgrown().getOvergrownTexture())),
                    light, overlay, 1, 1, 1, 1);*/

        profiler.push("emission");

        Identifier emissive = this.variant.emission();

        if (!variant.equals(ClientExteriorVariantRegistry.DOOM) && emissive != null && !emissive.equals(DatapackConsole.EMPTY)) {
            boolean power = tardis.fuel().hasPower();
            boolean alarms = tardis.alarm().isEnabled();

            float u;
            float t;
            float s;

            if ((tardis.stats().getName() != null && "partytardis".equalsIgnoreCase(tardis.stats().getName()) ||(!tardis.extra().getInsertedDisc().isEmpty()))) {
                int m = 25;
                int n = MinecraftClient.getInstance().player.age / m + MinecraftClient.getInstance().player.getId();
                int o = DyeColor.values().length;
                int p = n % o;
                int q = (n + 1) % o;
                float r = ((float)(MinecraftClient.getInstance().player.age % m)) / m;
                float[] fs = SheepEntity.getRgbColor(DyeColor.byId(p));
                float[] gs = SheepEntity.getRgbColor(DyeColor.byId(q));
                s = fs[0] * (1f - r) + gs[0] * r;
                t = fs[1] * (1f - r) + gs[1] * r;
                u = fs[2] * (1f - r) + gs[2] * r;
            } else {
                float[] hs = new float[]{ 1.0f, 1.0f, 1.0f };
                s = hs[0];
                t = hs[1];
                u = hs[2];
            }

            float colorAlpha = 1;

            float red = alarms ? !power ? 0.25f : s : s;
            float green = alarms ? !power ? 0.01f : 0.3f : t;
            float blue = alarms ? !power ? 0.01f : 0.3f : u;

            model.renderWithAnimations(tardis, entity, this.model.getPart(), matrices, vertexConsumers.getBuffer(AITRenderLayers.tardisEmissiveCullZOffset(variant.emission())),
                    0xf000f0, OverlayTexture.DEFAULT_UV, red, green, blue, colorAlpha, tickDelta);
        }

        if (DependencyChecker.hasIris()) {
            model.renderWithAnimations(tardis, entity, model.getPart(), matrices,
                    vertexConsumers.getBuffer(AITRenderLayers.getEntityTranslucentCull(texture)), light, overlay, 1, 1,
                    1, 1, tickDelta);
        }

        profiler.swap("biome");

        if (this.variant != ClientExteriorVariantRegistry.CORAL_GROWTH) {
            BiomeHandler biome = tardis.handler(TardisComponent.Id.BIOME);
            Identifier biomeTexture = biome.getBiomeKey().get(this.variant.overrides());

            if (biomeTexture != null && !texture.equals(biomeTexture)) {
                model.renderWithAnimations(tardis, entity, model.getPart(),
                        matrices, vertexConsumers.getBuffer(AITRenderLayers.getEntityCutoutNoCullZOffset(biomeTexture)),
                        light, overlay, 1, 1, 1, 1, tickDelta);
            }
        }

        if ((tardis.door().getLeftRot() > 0 || this.variant.hasTransparentDoors()) && !tardis.isGrowth() && !AITModClient.skipBuiltInBOTI())
            BOTI.DOOR_RENDER_QUEUE.add(entity);

        matrices.pop();
        profiler.pop();
    }

    /** The largest axis of a non uniform scale, since the bound is a sphere. */
    private static double maxScale(Vector3f scale) {
        return Math.max(1.0, Math.max(scale.x(), Math.max(scale.y(), scale.z())));
    }

    private void updateModel(Tardis tardis) {
        if (tardis.getExterior().getVariant() == null) return;
        ClientExteriorVariantSchema variant = tardis.getExterior().getVariant().getClient();

        if (this.variant != variant) {
            this.variant = variant;
            this.model = variant.getDoor().model();
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(DoorBlockEntity doorBlockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }

    @Override
    public boolean isInRenderDistance(DoorBlockEntity doorBlockEntity, Vec3d vec3d) {
        return Vec3d.ofCenter(doorBlockEntity.getPos()).multiply(1.0, 0.0, 1.0).isInRange(vec3d.multiply(1.0, 0.0, 1.0), this.getRenderDistance());
    }
}
