package cd4017be.lib.render.model;

import static java.lang.Float.floatToRawIntBits;
import static net.minecraft.util.Mth.clamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.script.ScriptException;

import cd4017be.lib.script.Script;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;
import cd4017be.lib.script.obj.Vector;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import com.mojang.math.Vector4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**Passed to scripts for building models.
 * <h1> Member functions: </h1>
 * <li><code><b>.rect(cube, uvs, "xyzF") (cube, uvs, "xyzF", texture)</b></code><dl>
 * add a new face from a cuboid given by <code>cube = [x0, y0, z0, x1, y1, z1]#</code>
 * to the model with transformations applied. <code>uvs = [u0, v0, u1, v1]#</code>
 * defines the rectangle in <code>texture</code> (= "particle" if not given)
 * and <code>"xyzF"</code> is a string defining the mapping of vertex coordinates 'xyz' to
 * uv coordinates ['u' -> -u (left), 'U' -> +u (right), 'v' -> -v (up), 'V' -> +v (down)]
 * or cubiod face ['+' -> +normal, '-' -> -normal] and 'F' is the cull-face [' ' -> none,
 * 'B' -> bottom, 'T' -> top, 'N' -> north, 'S' -> south, 'W'-> west, 'E' -> east] </dl></li>
 * <li><code><b>.quad(a, b, c, d, format) (a, b, c, d, format, texture)</b></code><dl>
 * add a new quad to the model with transformations applied. <code>a, b, c, d</code>
 * are vectors providing the data for each vertex. <code>format</code> is a string,
 * where the last character defines cull-face (one of ' BTNSWE') and the other characters
 * define the meaning of their corresponding vertex vector entries:
 * 'x' 'y' 'z' -> vertex coordinates (default [0, 0, 0]), 'u' 'v' -> texture coordinates
 * (default [0..16, 0..16]), 'r' 'g' 'b' 'a' -> color (default [1, 1, 1, 1]) </dl></li>
 * <li><code><b>.push()</b></code><dl> push a new transformation frame on stack </dl></li>
 * <li><code><b>.pop()</b></code><dl> pop current transformation frame from stack
 * (reverting to before push()) </dl></li>
 * <li><code><b>.color(r, g, b, a) ([r, g, b, a]#)</b></code><dl> apply a color multiplier </dl></li>
 * <li><code><b>.scaleUV(u, v) ([u, v]#)</b></code><dl> apply texture scaling </dl></li>
 * <li><code><b>.translateUV(u, v) ([u, v]#)</b></code><dl> apply texture offset </dl></li>
 * <li><code><b>.scale(x, y, z) ([x, y, z]#)</b></code><dl> apply vertex scaling </dl></li>
 * <li><code><b>.translate(x, y, z) ([x, y, z]#)</b></code><dl> apply vertex offset </dl></li>
 * <li><code><b>.rotate(x, y, z, an) ([x, y, z]#, an)</b></code><dl> apply vertex rotation
 * by angle <code>an</code> in degrees around the given axes from origin. </dl></li>
 * <li><code><b>.rotate([ox, oy, oz]#, [rx, ry, rz]#, an)</b></code><dl> apply vertex rotation with
 * a shifted origin, equivalent to <code>.translate(-o); .rotate(r, an); .translate(o);</code></dl></li>
 * <li><code><b>.orient("xyz", ox, oy, oz) ("xyz", [ox, oy, oz]#)</b></code><dl> apply a
 * re-orientation of coordinate axes and cull-faces. <code>ox, oy, oz</code>
 * define the origin of transformation and <code>"xyz"</code> is a string of
 * length 3 defining the new pointing direction for each axis:
 * 'x' or 'W' -> -x (west), 'X' or 'E' -> +x (east), 'y' or 'B' -> -y (down),
 * 'Y' or 'T' -> +y (up), 'z' or 'N' -> -z (north), 'Z' or 'S' -> +z (south) </dl></li>
 * All texture and block vertex coordinates range from 0 to 16.
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class ScriptModelBuilder implements IOperand {

	private static final String defaultFormat = "xyzuvrgba";
	private static final String cfNames = " BTNSWE", axes = "yYzZxXBTNSWE";
	private static final int rect = 0x01_03_02_00 * 0b001_001;
	private static final String[] F_NAMES = {
		"color", "orient", "pop", "push",
		"quad", "rect", "rotate",
		"scale", "scaleUV",
		"translate", "translateUV",
	};

	final ArrayList<Quad> quads = new ArrayList<>();
	final State[] states = new State[16];
	int curState;
	final IOperand[] stack = new IOperand[256];

	final IFunction[] functions = {
		   (s, b, t) -> { //color
			double[] v = IOperand.vec(s, b, t, 4);
			float[] uvc = states[curState].sUVC;
			for (int i = 0; i < 4; i++) uvc[i + 2] *= v[i];
		}, (s, b, t) -> { //orient
			IOperand.check(t-b, 2, 4);
			String or = s[b++].toString();
			double[] center = IOperand.vec(s, b, t, 3);
			State state = states[curState];
			float[] mat = new float[16];
			mat[3] = (float)center[0];
			mat[7] = (float)center[1];
			mat[11] = (float)center[2];
			mat[15] = 1;
			int o = 0;
			for (int i = 0; i < 3; i++) {
				int a = axes.indexOf(or.charAt(i)) % 6, j = ((a >> 1) + 1) % 3 << 2;
				float d = -1 + (a << 1 & 2);
				mat[j | i] = d;
				mat[j | 3] -= (float)center[i] * d;
				a <<= 2;
				a = state.orient >> (a ^ 4) & 0xf0
				  | state.orient >> a << 4 & 0xf00;
				o |= a << ((i+2) % 3 << 3);
			}
			state.matrix.multiply(new Matrix4f(mat));
			state.orient = o;
		}, (s, b, t) -> { //pop
			IOperand.check(t-b, 0, 0);
			if (curState <= 0)
				throw new ScriptException("transformation stack underflow");
			states[curState--] = null;
		}, (s, b, t) -> { //push
			IOperand.check(t-b, 0, 0);
			if (curState >= 15)
				throw new ScriptException("transformation stack overflow");
			states[++curState] = new State(states[curState - 1]);
		}, (s, b, t) -> { //quad
			IOperand.check(t-b, 5, 6);
			Quad quad = new Quad();
			String format = s[b + 4].toString();
			quad.tex = t - b > 5 ? s[b + 5].toString() : "particle";
			quad.cullFace = cfNames.indexOf(format.charAt(format.length() - 1));
			float[] vert = quad.vertices;
			for (int i = 0; i < 4; i++) {
				double[] vec = IOperand.get(s, b, i, Vector.class).value;
				for (int j = 0, n = 0; j < format.length(); j++) {
					int q = defaultFormat.indexOf(format.charAt(j));
					if (q >= 0) vert[q + i * 9] = (float)vec[n++];
				}
			}
			quads.add(quad.transform(states[curState]));
		}, (s, b, t) -> { //rect
			IOperand.check(t-b, 3, 4);
			Quad quad = new Quad();
			String format = s[b + 2].toString();
			quad.tex = t - b > 3 ? s[b + 3].toString() : "particle";
			quad.cullFace = format.length() <= 3 ? 0 : cfNames.indexOf(format.charAt(3));
			int iN = 0, iU = 0, iV = 0, tN, tU, tV;
			if ((tN = format.indexOf('-')) < 0) {tN = format.indexOf('+'); iN = 1;}
			if ((tU = format.indexOf('U')) < 0) {tU = format.indexOf('u'); iU = 1;}
			if ((tV = format.indexOf('V')) < 0) {tV = format.indexOf('v'); iV = 1;}
			double[] pos = IOperand.get(s, b, 0, Vector.class).value;
			double[] tex = IOperand.get(s, b, 1, Vector.class).value;
			float[] vert = quad.vertices;
			for (int i = 0; i < 4; i++) {
				int j = (iN != 0 ? i : 3 - i) * 9;
				int ri = rect >> (i << 3) | iN * 0b100_100;
				vert[j+0] = (float)pos[(ri >> tN + 2 & 1) * 3 + 0];
				vert[j+1] = (float)pos[(ri >> tN + 1 & 1) * 3 + 1];
				vert[j+2] = (float)pos[(ri >> tN + 0 & 1) * 3 + 2];
				vert[j+3] = (float)tex[(ri >> tN - tU + 2 & 1 ^ iU) * 2 + 0];
				vert[j+4] = (float)tex[(ri >> tN - tV + 2 & 1 ^ iV) * 2 + 1];
				vert[j+5] = 1; vert[j+6] = 1; vert[j+7] = 1; vert[j+8] = 1;
			}
			quads.add(quad.transform(states[curState]));
		}, (s, b, t) -> { //rotate
			Matrix4f mat = states[curState].matrix, tr = null;
			double[] v;
			if (t - b == 3) {
				v = IOperand.vec(s, b, ++b, 3);
				tr = Matrix4f.createTranslateMatrix((float)v[0], (float)v[1], (float)v[2]);
				mat.multiply(tr);
				tr.setTranslation(-(float)v[0], -(float)v[1], -(float)v[2]);
			}
			v = IOperand.vec(s, b, t - 1, 3);
			mat.multiply(new Quaternion(
				new Vector3f((float)v[0], (float)v[1], (float)v[2]),
				(float)s[t - 1].asDouble(), true
			));
			if (tr != null) mat.multiply(tr);
		}, (s, b, t) -> { //scale
			double[] v = IOperand.vec(s, b, t, 3);
			states[curState].matrix.multiply(Matrix4f.createScaleMatrix((float)v[0], (float)v[1], (float)v[2]));
		}, (s, b, t) -> { //scaleUV
			double[] v = IOperand.vec(s, b, t, 2);
			float[] uvc = states[curState].sUVC;
			uvc[0] *= v[0];
			uvc[1] *= v[1];
		}, (s, b, t) -> { //translate
			double[] v = IOperand.vec(s, b, t, 3);
			states[curState].matrix.multiply(Matrix4f.createTranslateMatrix((float)v[0], (float)v[1], (float)v[2]));
		}, (s, b, t) -> { //translateUV
			double[] v = IOperand.vec(s, b, t, 2);
			State st = states[curState];
			st.oU += v[0] * st.sUVC[0];
			st.oV += v[1] * st.sUVC[1];
		}
	};

	public List<Quad> run(Script s) throws ScriptException {
		quads.clear();
		states[0] = new State();
		stack[1] = this;
		s.call(stack, 1, 2);
		stack[0] = null;
		return quads;
	}

	@Override
	public boolean asBool() {
		return true;
	}

	@Override
	public IOperand get(String member) {
		int i = Arrays.binarySearch(F_NAMES, member);
		return i < 0 ? Nil.NIL : functions[i];
	}

	@OnlyIn(Dist.CLIENT)
	public static class Quad {
		//x, y, z, u, v, r, g, b, a
		public final float[] vertices = {
			0, 0, 0,  0,  0, 1, 1, 1, 1,
			0, 0, 0, 16,  0, 1, 1, 1, 1,
			0, 0, 0, 16, 16, 1, 1, 1, 1,
			0, 0, 0,  0, 16, 1, 1, 1, 1
		};
		public String tex;
		public int cullFace;
		public boolean shade = true;

		Quad transform(State s) {
			float[] vt = vertices;
			Vector4f v = new Vector4f();
			for (int i = 0; i < 36; i+=9) {
				v.set(vt[i], vt[i+1], vt[i+2], 1);
				v.transform(s.matrix);
				vt[i] = v.x();
				vt[i+1] = v.y();
				vt[i+2] = v.z();
				for (int k = i + 3, j = 0; j < 6; j++, k++)
					vt[k] *= s.sUVC[j];
				vt[i+3] += s.oU;
				vt[i+4] += s.oV;
			}
			cullFace = s.orient >> (cullFace << 2) & 15;
			return this;
		}

		public void bake(
			SimpleBakedModel.Builder b, Transformation transf,
			Function<String, TextureAtlasSprite> resolver
		) {
			TextureAtlasSprite sprite = resolver.apply(tex);
			Vector3f[] pos = new Vector3f[4];
			int[] data = new int[32];
			float[] vert = vertices;
			for (int i = 0, j = 0, k = 0; i < 4; i++, j+=8) {
				Vector4f posi = new Vector4f(vert[k++] / 16F - .5F, vert[k++] / 16F - .5F, vert[k++] / 16F - .5F, 1);
				transf.transformPosition(posi);
				pos[i] = new Vector3f(posi.x(), posi.y(), posi.z());
				data[j  ] = floatToRawIntBits(posi.x() + .5F);
				data[j+1] = floatToRawIntBits(posi.y() + .5F);
				data[j+2] = floatToRawIntBits(posi.z() + .5F);
				data[j+4] = floatToRawIntBits(sprite.getU(vert[k++]));
				data[j+5] = floatToRawIntBits(sprite.getV(vert[k++]));
				data[j+3] = (int)clamp(vert[k++] * 255D, 0, 255)
				| (int)clamp(vert[k++] * 255F, 0, 255) << 8
				| (int)clamp(vert[k++] * 255F, 0, 255) << 16
				| (int)clamp(vert[k++] * 255F, 0, 255) << 24;
			}
			Vector3f v = pos[2];
			v.sub(pos[0]);
			pos[3].sub(pos[1]);
			v.cross(pos[3]);
			v.normalize();
			data[7] = data[15] = data[23] = data[31] = 
				   Math.round(v.x() * 127F) & 0xff
				| (Math.round(v.y() * 127F) & 0xff) << 8
				| (Math.round(v.z() * 127F) & 0xff) << 16;
			Direction d = Direction.getNearest(v.x(), v.y(), v.z());
			BakedQuad bq = new BakedQuad(data, -1, d, sprite, shade);
			if (cullFace == 0) b.addUnculledFace(bq);
			else b.addCulledFace(transf.rotateTransform(Direction.from3DDataValue(cullFace - 1)), bq);
		}
	}

	@OnlyIn(Dist.CLIENT)
	static class State {
		final Matrix4f matrix;
		final float[] sUVC;
		float oU, oV;
		int orient;

		State() {
			matrix = new Matrix4f();
			matrix.setIdentity();
			sUVC = new float[]{1, 1, 1, 1, 1, 1};
			orient = 0x6543210;
		}

		State(State s) {
			matrix = new Matrix4f(s.matrix);
			oU = s.oU;
			oV = s.oV;
			sUVC = s.sUVC.clone();
			orient = s.orient;
		}

	}

}
