package dev.amble.ait.data.schema.door;

import java.util.Optional;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import dev.amble.ait.client.AITModClient;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.core.tardis.handler.DoorHandler;
import dev.amble.ait.data.schema.AnimatedFeature;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;

public interface AnimatedDoor extends AnimatedFeature {
    default Optional<BedrockAnimationReference> getLeftAnimation() {
        return Optional.empty();
    }

    default Optional<BedrockAnimationReference> getRightAnimation() {
        return Optional.empty();
    }

    @Environment(EnvType.CLIENT)
    private static void applyDoorAnimation(ModelPart root, float progress, float delta, boolean opening,
                                           Optional<BedrockAnimationReference> openAnim,
                                           Optional<BedrockAnimationReference> closeAnim) {
        boolean hasCloseAnim = closeAnim.isPresent() && closeAnim.get().get().isPresent();

        if (hasCloseAnim) {
            // Separate open/close animations available
            if (opening) {
                // Door is opening or held open - scrub through open animation with progress 0->1
                float p = Math.max(progress - 0.001F, 0F);
                openAnim.flatMap(BedrockAnimationReference::get)
                        .ifPresent(anim -> anim.apply(root, (int) (p * anim.animationLength * 20), delta));
            } else {
                // Door is closing - scrub through close animation
                // progress goes 1->0 as door closes, so close anim progress = 1 - progress (0->1)
                float p = Math.max((1.0F - progress) - 0.001F, 0F);
                closeAnim.flatMap(BedrockAnimationReference::get)
                        .ifPresent(anim -> anim.apply(root, (int) (p * anim.animationLength * 20), delta));
            }
        } else {
            // Single animation mode - reverse for close (original behavior)
            float finalProgress = progress - 0.001F;
            openAnim.flatMap(BedrockAnimationReference::get)
                    .ifPresent(anim -> anim.apply(root, (int) (finalProgress * anim.animationLength * 20), delta));
        }
    }

    /**
     * Optional separate close animation for the left door.
     * If empty, the open animation is played in reverse for closing.
     */
    default Optional<BedrockAnimationReference> getLeftCloseAnimation() {
        return Optional.empty();
    }

    default Vec3d getScale() {
        return new Vec3d(1, 1, 1);
    }

    default Vec3d getOffset() {
        return Vec3d.ZERO;
    }

    /**
     * Optional separate close animation for the right door.
     * If empty, the open animation is played in reverse for closing.
     */
    default Optional<BedrockAnimationReference> getRightCloseAnimation() {
        return Optional.empty();
    }

    @Environment(EnvType.CLIENT)
    default void runAnimations(ModelPart root, MatrixStack matrices, float tickDelta, ClientTardis tardis) {
        DoorHandler doors = tardis.door();

        Vec3d offset = this.getOffset().multiply(-1);
        matrices.translate(offset.x, offset.y, offset.z);

        Vec3d scale = this.getScale();
        matrices.scale((float) scale.x, (float) scale.y, (float) scale.z);

        matrices.push();
        float leftProgress = doors.getLeftRot();
        float rightProgress = doors.getRightRot();

        boolean leftOpening = doors.isLeftOpen();
        boolean rightOpening = doors.isRightOpen();

        if (!AITModClient.CONFIG.animateDoors) {
            leftProgress = leftOpening ? 1 : 0;
            rightProgress = rightOpening ? 1 : 0;
        }

        float leftDelta;
        if (leftProgress == 1 || leftProgress == 0) {
            leftDelta = 0;
        } else {
            leftDelta = tickDelta / 10F;
        }

        float rightDelta;
        if (rightProgress == 1 || rightProgress == 0) {
            rightDelta = 0;
        } else {
            rightDelta = tickDelta / 10F;
        }

        // Left door animation
        applyDoorAnimation(root, leftProgress, leftDelta, leftOpening,
                this.getLeftAnimation(), this.getLeftCloseAnimation());

        // Right door animation
        applyDoorAnimation(root, rightProgress, rightDelta, rightOpening,
                this.getRightAnimation(), this.getRightCloseAnimation());

        matrices.pop();
    }
}
