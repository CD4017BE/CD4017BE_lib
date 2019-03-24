package cd4017be.lib.script.obj;

import java.util.Arrays;
import java.util.function.Function;

/**
 * 
 * @author cd4017be
 */
public class Array implements IOperand {

	public IOperand[] array;

	public Array(int cap) {
		this.array = new IOperand[cap];
	}

	public Array(IOperand... arr) {
		this.array = arr;
	}

	public <T> Array(T[] objects, Function<T, IOperand> wrapper) {
		int n = objects.length;
		this.array = new IOperand[n];
		for (int i = 0; i < n; i++)
			array[i] = wrapper.apply(objects[i]);
	}

	@Override
	public boolean asBool() {
		return array.length != 0;
	}

	@Override
	public IOperand addR(IOperand x) {
		if (x instanceof Array) {
			IOperand[] a = ((Array)x).array;
			int l = array.length, l1 = a.length;
			IOperand[] arr = Arrays.copyOf(array, l + l1);
			System.arraycopy(a, 0, arr, l, l1);
			return new Array(arr);
		} else {
			int l = array.length;
			IOperand[] arr = Arrays.copyOf(array, l + 1);
			arr[l] = x;
			return new Array(arr);
		}
	}

	@Override
	public IOperand addL(IOperand x) {
		int l = array.length;
		IOperand[] arr = new IOperand[l + 1];
		arr[0] = x;
		System.arraycopy(array, 0, arr, 1, l);
		return new Array(arr);
	}

	@Override
	public IOperand subR(IOperand x) {
		IOperand[] a = array;
		if (x instanceof Array) {
			IOperand[] b = ((Array)x).array;
			int l = a.length, n = 0;
			int[] idx = new int[l];
			for (int i = 0; i < l; i++) {
				IOperand y = a[i];
				boolean stay = true;
				for (IOperand z : b)
					if (z.equals(y)) {
						stay = false;
						break;
					}
				if (stay) idx[n++] = i;
			}
			if (n == l) return this;
			IOperand[] c = new IOperand[n];
			for (int i = 0; i < n; i++)
				c[i] = a[idx[i]];
			return new Array(c);
		} else {
			for (int l = a.length, i = 0; i < l; i++)
				if (x.equals(a[i])) {
					IOperand[] b = new IOperand[--l];
					System.arraycopy(a, 0, b, 0, i);
					System.arraycopy(a, i + 1, b, i, l - i);
					return new Array(b);
				}
			return this;
		}
	}

	//TODO set operations

	@Override
	public IOperand grR(IOperand x) {
		IOperand[] a = array;
		int l = a.length;
		for (int i = 0; i < l; i++)
			if (x.equals(a[i]))
				return new Number(i + 1);
		return Number.FALSE;
	}

	@Override
	public IOperand grL(IOperand x) {
		try {
			for (IOperand op : array)
				if (!x.grR(op).asBool())
					return Number.FALSE;
			return Number.TRUE;
		} catch (Error e) {
			return IOperand.super.grL(x);
		}
	}

	@Override
	public IOperand nlsR(IOperand x) {
		if (x instanceof Array) {
			IOperand[] a = array, b = ((Array)x).array;
			for (IOperand opB : b) {
				boolean found = false;
				for (IOperand opA : a)
					if (opB.equals(opA)) {
						found = true;
						break;
					}
				if (!found) return Number.FALSE;
			}
			return Number.TRUE;
		} else try {
			IOperand[] a = array;
			int l = a.length;
			for (int i = 0; i < l; i++)
				if (x.nlsR(a[i]).asBool())
					return new Number(i + 1);
			return Number.FALSE;
		} catch (Error e) {
			return x.nlsL(this);
		}
	}

	@Override
	public IOperand nlsL(IOperand x) {
		return grL(x);
	}

	@Override
	public IOperand len() {
		return new Number(array.length);
	}

	@Override
	public IOperand get(IOperand idx) {
		int i = idx.asIndex();
		if (i >= 0 && i < array.length)
			return array[i];
		else return Nil.NIL;
	}

	@Override
	public void put(IOperand idx, IOperand val) {
		int i = idx.asIndex();
		if (i >= 0 && i < array.length)
			array[i] = val;
	}

	@Override
	public OperandIterator iterator() {
		return new Iterator();
	}

	@Override
	public Object[] value() {
		int l = array.length;
		Object[] arr = new Object[l];
		for (int i = l - 1; i >= 0; i--)
			arr[i] = array[i].value();
		return arr;
	}

	@Override
	public String toString() {
		return Arrays.toString(array);
	}

	class Iterator implements OperandIterator {

		int idx;

		@Override
		public boolean hasNext() {
			return idx < array.length;
		}

		@Override
		public IOperand next() {
			return array[idx++];
		}

		@Override
		public void set(IOperand e) {
			array[idx - 1] = e;
		}

		@Override
		public void reset() {
			idx = 0;
		}

		@Override
		public Object value() {
			return this;
		}

	}

}
