package cd4017be.lib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.Level;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLLog;

@Deprecated
public abstract class ScriptCompiler {

	public static class CompileException extends Exception {
		String stacktrace;
		
		public CompileException(String message, String code, int line) {
			super(message);
			this.stacktrace = "\nl." + line + "> " + code;
		}

		@Override
		public String getMessage() {
			return super.getMessage() + stacktrace;
		}

		public static CompileException of(Throwable e, String code, int line) {
			if (e instanceof CompileException) {
				((CompileException)e).stacktrace += "\nl." + line + "> " + code;
				return (CompileException)e;
			} else if (e instanceof NumberFormatException) return new CompileException("number expected", code, line);
			else if (e instanceof ClassCastException) return new CompileException("invalid argument type: " + e.getMessage(), code, line);
			else if (e instanceof ArrayIndexOutOfBoundsException) return new CompileException("vector has wrong size: " + e.getMessage(), code, line);
			else if (e instanceof IndexOutOfBoundsException || e instanceof NullPointerException) return new CompileException("syntax error: " + e.getMessage(), code, line);
			else if (e instanceof IOException) return new CompileException("file error: " + e.getMessage(), code, line);
			else return new CompileException("unknown error: " + e.getMessage(), code, line);
		}
		
	}
	
	public static class SubMethod {
		public SubMethod(String code, ResourceLocation env) {
			this.code = code;
			this.enviroment = env;
		}
		public int line;
		public final String code;
		public final ResourceLocation enviroment;
	}
	
	public static final int maxForLoop = 256;
	public static final int defaultRecLimit = 16;
	
	public HashMap<String, Object> variables;
	
	public ScriptCompiler(HashMap<String, Object> vars) {
		this.variables = vars;
	}
	
	public void run(SubMethod method, int recLimit) throws CompileException {
		if (recLimit <= 0) throw new CompileException("recursion limit exceeded!", "code", 0);
		method.line = 0;
		int p = 0, p1;
		String left = "", right;
		try {
			while (p >= 0 && p < method.code.length()) {
				int[] k = this.find(method.code, p, ";(=#");
				if (k == null) break;
				method.line++;
				left = method.code.substring(p, k[0]).trim();
				if (k[1] == 0) {
					Object o = variables.get(left);
					if (o == null || !(o instanceof SubMethod)) throw new CompileException("method not defined:", left, method.line);
					this.run((SubMethod)o, recLimit - 1);
					p = k[0] + 1;
				}
				if (k[1] == 1) {
					int[] k1 = this.enclosing(method.code, k[0], '(', ')');
					right = method.code.substring(k1[0], k1[1]);
					if (left.equals("for")) {
						String[] def = right.split("<");
						int min = ((Double)parameter(def[0], recLimit, method.line)).intValue();
						String var = def[1].trim();
						k1 = this.enclosing(method.code, k1[1], '{', '}');
						SubMethod ncode = new SubMethod(method.code.substring(k1[0], k1[1]), new ResourceLocation("for loop @l." + method.line));
						for (int i = min; i < (Double)parameter(def[2], recLimit, method.line) && i < min + maxForLoop; i++) {
							variables.put(var, (double)i);
							this.run(ncode, recLimit - 1);
						}
						p = k1[1] + 1;
					} else if (left.equals("if")) {
						Object o = this.parameter(right, recLimit, method.line);
						k1 = this.enclosing(method.code, k1[1], '{', '}');
						boolean cond = o != null && o != Boolean.FALSE && !(o instanceof Double && (double)o == 0);
						if (cond) {
							SubMethod ncode = new SubMethod(method.code.substring(k1[0], k1[1]), new ResourceLocation("if @l." + method.line));
							this.run(ncode, recLimit - 1);
						}
						p = this.skipWhitespace(method.code, k1[1] + 1);
						if (method.code.regionMatches(p, "else", 0, 4)) {
							k1 = this.enclosing(method.code, p + 4, '{', '}');
							if (!cond) {
								SubMethod ncode = new SubMethod(method.code.substring(k1[0], k1[1]), new ResourceLocation("if else @l." + method.line));
								this.run(ncode, recLimit - 1);
							}
							p = k1[1] + 1;
						}
					} else if (left.equals("print")) {
						FMLLog.log("ScriptOUT", Level.INFO, String.valueOf(this.parameter(right, recLimit, method.line)));
						p = this.gotoSemicolon(method, k[0], k1[1]);
					} else {
						String[] arr = this.methods();
						int m = -1;
						for (int i = 0; i < arr.length; i++) 
							if (arr[i].equals(left)) {
								m = i; break;
							}
						if (m < 0) throw new CompileException("method not defined:", left, method.line);
						right = right.trim();
						if (right.isEmpty()) this.runMethod(m, new Object[0], method.line);
						else {
							ArrayList<Object> params = new ArrayList<Object>();
							for (int p2 = 0; p2 < right.length(); p2 = p1 + 1) {
								p1 = this.findSepEnd(right, p2, "\"([", "\"])", ',');
								params.add(this.parameter(right.substring(p2, p1), recLimit, method.line));
							}
							this.runMethod(m, params.toArray(), method.line);
						}
						p = this.gotoSemicolon(method, k[0], k1[1]);
					}
				} else if (k[1] == 2) {
					p1 = this.findSepEnd(method.code, k[0] + 1, "\"{", "\"}", ';');
					right = method.code.substring(k[0] + 1, p1);
					variables.put(left, this.parameter(right, recLimit, method.line));
					p = p1 + 1;
				} else {
					p = method.code.indexOf('\n', k[0]);//Comment
				}
			}
		} catch (Throwable e) {
			CompileException ex = e instanceof CompileException ? (CompileException)e : CompileException.of(e, left, method.line);
			ex.stacktrace += "\nin: " + method.enviroment.getResourcePath();
			throw ex;
		}
	}

	public int gotoSemicolon(SubMethod method, int p0, int p1) throws CompileException {
		int p = method.code.indexOf(';', p1);
		if (p < 0 || !method.code.substring(p1 + 1, p).trim().isEmpty()) throw new CompileException("missing semicolon:", method.code.substring(p0, p >= 0 ? p : p1), method.line);
		return p + 1;
	}

	public Object parameter(String s, int recLimit, int line) throws CompileException {
		try {
		s = s.trim();
		try { return Double.parseDouble(s); } catch (NumberFormatException e){};
		if (s.equals("nil")) return null;
		if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
		if (s.startsWith("[") && s.endsWith("]")) {
			ArrayList<Object> comps = new ArrayList<Object>();
			boolean num = true;
			for (int p = 1, p1; p < s.length(); p = p1 + 1) {
				p1 = this.findSepEnd(s, p, "\"([{", "\")]}", ',');
				if (p1 == s.length()) p1--;
				Object o = this.parameter(s.substring(p, p1), recLimit, line);
				if (o instanceof Object[]) {
					for (Object o1 : (Object[])o) comps.add(o1);
					num = false;
				} else if (o instanceof VecN)
					for (double o1 : ((VecN)o).x) comps.add(o1);
				else {
					comps.add(o);
					num &= o instanceof Double;
				}
			}
			if (!num) return comps.toArray();
			VecN vec = new VecN(comps.size());
			for (int i = 0; i < vec.x.length; i++) 
				vec.x[i] = (Double)comps.get(i);
			return vec;
		}
		int p = s.indexOf(':');
		int q;
		String right = null;
		if (p >= 0 && ((q = s.indexOf('(')) < 0 || p < q)) {
			right = s.substring(p + 1);
			s = s.substring(0, p);
		}
		Object obj = variables.get(s);
		if (obj != null) {
			if (right == null) return obj;
			else if (obj instanceof ScriptCompiler) return ((ScriptCompiler)obj).variables.get(right);
			else if (obj instanceof VecN) return  "n".equals(right) ? (double)((VecN)obj).x.length : ((VecN)obj).x[((Double)this.parameter(right, recLimit, line)).intValue()];
			else if (obj instanceof Object[]) return "n".equals(right) ? (double)((Object[])obj).length : ((Object[])obj)[((Double)this.parameter(right, recLimit, line)).intValue()];
			else return this.indexArray(obj, right, recLimit, line);
		}
		int[] k = this.enclosing(s, 0, '(', ')');
		right = s.substring(k[0], k[1]);
		if (s.startsWith("script")) {
			k = this.enclosing(s, k[1] + 1, '{', '}');
			if (!s.substring(k[1] + 1).trim().isEmpty()) throw new CompileException("end of method expected:", s.substring(k[1] + 1).trim(), line);
			if (right.trim().isEmpty()) {
				return new SubMethod(s.substring(k[0], k[1]), new ResourceLocation("method @l." + line));
			} else {
				HashMap<String, Object> init = new HashMap<String, Object>();
				for (String var : s.substring(k[0], k[1]).split(";")) {
					p = var.indexOf('=');
					if (p < 0) continue;
					init.put(var.substring(0, p).trim(), this.parameter(var.substring(p + 1), recLimit, line));
				}
				return this.extScript(init, (String)this.parameter(right, recLimit, line), recLimit - 1);
			}
		} else if (!s.substring(k[1] + 1).trim().isEmpty()) throw new CompileException("end of expression expected:", s.substring(k[1] + 1).trim(), line);
		String left = s.substring(0, k[0] - 1);
		ArrayList<Object> param = new ArrayList<Object>();
		p = 0;
		int p1;
		if (!right.isEmpty()) while(p < right.length()) {
			p1 = this.findSepEnd(right, p, "\"([{", "\")]}", ',');
			param.add(this.parameter(right.substring(p, p1), recLimit, line));
			p = p1 + 1;
		}
		return this.function(left, param.toArray(), line);
		} catch (Throwable e) {
			throw CompileException.of(e, s, line);
		}
	}
	
	private Object function(String name, Object[] param, int line) throws CompileException {
		Object par0 = param.length > 0 ? param[0] : null;
		switch(name) {
		case "-"://subtract or negate
			if (par0 instanceof Double) {
				if (param.length == 1) return -(Double)par0;
				else return (Double)par0 - (Double)param[1];
			} else if (par0 instanceof VecN) {
				if (param.length == 1) return ((VecN)par0).neg();
				else return ((VecN)par0).diff((VecN)param[1]);
			} else if (par0 instanceof Boolean) {
				boolean x = (Boolean)par0;
				for (int i = 1; !x && i < param.length; i++) x |= (Boolean)param[i];
				return !x;
			} else break;
		case "+"://sum
			if (par0 instanceof Double) {
				double x = (Double)par0;
				for (int i = 1; i < param.length; i++) x += (Double)param[i];
				return x;
			} else if (par0 instanceof VecN) {
				VecN x = (VecN)par0;
				for (int i = 1; i < param.length; i++) x = x.add((VecN)param[i]);
				return x;
			} else if (par0 instanceof Boolean) {
				boolean x = (Boolean)par0;
				for (int i = 1; !x && i < param.length; i++) x |= (Boolean)param[i];
				return x;
			} else break;
		case "*"://product
			if (par0 instanceof Double) {
				double x = (Double)par0;
				for (int i = 1; i < param.length; i++) x *= (Double)param[i];
				return x;
			} else if (par0 instanceof VecN) {
				VecN x = (VecN)par0;
				for (int i = 1; i < param.length; i++) x = x.scale(((VecN)param[i]).x);
				return x;
			} else if (par0 instanceof Boolean) {
				boolean x = (Boolean)par0;
				for (int i = 1; x && i < param.length; i++) x &= (Boolean)param[i];
				return x;
			} else break;
		case "/"://division
			if (par0 instanceof Double) {
				if (param.length == 1) return 1D / (Double)par0;
				else return (Double)par0 / (Double)param[1];
			} else if (par0 instanceof VecN) {
				VecN x = ((VecN)par0).copy();
				if (param.length == 1) {
					for (int i = 0; i < x.x.length; i++) x.x[i] = 1D / x.x[i];
				} else {
					VecN y = (VecN)param[1];
					for (int i = 0; i < x.x.length; i++) x.x[i] /= y.x[i];
				}
				return x;
			} else if (par0 instanceof Boolean) {
				boolean x = (Boolean)par0;
				for (int i = 1; x && i < param.length; i++) x &= (Boolean)param[i];
				return !x;
			} else break;
		case "x"://cross product for Vec3
			if (!(par0 instanceof VecN)) break; 
			VecN a = (VecN)par0;
			VecN b = (VecN)param[1];
			return new VecN(a.x[1] * b.x[2] - a.x[2] * b.x[1],
							a.x[2] * b.x[0] - a.x[0] * b.x[2],
							a.x[0] * b.x[1] - a.x[1] * b.x[0]);
		case "s"://scalar product for VecN
			if (par0 instanceof Double) return ((VecN)param[1]).scale((Double)par0);
			else if (par0 instanceof VecN)return ((VecN)par0).scale((VecN)param[1]);
			else break;
		case "n":
			if (par0 instanceof VecN) return ((VecN)par0).norm();
			else if (par0 instanceof Double) return (double)par0 != 0;
			else return par0 != null;
		case "l":
			if (par0 instanceof VecN) return ((VecN)par0).l();
			else if (par0 instanceof Double) return Math.abs((double)par0);
			else break;
		case ">":
			if (par0 instanceof Double) {
				double x = (Double)par0;
				for (int i = 1; i < param.length; i++) 
					if (param[i] instanceof Double && (Double)param[i] < x) x = (Double)param[i];
					else return false;
				return true;
			} else break;
		case "$":
			if (par0 instanceof String) return String.format((String)par0, Arrays.copyOfRange(param, 1, param.length));
			else break;
		default:
			String[] arr = this.functions();
			for (int i = 0; i < arr.length; i++) 
				if (arr[i].equals(name)) return this.runFunction(i, param, line);
		}
		String msg = "unknown function or invalid parameters (";
		for (Object o : param) msg += o == null ? "null, " : o.getClass().getName() + ", ";
		throw new CompileException(param.length > 0 ? msg.substring(0, msg.length() - 2) + ")" : msg, name, line);
	}
	
	private int[] find(String s, int p, String k) {
		int m;
		for (;p < s.length(); p++)
			if ((m = k.indexOf(s.charAt(p))) >= 0) return new int[]{p, m};
		return null;
	}
	
	private int[] enclosing(String s, int p, char cl0, char cl1) {
		int p0 = s.indexOf(cl0, p);
		if (p0 < p) return null;
		p0++;
		int n;
		for (n = 1, p = p0; n > 0 && p < s.length(); p++) {
			char c = s.charAt(p);
			if (c == cl0) n++;
			else if (c == cl1) n--;
		}
		return n > 0 ? null : new int[]{p0, p - 1};
	}
	
	private int findSepEnd(String s, int p, String cl0, String cl1, char sep) {
		int skip = -1;
		while (p < s.length()) {
			char c = s.charAt(p++);
			if (c == sep) return p - 1;
			if (cl0.indexOf(c) >= 0) {
				skip = cl1.indexOf(c);
				break;
			}
		}
		int n = 1;
		while (p < s.length()) {
			char c = s.charAt(p++);
			if (skip >= 0) {
				if (cl1.indexOf(c) == skip) {
					skip = -1;
					n--;
				}
			} else if (c == sep && n == 0) return p - 1;
			else if (cl0.indexOf(c) >= 0) {
				skip = cl1.indexOf(c);
				n++;
			} else if (cl1.indexOf(c) >= 0 && --n < 0) return p - 1;
		}
		return p;
	}
	
	private int skipWhitespace(String s, int p) {
		while(p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
		return p;
	}
	
	protected abstract String[] methods();
	protected abstract String[] functions();
	protected abstract void runMethod(int i, Object[] param, int line) throws CompileException;
	protected abstract Object runFunction(int i, Object[] param, int line) throws CompileException;
	protected abstract Object indexArray(Object array, String index, int recLimit, int line) throws CompileException;
	public abstract ScriptCompiler extScript(HashMap<String, Object> var, String filename, int reclimit) throws CompileException;
	
}
