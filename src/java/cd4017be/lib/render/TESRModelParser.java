package cd4017be.lib.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cd4017be.lib.util.ScriptCompiler;
import cd4017be.lib.util.Vec2;
import cd4017be.lib.util.VecN;

/**
 * 
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public class TESRModelParser extends ScriptCompiler {

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

	private TESRModelParser(HashMap<String, Object> variables, State init) {
		super(variables);
		this.states = new ArrayList<State>();
		this.states.add(init);
	}

	private static final boolean[][] rect = {{false, false, true, true}, {false, true, true, false}};

	@Override
	protected String[] methods() {
		return new String[]{"draw", "push", "pop", "rotate", "translate", "scale", "offsetUV", "scaleUV", "color"};
	}

	@Override
	protected void runMethod(int i, Object[] param, int line) throws CompileException {
		switch(i) {
		case 0:{//draw
			Object o = param[0];
			State state = states.get(states.size() - 1);
			if (o instanceof Quad) quads.add(((Quad)o).transform(state));
			else if (o instanceof TESRModelParser)
				for (Quad quad : ((TESRModelParser)o).quads) quads.add(quad.transform(state));
			else if (o instanceof Object[]) 
				for (Object o1 : (Object[])o) quads.add(((Quad)o1).transform(state));
		} break;
		case 1: {//push
			if (states.size() >= maxStates) throw new CompileException("max state depth reached", "push", line);
			states.add(states.get(states.size() - 1).copy());
		} break;
		case 2: {//pop
			if (states.size() <= 1) throw new CompileException("already at origin state", "pop", line);
			states.remove(states.size() - 1);
		} break;
		case 3: {//rotate
			VecN vec = (VecN)param[0];
			State state = states.get(states.size() - 1);
			vec.x[3] = Math.toRadians(vec.x[3]);
			Matrix4d mat = new Matrix4d();
			mat.set(new AxisAngle4d(vec.x));
			state.matrix.mul(mat);
		} break;
		case 4: {//translate
			VecN vec = (VecN)param[0];
			State state = states.get(states.size() - 1);
			Matrix4d mat = new Matrix4d();
			mat.set(new Vector3d(vec.x));
			state.matrix.mul(mat);
		} break;
		case 5: {//scale
			VecN vec = (VecN)param[0];
			State state = states.get(states.size() - 1);
			Matrix4d mat = new Matrix4d();
			mat.setM00(vec.x[0]);
			mat.setM11(vec.x[1]);
			mat.setM22(vec.x[2]);
			state.matrix.mul(mat);
		} break;
		case 6: {//offsetUV
			VecN vec = (VecN)param[0];
			State state = states.get(states.size() - 1);
			state.uvOffset = state.uvOffset.add(vec.x[0] * state.uvScale.x, vec.x[1] * state.uvScale.z);
		} break;
		case 7: {//scaleUV
			VecN vec = (VecN)param[0];
			State state = states.get(states.size() - 1);
			state.uvScale = state.uvScale.scale(vec.x[0], vec.x[1]);
		} break;
		case 8: {//color
			VecN vec = (VecN)param[0];
			State state = states.get(states.size() - 1);
			for (int j = 0; j < vec.x.length; j++) state.color.x[j] = vec.x[j];
		} break;
		}
	}

	@Override
	protected String[] functions() {
		return new String[]{"quad", "rect"};
	}

	@Override
	protected Object runFunction(int c, Object[] param, int line) throws CompileException {
		switch(c) {
		case 0: {//create quad
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
		} case 1: {//create rectangle quad
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
		} default: return null;
		}
	}

	@Override
	protected Object indexArray(Object array, String index, int recLimit, int line) throws CompileException {
		return null;
	}

	@Override
	public ScriptCompiler extScript(HashMap<String, Object> var, String filename, int reclimit) throws CompileException {
		State state = new State();
		state.matrix = new Matrix4d();
		state.matrix.setIdentity();
		state.uvOffset = Vec2.Def(0, 0);
		state.uvScale = Vec2.Def(1, 1);
		state.color = new VecN(1, 1, 1, 1);
		TESRModelParser model = new TESRModelParser(var, state);
		ResourceLocation res = new ResourceLocation(filename + ".tesr");
		SubMethod m;
		try {m = new SubMethod(SpecialModelLoader.instance.loadTESRModelSourceCode(res), res);} catch (IOException e){throw CompileException.of(e, filename + ".tesr", 0);}
		model.run(m, reclimit);
		model.states.clear();
		return model;
	}

	public static int[] bake(String code, ResourceLocation res) throws CompileException {
		State state = new State();
		state.matrix = new Matrix4d();
		state.matrix.setIdentity();
		state.uvOffset = Vec2.Def(0, 0);
		state.uvScale = Vec2.Def(1, 1);
		state.color = new VecN(1, 1, 1, 1);
		HashMap<String, Object> variables = new HashMap<String, Object>();
		TESRModelParser model = new TESRModelParser(variables, state);
		model.run(new SubMethod(code, res), defaultRecLimit);
		int[] data = new int[28 * model.quads.size()];
		for (int i = 0; i < model.quads.size(); i++) {
			Quad quad = model.quads.get(i);
			for (int j = 0; j < 4; j++) {
				int p = i * 28 + j * 7;
				data[p] = Float.floatToIntBits((float)quad.vertices[j].x[0]);
				data[p + 1] = Float.floatToIntBits((float)quad.vertices[j].x[1]);
				data[p + 2] = Float.floatToIntBits((float)quad.vertices[j].x[2]);
				int r = MathHelper.clamp((int)(quad.vertices[j].x[5] * 255F), 0, 255);
				int g = MathHelper.clamp((int)(quad.vertices[j].x[6] * 255F), 0, 255);
				int b = MathHelper.clamp((int)(quad.vertices[j].x[7] * 255F), 0, 255);
				int a = MathHelper.clamp((int)(quad.vertices[j].x[8] * 255F), 0, 255);
				data[p + 3] = a << 24 | r << 16 | g << 8 | b;
				data[p + 4] = Float.floatToIntBits((float)quad.vertices[j].x[3]);
				data[p + 5] = Float.floatToIntBits((float)quad.vertices[j].x[4]);
				data[p + 6] = 0x00f000f0;
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

	public static void renderWithTOCB(VertexBuffer render, String model, TextureAtlasSprite tex, float dx, float dy, float dz, int c, int l) {
		int[] data = SpecialModelLoader.instance.tesrModelData.get(model);
		if (data == null) return;
		int[] res = new int[data.length];
		for (int i = 0; i < data.length; ++i) {
			res[i] = Float.floatToIntBits(dx + Float.intBitsToFloat(data[i]));	//X
			res[++i] = Float.floatToIntBits(dy + Float.intBitsToFloat(data[i]));//Y
			res[++i] = Float.floatToIntBits(dz + Float.intBitsToFloat(data[i]));//Z
			res[++i] = c;	//C
			res[++i] = Float.floatToIntBits(tex.getInterpolatedU(Float.intBitsToFloat(data[i])));	//U
			res[++i] = Float.floatToIntBits(tex.getInterpolatedV(Float.intBitsToFloat(data[i])));	//V
			res[++i] = l;	//L
		}
		render.addVertexData(res);
	}

}
