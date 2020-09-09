package cd4017be.lib.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Implementation of both List and Set at the same time, designed for objects that belong to only one certain Set of similar objects.<dl>
 * This uses a backing array for its underlying implementation and lets the elements keep track of the index they are stored in.
 * This allows all non bulk operations in this Set to run with a constant time performance much faster than that of HashSets.<br>
 * However the drawback is that all elements of this Set must implement the {@link IndexedElement} interface and they can only be part of one such Set at a time.
 * 
 * @author CD4017BE
 */
public class IndexedSet<E extends IndexedSet.IndexedElement> extends AbstractList<E> implements Set<E>, RandomAccess {

	protected E[] array;
	protected int count;

	public IndexedSet(E[] initArray) {
		this.array = initArray;
	}

	@Override
	public boolean add(E e) {
		if (e.getIdx() >= 0) return false;
		if (count == array.length) array = Arrays.copyOf(array, array.length << 1);
		array[count] = e;
		e.setIdx(count++);
		return true;
	}

	/**Adds all of the elements in the specified collection to this collection.
	 * <br>Note: If the specified collection is also an IndexedSet, it will be cleared during the process!
	 * @param c collection containing elements to be added to this collection
	 * @return true if this collection changed as a result of the call */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		int n = count + c.size();
		if (n > array.length) array = Arrays.copyOf(array, Math.max(n, array.length << 1));
		if (c instanceof IndexedSet) {
			IndexedSet<? extends E> is = (IndexedSet<? extends E>)c;
			for (int i = 0; i < is.count; i++) {
				E e = is.array[i];
				is.array[i] = null;
				array[count] = e;
				e.setIdx(count++);
			}
			is.count = 0;
			return true;
		} else return super.addAll(c);
	}

	@Override
	public void clear() {
		while(count > 0) {
			array[--count].setIdx(-1);
			array[count] = null;
		}
	}

	@Override
	public boolean contains(Object e) {
		if (e instanceof IndexedElement) {
			int i = ((IndexedElement)e).getIdx();
			return i >= 0 && i < count && array[i] == e;
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	@Override
	public boolean remove(Object o) {
		if (!(o instanceof IndexedElement)) return false;
		IndexedElement e = (IndexedElement)o;
		int i = e.getIdx();
		if (i < 0 || i >= count || array[i] != e) return false;
		e.setIdx(-1);
		E e_ = array[--count];
		array[count] = null;
		if (i < count) {
			array[i] = e_;
			e_.setIdx(i);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		for (Object e : c)
			modified |= remove(e);
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		BitSet register = new BitSet(count);
		for (Object o : c)
			if (o instanceof IndexedElement) {
				int i = ((IndexedElement)o).getIdx();
				if (i >= 0 && i < count && array[i] == o)
					register.set(i);
			}
		boolean modified = false;
		for (int i = register.nextClearBit(0); i < count; i = register.nextClearBit(i + 1)) {
			modified = true;
			E e = array[i];
			e.setIdx(-1);
			e = null;
			for (int j = count - 1; j > i; j--) {
				if (register.get(j)) {
					e = array[j];
					e.setIdx(i);
					array[j] = null;
					count = j;
					break;
				} else {
					array[j].setIdx(-1);
					array[j] = null;
				}
			}
			array[i] = e;
			if (e == null) count = i;
		}
		return modified;
	}

	@Override
	public int size() {
		return count;
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(array, count);
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length < count) a = Arrays.copyOf(a, count);
		System.arraycopy(array, 0, a, 0, count);
		return a;
	}

	@Override
	public void forEach(Consumer<? super E> op) {
		for (int i = 0; i < count; i++)
			op.accept(array[i]);
	}

	@Override
	public boolean removeIf(Predicate<? super E> op) {
		boolean modified = false;
		for (int i = 0; i < count; i++) {
			E e = array[i];
			if (op.test(e)) {
				e.setIdx(-1);
				e = array[--count];
				array[count] = null;
				if (i < count) {
					array[i] = e;
					e.setIdx(i);
				}
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public E get(int index) {
		return array[index];
	}

	/**
	 * Replaces the element at the specified position in this list with the specified element.<br>
	 * Note: the specified element must <b>not</b> already be member of this or any other IndexedSet!
	 */
	@Override
	public E set(int i, E e) {
		if (e.getIdx() >= 0) throw new IllegalArgumentException("Element must not be contained in an IndexedSet already!");
		E e_ = array[i];
		e_.setIdx(-1);
		array[i] = e;
		e.setIdx(i);
		return e_;
	}

	/**
	 * Inserts the specified element at the specified position in this list if it's not already contained in this set.
	 * Moves the element currently at that position (if any) to the end of this list.<br>
	 * Otherwise if the specified element is already contained in this set it will swap places with the element currently at the specified position.
	 */
	@Override
	public void add(int i, E e) {
		int j = indexOf(e);
		if (j >= 0) {
			if (j == i) return;
			E e_ = array[i];
			e_.setIdx(j);
			array[j] = e_;
			e.setIdx(i);
			array[i] = e;
		} else if (i == count) add(e);
		else add(set(i, e));
	}

	/**
	 * Removes the element at the specified position in this list.
	 * Moves the last element to the specified position.
	 * Returns the element that was removed from the list.
	 */
	@Override
	public E remove(int i) {
		E e = array[i];
		e.setIdx(-1);
		E e_ = array[--count];
		array[count] = null;
		if (i < count) {
			array[i] = e_;
			e_.setIdx(i);
		}
		return e;
	}

	@Override
	public int indexOf(Object o) {
		if (o instanceof IndexedElement) {
			int idx = ((IndexedElement)o).getIdx();
			if (idx >= 0 && idx < count && array[idx] == o)
				return idx;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		return indexOf(o);
	}

	@Override
	public Spliterator<E> spliterator() {
		return super.spliterator();
	}

	/**
	 * Must be implemented by elements of an {@link IndexedSet} exactly as it is implemented in {@link Element}.
	 * @author CD4017BE
	 */
	public interface IndexedElement {
		/**
		 * changes the index of this element
		 * @param idx new index or -1 if not part of the {@link IndexedSet} anymore
		 * @deprecated This method is intended for internal use only, do not call from your code!
		 */
		@Deprecated
		void setIdx(int idx);
		/**
		 * @return the current index of this element or -1 if not part of an {@link IndexedSet}
		 */
		int getIdx();
	}

	/**
	 * Basic implementation of {@link IndexedElement}.
	 * @author cd4017be
	 */
	public static class Element implements IndexedElement {

		private int idx = -1;

		@Override
		public void setIdx(int idx) {
			this.idx = idx;
		}

		@Override
		public int getIdx() {
			return idx;
		}

	}

}
