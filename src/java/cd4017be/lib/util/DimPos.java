package cd4017be.lib.util;

import java.lang.ref.WeakReference;

import com.google.common.base.MoreObjects;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Extended version of {@link BlockPos} with additional dimension (and optional world) information.
 * @author cd4017be
 */
public class DimPos extends BlockPos {

	private static final WeakReference<World> NO_WORLD = new WeakReference<>(null);

	/** the dimension id */
	public final int dimId;
	private WeakReference<World> world = NO_WORLD;
	private Side side;

	/**
	 * get the dimensional block position of the given Entity
	 * @param source an Entity
	 */
	public DimPos(Entity source) {
		super(source);
		this.dimId = source.dimension;
		World world = source.world;
		if (world != null) {
			this.world = new WeakReference<>(world);
			this.side = world.isRemote ? Side.CLIENT : Side.SERVER;
		}
	}

	/**
	 * get the dimensional block position of the given TileEntity
	 * @param source a TileEntity (must be added to a world)
	 */
	public DimPos(TileEntity source) {
		this(source.getPos(), source.getWorld());
	}

	/**
	 * @param source the block position
	 * @param world the dimension's world
	 */
	public DimPos(Vec3i source, World world) {
		this(source, world.provider.getDimension());
		this.world = new WeakReference<>(world);
		this.side = world.isRemote ? Side.CLIENT : Side.SERVER;
	}

	/**
	 * @param source the block position
	 * @param dim the dimension id
	 */
	public DimPos(Vec3i source, int dim) {
		this(source.getX(), source.getY(), source.getZ(), dim);
	}

	/**
	 * @param x block X-coord
	 * @param y block Y-coord
	 * @param z block Z-coord
	 * @param d dimension id
	 */
	public DimPos(int x, int y, int z, int d) {
		super(x, y, z);
		this.dimId = d;
	}

	/**
	 * @param x block X-coord
	 * @param y block Y-coord
	 * @param z block Z-coord
	 * @param world the dimension's world
	 */
	public DimPos(int x, int y, int z, WorldServer world) {
		this(x, y, z, world.provider.getDimension());
		this.world = new WeakReference<>(world);
		this.side = world.isRemote ? Side.CLIENT : Side.SERVER;
	}

	/**
	 * assigns a world instance for later use
	 * @param world the world instance for this dimension
	 */
	public DimPos assignWorld(World world) {
		if (world.provider.getDimension() != dimId)
			throw new IllegalArgumentException("given world represents a different dimension");
		if (side != null && (side == Side.CLIENT ^ world.isRemote))
			throw new IllegalArgumentException("given world represents a different logical side");
		this.world = new WeakReference<>(world);
		this.side = world.isRemote ? Side.CLIENT : Side.SERVER;
		return this;
	}

	/**
	 * @return the world associated with this position or null if not assigned
	 * @see #assignWorld(World)
	 */
	public World getWorld() {
		return world.get();
	}

	/**
	 * A special version of {@link #getWorld()} for <b> server side use only</b>, that automatically retrieves and assignes the WorldServer.
	 * @return the world server associated with this position or null if this dimension id is invalid or the world couldn't be loaded.
	 */
	public WorldServer getWorldServer() {
		World world = this.world.get();
		if (world instanceof WorldServer) return (WorldServer)world;
		WorldServer ws = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimId);
		if (side != Side.CLIENT) {
			this.world = new WeakReference<World>(ws);
			side = Side.SERVER;
		}
		return ws;
	}

	/**
	 * @return the logical side of this dimensional position or null if unknown
	 */
	public Side getSide() {
		return side;
	}

	@Override
	public DimPos add(int x, int y, int z) {
		if (x == 0 && y == 0 && z == 0) return this;
		DimPos pos = new DimPos(getX() + x, getY() + y, getZ() + z, dimId);
		pos.world = world;
		pos.side = side;
		return pos;
	}

	@Override
	public DimPos offset(EnumFacing facing, int n) {
		if (n == 0) return this;
		DimPos pos = new DimPos(getX() + facing.getFrontOffsetX() * n, getY() + facing.getFrontOffsetY() * n, getZ() + facing.getFrontOffsetZ() * n, dimId);
		pos.world = world;
		pos.side = side;
		return pos;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && !(o instanceof DimPos && ((DimPos)o).dimId != dimId);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + dimId;
	}

	@Override
	public int compareTo(Vec3i o) {
		if (o instanceof DimPos) {
			int dim = ((DimPos)o).dimId;
			if (dim != dimId)
				return dimId - dim;
		}
		return super.compareTo(o);
	}

	@Override
	public double distanceSq(Vec3i to) {
		return to instanceof DimPos && ((DimPos)to).dimId != dimId ? Double.POSITIVE_INFINITY : super.distanceSq(to);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).add("d", dimId).toString();
	}

	/**
	 * @return whether this position is loaded (returns false if no world assigned)
	 */
	public boolean isLoaded() {
		World world = this.world.get();
		return world != null && world.isBlockLoaded(this);
	}

	/**
	 * @return the BlockState at this position.
	 */
	public IBlockState getBlock() {
		World world = this.world.get();
		return world == null ? Blocks.AIR.getDefaultState() : world.getBlockState(this);
	}

	/**
	 * set the BlockState at this position
	 * @param state new block state
	 * @return whether the change was successful
	 */
	public boolean setBlock(IBlockState state) {
		World world = this.world.get();
		return world != null && world.setBlockState(this, state);
	}

	/**
	 * utility method to get a TileEntity without force-loading chunks.
	 * @return the TileEntity at this position or null if none there or chunk not loaded or world not assigned.
	 */
	public TileEntity getTileEntity() {
		World world = this.world.get();
		return world == null || !world.isBlockLoaded(this) ? null : world.getTileEntity(this);
	}

}
