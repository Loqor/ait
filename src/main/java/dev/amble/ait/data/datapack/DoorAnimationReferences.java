package dev.amble.ait.data.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;

import java.util.Optional;

/**
 * Holds references to door open/close animations for both left and right doors.
 * Supports both single-animation (reversed for close) and separate open/close animations.
 */
public class DoorAnimationReferences {
    public static final DoorAnimationReferences EMPTY = new DoorAnimationReferences(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<DoorAnimationReferences> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    BedrockAnimationReference.CODEC.optionalFieldOf("left").forGetter(DoorAnimationReferences::getLeft),
                    BedrockAnimationReference.CODEC.optionalFieldOf("right").forGetter(DoorAnimationReferences::getRight),
                    BedrockAnimationReference.CODEC.optionalFieldOf("left_close").forGetter(DoorAnimationReferences::getLeftClose),
                    BedrockAnimationReference.CODEC.optionalFieldOf("right_close").forGetter(DoorAnimationReferences::getRightClose)
            ).apply(instance, DoorAnimationReferences::new)
    );

    /**
     * Reads the grouped {@code door_animations} object together with the legacy top level
     * {@code left_animation} / {@code right_animation} keys, so a pack written against either
     * spelling still loads. The grouped values win where a door supplies both.
     *
     * <p>This occupies a single {@code group} slot, which matters for schemas already close to
     * {@link RecordCodecBuilder}'s sixteen field ceiling. Re-serializing writes everything back
     * under {@code door_animations}.
     */
    public static final MapCodec<DoorAnimationReferences> LEGACY_AWARE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    CODEC.optionalFieldOf("door_animations", EMPTY).forGetter(refs -> refs),
                    BedrockAnimationReference.CODEC.optionalFieldOf("left_animation").forGetter(refs -> Optional.empty()),
                    BedrockAnimationReference.CODEC.optionalFieldOf("right_animation").forGetter(refs -> Optional.empty())
            ).apply(instance, DoorAnimationReferences::withLegacy)
    );

    private static DoorAnimationReferences withLegacy(DoorAnimationReferences grouped,
                                                      Optional<BedrockAnimationReference> legacyLeft,
                                                      Optional<BedrockAnimationReference> legacyRight) {
        if (legacyLeft.isEmpty() && legacyRight.isEmpty())
            return grouped;

        return new DoorAnimationReferences(grouped.getLeft().or(() -> legacyLeft),
                grouped.getRight().or(() -> legacyRight), grouped.getLeftClose(), grouped.getRightClose());
    }

    private final BedrockAnimationReference left;
    private final BedrockAnimationReference right;
    private final BedrockAnimationReference leftClose;
    private final BedrockAnimationReference rightClose;

    public DoorAnimationReferences(Optional<BedrockAnimationReference> left,
                                   Optional<BedrockAnimationReference> right,
                                   Optional<BedrockAnimationReference> leftClose,
                                   Optional<BedrockAnimationReference> rightClose) {
        this.left = left.orElse(null);
        this.right = right.orElse(null);
        this.leftClose = leftClose.orElse(null);
        this.rightClose = rightClose.orElse(null);
    }

    public Optional<BedrockAnimationReference> getLeft() {
        return Optional.ofNullable(left);
    }

    public Optional<BedrockAnimationReference> getRight() {
        return Optional.ofNullable(right);
    }

    public Optional<BedrockAnimationReference> getLeftClose() {
        return Optional.ofNullable(leftClose);
    }

    public Optional<BedrockAnimationReference> getRightClose() {
        return Optional.ofNullable(rightClose);
    }

    public boolean isEmpty() {
        return left == null && right == null && leftClose == null && rightClose == null;
    }
}

