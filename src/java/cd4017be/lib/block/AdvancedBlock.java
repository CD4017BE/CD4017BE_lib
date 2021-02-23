package cd4017be.lib.block;

import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import cd4017be.lib.block.MultipartBlock.IModularTile;
import net.minecraft.block.*;
import net.minecraft.state.Property;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

/**
 * 
 * @author CD4017BE
 */
public class AdvancedBlock<T extends TileEntity> extends Block {

	private final Supplier<T> teFactory;
	protected BlockRenderType renderType = BlockRenderType.MODEL;
	/**1:NeighborAware, 2:BreakCleanup, 4:Interactive, 8:PlaceHarvest, 0x10:Redstone, 0x20:Collision, 0x40:hasGui, 0x80:Comparator, 0x100:Multipart, 0x10000:nonOpaque, 0x40000:no sneak place */
	protected int flags;

	public static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(.5, .5, .5, .5, .5, .5);
	protected static final Set<Property<?>> L_PROPERTIES = new HashSet<>();

	/**
	 * 
	 * @param id registry name
	 * @param m material
	 * @param flags 2 = nonOpaque, 1 = noFullBlock, 4 = don't open GUI
	 * @param tile optional TileEntity to register with this block
	 */
	@SuppressWarnings("unchecked")
	public AdvancedBlock(String id, AbstractBlock.Properties p, int flags, Class<T> teType) {
		super(p);
		if (teType != null) {
			try {
				this.teFactory = asInterfaceInstance(Supplier.class, lookup().findConstructor(teType, methodType(void.class)));
			} catch(NoSuchMethodException | IllegalAccessException e) {
				throw new IllegalArgumentException("Can't create TileEntity factory from constructor!", e);
			}
			if (INeighborAwareTile.class.isAssignableFrom(teType)) this.flags |= 1;
			if (ISelfAwareTile.class.isAssignableFrom(teType)) this.flags |= 2;
			if (IInteractiveTile.class.isAssignableFrom(teType)) this.flags |= 4;
			if (ITilePlaceHarvest.class.isAssignableFrom(teType)) this.flags |= 8;
			if (IRedstoneTile.class.isAssignableFrom(teType)) this.flags |= 16;
			if (ITileCollision.class.isAssignableFrom(teType)) this.flags |= 32;
			if ((flags & 4) == 0 && INamedContainerProvider.class.isAssignableFrom(teType)) this.flags |= 64;
			if (IComparatorSource.class.isAssignableFrom(teType)) this.flags |= 128;
			if (IModularTile.class.isAssignableFrom(teType)) this.flags |= 256;
		} else this.teFactory = null;
		setRegistryName(id);
	}

	public TileEntityType<T> createTileEntityType() {
		TileEntityType<T> type = TileEntityType.Builder.create(teFactory, this).build(null);
		type.setRegistryName(getRegistryName());
		return type;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return teFactory != null;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return teFactory != null ? teFactory.get() : null;
	}

	public interface INeighborAwareTile {
		/**
		 * when neighboring block state changes
		 * @param b event source Block-type
		 * @param src event source position
		 */
		void neighborBlockChange(Block b, BlockPos src);
		/**
		 * when neighboring tileEntity added/removed by chunk load/unload
		 * @param side on which the TileEntity changed
		 */
		void neighborTileChange(Direction side, boolean unload);
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		if ((flags & 1) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof INeighborAwareTile) ((INeighborAwareTile)te).neighborBlockChange(block, fromPos);
	}

	public interface ISelfAwareTile {
		/**
		 * when this block is about to be removed/changed
		 */
		boolean breakBlock(BlockState oldState, BlockState newState);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if ((flags & 2) == 0) {
			super.onReplaced(state, world, pos, newState, isMoving);
			return;
		}
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ISelfAwareTile && ((ISelfAwareTile)te).breakBlock(state, newState))
			world.removeTileEntity(pos);
	}

	public interface IInteractiveTile {
		/**
		 * when right-clicked by player
		 * @param player event source player
		 * @param hand event source hand
		 * @param item held item
		 * @param s clicked block face
		 * @param X block relative x hit pos
		 * @param Y block relative y hit pos
		 * @param Z block relative z hit pos
		 * @return consume event
		 */
		ActionResultType onActivated(PlayerEntity player, Hand hand, ItemStack item, BlockRayTraceResult hit);
		/**
		 * when left-clicked (hit) by player
		 * @param player event source player
		 */
		void onClicked(PlayerEntity player);
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		if ((flags & 4) != 0) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IInteractiveTile)
				return ((IInteractiveTile)te).onActivated(player, hand, player.getHeldItem(hand), hit);
		}
		if ((flags & 64) != 0) {
			if (world.isRemote) return ActionResultType.SUCCESS;
			player.openContainer(getContainer(state, world, pos));
			return ActionResultType.CONSUME;
		}
		return ActionResultType.PASS;
	}

	@Override
	public void onBlockClicked(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if ((flags & 4) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IInteractiveTile) ((IInteractiveTile)te).onClicked(player);
	}

	public interface ITilePlaceHarvest {
		/**
		 * when placed via item
		 * @param entity event source entity
		 * @param item held item
		 */
		void onPlaced(LivingEntity entity, ItemStack item);
		/**
		 * ask for theoretically dropped items
		 * @param state state of this block
		 * @param fortune fortune modifier
		 * @return drop list
		 */
		List<ItemStack> dropItem(BlockState state, int fortune);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack item) {
		if ((flags & 8) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ITilePlaceHarvest) ((ITilePlaceHarvest)te).onPlaced(entity, item);
	}

	@Override
	public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, TileEntity te, ItemStack stack) {
		super.harvestBlock(world, player, pos, state, te, stack);
		world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
	}
/*
	@Override
	public void onPlayerDestroy(IWorld world, BlockPos pos, BlockState state) {
		if ((flags & 8) != 0 && !world.isRemote() && !world.restoringBlockSnapshots) {
			Item item = getItemDropped(state, RANDOM, 0);
			int dmg = damageDropped(state);
			for (ItemStack stack : getDrops(world, pos, state, 0))
				if (stack.getItem() != item || stack.getItemDamage() != dmg || stack.hasTag())
					spawnAsEntity(world, pos, stack);
		}
		return world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, net.minecraft.loot.LootContext.Builder builder) {
		// TODO Auto-generated method stub
		return super.getDrops(state, builder);
	}

	@Override
	public List<ItemStack> getDrops(IWorldReader world, BlockPos pos, BlockState state, int fortune) {
		if ((flags & 8) == 0) return super.getDrops(world, pos, state, fortune);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ITilePlaceHarvest) return ((ITilePlaceHarvest)te).dropItem(state, fortune);
		else return super.getDrops(world, pos, state, fortune);
	}
*/
	public interface IRedstoneTile {
		/**
		 * ask for emitted redstone signal
		 * @param side face of neighbor block to emit in
		 * @param strong whether asked for strong signal
		 * @return redstone signal
		 */
		int redstoneLevel(Direction side, boolean strong);
		/**
		 * check for redstone connection
		 * @param side face of neighbor block to emit in
		 * @return whether to connect
		 */
		boolean connectRedstone(Direction side);
	}

	@Override
	public boolean canProvidePower(BlockState state) {
		return (flags & 16) != 0;
	}

	@Override
	public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side) {
		return (flags & 16) == 0;
	}

	@Override
	public int getWeakPower(BlockState blockState, IBlockReader world, BlockPos pos, Direction side) {
		if ((flags & 16) == 0) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IRedstoneTile) return ((IRedstoneTile)te).redstoneLevel(side.getOpposite(), false);
		else return 0;
	}

	@Override
	public int getStrongPower(BlockState state, IBlockReader world, BlockPos pos, Direction s) {
		if ((flags & 16) == 0) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IRedstoneTile) return ((IRedstoneTile)te).redstoneLevel(s.getOpposite(), true);
		else return 0;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side) {
		if ((flags & 16) == 0) return false;
		if (side == null) return true;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IRedstoneTile) return ((IRedstoneTile)te).connectRedstone(side.getOpposite());
		return false;
	}

	public interface IComparatorSource {
		/**
		 * ask for comparator value
		 * @return comparator signal
		 */
		int comparatorValue();
	}

	@Override
	public boolean hasComparatorInputOverride(BlockState state) {
		return (flags & 128) != 0;
	}

	@Override
	public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
		if ((flags & 128) == 0) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IComparatorSource) return ((IComparatorSource)te).comparatorValue();
		return 0;
	}

	public interface ITileCollision {
		/**
		 * when entity collides into this block (<b>doesn't work on fullCube blocks!</b>)
		 * @param entity collided entity
		 */
		void onEntityCollided(Entity entity);

		void onProjectileCollided(ProjectileEntity projectile, BlockRayTraceResult hit);
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if ((flags & 32) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ITileCollision) ((ITileCollision)te).onEntityCollided(entity);
	}

	@Override
	public void onProjectileCollision(World world, BlockState state, BlockRayTraceResult hit, ProjectileEntity projectile) {
		if ((flags & 32) == 0) return;
		TileEntity te = world.getTileEntity(hit.getPos());
		if (te instanceof ITileCollision) ((ITileCollision)te).onProjectileCollided(projectile, hit);
	}

	@Override
	public INamedContainerProvider getContainer(BlockState state, World world, BlockPos pos) {
		if ((flags & 64) == 0) return null;
		TileEntity te = world.getTileEntity(pos);
		return te instanceof INamedContainerProvider ? (INamedContainerProvider)te : null;
	}

	public void setRenderType(BlockRenderType t) {
		renderType = t;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return renderType;
	}

/*
	@Override
	public boolean isOpaqueCube(BlockState state) {
		return (flags & 65536) == 0;
	}

	@Override
	public boolean isFullCube(BlockState state) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isNormalCube(BlockState state) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isNormalCube(BlockState state, IWorldReader world, BlockPos pos) {
		return getBoundingBox(state, world, pos) == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isDistSolid(BlockState state, IWorldReader world, BlockPos pos, Direction side) {
		AxisAlignedBB box = getBoundingBox(state, world, pos);
		if (box == FULL_BLOCK_AABB) return true;
		return getFace(box, side) == (side.getAxisDirection() == AxisDirection.POSITIVE ? 1.0 : 0.0);
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IWorldReader world, BlockState state, BlockPos pos, Direction side) {
		AxisAlignedBB box = getBBWithoutCover(state, world, pos);
		if (box == FULL_BLOCK_AABB) return BlockFaceShape.SOLID;
		BlockState cover = getCover(world, pos);
		if (cover != null) {
			BlockFaceShape shape = cover.getBlockFaceShape(world, pos, side);
			if (shape != BlockFaceShape.UNDEFINED) return shape;
		}
		if (box == NULL_AABB) return BlockFaceShape.UNDEFINED;
		double radA = 1.0, radB = 1.0;
		int o = 3 - (side.ordinal() >> 1);
		for (Direction f : Direction.values()) {
			if (f == side.getOpposite()) continue;
			double d = getFace(box, f) / 2.0 * (double)f.getAxisDirection().getOffset();
			if (d < 0.0625 || (f == side && d < 0.5)) return BlockFaceShape.UNDEFINED;
			if (((f.ordinal() >> 1) + o) % 3 == 1) {
				if (d < radA) radA = d;
			} else if (d < radB) radB = d;
		}
		if (radB > radA) {double r = radB; radB = radA; radA = r;}
		return radA < 0.5 ? (radB >= 0.25 ? BlockFaceShape.CENTER_BIG : radB >= 0.125 ? BlockFaceShape.CENTER : BlockFaceShape.CENTER_SMALL)
			: radB < 0.5 ? (radB >= 0.25 ? BlockFaceShape.MIDDLE_POLE_THICK : radB >= 0.125 ? BlockFaceShape.MIDDLE_POLE : BlockFaceShape.MIDDLE_POLE_THIN)
			: BlockFaceShape.SOLID;
	}

	private static double getFace(AxisAlignedBB box, Direction side) {
		switch (side) {
		case DOWN: return box.minY;
		case UP: return box.maxY;
		case NORTH: return box.minZ;
		case SOUTH: return box.maxZ;
		case WEST: return box.minX;
		case EAST: return box.maxX;
		default: return Double.NaN;
		}
	}

	@Override
	public boolean isTopSolid(BlockState state) {
		return isNormalCube(state);
	}

	@Override
	public boolean canBeReplacedByLeaves(BlockState state, IWorldReader world, BlockPos pos) {
		return false;
	}

	private BlockRenderLayer blockLayer = BlockRenderLayer.SOLID;

	public void setBlockLayer(BlockRenderLayer layer) {
		this.blockLayer = layer;
	}

	public BlockRenderLayer getBlockLayer() {
		return this.blockLayer;
	}

	@Override
	public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
		return blockLayer == null || layer == blockLayer;
	}

	public AdvancedBlock setBlockBounds(AxisAlignedBB box) {
		boundingBox[0] = box;
		return this;
	}

	protected AxisAlignedBB getMainBB(BlockState state, IWorldReader world, BlockPos pos) {
		return boundingBox[0];
	}

	protected AxisAlignedBB getBBWithoutCover(BlockState state, IWorldReader world, BlockPos pos) {
		AxisAlignedBB box = getMainBB(state, world, pos);
		if (box == FULL_BLOCK_AABB || (flags & 0x100) == 0) return box;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IModularTile) {
			IModularTile tile = (IModularTile) te;
			for (int i = 1; i < boundingBox.length; i++) {
				AxisAlignedBB box1 = boundingBox[i];
				if (box1 != NULL_AABB && tile.isModulePresent(i - 1)) {
					if (box1 == FULL_BLOCK_AABB) return box1;
					box = box == NULL_AABB ? box1 : box.union(box1);
				}
			}
		}
		return box;
	}

	@Override
	public AxisAlignedBB getBoundingBox(BlockState state, IWorldReader world, BlockPos pos) {
		AxisAlignedBB box = getBBWithoutCover(state, world, pos);
		if (box == FULL_BLOCK_AABB) return box;
		BlockState cover = getCover(world, pos);
		if (cover != null) {
			AxisAlignedBB box1 = cover.getBoundingBox(world, pos);
			if (box1 == FULL_BLOCK_AABB) return box1;
			box = box == NULL_AABB ? box1 : box.union(box1);
		}
		return box == NULL_AABB ? EMPTY_AABB : box;
	}

	@Override
	public void addCollisionBoxToList(BlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> list, Entity entity, boolean b) {
		if ((flags & 0x100) == 0) {
			super.addCollisionBoxToList(state, world, pos, entityBox, list, entity, b);
			return;
		}
		AxisAlignedBB box = getMainBB(state, world, pos);
		addCollisionBoxToList(pos, entityBox, list, box);
		if (box == FULL_BLOCK_AABB) return;
		BlockState cover = getCover(world, pos);
		if (cover != null) cover.addCollisionBoxToList(world, pos, entityBox, list, entity, false);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IModularTile) {
			IModularTile tile = (IModularTile) te;
			for (int i = 1; i < boundingBox.length; i++) {
				box = boundingBox[i];
				if (box != NULL_AABB && tile.isModulePresent(i - 1)) {
					addCollisionBoxToList(pos, entityBox, list, box);
					if (box == FULL_BLOCK_AABB) return;
				}
			}
		}
	}

	@Override
	public RayTraceResult collisionRayTrace(BlockState state, World world, BlockPos pos, Vector3d start, Vector3d end) {
		if ((flags & 0x100) == 0) return super.collisionRayTrace(state, world, pos, start, end);
		Vector3d start1 = start.subtract(pos.getX(), pos.getY(), pos.getZ());
		Vector3d end1 = end.subtract(pos.getX(), pos.getY(), pos.getZ());
		int p = 0;
		RayTraceResult collision = null;
		AxisAlignedBB box = getMainBB(state, world, pos);
		if (box != NULL_AABB) {
			collision = box.calculateIntercept(start1, end1);
			if (collision != null) end1 = collision.hitVec;
		}
		BlockState cover;
		if (box != FULL_BLOCK_AABB) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IModularTile) {
				IModularTile tile = (IModularTile) te;
				for (int i = 1; i < boundingBox.length; i++) {
					box = boundingBox[i];
					if (box != NULL_AABB && tile.isModulePresent(i - 1)) {
						RayTraceResult collision1 = box.calculateIntercept(start1, end1);
						if (collision1 != null) {
							collision = collision1;
							end1 = collision.hitVec;
							p = i;
						}
						if (box == FULL_BLOCK_AABB) break;
					}
				}
			}
			cover = getCover(world, pos);
		} else cover = null;
		if (collision != null) {
			end = end1.addVector(pos.getX(), pos.getY(), pos.getZ());
			collision = new RayTraceResult(end, collision.sideHit, pos);
			collision.subHit = p;
		}
		if (cover != null) {
			RayTraceResult rtr = cover.collisionRayTrace(world, pos, start, end);
			if (rtr != null) {
				rtr.subHit = -2;
				return rtr;
			}
		}
		return collision; 
	}

	public interface ICoverableTile extends IModularTile, IInteractiveTile {

		Cover getCover();

		@Override
		default boolean onActivated(PlayerEntity player, Hand hand, ItemStack item, Direction s, float X, float Y, float Z) {
			return getCover().interact((BaseTileEntity)this, player, hand, item, s, X, Y, Z);
		}

		@Override
		default void onClicked(PlayerEntity player) {
			getCover().hit((BaseTileEntity)this, player);
		}

		@Override
		default <T> T getModuleState(int m) {
			return getCover().module();
		}

		@Override
		default boolean isModulePresent(int m) {
			return false;
		}

		@Override
		default boolean isOpaque() {
			return getCover().opaque;
		}

	}

	protected BlockState getCover(IWorldReader world, BlockPos pos) {
		if ((flags & 0x100) == 0) return null;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ICoverableTile && te.getBlockState().getBlock() == this)
			return ((ICoverableTile)te).getCover().state;
		return null;
	}

	@Override
	public float getBlockHardness(BlockState state, World world, BlockPos pos) {
		BlockState cover = getCover(world, pos);
		if (cover == null) return blockHardness;
		float h = cover.getBlockHardness(world, pos);
		return h < 0 ? h : h + blockHardness;
	}

	@Override
	public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
		float r = blockResistance / 5.0F;
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			r += cover.getBlock().getExplosionResistance(world, pos, exploder, explosion);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return r;
	}

	@Override
	public boolean canEntityDestroy(BlockState state, IWorldReader world, BlockPos pos, Entity entity) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().canEntityDestroy(cover, world, pos, entity);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int getLightValue(BlockState state, IWorldReader world, BlockPos pos) {
		BlockState cover = getCover(world, pos);
		if (cover == null) return lightValue;
		//using getLightValue(World, BlockPos) would end in infinite recursion because ... Minecraft.
		return Math.max(lightValue, cover.getLightValue());
	}

	@Override
	public int getLightOpacity(BlockState state, IWorldReader world, BlockPos pos) {
		BlockState cover = getCover(world, pos);
		if (cover == null) return lightOpacity;
		return Math.max(lightOpacity, cover.getLightOpacity(world, pos));
	}

	@Override
	public boolean doesDistBlockRendering(BlockState state, IWorldReader world, BlockPos pos, Direction face) {
		if (super.doesDistBlockRendering(state, world, pos, face)) return true;
		BlockState cover = getCover(world, pos);
		return cover != null && cover.doesDistBlockRendering(world, pos, face);
	}

	@Override
	public boolean canBeConnectedTo(IWorldReader world, BlockPos pos, Direction facing) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().canBeConnectedTo(world, pos, facing);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return false;
	}

	@Override
	public SoundType getSoundType(BlockState state, World world, BlockPos pos, Entity entity) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().getSoundType(cover, world, pos, entity);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return blockSoundType;
	}

	@Override
	public boolean canSustainLeaves(BlockState state, IWorldReader world, BlockPos pos) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().canSustainLeaves(cover, world, pos);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return false;
	}

	@Override
	public boolean canSustainPlant(BlockState state, IWorldReader world, BlockPos pos, Direction direction, IPlantable plantable) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().canSustainPlant(cover, world, pos, direction, plantable);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return false;
	}

	@Override
	public boolean isBurning(IWorldReader world, BlockPos pos) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().isBurning(world, pos);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return false;
	}

	@Override
	public boolean isFireSource(World world, BlockPos pos, Direction side) {
		BlockState cover = getCover(world, pos);
		if (cover != null) try {
			return cover.getBlock().isFireSource(world, pos, side);
		} catch(IllegalArgumentException e) {} //block trying to get invalid properties from wrong BlockState
		return false;
	}
*/
}
