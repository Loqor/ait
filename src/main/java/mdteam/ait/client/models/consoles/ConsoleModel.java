package mdteam.ait.client.models.consoles;

import mdteam.ait.client.animation.console.hartnell.HartnellAnimations;
import mdteam.ait.core.blockentities.ConsoleBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import mdteam.ait.tardis.TardisTravel;

import java.util.HashMap;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public abstract class ConsoleModel extends SinglePartEntityModel {
    public static int MAX_TICK_COUNT = 2 * 20;

    public ConsoleModel() {
        this(RenderLayer::getEntityCutoutNoCull);
    }

    public ConsoleModel(Function<Identifier, RenderLayer> function) {
        super(function);
    }

    // Thanks craig for help w animation code
    public void animateTile(ConsoleBlockEntity console) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
        if (console.getClientTardis() == null)
            return;
        // System.out.println(getAnimationForState(console.getTardis().getTravel().getState()));

        TardisTravel.State state = console.getClientTardis().getTravel().getState();

        if (!console.getClientTardis().isPowered()) return;

        this.updateAnimation(console.ANIM_FLIGHT, getAnimationForState(state), console.animationTimer);
        /*if(console.getControlEntityFromName("direction") != null && console.getControlEntityFromName("direction").getControl() != null) {
            this.updateAnimation(console.getControlEntityFromName("direction")
                            .getControl().getAnimationState(HartnellAnimations.animationFromDirection(console.getTardis().getTravel())),
                    console.getControlEntityFromName("direction").getControl().getAnimation(HartnellAnimations.animationFromDirection(console.getTardis().getTravel())), console.animationTimer);
        }*/
    }

    public void renderWithAnimations(ConsoleBlockEntity console, ModelPart root, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float pAlpha) {
        if (console.getClientTardis() == null) return;
        root.render(matrices, vertices, light, overlay, red, green, blue, pAlpha);
    }

    @Override
    public void setAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    public abstract Animation getAnimationForState(TardisTravel.State state);
}
