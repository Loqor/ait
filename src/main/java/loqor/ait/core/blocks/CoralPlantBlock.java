package loqor.ait.core.blocks;

import loqor.ait.core.AITBlocks;
import loqor.ait.core.AITDimensions;
import loqor.ait.core.advancement.TardisCriterions;
import loqor.ait.core.blockentities.CoralBlockEntity;
import loqor.ait.core.blocks.types.HorizontalDirectionalBlock;
import loqor.ait.core.data.AbsoluteBlockPos;
import loqor.ait.core.managers.RiftChunkManager;
import loqor.ait.registry.impl.DesktopRegistry;
import loqor.ait.registry.impl.exterior.ExteriorVariantRegistry;
import loqor.ait.tardis.exterior.variant.growth.CoralGrowthVariant;
import loqor.ait.tardis.manager.TardisBuilder;
import loqor.ait.tardis.wrapper.server.ServerTardis;
import loqor.ait.tardis.wrapper.server.manager.ServerTardisManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class CoralPlantBlock extends HorizontalDirectionalBlock implements BlockEntityProvider {

	private final VoxelShape DEFAULT = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);

	public static final IntProperty AGE = Properties.AGE_7;;

	public CoralPlantBlock(Settings settings) {
		super(settings);
	}

	protected IntProperty getAgeProperty() {
		return AGE;
	}

	public int getMaxAge() {
		return 7;
	}

	public int getAge(BlockState state) {
		return state.get(this.getAgeProperty());
	}

	public BlockState withAge(int age) {
		return this.getDefaultState().with(this.getAgeProperty(), age);
	}

	public final boolean isMature(BlockState blockState) {
		return this.getAge(blockState) >= this.getMaxAge();
	}

	public boolean hasRandomTicks(BlockState state) {
		return true;
	}

	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBaseLightLevel(pos, 0) >= 9) {
			int i = this.getAge(state);
			if (i < this.getMaxAge()) {
				if (!(world.getBlockState(pos.down()).getBlock() instanceof SoulSandBlock)) {
					world.breakBlock(pos, true);
					return;
				}

				world.setBlockState(pos, this.withAge(i + 1), 2);
			}
		}

		if (this.getAge(state) >= this.getMaxAge()) {
			if (world.getRegistryKey() != AITDimensions.TARDIS_DIM_WORLD) {
				if(world.getBlockEntity(pos) instanceof CoralBlockEntity coral) {
					createTardis(world, pos, coral.creator);
				}
			} else {
				createConsole(world, pos);
			}
		}
	}

	private void createConsole(ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, AITBlocks.CONSOLE.getDefaultState());
	}

	private void createTardis(ServerWorld world, BlockPos pos, UUID creatorId) {
		ServerTardis created = ServerTardisManager.getInstance().create(new TardisBuilder()
				.at(new AbsoluteBlockPos.Directed(pos, world, 0))
				.exterior(ExteriorVariantRegistry.getInstance().get(CoralGrowthVariant.REFERENCE))
				.desktop(DesktopRegistry.DEFAULT_CAVE).owner(world.getPlayerByUuid(creatorId))
		);

		created.fuel().setCurrentFuel(0);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);

		if (!(placer instanceof ServerPlayerEntity player))
			return;

		if (!(world.getBlockState(pos.down()).getBlock() instanceof SoulSandBlock) ||
				(!RiftChunkManager.isRiftChunk(pos) &&
						!(world.getRegistryKey() == AITDimensions.TARDIS_DIM_WORLD))) {
			// GET IT OUTTA HERE!!!
			world.breakBlock(pos, placer.isPlayer() ? !player.isCreative() : true);
			return;
		}

		if (world.getBlockEntity(pos) instanceof CoralBlockEntity coral) {
			coral.creator = player.getUuid();
			coral.markDirty();
		}

		TardisCriterions.PLACE_CORAL.trigger(player);
	}

	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return (world.getBaseLightLevel(pos, 0) >= 8 || world.isSkyVisible(pos)) && super.canPlaceAt(state, world, pos);
	}

	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof RavagerEntity && world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			world.breakBlock(pos, true, entity);
		}
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return DEFAULT;
	}

	@Override
	public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		return DEFAULT;
	}

	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		return AITBlocks.CORAL_PLANT.asItem().getDefaultStack();
	}

	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE).add(FACING);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CoralBlockEntity(pos, state);
	}

	@Override
	public BlockState getAppearance(BlockState state, BlockRenderView renderView, BlockPos pos, Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
		return super.getAppearance(state, renderView, pos, side, sourceState, sourcePos);
	}
}