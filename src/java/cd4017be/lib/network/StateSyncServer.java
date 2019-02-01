package cd4017be.lib.network;

import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Utility class for encoding multi-element synchronization packets.
 * @author CD4017BE
 */
public class StateSyncServer extends StateSynchronizer {

	public byte[] header;
	private final byte[] lastFix;
	private final byte[][] lastVar;
	/**helper buffer instance */
	public final PacketBuffer buffer;
	private final int varCount;
	private int elIdx = -1;
	private boolean sendAll = true;

	public StateSyncServer(int count, int... sizes) {
		super(count, sizes);
		int l = 0;
		for (int n : sizes) l += n;
		this.varCount = count - sizes.length;
		this.lastFix = new byte[l];
		this.lastVar = new byte[varCount][];
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
	 * Resolves all changes in the fixed sized elements that have to be written to {@link #buffer} prior to this call.
	 * @return this
	 */
	public StateSyncServer endFixed() {
		BitSet chng = changes;
		int l;
		if (sendAll) {
			buffer.readBytes(lastFix);
			l = buffer.readableBytes();
			chng.set(1, count + 1);
		} else {
			chng.clear();
			byte[] arr0 = buffer.array(), arr1 = lastFix;
			int i0 = buffer.arrayOffset(), i1 = 0;
			l = buffer.readableBytes();
			int j = 0;
			for (int n : sizes) {
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
		elIdx = 0;
		return this;
	}

	/**
	 * encode a data packet containing the resolved changes.
	 * @return the synchronization packet to be send to {@link StateSyncClient#decodePacket(PacketBuffer)} or null if there are no changes to send
	 */
	public PacketBuffer encodePacket() {
		if (elIdx < 0) endFixed();
		BitSet chng = changes;
		int cc = chng.cardinality();
		if (cc == 0) return null;
		PacketBuffer buf = buffer;
		buf.clear();
		if (header != null) buf.writeByteArray(header);
		boolean all = sendAll || cc >= count;
		if (cc > maxIdxCount) {
			int p = buf.writerIndex() + modSetBytes;
			buf.writeBytes(chng.toByteArray());
			buf.writerIndex(p);
		} else {
			int b = idxBits;
			int j = 1 + idxCountBits;
			int v = 1 | cc << 1;
			for (int i = chng.nextSetBit(1); i >= 0; i = chng.nextSetBit(i + 1)) {
				v |= i << j;
				if ((j += b) >= 16) {
					buf.writeShortLE(v);
					v >>>= 16; j -= 16;
				}
			}
			if (j > 8) buf.writeShortLE(v);
			else if (j > 0) buf.writeByte(v);
		}
		if (all) {
			buf.writeBytes(lastFix);
			for (byte[] arr : lastVar)
				buf.writeBytes(arr);
		} else {
			int[] sizes = this.sizes;
			for (int i = 0, j = 0, l = varCount; i < l; i++) {
				int n = sizes[i];
				if (chng.get(i))
					buf.writeBytes(lastFix, j, j + n);
				j += n;
			}
			for (int p = chng.nextSetBit(sizes.length + 1); p >= 0; p = chng.nextSetBit(p + 1))
				buf.writeByteArray(lastVar[p]);
		}
		elIdx = -1;
		sendAll = false;
		return buf;
	}

	/**
	 * submit the data that was just written to {@link #buffer} as next variable sized element.
	 * @return this
	 */
	public void put() {
		int i = elIdx;
		if (i < 0) throw new IllegalStateException("element must be submitted as variabled sized!");
		byte[] arr0 = buffer.array(), arr1 = lastVar[i];
		int i0 = buffer.arrayOffset(), l = buffer.readableBytes();
		if (sendAll || l != arr1.length) {
			changes.set(i+1);
			lastVar[i] = Arrays.copyOfRange(arr0, i0, i0 + l);
		} else {
			for (int i1 = 0; i1 < l; i0++, i1++)
				if (arr0[i0] != arr1[i1]) {
					changes.set(i+1);
					System.arraycopy(arr0, i0, arr1, i1, l - i1);
				}
		}
		elIdx = i + 1;
	}

	/**
	 * submit a byte array as next variable sized element.
	 * @param data byte array element
	 */
	public void put(byte[] data) {
		int i = elIdx++;
		if (sendAll || !Arrays.equals(data, lastVar[i])) {
			lastVar[i] = data;
			changes.set(i+1);
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
		int j = buffer.readableBytes();
		for (int n : sizes)
			if ((j -= n) < 0) return n;
		return 0;
	}

	private StateSyncServer check(int l) {
		if (elIdx >= 0) throw new IllegalStateException("element must be submitted as fixed sized!");
		if (curSize() != l) throw new IllegalStateException("wrong element size!");
		return this;
	}

	/**
	 * write an int value to buffer with bit resolution according to element size
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
	 * submit elements for synchronization
	 * @param values the element values in proper order
	 * @return this
	 */
	public StateSyncServer putAll(Object... values) {
		for (Object v : values)
			if (v instanceof Number) {
				Number n = (Number)v;
				if (n instanceof Float)
					check(4).buffer.writeFloat(n.floatValue());
				else if (n instanceof Double)
					check(8).buffer.writeDouble(n.doubleValue());
				else if (n instanceof Long)
					check(8).buffer.writeLong(n.longValue());
				else writeInt(n.intValue());
			} else if (v instanceof byte[]) {
				if (elIdx < 0) buffer.writeBytes((byte[])v);
				else {buffer.writeByteArray((byte[])v); put();}
			} else if (v instanceof int[])
				if (elIdx < 0) writeIntArray((int[])v);
				else {buffer.writeVarIntArray((int[])v); put();}
			else if (v instanceof BlockPos)
				check(8).buffer.writeBlockPos((BlockPos)v);
			else if (v instanceof Enum)
				writeInt(((Enum<?>)v).ordinal());
			else if (v == null)
				if (elIdx < 0) writeInt(-1);
				else {buffer.writeString(""); put();}
			else if (v instanceof String)
				{buffer.writeString((String)v); put();}
			else if (v instanceof UUID)
				check(16).buffer.writeUniqueId((UUID)v);
			else if (v instanceof NBTTagCompound)
				{buffer.writeCompoundTag((NBTTagCompound)v); put();}
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
				put();
			} else if (v instanceof FluidStack) {
				FluidStack stack = (FluidStack)v;
				buffer.writeString(FluidRegistry.getFluidName(stack));
				buffer.writeInt(stack.amount);
				buffer.writeCompoundTag(stack.tag);
				put();
			} else throw new IllegalArgumentException("invalid element type!");
		return this;
	}

}
