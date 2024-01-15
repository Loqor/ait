package mdteam.ait.core.blocks;

import mdteam.ait.core.blockentities.DoorBlockEntity;
import mdteam.ait.core.blocks.types.HorizontalDirectionalBlock;
import mdteam.ait.core.AITBlockEntityTypes;
import mdteam.ait.tardis.util.TardisUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DoorBlock extends HorizontalDirectionalBlock implements BlockEntityProvider {

    public static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 11.1, 16.0, 32.0, 16.0);
    public static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 4.9, 32.0, 16.0);
    public static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 32.0, 4.9);
    public static final VoxelShape WEST_SHAPE = Block.createCuboidShape(11.1, 0.0, 0.0, 16.0, 32.0, 16.0);

    public DoorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (world.getBlockEntity(pos) instanceof DoorBlockEntity doorBlockEntity && (TardisUtil.isClient() ? doorBlockEntity.getClientTardis() : doorBlockEntity.getTardis()) == null &&
                (TardisUtil.isClient() ? doorBlockEntity.getClientTardis().isInSiegeMode() : doorBlockEntity.getTardis().isSiegeMode())) return VoxelShapes.empty();

        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> throw new RuntimeException("Invalid facing direction in " + this);
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (world.getBlockEntity(pos) instanceof DoorBlockEntity door) {
            door.useOn(world, player.isSneaking(), player);
        }

        return ActionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == AITBlockEntityTypes.DOOR_BLOCK_ENTITY_TYPE ? DoorBlockEntity::tick : null;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient())
            return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        //if(Objects.equals(this.getOutlineShape(state, world, pos, ShapeContext.of(entity)).getClosestPointTo(entity.getPos()), Optional.of(new Vec3d(0, 0, 0)))) {
        if (blockEntity instanceof DoorBlockEntity door)
            door.onEntityCollision(entity);
        //}
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DoorBlockEntity(pos, state);
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockRenderView renderView, BlockPos pos, Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        return super.getAppearance(state, renderView, pos, side, sourceState, sourcePos);
    }
}
