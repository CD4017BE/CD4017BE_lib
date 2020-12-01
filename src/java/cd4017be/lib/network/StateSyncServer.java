package cd4017be.lib.network;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;
import cd4017be.lib.util.ItemFluidUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * Utility class for encoding multi-element synchronization packets.<dl>
 * <b>Usage:</b><br>
 * To change the custom packet header, write its contents to {@link #buffer} and call {@link #setHeader()}.<br>
 * To encode a packet, first write all fixed sized objects to {@link #buffer} in proper order and call {@link #endFixed()} afterwards.
 * Then write the variable sized objects either to {@link #buffer} one by one in proper order calling {@link #put()} after each object or use the {@link #putAll(Object...)} method to automatically encode certain types of objects.
 * Finally call {@link #encodePacket()} to get the packet.<br>
 * An alternative approach is to encode only some objects through the various {@code set(...)} methods by their index which can be in any order then. Again create the packet with {@link #encodePacket()} afterwards.
 * 
 * @author CD4017BE
 */
public class StateSyncServer extends StateSynchronizer {

	public byte[] header;
	/**helper buffer instance */
	public final PacketBuffer buffer;
	private final int fixCount;
	private int elIdx = -1;
	public boolean sendAll = true;

	public StateSyncServer(int count, int... sizes) {
		super(
			count, accumulate(sizes), sizes,
			new byte[count - sizes.length][],
			ByteBuffer.allocate(count(sizes))
		);
		if (sizes.length > count)
			throw new IllegalArgumentException("can't have more fixed sized elements than total elements!");
		this.fixCount = sizes.length;
		this.buffer = new PacketBuffer(Unpooled.buffer());
	}

	/**
	 * mark the next packet to be an initial packet, meaning it will send all states regardless of changed or not.
	 * @return this
	 */
	public StateSyncServer setInitPkt() {
		this.sendAll = true;
		return this;
	}

	/**
	 * sets the header data to what was just written to {@link #buffer}.
	 */
	public void setHeader() {
		int l = buffer.readableBytes();
		if (header == null || header.length != l)
			header = new byte[l];
		buffer.readBytes(header);
	}

	/**
	 * start writing objects to synchronize
	 * @return this
	 */
	public StateSyncServer begin() {
		buffer.clear();
		set.clear();
		elIdx = -1;
		return this;
	}

	/**
	 * Resolves all changes in the fixed sized elements that have to be written to {@link #buffer} prior to this call.
	 * @return this
	 */
	public StateSyncServer endFixed() {
		BitSet chng = set;
		int l;
		if (sendAll) {
			buffer.readBytes(rawStates.array());
			l = buffer.readableBytes();
			chng.set(1, count + 1);
		} else {
			byte[] arr0 = buffer.array(), arr1 = rawStates.array();
			int i0 = buffer.arrayOffset(), i1 = 0;
			l = buffer.readableBytes();
			int j = 0;
			for (int n : objIdx) {
				if ((l -= n) < 0) throw new IllegalStateException("end of buffer reached!");
				for (; n > 0; n--, i0++, i1++)
					if (arr0[i0] != arr1[i1]) {
						chng.set(j+1);
						System.arraycopy(arr0, i0, arr1, i1, n);
					}
				j++;
			}
		}
		if (l > 0) throw new IllegalStateException("buffer still has unread data left!");
		buffer.clear();
		elIdx = objIdx.length;
		return this;
	}

	/**
	 * encode a data packet containing the resolved changes.
	 * @return the synchronization packet to be send to {@link StateSyncClient#decodePacket(PacketBuffer)} or null if there are no changes to send
	 */
	public PacketBuffer encodePacket() {
		if (elIdx < 0 && buffer.readableBytes() > 0) endFixed();
		BitSet chng = set;
		int cc = chng.cardinality();
		if (cc == 0) return null;
		PacketBuffer buf = buffer;
		buf.clear();
		if (header != null) buf.writeBytes(header);
		boolean all = sendAll || cc >= count;
		write(buf);
		if (all) {
			buf.writeBytes(rawStates);
			for (Object arr : objStates)
				buf.writeBytes((byte[])arr);
		} else {
			int[] sizes = this.objIdx;
			for (int i = 0, j = 0; i < fixCount;) {
				int n = sizes[i];
				if (chng.get(++i))
					buf.writeBytes(rawStates.array(), j, n);
				j += n;
			}
			for (int p = chng.nextSetBit(fixCount + 1); p >= 0; p = chng.nextSetBit(p + 1))
				buf.writeBytes((byte[])objStates[p - fixCount - 1]);
		}
		elIdx = -1;
		sendAll = false;
		return new PacketBuffer(buf.copy());
	}

	/**
	 * submit the data that was just written to {@link #buffer} as next variable sized element.
	 * @return this
	 */
	public void put() {
		set(elIdx++);
	}

	/**
	 * submit a byte array as next variable sized element.
	 * @param data byte array element
	 */
	public void put(byte[] data) {
		set(elIdx++, data);
	}

	/**
	 * update variable i with the data that was just written to {@link #buffer}.
	 * @param i variable index
	 * @return this
	 */
	public void set(int i) {
		if (i < 0) throw new IllegalStateException("element must be submitted as variabled sized!");
		if (i < fixCount) {
			byte[] arr0 = buffer.array(), arr1 = rawStates.array();
			int i0 = buffer.arrayOffset() + buffer.readerIndex(), l0 = buffer.readableBytes();
			int i1 = indices[i], l1 = objIdx[i];
			if (sendAll) {
				set.set(i+1);
				System.arraycopy(arr0, i0, arr1, i1, l1);
			} else for (int l = i1 + l1; i1 < l; i0++, i1++)
				if (arr0[i0] != arr1[i1]) {
					set.set(i+1);
					System.arraycopy(arr0, i0, arr1, i1, l - i1);
				}
			if (l0 > l1) {
				buffer.skipBytes(l1);
				set(i + 1);
			}
		} else {
			byte[] arr0 = buffer.array(), arr1 = (byte[])objStates[i - fixCount];
			int i0 = buffer.arrayOffset() + buffer.readerIndex(), l = buffer.readableBytes();
			if (sendAll || l != arr1.length) {
				set.set(i+1);
				objStates[i - fixCount] = Arrays.copyOfRange(arr0, i0, i0 + l);
			} else for (int i1 = 0; i1 < l; i0++, i1++)
				if (arr0[i0] != arr1[i1]) {
					set.set(i+1);
					System.arraycopy(arr0, i0, arr1, i1, l - i1);
				}
		}
		buffer.clear();
	}

	/**
	 * update variable i with given byte array.
	 * @param data byte array element
	 */
	public void set(int i, byte[] data) {
		i -= fixCount;
		if (sendAll || !Arrays.equals(data, (byte[])objStates[i])) {
			objStates[i] = data;
			set.set(i+1);
		}
	}

	/**
	 * write an int array to {@link #buffer}
	 * @param data int array element
	 * @return this
	 */
	public StateSyncServer writeIntArray(int[] data) {
		PacketBuffer buf = buffer;
		for (int v : data) buf.writeInt(v);
		return this;
	}

	private int curSize() {
		int i = Arrays.binarySearch(indices, buffer.writerIndex());
		return i >= 0 ? objIdx[i] : 0;
	}

	private StateSyncServer check(int i, int l) {
		if (i >= fixCount) throw new IllegalStateException("element must be submitted as fixed sized!");
		if ((i < 0 ? curSize() : objIdx[i]) != l) throw new IllegalStateException("wrong element size!");
		return this;
	}

	/**
	 * write an int value to {@link #buffer} with bit resolution according to its registered fixed element size
	 * @param v value
	 * @return this
	 */
	public StateSyncServer writeInt(int v) {
		switch(curSize()) {
		case 4: buffer.writeInt(v); break;
		case 3: buffer.writeMedium(v); break;
		case 2: buffer.writeShort(v); break;
		case 1: buffer.writeByte(v); break;
		default: throw new IllegalStateException("size for int element must be 1, 2, 3 or 4!");
		}
		return this;
	}

	/**
	 * update variable i with the given value
	 * @param i variable index
	 * @param v value<br>
	 * Supported fixed sized types are: byte, short, int, long, float, double, byte[], int[], BlockPos, UUID, any Enum (+null)<br>
	 * Supported variable sized types are: byte[], int[], String (+null), NBTTagCompound, ItemStack, FluidStack
	 * @return this
	 */
	public StateSyncServer set(int i, Object v) {
		if (v instanceof Number) {
			Number n = (Number)v;
			if (n instanceof Float)
				check(i, 4).buffer.writeFloat(n.floatValue());
			else if (n instanceof Double)
				check(i, 8).buffer.writeDouble(n.doubleValue());
			else if (n instanceof Long)
				check(i, 8).buffer.writeLong(n.longValue());
			else writeInt(n.intValue());
		} else if (v instanceof byte[]) {
			if (i < fixCount) buffer.writeBytes((byte[])v);
			else buffer.writeByteArray((byte[])v);
		} else if (v instanceof int[])
			if (i < fixCount) writeIntArray((int[])v);
			else buffer.writeVarIntArray((int[])v);
		else if (v instanceof BlockPos)
			check(i, 8).buffer.writeBlockPos((BlockPos)v);
		else if (v instanceof Enum)
			writeInt(((Enum<?>)v).ordinal());
		else if (v == null)
			if (i < fixCount) writeInt(-1);
			else buffer.writeString("");
		else if (v instanceof String)
			buffer.writeString((String)v);
		else if (v instanceof UUID)
			check(i, 16).buffer.writeUniqueId((UUID)v);
		else if (v instanceof NBTTagCompound)
			buffer.writeCompoundTag((NBTTagCompound)v);
		else if (v instanceof ItemStack) {
			ItemStack stack = (ItemStack)v;
			if (stack.isEmpty()) buffer.writeShort(-1);
			else {
				Item item = stack.getItem();
				buffer.writeShort(Item.getIdFromItem(item));
				buffer.writeInt(stack.getCount());
				buffer.writeShort(stack.getMetadata());
				NBTTagCompound nbt = null;
				if (item.isDamageable() || item.getShareTag())
					nbt = item.getNBTShareTag(stack);
				buffer.writeCompoundTag(nbt);
			}
		} else if (v instanceof FluidStack)
			ItemFluidUtil.writeFluidStack(buffer, (FluidStack)v);
		else throw new IllegalArgumentException("invalid element type!");
		if (i >= 0) set(i);
		return this;
	}

	/**
	 * submit elements for synchronization
	 * @param values the element values in proper order<br>
	 * Note: the given values must be either all fixed sized or all variable sized
	 * @return this
	 * @see #set(int, Object)
	 */
	public StateSyncServer putAll(Object... values) {
		int i = elIdx;
		if (i < 0) for (Object v : values) set(-1, v);
		else {
			for (Object v : values) set(i++, v);
			elIdx = i;
		}
		return this;
	}

}
