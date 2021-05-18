package cd4017be.lib.script;

import java.nio.ByteBuffer;
import java.util.*;
import javax.script.ScriptException;

import static cd4017be.lib.script.Function.*;
import static cd4017be.lib.script.Parser.*;

/**
 * 
 * @author CD4017BE
 */
public class Compiler {

	public final String fileName;
	private ByteBuffer code, out;
	private final String[] names;
	private final char[] nameIdx;
	private final int globals;
	private final ArrayList<Function> functions = new ArrayList<>();
	private final ArrayList<Long> lines = new ArrayList<>();
	private char line, col, val;
	byte last;
	//block stack frame:
	private char locals;
	//function stack frame:
	private char locOfs, params, curStack, maxStack;
	private char[] extraVars;
	private int funStart;
	//loop stack frame:
	ArrayList<Integer> breakPos, continuePos;

	public Compiler(String fileName, Parser parser) {
		this.fileName = fileName;
		this.code = parser.getTokens();
		this.out = (ByteBuffer)code.duplicate().clear().position(code.limit());
		int l = parser.nameCount();
		this.names = new String[l];
		this.nameIdx = new char[l];
		this.globals = parser.getNames(names, nameIdx);
	}

	//compiling:

	public Script compile() throws ScriptException {
		int version = 0;
		byte c = next();
		if (c == VAR && "VERSION".equals(names[name()])) {
			check(next(), ASN);
			c = check(next(), NUM, FALSE, TRUE);
			if (c == TRUE) version = 1;
			else if (c == NUM) version = (int)number();
			check(next(), SEP_CMD);
		} else code.rewind();
		functions.add(null);
		compFunc(-1);
		functions.set(0, functions.remove(functions.size() - 1));
		Script script = new Script(fileName, functions.toArray(new Function[functions.size()]), names, globals);
		script.version = version;
		return script;
	}

	private char[] compFunc(int param) throws ScriptException {
		//store old frame
		int funStart = this.funStart;
		char line = this.line;
		char locOfs = this.locOfs;
		char params = this.params;
		char curStack = this.curStack;
		char maxStack = this.maxStack;
		char[] extraVars = this.extraVars;
		//init new frame
		this.funStart = out.position();
		if (param >= 0) {
			this.extraVars = new char[param];
			Arrays.fill(this.extraVars, (char)0xffff);
			byte t;
			while((t = next()) == LOCVAR) {
				defLocal();
				param++;
			}
			check(t, B_BLOCK);
		} else this.extraVars = new char[param = 0];
		this.params = (char)param;
		this.locOfs = (char)(locals - param);
		this.curStack = 0;
		this.maxStack = (char)param;
		//compile
		compBlock();
		addLine();
		int i = 0, l = lines.size();
		while(i < l && lines.get(i).intValue() <= this.funStart) i++;
		char[] lineNumbers = new char[l -= i];
		char[] codeIndices = new char[l];
		for (int j = 0; j < l; j++, i++) {
			long x = lines.get(i);
			codeIndices[j] = (char)((int)x - this.funStart);
			lineNumbers[j] = (char)(x >> 32);
		}
		lines.subList(lines.size() - l, lines.size()).clear();
		byte[] data = new byte[out.position() - this.funStart];
		out.position(this.funStart).mark();
		out.get(data).reset();
		functions.add(new Function(param, this.maxStack, line, data, codeIndices, lineNumbers));
		
		//restore old frame
		this.funStart = funStart;
		this.locOfs = locOfs;
		this.params = params;
		this.curStack = curStack;
		this.maxStack = maxStack;
		char[] ev = this.extraVars;
		this.extraVars = extraVars;
		return ev.length > 0 ? ev : null;
	}

	private void compBlock() throws ScriptException {
		char locals = this.locals;
		byte t = next();
		while(t != E_BLOCK && t != EOF) {
			t = compExec(t, false);
			if (curStack != 0)
				throw err("internal error: invalid stack " + curStack);
		}
		if (this.locals != locals) {
			opClear(locals - locOfs);
			this.locals = locals;
		}
	}

	private byte compExec(byte t, boolean cond) throws ScriptException {
		switch(t) {
		case SEP_CMD: break;
		case B_BLOCK:
			compBlock();
			break;
		case K_LOC:
			if (cond) err(t);
			do {
				check(next(), LOCVAR);
				byte name = defLocal();
				t = next();
				if (t == ASN) {
					curStack--;
					t = compExpr(next());
				} else opClear(name + 1);
			} while(t == SEP_PAR);
			check(t, SEP_CMD);
			break;
		case K_BR:
			breakPos.add(opJmp(go));
			check(next(), SEP_CMD);
			break;
		case K_CONT:
			continuePos.add(opJmp(go));
			check(next(), SEP_CMD);
			break;
		case K_RET:
			t = next();
			if (t == SEP_CMD) op(cst_nil, 1);
			else check(compExpr(t), SEP_CMD);
			op(ret, -1);
			break;
		case K_FOR: {
			ArrayList<Integer> continuePos = this.continuePos, breakPos = this.breakPos;
			this.continuePos = new ArrayList<>();
			this.breakPos = new ArrayList<>();
			//header
			check(next(), B_PAR);
			check(next(), LOCVAR);
			byte itvar = defLocal();
			check(next(), index);
			check(compExpr(next()), E_PAR);
			char p = pos();
			int p1 = opJmp(iterate);
			//body
			t = compExec(next(), true);
			char p2 = pos();
			for (int i : this.continuePos)
				out.putChar(i, p2);
			//end
			op(end, 0);
			out.put(itvar);
			out.putChar(p);
			locals -= 2;
			p2 = pos();
			out.putChar(p1, p2);
			if (!this.breakPos.isEmpty()) {
				op(clear, 0);
				out.put((byte)(locals - locOfs));
				for (int i : this.breakPos)
					out.putChar(i, p2);
			}
			//restore jump ptrs
			this.continuePos = continuePos;
			this.breakPos = breakPos;
		}	return t;
		case K_IF:
		case K_IFERR:
		case K_IFNOT:
		case K_IFVAL: {
			check(next(), B_PAR);
			check(compExpr(next()), E_PAR);
			//TODO optimize conditional break / continue
			int p = opJmp(
				t == K_IF ? goifn :
				t == K_IFNOT ? goif :
				t == K_IFERR ? gosucc :
				gofail
			);
			t = compExec(next(), true);
			if (t == K_ELSE) {
				int p1 = p; p = opJmp(go);
				out.putChar(p1, pos());
				t = compExec(next(), true);
			}
			out.putChar(p, pos());
			return t;
		}
		default:
			t = compExpr(t);
			if (t == FUNC) {
				if (last != gvar)
					err("Function name must be a global variable!");
				char var = out.getChar(out.position() - 2);
				out.position(out.position() - 3);
				curStack--;
				t = compOperand(t);
				op(svar, -1);
				out.putChar(var);
				return t;
			} else if (t != ASN) op(pop, -1);
			else if (last == gloc) {
				byte var = out.get(out.position() - 1);
				out.position(out.position() - 2);
				curStack--;
				t = compExpr(next());
				op(sloc, -1);
				out.put(var);
			} else if (last == gvar) {
				char var = out.getChar(out.position() - 2);
				out.position(out.position() - 3);
				curStack--;
				t = compExpr(next());
				op(svar, -1);
				out.putChar(var);
			} else if (last == access) {
				char var = out.getChar(out.position() - 2);
				out.position(out.position() - 3);
				t = compExpr(next());
				op(assign, -2);
				out.putChar(var);
			} else if (last == (index | 32)) {
				out.position(out.position() - 1);
				curStack++;
				t = compExpr(next());
				op(arr_set, -3);
			} else err("invalid assignment");
			check(t, SEP_CMD);
		}
		return next();
	}

	private byte compExpr(byte t) throws ScriptException {
		if ((t = compOperand(t)) >= 32) return t;
		byte[] ops = new byte[8];
		int size = 0;
		do {
			ops[size++] = t;
			t = compOperand(next());
			for (
				byte p = t < 32 ? PRIOR[t] : 0, c;
				size > 0 && PRIOR[c = ops[size - 1]] >= p;
				size--
			) op((byte)(c | 32), -1);
		} while(t < 32);
		return t;
	}

	private byte compOperand(byte t) throws ScriptException {
		switch(t) {
		case FUNC: {
			op(cst_func, 1);
			out.put((byte)functions.size());
			char[] param = compFunc(val);
			if (param != null) {
				//pack passed local variables
				for (char v : param) {
					op(gloc, 1);
					val = v;
					out.put(local());
				}
				op(arr_pack, -param.length);
				out.put((byte)(param.length + 1));
			}
			t = next();
		}	break;
		case LOCVAR:
			op(gloc, 1);
			out.put(local());
			t = next();
			break;
		case VAR:
			op(gvar, 1);
			out.putChar(name());
			t = next();
			break;
		case NUM: {
			double v = number();
			int iv = (int)v;
			if (v != iv) {
				op(cst_F64, 1);
				out.putDouble(v);
			} else if (iv == (byte)iv) {
				op(cst_I8, 1);
				out.put((byte)iv);
			} else if (iv == (short)iv) {
				op(cst_I16, 1);
				out.putShort((short)iv);
			} else {
				op(cst_I32, 1);
				out.putInt(iv);
			}
			t = next();
		}	break;
		case FALSE:
			op(cst_false, 1);
			t = next();
			break;
		case TRUE:
			op(cst_true, 1);
			t = next();
			break;
		case NIL:
			op(cst_nil, 1);
			t = next();
			break;
		case STR:
			op(cst_T, 1);
			out.putChar(name());
			t = next();
			break;
		case B_LIST: {
			int n = 0;
			t = next();
			if (t != E_ARR && t != E_VEC && t != E_TEXT) {
				n++;
				t = compExpr(t);
				while(t == SEP_PAR) {
					if (++n > 255) throw err("too many parameters");
					t = compExpr(next());
				}
			}
			if (t == E_VEC)
				op(vec_pack, 1 - n);
			else if (t == E_TEXT)
				op(text_pack, 1 - n);
			else {
				check(t, E_ARR);
				op(arr_pack, 1 - n);
			}
			out.put((byte)n);
			t = next();
		}	break;
		case B_PAR:
			check(compExpr(next()), E_PAR);
			t = next();
			break;
		default:
			if (t >= 32) throw err("expression", t);
			byte c = t;
			t = compOperand(next());
			op(c, 0);
		}
		while(t == ACCESS || t == B_PAR) {
			if (t == ACCESS) {
				op(access, 0);
				out.putChar(name());
			} else {
				int n = 0;
				t = next();
				if (t != E_PAR) {
					n++;
					t = compExpr(t);
					while(t == SEP_PAR) {
						if (++n > 255) throw err("too many parameters");
						t = compExpr(next());
					}
					check(t, E_PAR);
				}
				op(call, -n);
				out.put((byte)n);
			}
			t = next();
		}
		return t;
	}

	//utils:

	private byte next() {
		byte t = code.get();
		char v = code.getChar();
		switch(t) {
		case VAR:
		case LOCVAR:
		case NUM:
		case STR:
		case FUNC:
		case ACCESS:
			val = v;
			col++;
			break;
		case LINE:
			addLine();
			line = v;
			col = 0;
			return next();
		default:
			col = v;
		}
		return t;
	}

	private void addLine() {
		int p = out.position(), l = lines.size() - 1;
		if (l >= 0 && lines.get(l).intValue() == p)
			lines.remove(l);
		lines.add((long)line << 32 | p);
	}

	private void check(byte t, byte exp) throws ScriptException {
		if (t != exp)
			throw err(String.format("exp. %s , got %s", OP_NAMES[exp], OP_NAMES[t]));
	}

	private byte check(byte t, byte... exp) throws ScriptException {
		for (byte e : exp)
			if (e == t) return t;
		throw err(String.format("exp. %s , got %s", OP_NAMES[exp[0]], OP_NAMES[t]));
	}

	private ScriptException err(String exp, byte t) {
		return err(String.format("exp. %s, got %s", exp, OP_NAMES[t]));
	}
	
	private ScriptException err(byte c) {
		return err(String.format("unexpected token: %s", OP_NAMES[c]));
	}

	private ScriptException err(String msg) {
		return new ScriptException(msg, fileName, line, col);
	}

	private char name() {
		return nameIdx[val];
	}

	private double number() {
		long val = this.val;
		for (int i = 16; i < 64; i+=16) {
			next();
			val |= (long)this.val << i;
		}
		return Double.longBitsToDouble(val);
	}

	private byte defLocal() throws ScriptException {
		locals = (char)(this.val + 1);
		if (locals - locOfs > maxStack)
			maxStack = (char)(locals - locOfs);
		return local();
	}

	private byte local() throws ScriptException {
		int val = this.val - locOfs;
		if (val >= params || (val -= extraVars.length) >= 0)
			return (byte)val;
		for (int i = 0; i < extraVars.length; i++) {
			char v = extraVars[i];
			if (v == 0xffff) extraVars[i] = this.val;
			else if (v != this.val) continue;
			return (byte)i;
		}
		throw err("internal error");
	}

	private void op(byte code, int stack) {
		out.put(last = code);
		curStack += stack;
		int i = locals - locOfs + curStack;
		if (i > maxStack) maxStack = (char)i;
	}

	private void opClear(int lvl) {
		if (last == clear) out.position(out.position() - 1);
		else op(clear, 0);
		out.put((byte)lvl);
		curStack = 0;
	}

	private int opJmp(byte code) {
		op(code, code == go ? 0 : -1);
		out.putChar('\0');
		return out.position() - 2;
	}

	private char pos() {
		return (char)(out.position() - funStart);
	}

}
