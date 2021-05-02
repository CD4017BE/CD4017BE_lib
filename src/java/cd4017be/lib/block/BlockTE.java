package cd4017be.lib.block;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import cd4017be.lib.container.IUnnamedContainerProvider;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntityType.Builder;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

/**Passes most events and actions to interfaces implemented by the TileEntity.
 * @param <T> the TileEntity used by this block
 * @author CD4017BE */
public class BlockTE<T extends TileEntity> extends Block {

	public TileEntityType<T> tileType;
	public final int handlerFlags;

	/**@param properties the block properties
	 * @param flags events passed to the TileEntity
	 * @see #flags(Class) */
	public BlockTE(Properties properties, int flags) {
		super(properties);
		this.handlerFlags = flags;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return hasTileEntity(state) ? tileType.create() : null;
	}

	/**@param factory the TileEntity factory function
	 * @return the TileEntityType created for this block */
	public TileEntityType<T> makeTEType(Function<TileEntityType<T>, T> factory) {
		tileType = Builder.of(() -> factory.apply(tileType), this).build(null);
		tileType.setRegistryName(getRegistryName());
		return tileType;
	}

	private final <I> void handleTE(
		BlockState state, int event, IWorldReader world, BlockPos pos,
		Class<I> type, Consumer<I> action
	) {
		if((handlerFlags & event) == 0 || !hasTileEntity(state)) return;
		TileEntity te = world.getBlockEntity(pos);
		if(type.isInstance(te))
			action.accept(type.cast(te));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(
		BlockState state, World world, BlockPos pos,
		BlockState newState, boolean isMoving
	) {
		handleTE(
			state, H_BREAK, world, pos, ITEBreak.class,
			te -> te.onBreak(newState, isMoving)
		);
		super.onRemove(state, world, pos, newState, isMoving);
	}

	@Override
	public void onNeighborChange(
		BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor
	) {
		handleTE(
			state, H_NEIGHBOR, world, pos, ITENeighborChange.class,
			te -> te.onNeighborTEChange(neighbor)
		);
	}

	@Override
	public void neighborChanged(
		BlockState state, World world, BlockPos pos,
		Block block, BlockPos fromPos, boolean isMoving
	) {
		handleTE(
			state, H_UPDATE, world, pos, ITEBlockUpdate.class,
			te -> te.onNeighborBlockChange(fromPos, block, isMoving)
		);
	}

	@Override
	public ActionResultType use(
		BlockState state, World world, BlockPos pos,
		PlayerEntity player, Hand hand, BlockRayTraceResult hit
	) {
		if (!hasTileEntity(state)) return ActionResultType.PASS;
		if((handlerFlags & H_INTERACT) != 0) {
			TileEntity te = world.getBlockEntity(pos);
			return te instanceof ITEInteract ? ((ITEInteract)te).onActivated(player, hand, hit)
				: ActionResultType.PASS;
		}
		INamedContainerProvider ncp = getMenuProvider(state, world, pos);
		if (ncp == null) return ActionResultType.PASS;
		if (!(player instanceof ServerPlayerEntity))
			return ActionResultType.SUCCESS;
		ServerPlayerEntity splayer = (ServerPlayerEntity)player;
		if (ncp instanceof ISpecialContainerProvider)
			NetworkHooks.openGui(splayer, ncp, (ISpecialContainerProvider)ncp);
		else NetworkHooks.openGui(splayer, ncp, pos);
		return ActionResultType.CONSUME;
	}

	@Override
	public void attack(
		BlockState state, World world, BlockPos pos, PlayerEntity player
	) {
		handleTE(
			state, H_INTERACT, world, pos, ITEInteract.class,
			te -> te.onClicked(player)
		);
	}

	@Override
	public void entityInside(BlockState state, World world, BlockPos pos, Entity entity) {
		handleTE(
			state, H_COLLIDE, world, pos, ITECollision.class,
			te -> te.onEntityCollided(entity)
		);
	}

	@Override
	public void onProjectileHit(
		World world, BlockState state, BlockRayTraceResult hit, ProjectileEntity projectile
	) {
		handleTE(
			state, H_COLLIDE, world, hit.getBlockPos(), ITECollision.class,
			te -> te.onProjectileCollided(projectile, hit)
		);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return (handlerFlags & H_REDSTONE) != 0 && hasTileEntity(state);
	}

	@Override
	public boolean shouldCheckWeakPower(
		BlockState state, IWorldReader world, BlockPos pos, Direction side
	) {
		return !isSignalSource(state) && super.shouldCheckWeakPower(state, world, pos, side);
	}

	@Override
	public int getSignal(
		BlockState state, IBlockReader world, BlockPos pos, Direction side
	) {
		if(!isSignalSource(state)) return 0;
		TileEntity te = world.getBlockEntity(pos);
		return te instanceof ITERedstone ? ((ITERedstone)te).redstoneSignal(side, false) : 0;
	}

	@Override
	public int getDirectSignal(
		BlockState state, IBlockReader world, BlockPos pos, Direction side
	) {
		if(!isSignalSource(state)) return 0;
		TileEntity te = world.getBlockEntity(pos);
		return te instanceof ITERedstone ? ((ITERedstone)te).redstoneSignal(side, true) : 0;
	}

	@Override
	public boolean canConnectRedstone(
		BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side
	) {
		if(!isSignalSource(state)) return false;
		if(side == null) return true;
		TileEntity te = world.getBlockEntity(pos);
		return te instanceof ITERedstone && ((ITERedstone)te).redstoneConnection(side);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return (handlerFlags & H_COMPARATOR) != 0 && hasTileEntity(state);
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos) {
		if(!hasAnalogOutputSignal(state)) return 0;
		TileEntity te = world.getBlockEntity(pos);
		return te instanceof ITEComparator ? ((ITEComparator)te).comparatorSignal() : 0;
	}

	@Override
	public void spawnAfterBreak(
		BlockState state, ServerWorld world, BlockPos pos, ItemStack stack
	) {
		handleTE(
			state, H_DROPS, world, pos, ITEDropItems.class,
			te -> te.spawnExtraDrops(stack)
		);
	}

	@Override
	public INamedContainerProvider getMenuProvider(BlockState state, World world, BlockPos pos) {
		if ((handlerFlags & H_GUI) == 0 || !hasTileEntity(state)) return null;
		TileEntity te = world.getBlockEntity(pos);
		return te instanceof INamedContainerProvider ? (INamedContainerProvider)te : null;
	}

	@Override
	public VoxelShape getShape(
		BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context
	) {
		if ((handlerFlags & H_SHAPE) != 0 && hasTileEntity(state)) {
			TileEntity te = world.getBlockEntity(pos);
			if (te instanceof ITEShape)
				return ((ITEShape)te).getShape(context);
		}
		return VoxelShapes.block();
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return VoxelShapes.block();
	}

	@Override
	public ItemStack getCloneItemStack(IBlockReader world, BlockPos pos, BlockState state) {
		ItemStack stack = new ItemStack(this);
		if ((handlerFlags & H_ITEMDATA) != 0 && hasTileEntity(state)) {
			TileEntity te = world.getBlockEntity(pos);
			if (te instanceof ITEPickItem)
				return ((ITEPickItem)te).getItem();
			else if (te != null)
				te.save(stack.getOrCreateTagElement(TE_TAG));
		}
		return stack;
	}

	@Override
	public void playerWillDestroy(
		World world, BlockPos pos, BlockState state, PlayerEntity player
	) {
		super.playerWillDestroy(world, pos, state, player);
		if (world.isClientSide || !player.isCreative()) return;
		if ((handlerFlags & H_ITEMDATA) == 0 || !hasTileEntity(state)) return;
		TileEntity te = world.getBlockEntity(pos);
		if (te == null) return;
		ItemStack stack;
		if (te instanceof ITEPickItem) {
			stack = ((ITEPickItem)te).getItem();
			if (!stack.hasTag()) return;
		} else {
			stack = new ItemStack(this);
			CompoundNBT nbt = te.save(stack.getOrCreateTagElement(TE_TAG));
			nbt.remove("id");
			nbt.remove("x");
			nbt.remove("y");
			nbt.remove("z");
		}
		ItemFluidUtil.dropStack(stack, world, pos);
	}

	public static final String TE_TAG = "BlockEntityTag";

	/** TileEntity handler flags */
	public static final int
	H_BREAK = 1, H_NEIGHBOR = 2, H_UPDATE = 4, H_INTERACT = 8,
	H_COLLIDE = 16, H_REDSTONE = 32, H_COMPARATOR = 64, H_DROPS = 128,
	H_GUI = 256, H_SHAPE = 512, H_ITEMDATA = 1024;

	/**@param c TileEntity class
	 * @return handler flags based on implemented interfaces */
	public static int flags(Class<?> c) {
		int f = 0;
		if(ITEBreak.class.isAssignableFrom(c)) f |= H_BREAK;
		if(ITENeighborChange.class.isAssignableFrom(c)) f |= H_NEIGHBOR;
		if(ITEBlockUpdate.class.isAssignableFrom(c)) f |= H_UPDATE;
		if(ITEInteract.class.isAssignableFrom(c)) f |= H_INTERACT;
		if(ITECollision.class.isAssignableFrom(c)) f |= H_COLLIDE;
		if(ITERedstone.class.isAssignableFrom(c)) f |= H_REDSTONE;
		if(ITEComparator.class.isAssignableFrom(c)) f |= H_COMPARATOR;
		if(ITEDropItems.class.isAssignableFrom(c)) f |= H_DROPS;
		if(INamedContainerProvider.class.isAssignableFrom(c)) f |= H_GUI;
		if(ITEShape.class.isAssignableFrom(c)) f |= H_SHAPE;
		if(ITEPickItem.class.isAssignableFrom(c)) f |= H_ITEMDATA;
		return f;
	}

	public interface ITEBreak {

		void onBreak(BlockState newState, boolean moving);
	}

	public interface ITENeighborChange {

		void onNeighborTEChange(BlockPos from);
	}

	public interface ITEBlockUpdate {

		void onNeighborBlockChange(BlockPos from, Block block, boolean moving);
	}

	public interface ITEInteract {

		ActionResultType onActivated(PlayerEntity player, Hand hand, BlockRayTraceResult hit);

		void onClicked(PlayerEntity player);
	}

	public interface ITECollision {

		/** when entity collides into this block (<b>doesn't work on fullCube
		 * blocks!</b>) */
		void onEntityCollided(Entity entity);

		/** when projectile collides into this block */
		default void
		onProjectileCollided(ProjectileEntity projectile, BlockRayTraceResult hit) {}
	}

	public interface ITERedstone {

		/** @param side face of neighbor block to emit in
		 * @param strong whether requesting strong signal
		 * @return emitted redstone signal */
		int redstoneSignal(Direction side, boolean strong);

		/** @param side face of neighbor block to emit in
		 * @return whether to connect */
		boolean redstoneConnection(@Nullable Direction side);
	}

	public interface ITEComparator {

		/** @return comparator signal */
		int comparatorSignal();
	}

	public interface ITEDropItems {

		void spawnExtraDrops(ItemStack tool);
	}

	public interface ISpecialContainerProvider extends Consumer<PacketBuffer>, IUnnamedContainerProvider {
		/**Write extra data to be send to client for container creation.
		 * @param extraData */
		@Override
		void accept(PacketBuffer extraData);
	}

	public interface ITEShape {
		/**@param context what the shape is used for
		 * @return block shape for collision, ray-trace and outline */
		VoxelShape getShape(ISelectionContext context);
	}

	public interface ITEPickItem {

		/**@return item for creative harvest and pick block. */
		ItemStack getItem();
	}

}
