package cd4017be.lib.network;

import java.nio.ByteBuffer;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * <b>Packet format:</b><br>
 * - custom Header (optional, user defined size)<br>
 * - change table: list of object indices if only few changes, otherwise a bit-set with indices of all changed objects being set.<br>
 * - ordered list of changed fixed sized objects<br>
 * - ordered list of changed variable sized objects<br>
 * @author CD4017BE
 */
public abstract class StateSynchronizer extends SyncBitset {

	protected final Object[] objStates;
	protected final ByteBuffer rawStates;
	protected final int[] indices, objIdx;

	/**
	 * @param count
	 */
	public StateSynchronizer(int count, int[] indices, int[] objIdx, Object[] objStates, ByteBuffer rawStates) {
		super(count);
		this.indices = indices;
		this.objIdx = objIdx;
		this.objStates = objStates;
		this.rawStates = rawStates;
	}

	protected static int count(int[] sizes) {
		int l = 0;
		for (int n : sizes) l += n;
		return l;
	}

	protected static int[] accumulate(int[] sizes) {
		int[] indices = new int[sizes.length];
		for (int i = 0, l = 0; i < sizes.length; i++) {
			indices[i] = l;
			l += sizes[i];
		}
		return indices;
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
