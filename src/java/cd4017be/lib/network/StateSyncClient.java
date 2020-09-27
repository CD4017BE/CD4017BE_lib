package cd4017be.lib.network;

import java.io.IOException;
import java.util.BitSet;
import java.util.UUID;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * Utility class for decoding multi-element synchronization packets.
 * @author CD4017BE
 */
public class StateSyncClient extends StateSynchronizer {

	/**Contains the raw element data after decoding the change set. Use {@link #next()} or the different {@code get()} methods to read them out. */
	public PacketBuffer buffer;
	private final int fixCount;
	private int elIdx;

	/**
	 * @param count total number of elements to synchronize
	 * @param sizes the sizes in bytes for each fixed sized element
	 */
	public StateSyncClient(int count, int... sizes) {
		super(count, sizes);
		this.fixCount = sizes.length;
	}

	/**
	 * decode a synchronization packet to start reading values.<br>
	 * The result is stored in {@link #buffer}, with all fixed sized elements first, followed by the variable sized elements.
	 * @param buf packet data
	 * @return this
	 */
	public StateSyncClient decodePacket(PacketBuffer buf) {
		if ((buf.getByte(buf.readerIndex()) & 1) == 0) {
			byte[] arr = new byte[modSetBytes];
			buf.readBytes(arr);
			this.changes = BitSet.valueOf(arr);
		} else {
			int j = idxCountBits + 1, b = idxBits, mask = 0xffff >> (16 - b);
			int v = j <= 8 ? buf.readUnsignedByte() : buf.readUnsignedShortLE();
			int cc = (v & 0xffff >> (16 - j)) >> 1;
			BitSet chng = changes;
			chng.clear();
			v >>= j; j = -j & 7;
			for (int i = 0; i < cc; i++) {
				if (j < b)
					if(j < b - 8) {
						v |= buf.readUnsignedShortLE() << j;
						j += 16;
					} else {
						v |= buf.readUnsignedByte() << j;
						j += 8;
					}
				chng.set((v & mask) + 1);
				v >>= b;
				j -= b;
			}
		}
		this.buffer = buf;
		this.elIdx = 0;
		return this;
	}

	/**
	 * call before manually reading custom elements from {@link #buffer}
	 * @return whether the next element has changed
	 */
	public boolean next() {
		return changes.get(++elIdx);
	}

	/**
	 * read a 1, 2, 3 or 4 byte integer value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public int get(int old) {
		int i = ++elIdx;
		if (changes.get(i))
			switch(sizes[i - 1]) {
			case 4: return buffer.readInt();
			case 3: return buffer.readMedium();
			case 2: return buffer.readShort();
			case 1: return buffer.readByte();
			default: throw new IllegalStateException("wrong element!");
			}
		return old;
	}

	/**
	 * read a long value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public long get(long old) {
		return changes.get(++elIdx) ? buffer.readLong() : old;
	}

	/**
	 * read a float value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public float get(float old) {
		return changes.get(++elIdx) ? buffer.readFloat() : old;
	}

	/**
	 * read a double value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public double get(double old) {
		return changes.get(++elIdx) ? buffer.readDouble() : old;
	}

	/**
	 * read a byte array (fixed or variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public byte[] get(byte[] old) {
		int i = elIdx;
		if (i < fixCount) {
			int[] sizes = this.sizes;
			BitSet chng = changes;
			PacketBuffer buf = buffer;
			for (int l = old.length, j = 0; j < l; i++) {
				int n = sizes[i];
				if (chng.get(i + 1))
					buf.readBytes(old, j, n);
				j += n;
			}
			elIdx = i;
		} else if (changes.get(elIdx = i + 1))
			return buffer.readByteArray();
		return old;
	}

	/**
	 * read an integer array (fixed or variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public int[] get(int[] old) {
		int i = elIdx;
		if (i < fixCount) {
			int[] sizes = this.sizes;
			BitSet chng = changes;
			PacketBuffer buf = buffer;
			for (int l = old.length, j = 0; j < l; i++) {
				int n = sizes[i];
				if ((n & 3) != 0) throw new IllegalStateException("int array element size is " + n + " but must be multiple of 4!");
				if (chng.get(i + 1)) {
					for (n >>= 2; n > 0; n--, j++)
						old[j] = buf.readInt();
				} else j += n >> 2;
			}
			elIdx = i;
		} else if (changes.get(elIdx = i + 1))
			return buffer.readVarIntArray();
		return old;
	}

	/**
	 * read a String value (variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public String get(String old) {
		return changes.get(++elIdx) ? buffer.readString(32767) : old;
	}

	/**
	 * read an Enum value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public <V extends Enum<V>> V get(V old, Class<V> type) {
		int v0 = old == null ? -1 : old.ordinal();
		int v1 = get(v0);
		if (v1 == v0) return old;
		V[] values = type.getEnumConstants();
		return v1 >= 0 && v1 < values.length ? values[v1] : null; 
	}

	/**
	 * read a Block position (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public BlockPos get(BlockPos old) {
		return changes.get(++elIdx) ? buffer.readBlockPos() : old;
	}

	/**
	 * read an UUID value  (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public UUID get(UUID old) {
		return changes.get(++elIdx) ? buffer.readUniqueId() : old;
	}

	/**
	 * read an ItemStack (variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public ItemStack get(ItemStack old) throws IOException {
		if (changes.get(++elIdx)) {
			PacketBuffer buf = buffer;
			int id = buf.readShort();
			if (id < 0) return ItemStack.EMPTY;
			int n = buf.readInt(), m = buf.readShort();
			ItemStack stack = new ItemStack(Item.getItemById(id), n, m);
			stack.setTagCompound(buf.readCompoundTag());
			return stack;
		}
		return old;
	}

	/**
	 * read a FluidStack (variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public FluidStack get(FluidStack old) throws IOException {
		if (changes.get(++elIdx))
			return ItemFluidUtil.readFluidStack(buffer);
		return old;
	}

}
