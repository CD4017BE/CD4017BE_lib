package cd4017be.lib.script;

import static cd4017be.lib.script.Function.*;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.parseDouble;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;
import java.util.Map.Entry;

import javax.script.ScriptException;

/**
 * 
 * @author CD4017BE */
public class Parser {

	ByteBuffer tokens = ByteBuffer.allocate(196608);
	CharBuffer string = tokens.asCharBuffer();
	final HashMap<String, Short> names = new HashMap<>();
	final ArrayList<String> nameList = new ArrayList<>();
	/**key: name, value: 16bit index, 16bit def block lvl, 32bit used in func lvls */
	final HashMap<String, Long> locals = new HashMap<>();
	final ArrayList<Long> funcPtrs = new ArrayList<>();
	BitSet locLvls = new BitSet(), globals = new BitSet();
	int nextLocal;
	short line, col;
	boolean newline;

	public void clear() {
		tokens.clear();
		names.clear();
		nameList.clear();
		locals.clear();
		locLvls.clear();
		globals.clear();
		funcPtrs.clear();
		nextLocal = 0;
		line = 1;
		col = 0;
		newline = true;
	}

	/**Parse a script into tokens for the compiler
	 * @param reader script source
	 * @param name script name
	 * @return result is obtained via {@link #getTokens()} and {@link #getNames()}.
	 * @throws ScriptException parsing error / invalid syntax
	 * @throws IOException io error */
	public Parser parse(Reader reader, String name) throws ScriptException, IOException {
		try (Reader script = reader) {
			clear();
			int bracketLvl = 0, blockLvl = 0;
			boolean read = true, defLoc = false, loop = false;
			int c = next(script);
			while(c >= 0) {
				switch(c) {
				case ',':
					add(SEP_PAR);
					defLoc = locLvls.get(bracketLvl);
					break;
				case '(':
					add(B_PAR);
					bracketLvl++;
					break;
				case ')':
					add(E_PAR);
					bracketLvl--;
					break;
				case '[':
					add(B_LIST);
					bracketLvl++;
					break;
				case ']':
					bracketLvl--;
					c = next(script);
					if (c == '#') add(E_VEC);
					else if (c == '$') add(E_TEXT);
					else {read = false; add(E_ARR);}
					break;
				case '{':
					bracketLvl++;
					if (prev(-1) == E_PAR)
						for(int i = tokens.position() - 6; i >= 0; i-=3) {
							byte t = tokens.get(i);
							if (t == VAR || t == SEP_PAR || t == LINE) continue;
							if (t != B_PAR) break;
							funcPtrs.add((long)blockLvl | (long)i << 32);
							tokens.put(i, FUNC);
							tokens.position(i += 3);
							//filter & compress parameters
							for (int j = i; (t = tokens.get(j)) != E_PAR; j+=3)
								if (t == VAR) {
									tokens.put(LOCVAR).putShort(tokens.getShort(j + 1));
									nextLocal++;
								} else if (t == LINE) newline = true;
							//translate parameters
							for (int j = tokens.position() - 2, l = nextLocal - 1; j > i; j-=3, l--) {
								String s = rem(tokens.getShort(j));
								if (locals.put(s, (long)(l & 0xffff | blockLvl + 1 << 16)) != null)
									throw new ScriptException("duplicate local variable: " + s, name, line, col);
								tokens.putShort(j, (short)l);
							}
							break;
						}
					add(B_BLOCK);
					if (loop) loop = false;
					else blockLvl++;
					break;
				case ';':
					add(SEP_CMD);
					locLvls.clear(bracketLvl);
					if (loop) loop = false;
					else break;
				case '}': {
					if (c == '}') {
						bracketLvl--;
						add(E_BLOCK);
					}
					final int lvl = blockLvl;
					nextLocal = 0;
					locals.values().removeIf(x -> {
						if (x.intValue() >>> 16 == lvl)
							return true;
						nextLocal = Math.max(nextLocal, x.shortValue() + 1);
						return false;
					});
					if (--blockLvl < 0)
						throw new ScriptException("excess }", name, line, col);
					if (!funcPtrs.isEmpty()) {
						int l = funcPtrs.size() - 1;
						long fp = funcPtrs.get(l);
						if ((int)fp == blockLvl) {
							long mask = -1L << l + 32;
							final int[] n = {0};
							locals.replaceAll((k, v) -> {
								if ((v & mask) == 0) return v;
								n[0]++;
								return v & ~mask;
							});
							tokens.putShort((int)(fp >> 32) + 1, (short)n[0]);
							funcPtrs.remove(l);
						}
					}
				}	break;
				case '=': c = next(script);
					if (c == '=') add(eq);
					else {read = false; add(ASN);}
					break;
				case '<': c = next(script);
					if (c == '=') add(ngr);
					else if (c == '<') add(lsh);
					else {read = false; add(ls);}
					break;
				case '>': c = next(script);
					if (c == '=') add(nls);
					else if (c == '>') add(rsh);
					else {read = false; add(gr);}
					break;
				case '~': c = next(script);
					if (c == '=') add(neq);
					else if (c == '&') add(nand);
					else if (c == '|') add(nor);
					else if (c == '~') add(xnor);
					else {read = false; add(xor);}
					break;
				case '&': add(and); break;
				case '|': add(or); break;
				case '+': add(add); break;
				case '-': add(sub); break;
				case '*': add(mul); break;
				case '/': add(div); break;
				case '%': add(mod); break;
				case '^': add(pow); break;
				case '?': add(ref); break;
				case '#': add(len); break;
				case ':': add(index); break;
				case '$': add(text); break;
				case '\'':
				case '"': {//string
					startString();
					while(true) {
						int c1 = next(script);
						if (c1 == '\\') {
							c1 = next(script);
							if (c1 == 'n') c1 = '\n';
							else if (c1 == 't') c1 = '\t';
						} else if (c1 == c) break;
						if (c1 < 0) break;
						string.append((char)c1);
					}
					add(STR, getString());
				} break;
				case '!': //comment
					while((c = next(script)) >= 0 && c != '\n');
					break;
				case '.': {
					startString();
					while((c = next(script)) >= 0) {
						if (Character.isJavaIdentifierPart(c))
							string.append((char)c);
						else {read = false; break;}
					}
					add(ACCESS, getString());
				}	break;
				default:
					if (c >= '0' && c <= '9') {//number
						startString();
						string.append((char)c);
						while((c = next(script)) >= 0) {
							if ((c >= '0' && c <= '9') || c == '.' || c == 'e')
								string.append((char)c);
							else {read = false;	break;}
						}
						String s = getString();
						try {
							double x = parseDouble(s);
							if (prev(-1) == div && canInv(prev(-2))) {
								remLast();
								x = 1.0 / x;
							}
							if (prev(-1) == sub && canInv(prev(-2))) {
								remLast();
								x = -x;
							}
							if (x != 0) add(x);
							else add(doubleToRawLongBits(x) < 0L ? TRUE : FALSE);
						} catch(NumberFormatException e) {
							throw new ScriptException("illegal number format: " + s, name, line, col);
						}
					} else if (Character.isJavaIdentifierStart(c)) {
						startString();
						string.append((char)c);
						boolean extvar = false;
						while((c = next(script)) >= 0) {
							if (Character.isJavaIdentifierPart(c))
								string.append((char)c);
							else {read = false; break;}
						}
						String s = getString();
						if (extvar) add(VAR, s);
						else if ("nil".equals(s)) add(NIL);
						else if ("false".equals(s)) add(FALSE);
						else if ("true".equals(s)) add(TRUE);
						else if ("NaN".equals(s)) add(Double.NaN);
						else if ("if".equals(s)) add(K_IF);
						else if ("ifnot".equals(s)) add(K_IFNOT);
						else if ("else".equals(s)) add(K_ELSE);
						else if ("ifval".equals(s)) add(K_IFVAL);
						else if ("iferr".equals(s)) add(K_IFERR);
						else if ("for".equals(s)) {
							loop = defLoc = true;
							nextLocal++;
							blockLvl++;
							add(K_FOR);
						} else if ("return".equals(s)) add(K_RET);
						else if ("break".equals(s)) add(K_BR);
						else if ("continue".equals(s)) add(K_CONT);
						else if ("Loc".equals(s)) {
							add(K_LOC);
							locLvls.set(bracketLvl);
							defLoc = true;
						} else if(defLoc) {
							defLoc = false;
							short l = (short)nextLocal++;
							if (locals.put(s, (long)(l & 0xffff | blockLvl << 16)) != null)
								throw new ScriptException("duplicate local variable: " + s, name, line, col);
							add(LOCVAR, l);
						} else {
							Long l = locals.get(s);
							if (l != null) {
								add(LOCVAR, l.shortValue());
								if (!funcPtrs.isEmpty())
									locals.put(s, l | 1L << 31 + funcPtrs.size());
							} else add(VAR, s); //variable name
						}
					} else if (!Character.isWhitespace(c))
						throw new ScriptException("unexpected character: " + (char)c, name, line, col);
				}
				if (read) c = next(script);
				else read = true;
				if (bracketLvl < 0)
					throw new ScriptException("excess closing bracket", name, line, col);
			}
			add(EOF);
			if (blockLvl != 0)
				throw new ScriptException("unclosed blocks", name, line, col);
			if (bracketLvl != 0)
				throw new ScriptException("unclosed brackets", name, line, col);
			return this;
		}
	}

	/**@return sequence of tokens: 1 byte id, followed by
	 * 2 bytes name table index ({@link #VAR}, {@link #EXTVAR}, {@link #STR})
	 * or 16 bit chunk of a double value (4x {@link #NUM})
	 * or source line number ({@link #LINE})
	 * or otherwise source column number */
	public ByteBuffer getTokens() {
		return tokens.flip();
	}

	public int nameCount() {
		return names.size();
	}

	/**Fill in the name tables (of length {@link #nameCount()})
	 * @param n table of variable names and string constants
	 * @param idx name index translation table
	 * @return number of global variables */
	public int getNames(String[] n, char[] idx) {
		@SuppressWarnings("unchecked")
		Entry<String, Short>[] entries = names.entrySet()
		.toArray((Entry<String, Short>[])new Entry[names.size()]);
		Arrays.sort(entries, (a, b)-> a.getKey().compareTo(b.getKey()));
		
		char iv = 0, in = (char)globals.cardinality();
		for (Entry<String, Short> e : entries) {
			int i = e.getValue() & 0xffff;
			n[(idx[i] = globals.get(i) ? iv++ : in++)] = e.getKey();
		}
		names.clear();
		return iv;
	}

	private static boolean canInv(byte t) {
		switch(t) {
		case NUM: case FALSE: case TRUE: case NIL: case STR: case VAR: case LOCVAR:
		case E_PAR: case E_ARR: case E_VEC: case E_TEXT: return false;
		default: return true;
		}
	}

	private int next(Reader reader) throws IOException {
		int c = reader.read();
		if (c == '\n') {
			line++;
			col = 0;
			newline = true;
		} else if (c == '\t') col += 4;
		else col++;
		return c;
	}

	private void startString() {
		string.clear().position(tokens.position() + 1 >> 1).mark();
	}

	private String getString() {
		return string.limit(string.position()).reset().toString();
	}

	private void add(byte t) {
		add(t, col);
	}

	private void add(double val) {
		long v = Double.doubleToLongBits(val);
		for (int i = 0; i < 4; i++, v >>>= 16)
			tokens.put(NUM).putShort((short)v);
	}

	private void add(byte t, String s) {
		int i = names.computeIfAbsent(s, k -> {
			nameList.add(k);
			return (short)(nameList.size() - 1);
		});
		add(t, (short)i);
		if (t == VAR) globals.set(i);
	}

	private String rem(short i) {
		String s = nameList.get(i);
		if (i == nameList.size() - 1)
			globals.clear(names.remove(nameList.remove(i)));
		return s;
	}

	private void add(byte t, short arg) {
		if (newline) {
			newline = false;
			tokens.put(LINE).putShort(line);
		}
		tokens.put(t).putShort(arg);
	}

	private byte prev(int p) {
		return tokens.get(p * 3 + tokens.position());
	}

	private void remLast() {
		tokens.position(tokens.position() - 3);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		ByteBuffer buf = tokens.duplicate().flip();
		while(buf.hasRemaining()) {
			byte b = buf.get();
			sb.append(OP_NAMES[b]);
			char c = buf.getChar();
			switch(b) {
			case NUM:
				buf.position(buf.position() + 9);
				break;
			case VAR: case LOCVAR: case FUNC: case STR: case ACCESS:
				sb.append((int)c);
			}
			sb.append(' ');
		}
		return sb.toString();
	}

	public static final String[] OP_NAMES = {
		"==", "~=", "~", "~~", "<", ">=", ">", "<=", "&", "~&", "|", "~|",
		"+", "-", "*", "/", "%", "^", "<<", ">>", ":", "?", "#", "$",
		null, null, null, null, null, null, null, null,
		"\n", ",", ";", "=", "(", ")", "{", "}", "[", "]", "]#", "]$",
		"var", "lvar", "nil", "function", "string", "number", "false", "true",
		"if", "ifnot", "ifval", "iferr", "else", "for", "continue", "break",
		"return", "Loc", ".x", "EOF"
	};
	public static final byte[] PRIOR = {
		4, 4, 3, 3, 4, 4, 4, 4, 2, 2, 1, 1,
		6, 6, 7, 7, 7, 8, 5, 5, 8, 1, 1, 1
	};
	public static final byte
	LINE = 0x20, SEP_PAR = 0x21, SEP_CMD = 0x22, ASN = 0x23,
	B_PAR = 0x24, E_PAR = 0x25, B_BLOCK = 0x26, E_BLOCK = 0x27,
	B_LIST = 0x28, E_ARR = 0x29, E_VEC = 0x2a, E_TEXT = 0x2b,
	VAR = 0x2c, LOCVAR = 0x2d, NIL = 0x2e, FUNC = 0x2f,
	STR = 0x30, NUM = 0x31, FALSE = 0x32, TRUE = 0x33,
	K_IF = 0x34, K_IFNOT = 0x35, K_IFVAL = 0x36, K_IFERR = 0x37,
	K_ELSE = 0x38, K_FOR = 0x39, K_CONT = 0x3a, K_BR = 0x3b,
	K_RET = 0x3c, K_LOC = 0x3d, ACCESS = 0x3e, EOF = 0x3f;

}
