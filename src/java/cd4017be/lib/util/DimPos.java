package cd4017be.lib.util;

import java.lang.ref.WeakReference;

import com.google.common.base.MoreObjects;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/**
 * Extended version of {@link BlockPos} with additional dimension (and optional world) information.
 * @author cd4017be
 */
public class DimPos extends BlockPos {

	private static final WeakReference<Level> NO_WORLD = new WeakReference<>(null);

	public final ResourceKey<Level> dim;
	private WeakReference<Level> world = NO_WORLD;

	/**
	 * get the dimensional block position of the given Entity
	 * @param source an Entity
	 */
	public DimPos(Entity source) {
		this(source.blockPosition(), source.level);
	}

	/**
	 * get the dimensional block position of the given BlockEntity
	 * @param source a BlockEntity (must be added to a world)
	 */
	public DimPos(BlockEntity source) {
		this(source.getBlockPos(), source.getLevel());
	}

	/**
	 * @param source the block position
	 * @param world the dimension's world
	 */
	public DimPos(Vec3i source, Level world) {
		this(source, world.dimension());
		this.world = new WeakReference<>(world);
	}

	/**
	 * @param source the block position
	 * @param dim the dimension id
	 */
	public DimPos(Vec3i source, ResourceKey<Level> dim) {
		this(source.getX(), source.getY(), source.getZ(), dim);
	}

	/**
	 * @param x block X-coord
	 * @param y block Y-coord
	 * @param z block Z-coord
	 * @param d dimension id
	 */
	public DimPos(int x, int y, int z, ResourceKey<Level> d) {
		super(x, y, z);
		this.dim = d;
	}

	/**
	 * @param x block X-coord
	 * @param y block Y-coord
	 * @param z block Z-coord
	 * @param world the dimension's world
	 */
	public DimPos(int x, int y, int z, ServerLevel world) {
		this(x, y, z, world.dimension());
		this.world = new WeakReference<>(world);
	}

	/**@param nbt data */
	public DimPos(CompoundTag nbt) {
		this(
			nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"),
			ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString("d")))
		);
	}

	/**
	 * assigns a world instance for later use
	 * @param world the world instance for this dimension
	 */
	public DimPos assignWorld(Level world) {
		if (world.dimension() != dim)
			throw new IllegalArgumentException("given world represents a different dimension");
		this.world = new WeakReference<>(world);
		return this;
	}

	/**
	 * @return the world associated with this position or null if not assigned
	 * @see #assignWorld(Level)
	 */
	public Level getWorld() {
		return world.get();
	}

	/**
	 * A special version of {@link #getWorld()} for <b> server side use only</b>, that automatically retrieves and assignes the ServerLevel.
	 * @return the world server associated with this position or null if this dimension id is invalid or the world couldn't be loaded.
	 */
	public ServerLevel getServerWorld(Level ref) {
		Level world = this.world.get();
		if (world instanceof ServerLevel) return (ServerLevel)world;
		ServerLevel ws = ref.getServer().getLevel(dim);
		if (ws != null) this.world = new WeakReference<Level>(ws);
		return ws;
	}

	@Override
	public DimPos offset(int x, int y, int z) {
		if (x == 0 && y == 0 && z == 0) return this;
		DimPos pos = new DimPos(getX() + x, getY() + y, getZ() + z, dim);
		pos.world = world;
		return pos;
	}

	@Override
	public DimPos relative(Direction facing, int n) {
		if (n == 0) return this;
		DimPos pos = new DimPos(getX() + facing.getStepX() * n, getY() + facing.getStepY() * n, getZ() + facing.getStepZ() * n, dim);
		pos.world = world;
		return pos;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && !(o instanceof DimPos && ((DimPos)o).dim != dim);
	}

	@Override
	public int compareTo(Vec3i o) {
		if (o instanceof DimPos) {
			ResourceKey<Level> dim = ((DimPos)o).dim;
			if (dim != this.dim)
				return this.dim.getRegistryName().compareTo(dim.getRegistryName());
		}
		return super.compareTo(o);
	}

	@Override
	public double distSqr(Vec3i to) {
		return to instanceof DimPos && ((DimPos)to).dim != dim ? Double.POSITIVE_INFINITY : super.distSqr(to);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).add("d", dim).toString();
	}

	/**
	 * @return whether this position is loaded (returns false if no world assigned)
	 */
	public boolean isLoaded() {
		Level world = this.world.get();
		return world != null && world.isLoaded(this);
	}

	/**
	 * @return the BlockState at this position.
	 */
	public BlockState getBlock() {
		Level world = this.world.get();
		return world == null ? Blocks.AIR.defaultBlockState() : world.getBlockState(this);
	}

	/**
	 * set the BlockState at this position
	 * @param state new block state
	 * @return whether the change was successful
	 */
	public boolean setBlock(BlockState state) {
		Level world = this.world.get();
		return world != null && world.setBlockAndUpdate(this, state);
	}

	/**
	 * utility method to get a BlockEntity without force-loading chunks.
	 * @return the BlockEntity at this position or null if none there or chunk not loaded or world not assigned.
	 */
	public BlockEntity getTileEntity() {
		Level world = this.world.get();
		return world == null || !world.isLoaded(this) ? null : world.getBlockEntity(this);
	}

	public CompoundTag write(CompoundTag nbt) {
		nbt.putInt("x", getX());
		nbt.putInt("y", getY());
		nbt.putInt("z", getZ());
		nbt.putString("d", dim.location().toString());
		return nbt;
	}

}
