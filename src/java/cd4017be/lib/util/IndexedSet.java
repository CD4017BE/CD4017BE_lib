package cd4017be.lib.util;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Set implementation designed for objects that belong to only one certain Set of similar objects.<br><br>
 * This uses a backing array for its underlying implementation and lets the elements keep track of the index they are stored in.
 * This allows adding and removing elements with a constant time performance much faster than that of HashSets.<br>
 * However the drawback is that all elements of this Set must implement the {@link IndexedElement} interface and they can only be part of one such Set at a time.
 * 
 * @author CD4017BE
 */
public class IndexedSet<E extends IndexedSet.IndexedElement> implements Set<E> {

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

	@Override
	public boolean addAll(Collection<? extends E> c) {
		int n = count + c.size();
		if (n > array.length) array = Arrays.copyOf(array, Math.max(n, array.length << 1));
		boolean modified = false;
		for (E e : c) modified |= add(e);
		return modified;
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
	public boolean containsAll(Collection<?> c) {
		for (Object e : c)
			if (!contains(e))
				return false;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	@Override
	public Iterator iterator() {
		return new Iterator();
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
		for (int i = 0; i < count; i++)
			if (!register.get(i)) {
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

	/**
	 * Must be implemented by elements of an {@link IndexedSet} as follows:<br>
	 * {@link #getIdx()} should always return the number given to the last call of {@link #setIdx(int)} or -1 if that method hasn't been called yet.
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
		 * @return the current index of this element of -1 if not part of an {@link IndexedSet}
		 */
		int getIdx();
	}

	class Iterator implements java.util.Iterator<E> {

		int i = 0;

		@Override
		public boolean hasNext() {
			return i < count;
		}

		@Override
		public E next() {
			return array[i++];
		}

		@Override
		public void remove() {
			E e = array[--i];
			e.setIdx(-1);
			e = array[--count];
			array[count] = null;
			if (i < count) {
				array[i] = e;
				e.setIdx(i);
			}
		}

	}

}
