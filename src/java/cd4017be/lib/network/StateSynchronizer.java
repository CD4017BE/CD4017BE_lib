package cd4017be.lib.network;

import java.util.Arrays;
import java.util.BitSet;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.network.PacketBuffer;

/**
 * <b>Packet format:</b><br>
 * - custom Header (optional, user defined size)<br>
 * - change table: list of object indices if only few changes, otherwise a bit-set with indices of all changed objects being set.<br>
 * - ordered list of changed fixed sized objects<br>
 * - ordered list of changed variable sized objects<br>
 * @author CD4017BE
 */
public abstract class StateSynchronizer {

	/**the byte sizes / indices of all fixed sized objects */
	protected final int[] sizes, indices;
	/**total number of objects */
	public final int count;
	/**number of bytes used to encode the modification set */
	protected final int modSetBytes;
	/**number of bits used to encode each modified index */
	protected final int idxBits;
	/**number of bits used to encode how many objects were modified */
	protected final int idxCountBits;
	/**threshold above which to encode the full set rather than individual indices */
	protected final int maxIdxCount;
	/**set of modified object indices (starts at 1 for encoding reasons) */
	public BitSet changes;

	protected StateSynchronizer(int count, int... sizes) {
		if (sizes.length > count) throw new IllegalArgumentException("can't have more fixed sized elements than total elements!");
		this.count = count;
		this.sizes = sizes;
		int l = sizes.length;
		int[] indices = new int[l];
		for (int i = 0, j = 0; i < l; i++) {
			indices[i] = j;
			j += sizes[i];
		}
		this.indices = indices;
		this.changes = new BitSet(count);
		if (count < 8) {
			this.modSetBytes = 1;
			this.maxIdxCount = 0;
			this.idxBits = 0;
			this.idxCountBits = 0;
		} else {
			int m = (this.modSetBytes = count / 8 + 1) * 8 - 9;
			int i = this.idxBits = 32 - Integer.numberOfLeadingZeros(count - 1);
			int j = 32 - Integer.numberOfLeadingZeros(m / i);
			j = (m - j) / i;
			this.idxCountBits = 32 - Integer.numberOfLeadingZeros(j);
			this.maxIdxCount = j;
		}
	}

	/**
	 * @return a builder to conveniently create instances of {@link StateSyncServer} or {@link StateSyncClient}
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private IntArrayList sizes = new IntArrayList();
		private int count;

		private Builder() {}

		/**
		 * register some variable sized objects to synchronize
		 * @param count number of objects to add
		 * @return this
		 */
		public Builder addVar(int count) {
			this.count += count;
			return this;
		}

		/**
		 * register some fixed sized objects to synchronize
		 * @param sizes individual numbers of bytes required to encode each object
		 * @return this
		 */
		public Builder addFix(int... sizes) {
			this.sizes.addElements(this.sizes.size(), sizes);
			return this;
		}

		/**
		 * register multiple fixed sized objects of the same size
		 * @param size number of bytes required to encode each object
		 * @param count number of objects to add
		 * @return
		 */
		public Builder addMulFix(int size, int count) {
			for (; count > 0; count--)
				this.sizes.add(size);
			return this;
		}

		/**
		 * @return total number of variable sized objects registered
		 */
		public int varCount() {
			return count;
		}

		/**
		 * @return total number of fixed sized objects registered
		 */
		public int fixCount() {
			return sizes.size();
		}

		/**
		 * @return created sender side instance
		 */
		public StateSyncServer buildSender() {
			return new StateSyncServer(count + sizes.size(), sizes.toIntArray());
		}

		/**
		 * @return created receiver side instance
		 */
		public StateSyncClient buildReceiver() {
			return new StateSyncClient(count + sizes.size(), sizes.toIntArray());
		}

		/**
		 * @param receive true to create a receiver, false to create a sender
		 * @return sender or receiver side instance
		 */
		public StateSynchronizer build(boolean receive) {
			return receive ? buildReceiver() : buildSender();
		}

	}

}
