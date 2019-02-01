package cd4017be.lib.network;

import java.util.BitSet;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * @author CD4017BE
 *
 */
public abstract class StateSynchronizer {

	protected final int[] sizes;
	public final int count;
	protected final int modSetBytes, idxBits, idxCountBits, maxIdxCount;
	public BitSet changes;

	protected StateSynchronizer(int count, int... sizes) {
		if (sizes.length > count) throw new IllegalArgumentException("can't have more fixed sized elements than total elements!");
		this.count = count;
		this.sizes = sizes;
		this.changes = new BitSet(count);
		if (count <= 8) {
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

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private IntArrayList sizes;
		private int count;

		private Builder() {}

		public Builder addVar(int count) {
			count += this.count;
			return this;
		}

		public Builder addFix(int... sizes) {
			this.sizes.addElements(this.sizes.size(), sizes);
			return this;
		}

		public Builder addMulFix(int size, int count) {
			for (; count > 0; count--)
				this.sizes.add(size);
			return this;
		}

		public int varCount() {
			return count;
		}

		public int fixCount() {
			return sizes.size();
		}

		public StateSyncServer buildSender() {
			return new StateSyncServer(count + sizes.size(), sizes.toIntArray());
		}

		public StateSyncClient buildReceiver() {
			return new StateSyncClient(count + sizes.size(), sizes.toIntArray());
		}

		public StateSynchronizer build(boolean receive) {
			return receive ? buildReceiver() : buildSender();
		}

	}

}
