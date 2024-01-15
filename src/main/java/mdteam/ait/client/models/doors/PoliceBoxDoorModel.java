package mdteam.ait.client.models.doors;

import mdteam.ait.AITMod;
import mdteam.ait.client.animation.exterior.door.DoorAnimations;
import mdteam.ait.core.blockentities.DoorBlockEntity;
import mdteam.ait.tardis.handler.DoorHandler;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class PoliceBoxDoorModel extends DoorModel {
    private final ModelPart TARDIS;

    public PoliceBoxDoorModel(ModelPart root) {
        super(RenderLayer::getEntityCutoutNoCull);
        this.TARDIS = root.getChild("TARDIS");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData TARDIS = modelPartData.addChild("TARDIS", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData Posts = TARDIS.addChild("Posts", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, -32.0F));

        ModelPartData cube_r1 = Posts.addChild("cube_r1", ModelPartBuilder.create().uv(68, 227).cuboid(-18.0F, -65.0F, -18.0F, 4.0F, 61.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 4.0F, 10.0F, 0.0F, 3.1416F, 0.0F));

        ModelPartData cube_r2 = Posts.addChild("cube_r2", ModelPartBuilder.create().uv(85, 227).cuboid(-18.0F, -65.0F, -18.0F, 4.0F, 61.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 4.0F, 10.0F, 0.0F, 1.5708F, 0.0F));

        ModelPartData Doors = TARDIS.addChild("Doors", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData right_door = Doors.addChild("right_door", ModelPartBuilder.create().uv(181, 177).cuboid(0.5F, -25.5F, -0.5F, 13.0F, 55.0F, 1.0F, new Dilation(0.0F))
                .uv(102, 228).cuboid(1.5F, -10.5F, 0.5F, 10.0F, 12.0F, 3.0F, new Dilation(0.0F))
                .uv(0, 198).cuboid(0.5F, -25.5F, -1.0F, 14.0F, 55.0F, 0.0F, new Dilation(0.0F))
                .uv(0, 10).cuboid(9.5F, -5.5F, -1.5F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-13.5F, -29.5F, -5.5F));

        ModelPartData left_door = Doors.addChild("left_door", ModelPartBuilder.create().uv(189, 41).cuboid(-13.5F, -25.5F, -0.5F, 13.0F, 55.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-12.5F, -6.5F, -1.5F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(13.5F, -29.5F, -5.5F));

        ModelPartData Walls = TARDIS.addChild("Walls", ModelPartBuilder.create().uv(63, 227).cuboid(-14.0F, -56.0F, -6.0F, 1.0F, 56.0F, 1.0F, new Dilation(0.0F))
                .uv(116, 170).cuboid(13.0F, -56.0F, -6.0F, 1.0F, 56.0F, 1.0F, new Dilation(0.0F))
                .uv(115, 0).cuboid(-13.0F, -56.0F, -6.0F, 26.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(59, 113).cuboid(13.0F, -56.0F, -6.5F, 1.0F, 56.0F, 0.0F, new Dilation(0.0F))
                .uv(115, 3).cuboid(-13.0F, -56.0F, -6.5F, 26.0F, 1.0F, 0.0F, new Dilation(0.0F))
                .uv(62, 113).cuboid(-14.0F, -56.0F, -6.5F, 1.0F, 56.0F, 0.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData PCB = TARDIS.addChild("PCB", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -1.0F, 0.0F));

        ModelPartData cube_r3 = PCB.addChild("cube_r3", ModelPartBuilder.create().uv(160, 9).cuboid(-17.0F, -61.0F, 13.0F, 34.0F, 5.0F, 6.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 1.0F, 10.0F, 0.0F, 3.1416F, 0.0F));
        return TexturedModelData.of(modelData, 512, 512);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        TARDIS.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }

    @Override
    public void renderWithAnimations(DoorBlockEntity doorEntity, ModelPart root, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float pAlpha) {

        this.TARDIS.getChild("Doors").getChild("left_door").yaw = (doorEntity.getClientTardis().getExterior().isLeftDoorOpen() || doorEntity.getClientTardis().getExterior().isBothDoorsOpen())  ? -5F : 0.0F;
        this.TARDIS.getChild("Doors").getChild("right_door").yaw = (doorEntity.getClientTardis().getExterior().isRightDoorOpen() || doorEntity.getClientTardis().getExterior().isBothDoorsOpen()) ? 5F : 0.0F;

        matrices.push();
        matrices.scale(0.63F, 0.63F, 0.63F);
        matrices.translate(0, -1.5, 0.35);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180));

        super.renderWithAnimations(doorEntity, root, matrices, vertices, light, overlay, red, green, blue, pAlpha);
        matrices.pop();
    }

    @Override
    public Animation getAnimationForDoorState(DoorHandler.DoorStateEnum state) {
        return switch (state) {
            case CLOSED -> DoorAnimations.INTERIOR_BOTH_CLOSE_ANIMATION;
            case FIRST -> DoorAnimations.INTERIOR_FIRST_OPEN_ANIMATION;
            case SECOND -> DoorAnimations.INTERIOR_SECOND_OPEN_ANIMATION;
            case BOTH -> DoorAnimations.INTERIOR_BOTH_OPEN_ANIMATION;
        };
    }

    @Override
    public ModelPart getPart() {
        return TARDIS;
    }
}
