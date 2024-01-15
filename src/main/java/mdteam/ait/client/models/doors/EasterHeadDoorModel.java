package mdteam.ait.client.models.doors;

import mdteam.ait.client.animation.exterior.door.DoorAnimations;
import mdteam.ait.client.animation.exterior.door.easter_head.EasterHeadAnimations;
import mdteam.ait.core.blockentities.DoorBlockEntity;
import mdteam.ait.tardis.handler.DoorHandler;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

public class EasterHeadDoorModel extends DoorModel {
	private final ModelPart bottom;
	public EasterHeadDoorModel(ModelPart root) {
		this.bottom = root.getChild("bottom");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData bottom = modelPartData.addChild("bottom", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 54.0F, 0.0F));

		ModelPartData cube_r1 = bottom.addChild("cube_r1", ModelPartBuilder.create().uv(0, 0).cuboid(-12.0F, 30.0F, -12.0F, 24.0F, 14.0F, 24.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.1416F));

		ModelPartData door = bottom.addChild("door", ModelPartBuilder.create().uv(8, 1).mirrored().cuboid(-9.0F, -30.0F, -8.0F, 18.0F, 0.0F, 19.0F, new Dilation(0.005F)).mirrored(false), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		return TexturedModelData.of(modelData, 256, 256);
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		bottom.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}

	@Override
	public void renderWithAnimations(DoorBlockEntity door, ModelPart root, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float pAlpha) {
		matrices.push();

		matrices.translate(0,-1.5f,0);

		if (door.getClientTardis().getExterior().isDoorOpen())
			this.bottom.translate(new Vector3f(0,-30,0));

		super.renderWithAnimations(door, root, matrices, vertices, light, overlay, red, green, blue, pAlpha);

		matrices.pop();
	}

	@Override
	public ModelPart getPart() {
		return bottom;
	}

	@Override
	public Animation getAnimationForDoorState(DoorHandler.DoorStateEnum state) {
		return switch (state) {
			case CLOSED -> EasterHeadAnimations.EASTER_HEAD_INTERIOR_DOOR_CLOSE_ANIMATION;
			case FIRST -> EasterHeadAnimations.EASTER_HEAD_INTERIOR_DOOR_OPEN_ANIMATION;
			default -> Animation.Builder.create(0).build();
		};
	}
}