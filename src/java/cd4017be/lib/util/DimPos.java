package cd4017be.lib.util;

import java.lang.ref.WeakReference;

import com.google.common.base.MoreObjects;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * Extended version of {@link BlockPos} with additional dimension (and optional world) information.
 * @author cd4017be
 */
public class DimPos extends BlockPos {

	private static final WeakReference<World> NO_WORLD = new WeakReference<>(null);

	public final RegistryKey<World> dim;
	private WeakReference<World> world = NO_WORLD;

	/**
	 * get the dimensional block position of the given Entity
	 * @param source an Entity
	 */
	public DimPos(Entity source) {
		this(source.getPosition(), source.world);
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
	public DimPos(Vector3i source, World world) {
		this(source, world.getDimensionKey());
		this.world = new WeakReference<>(world);
	}

	/**
	 * @param source the block position
	 * @param dim the dimension id
	 */
	public DimPos(Vector3i source, RegistryKey<World> dim) {
		this(source.getX(), source.getY(), source.getZ(), dim);
	}

	/**
	 * @param x block X-coord
	 * @param y block Y-coord
	 * @param z block Z-coord
	 * @param d dimension id
	 */
	public DimPos(int x, int y, int z, RegistryKey<World> d) {
		super(x, y, z);
		this.dim = d;
	}

	/**
	 * @param x block X-coord
	 * @param y block Y-coord
	 * @param z block Z-coord
	 * @param world the dimension's world
	 */
	public DimPos(int x, int y, int z, ServerWorld world) {
		this(x, y, z, world.getDimensionKey());
		this.world = new WeakReference<>(world);
	}

	/**@param nbt data */
	public DimPos(CompoundNBT nbt) {
		this(
			nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"),
			RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(nbt.getString("d")))
		);
	}

	/**
	 * assigns a world instance for later use
	 * @param world the world instance for this dimension
	 */
	public DimPos assignWorld(World world) {
		if (world.getDimensionKey() != dim)
			throw new IllegalArgumentException("given world represents a different dimension");
		this.world = new WeakReference<>(world);
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
	 * A special version of {@link #getWorld()} for <b> server side use only</b>, that automatically retrieves and assignes the ServerWorld.
	 * @return the world server associated with this position or null if this dimension id is invalid or the world couldn't be loaded.
	 */
	public ServerWorld getServerWorld(World ref) {
		World world = this.world.get();
		if (world instanceof ServerWorld) return (ServerWorld)world;
		ServerWorld ws = ref.getServer().getWorld(dim);
		if (ws != null) this.world = new WeakReference<World>(ws);
		return ws;
	}

	@Override
	public DimPos add(int x, int y, int z) {
		if (x == 0 && y == 0 && z == 0) return this;
		DimPos pos = new DimPos(getX() + x, getY() + y, getZ() + z, dim);
		pos.world = world;
		return pos;
	}

	@Override
	public DimPos offset(Direction facing, int n) {
		if (n == 0) return this;
		DimPos pos = new DimPos(getX() + facing.getXOffset() * n, getY() + facing.getYOffset() * n, getZ() + facing.getZOffset() * n, dim);
		pos.world = world;
		return pos;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && !(o instanceof DimPos && ((DimPos)o).dim != dim);
	}

	@Override
	public int compareTo(Vector3i o) {
		if (o instanceof DimPos) {
			RegistryKey<World> dim = ((DimPos)o).dim;
			if (dim != this.dim)
				return this.dim.getRegistryName().compareTo(dim.getRegistryName());
		}
		return super.compareTo(o);
	}

	@Override
	public double distanceSq(Vector3i to) {
		return to instanceof DimPos && ((DimPos)to).dim != dim ? Double.POSITIVE_INFINITY : super.distanceSq(to);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).add("d", dim).toString();
	}

	/**
	 * @return whether this position is loaded (returns false if no world assigned)
	 */
	public boolean isLoaded() {
		World world = this.world.get();
		return world != null && world.isBlockPresent(this);
	}

	/**
	 * @return the BlockState at this position.
	 */
	public BlockState getBlock() {
		World world = this.world.get();
		return world == null ? Blocks.AIR.getDefaultState() : world.getBlockState(this);
	}

	/**
	 * set the BlockState at this position
	 * @param state new block state
	 * @return whether the change was successful
	 */
	public boolean setBlock(BlockState state) {
		World world = this.world.get();
		return world != null && world.setBlockState(this, state);
	}

	/**
	 * utility method to get a TileEntity without force-loading chunks.
	 * @return the TileEntity at this position or null if none there or chunk not loaded or world not assigned.
	 */
	public TileEntity getTileEntity() {
		World world = this.world.get();
		return world == null || !world.isBlockPresent(this) ? null : world.getTileEntity(this);
	}

	public CompoundNBT write(CompoundNBT nbt) {
		nbt.putInt("x", getX());
		nbt.putInt("y", getY());
		nbt.putInt("z", getZ());
		nbt.putString("d", dim.getLocation().toString());
		return nbt;
	}

}
