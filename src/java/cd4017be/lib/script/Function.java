package cd4017be.lib.script;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.script.ScriptException;

import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Number;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Vector;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.Nil;
import cd4017be.lib.script.obj.Error;

/**
 * The script function interpreter.
 * @author CD4017BE
 */
public class Function implements IOperand {

	private static final int TICK_LIMIT = 262144;
	public static final byte
		gloc = 0x40, gvar = 0x41, sloc = 0x42, svar = 0x43,
		clear = 0x44, pop = 0x45, cst_nil = 0x46, cst_func = 0x47,
		cst_T = 0x48, cst_F64 = 0x49, cst_false = 0x4a, cst_true = 0x4b,
		arr_set = 0x4c, arr_pack = 0x4d, vec_pack = 0x4e, text_pack = 0x4f,
		goif = 0x50, goifn = 0x51, gosucc = 0x52, gofail = 0x53,
		go = 0x54, call = 0x55, iterate = 0x56, end = 0x57,
		ret = 0x58, access = 0x59, assign = 0x5a, cst_I8 = 0x5b, cst_I16 = 0x5c, cst_I32 = 0x5d;

	private final byte[] code;
	public final int Nparam, lineOfs, Nstack;
	private final char[] codeIndices, lineNumbers;
	public Script script;

	public Function(int param, int stack, int lineOfs, byte[] code, char[] codeIndices, char[] lineNumbers) {
		this.Nparam = param;
		this.Nstack = stack;
		this.lineOfs = lineOfs;
		this.code = code;
		this.codeIndices = codeIndices;
		this.lineNumbers = lineNumbers;
	}

	public Function(DataInputStream dis) throws IOException {
		this.Nparam = dis.readByte() & 0x7f;
		this.Nstack = dis.readByte() & 0xff;
		this.code = new byte[dis.readUnsignedShort()];
		dis.read(code);
		this.lineOfs = dis.readUnsignedShort();
		int i = dis.readUnsignedShort();
		this.lineNumbers = new char[i];
		this.codeIndices = new char[i];
		for (int j = 0; j < i; j++) {
			codeIndices[j] = dis.readChar();
			lineNumbers[j] = dis.readChar();
		}
	}

	public void call(IOperand[] stack, int bot, int top) throws ScriptException {
		if (top - bot != Nparam) throw new ScriptException("wrong number of parameters!", script.fileName, lineOfs);
		if (bot + Nstack > stack.length)
			throw new ScriptException("Stack overflow!", script.fileName, lineOfs);
		ByteBuffer code = ByteBuffer.wrap(this.code);
		int tc = TICK_LIMIT;
		try {
			while(code.hasRemaining()) {
				if (--tc < 0) throw err(new Exception(
						"ran for more than " + TICK_LIMIT + " cycles: infinite loop?"
					), code);
				IOperand a;
				int l;
				byte op = code.get();
				if ((op & 0xc0) == 0) {
					a = stack[--top];
					if ((op & 0x20) == 0) a = a.op(op);
					else a = stack[--top].opR(op & 0x1f, a);
					stack[top++] = a;
				} else switch(op) {
					case gloc:
						a = stack[bot + (code.get() & 0xff)];
						stack[top++] = a == null ? Nil.NIL : a.onCopy();
						break;
					case sloc:
						stack[bot + (code.get() & 0xff)] = stack[--top];
						break;
					case gvar:
						stack[top++] = script.globals[code.getChar()].onCopy();
						break;
					case svar:
						script.globals[code.getChar()] = stack[--top];
						break;
					case clear:
						l = bot + (code.get() & 0xff);
						if (l < top) Arrays.fill(stack, l, top, null);
						top = l;
						break;
					case pop:
						if ((a = stack[--top]) instanceof Error)
							throw (Error)a;
						break;
					case cst_nil:
						stack[top++] = Nil.NIL;
						break;
					case cst_func:
						stack[top++] = script.functions[code.get() & 0xff];
						break;
					case cst_T:
						stack[top++] = new Text(script.dictionary[code.getChar()]);
						break;
					case cst_F64:
						stack[top++] = new Number(code.getDouble());
						break;
					case cst_I32:
						stack[top++] = new Number(code.getInt());
						break;
					case cst_I16:
						stack[top++] = new Number(code.getShort());
						break;
					case cst_I8:
						stack[top++] = new Number(code.get());
						break;
					case cst_true:
						stack[top++] = Number.TRUE;
						break;
					case cst_false:
						stack[top++] = Number.FALSE;
						break;
					case arr_set:
						stack[top - 3].put(stack[top - 2], stack[top - 1]);
						top -= 3;
						break;
					case arr_pack:
						l = top; top -= code.get() & 0xff;
						stack[top] = new Array(stack, top++, l);
						break;
					case vec_pack:
						l = top; top -= code.get() & 0xff;
						stack[top] = new Vector(stack, top++, l);
						break;
					case text_pack: {
						l = top; top -= code.get() & 0xff;
						StringBuilder s = new StringBuilder();
						for (int i = top; i < l; i++) 
							s.append(stack[i].toString());
						stack[top++] = new Text(s.toString());
					}	break;
					case goif:
						l = code.getChar();
						if (stack[--top].asBool()) code.position(l);
						break;
					case goifn:
						l = code.getChar();
						if (!stack[--top].asBool()) code.position(l);
						break;
					case gosucc:
						l = code.getChar();
						if (!stack[--top].isError()) code.position(l);
						break;
					case gofail:
						l = code.getChar();
						if (stack[--top].isError()) code.position(l);
						break;
					case go:
						code.position(code.getChar());
						break;
					case call:
						l = top; top -= code.get() & 0xff;
						stack[top - 1].call(stack, top, l);
						break;
					case iterate: {
						a = stack[--top];
						OperandIterator it;
						if (a instanceof OperandIterator)
							it = (OperandIterator)a;
						else
							stack[top] = it = a.iterator();
						int p = code.getChar();
						if (it.hasNext()) {
							stack[++top] = it.next();
							top++;
						} else code.position(p);
					}	break;
					case end:
						a = stack[top = bot + (code.get() & 0xff)];
						((OperandIterator)stack[top - 1]).set(a);
						code.position(code.getChar());
						break;
					case ret:
						stack[bot - 1] = stack[top - 1];
						return;
					case access:
						stack[top - 1] = stack[top - 1].get(script.dictionary[code.getChar()]);
						break;
					case assign:
						a = stack[--top];
						stack[--top].set(script.dictionary[code.getChar()], a);
						break;
					default:
						throw new Exception(String.format("invalid opcode 0x%02X", op));
				}
			}
			stack[bot - 1] = Nil.NIL;
			Arrays.fill(stack, bot, bot + Nstack, null);
		} catch (Exception e) {
			throw err(e, code);
		}
	}

	private ScriptException err(Exception ex, ByteBuffer code) {
		int p = Arrays.binarySearch(codeIndices, (char)code.position());
		p = p == -1 ? lineOfs : lineNumbers[p < 0 ? -2 - p : p];
		String msg = ex.getMessage();
		return (ScriptException)new ScriptException(ex.getClass().getSimpleName() + (msg == null ? "" : ": " + msg), script.fileName, p).initCause(ex);
	}

	public void writeData(DataOutputStream dos) throws IOException {
		dos.writeByte(Nparam);
		dos.writeByte(Nstack);
		dos.writeShort(code.length);
		dos.write(code);
		dos.writeShort(lineOfs);
		dos.writeShort(lineNumbers.length);
		for (int i = 0; i < lineNumbers.length; i++) {
			dos.writeChar(codeIndices[i]);
			dos.writeChar(lineNumbers[i]);
		}
	}

	public int size() {
		return code.length;
	}

	@Override
	public boolean asBool() {
		return true;
	}

	@Override
	public Object value() {
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i < Nparam; i++) {
			if (i != 0) sb.append(',');
			sb.append("loc").append(i);
		}
		sb.append(") {bytes ").append(code.length).append(", stack ").append(Nstack).append('}');
		return sb.toString();
	}

}
