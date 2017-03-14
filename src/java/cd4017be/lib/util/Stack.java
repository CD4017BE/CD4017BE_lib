package cd4017be.lib.util;

import java.util.Arrays;

@SuppressWarnings("unchecked")
public class Stack<T> {

	private final Object[] arr;
	private int pos;

	public Stack(int cap) {
		arr = new Object[cap];
		pos = -1;
	}

	public int getPos() {
		return pos;
	}

	public boolean isEmpty() {
		return pos < 0;
	}

	public void setPos(int p) {
		if (p < pos) Arrays.fill(arr, p + 1, pos + 1, null);
		pos = p;
	}

	public void add(T obj) {
		arr[++pos] = obj;
	}

	public T rem() {
		T r = (T)arr[pos];
		arr[pos--] = null;
		return r;
	}

	public T get() {
		return (T)arr[pos];
	}

	public void set(T obj) {
		arr[pos] = obj;
	}

	public T get(int i) {
		return (T)arr[i < 0 ? pos + i : i];
	}

	public void set(int i, T obj) {
		arr[i < 0 ? pos + i : i] = obj;
	}

	public void fill(T[] obj) {
		System.arraycopy(obj, 0, arr, pos + 1, obj.length);
		pos += obj.length;
	}

	public void drain(T[] obj) {
		int i = (pos -= obj.length) + 1;
		System.arraycopy(arr, i, obj, 0, obj.length);
		Arrays.fill(arr, i, i + obj.length, null);
	}

	@Override
	public String toString() {
		String s = "[";
		for (int i = 0; i < arr.length; i++) {
			Object obj = arr[i];
			s += (obj == null ? "null" : obj.toString()) + (i == arr.length - 1 ? "]" : i == pos ? "| " : ", ");
		}
		return s;
	}

}
