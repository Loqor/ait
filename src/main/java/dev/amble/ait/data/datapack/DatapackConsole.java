package dev.amble.ait.data.datapack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.tardis.control.ControlTypes;
import dev.amble.ait.core.util.Transformations;
import dev.amble.ait.data.codec.MoreCodec;
import dev.amble.ait.data.schema.console.ConsoleTypeSchema;
import dev.amble.ait.data.schema.console.ConsoleVariantSchema;
import dev.amble.ait.registry.impl.console.ConsoleRegistry;
import dev.amble.ait.registry.impl.console.variant.ConsoleVariantRegistry;

// Example usage
/*
{
  "id": "ait:white_alnico",
  "parent": "ait:console/alnico",
  "texture": "ait:textures/console/alnico.png",
  "emission": "ait:textures/console/alnico_emission.png"
}
 */
public class DatapackConsole extends ConsoleVariantSchema implements TravelAnimationMap.Holder {
    public static final Identifier EMPTY = AITMod.id("intentionally_empty");

    public static final Codec<DatapackConsole> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(Identifier.CODEC.fieldOf("id").forGetter(ConsoleVariantSchema::id),
                    Identifier.CODEC.optionalFieldOf("parent").forGetter(c -> Optional.ofNullable(c.parentId())),
                    Identifier.CODEC.fieldOf("texture").forGetter(DatapackConsole::texture),
                    Identifier.CODEC.optionalFieldOf("emission", EMPTY).forGetter(DatapackConsole::emission),
                    Codec.list(Codec.FLOAT).optionalFieldOf("sonic_rotation", List.of())
                            .forGetter(DatapackConsole::sonicRotation),
                    MoreCodec.VECTOR3F.optionalFieldOf("sonic_translation", new Vector3f()).forGetter(DatapackConsole::sonicTranslation),
                    Codec.list(Codec.FLOAT).optionalFieldOf("handles_rotation", List.of())
                            .forGetter(DatapackConsole::handlesRotation),
                    MoreCodec.VECTOR3F.optionalFieldOf("handles_translation", new Vector3f()).forGetter(DatapackConsole::handlesTranslation),
                    Identifier.CODEC.optionalFieldOf("model").forGetter(DatapackConsole::model),
		            Transformations.CODEC.optionalFieldOf("transformations", Transformations.MISSING).forGetter(DatapackConsole::getTransformations),
                    TravelAnimationMap.CODEC.optionalFieldOf("animations", new TravelAnimationMap())
                            .forGetter(DatapackConsole::getAnimations),
                    SimpleType.CODEC.optionalFieldOf("type").forGetter(DatapackConsole::getCustomType),
                    Codec.BOOL.optionalFieldOf("isDatapack", true).forGetter(DatapackConsole::wasDatapack))
            .apply(instance, DatapackConsole::new));
	protected Identifier texture;
    protected final Identifier id;
	protected Identifier emission;
	protected List<Float> sonicRotation;
	protected Vector3f sonicTranslation;
	protected List<Float> handlesRotation;
	protected Vector3f handlesTranslation;
	protected Identifier model;
	protected Transformations transformations;
    protected boolean initiallyDatapack;
	protected TravelAnimationMap animations;
	private boolean animationsResolved;

    public DatapackConsole(Identifier id,
                           Optional<Identifier> category,
                           Identifier texture,
                           Identifier emission,
                           List<Float> sonicRot,
                           Vector3f sonicTranslation,
                           List<Float> handlesRot,
                           Vector3f handlesTranslation,
                           Optional<Identifier> model,
                           Transformations transformations,
                           TravelAnimationMap animations,
                           Optional<SimpleType> type,
                           boolean isDatapack) {
        super(resolveParentId(category, type), id);
        this.id = id;
        this.texture = texture;
        this.emission = emission;
        this.initiallyDatapack = isDatapack;
        this.sonicRotation = sonicRot;
        this.sonicTranslation = sonicTranslation;
        this.handlesRotation = handlesRot;
        this.handlesTranslation = handlesTranslation;
        this.model = model.orElse(null);
	    this.transformations = transformations;
        this.animations = animations != null ? animations : new TravelAnimationMap();
    }

    private static Identifier resolveParentId(Optional<Identifier> parent, Optional<SimpleType> type) {
        if (parent.isPresent()) {
            return parent.get();
        } else if (type.isPresent()) {
            type.get().register();
            return type.get().id();
        } else {
            throw new IllegalArgumentException("DatapackConsole must have a parent or a type defined");
        }
    }

    public boolean wasDatapack() {
        return this.initiallyDatapack;
    }

    public Identifier texture() {
        return this.texture;
    }

    public Identifier emission() {
        return this.emission;
    }

    public Identifier id() {
        return this.id;
    }

    public List<Float> sonicRotation() {
        return this.sonicRotation;
    }
    public Vector3f sonicTranslation() {
        return this.sonicTranslation;
    }

    public List<Float> handlesRotation() {
        return this.handlesRotation;
    }
    public Vector3f handlesTranslation() {
        return this.handlesTranslation;
    }

    public Optional<Identifier> model() {
	    if (this.model == null) {
		    ConsoleVariantSchema parentConsole = getSameParent(val -> val instanceof DatapackConsole val2 && val2.model != null);
		    if (parentConsole instanceof DatapackConsole datapackParent) {
			    this.model = datapackParent.model;
		    }
	    }

        return Optional.ofNullable(model);
    }

	@Deprecated(forRemoval = true)
    public Vec3d getScale() {
		return getTransformations().scale();
    }

	@Deprecated(forRemoval = true)
    public Vec3d getOffset() {
		return getTransformations().offset();
	}

	public Transformations getTransformations() {
		if (transformations.equals(Transformations.MISSING)) {
			transformations = (getSameParent(val -> val instanceof DatapackConsole val2 && !val2.transformations.equals(Transformations.MISSING)) instanceof DatapackConsole other) ? other.getTransformations() : Transformations.DEFAULT;
		}

		return transformations;
	}

    @Override
    public TravelAnimationMap getAnimations() {
	    // Resolved once. This is read every frame by the console renderer, and a variant with no
	    // animations of its own and no sibling to borrow from re-ran the whole sibling search, and
	    // logged from it, on each of those frames.
	    if (!this.animationsResolved) {
		    this.animationsResolved = true;

		    if (animations.isEmpty() && getSameParent(val -> val instanceof DatapackConsole val2 && !val2.animations.isEmpty()) instanceof DatapackConsole datapackParent)
			    this.animations = datapackParent.getAnimations();
	    }

        return animations;
    }

    public Optional<SimpleType> getCustomType() {
        if (this.parent() instanceof SimpleType simpleType) {
            return Optional.of(simpleType);
        }

        ConsoleVariantSchema sibling = this.getSameParent(val -> val.parent() instanceof SimpleType);
        if (sibling != null && sibling.parent() instanceof SimpleType siblingType) {
            return Optional.of(siblingType);
        }

        return Optional.empty();
    }

	public @Nullable ConsoleVariantSchema getSameParent(Predicate<ConsoleVariantSchema> predicate) {
        if (this.parent() == null) return null;

        Optional<ConsoleVariantSchema> found = ConsoleVariantRegistry.withParent(this.parent()).stream().filter(val -> val != this).filter(predicate).findAny();

        if (found.isPresent()) return found.get();

        ConsoleVariantSchema result = ConsoleVariantRegistry.getInstance().toList().get(0);
        AITMod.LOGGER.warn("DatapackConsole {} could not find parent variant matching predicate - Returning {}", this.id(), result != null ? result.id() : null);

        return result;
    }

    public static DatapackConsole fromInputStream(InputStream stream) {
        return fromJson(JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject());
    }

    public static DatapackConsole fromJson(JsonObject json) {
        AtomicReference<DatapackConsole> created = new AtomicReference<>();

        CODEC.decode(JsonOps.INSTANCE, json).get().ifLeft(var -> created.set(var.getFirst())).ifRight(err -> {
            created.set(null);
            AITMod.LOGGER.error("Error decoding datapack console variant: {}", err);
        });

        DatapackConsole console = created.get();

        // legacy support: older datapacks set top-level "scale"/"offset" instead of a "transformations" object
        if (console != null && console.transformations.equals(Transformations.MISSING) && (json.has("scale") || json.has("offset"))) {
            Vec3d scale = json.has("scale")
                    ? Vec3d.CODEC.parse(JsonOps.INSTANCE, json.get("scale")).result().orElse(Transformations.DEFAULT.scale())
                    : Transformations.DEFAULT.scale();
            Vec3d offset = json.has("offset")
                    ? Vec3d.CODEC.parse(JsonOps.INSTANCE, json.get("offset")).result().orElse(Transformations.DEFAULT.offset())
                    : Transformations.DEFAULT.offset();
            console.transformations = new Transformations(offset, scale, Transformations.DEFAULT.rotation());
        }

        return console;
    }

    public static class SimpleType extends ConsoleTypeSchema {
        public static final Codec<SimpleType> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(Identifier.CODEC.fieldOf("id").forGetter(SimpleType::id),
                        Codec.STRING.fieldOf("name").forGetter(SimpleType::name),
                        ControlTypes.CODEC.listOf().fieldOf("controls").forGetter(c -> c.controls))
                .apply(instance, SimpleType::new));

        private final List<ControlTypes> controls;

        protected SimpleType(Identifier id, String name, List<ControlTypes> controls) {
            super(id, name);

            this.controls = controls;
        }

        @Override
        public ControlTypes[] getControlTypes() {
            return controls.toArray(new ControlTypes[0]);
        }

        @Override
        public Text text() {
            return Text.literal(this.name());
        }

        public void register() {
            ConsoleRegistry.getInstance().register(this);
        }
    }
}
