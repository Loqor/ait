package dev.amble.ait.data.datapack;

import static dev.amble.ait.data.datapack.DatapackConsole.EMPTY;

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

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.util.PortalOffsets;
import dev.amble.ait.data.Loyalty;
import dev.amble.ait.data.datapack.exterior.BiomeOverrides;
import dev.amble.ait.data.schema.door.AnimatedDoor;
import dev.amble.ait.data.schema.door.DoorSchema;
import dev.amble.ait.data.schema.exterior.ExteriorVariantSchema;
import dev.amble.ait.registry.impl.door.DoorRegistry;
import dev.amble.ait.registry.impl.exterior.ExteriorVariantRegistry;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DatapackExterior extends ExteriorVariantSchema implements AnimatedDoor, TravelAnimationMap.Holder {

    public static final Identifier DEFAULT_TEXTURE = new Identifier(AITMod.MOD_ID,
            "textures/gui/tardis/desktop/missing_preview.png");

    public static final Codec<DatapackExterior> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(Identifier.CODEC.fieldOf("id").forGetter(ExteriorVariantSchema::id),
                    Identifier.CODEC.fieldOf("category").forGetter(ExteriorVariantSchema::categoryId),
                    Identifier.CODEC.fieldOf("parent").forGetter(DatapackExterior::getParentId),
                    Identifier.CODEC.fieldOf("texture").forGetter(DatapackExterior::texture),
                    Identifier.CODEC.optionalFieldOf("emission", EMPTY).forGetter(DatapackExterior::emission),
                    Loyalty.CODEC.optionalFieldOf("loyalty").forGetter(DatapackExterior::requirement),
                    BiomeOverrides.CODEC.fieldOf("overrides").orElse(BiomeOverrides.EMPTY)
                            .forGetter(DatapackExterior::overrides),
                    Vec3d.CODEC.optionalFieldOf("seat_translations", new Vec3d(0.5, 1, 0.5)).forGetter(DatapackExterior::seatTranslations),
                    Codec.BOOL.optionalFieldOf("has_transparent_doors", false).forGetter(DatapackExterior::hasTransparentDoors),
                    Identifier.CODEC.optionalFieldOf("model").forGetter(DatapackExterior::model),
                    Identifier.CODEC.optionalFieldOf("door").forGetter(DatapackExterior::getDoorId),
                    PortalOffsets.CODEC.optionalFieldOf("portal_info").forGetter(ext -> Optional.ofNullable(ext.getPortalOffsets())),
                    DoorAnimationReferences.LEGACY_AWARE_CODEC.forGetter(DatapackExterior::getDoorAnimations),
                    Vec3d.CODEC.optionalFieldOf("scale", new Vec3d(1, 1, 1)).forGetter(DatapackExterior::getScale),
                    TravelAnimationMap.CODEC.optionalFieldOf("animations", new TravelAnimationMap())
                            .forGetter(DatapackExterior::getAnimations)
            ).apply(instance, DatapackExterior::new)
    );

    protected final Identifier parent;
    protected final Identifier texture;
    protected final Identifier emission;
    protected final BiomeOverrides overrides;
    protected final Vec3d seatTranslations;
    protected final boolean initiallyDatapack;
    protected final boolean hasTransparentDoors;
    protected final Identifier model;
    protected final Identifier doorId;
    protected final PortalOffsets portalOffsets;
    protected final DoorAnimationReferences doorAnimations;
    protected final Vec3d scale;
    protected final TravelAnimationMap animations;

    public DatapackExterior(Identifier id, Identifier category, Identifier parent, Identifier texture,
                            Identifier emission, Optional<Loyalty> loyalty, BiomeOverrides overrides, Vec3d seatTranslations, boolean hasTransparentDoors, Optional<Identifier> model, Optional<Identifier> door, Optional<PortalOffsets> offsets, DoorAnimationReferences doorAnimations, Vec3d scale, TravelAnimationMap animations) {
        super(category, id, loyalty);
        this.parent = parent;
        this.texture = texture;
        this.emission = emission;
        this.seatTranslations = seatTranslations;
        this.hasTransparentDoors = hasTransparentDoors;
        this.initiallyDatapack = true;
        this.overrides = overrides;
        this.model = model.orElse(null);
        this.doorId = door.orElse(null);
        this.portalOffsets = offsets.orElse(null);
        this.doorAnimations = doorAnimations;
        this.scale = scale;
        this.animations = animations;
    }

    public static DatapackExterior fromInputStream(InputStream stream) {
        return fromJson(JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject());
    }

    public static DatapackExterior fromJson(JsonObject json) {
        AtomicReference<DatapackExterior> created = new AtomicReference<>();

        CODEC.decode(JsonOps.INSTANCE, json).get().ifLeft(var -> created.set(var.getFirst())).ifRight(err -> {
            created.set(null);
            AITMod.LOGGER.error("Error decoding datapack exterior variant: {}", err);
        });

        return created.get();
    }

    public ExteriorVariantSchema getParent() {
        return ExteriorVariantRegistry.getInstance().get(this.getParentId());
    }

    public Identifier getParentId() {
        return this.parent;
    }

    private Optional<Identifier> getDoorId() {
        return Optional.ofNullable(this.doorId);
    }

    @Override
    public DoorSchema door() {
        ExteriorVariantSchema parent = this.getParent();

        if (doorId == null)
            return parent != null ? parent.door() : null;

        return DoorRegistry.getInstance().getOrElse(doorId, parent != null ? parent.door() : null);
    }

    public BiomeOverrides overrides() {
        return overrides;
    }

    @Override
    public Vec3d seatTranslations() {
        return seatTranslations;
    }

    public boolean hasTransparentDoors() {
        return hasTransparentDoors;
    }

    @Override
    public VoxelShape bounding(Direction dir) {
        return this.getParent().bounding(dir);
    }

    @Override
    public boolean hasPortals() {
        if (this.getPortalOffsets() != null) {
            return this.getPortalOffsets().enabled();
        }

        return this.getParent().hasPortals();
    }

    @Override
    public @Nullable Vec3d getPortalPosition() {
        if (this.getPortalOffsets() != null) {
            return this.getPortalOffsets().offset();
        }

        return this.getParent().getPortalPosition();
    }

    @Override
    public double portalWidth() {
        if (this.getPortalOffsets() != null) {
            return this.getPortalOffsets().width();
        }

        return this.getParent().portalWidth();
    }

    @Override
    public double portalHeight() {
        if (this.getPortalOffsets() != null) {
            return this.getPortalOffsets().height();
        }

        return this.getParent().portalHeight();
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

    /**
     * A possible identifier for a bedrock model in the BedrockModelRegistry.
     */
    public Optional<Identifier> model() {
        return Optional.ofNullable(this.model);
    }

    public PortalOffsets getPortalOffsets() {
        return this.portalOffsets;
    }

    @Override
    public Optional<BedrockAnimationReference> getLeftAnimation() {
        return doorAnimations.getLeft();
    }

    @Override
    public Optional<BedrockAnimationReference> getRightAnimation() {
        return doorAnimations.getRight();
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
    public Vec3d getScale() {
        return scale;
    }

    @Override
    public TravelAnimationMap getAnimations() {
        return animations;
    }
}
