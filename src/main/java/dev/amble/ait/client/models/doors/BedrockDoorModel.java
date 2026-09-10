package dev.amble.ait.client.models.doors;

import dev.amble.ait.client.models.AnimatedModel;
import dev.amble.ait.client.tardis.ClientTardis;
import dev.amble.ait.core.blockentities.DoorBlockEntity;
import dev.amble.ait.data.schema.door.AnimatedDoor;
import dev.amble.ait.data.schema.door.DoorSchema;
import dev.amble.lib.client.bedrock.BedrockModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class BedrockDoorModel implements AnimatedModel<DoorBlockEntity> {
    private final ModelPart root;

    public BedrockDoorModel(BedrockModel model) {
        if (model == null)
            throw new IllegalStateException("Bedrock Door Model is null. Ensure the resource pack is loaded correctly.");
        this.root = model.create().createModel();
    }

    @Override
    public void renderWithAnimations(ClientTardis tardis, DoorBlockEntity be, ModelPart root, MatrixStack matrices,
                                     VertexConsumer vertices, int light, int overlay, float red, float green,
                                     float blue, float pAlpha, float tickDelta) {
        matrices.push();

        DoorSchema schema = tardis.getExterior().getVariant().door();

        if (schema instanceof AnimatedDoor animDoor) {
            this.getPart().traverse().forEach(ModelPart::resetTransform);
            animDoor.runAnimations(root, matrices, tickDelta, tardis);
        }

        root.render(matrices, vertices, light, overlay, red, green, blue, pAlpha);
        matrices.pop();
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}

