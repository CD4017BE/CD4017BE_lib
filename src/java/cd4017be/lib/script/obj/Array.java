package cd4017be.lib.script.obj;

import java.util.Arrays;
import java.util.function.Function;

import javax.script.ScriptException;

/**
 * 
 * @author cd4017be
 */
public class Array implements IOperand {

	public IOperand[] array;

	public Array(int cap) {
		this.array = new IOperand[cap];
	}

	public Array(IOperand[] arr, int from, int to) {
		this(to -= from);
		System.arraycopy(arr, from, array, 0, to);
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
	public IOperand op(int code) {
		switch(code) {
		case len:
			return new Number(array.length);
		case text:
			return new Text(toString());
		default:
			return IOperand.super.op(code);
		}
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		IOperand[] a = array;
		int l = a.length;
		switch(code) {
		case add:
			if (x instanceof Array) {
				IOperand[] b = ((Array)x).array;
				int l1 = b.length;
				IOperand[] arr = Arrays.copyOf(a, l + l1);
				System.arraycopy(b, 0, arr, l, l1);
				return new Array(arr);
			} else {
				IOperand[] arr = Arrays.copyOf(a, l + 1);
				arr[l] = x;
				return new Array(arr);
			}
		case sub:
			if (x instanceof Array) {
				IOperand[] b = ((Array)x).array;
				int n = 0;
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
				for (int i = 0; i < l; i++)
					if (x.equals(a[i])) {
						IOperand[] b = new IOperand[--l];
						System.arraycopy(a, 0, b, 0, i);
						System.arraycopy(a, i + 1, b, i, l - i);
						return new Array(b);
					}
				return this;
			}
		case index: {
			int i = x.asIndex();
			return i >= 0 && i < l ? a[i] : Nil.NIL;
		}
		case gr:
			for (int i = 0; i < l; i++)
				if (x.equals(a[i]))
					return new Number(i + 1);
			return Number.FALSE;
		case nls:
			if (x instanceof Array) {
				IOperand[] b = ((Array)x).array;
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
			} else {
				for (int i = 0; i < l; i++)
					if (x.opR(code, a[i]).asBool())
						return new Number(i + 1);
				return Number.FALSE;
			}
		default:
			return IOperand.super.opR(code, x);
		}
	}

	@Override
	public IOperand opL(int code, IOperand x) {
		switch(code) {
		case add: {
			int l = array.length;
			IOperand[] arr = new IOperand[l + 1];
			arr[0] = x;
			System.arraycopy(array, 0, arr, 1, l);
			return new Array(arr);
		}
		case nls:
		case gr:
			for (IOperand op : array)
				if (!x.opR(code, op).asBool())
					return Number.FALSE;
			return Number.TRUE;
		default:
			return IOperand.super.opL(code, x);
		}
	}

	//TODO set operations

	@Override
	public void put(IOperand idx, IOperand val) {
		int i = idx.asIndex();
		if (i >= 0 && i < array.length)
			array[i] = val;
	}

	@Override
	public void call(IOperand[] stack, int bot, int top) throws ScriptException {
		int l = array.length - 1;
		if (l < 0) {
			stack[bot - 1] = Nil.NIL;
			Arrays.fill(stack, bot, top, null);
		} else {
			if (top + l > stack.length)
				throw new ScriptException("Stack overflow: Array call [" + l + "]");
			System.arraycopy(array, 1, stack, top, l);
			array[0].call(stack, bot, top + l);
		}
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
