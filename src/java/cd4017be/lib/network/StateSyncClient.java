package cd4017be.lib.network;

import java.io.IOException;
import java.util.BitSet;
import java.util.UUID;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * Utility class for decoding multi-element synchronization packets.
 * @author CD4017BE
 */
public class StateSyncClient extends StateSynchronizer {

	/**Contains the raw element data after decoding the change set. Use {@link #next()} or the different {@code get()} methods to read them out. */
	public FriendlyByteBuf buffer;
	private final int fixCount;
	private int elIdx;

	/**
	 * @param count total number of elements to synchronize
	 * @param sizes the sizes in bytes for each fixed sized element
	 */
	public StateSyncClient(int count, int... sizes) {
		super(count, accumulate(sizes), sizes, null, null);
		this.fixCount = sizes.length;
	}

	/**
	 * decode a synchronization packet to start reading values.<br>
	 * The result is stored in {@link #buffer}, with all fixed sized elements first, followed by the variable sized elements.
	 * @param buf packet data
	 * @return this
	 */
	public StateSyncClient decodePacket(FriendlyByteBuf buf) {
		read(buf);
		this.buffer = buf;
		this.elIdx = 0;
		return this;
	}

	/**
	 * call before manually reading custom elements from {@link #buffer}
	 * @return whether the next element has changed
	 */
	public boolean next() {
		return set.get(++elIdx);
	}

	/**
	 * read a 1, 2, 3 or 4 byte integer value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public int get(int old) {
		int i = ++elIdx;
		if (set.get(i))
			switch(objIdx[i - 1]) {
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
		return set.get(++elIdx) ? buffer.readLong() : old;
	}

	/**
	 * read a float value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public float get(float old) {
		return set.get(++elIdx) ? buffer.readFloat() : old;
	}

	/**
	 * read a double value (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public double get(double old) {
		return set.get(++elIdx) ? buffer.readDouble() : old;
	}

	/**
	 * read a byte array (fixed or variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public byte[] get(byte[] old) {
		int i = elIdx;
		if (i < fixCount) {
			int[] sizes = this.objIdx;
			BitSet chng = set;
			FriendlyByteBuf buf = buffer;
			for (int l = old.length, j = 0; j < l; i++) {
				int n = sizes[i];
				if (chng.get(i + 1))
					buf.readBytes(old, j, n);
				j += n;
			}
			elIdx = i;
		} else if (set.get(elIdx = i + 1))
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
			int[] sizes = this.objIdx;
			BitSet chng = set;
			FriendlyByteBuf buf = buffer;
			for (int l = old.length, j = 0; j < l; i++) {
				int n = sizes[i];
				if ((n & 3) != 0) throw new IllegalStateException("int array element size is " + n + " but must be multiple of 4!");
				if (chng.get(i + 1)) {
					for (n >>= 2; n > 0; n--, j++)
						old[j] = buf.readInt();
				} else j += n >> 2;
			}
			elIdx = i;
		} else if (set.get(elIdx = i + 1))
			return buffer.readVarIntArray();
		return old;
	}

	/**
	 * read a String value (variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public String get(String old) {
		return set.get(++elIdx) ? buffer.readUtf(32767) : old;
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
		return set.get(++elIdx) ? buffer.readBlockPos() : old;
	}

	/**
	 * read an UUID value  (fixed sized)
	 * @param old prev value
	 * @return new value
	 */
	public UUID get(UUID old) {
		return set.get(++elIdx) ? buffer.readUUID() : old;
	}

	/**
	 * read an ItemStack (variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public ItemStack get(ItemStack old) throws IOException {
		if (set.get(++elIdx))
			return ItemFluidUtil.readItemHighRes(buffer);
		return old;
	}

	/**
	 * read a FluidStack (variable sized)
	 * @param old prev value
	 * @return new value
	 */
	public FluidStack get(FluidStack old) throws IOException {
		if (set.get(++elIdx))
			return ItemFluidUtil.readFluidStack(buffer);
		return old;
	}

}
