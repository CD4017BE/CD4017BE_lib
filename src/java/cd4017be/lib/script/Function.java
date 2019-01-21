package cd4017be.lib.script;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import javax.script.ScriptException;

import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.IOperand.OperandIterator;
import cd4017be.lib.script.obj.Number;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Vector;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.Nil;
import cd4017be.lib.script.obj.Error;
import cd4017be.lib.util.Stack;

/**
 * The script function interpreter.
 * @author CD4017BE
 */
public class Function {

	private static final int TICK_LIMIT = 262144, REC_LIMIT = 64;
	public static final byte
		gloc = 0, gvar = 1, sloc = 2, svar = 3,
		cst_N = 4, cst_T = 5, cst_true = 6, cst_false = 7, cst_nil = 8,
		go = 9, goif = 10, goifn = 11, call = 12, gosucc = 13,
		arr_get = 14, arr_set = 15, arr_l = 16, arr_pack = 17, vec_pack = 18, text_pack = 19,
		add = 20, sub = 21, mul = 22, div = 23, neg = 24, inv = 25, mod = 26,
		eq = 27, neq = 28, ls = 29, nls = 30, gr = 31, ngr = 32,
		and = 33, or = 34, nand = 35, nor = 36, xor = 37, xnor = 38,
		form = 39, clear = 40, iterate = 41, end = 42, gofail = 43;

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

	public IOperand apply(Parameters param) throws ScriptException {
		if (param.param.length != Nparam) throw new ScriptException("wrong number of parameters!", name, lineOfs);
		Stack<IOperand> stack = new Stack<IOperand>(Nstack);
		stack.fill(param.param);
		ByteBuffer code = ByteBuffer.wrap(this.code);
		int tc = TICK_LIMIT;
		try {
			while(code.hasRemaining()) {
				if (--tc < 0) throw err(new Exception("ran for more than " + TICK_LIMIT + " cycles: infinite loop?"), code);
				IOperand a, b;
				switch(code.get()) {
				case gloc:
					stack.add(stack.get(code.get()).onCopy());
					break;
				case gvar: {
					String name = getName(code, false);
					Module m = name.indexOf('.') < 0 ? script : script.context;
					stack.add(m.read(name).onCopy());
				}	break;
				case sloc:
					stack.set(code.get(), stack.rem());
					break;
				case svar: {
					String name = getName(code, false);
					a = stack.rem();
					if (name.indexOf('.') < 0) script.variables.put(name, a);
					else script.context.assign(name, a);
				}	break;
				case cst_N:
					stack.add(new Number(code.getDouble()));
					break;
				case cst_T:
					stack.add(new Text(getName(code, true)));
					break;
				case cst_true:
					stack.add(Number.TRUE);
					break;
				case cst_false:
					stack.add(Number.FALSE);
					break;
				case cst_nil:
					stack.add(Nil.NIL);
					break;
				case go:
					code.position(code.getShort() & 0xffff);
					break;
				case goif: {
					int pos = code.getShort() & 0xffff;
					if (stack.rem().asBool()) code.position(pos);
				}	break;
				case goifn: {
					int pos = code.getShort() & 0xffff;
					if (!stack.rem().asBool()) code.position(pos);
				}	break;
				case gosucc:{
					int pos = code.getShort() & 0xffff;
					if (!stack.rem().isError()) code.position(pos);
				}	break;
				case gofail: {
					int pos = code.getShort() & 0xffff;
					if (stack.rem().isError()) code.position(pos);
				}	break;
				case call: {
					Context c = script.context;
					String name = getName(code, false);
					int n = code.get();
					boolean doRet = (n & 0x80) != 0;
					try {
						if (++c.recursion > REC_LIMIT)
							throw new Exception("more than " + REC_LIMIT + " recursive function calls");
						IOperand[] param1 = new IOperand[n & 0x7f];
						stack.drain(param1);
						Module m = name.indexOf('.') < 0 ? script : c;
						a = m.invoke(name, new Parameters(param1));
						if (doRet) stack.add(a);
					} catch (Exception e) {
						if (doRet) stack.add(Error.of(err(e, code)));
						else c.LOG.error(Context.ERROR, "failed operation " + name + " in script " + script.fileName, err(e, code));
					} finally {
						c.recursion--;
					}
				}	break;
				case arr_get:
					b = stack.rem(); a = stack.rem();
					stack.add(a.get(b));
					break;
				case arr_set:
					b = stack.rem(); a = stack.rem();
					stack.rem().put(b, a);
					break;
				case arr_l:
					stack.add(stack.rem().len());
					break;
				case arr_pack: {
					Array list = new Array(code.get() & 0xff);
					stack.drain(list.array);
					stack.add(list);
				}	break;
				case vec_pack: {
					IOperand[] arr = new IOperand[code.get() & 0xff];
					stack.drain(arr);
					stack.add(new Vector(arr));
				}	break;
				case text_pack: {
					IOperand[] arr = new IOperand[code.get() & 0xff];
					stack.drain(arr);
					StringBuilder s = new StringBuilder();
					for (Object obj : arr) 
						s.append(obj.toString());
					stack.add(new Text(s.toString()));
				}	break;
				case add:
					b = stack.rem(); a = stack.rem();
					stack.add(a.addR(b));
					break;
				case sub:
					b = stack.rem(); a = stack.rem();
					stack.add(a.subR(b));
					break;
				case mul:
					b = stack.rem(); a = stack.rem();
					stack.add(a.mulR(b));
					break;
				case div:
					b = stack.rem(); a = stack.rem();
					stack.add(a.divR(b));
					break;
				case neg:
					stack.add(stack.rem().neg());
					break;
				case inv:
					stack.add(stack.rem().inv());
					break;
				case mod:
					b = stack.rem(); a = stack.rem();
					stack.add(a.modR(b));
					break;
				case eq:
					b = stack.rem(); a = stack.rem();
					stack.add(a.equals(b) ? Number.TRUE : Number.FALSE);
					break;
				case neq:
					b = stack.rem(); a = stack.rem();
					stack.add(a.equals(b) ? Number.FALSE : Number.TRUE);
					break;
				case ls:
					b = stack.rem(); a = stack.rem();
					stack.add(b.grR(a));
					break;
				case nls:
					b = stack.rem(); a = stack.rem();
					stack.add(a.nlsR(b));
					break;
				case gr:
					b = stack.rem(); a = stack.rem();
					stack.add(a.grR(b));
					break;
				case ngr:
					b = stack.rem(); a = stack.rem();
					stack.add(b.nlsR(a));
					break;
				case and:
					b = stack.rem(); a = stack.rem();
					stack.add(a.and(b));
					break;
				case or:
					b = stack.rem(); a = stack.rem();
					stack.add(a.or(b));
					break;
				case nand:
					b = stack.rem(); a = stack.rem();
					stack.add(a.nand(b));
					break;
				case nor:
					b = stack.rem(); a = stack.rem();
					stack.add(a.nor(b));
					break;
				case xor:
					b = stack.rem(); a = stack.rem();
					stack.add(a.xor(b));
					break;
				case xnor:
					b = stack.rem(); a = stack.rem();
					stack.add(a.xnor(b));
					break;
				case form:
					stack.add(new Text(getName(code, false), stack.rem()));
					break;
				case clear:
					stack.setPos(code.get());
					break;
				case iterate: {
					a = stack.get();
					OperandIterator it;
					if (a instanceof OperandIterator)
						it = (OperandIterator)a;
					else
						stack.set(it = a.iterator());
					int p = code.getShort() & 0xffff;
					if (it.hasNext())
						stack.add(it.next());
					else {
						stack.rem();
						code.position(p);
					}
				}	break;
				case end: {
					stack.setPos(code.get());
					a = stack.rem();
					((OperandIterator)stack.get()).set(a);
					code.position(code.getShort() & 0xffff);
				}	break;
				}
			}
			return hasReturn ? stack.rem() : null;
		} catch (Error e) {
			throw err(e, code);
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

	public int size() {
		return code.length;
	}

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

}
