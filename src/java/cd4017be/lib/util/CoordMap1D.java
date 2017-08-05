package cd4017be.lib.util;

import java.util.Arrays;

/**
 * int -> Object Map for an one dimensional coordinate space from {@code -2^31} to {@code 2^31 - 1}.<br>
 * Provides lookup and storage with constant time performance similar to that of ArrayList and is best used in cases where the populated positions are all very close together.<br>
 * Large gaps between positions make this very inefficient in memory usage and iteration performance which are proportional to the distance between lowest and highest populated position.
 * @param <E> element type
 * @author CD4017BE
 */
@SuppressWarnings("unchecked")
public class CoordMap1D<E> implements Iterable<E> {

	private Object[] array;
	private int min, max, mask;

	/**
	 * creates a new empty CoordMap1D with an initial capacity of the given value rounded up to the next power of 2.
	 * @param initCapm1 capacity - 1 (capacity will be at least 1 greater than given value)
	 */
	public CoordMap1D(int initCapm1) {
		if (initCapm1 < 0) throw new IllegalArgumentException("capacity can't be negative!");
		int i = Integer.numberOfLeadingZeros(initCapm1);
		mask = 0xffffffff >>> i;
		array = new Object[1 << (32 - i)];
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
	}

	/**
	 * creates a new empty CoordMap1D with a default initial capacity of 8
	 */
	public CoordMap1D() {this(7);}

	/**
	 * sets the element for a given position.<br>
	 * If the given element is null, this call is equivalent to {@code remove(k);} 
	 * @param k position to set
	 * @param v element to store
	 */
	public E set(int k, E v) {
		if (v == null) return remove(k);
		if (k < min || k > max) {
			if (max == Integer.MIN_VALUE) min = max = k;
			else if (k > max) {
				if (k - min >= array.length) grow(k - min);
				max = k;
			} else {
				if (max - k >= array.length) grow(max - k);
				min = k;
			}
		}
		k &= mask;
		E prev = (E)array[k];
		array[k] = v;
		return prev;
	}

	/**
	 * @param k
	 * @return the element stored at given position k or null if none
	 */
	public E get(int k) {
		if (k < min || k > max) return null;
		return (E) array[k & mask];
	}

	/**
	 * remove the element at given position if it exists
	 * @param k position of the element to remove
	 * @return previously stored element or null
	 */
	public E remove(int k) {
		if (k < min || k > max) return null;
		E obj = (E) array[k & mask];
		array[k & mask] = null;
		if (k == min) {
			while (++k <= max && array[k & mask] == null);
			if (k > max) {
				min = Integer.MAX_VALUE;
				max = Integer.MIN_VALUE;
			} else min = k;
		} else if (k == max) {
			while(--k >= min && array[k & mask] == null);
			if (k < min) {
				min = Integer.MAX_VALUE;
				max = Integer.MIN_VALUE;
			} else max = k;
		}
		return obj;
	}

	/**
	 * @return range from lowest to highest populated position (inclusive) or 0 if map is empty  
	 */
	public int range() {
		return max == Integer.MIN_VALUE ? 0 : max - min + 1;
	}

	/**
	 * @return whether all positions are empty
	 */
	public boolean isEmpty() {
		return max < min;
	}

	/**
	 * @return lowest populated position or Integer.MAX_VALUE if map is empty
	 */
	public int getMin() {
		return min;
	}

	/**
	 * @return highest populated position or Integer.MIN_VALUE if map is empty
	 */
	public int getMax() {
		return max;
	}

	/**
	 * @return the element at lowest populated position or null if map is empty
	 */
	public E getFirst() {
		return min == Integer.MAX_VALUE ? null : (E) array[min & mask];
	}

	/**
	 * @return the element at highest populated position or null if map is empty
	 */
	public E getLast() {
		return max == Integer.MIN_VALUE ? null : (E) array[max & mask];
	}

	/**
	 * increase capacity to be <b>greater</b> than minSize.
	 * @param minSize
	 */
	private void grow(int minSize) {
		int l = Integer.numberOfLeadingZeros(minSize);
		Object[] narr = new Object[1 << (32 - l)];
		int p0 = min & mask, p1 = max & mask;
		int size = max - min + 1;
		mask = 0xffffffff >>> l;
		if (p1 > p0) System.arraycopy(array, p0, narr, min & mask, size);
		else {
			int m = array.length - p0;
			System.arraycopy(array, p0, narr, min & mask, m);
			System.arraycopy(array, 0, narr, (max & mask) + 1 - size + m, size - m);
		}
		array = narr;
	}

	/**
	 * @return an Iterator to iterate over all existing (= non null) elements from min to max
	 */
	@Override
	public Iterator iterator() {
		return new Iterator();
	}

	/**
	 * adds all elements present in the given map into this one. If a position is populated in both this and the given map it will be overridden by the given map.
	 * @param map mappings to add
	 */
	public void putAll(CoordMap1D<E> map) {
		int nmin = map.min < min ? map.min : min;
		int nmax = map.max > max ? map.max : max;
		if (nmax - nmin >= array.length) grow(nmax - nmin);
		if (map.max < min || map.min > max) {
			int p0 = map.min & map.mask, p1 = map.max & map.mask;
			int q0 = map.min & mask, q1 = map.max & mask;
			int size = map.range();
			if (p1 < p0) {
				int m = map.array.length - p0;
				System.arraycopy(map.array, p0, array, q0, m);
				System.arraycopy(map.array, 0, array, q1 + 1 - size + m, size - m);
			} else if (q1 < q0) {
				int m = array.length - q0;
				System.arraycopy(map.array, p0, array, q0, m);
				System.arraycopy(map.array, 0, array, q1 + 1 - size + m, size - m);
			} else {
				System.arraycopy(map.array, p0, array, q0, size);
			}
		} else {
			Object o;
			for (int i = map.min; i <= map.max; i++)
				if ((o = map.array[i & map.mask]) != null)
					array[i & mask] = o;
		}
		max = nmax;
		min = nmin;
	}

	/**
	 * copies the given range of elements into given CoordMap1D
	 * @param start first index inclusive
	 * @param end last index inclusive
	 * @param map destination
	 */
	public void copyInto(int start, int end, CoordMap1D<E> map) {
		if (start < min) start = min;
		if (end > max) end = max;
		int l = end - start;
		if (l < 0) return;
		if (l >= map.array.length) map.grow(l);
		Object o;
		for (int i = start; i <= end; i++)
			if ((o = array[i & mask]) != null)
				map.array[i & map.mask] = o;
	}

	/**
	 * removes all elements in the given range
	 * @param start first index inclusive
	 * @param end last index inclusive
	 */
	public void clear(int start, int end) {
		boolean checkStart = start <= min;
		if (checkStart) {
			start = min;
			min = end + 1;
		}
		boolean checkEnd = end >= max;
		if (checkEnd) {
			end = max;
			max = start - 1;
		}
		if (max < min) 
			{max = Integer.MIN_VALUE; min = Integer.MAX_VALUE;}
		else if (checkStart)
			while(min < max && array[min & mask] == null) min++;
		else if (checkEnd)
			while(max > min && array[max & mask] == null) max--;
		start &= mask; end &= mask;
		if (end >= start) Arrays.fill(array, start, end + 1, null);
		else {
			Arrays.fill(array, start, array.length, null);
			Arrays.fill(array, 0, end + 1, null);
		}
	}

	public class Iterator implements java.util.Iterator<E> {

		private int idx;

		private Iterator() {
			idx = min;
		}

		@Override
		public boolean hasNext() {
			return idx <= max;
		}

		@Override
		public E next() {
			Object next;
			while((next = array[idx++ & mask]) == null && idx <= max);
			return (E) next;
		}

		public int getKey() {
			return idx - 1;
		}

	}

}
