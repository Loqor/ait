package dev.amble.ait.client.models.consoles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.client.tardis.ControlAnimationState;
import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.ControlTypes;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import dev.amble.ait.data.datapack.DatapackConsole;
import dev.amble.ait.data.datapack.TravelAnimationMap;
import dev.amble.ait.data.schema.console.ConsoleVariantSchema;
import dev.amble.lib.api.Identifiable;
import dev.amble.lib.client.bedrock.BedrockAnimation;
import dev.amble.lib.client.bedrock.BedrockAnimationReference;
import dev.amble.lib.client.bedrock.BedrockModel;

public class BedrockConsoleModel implements ConsoleModel, Identifiable {
    private final BedrockModel model;
    private final ModelPart root;
	private final Map<Identifier, Optional<BedrockAnimation>> animationCache = new HashMap<>();
	private ModelPart[] flattened;

    public BedrockConsoleModel(BedrockModel model) {
        this.model = model;

        if (this.model == null) throw new IllegalStateException("Bedrock Model is null. Ensure the resource pack is loaded correctly.");

        this.root = this.model.create().createModel();
    }

    @Override
    public Identifier id() {
        return this.model.id();
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    /**
     * The model's parts as a flat array, walked once.
     *
     * <p>{@code ModelPart.traverse()} builds a {@code Stream} per node, so resetting the transforms
     * through it allocated one per part every frame for a tree that never changes. Datapack consoles
     * are the largest models in the mod, so this is the worst place to pay it.
     *
     * <p>{@link SimpleConsoleModel} caches the same walk but guards on the root it was built
     * against, because its root can be swapped. This root is assigned once in the constructor and is
     * final, so there is nothing to invalidate against.
     */
    private ModelPart[] parts() {
        if (this.flattened == null)
            this.flattened = this.root.traverse().toArray(ModelPart[]::new);

        return this.flattened;
    }

    @Override
    public void renderWithAnimations(ClientTardis tardis, ConsoleBlockEntity console, ModelPart root, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float pAlpha, float tickDelta) {
        matrices.push();

        ConsoleVariantSchema schema = console.getVariant();

        this.applyOffsets(matrices, schema);

        getPart().render(matrices, vertices, light, overlay);

        matrices.pop();

    }

	public void applyOffsets(MatrixStack matrices, ConsoleVariantSchema schema) {
		if (schema instanceof DatapackConsole datapackConsole) {
			datapackConsole.getTransformations().apply(matrices);
		}
	}

    @Override
    public void animateBlockEntity(ConsoleBlockEntity console, TravelHandlerBase.State state, boolean hasPower) {
        if (!(console.getVariant() instanceof TravelAnimationMap.Holder schema)) return;

        TravelAnimationMap map = schema.getAnimations();
        if (map == null) {
            throw new IllegalStateException("DatapackConsole " + console.getVariant().id() + " has no animations defined.");
        }

        for (ModelPart part : this.parts())
            part.resetTransform();

		this.applyControlAnimations(console);

		BedrockAnimation anim = map.getAnimation(state);
		if (anim == null) return;

		anim.apply(this.getPart(), console.ANIM_STATE, console.getAge(), 1F, null);
    }

	/**
	 * Poses every control slot on the console.
	 *
	 * <p>Driven off the console's type schema rather than its control entities. The control, its
	 * animation reference and its offsets are all schema data, and the only per control state a
	 * render needs is progress and cooldown, both of which the console holds. Slots are addressed by
	 * index so a console listing the same control more than once, as Copper does with its handbrake,
	 * keeps one animation per lever instead of sharing one between them.
	 */
	private void applyControlAnimations(ConsoleBlockEntity console) {
		if (console.tardis().isEmpty()) return;

		Tardis tardis = console.tardis().get();
		ControlTypes[] types = console.controlTypes();

		for (int i = 0; i < types.length; i++) {
			BedrockAnimationReference ref = types[i].getAnimation().orElse(null);
			if (ref == null) continue;

			BedrockAnimation anim = this.animation(ref);
			if (anim == null) continue;

			ControlAnimationState slot = console.controlAnimation(i);
			if (slot == null) continue;

			Control control = types[i].getControl();

			slot.state().setTargetProgress(
					control.getTargetProgress(tardis, slot.cooldown(console.getAge()), console));

			anim.apply(this.getPart(), slot.state(), console);
		}
	}

	/**
	 * The animation a reference resolves to, or null.
	 *
	 * <p>Keyed on the reference id rather than the control id, so a control mapped to a different
	 * animation per console or variant does not reuse the wrong one. Values are Optional so a miss is
	 * a cached empty rather than an absent key, which keeps this to one map lookup and stops a
	 * datapack naming a missing animation re-resolving every frame.
	 */
	private @Nullable BedrockAnimation animation(BedrockAnimationReference ref) {
		Optional<BedrockAnimation> cached = this.animationCache.get(ref.id());

		if (cached == null) {
			cached = ref.get();
			this.animationCache.put(ref.id(), cached);
		}

		return cached.orElse(null);
	}
}
