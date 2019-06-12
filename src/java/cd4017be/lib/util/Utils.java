package cd4017be.lib.util;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import javax.annotation.Nullable;

import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.oredict.OreDictionary;

/**
 *
 * @author CD4017BE
 */
public class Utils {

	public static final BlockPos NOWHERE = new BlockPos(0, -1, 0);
	public static final byte IN = -1, OUT = 1, ACC = 0;
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * performs the given operation for all blocks in a square ring.
	 * @param center center of the ring
	 * @param axis the axis around which the ring forms
	 * @param radius radius of the ring in blocks
	 * @param operation performed operation supplied with the block's position and its square distance to the center
	 */
	public static void forRing(BlockPos center, Axis axis, int radius, ObjIntConsumer<BlockPos> operation) {
		int x = center.getX(), y = center.getY(), z = center.getZ();
		switch(axis) {
		case X:
			for (int i = -radius; i < radius; i++) {
				int d = radius * radius + i * i;
				operation.accept(new BlockPos(x, y - radius, z + i), d);
				operation.accept(new BlockPos(x, y + radius, z - i), d);
				operation.accept(new BlockPos(x, y - i, z - radius), d);
				operation.accept(new BlockPos(x, y + i, z + radius), d);
			} break;
		case Y:
			for (int i = -radius; i < radius; i++) {
				int d = radius * radius + i * i;
				operation.accept(new BlockPos(x - radius, y, z + i), d);
				operation.accept(new BlockPos(x + radius, y, z - i), d);
				operation.accept(new BlockPos(x - i, y, z - radius), d);
				operation.accept(new BlockPos(x + i, y, z + radius), d);
			} break;
		case Z:
			for (int i = -radius; i < radius; i++) {
				int d = radius * radius + i * i;
				operation.accept(new BlockPos(x - radius, y + i, z), d);
				operation.accept(new BlockPos(x + radius, y - i, z), d);
				operation.accept(new BlockPos(x - i, y - radius, z), d);
				operation.accept(new BlockPos(x + i, y + radius, z), d);
			} break;
		}
	}

	/**
	 * Chunk-flushing save way to get a neighboring TileEntity
	 * @param tile source tile
	 * @param side side of source tile to get neighbor for
	 * @return neighbor TileEntity or null if not existing or chunk not loaded
	 */
	public static @Nullable TileEntity neighborTile(TileEntity tile, EnumFacing side) {
		World world = tile.getWorld();
		BlockPos pos = tile.getPos().offset(side);
		return world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
	}

	/**
	 * Chunk-flushing save way to get a TileEntity for given position
	 * @param world World
	 * @param pos tile position
	 * @return the TileEntity or null if not existing or chunk not loaded
	 */
	public static @Nullable TileEntity getTileAt(World world, BlockPos pos) {
		return world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
	}

	/**
	 * Chunk-flushing save way to get a capability from a neighboring block
	 * @param tile source tile
	 * @param side side of source tile to get neighboring capability for
	 * @param cap the capability type
	 * @return capability instance or null if chunk not loaded, no TileEntity or capability not available
	 */
	public static @Nullable <T> T neighborCapability(TileEntity tile, EnumFacing side, Capability<T> cap) {
		World world = tile.getWorld();
		BlockPos pos = tile.getPos().offset(side);
		if (!world.isBlockLoaded(pos)) return null;
		TileEntity te = world.getTileEntity(pos);
		return te != null ? te.getCapability(cap, side.getOpposite()) : null;
	}

	/**
	 * Chunk-flushing save way to get a capability for given position and side
	 * @param world World
	 * @param pos tile position
	 * @param side side to access
	 * @param cap the capability type
	 * @return capability instance or null if chunk not loaded, no TileEntity or capability not available
	 */
	public static @Nullable <T> T getCapabilityAt(World world, BlockPos pos, @Nullable EnumFacing side, Capability<T> cap) {
		if (!world.isBlockLoaded(pos)) return null;
		TileEntity te = world.getTileEntity(pos);
		return te != null ? te.getCapability(cap, side) : null; 
	}

	/**
	 * @param world World
	 * @param pos starting position (assumed to be loaded)
	 * @return whether neighbouring blocks are loaded too
	 */
	public static boolean neighboursLoaded(World world, BlockPos pos) {
		int x = pos.getX() & 15, z = pos.getZ() & 15;
		return (x == 0 ? world.isBlockLoaded(pos.add(-1, 0, 0)) : x == 15 ? world.isBlockLoaded(pos.add(1, 0, 0)) : true)
			&& (z == 0 ? world.isBlockLoaded(pos.add(0, 0, -1)) : z == 15 ? world.isBlockLoaded(pos.add(0, 0, 1)) : true);
	}

	/**
	 * checks to which block side the given position belongs
	 * @param X block relative x
	 * @param Y block relative y
	 * @param Z block relative z
	 * @return side
	 */
	public static EnumFacing hitSide(float X, float Y, float Z) {
		float dx = Math.abs(X -= 0.5F);
		float dy = Math.abs(Y -= 0.5F);
		float dz = Math.abs(Z -= 0.5F);
		return dy > dz && dy > dx ?
				Y < 0 ? EnumFacing.DOWN : EnumFacing.UP
			: dz > dx ?
				Z < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH
			: X < 0 ? EnumFacing.WEST : EnumFacing.EAST;
	}

	public static RayTraceResult getHit(EntityPlayer player, IBlockState block, BlockPos pos) {
		Vec3d p = player.getPositionEyes(1);
		return block.collisionRayTrace(player.world, pos, p, p.add(player.getLook(1).scale(16)));
	}

	public static class ItemType {
		public final ItemStack[] types;
		public final boolean meta;
		public final boolean nbt;
		public final int[] ores;
		/**
		 * An ItemType that matches all items
		 */
		public ItemType() 
		{
			this.types = null;
			this.ores = null;
			this.meta = false;
			this.nbt = false;
		}
		/**
		 * An ItemType that matches only the exact given items
		 * @param types the items to match
		 */
		public ItemType(ItemStack... types)
		{
			this.types = types;
			this.ores = null;
			this.meta = true;
			this.nbt = true;
		}
		/**
		 * This ItemType matches the given items with special flags
		 * @param meta Metadata flag (false = ignore different metadata)
		 * @param nbt NBT-data flag (false = ignore different NBT-data)
		 * @param ore OreDictionary flag (true = also matches if equal ore types)
		 * @param types the items to match
		 */
		public ItemType(boolean meta, boolean nbt, boolean ore, ItemStack... types)
		{
			this.types = types;
			this.meta = meta;
			this.nbt = nbt;
			if (ore) {
				Set<Integer> list = new HashSet<Integer>();
				for (int i = 0; i < types.length; i++)
					for (int j : OreDictionary.getOreIDs(types[i])) 
						list.add(j);
				ores = new int[list.size()];
				int n = 0;
				for (int i : list) ores[n++] = i;
			} else ores = null;
		}
		
		public boolean matches(ItemStack item) 
		{
			return getMatch(item) >= 0;
		}
		
		public int getMatch(ItemStack item)
		{
			if (item.isEmpty()) return -1;
			else if (types == null) return -1;
			for (int i = 0; i < types.length; i++) {
				ItemStack type = types[i];
				if (item.getItem() == type.getItem() && 
					(!meta || item.getItemDamage() == type.getItemDamage()) &&
					(!nbt || ItemStack.areItemStackTagsEqual(item, type)))
					return i;
			}
			if (ores == null) return -1;
			for (int o : OreDictionary.getOreIDs(item))
				for (int i = 0; i < ores.length; i++)
					if (ores[i] == o) return i;
			return -1;
		}
		
	}
	
	public static FluidStack getFluid(World world, BlockPos pos, boolean sourceOnly)
	{
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block == Blocks.AIR) return null;
		else if (block instanceof IFluidBlock) {
			FluidStack fluid = ((IFluidBlock)block).drain(world, pos, false);
			if (!sourceOnly && fluid == null) return new FluidStack(((IFluidBlock)block).getFluid(), 0);
			else return fluid;
		}
		boolean source = state == block.getDefaultState();
		if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) return source || !sourceOnly ? new FluidStack(FluidRegistry.WATER, source ? 1000 : 0) : null;
		else if (block == Blocks.LAVA|| block == Blocks.FLOWING_LAVA) return source || !sourceOnly ? new FluidStack(FluidRegistry.LAVA, source ? 1000 : 0) : null;
		else return null;
	}

	public static EnumFacing getLookDirStrict(Entity entity) {
		if (entity.rotationPitch < -45.0F) return EnumFacing.DOWN;
		if (entity.rotationPitch > 45.0F) return EnumFacing.UP;
		return entity.getHorizontalFacing();
	}

	public static EnumFacing getLookDirPlacement(Entity entity) {
		if (entity.rotationPitch < -35.0F) return EnumFacing.DOWN;
		if (entity.rotationPitch > 40.0F) return EnumFacing.UP;
		return entity.getHorizontalFacing();
	}

	/**
	 * @param pos position
	 * @param ref reference point
	 * @return the side of the reference point that faces towards the given position or null if both are equal or not direct neighbors of each other.
	 */
	public static EnumFacing getSide(BlockPos pos, BlockPos ref) {
		int dx = pos.getX() - ref.getX();
		int dy = pos.getY() - ref.getY();
		int dz = pos.getZ() - ref.getZ();
		if (dx != 0)
			if (dy != 0 || dz != 0) return null;
			else return dx == -1 ? EnumFacing.WEST : dx == 1 ? EnumFacing.EAST : null;
		else if (dy != 0)
			if (dz != 0) return null;
			else return dy == -1 ? EnumFacing.DOWN : dy == 1 ? EnumFacing.UP : null;
		else return dz == -1 ? EnumFacing.NORTH : dz == 1 ? EnumFacing.SOUTH : null;
	}

	/**
	 * @param pos
	 * @param axis
	 * @return the element of pos specified by axis
	 */
	public static int coord(BlockPos pos, Axis axis) {
		switch(axis) {
		case X: return pos.getX();
		case Y: return pos.getY();
		case Z: return pos.getZ();
		default: return 0;
		}
	}

	public static double coord(double x, double y, double z, EnumFacing d) {
		switch(d) {
		case DOWN: return -y;
		case UP: return y;
		case NORTH: return -z;
		case SOUTH: return z;
		case WEST: return -x;
		case EAST: return x;
		default: return 0;
		}
	}

	public static void updateRedstoneOnSide(TileEntity te, int value, EnumFacing side) {
		ICapabilityProvider cp = neighborTile(te, side);
		if (cp != null && cp instanceof IQuickRedstoneHandler) ((IQuickRedstoneHandler)cp).onRedstoneStateChange(side.getOpposite(), value, te);
		else te.getWorld().neighborChanged(te.getPos().offset(side), te.getBlockType(), te.getPos());
	}

	public static <T extends TileEntity & IRedstoneTile> void updateRedstoneOnSide(T te, EnumFacing side) {
		ICapabilityProvider cp = neighborTile(te, side);
		if (cp instanceof IQuickRedstoneHandler)
			((IQuickRedstoneHandler)cp).onRedstoneStateChange(side.getOpposite(), te.redstoneLevel(side, false), te);
		else te.getWorld().neighborChanged(te.getPos().offset(side), te.getBlockType(), te.getPos());
	}

	/**
	 * Notify neighboring block(s) of TileEntity change
	 * @param te the TileEntity that changed
	 * @param side the side on which a neighbor should be notified or null to notify on all sides.
	 */
	public static void notifyNeighborTile(TileEntity te, EnumFacing side) {
		if (side != null) {
			BlockPos pos = te.getPos().offset(side);
			World world = te.getWorld();
			if (world == null) return;
			if (world.isBlockLoaded(pos)) {
				IBlockState state = world.getBlockState(pos);
				state.getBlock().onNeighborChange(world, pos, te.getPos());			
			}
		} else for (EnumFacing f : EnumFacing.VALUES)
			notifyNeighborTile(te, f);
	}

	/**
	 * forward or backward cycle a number stored in some sub-bits of an integer
	 * @param cfg storage integer
	 * @param i start bit index
	 * @param m bit mask
	 * @param r number range
	 * @param incr true to increase, false to decrease
	 * @return edited storage integer
	 */
	public static int cycleState(int cfg, int i, int m, int r, boolean incr) {
		return cfg & ~(m << i) | ((cfg >> i & m) + (incr ? 1 : r - 1)) % r << i;
	}

	/**
	 * set a number stored in some sub-bits of an integer
	 * @param cfg storage integer
	 * @param i start bit index
	 * @param m bit mask
	 * @param x number to store
	 * @return edited storage integer
	 */
	public static int setState(int cfg, int i, int m, int x) {
		return cfg & ~(m << i) | (x & m) << i;
	}

	/**
	 * forward or backward cycle a number stored in some sub-bits of an long
	 * @param cfg storage long
	 * @param i start bit index
	 * @param m bit mask
	 * @param r number range
	 * @param incr true to increase, false to decrease
	 * @return edited storage long
	 */
	public static long cycleState(long cfg, int i, long m, int r, boolean incr) {
		return cfg & ~(m << i) | (long)(((int)(cfg >> i & m) + (incr ? 1 : r - 1)) % r) << i;
	}

	/**
	 * set a number stored in some sub-bits of an long
	 * @param cfg storage long
	 * @param i start bit index
	 * @param m bit mask
	 * @param x number to store
	 * @return edited storage long
	 */
	public static long setState(long cfg, int i, long m, int x) {
		return cfg & ~(m << i) | ((long)x & m) << i;
	}

	/**
	 * sets all elements of the array to a given value
	 * @param arr
	 * @param val
	 * @return the same array
	 */
	public static <T> T[] init(T[] arr, T val) {
		for (int i = 0; i < arr.length; i++) arr[i] = val;
		return arr;
	}

	/**
	 * sets all elements of the array to a value computed by the given function
	 * @param arr
	 * @param val function to compute array elements
	 * @return the same array
	 */
	public static <T> T[] init(T[] arr, IntFunction<T> val) {
		for (int i = 0; i < arr.length; i++) arr[i] = val.apply(i);
		return arr;
	}

	/**
	 * @param arr array of Strings
	 * @return list tag containing the serialized NBT data of each element
	 */
	public static NBTTagList writeStringArray(String[] arr) {
		NBTTagList list = new NBTTagList();
		for (String s : arr)
			list.appendTag(new NBTTagString(s));
		return list;
	}

	/**
	 * @param list NBT data
	 * @param arr optional pre-initialized array
	 * @return String array from given NBT data
	 */
	public static String[] readStringArray(NBTTagList list, String[] arr) {
		int l = list.tagCount();
		if (arr == null || arr.length < l) arr = new String[l];
		for (int i = 0; i < l; i++)
			arr[i] = list.getStringTagAt(i);
		return arr;
	}

	public static NBTBase readTag(ByteBuf data, byte tagId) {
		switch(tagId) {
		case NBT.TAG_BYTE: return new NBTTagByte(data.readByte());
		case NBT.TAG_SHORT: return new NBTTagShort(data.readShort());
		case NBT.TAG_INT: return new NBTTagInt(data.readInt());
		case NBT.TAG_LONG: return new NBTTagLong(data.readLong());
		case NBT.TAG_FLOAT: return new NBTTagFloat(data.readFloat());
		case NBT.TAG_DOUBLE: return new NBTTagDouble(data.readDouble());
		case NBT.TAG_BYTE_ARRAY: {
			int l = data.readInt();
			if (l > data.readableBytes())
				throw new IndexOutOfBoundsException(l + " > " + data.readableBytes());
			byte[] arr = new byte[l];
			data.readBytes(arr);
			return new NBTTagByteArray(arr);
		}
		case NBT.TAG_INT_ARRAY: {
			int l = data.readInt();
			if (l * 4 > data.readableBytes())
				throw new IndexOutOfBoundsException((l*4) + " > " + data.readableBytes());
			int[] arr = new int[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readInt();
			return new NBTTagIntArray(arr);
		}
		case NBT.TAG_LONG_ARRAY: {
			int l = data.readInt();
			if (l * 8 > data.readableBytes())
				throw new IndexOutOfBoundsException((l*8) + " > " + data.readableBytes());
			long[] arr = new long[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readLong();
			return new NBTTagLongArray(arr);
		}
		case NBT.TAG_STRING: {
			int l = data.readUnsignedShort();
			if (l * 2 > data.readableBytes())
				throw new IndexOutOfBoundsException((l*2) + " > " + data.readableBytes());
			byte[] arr = new byte[l];
			data.readBytes(arr);
			return new NBTTagString(new String(arr, UTF8));
		}
		case NBT.TAG_LIST: {
			NBTTagList list = new NBTTagList();
			tagId = data.readByte();
			for (int l = data.readInt(); l > 0; l--)
				list.appendTag(readTag(data, tagId));
			return list;
		}
		default: return null;
		}
	}

	public static void writeTag(ByteBuf data, NBTBase tag) {
		switch(tag.getId()) {
		case NBT.TAG_BYTE: data.writeByte(((NBTTagByte)tag).getByte()); return;
		case NBT.TAG_SHORT: data.writeShort(((NBTTagShort)tag).getShort()); return;
		case NBT.TAG_INT: data.writeInt(((NBTTagInt)tag).getInt()); return;
		case NBT.TAG_LONG: data.writeLong(((NBTTagLong)tag).getLong()); return;
		case NBT.TAG_FLOAT: data.writeFloat(((NBTTagFloat)tag).getFloat()); return;
		case NBT.TAG_DOUBLE: data.writeDouble(((NBTTagDouble)tag).getDouble()); return;
		case NBT.TAG_BYTE_ARRAY: {
			byte[] arr = ((NBTTagByteArray)tag).getByteArray();
			data.writeInt(arr.length);
			data.writeBytes(arr);
		}	return;
		case NBT.TAG_INT_ARRAY: {
			int[] arr = ((NBTTagIntArray)tag).getIntArray();
			data.writeInt(arr.length);
			for (int v : arr)
				data.writeInt(v);
		}	return;
		/*case NBT.TAG_LONG_ARRAY: {
			long[] arr = ((NBTTagLongArray)tag).getLongArray();
			data.writeInt(arr.length);
			for (long v : arr)
				data.writeLong(v);
		}	return;*/
		case NBT.TAG_STRING: {
			byte[] arr = ((NBTTagString)tag).getString().getBytes(UTF8);
			data.writeShort(arr.length);
			data.writeBytes(arr);
		}	return;
		case NBT.TAG_LIST: {
			NBTTagList list = (NBTTagList)tag;
			data.writeByte(list.getTagType());
			data.writeInt(list.tagCount());
			for (NBTBase stag : list)
				writeTag(data, stag);
		}	return;
		}
	}

}
