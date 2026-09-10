package dev.amble.ait.data.schema.door;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.util.PortalOffsets;
import dev.amble.ait.data.datapack.DoorAnimationReferences;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;

public class DatapackDoor extends DoorSchema implements AnimatedDoor {
    public static final Codec<DatapackDoor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(DoorSchema::id),
            Identifier.CODEC.fieldOf("open_sound").forGetter(DatapackDoor::getOpenSoundId),
            Identifier.CODEC.fieldOf("close_sound").forGetter(DatapackDoor::getCloseSoundId),
            Identifier.CODEC.fieldOf("model").forGetter(DatapackDoor::getModelId),
            Codec.BOOL.fieldOf("is_double").forGetter(DoorSchema::isDouble),
            PortalOffsets.CODEC.optionalFieldOf("portal_info").forGetter(door -> Optional.ofNullable(door.getPortalOffsets())),
            BedrockAnimationReference.CODEC.optionalFieldOf("left_animation").forGetter(DatapackDoor::getRawLeftAnimation),
            BedrockAnimationReference.CODEC.optionalFieldOf("right_animation").forGetter(DatapackDoor::getRawRightAnimation),
            DoorAnimationReferences.CODEC.optionalFieldOf("door_animations", DoorAnimationReferences.EMPTY)
                    .forGetter(DatapackDoor::getDoorAnimations),
            Vec3d.CODEC.optionalFieldOf("scale", new Vec3d(1, 1, 1)).forGetter(DatapackDoor::getScale),
            Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(DatapackDoor::getOffset),
            Codec.BOOL.optionalFieldOf("isDatapack", true).forGetter(DatapackDoor::wasDatapack)
        ).apply(instance, DatapackDoor::new)
    );

    protected final Identifier openSound;
    protected final Identifier closeSound;
    protected final Identifier model;
    protected final boolean isDouble;
    protected final PortalOffsets portalOffsets;
    protected final BedrockAnimationReference leftAnimation;
    protected final BedrockAnimationReference rightAnimation;
    protected final DoorAnimationReferences doorAnimations;
    protected final Vec3d scale;
    protected final Vec3d offset;
    protected final boolean initiallyDatapack;

    public DatapackDoor(Identifier id, Identifier openSound, Identifier closeSound, Identifier model, boolean isDouble, Optional<PortalOffsets> portalOffsets, Optional<BedrockAnimationReference> leftAnimation, Optional<BedrockAnimationReference> rightAnimation, DoorAnimationReferences doorAnimations, Vec3d scale, Vec3d offset, boolean initiallyDatapack) {
        super(id);

        this.openSound = openSound;
        this.closeSound = closeSound;
        this.model = model;
        this.isDouble = isDouble;
        this.portalOffsets = portalOffsets.orElse(null);
        this.leftAnimation = leftAnimation.orElse(null);
        this.rightAnimation = rightAnimation.orElse(null);
        this.doorAnimations = doorAnimations;
        this.scale = scale;
        this.offset = offset;
        this.initiallyDatapack = initiallyDatapack;
    }

    @Override
    public boolean isDouble() {
        return isDouble;
    }

    @Override
    public SoundEvent openSound() {
        return SoundEvent.of(getOpenSoundId());
    }

    @Override
    public SoundEvent closeSound() {
        return SoundEvent.of(getCloseSoundId());
    }

    private Optional<BedrockAnimationReference> getRawLeftAnimation() {
        return Optional.ofNullable(leftAnimation);
    }

    private Optional<BedrockAnimationReference> getRawRightAnimation() {
        return Optional.ofNullable(rightAnimation);
    }

    @Override
    public Optional<BedrockAnimationReference> getLeftAnimation() {
        if (!doorAnimations.isEmpty() && doorAnimations.getLeft().isPresent()) {
            return doorAnimations.getLeft();
        }
        return Optional.ofNullable(leftAnimation);
    }

    @Override
    public Optional<BedrockAnimationReference> getRightAnimation() {
        if (!doorAnimations.isEmpty() && doorAnimations.getRight().isPresent()) {
            return doorAnimations.getRight();
        }
        return Optional.ofNullable(rightAnimation);
    }

    @Override
    public Optional<BedrockAnimationReference> getLeftCloseAnimation() {
        return doorAnimations.getLeftClose();
    }

    @Override
    public Optional<BedrockAnimationReference> getRightCloseAnimation() {
        return doorAnimations.getRightClose();
    }

    public DoorAnimationReferences getDoorAnimations() {
        return doorAnimations;
    }

    @Override
    public @Nullable Vec3d getPortalPosition() {
        return this.portalOffsets != null ? this.portalOffsets.offset() : null;
    }

    public Identifier getOpenSoundId() {
        return openSound;
    }

    public Identifier getCloseSoundId() {
        return closeSound;
    }

    public PortalOffsets getPortalOffsets() {
        return portalOffsets;
    }

    public boolean wasDatapack() {
        return initiallyDatapack; // Datapack doors are always considered as such
    }

    public Identifier getModelId() {
        return model;
    }

    @Override
    public Vec3d getScale() {
        return scale;
    }

    @Override
    public Vec3d getOffset() {
        return offset;
    }

    public static DatapackDoor fromInputStream(InputStream stream) {
        return fromJson(JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject());
    }

    public static DatapackDoor fromJson(JsonObject json) {
        AtomicReference<DatapackDoor> created = new AtomicReference<>();

        CODEC.decode(JsonOps.INSTANCE, json).get().ifLeft(recipe -> {
            created.set(recipe.getFirst());
        }).ifRight(err -> {
            created.set(null);
            AITMod.LOGGER.error("Error decoding datapack door type: {}", err);
        });

        return created.get();
    }

}
