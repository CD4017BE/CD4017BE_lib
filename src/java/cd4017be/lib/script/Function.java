package cd4017be.lib.script;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import javax.script.ScriptException;

import cd4017be.lib.util.Stack;
import static cd4017be.lib.util.ArrayMath.*;

/**
 * 
 * @author CD4017BE
 */
public class Function {

	private static final int TICK_LIMIT = 262144, REC_LIMIT = 64;

	private final byte[] code;
	public final int Nparam, lineOfs, Nstack;
	public final boolean hasReturn;
	public final String name;
	private final short[] codeIndices, lineNumbers;
	public Script script;

	public Function(int param, int stack, int lineOfs, byte[] code, boolean ret, String name, HashMap<Short, Short> lines) {
		this.Nparam = param;
		this.Nstack = stack;
		this.lineOfs = lineOfs;
		this.code = code;
		this.hasReturn = ret;
		this.name = name;
		this.lineNumbers = new short[lines.size()];
		int i = 0;
		for (short s : lines.keySet()) lineNumbers[i++] = s;
		Arrays.sort(lineNumbers);
		this.codeIndices = new short[lineNumbers.length];
		for (i = 0; i < lineNumbers.length; i++)
			codeIndices[i] = lines.get(lineNumbers[i]);
	}

	public Function(String name, DataInputStream dis) throws IOException {
		this.name = name;
		int i = dis.readByte();
		this.Nparam = i & 0x7f;
		this.hasReturn = (i & 0x80) != 0;
		this.Nstack = dis.readByte() & 0xff;
		this.code = new byte[dis.readShort() & 0xffff];
		dis.read(code);
		this.lineOfs = dis.readShort() & 0xffff;
		i = dis.readShort();
		this.lineNumbers = new short[i];
		this.codeIndices = new short[i];
		for (int j = 0; j < i; j++) {
			codeIndices[j] = dis.readShort();
			lineNumbers[j] = dis.readShort();
		}
	}

	public Object apply(Parameters param) throws ScriptException {
		if (param.param.length != Nparam) throw new ScriptException("wrong number of parameters!", name, lineOfs);
		Stack<Object> stack = new Stack<Object>(Nstack);
		stack.fill(param.param);
		ByteBuffer code = ByteBuffer.wrap(this.code);
		int n = 0;
		try {
			while(code.hasRemaining()) {
				if (++n > TICK_LIMIT) throw new Exception("ran for more than " + TICK_LIMIT + " cycles: infinite loop?");
				Operator lo = Operator.operators[code.get()];
				lo.eval(code, stack, this);
			}
			return hasReturn ? stack.rem() : null;
		} catch (ScriptException ex) {
			throw ex;
		} catch (Exception ex) {
			throw err(ex, code);
		}
	}

	private ScriptException err(Exception ex, ByteBuffer code) {
		int p = Arrays.binarySearch(codeIndices, (short)code.position());
		p = p == -1 ? lineOfs : lineOfs + lineNumbers[p < 0 ? -2 - p : p];
		String msg = ex.getMessage();
		return (ScriptException)new ScriptException(ex.getClass().getSimpleName() + (msg == null ? "" : ": " + msg), name, p).initCause(ex);
	}

	public void writeData(DataOutputStream dos) throws IOException {
		dos.writeByte(Nparam & 0x7f | (hasReturn ? 0x80 : 0x00));
		dos.writeByte(Nstack);
		dos.writeShort(code.length);
		dos.write(code);
		dos.writeShort(lineOfs);
		dos.writeShort(lineNumbers.length);
		for (int i = 0; i < lineNumbers.length; i++) {
			dos.writeShort(codeIndices[i]);
			dos.writeShort(lineNumbers[i]);
		}
	}

	public int size() {return code.length;}

	public static String getName(ByteBuffer code, boolean big) {
		int n = big ? code.getShort() & 0xffff : code.get() & 0xff;
		byte[] data = new byte[n];
		code.get(data);
		return new String(data);
	}

	public static void putName(ByteBuffer code, String name, boolean big) {
		byte[] data = name.getBytes();
		if (big) code.putShort((short)data.length);
		else code.put((byte)data.length);
		code.put(data);
	}

	public static int asInt(Object o) {
		return o instanceof Number ? ((Number)o).intValue() :
			o == null || o == Boolean.FALSE ? 0 : 1;
	}

	public static enum Operator {
		gloc(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.add(stack.get(code.get()));
			}
		}, gvar(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				String name = getName(code, false);
				Module m = name.indexOf('.') < 0 ? cont.script : cont.script.context;
				stack.add(m.read(name));
			}
		}, sloc(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.set(code.get(), stack.rem());
			}
		}, svar(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				String name = getName(code, false);
				Object obj = stack.rem();
				if (name.indexOf('.') < 0) cont.script.variables.put(name, obj);
				else cont.script.context.assign(name, obj);
			}
		}, cst_N(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.add(code.getDouble());
			}
		}, cst_T(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.add(getName(code, true));
			}
		}, cst_true(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.add(true);
			}
		}, cst_false(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.add(false);
			}
		}, cst_nil(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				stack.add(null);
			}
		}, go(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				int p = code.getShort() & 0xffff;
				code.position(p);
			}
		}, goif(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				int pos = code.getShort() & 0xffff;
				if ((Boolean)stack.rem())
					code.position(pos);
			}
		}, goifn(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				int pos = code.getShort() & 0xffff;
				if (!(Boolean)stack.rem())
					code.position(pos);
			}
		}, call(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Context c = cont.script.context;
				if (++c.recursion > REC_LIMIT) throw new Exception("more than " + REC_LIMIT + " recursive function calls");
				String name = getName(code, false);
				int n = code.get();
				Object[] param = new Object[n & 0x7f];
				stack.drain(param);
				Module m = name.indexOf('.') < 0 ? cont.script : c;
				boolean doRet = (n & 0x80) != 0;
				try {
					Object ret = m.invoke(name, new Parameters(param));
					if (doRet) stack.add(ret);
				} catch (Exception e) {
					if (doRet) stack.add(cont.err(e, code));
					else c.handleError(cont.err(e, code), cont.script, name);
				} finally {
					c.recursion--;
				}
			}
		}, call_save(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Context c = cont.script.context;
				int r = c.recursion++;
				if (r >= REC_LIMIT) throw new Exception("more than " + REC_LIMIT + " recursive function calls");
				String name = getName(code, false);
				int n = code.get();
				Object[] param = new Object[n & 0x7f];
				stack.drain(param);
				Module m = name.indexOf('.') < 0 ? cont.script : c;
				try {
					Object ret = m.invoke(name, new Parameters(param));
					if ((n & 0x80) != 0) stack.add(ret);
					stack.add(true);
				} catch (Exception e) {
					stack.add(false);
				} finally {
					c.recursion = r;
				}
			}
		}, arr_get(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object pos = stack.rem();
				Object obj = stack.rem();
				if (obj instanceof double[])
					stack.add(((double[])obj)[asInt(pos)]);
				else if (obj instanceof String) {
					Object[] ind = (Object[])pos;
					stack.add(((String)obj).substring(((Double)ind[0]).intValue(), ((Double)ind[1]).intValue()));
				} else if (obj instanceof Object[])
					stack.add(((Object[])obj)[asInt(pos)]);
				else throw new IllegalArgumentException("array expected!");
			}
		}, arr_set(-3) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object pos = stack.rem();
				Object obj = stack.rem();
				Object arr = stack.rem();
				if (arr instanceof double[])
					((double[])arr)[asInt(pos)] = (Double)obj;
				else if (arr instanceof String) {
					Object[] ind = (Object[])pos;
					int p0 = ((Double)ind[0]).intValue(), p1 = ((Double)ind[1]).intValue();
					String s0 = (String)arr, s1 = obj.toString();
					stack.add(s0.substring(0, p0).concat(s1).concat(s0.substring(p1)));
				} else if (arr instanceof Object[])
					((Object[])arr)[asInt(pos)] = obj;
				else throw new IllegalArgumentException("array expected!");
			}
		}, arr_l(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object obj = stack.rem();
				if (obj instanceof double[])
					stack.add((double)((double[])obj).length);
				else if (obj instanceof String)
					stack.add((double)((String)obj).length());
				else if (obj instanceof Object[])
					stack.add((double)((Object[])obj).length);
				else throw new IllegalArgumentException("array expected!");
			}
		}, arr_pack(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object[] arr = new Object[code.get() & 0xff];
				stack.drain(arr);
				stack.add(arr);
			}
		}, vec_pack(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object[] arr = new Object[code.get() & 0xff];
				stack.drain(arr);
				int n = 0;
				for (Object obj : arr) {
					if (obj instanceof double[]) n += ((double[])obj).length;
					else n++;
				}
				double[] vec = new double[n];
				for (int i = 0, j = 0; i < arr.length; i++) {
					Object obj = arr[i];
					if (obj instanceof double[]) {
						double[] sub = (double[])obj;
						System.arraycopy(sub, 0, vec, j, sub.length);
						j += sub.length;
					} else vec[j++] = (Double)obj;
				}
				stack.add(vec);
			}
		}, text_pack(1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object[] arr = new Object[code.get() & 0xff];
				stack.drain(arr);
				String s = "";
				for (Object obj : arr) s += obj.toString();
				stack.add(s);
			}
		}, add(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object b = stack.rem(), a = stack.rem(), c;
				if (b instanceof double[])
					if (a instanceof double[])	c = add((double[])a, (double[])b);
					else						c = ofs((double[])b, (Double)a);
				else
					if (a instanceof double[])	c = ofs((double[])a, (Double)b);
					else						c = (Double)a + (Double)b;
				stack.add(c);
			}
		}, sub(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object b = stack.rem(), a = stack.rem(), c;
				if (b instanceof double[])
					if (a instanceof double[])	c = sub((double[])a, (double[])b);
					else						c = neg((double[])b, (Double)a);
				else
					if (a instanceof double[])	c = ofs((double[])a, -(Double)b);
					else						c = (Double)a - (Double)b;
				stack.add(c);
			}
		}, mul(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object b = stack.rem(), a = stack.rem(), c;
				if (b instanceof double[])
					if (a instanceof double[])	c = mul((double[])a, (double[])b);
					else						c = sca((double[])b, (Double)a);
				else
					if (a instanceof double[])	c = sca((double[])a, (Double)b);
					else						c = (Double)a * (Double)b;
				stack.add(c);
			}
		}, div(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				Object b = stack.rem(), a = stack.rem(), c;
				if (b instanceof double[])
					if (a instanceof double[])	c = div((double[])a, (double[])b);
					else						c = inv((double[])b, (Double)a);
				else
					if (a instanceof double[])	c = sca((double[])a, 1.0 / (Double)b);
					else						c = (Double)a / (Double)b;
				stack.add(c);
			}
		}, neg(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object a = stack.rem(), c;
				if (a instanceof double[]) c = neg((double[])a, 0.0);
				else c = -(Double)a;
				stack.add(c);
			}
		}, inv(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object a = stack.rem(), c;
				if (a instanceof double[]) c = inv((double[])a, 1.0);
				else c = 1.0 / (Double)a;
				stack.add(c);
			}
		}, mod(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				double b = (Double)stack.rem(), a = (Double)stack.rem();
				stack.add(a % b);
			}
		}, eq(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object b = stack.rem(), a = stack.rem();
				stack.add(a == null ? b == null : b.equals(a));
			}
		}, neq(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object b = stack.rem(), a = stack.rem();
				stack.add(a == null ? b != null : !b.equals(a));
			}
		}, ls(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				double b = (Double)stack.rem(), a = (Double)stack.rem();
				stack.add(a < b);
			}
		}, nls(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				double b = (Double)stack.rem(), a = (Double)stack.rem();
				stack.add(a >= b);
			}
		}, gr(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				double b = (Double)stack.rem(), a = (Double)stack.rem();
				stack.add(a > b);
			}
		}, ngr(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				double b = (Double)stack.rem(), a = (Double)stack.rem();
				stack.add(a <= b);
			}
		}, and(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				boolean b = (Boolean)stack.rem(), a = (Boolean)stack.rem();
				stack.add(a & b);
			}
		}, or(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				boolean b = (Boolean)stack.rem(), a = (Boolean)stack.rem();
				stack.add(a | b);
			}
		}, nand(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				boolean b = (Boolean)stack.rem(), a = (Boolean)stack.rem();
				stack.add(!(a & b));
			}
		}, nor(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				boolean b = (Boolean)stack.rem(), a = (Boolean)stack.rem();
				stack.add(!(a | b));
			}
		}, xor(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				boolean b = (Boolean)stack.rem(), a = (Boolean)stack.rem();
				stack.add(a ^ b);
			}
		}, xnor(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) {
				boolean b = (Boolean)stack.rem(), a = (Boolean)stack.rem();
				stack.add(!a ^ b);
			}
		}, form(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				stack.add(String.format(getName(code, false), stack.rem()));
			}
		}, clear(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				stack.setPos(code.get());
			}
		}, iterate(-1) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				Object arr = stack.get();
				Iterator it;
				if (arr instanceof Iterator) it = (Iterator)arr;
				else if (arr instanceof Object[]) stack.set(it = new ArrayIterator((Object[])arr));
				else if (arr instanceof double[]) stack.set(it = new VectIterator((double[])arr));
				else stack.set(it = new NumIterator((Double)arr));
				int p = code.getShort() & 0xffff;
				if (it.next()) {
					stack.add(it.get());
				} else {
					stack.rem();
					code.position(p);
				}
			}
		}, end(0) {
			@Override
			public void eval(ByteBuffer code, Stack<Object> stack, Function cont) throws Exception {
				stack.setPos(code.get());
				Object val = stack.rem();
				Iterator it = (Iterator)stack.get();
				it.set(val);
				code.position(code.getShort() & 0xffff);
			}
		};
		public final int stack;
		private Operator(int stack) {this.stack = stack;}
		public abstract void eval(ByteBuffer code, Stack<Object> stack, Function function) throws Exception;
		public static final Operator[] operators = values();
	}

	public interface Iterator {
		public Object get();
		public void set(Object o);
		public boolean next();
		public void reset();
	}

	private static class NumIterator implements Iterator {
		public NumIterator(double max) {this.max = max; idx = -1;}
		private final double max;
		private double idx;
		@Override
		public Object get() {
			return idx;
		}
		@Override
		public void set(Object o) {
			idx = (Double)o;
		}
		@Override
		public boolean next() {
			return ++idx < max;
		}
		@Override
		public void reset() {
			idx = -1;
		}
	}

	private static class VectIterator implements Iterator {
		public VectIterator(double[] vec) {this.vec = vec; idx = -1;}
		private final double[] vec;
		private int idx;
		@Override
		public Object get() {
			return vec[idx];
		}
		@Override
		public void set(Object o) {
			vec[idx] = (Double)o;
		}
		@Override
		public boolean next() {
			return ++idx < vec.length;
		}
		@Override
		public void reset() {
			idx = -1;
		}
	}

	public static class ArrayIterator implements Iterator {
		public ArrayIterator(Object[] arr) {this.arr = arr; idx = -1;}
		protected final Object[] arr;
		protected int idx;
		@Override
		public Object get() {
			return arr[idx];
		}
		@Override
		public void set(Object o) {
			arr[idx] = o;
		}
		@Override
		public boolean next() {
			return ++idx < arr.length;
		}
		@Override
		public void reset() {
			idx = -1;
		}
	}

	public static class ListIterator<T> implements Iterator {
		public ListIterator(List<T> arr) {this.arr = arr; idx = -1;}
		protected final List<T> arr;
		protected int idx;
		@Override
		public Object get() {
			return arr.get(idx);
		}
		@SuppressWarnings("unchecked")
		@Override
		public void set(Object o) {
			arr.set(idx, (T)o);
		}
		@Override
		public boolean next() {
			return ++idx < arr.size();
		}
		@Override
		public void reset() {
			idx = -1;
		}
	}

	public static class FilteredIterator implements Iterator {
		public FilteredIterator(Iterator it, Predicate<Object> key) {this.it = it; this.key = key;}
		private final Iterator it;
		private final Predicate<Object> key;
		@Override
		public Object get() {return it.get();}
		@Override
		public void set(Object o) {it.set(o);}
		@Override
		public boolean next() {
			while(it.next())
				if (key.test(it.get()))
					return true;
			return false;
		}
		@Override
		public void reset() {it.reset();}
	}

}
