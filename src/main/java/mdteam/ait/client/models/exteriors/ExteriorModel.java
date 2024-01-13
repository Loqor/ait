package mdteam.ait.client.models.exteriors;

import mdteam.ait.core.blockentities.ExteriorBlockEntity;
import mdteam.ait.core.entities.FallingTardisEntity;
import mdteam.ait.core.entities.TardisRealEntity;
import mdteam.ait.tardis.handler.DoorHandler;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import java.util.function.Function;

@SuppressWarnings("rawtypes")
public abstract class ExteriorModel extends SinglePartEntityModel {
    public static int MAX_TICK_COUNT = 2 * 20;

    public ExteriorModel() {
        this(RenderLayer::getEntityCutoutNoCull);
    }

    public ExteriorModel(Function<Identifier, RenderLayer> function) {
        super(function);
    }

    // Thanks craig for help w animation code @TODO more craig thank yous
    public void animateTile(ExteriorBlockEntity exterior) {
        // this.getPart().traverse().forEach(ModelPart::resetTransform);
        // if (exterior.tardis() == null)
        //     return;
        // DoorHandler.DoorStateEnum state = exterior.tardis().getDoor().getDoorState();
        // // checkAnimationTimer(exterior);
        // updateAnimation(exterior.DOOR_STATE, getAnimationForDoorState(state), exterior.animationTimer);
    }

    private static float getAnimationLengthInTicks(Animation anim) {
        return anim.lengthInSeconds() * 20;
    }

    private void checkAnimationTimer(ExteriorBlockEntity exterior) {
        DoorHandler.DoorStateEnum state = exterior.getClientTardis().getExterior().getDoorState();
        Animation anim = getAnimationForDoorState(state);


        int max = (int) getAnimationLengthInTicks(anim);
        if (exterior.animationTimer > max) {
            exterior.animationTimer = max;
        }
    }

    public void renderWithAnimations(ExteriorBlockEntity exterior, ModelPart root, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float pAlpha) {

        root.render(matrices, vertices, light, overlay, red, green, blue, exterior.getAlpha());
    }

    public void renderFalling(FallingTardisEntity falling, ModelPart root, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        root.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }

    public void renderRealWorld(TardisRealEntity realEntity, ModelPart root, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {

        root.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }

    @Override
    public void setAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }


    public abstract Animation getAnimationForDoorState(DoorHandler.DoorStateEnum state);
}
