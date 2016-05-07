package cd4017be.lib.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import cd4017be.lib.util.Vec2;
import cd4017be.lib.util.VecN;

public class TESRModelParser {

	public static class CompileException extends Exception {
		String stacktrace;
		
		CompileException(String message, String code, int line) {
			super(message);
			this.stacktrace = "\nl." + line + "> " + code;
		}

		@Override
		public String getMessage() {
			return super.getMessage() + stacktrace;
		}

		static CompileException of(Throwable e, String code, int line) {
			if (e instanceof CompileException) {
				((CompileException)e).stacktrace += "\nl." + line + "> " + code;
				return (CompileException)e;
			} else if (e instanceof NumberFormatException) return new CompileException("number expected", code, line);
			else if (e instanceof ClassCastException) return new CompileException("invalid argument type", code, line);
			else if (e instanceof ArrayIndexOutOfBoundsException) return new CompileException("vector has wrong size", code, line);
			else if (e instanceof IndexOutOfBoundsException || e instanceof NullPointerException) return new CompileException("syntax error", code, line);
			else if (e instanceof IOException) return new CompileException("file error: " + e.getMessage(), code, line);
			else return new CompileException("unknown error: " + e.getMessage(), code, line);
		}
		
	}
	
	private static class State {
		Matrix4d matrix;
		Vec2 uvOffset;
		Vec2 uvScale;
		VecN color;
		
		State copy(){
			State state = new State();
			state.matrix = new Matrix4d(matrix);
			state.uvOffset = uvOffset.copy();
			state.uvScale = uvScale.copy();
			state.color = color.copy();
			return state;
		}
	}
	
	private static class Quad {
		//x, y, z, u, v, r, g, b, a
		VecN[] vertices = new VecN[4];
		
		Quad transform(State state) {
			Quad quad = new Quad();
			for (int i = 0; i < 4; i++) {
				Point3d vec = new Point3d(vertices[i].x[0], vertices[i].x[1], vertices[i].x[2]);
				Vec2 uv = state.uvScale.scale(vertices[i].x[3], vertices[i].x[4]).add(state.uvOffset);
				VecN c = state.color.scale(vertices[i].x[5], vertices[i].x[6], vertices[i].x[7], vertices[i].x[8]);
				state.matrix.transform(vec);
				quad.vertices[i] = new VecN(vec.x, vec.y, vec.z, uv.x, uv.z, c.x[0], c.x[1], c.x[2], c.x[3]);
			}
			return quad;
		}
	}
	
	private static final VecN defaultVertex = new VecN(0, 0, 0, 0, 0, 1, 1, 1, 1);
	private static final String defaultFormat = "xyzuvrgba";
	private static final int maxStates = 8;
	
	ArrayList<State> states = new ArrayList<State>();
	ArrayList<Quad> quads = new ArrayList<Quad>();
	HashMap<String, Object> variables = new HashMap<String, Object>();
	int line;
	
	private TESRModelParser(String code, HashMap<String, Object> variables, State init, ResourceLocation res) throws CompileException {
		this.variables = variables;
		this.states = new ArrayList<State>();
		this.states.add(init);
		line = 0;
		try {
			this.parse(code);
		} catch (CompileException e) {
			e.stacktrace += "\nin file: " + res.toString();
			throw e;
		}
		
	}
	
	private void parse(String code) throws CompileException {
		int p = 0, p1;
		String left = "", right;
		try {
			while (p >= 0 && p < code.length()) {
				int[] k = this.find(code, p, ";(=#");
				if (k == null) break;
				line++;
				left = code.substring(p, k[0]).trim();
				if (k[1] == 1) {
					int[] k1 = this.enclosing(code, k[0], '(', ')');
					right = code.substring(k1[0], k1[1]);
					if (left.equals("for")) {
						String[] def = right.split("<");
						int min = ((Double)parameter(def[0])).intValue(), max = ((Double)parameter(def[2])).intValue();
						String var = def[1].trim();
						k1 = this.enclosing(code, k1[1], '{', '}');
						String ncode = code.substring(k1[0], k1[1]);
						for (int i = min; i < max; i++) {
							variables.put(var, (double)i);
							this.parse(ncode);
						}
						p = k1[1] + 1;
						break;
					}
					if (left.equals("push")) {
						if (states.size() >= maxStates) throw new CompileException("max state depth reached", left, line);
						states.add(states.get(states.size() - 1).copy());
					} else if (left.equals("pop")) {
						if (states.size() <= 1) throw new CompileException("already at origin state", left, line);
						states.remove(states.size() - 1);
					} else if (left.equals("rotate")) {
						VecN vec = (VecN)this.parameter(right);
						State state = states.get(states.size() - 1);
						vec.x[3] = Math.toRadians(vec.x[3]);
						Matrix4d mat = new Matrix4d();
						mat.set(new AxisAngle4d(vec.x));
						state.matrix.mul(mat);
					} else if (left.equals("translate")) {
						VecN vec = (VecN)this.parameter(right);
						State state = states.get(states.size() - 1);
						Matrix4d mat = new Matrix4d();
						mat.set(new Vector3d(vec.x));
						state.matrix.mul(mat);
					} else if (left.equals("scale")) {
						VecN vec = (VecN)this.parameter(right);
						State state = states.get(states.size() - 1);
						Matrix4d mat = new Matrix4d();
						mat.setM00(vec.x[0]);
						mat.setM11(vec.x[1]);
						mat.setM22(vec.x[2]);
						state.matrix.mul(mat);
					} else if (left.equals("offsetUV")) {
						VecN vec = (VecN)this.parameter(right);
						State state = states.get(states.size() - 1);
						state.uvOffset = state.uvOffset.add(vec.x[0] * state.uvScale.x, vec.x[1] * state.uvScale.z);
					} else if (left.equals("scaleUV")) {
						VecN vec = (VecN)this.parameter(right);
						State state = states.get(states.size() - 1);
						state.uvScale = state.uvScale.scale(vec.x[0], vec.x[1]);
					} else if (left.equals("color")) {
						VecN vec = (VecN)this.parameter(right);
						State state = states.get(states.size() - 1);
						for (int i = 0; i < vec.x.length; i++) state.color.x[i] = vec.x[i];
					} else if (left.equals("draw")) {
						Object o = this.parameter(right);
						State state = states.get(states.size() - 1);
						if (o instanceof Quad) quads.add(((Quad)o).transform(state));
						else if (o instanceof TESRModelParser)
							for (Quad quad : ((TESRModelParser)o).quads) quads.add(quad.transform(state));
					}
					p = code.indexOf(';', k1[1]) + 1;
				} else if (k[1] == 2) {
					p1 = this.findSepEnd(code, k[0] + 1, "{", "}", ';');
					right = code.substring(k[0] + 1, p1);
					variables.put(left, this.parameter(right));
					p = p1 + 1;
				} else {
					p = code.indexOf('\n', k[0]);//Comment
				}
			}
		} catch (Throwable e) {
			if (e instanceof CompileException) throw (CompileException)e;
			else throw CompileException.of(e, left, line);
		}
	}
	
	private Object parameter(String s) throws CompileException {
		try {
		s = s.trim();
		try { return Double.parseDouble(s); } catch (NumberFormatException e){};
		if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
		if (s.startsWith("[") && s.endsWith("]")) {
			VecN vec = new VecN();
			int p = 1, p1;
			while (p < s.length()) {
				p1 = this.findSepEnd(s, p, "([{", ")]}", ',');
				if (p1 == s.length()) p1--;
				Object obj = this.parameter(s.substring(p, p1));
				if (obj instanceof Double) vec = new VecN(vec, (Double)obj);
				else vec = new VecN(vec, ((VecN)obj).x);
				p = p1 + 1;
			}
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
			else if (obj instanceof VecN) return ((VecN)obj).x[Integer.parseInt(right)];
			else if (obj instanceof TESRModelParser) return ((TESRModelParser)obj).variables.get(right);
		}
		int[] k = this.enclosing(s, 0, '(', ')');
		right = s.substring(k[0], k[1]);
		if (s.startsWith("model")) {
			k = this.enclosing(s, k[1] + 1, '{', '}');
			State state = new State();
			state.matrix = new Matrix4d();
			state.matrix.setIdentity();
			state.uvOffset = Vec2.Def(0, 0);
			state.uvScale = Vec2.Def(1, 1);
			state.color = new VecN(1, 1, 1, 1);
			TESRModelParser model;
			if (right.trim().isEmpty()) {
				model = new TESRModelParser(s.substring(k[0], k[1]), variables, state, new ResourceLocation("internal function line " + line));
			} else {
				ResourceLocation res = new ResourceLocation((String)this.parameter(right) + ".tesr");
				HashMap<String, Object> init = new HashMap<String, Object>();
				for (String var : s.substring(k[0], k[1]).split(";")) {
					p = var.indexOf('=');
					if (p < 0) continue;
					init.put(var.substring(0, p).trim(), this.parameter(var.substring(p + 1)));
				}
				model = new TESRModelParser(SpecialModelLoader.instance.loadTESRModelSourceCode(res), init, state, res);
			}
			model.states.clear();
			return model;
		}
		String left = s.substring(0, k[0] - 1);
		ArrayList<Object> param = new ArrayList<Object>();
		p = 0;
		int p1;
		if (!right.isEmpty()) while(p < right.length()) {
			p1 = this.findSepEnd(right, p, "([{", ")]}", ',');
			param.add(this.parameter(right.substring(p, p1)));
			p = p1 + 1;
		}
		return this.function(left, param.toArray());
		} catch (Throwable e) {
			throw CompileException.of(e, s, line);
		}
	}
	
	private static final boolean[][] rect = {{false, false, true, true}, {false, true, true, false}};
	
	private Object function(String name, Object[] param) throws CompileException {
		switch(name) {
		case "quad": {//create quad
			Quad quad = new Quad();
			String format = (String)param[4];
			if (format == null) format = defaultFormat;
			for (int i = 0; i < 4; i++) {
				VecN vec = (VecN)param[i];
				quad.vertices[i] = defaultVertex.copy();
				for (int j = 0, n = 0; j < format.length(); j++) {
					int p = defaultFormat.indexOf(format.charAt(j));
					if (p >= 0) quad.vertices[i].x[p] = vec.x[n++];
				}
			}
			return quad;
		} case "rect": {//create rectangle quad
			Quad quad = new Quad();
			VecN pos = (VecN)param[0], tex = (VecN)param[1];
			String format = (String)param[2];
			int t;
			int tN = (t = format.indexOf('-')) < 0 ? format.indexOf('+') + 3 : t;
			boolean inv = tN >= 3; tN %= 3;
			int tU = (t = format.indexOf('u')) < 0 ? format.indexOf('U') + 3 : t;
			int tV = (t = format.indexOf('v')) < 0 ? format.indexOf('V') + 3 : t;
			for (int i = 0; i < 4; i++) {
				VecN vert = defaultVertex.copy();
				vert.x[0] = pos.x[(tN == 0 ? inv : rect[tN - 1][i]) ? 3 : 0];
				vert.x[1] = pos.x[(tN == 1 ? inv : rect[(tN + 1) % 3][i]) ? 4 : 1];
				vert.x[2] = pos.x[(tN == 2 ? inv : rect[tN][i]) ? 5 : 2];
				vert.x[3] = tex.x[(tU >= 3 ^ rect[(5 - tU + tN) % 3][i]) ? 2 : 0];
				vert.x[4] = tex.x[(tV >= 3 ^ rect[(5 - tV + tN) % 3][i]) ? 3 : 1];
				quad.vertices[inv ? i : 3 - i] = vert;
			}
			return quad;
		} case "-"://subtract or negate
			if (param[0] instanceof Double) {
				if (param.length == 1) return -(Double)param[0];
				else return (Double)param[0] - (Double)param[1];
			} else if (param[0] instanceof VecN) {
				if (param.length == 1) return ((VecN)param[0]).neg();
				else return ((VecN)param[0]).diff((VecN)param[1]);
			} else break;
		case "+"://sum
			if (param[0] instanceof Double) {
				double x = (Double)param[0];
				for (int i = 1; i < param.length; i++) x += (Double)param[i];
				return x;
			} else if (param[0] instanceof VecN) {
				VecN x = (VecN)param[0];
				for (int i = 1; i < param.length; i++) x = x.add((VecN)param[i]);
				return x;
			} else break;
		case "*"://product
			if (param[0] instanceof Double) {
				double x = (Double)param[0];
				for (int i = 1; i < param.length; i++) x *= (Double)param[i];
				return x;
			} else if (param[0] instanceof VecN) {
				VecN x = (VecN)param[0];
				for (int i = 1; i < param.length; i++) x = x.scale(((VecN)param[i]).x);
				return x;
			} else break;
		case "/"://division
			if (param[0] instanceof Double) {
				if (param.length == 1) return 1D / (Double)param[0];
				else return (Double)param[0] / (Double)param[1];
			} else if (param[0] instanceof VecN) {
				VecN x = ((VecN)param[0]).copy();
				if (param.length == 1) {
					for (int i = 0; i < x.x.length; i++) x.x[i] = 1D / x.x[i];
				} else {
					VecN y = (VecN)param[1];
					for (int i = 0; i < x.x.length; i++) x.x[i] /= y.x[i];
				}
				return x;
			} else break;
		case "x"://cross product for Vec3
			VecN a = (VecN)param[0];
			VecN b = (VecN)param[1];
			return new VecN(a.x[1] * b.x[2] - a.x[2] * b.x[1],
							a.x[2] * b.x[0] - a.x[0] * b.x[2],
							a.x[0] * b.x[1] - a.x[1] * b.x[0]);
		case "s"://scalar product for VecN
			if (param[0] instanceof Double) return ((VecN)param[1]).scale((Double)param[0]);
			else return ((VecN)param[0]).scale((VecN)param[1]);
		case "n":
			return ((VecN)param[0]).norm();
		case "l":
			return ((VecN)param[0]).l();
		}
		throw new CompileException("unknown method or invalid parameters (" + param.length + ")", name, line);
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
		while (p < s.length()) {
			char c = s.charAt(p++);
			if (c == sep) return p - 1;
			if (cl0.indexOf(c) >= 0) break;
		}
		int n = 1;
		while (p < s.length()) {
			char c = s.charAt(p++);
			if (c == sep && n == 0) return p - 1;
			if (cl0.indexOf(c) >= 0) n++;
			else if (cl1.indexOf(c) >= 0 && --n < 0) return p - 1;
		}
		return p;
	}
	
	public static int[] bake(String code, ResourceLocation res) throws CompileException {
		State state = new State();
		state.matrix = new Matrix4d();
		state.matrix.setIdentity();
		state.uvOffset = Vec2.Def(0, 0);
		state.uvScale = Vec2.Def(1, 1);
		state.color = new VecN(1, 1, 1, 1);
		HashMap<String, Object> variables = new HashMap<String, Object>();
		TESRModelParser model = new TESRModelParser(code, variables, state, res);
		int[] data = new int[28 * model.quads.size()];
		for (int i = 0; i < model.quads.size(); i++) {
			Quad quad = model.quads.get(i);
			for (int j = 0; j < 4; j++) {
				int p = i * 28 + j * 7;
				data[p] = Float.floatToIntBits((float)quad.vertices[j].x[0]);
				data[p + 1] = Float.floatToIntBits((float)quad.vertices[j].x[1]);
				data[p + 2] = Float.floatToIntBits((float)quad.vertices[j].x[2]);
				int r = MathHelper.clamp_int((int)(quad.vertices[j].x[5] * 255F), 0, 255);
				int g = MathHelper.clamp_int((int)(quad.vertices[j].x[6] * 255F), 0, 255);
				int b = MathHelper.clamp_int((int)(quad.vertices[j].x[7] * 255F), 0, 255);
				int a = MathHelper.clamp_int((int)(quad.vertices[j].x[8] * 255F), 0, 255);
				data[p + 3] = a << 24 | r << 16 | g << 8 | b;
				data[p + 4] = Float.floatToIntBits((float)quad.vertices[j].x[3]);
				data[p + 5] = Float.floatToIntBits((float)quad.vertices[j].x[4]);
			}
		}
		return data;
	}
	
	public static void renderWithOffsetAndBrightness(VertexBuffer render, String model, float dx, float dy, float dz, int l) {
		int[] data = SpecialModelLoader.instance.tesrModelData.get(model);
		if (data == null) return;
		int[] res = new int[data.length];
		for (int i = 0; i < data.length; ++i) {
			res[i] = Float.floatToIntBits(dx + Float.intBitsToFloat(data[i]));	//X
			res[++i] = Float.floatToIntBits(dy + Float.intBitsToFloat(data[i]));//Y
			res[++i] = Float.floatToIntBits(dz + Float.intBitsToFloat(data[i]));//Z
			res[++i] = data[i];	//C
			res[++i] = data[i];	//U
			res[++i] = data[i];	//V
			res[++i] = l;		//L
		}
		render.addVertexData(res);
	}

}
