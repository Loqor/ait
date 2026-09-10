package dev.amble.ait.core.tardis.control;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amble.ait.AITMod;
import dev.amble.ait.core.blocks.ConsoleBlock;
import dev.amble.ait.core.entities.ConsoleControlEntity;
import dev.amble.ait.data.codec.MoreCodec;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.ait.registry.impl.ControlRegistry;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Holds a control which will be ran when interacted with, an
 * {@linkplain Vector3f offset} from the centre of the {@link ConsoleBlock} and
 * a {@linkplain EntityDimensions scale} for the entity <br>
 * <br>
 * A list of these is gotten by {@link ConsoleTypeSchema#getControlTypes()} and
 * used in {@link ConsoleControlEntity} to hold its information
 *
 * @author loqor
 * @see ConsoleControlEntity
 */
public class ControlTypes {
    public static final Codec<ControlTypes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(c -> c.getControl().id()),
            Codec.FLOAT.fieldOf("width").forGetter(c -> c.getScale().width),
            Codec.FLOAT.fieldOf("height").forGetter(c -> c.getScale().height),
		    MoreCodec.VECTOR3F.fieldOf("offset").forGetter(ControlTypes::getOffset),
		    BedrockAnimationReference.CODEC.optionalFieldOf("animation").forGetter(ControlTypes::getAnimation)
    ).apply(instance, ControlTypes::new));

    private final Control control;
    private EntityDimensions scale;
    private Vector3f offset;
	private final @Nullable BedrockAnimationReference animation;

	public ControlTypes(Control control, EntityDimensions scaling, Vector3f offset, @Nullable BedrockAnimationReference animation) {
        this.control = control;
        this.scale = scaling;
        this.offset = offset;
		this.animation = animation;
    }

	public ControlTypes(Control control, EntityDimensions scaling, Vector3f offset) {
		this(control, scaling, offset, null);
	}

	public ControlTypes(Identifier controlId, float width, float height, Vector3f offset, Optional<BedrockAnimationReference> animation) {
		this(ControlRegistry.REGISTRY.getOrEmpty(controlId).orElseGet(ControlRegistry::fallback), EntityDimensions.fixed(width, height), offset, animation.orElse(null));
    }

    @Override
    public String toString() {
        return "ControlTypes{" + "control=" + control + ", scale=" + scale + ", offset=" + offset + '}';
    }

    public Control getControl() {
        if (this.control == null) {
            AITMod.LOGGER.error("ControlTypes has null control! Returning fallback control.");
            return ControlRegistry.fallback();
        }

        return this.control;
    }

    public EntityDimensions getScale() {
        return this.scale;
    }

    public void setScale(EntityDimensions scale) {
        this.scale = scale;
    }

    public Vector3f getOffset() {
        return this.offset;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

	public Optional<BedrockAnimationReference> getAnimation() {
		return Optional.ofNullable(this.animation);
	}

	public boolean hasAnimation() {
		return this.animation != null;
	}
}
