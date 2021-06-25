package cd4017be.lib.util;

import static java.lang.Math.min;

import java.nio.charset.Charset;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import javax.annotation.Nullable;

import cd4017be.lib.block.BlockTE.ITERedstone;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;

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
	public static @Nullable TileEntity neighborTile(TileEntity tile, Direction side) {
		World world = tile.getLevel();
		BlockPos pos = tile.getBlockPos().relative(side);
		return world.isLoaded(pos) ? world.getBlockEntity(pos) : null;
	}

	/**
	 * Chunk-flushing save way to get a TileEntity for given position
	 * @param world World
	 * @param pos tile position
	 * @return the TileEntity or null if not existing or chunk not loaded
	 */
	public static @Nullable TileEntity getTileAt(World world, BlockPos pos) {
		return world.isLoaded(pos) ? world.getBlockEntity(pos) : null;
	}

	/**
	 * Chunk-flushing save way to get a capability from a neighboring block
	 * @param tile source tile
	 * @param side side of source tile to get neighboring capability for
	 * @param cap the capability type
	 * @return capability instance or null if chunk not loaded, no TileEntity or capability not available
	 */
	@Deprecated
	public static @Nullable <T> T neighborCapability(TileEntity tile, Direction side, Capability<T> cap) {
		World world = tile.getLevel();
		BlockPos pos = tile.getBlockPos().relative(side);
		if (!world.isLoaded(pos)) return null;
		TileEntity te = world.getBlockEntity(pos);
		return te != null ? te.getCapability(cap, side.getOpposite()).orElse(null) : null;
	}

	/**
	 * Chunk-flushing save way to get a capability for given position and side
	 * @param world World
	 * @param pos tile position
	 * @param side side to access
	 * @param cap the capability type
	 * @return capability instance or null if chunk not loaded, no TileEntity or capability not available
	 */
	@Deprecated
	public static @Nullable <T> T getCapabilityAt(World world, BlockPos pos, @Nullable Direction side, Capability<T> cap) {
		if (!world.isLoaded(pos)) return null;
		TileEntity te = world.getBlockEntity(pos);
		return te != null ? te.getCapability(cap, side).orElse(null) : null;
	}

	/**
	 * @param world World
	 * @param pos starting position (assumed to be loaded)
	 * @return whether neighbouring blocks are loaded too
	 */
	public static boolean neighboursLoaded(World world, BlockPos pos) {
		int x = pos.getX() & 15, z = pos.getZ() & 15;
		return (x == 0 ? world.isLoaded(pos.offset(-1, 0, 0)) : x == 15 ? world.isLoaded(pos.offset(1, 0, 0)) : true)
			&& (z == 0 ? world.isLoaded(pos.offset(0, 0, -1)) : z == 15 ? world.isLoaded(pos.offset(0, 0, 1)) : true);
	}

	/**
	 * checks to which block side the given position belongs
	 * @param X block relative x
	 * @param Y block relative y
	 * @param Z block relative z
	 * @return side
	 */
	public static Direction hitSide(float X, float Y, float Z) {
		float dx = Math.abs(X -= 0.5F);
		float dy = Math.abs(Y -= 0.5F);
		float dz = Math.abs(Z -= 0.5F);
		return dy > dz && dy > dx ?
				Y < 0 ? Direction.DOWN : Direction.UP
			: dz > dx ?
				Z < 0 ? Direction.NORTH : Direction.SOUTH
			: X < 0 ? Direction.WEST : Direction.EAST;
	}

	public static RayTraceResult getHit(PlayerEntity player, BlockState block, BlockPos pos) {
		throw new UnsupportedOperationException();
		/* TODO implement
		Vector3d p = player.getPositionEyes(1);
		return block.collisionRayTrace(player.world, pos, p, p.add(player.getLook(1).scale(16)));*/
	}

	public static FluidStack getFluid(World world, BlockPos pos, boolean sourceOnly) {
		throw new UnsupportedOperationException();
		/* TODO implement
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block == Blocks.AIR) return null;
		else if (block instanceof IFluidBlock) {
			FluidStack fluid = ((IFluidBlock)block).drain(world, pos, false);
			if (!sourceOnly && fluid == null) return new FluidStack(((IFluidBlock)block).getFluid(), 0);
			else return fluid;
		}
		boolean source = state == block.getDefaultState();
		if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) return source || !sourceOnly ? new FluidStack(Fluids.WATER, source ? 1000 : 0) : null;
		else if (block == Blocks.LAVA|| block == Blocks.FLOWING_LAVA) return source || !sourceOnly ? new FluidStack(Fluids.LAVA, source ? 1000 : 0) : null;
		else return null;*/
	}

	public static Direction getLookDirStrict(Entity entity) {
		if (entity.xRot < -45.0F) return Direction.DOWN;
		if (entity.xRot > 45.0F) return Direction.UP;
		return entity.getDirection();
	}

	public static Direction getLookDirPlacement(Entity entity) {
		if (entity.xRot < -35.0F) return Direction.DOWN;
		if (entity.xRot > 40.0F) return Direction.UP;
		return entity.getDirection();
	}

	/**
	 * @param pos position
	 * @param ref reference point
	 * @return the side of the reference point that faces towards the given position or null if both are equal or not direct neighbors of each other.
	 */
	public static Direction getSide(BlockPos pos, BlockPos ref) {
		int dx = pos.getX() - ref.getX();
		int dy = pos.getY() - ref.getY();
		int dz = pos.getZ() - ref.getZ();
		if (dx != 0)
			if (dy != 0 || dz != 0) return null;
			else return dx == -1 ? Direction.WEST : dx == 1 ? Direction.EAST : null;
		else if (dy != 0)
			if (dz != 0) return null;
			else return dy == -1 ? Direction.DOWN : dy == 1 ? Direction.UP : null;
		else return dz == -1 ? Direction.NORTH : dz == 1 ? Direction.SOUTH : null;
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

	public static double coord(double x, double y, double z, Direction d) {
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

	public static void updateRedstoneOnSide(TileEntity te, int value, Direction side) {
		throw new UnsupportedOperationException();
		/* TODO implement
		ICapabilityProvider cp = neighborTile(te, side);
		if (cp != null && cp instanceof IQuickRedstoneHandler) ((IQuickRedstoneHandler)cp).onRedstoneStateChange(side.getOpposite(), value, te);
		else te.getWorld().neighborChanged(te.getPos().offset(side), te.getBlockType(), te.getPos());*/
	}

	public static <T extends TileEntity & ITERedstone> void updateRedstoneOnSide(T te, Direction side) {
		throw new UnsupportedOperationException();
		/* TODO implement
		ICapabilityProvider cp = neighborTile(te, side);
		if (cp instanceof IQuickRedstoneHandler)
			((IQuickRedstoneHandler)cp).onRedstoneStateChange(side.getOpposite(), te.redstoneLevel(side, false), te);
		else te.getWorld().neighborChanged(te.getPos().offset(side), te.getBlockType(), te.getPos());*/
	}

	/**
	 * Notify neighboring block(s) of TileEntity change
	 * @param te the TileEntity that changed
	 * @param side the side on which a neighbor should be notified or null to notify on all sides.
	 */
	public static void notifyNeighborTile(TileEntity te, Direction side) {
		throw new UnsupportedOperationException();
		/* TODO implement
		if (side != null) {
			BlockPos pos = te.getPos().offset(side);
			World world = te.getWorld();
			if (world == null) return;
			if (world.isBlockLoaded(pos)) {
				BlockState state = world.getBlockState(pos);
				state.getBlock().onNeighborChange(world, pos, te.getPos());			
			}
		} else for (Direction f : Direction.VALUES)
			notifyNeighborTile(te, f);*/
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
	public static ListNBT writeStringArray(String[] arr) {
		ListNBT list = new ListNBT();
		for (String s : arr)
			list.add(StringNBT.valueOf(s));
		return list;
	}

	/**
	 * @param list NBT data
	 * @param arr optional pre-initialized array
	 * @return String array from given NBT data
	 */
	public static String[] readStringArray(ListNBT list, String[] arr) {
		int l = list.size();
		if (arr == null || arr.length < l) arr = new String[l];
		for (int i = 0; i < l; i++)
			arr[i] = list.getString(i);
		return arr;
	}

	public static byte[] putShortArray(short[] arr) {
		byte[] buf = new byte[arr.length << 1];
		for (int i = 0; i < arr.length; i++) {
			short v = arr[i];
			buf[i << 1] = (byte)v;
			buf[i << 1 | 1] = (byte)(v >> 8);
		}
		return buf;
	}

	public static short[] getShortArray(byte[] nbt) {
		short[] arr = new short[nbt.length >> 1];
		getShortArray(nbt, arr);
		return arr;
	}

	public static void getShortArray(byte[] nbt, short[] arr) {
		for (int l = min(arr.length, nbt.length >> 1), i = 0; i < l; i++)
			arr[i] = (short)(nbt[i << 1] & 0xff | nbt[i << 1 | 1] << 8);
	}

	public static INBT readTag(ByteBuf data, byte tagId) {
		switch(tagId) {
		case NBT.TAG_BYTE: return ByteNBT.valueOf(data.readByte());
		case NBT.TAG_SHORT: return ShortNBT.valueOf(data.readShort());
		case NBT.TAG_INT: return IntNBT.valueOf(data.readInt());
		case NBT.TAG_LONG: return LongNBT.valueOf(data.readLong());
		case NBT.TAG_FLOAT: return FloatNBT.valueOf(data.readFloat());
		case NBT.TAG_DOUBLE: return DoubleNBT.valueOf(data.readDouble());
		case NBT.TAG_BYTE_ARRAY: {
			int l = data.readInt();
			if (l > data.readableBytes())
				throw new IndexOutOfBoundsException(l + " > " + data.readableBytes());
			byte[] arr = new byte[l];
			data.readBytes(arr);
			return new ByteArrayNBT(arr);
		}
		case NBT.TAG_INT_ARRAY: {
			int l = data.readInt();
			if (l * 4 > data.readableBytes())
				throw new IndexOutOfBoundsException((l*4) + " > " + data.readableBytes());
			int[] arr = new int[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readInt();
			return new IntArrayNBT(arr);
		}
		case NBT.TAG_LONG_ARRAY: {
			int l = data.readInt();
			if (l * 8 > data.readableBytes())
				throw new IndexOutOfBoundsException((l*8) + " > " + data.readableBytes());
			long[] arr = new long[l];
			for (int i = 0; i < l; i++)
				arr[i] = data.readLong();
			return new LongArrayNBT(arr);
		}
		case NBT.TAG_STRING: {
			int l = data.readUnsignedShort();
			if (l * 2 > data.readableBytes())
				throw new IndexOutOfBoundsException((l*2) + " > " + data.readableBytes());
			byte[] arr = new byte[l];
			data.readBytes(arr);
			return StringNBT.valueOf(new String(arr, UTF8));
		}
		case NBT.TAG_LIST: {
			ListNBT list = new ListNBT();
			tagId = data.readByte();
			for (int l = data.readInt(); l > 0; l--)
				list.add(readTag(data, tagId));
			return list;
		}
		default: return null;
		}
	}

	public static void writeTag(ByteBuf data, INBT tag) {
		switch(tag.getId()) {
		case NBT.TAG_BYTE: data.writeByte(((ByteNBT)tag).getAsByte()); return;
		case NBT.TAG_SHORT: data.writeShort(((ShortNBT)tag).getAsShort()); return;
		case NBT.TAG_INT: data.writeInt(((IntNBT)tag).getAsInt()); return;
		case NBT.TAG_LONG: data.writeLong(((LongNBT)tag).getAsLong()); return;
		case NBT.TAG_FLOAT: data.writeFloat(((FloatNBT)tag).getAsFloat()); return;
		case NBT.TAG_DOUBLE: data.writeDouble(((DoubleNBT)tag).getAsDouble()); return;
		case NBT.TAG_BYTE_ARRAY: {
			byte[] arr = ((ByteArrayNBT)tag).getAsByteArray();
			data.writeInt(arr.length);
			data.writeBytes(arr);
		}	return;
		case NBT.TAG_INT_ARRAY: {
			int[] arr = ((IntArrayNBT)tag).getAsIntArray();
			data.writeInt(arr.length);
			for (int v : arr)
				data.writeInt(v);
		}	return;
		/*case NBT.TAG_LONG_ARRAY: {
			long[] arr = ((LongArrayNBT)tag).getLongArray();
			data.writeInt(arr.length);
			for (long v : arr)
				data.writeLong(v);
		}	return;*/
		case NBT.TAG_STRING: {
			byte[] arr = ((StringNBT)tag).getAsString().getBytes(UTF8);
			data.writeShort(arr.length);
			data.writeBytes(arr);
		}	return;
		case NBT.TAG_LIST: {
			ListNBT list = (ListNBT)tag;
			data.writeByte(list.getElementType());
			data.writeInt(list.size());
			for (INBT stag : list)
				writeTag(data, stag);
		}	return;
		}
	}

}
