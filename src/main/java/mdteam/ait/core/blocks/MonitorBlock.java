package mdteam.ait.core.blocks;

import mdteam.ait.api.ICantBreak;
import mdteam.ait.core.AITBlockEntityTypes;
import mdteam.ait.core.AITBlocks;
import mdteam.ait.core.AITDimensions;
import mdteam.ait.core.blockentities.ConsoleBlockEntity;
import mdteam.ait.core.blockentities.MonitorBlockEntity;
import mdteam.ait.core.blocks.types.HorizontalDirectionalBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static mdteam.ait.core.blocks.DoorBlock.rotateShape;

public class MonitorBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final int MAX_ROTATION_INDEX = RotationPropertyHelper.getMax();
    private static final int MAX_ROTATIONS = MAX_ROTATION_INDEX + 1;
    public static final IntProperty ROTATION = Properties.ROTATION;
    protected static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public MonitorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ROTATION, 0));
    }

    public VoxelShape eastShape() {
        VoxelShape shape = VoxelShapes.empty();
        shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.6875, 0.125, 0.125, 0.875, 0.875, 0.875), BooleanBiFunction.OR);
        shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.125, 0.25, 0.15625, 0.6875, 0.8125, 0.84375), BooleanBiFunction.OR);
        shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.625, 0.125, 0.15625, 0.6875, 0.25, 0.84375), BooleanBiFunction.OR);
        shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.125, 0.125, 0.1875, 0.625, 0.25, 0.8125), BooleanBiFunction.OR);
        shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.375, 0.0625, 0.375, 0.625, 0.125, 0.625), BooleanBiFunction.OR);
        shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.25, 0, 0.25, 0.75, 0.0625, 0.75), BooleanBiFunction.OR);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }


    @Override
    public boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MonitorBlockEntity(pos, state);
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockRenderView renderView, BlockPos pos, Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        return super.getAppearance(state, renderView, pos, side, sourceState, sourcePos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MonitorBlockEntity monitorBlockEntity)
            monitorBlockEntity.useOn(world, player.isSneaking(), player);

        return ActionResult.SUCCESS;
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(ROTATION, RotationPropertyHelper.fromYaw(ctx.getPlayerYaw()));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(ROTATION, rotation.rotate(state.get(ROTATION), MAX_ROTATIONS));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(ROTATION, mirror.mirror(state.get(ROTATION), MAX_ROTATIONS));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }
}
