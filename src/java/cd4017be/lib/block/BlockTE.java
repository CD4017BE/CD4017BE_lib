package cd4017be.lib.block;

import java.util.function.Consumer;
import cd4017be.lib.container.IUnnamedContainerProvider;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.tileentity.BaseTileEntity.TickableClient;
import cd4017be.lib.tileentity.BaseTileEntity.TickableServer;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

/**Passes most events and actions to interfaces implemented by the BlockEntity.
 * @param <T> the BlockEntity used by this block
 * @author CD4017BE */
public class BlockTE<T extends BlockEntity> extends BaseEntityBlock {

	public BlockEntityType<T> tileType;
	public final int handlerFlags;

	/**@param properties the block properties
	 * @param flags events passed to the BlockEntity
	 * @see #flags(Class) */
	public BlockTE(Properties properties, int flags) {
		super(properties);
		this.handlerFlags = flags;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		BlockEntity te = tileType.create(pos, state);
		if (te instanceof BaseTileEntity)
			((BaseTileEntity)te).unloaded = false;
		return te;
	}

	@Override
	public <T1 extends BlockEntity> BlockEntityTicker<T1>
	getTicker(Level world, BlockState state, BlockEntityType<T1> type) {
		if ((handlerFlags & (world.isClientSide ? H_TICKCLIENT : H_TICKSERVER)) == 0) return null;
		return createTickerHelper(type, tileType, world.isClientSide
			? (w, p, s, t)-> ((TickableClient)t).tickClient(w, p, s)
			: (w, p, s, t)-> ((TickableServer)t).tickServer(w, p, s)
		);
	}

	@Override
	public <T1 extends BlockEntity> GameEventListener getListener(Level world, T1 te) {
		te.onLoad();
		return !world.isClientSide && te instanceof GameEventListener ? (GameEventListener)te : null;
	}

	@Override
	public RenderShape getRenderShape(BlockState p_49232_) {
		return (handlerFlags & H_NOMODEL) != 0 ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	/**@param factory the BlockEntity factory function
	 * @return the BlockEntityType created for this block */
	public BlockEntityType<T> makeTEType(TEFactory<T> factory) {
		return makeTEType(factory, this);
	}

	/**@param factory the BlockEntity factory function
	 * @param blocks the assigned blocks
	 * @return the BlockEntityType created for the given blocks */
	@SafeVarargs
	public static <T extends BlockEntity> BlockEntityType<T> makeTEType(
		TEFactory<T> factory, BlockTE<T>... blocks
	) {
		BlockTE<T> block = blocks[0];
		BlockEntityType<T> type = Builder.of((pos, state) -> factory.apply(block.tileType, pos, state), blocks).build(null);
		type.setRegistryName(block.getRegistryName());
		for (BlockTE<T> b : blocks) b.tileType = type;
		return type;
	}

	private final <I> void handleTE(
		BlockState state, int event, Level world, BlockPos pos,
		Class<I> type, Consumer<I> action
	) {
		if((handlerFlags & event) == 0) return;
		BlockEntity te = world.getBlockEntity(pos);
		if(type.isInstance(te))
			action.accept(type.cast(te));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(
		BlockState state, Level world, BlockPos pos,
		BlockState newState, boolean isMoving
	) {
		handleTE(
			state, H_BREAK, world, pos, ITEBreak.class,
			te -> te.onBreak(newState, isMoving)
		);
		super.onRemove(state, world, pos, newState, isMoving);
	}

	@Override
	public void neighborChanged(
		BlockState state, Level world, BlockPos pos,
		Block block, BlockPos fromPos, boolean isMoving
	) {
		handleTE(
			state, H_UPDATE, world, pos, ITEBlockUpdate.class,
			te -> te.onNeighborBlockChange(fromPos, block, isMoving)
		);
	}

	@Override
	public InteractionResult use(
		BlockState state, Level world, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hit
	) {
		if((handlerFlags & H_INTERACT) != 0) {
			BlockEntity te = world.getBlockEntity(pos);
			return te instanceof ITEInteract ? ((ITEInteract)te).onActivated(player, hand, hit)
				: InteractionResult.PASS;
		}
		MenuProvider ncp = getMenuProvider(state, world, pos);
		if (ncp == null) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer))
			return InteractionResult.SUCCESS;
		ServerPlayer splayer = (ServerPlayer)player;
		if (ncp instanceof ISpecialContainerProvider)
			NetworkHooks.openGui(splayer, ncp, (ISpecialContainerProvider)ncp);
		else NetworkHooks.openGui(splayer, ncp, pos);
		return InteractionResult.CONSUME;
	}

	@Override
	public void attack(
		BlockState state, Level world, BlockPos pos, Player player
	) {
		handleTE(
			state, H_INTERACT, world, pos, ITEInteract.class,
			te -> te.onClicked(player)
		);
	}

	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
		handleTE(
			state, H_COLLIDE, world, pos, ITECollision.class,
			te -> te.onEntityCollided(entity)
		);
	}

	@Override
	public void onProjectileHit(
		Level world, BlockState state, BlockHitResult hit, Projectile projectile
	) {
		handleTE(
			state, H_COLLIDE, world, hit.getBlockPos(), ITECollision.class,
			te -> te.onProjectileCollided(projectile, hit)
		);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return (handlerFlags & H_REDSTONE) != 0;
	}

	@Override
	public boolean shouldCheckWeakPower(
		BlockState state, LevelReader world, BlockPos pos, Direction side
	) {
		return !isSignalSource(state) && super.shouldCheckWeakPower(state, world, pos, side);
	}

	@Override
	public int getSignal(
		BlockState state, BlockGetter world, BlockPos pos, Direction side
	) {
		if(!isSignalSource(state)) return 0;
		BlockEntity te = world.getBlockEntity(pos);
		return te instanceof ITERedstone ? ((ITERedstone)te).redstoneSignal(side, false) : 0;
	}

	@Override
	public int getDirectSignal(
		BlockState state, BlockGetter world, BlockPos pos, Direction side
	) {
		if(!isSignalSource(state)) return 0;
		BlockEntity te = world.getBlockEntity(pos);
		return te instanceof ITERedstone ? ((ITERedstone)te).redstoneSignal(side, true) : 0;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return (handlerFlags & H_COMPARATOR) != 0;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		if(!hasAnalogOutputSignal(state)) return 0;
		BlockEntity te = world.getBlockEntity(pos);
		return te instanceof ITEComparator ? ((ITEComparator)te).comparatorSignal() : 0;
	}

	@Override
	public VoxelShape getShape(
		BlockState state, BlockGetter world, BlockPos pos, CollisionContext context
	) {
		if ((handlerFlags & H_SHAPE) != 0) {
			BlockEntity te = world.getBlockEntity(pos);
			if (te instanceof ITEShape)
				return ((ITEShape)te).getShape(context);
		}
		return Shapes.block();
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return Shapes.block();
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
		ItemStack stack = new ItemStack(this);
		if ((handlerFlags & H_ITEMDATA) != 0) {
			BlockEntity te = world.getBlockEntity(pos);
			if (te instanceof ITEPickItem)
				return ((ITEPickItem)te).getItem();
			else if (te != null)
				te.save(stack.getOrCreateTagElement(TE_TAG));
		}
		return stack;
	}

	@Override
	public void playerWillDestroy(
		Level world, BlockPos pos, BlockState state, Player player
	) {
		super.playerWillDestroy(world, pos, state, player);
		if (world.isClientSide || !player.isCreative()) return;
		if ((handlerFlags & H_ITEMDATA) == 0) return;
		BlockEntity te = world.getBlockEntity(pos);
		if (te == null) return;
		ItemStack stack;
		if (te instanceof ITEPickItem) {
			stack = ((ITEPickItem)te).getItem();
			if (!stack.hasTag()) return;
		} else {
			stack = new ItemStack(this);
			CompoundTag nbt = te.save(stack.getOrCreateTagElement(TE_TAG));
			nbt.remove("id");
			nbt.remove("x");
			nbt.remove("y");
			nbt.remove("z");
		}
		ItemFluidUtil.dropStack(stack, world, pos);
	}

	@Override
	public void setPlacedBy(
		Level world, BlockPos pos, BlockState state,
		LivingEntity entity, ItemStack stack
	) {
		handleTE(state, H_PLACE, world, pos, ITEPlace.class,
			te -> te.onPlace(state, stack, entity));
	}

	public static final String TE_TAG = "BlockEntityTag";

	/** BlockEntity handler flags */
	public static final int
	H_BREAK = 1, H_NOMODEL = 2, H_UPDATE = 4, H_INTERACT = 8,
	H_COLLIDE = 16, H_REDSTONE = 32, H_COMPARATOR = 64, H_DROPS = 128,
	H_GUI = 256, H_SHAPE = 512, H_ITEMDATA = 1024, H_PLACE = 2048,
	H_TICKCLIENT = 4096, H_TICKSERVER = 8192;

	/**@param c BlockEntity class
	 * @return handler flags based on implemented interfaces */
	public static int flags(Class<?> c) {
		int f = 0;
		if(ITEBreak.class.isAssignableFrom(c)) f |= H_BREAK;
		if(ITEBlockUpdate.class.isAssignableFrom(c)) f |= H_UPDATE;
		if(ITEInteract.class.isAssignableFrom(c)) f |= H_INTERACT;
		if(ITECollision.class.isAssignableFrom(c)) f |= H_COLLIDE;
		if(ITERedstone.class.isAssignableFrom(c)) f |= H_REDSTONE;
		if(ITEComparator.class.isAssignableFrom(c)) f |= H_COMPARATOR;
		if(MenuProvider.class.isAssignableFrom(c)) f |= H_GUI;
		if(ITEShape.class.isAssignableFrom(c)) f |= H_SHAPE;
		if(ITEPickItem.class.isAssignableFrom(c)) f |= H_ITEMDATA;
		if(ITEPlace.class.isAssignableFrom(c)) f |= H_PLACE;
		if(TickableClient.class.isAssignableFrom(c)) f |= H_TICKCLIENT;
		if(TickableServer.class.isAssignableFrom(c)) f |= H_TICKSERVER;
		return f;
	}

	public interface ITEBreak {

		void onBreak(BlockState newState, boolean moving);
	}

	public interface ITEBlockUpdate {

		void onNeighborBlockChange(BlockPos from, Block block, boolean moving);
	}

	public interface ITEInteract {

		InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit);

		void onClicked(Player player);
	}

	public interface ITECollision {

		/** when entity collides into this block (<b>doesn't work on fullCube
		 * blocks!</b>) */
		void onEntityCollided(Entity entity);

		/** when projectile collides into this block */
		default void
		onProjectileCollided(Projectile projectile, BlockHitResult hit) {}
	}

	public interface ITERedstone {

		/** @param side face of neighbor block to emit in
		 * @param strong whether requesting strong signal
		 * @return emitted redstone signal */
		int redstoneSignal(Direction side, boolean strong);
	}

	public interface ITEComparator {

		/** @return comparator signal */
		int comparatorSignal();
	}

	public interface ISpecialContainerProvider extends Consumer<FriendlyByteBuf>, IUnnamedContainerProvider {
		/**Write extra data to be send to client for container creation.
		 * @param extraData */
		@Override
		void accept(FriendlyByteBuf extraData);
	}

	public interface ITEShape {
		/**@param context what the shape is used for
		 * @return block shape for collision, ray-trace and outline */
		VoxelShape getShape(CollisionContext context);
	}

	public interface ITEPickItem {

		/**@return item for creative harvest and pick block. */
		ItemStack getItem();
	}

	public interface ITEPlace {

		void onPlace(BlockState state, ItemStack stack, LivingEntity entity);
	}

	@FunctionalInterface
	public interface TEFactory<T extends BlockEntity> {

		T apply(BlockEntityType<T> type, BlockPos pos, BlockState state);
	}

}
