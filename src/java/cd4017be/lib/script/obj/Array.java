package cd4017be.lib.script.obj;

import java.util.Arrays;

/**
 * 
 * @author cd4017be
 */
public class Array implements IOperand {

	public IOperand[] array;

	public Array(int cap) {
		this.array = new IOperand[cap];
	}

	public Array(IOperand[] arr) {
		this.array = arr;
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

	//TODO set operations

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
