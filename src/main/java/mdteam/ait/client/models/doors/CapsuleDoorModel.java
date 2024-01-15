package mdteam.ait.client.models.doors;

import mdteam.ait.client.animation.exterior.door.DoorAnimations;
import mdteam.ait.client.models.doors.DoorModel;
import mdteam.ait.compat.DependencyChecker;
import mdteam.ait.core.blockentities.DoorBlockEntity;
import mdteam.ait.tardis.handler.DoorHandler;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

// Made with Blockbench 4.9.1
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class CapsuleDoorModel extends DoorModel {
    private final ModelPart body;

    public CapsuleDoorModel(ModelPart root) {
        super(RenderLayer::getEntityCutoutNoCull);
        this.body = root.getChild("body");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData body = modelPartData.addChild("body", ModelPartBuilder.create(), ModelTransform.of(0.0F, 3.0F, -15.0F, 0.0F, 3.1416F, 0.0F));

        ModelPartData top = body.addChild("top", ModelPartBuilder.create().uv(87, 15).cuboid(-12.0F, -36.1F, -12.0F, 24.0F, 0.0F, 10.0F, new Dilation(0.0F))
                .uv(15, 40).cuboid(-12.0F, -33.89F, -12.0F, 24.0F, 0.0F, 9.0F, new Dilation(0.0F))
                .uv(61, 114).cuboid(-4.9706F, -36.0F, -12.0F, 9.9411F, 2.0F, 8.0F, new Dilation(0.001F)), ModelTransform.pivot(0.0F, 21.0F, 0.0F));

        ModelPartData octagon_r1 = top.addChild("octagon_r1", ModelPartBuilder.create().uv(125, 35).cuboid(-4.9706F, -2.0F, -12.0F, 3.0F, 2.0F, 24.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -34.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        ModelPartData octagon_r2 = top.addChild("octagon_r2", ModelPartBuilder.create().uv(20, 101).cuboid(-4.9706F, -2.0F, -12.0F, 9.9411F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -34.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        ModelPartData octagon_r3 = top.addChild("octagon_r3", ModelPartBuilder.create().uv(93, 85).cuboid(-4.9706F, -2.0F, -12.0F, 9.9411F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -34.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        ModelPartData middle = body.addChild("middle", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 21.0F, 0.0F));

        ModelPartData octagon_r4 = middle.addChild("octagon_r4", ModelPartBuilder.create().uv(48, 135).cuboid(-2.2365F, -34.0F, 9.5F, 8.0F, 32.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -3.1416F, -0.7854F, 3.1416F));

        ModelPartData octagon_r5 = middle.addChild("octagon_r5", ModelPartBuilder.create().uv(142, 128).cuboid(-5.7635F, -34.0F, 9.5F, 8.0F, 32.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -3.1416F, 0.7854F, 3.1416F));

        ModelPartData back = middle.addChild("back", ModelPartBuilder.create().uv(146, 0).cuboid(-12.0F, -34.0F, -4.0F, 24.0F, 32.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData bottom = body.addChild("bottom", ModelPartBuilder.create().uv(135, 85).cuboid(-5.0294F, -2.0F, -12.0F, 10.0F, 2.0F, 7.0F, new Dilation(0.001F))
                .uv(14, 64).cuboid(-12.0F, 0.01F, -12.0F, 24.0F, 0.0F, 10.0F, new Dilation(0.0F))
                .uv(14, 15).cuboid(-12.0F, -2.1F, -12.0F, 24.0F, 0.0F, 10.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 21.0F, 0.0F));

        ModelPartData octagon_r6 = bottom.addChild("octagon_r6", ModelPartBuilder.create().uv(125, 68).cuboid(-4.9706F, -2.0F, -12.0F, 3.0F, 2.0F, 24.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        ModelPartData octagon_r7 = bottom.addChild("octagon_r7", ModelPartBuilder.create().uv(138, 55).cuboid(-4.9706F, -2.0F, -12.0F, 9.9411F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        ModelPartData octagon_r8 = bottom.addChild("octagon_r8", ModelPartBuilder.create().uv(20, 128).cuboid(-4.9706F, -2.0F, -12.0F, 9.9411F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        ModelPartData doors = body.addChild("doors", ModelPartBuilder.create(), ModelTransform.of(0.0F, -2.0F, -17.0F, 0.0F, 3.1416F, 0.0F));

        ModelPartData door_right = doors.addChild("door_right", ModelPartBuilder.create().uv(161, 95).cuboid(0.4706F, -11.0F, -0.5F, 6.0F, 32.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-6.5F, 0.0F, -8.5F));

        ModelPartData door_left = doors.addChild("door_left", ModelPartBuilder.create().uv(162, 162).cuboid(-6.5294F, -11.0F, -0.5F, 6.0F, 32.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(6.5F, 0.0F, -8.5F));

        return TexturedModelData.of(modelData, 256, 256);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        body.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
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
        return body;
    }

    @Override
    public void renderWithAnimations(DoorBlockEntity door, ModelPart root, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float pAlpha) {
        if (door.getTardis() == null) return;
        matrices.push();
        // matrices.scale(0.6F,0.6f,0.6f);
        matrices.translate(0, -1.5f, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));

        /*this.body.getChild("doors").getChild("left_door").yaw = door.getLeftDoorRotation();
        this.body.getChild("doors").getChild("right_door").yaw = -door.getRightDoorRotation();*/

        DoorHandler handler = door.getTardis().getDoor();

        this.body.getChild("doors").getChild("door_left").yaw = (handler.isLeftOpen() || handler.isOpen())  ? -5F : 0.0F;
        this.body.getChild("doors").getChild("door_right").yaw = (handler.isRightOpen() || handler.isBothOpen()) ? 5F : 0.0F;

         if (DependencyChecker.hasPortals())
             this.getPart().getChild("middle").getChild("back").visible = false;

        super.renderWithAnimations(door, root, matrices, vertices, light, overlay, red, green, blue, pAlpha);

        matrices.pop();
    }
}