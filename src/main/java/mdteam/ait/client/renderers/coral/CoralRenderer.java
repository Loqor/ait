package mdteam.ait.client.renderers.coral;

import com.mojang.blaze3d.systems.RenderSystem;
import mdteam.ait.AITMod;
import mdteam.ait.client.models.coral.CoralGrowthModel;
import mdteam.ait.core.blockentities.CoralBlockEntity;
import mdteam.ait.core.blocks.CoralPlantBlock;
import mdteam.ait.core.blocks.ExteriorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CoralBlock;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class CoralRenderer<T extends CoralBlockEntity> implements BlockEntityRenderer<T> {

    public static final Identifier CORAL_GROWTH_TEXTURE = new Identifier(AITMod.MOD_ID, "textures/blockentities/coral/coral_growth.png");

    private final CoralGrowthModel coralModel;

    public CoralRenderer(BlockEntityRendererFactory.Context ctx) {
        this.coralModel = new CoralGrowthModel(CoralGrowthModel.getTexturedModelData().createModel());
    }
    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));
        BlockState blockState = entity.getCachedState();
        float f = blockState.get(CoralPlantBlock.FACING).asRotation();
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(f));
        ModelPart currentAgeModel = getCurrentAge(blockState.get(CoralPlantBlock.AGE), this.coralModel);;
        currentAgeModel.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(
                CORAL_GROWTH_TEXTURE, true)), light, overlay, 1f, 1f, 1f, 1);
        matrices.pop();
    }

    public ModelPart getCurrentAge(int age, CoralGrowthModel coralModel) {
        return switch (age) {
            case 1 -> coralModel.coral.getChild("two");
            case 2 -> coralModel.coral.getChild("three");
            case 3 -> coralModel.coral.getChild("four");
            case 4 -> coralModel.coral.getChild("five");
            case 5, 7, 6 -> coralModel.coral.getChild("six");
            default -> coralModel.coral.getChild("one");
        };
    }
}
