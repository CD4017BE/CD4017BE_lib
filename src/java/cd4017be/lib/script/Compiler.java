package cd4017be.lib.script;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.script.ScriptException;

import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Number;

import static cd4017be.lib.script.Function.*;

/**
 * 
 * @author CD4017BE
 */
public class Compiler {

	private static ByteBuffer buffer = ByteBuffer.allocate(65536);
	public static void deallocate() {buffer = null;}

	public static Script compile(Context cont, String name, Reader script) throws ScriptException {
		Compiler c = new Compiler(name, parse(name, script));
		Script sc = c.compile();
		cont.add(sc);
		return sc;
	}

	private Iterator<Comp> code;
	private HashMap<String, Function> functions = new HashMap<>();
	private HashMap<String, IOperand> globals = new HashMap<>();
	public final String fileName;

	public Compiler(String fileName, List<Comp> code) {
		this.fileName = fileName;
		this.code = code.iterator();
	}

	//parsing:

	public static ArrayList<Comp> parse(String name, Reader script) throws ScriptException {
		LineTracker code = new LineTracker(script);
		try {try {
			boolean read = true;
			int c = code.next();
			while(c >= 0) {
				switch(c) {
				case ';': code.add(new Comp(Type.sep_cmd)); break;
				case ',': code.add(new Comp(Type.sep_par)); break;
				case '(': code.add(new Comp(Type.B_par)); break;
				case ')': code.add(new Comp(Type.E_par)); break;
				case '[': code.add(new Comp(Type.B_list)); break;
				case ']': code.add(new Comp(Type.E_list)); break;
				case '{': code.add(new Comp(Type.B_block)); break;
				case '}': code.add(new Comp(Type.E_block)); break;
				case '=': c = code.next();
					if (c == '=') code.add(new Comp(Type.op_eq));
					else {read = false; code.add(new Comp(Type.op_asn));}
					break;
				case '<': c = code.next();
					if (c == '=') code.add(new Comp(Type.op_ngr));
					else {read = false; code.add(new Comp(Type.op_ls));}
					break;
				case '>': c = code.next();
					if (c == '=') code.add(new Comp(Type.op_nls));
					else {read = false; code.add(new Comp(Type.op_gr));}
					break;
				case '~': c = code.next();
					if (c == '=') code.add(new Comp(Type.op_neq));
					else if (c == '&') code.add(new Comp(Type.op_nand));
					else if (c == '|') code.add(new Comp(Type.op_nor));
					else if (c == '^') code.add(new Comp(Type.op_xnor));
					else throw new ScriptException("invalid pattern: '~" + (char)c + "'", name, code.line, code.col);
					break;
				case '&': code.add(new Comp(Type.op_and)); break;
				case '|': code.add(new Comp(Type.op_or)); break;
				case '^': code.add(new Comp(Type.op_xor)); break;
				case '+': code.add(new Comp(Type.op_add)); break;
				case '-': code.add(new Comp(Type.op_sub)); break;
				case '*': code.add(new Comp(Type.op_mul)); break;
				case '/': code.add(new Comp(Type.op_div)); break;
				case '%': code.add(new Comp(Type.op_mod)); break;
				case '#': code.add(new Comp(Type.op_num)); break;
				case ':': code.add(new Comp(Type.op_ind)); break;
				case '$': code.add(new Comp(Type.op_text)); break;
				case '"': {//string
					String s = "";
					while(true) {
						c = code.next();
						if (c == '\\') {
							c = code.next();
							if (c == 'n') c = '\n';
							else if (c == 't') c = '\t';
						} else if (c == '"') break;
						if (c < 0) break;
						s += (char)c;
					}
					code.add(new ConstValue(new Text(s)));
				} break;
				case '!': //comment
					while((c = code.next()) >= 0 && c != '\n');
					break;
				default:
					if (c >= '0' && c <= '9') {//number
						String s = "" + (char)c;
						while((c = code.next()) >= 0) {
							if ((c >= '0' && c <= '9') || c == '.' || c == 'e') s += (char)c;
							else {read = false;	break;}
						}
						try {
							double x = Double.parseDouble(s);
							if (code.prev(-1).type == Type.op_div && canInv(code.prev(-2).type)) {
								code.remPrev();
								x = 1.0 / x;
							}
							if (code.prev(-1).type == Type.op_sub && canInv(code.prev(-2).type)) {
								code.remPrev();
								x = -x;
							}
							code.add(new ConstValue(new Number(x)));
						} catch(NumberFormatException e) {
							throw new ScriptException("illegal number format: " + s, name, code.line, code.col);
						}
					} else if (Character.isJavaIdentifierStart(c)) {
						String s = "" + (char)c;
						boolean comp = false;
						while((c = code.next()) >= 0) {
							if (Character.isJavaIdentifierPart(c)) s += (char)c;
							else if (c == '.') {s += (char)c; comp = true;}
							else {read = false; break;}
						}
						if ("nil".equals(s))
							code.add(new ConstValue(Nil.NIL));
						else if ("false".equals(s))
							code.add(new ConstValue(Number.FALSE));
						else if ("true".equals(s))
							code.add(new ConstValue(Number.TRUE));
						else if ("NaN".equals(s))
							code.add(new ConstValue(Number.NAN));
						else if ("fail".equals(s))
							code.add(new Comp(Type.K_fail));
						else if ("if".equals(s))
							code.add(new Comp(Type.K_if));
						else if ("else".equals(s))
							code.add(new Comp(Type.K_else));
						else if ("for".equals(s))
							code.add(new Comp(Type.K_for));
						else if ("return".equals(s))
							code.add(new Comp(Type.K_ret));
						else if ("break".equals(s))
							code.add(new Comp(Type.K_br));
						else if ("continue".equals(s))
							code.add(new Comp(Type.K_cont));
						else if ("Loc".equals(s))
							code.add(new Comp(Type.K_loc));
						else //identifier
							code.add(new Identifier(s, comp));
					} else if (!Character.isWhitespace(c))
						throw new ScriptException("unexpected character: " + (char)c, name, code.line, code.col);
				}
				if (read) c = code.next();
				else read = true;
			}
			code.reader.close();
		} catch (ScriptException e) {
			code.reader.close();
			throw e;
		}} catch (IOException e) {
			throw new ScriptException("[IO-Err] " + e.getMessage());
		}
		return code.parsed;
	}

	private static boolean canInv(Type t) {
		switch(t) {
		case val: case id: case op_num: case E_par: case E_list: return false;
		default: return true;
		}
	}

	//compiling:

	public Script compile() throws ScriptException {
		while (code.hasNext()) {
			Comp c = code.next();
			try {
				if (c.type == Type.B_block) {
					Function func = compFunc(fileName, true);
					func.script = new Script(fileName, new HashMap<String, Function>(), globals);
					func.apply(new Parameters());
					continue;
				}
				check(c, Type.id);
				if (((Identifier)c).com) throw new ScriptException("invalid identifier", fileName, c.line, c.col);
				String name = ((Identifier)c).id;
				Comp c1 = code.next();
				if (c1.type == Type.op_asn) {
					c1 = code.next();
					check(c1, Type.val);
					globals.put(name, ((ConstValue)c1).val);
					check(code.next(), Type.sep_cmd);
				} else if (c1.type == Type.B_par) {
					functions.put(name, compFunc(fileName + "." + name, false));
				} else throw new ScriptException("exp. assignment or function header", fileName, c1.line, c1.col);
			} catch(NoSuchElementException e) {
				throw new ScriptException("unexpected end of file!", fileName, c.line, c.col);
			}
		}
		Script script = new Script(fileName, functions, globals);
		Object v = globals.remove("VERSION");
		if (v != null && v instanceof Double) script.version = ((Double)v).intValue();
		return script;
	}

	private Function compFunc(String name, boolean root) throws ScriptException {
		State state = new State(name);
		if (root) state.lineOfs = 0;
		else {
			while(true) {
				Comp id = code.next();
				if (id.type == Type.E_par) break;
				check(id, Type.id);
				if (((Identifier)id).com) state.err("invalid identifier", id);
				state.regVar(((Identifier)id).id);
				Comp sep = code.next();
				if (sep.type == Type.E_par) break;
				check(sep, Type.sep_par);
			}
			Comp c = state.next(Type.B_block);
			state.lineOfs = c.line;
		}
		int param = state.lastId;
		buffer.rewind();
		boolean st = compBlock(state);
		int p = buffer.position() - (st ? 2 : 0);
		for (int i : state.returnPos)
			buffer.putShort(i, (short)p);
		byte[] data = new byte[p];
		buffer.rewind();
		buffer.get(data);
		return new Function(param, state.maxStack, state.lineOfs, data, state.hasRet, name, state.lines);
	}

	private boolean compBlock(State state) throws ScriptException {
		int lastIf = -1, lastElse = -1, variables = state.lastId;
		Comp c;
		while((c = code.next()).type != Type.E_block) {
			state.curStack = 0;
			switch(c.type) {
			case id: {
				String name = ((Identifier)c).id;
				Comp c1 = code.next();
				if (c1.type == Type.op_asn) {//assignment
					c1 = eval(state, null);
					check(c1, Type.sep_cmd);
					Byte p = state.varIds.get(name);
					if (p != null) {
						state.op(sloc, c1);
						buffer.put(p);
					} else {
						state.op(svar, c1);
						Function.putName(buffer, name, false);
					}
				} else if (c1.type == Type.op_ind) {//Array set
					Byte p = state.varIds.get(name);
					if (p != null) {
						state.op(gloc, c1);
						buffer.put(p);
					} else {
						state.op(gvar, c1);
						Function.putName(buffer, name, false);
					}
					c1 = eval(state, null);
					check(c1, Type.op_asn);
					c1 = eval(state, null);
					check(c1, Type.sep_cmd);
					state.op(arr_set, c1);
				} else if (c1.type == Type.B_par) {//Function call
					func(state, name, false);
					state.next(Type.sep_cmd);
				} else throw state.err(c1);
			} break;
			case K_loc: while(true) {//def local var
				Comp c1 = state.next(Type.id), c2 = code.next();
				if (((Identifier)c1).com) throw state.err("invalid identifier", c1);
				String name = ((Identifier)c1).id;
				if (state.varIds.containsKey(name)) throw state.err("duplicate local variable: " + name, c1);
				if (c2.type == Type.op_asn) {
					c2 = eval(state, null);
					state.curStack--;
					state.regVar(name);
				} else state.regVar(name);
				if (c2.type == Type.sep_cmd) break;
				check(c2, Type.sep_par);
			} break;
			case K_else: {
				if (lastElse >= 0 || lastIf < 0) state.err("else without if", c);
				Comp c1 = code.next();
				if (jump(state, c1, lastIf)) {
					lastIf = -1;
					break;
				}
				state.op(go, c);
				lastElse = buffer.position();
				buffer.position(lastElse + 2);
				buffer.putShort(lastIf, (short)(lastElse + 2));
				if (c1.type == Type.B_block) {
					boolean st = compBlock(state);
					buffer.putShort(lastElse, (short)(buffer.position() - (st ? 2 : 0)));
					lastElse = lastIf = -1;
					break;
				}
				check(c1, Type.K_if);
			}
			case K_fail:
			case K_if: {
				state.next(Type.B_par);
				check(eval(state, null), Type.E_par);
				Comp c1 = code.next();
				lastIf = buffer.position() + 1;
				int p;
				if (jump(state, c1, lastIf)) {
					state.op(c.type == Type.K_fail ? gofail : goif, c1);
					buffer.position(p = lastIf + 2);
					lastIf = -1;
				} else {
					state.op(c.type == Type.K_fail ? gosucc : goifn, c1);
					buffer.position(lastIf + 2);
					check(c1, Type.B_block);
					boolean st = compBlock(state);
					p = buffer.position() - (st ? 2 : 0);
					buffer.putShort(lastIf, (short)p);
				}
				if (lastElse >= 0) {
					buffer.putShort(lastElse, (short)p);
					lastElse = -1;
				}
			} break;
			case K_for: {
				//state backup
				ArrayList<Integer> breakPos = state.breakPos;
				state.breakPos = new ArrayList<Integer>();
				ArrayList<Integer> continuePos = state.continuePos;
				state.continuePos = new ArrayList<Integer>();
				//header
				state.next(Type.B_par);
				Identifier c1 = (Identifier)state.next(Type.id);
				if (c1.com) throw state.err("invalid identifier", c1);
				state.next(Type.op_ind);
				check(eval(state, null), Type.E_par);
				state.lastId++;
				state.regVar(c1.id);
				int p = buffer.position();
				state.op(iterate, state.next(Type.B_block));
				int p1 = buffer.position();
				buffer.position(p1 + 2);
				//body
				boolean st = compBlock(state);
				int p2 = buffer.position();
				if (st) buffer.position(p2 -= 2);
				for (int i : state.continuePos) buffer.putShort(i, (short)p2);
				state.op(end, null);
				buffer.put((byte)(state.lastId - 1));
				buffer.putShort((short)p);
				state.varIds.remove(c1.id);
				state.lastId -= 2;
				p2 = buffer.position();
				buffer.putShort(p1, (short)p2);
				if (!state.breakPos.isEmpty()) {
					state.op(clear, null);
					buffer.put((byte)(state.lastId - 1));
					for (int i : state.breakPos) buffer.putShort(i, (short)p2);
				}
				//reset state
				state.breakPos = breakPos;
				state.continuePos = continuePos;
			} break;
			case K_br: case K_cont: {
				state.op(go, c);
				int p = buffer.position();
				buffer.position(p + 2);
				jump(state, c, p);
			} break;
			case K_ret: {
				Comp c1 = eval(state, Type.sep_cmd);
				if (c1 != null) {
					state.hasRet = true;
					check(c1, Type.sep_cmd);
				} else c1 = c;
				state.op(go, c1);
				int p = buffer.position();
				state.returnPos.add(p);
				buffer.position(p + 2);
			} break;
			case B_block: compBlock(state); break;
			default: throw state.err(c);
			}
		}
		if (state.lastId <= variables) return false;
		for (Iterator<Entry<String, Byte>> it = state.varIds.entrySet().iterator(); it.hasNext();) {
			Entry<String, Byte> e = it.next();
			if (e.getValue() >= variables) it.remove();
		}
		int p = buffer.position();
		if (buffer.get(p - 2) == clear) buffer.position(p - 1);
		else state.op(clear, null);
		buffer.put((byte)(variables - 1));
		state.lastId = variables;
		return true;
	}

	private boolean jump(State state, Comp c, int p) throws ScriptException {
		switch(c.type) {
		case K_br:
			if (state.breakPos == null) throw state.err("break outside loop", c);
			state.breakPos.add(p); break;
		case K_cont:
			if (state.continuePos == null) throw state.err("continue outside loop", c);
			state.continuePos.add(p); break;
		case K_ret:
			state.returnPos.add(p); break;
		default: return false;
		}
		state.next(Type.sep_cmd);
		return true;
	}

	private Comp eval(State state, Type end) throws ScriptException {
		Comp c = code.next();
		if (end != null && c.type == end) return null;
		ArrayList<Comp> ops = new ArrayList<Comp>();
		while(true) {
			c = evalPre(c, state);
			int p = c.type.prior;
			if (!ops.isEmpty()) operators(ops, p, state);
			if (p == 0) return c;
			ops.add(c);
			c = code.next();
		}
	}

	@SuppressWarnings("incomplete-switch")
	private void operators(ArrayList<Comp> ops, int prior, State state) {
		int i = ops.size() - 1;
		Comp c = ops.get(i);
		while (c.type.prior >= prior) {
			switch(c.type) {
			case op_or: state.op(or, c); break;
			case op_nor: state.op(nor, c); break;
			case op_and: state.op(and, c); break;
			case op_nand: state.op(nand, c); break;
			case op_xor: state.op(xor, c); break;
			case op_xnor: state.op(xnor, c); break;
			case op_eq: state.op(eq, c); break;
			case op_neq: state.op(neq, c); break;
			case op_ls: state.op(ls, c); break;
			case op_nls: state.op(nls, c); break;
			case op_gr: state.op(gr, c); break;
			case op_ngr: state.op(ngr, c); break;
			case op_add: state.op(add, c); break;
			case op_sub: state.op(sub, c); break;
			case op_mul: state.op(mul, c); break;
			case op_div: state.op(div, c); break;
			case op_mod: state.op(mod, c); break;
			case op_ind: state.op(arr_get, c); break;
			}
			ops.remove(i--);
			if (i < 0) return;
			c = ops.get(i);
		}
	}

	private Comp evalPre(Comp c, State state) throws ScriptException {
		switch(c.type) {
		case op_sub:
			c = evalPre(code.next(), state);
			state.op(neg, null);
			return c;
		case op_div:
			c = evalPre(code.next(), state);
			state.op(inv, null);
			return c;
		case op_num:
			c = evalPre(code.next(), state);
			state.op(arr_l, null);
			return c;
		case op_text:{
			ConstValue c1 = (ConstValue)state.next(Type.val);
			if (!(c1.val instanceof Text)) state.err("exp. string literal", c1);
			c = evalPre(code.next(), state);
			state.op(form, null);
			Function.putName(buffer, c1.val.toString(), false);
			return c;
		}
		case id: {
			String name = ((Identifier)c).id;
			Comp c1 = code.next();
			if (c1.type == Type.B_par) {
				func(state, name, true);
				return code.next();
			} else {
				Byte p = state.varIds.get(name);
				if (p != null) {
					state.op(gloc, c);
					buffer.put(p);
				} else {
					state.op(gvar, c);
					Function.putName(buffer, name, false);
				}
				return c1;
			}
		}
		case val: {
			Object val = ((ConstValue)c).val;
			if (val instanceof Double) {
				state.op(cst_N, c);
				buffer.putDouble((Double)val);
			} else if (val instanceof String) {
				state.op(cst_T, c);
				Function.putName(buffer, (String)val, true);
			} else {
				state.op(val instanceof Boolean ? (Boolean)val ? cst_true : cst_false : cst_nil, c);
			}
		} return code.next();
		case B_par: {
			Comp c1 = eval(state, null);
			check(c1, Type.E_par);
		} return code.next();
		case B_list: {
			int n = params(state, Type.E_list);
			state.curStack -= n;
			Comp c1 = code.next();
			if (c1.type == Type.op_num) {
				state.op(vec_pack, null);
				buffer.put((byte)n);
				return code.next();
			} else if (c1.type == Type.op_text) {
				state.op(text_pack, null);
				buffer.put((byte)n);
				return code.next();
			} else {
				state.op(arr_pack, null);
				buffer.put((byte)n);
				return c1;
			}
		}
		default: throw state.err(c);
		}
	}

	private int params(State state, Type end) throws ScriptException {
		int n = 0;
		while(true) {
			Comp c = eval(state, end);
			if (c == null) break;
			n++;
			if (c.type == end) break;
			check(c, Type.sep_par);
		}
		return n;
	}

	private void func(State state, String name, boolean ret) throws ScriptException {
		int n = params(state, Type.E_par);
		state.curStack -= n;
		state.op(call, null);
		Function.putName(buffer, name, false);
		if (ret) {
			if (++state.curStack + state.lastId > state.maxStack) state.maxStack++;
			n |= 0x80;
		}
		buffer.put((byte)n);
	}

	//utils:

	private void check(Comp c, Type t) throws ScriptException {
		if (c.type != t) throw new ScriptException(String.format("exp. %s , got %s", t, c.type), fileName, c.line, c.col);
	}

	private static class LineTracker {
		final ArrayList<Comp> parsed = new ArrayList<Comp>();
		final Reader reader;
		short line = 1, col = 0;
		
		LineTracker(Reader reader) {this.reader = reader;}
		
		int next() throws IOException {
			int c = reader.read();
			if (c == '\n') {
				line++; col = 0;
			} else if (c == '\t') col += 4;
			else col++;
			return c;
		}
		void add(Comp c) {
			c.line = line;
			c.col = col;
			parsed.add(c);
		}
		Comp prev(int p) {return parsed.get(parsed.size() + p);}
		void remPrev() {parsed.remove(parsed.size() - 1);}
	}

	private class State {
		HashMap<String, Byte> varIds = new HashMap<String, Byte>();
		HashMap<Short, Short> lines = new HashMap<Short, Short>();
		ArrayList<Integer> breakPos, continuePos, returnPos = new ArrayList<Integer>();
		int lastId = 0, maxStack = 0, lineOfs, curStack = 0;
		boolean hasRet = false;
		final String name;
		
		State(String name) {this.name = name;}
		
		Comp next(Type t) throws ScriptException {
			Comp c = code.next();
			if (c.type != t) throw new ScriptException(String.format("exp. %s , got %s", t, c.type), name, c.line, c.col);
			return c;
		}
 		void op(byte op, Comp c) {
			buffer.put(op);
			if (c != null) lines.putIfAbsent((short)(c.line - lineOfs), (short)buffer.position());
			int i = lastId + (curStack += stack(op));
			if (i > maxStack) maxStack = i;
		}
		int regVar(String name) {
			varIds.put(name, (byte)lastId++);
			if (lastId > maxStack) maxStack = lastId;
			return lastId - 1;
		}
		ScriptException err(String msg, Comp c) {
			return new ScriptException(msg, name, c.line, c.col);
		}
		ScriptException err(Comp c) {
			return new ScriptException(String.format("unexpected token: %s", c.type), name, c.line, c.col);
		}
	}

	private static class Comp {
		Comp(Type t) {type = t;}
		Type type;
		short line, col;
	}

	private static class Identifier extends Comp {
		Identifier(String s, boolean com) {super(Type.id); this.com = com; this.id = s;}
		/**is combined (containing '.') */
		boolean com;
		/**identifier string */
		String id;
	}

	private static class ConstValue extends Comp {
		ConstValue(IOperand val) {super(Type.val); this.val = val;}
		IOperand val;
	}

	private static enum Type {
		val("literal"), id("identifier"), op_num("#"), op_ind(7, ":"), op_text("$"), op_asn("="),
		op_or(1, "|"), op_nor(1, "~|"), op_and(2, "&"), op_nand(2, "~&"), op_xor(3, "^"), op_xnor(3, "~^"),
		op_eq(4, "=="), op_neq(4, "~="), op_ls(4, "<"), op_gr(4, ">"), op_nls(4, ">="), op_ngr(4, "<="),
		op_add(5, "+"), op_sub(5, "-"), op_mul(6, "*"), op_div(6, "/"), op_mod(6, "%"),
		B_par("("), E_par(")"), B_list("["), E_list("]"), B_block("{"), E_block("}"), sep_par(","), sep_cmd(";"),
		K_fail("fail"), K_if("if"), K_else("else"), K_for("for"), K_ret("return"), K_br("break"), K_cont("continue"), K_loc("Loc");
		private Type(String text) {this(0, text);}
		private Type(int prior, String text) {this.prior = prior; this.text = text;}
		public final int prior;
		private final String text;
		@Override
		public String toString() {return text;}
	}

	private static int stack(byte op) {
		switch(op) {
		case gloc: case gvar: case cst_N: case cst_T: case cst_true: case cst_false: case cst_nil:
		case arr_pack: case vec_pack: case text_pack:
			return 1;
		case sloc: case svar: case arr_get:
		case goif: case goifn: case gosucc: case gofail: case iterate:
		case add: case sub: case mul: case div: case mod:
		case eq: case neq: case ls: case nls: case gr: case ngr:
		case and: case or: case nand: case nor: case xor: case xnor:
			return -1;
		case arr_set:
			return -3;
		default:
			return 0;
		}
	}

	/**
	 * Executable utility program for manual script compilation
	 * @param args takes as argument the combined output file path 
	 * and will compile all scripts in the same directory.
	 */
	public static void main(String[] args) {
		ScriptFiles.createCompiledPackage(new File(args[0]));
	}

}
