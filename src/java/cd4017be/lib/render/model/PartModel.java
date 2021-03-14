package cd4017be.lib.render.model;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

import java.util.List;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.ModelProperty;

/**ModelProperty for {@link TileEntityModel} to render a part model.
 * @author CD4017BE */
public class PartModel {

	public final String name;
	private float dx, dy, dz;
	/** &0x1 mirX, &0x6 dstX, &0x10 mirY, &0x60 dstY, &0x100 mirZ, &0x600 dstZ, &1000 invFace, &2000 parity */
	private int orient;

	/**@param name of a part specified in model.json */
	public PartModel(String name) {
		this.name = name;
	}

	/**@param x model relative X-offset
	 * @param y model relative Y-offset
	 * @param z model relative Z-offset
	 * @return this (before {@link #orient()}) */
	public PartModel offset(float x, float y, float z) {
		this.dx = x;
		this.dy = y;
		this.dz = z;
		return this;
	}

	/**@param o re-orientation to apply
	 * @param x0 absolute X-origin
	 * @param y0 absolute Y-origin
	 * @param z0 absolute Z-origin
	 * @return this */
	public PartModel orient(int o, float x0, float y0, float z0) {
		this.orient = o;
		float[] p0 = {x0, -x0, y0, -y0, z0, -z0};
		dx += p0[o      & 7] - x0;
		dy += p0[o >> 4 & 7] - y0;
		dz += p0[o >> 8 & 7] - z0;
		return this;
	}

	public boolean hasTransform() {
		return orient != IDEN || dx != 0 || dy != 0 || dz != 0;
	}

	public Direction getSource(Direction d) {
		return d == null ? null : orient(inv(orient), d);
	}

	public BakedQuad transform(BakedQuad quad) {
		int o = orient;
		int[] old = quad.getVertexData(), data = old.clone(); {
			int mirX = (o &   1) << 31, iX = o >> 1 & 3;
			int mirY = (o &  16) << 27, iY = o >> 5 & 3;
			int mirZ = (o & 256) << 23, iZ = o >> 9 & 3;
			int inv = o >> 16 & 16;
			for (int i = 0; i < 32; i+=8) { //re-orient vertices
				int j = i ^ i << 1 & inv;
				data[j+iX] = floatToIntBits(intBitsToFloat(old[i  ]) + dx) ^ mirX;
				data[j+iY] = floatToIntBits(intBitsToFloat(old[i+1]) + dy) ^ mirY;
				data[j+iZ] = floatToIntBits(intBitsToFloat(old[i+2]) + dz) ^ mirZ;
			}
			int n = data[7] ^ o << 18 >> 31; //re-orient normals
			data[7] = data[15] = data[23] = data[31]
			= ((n       ^ mirX >> 31) & 0xff) << (iX << 3)
			| ((n >>  8 ^ mirY >> 31) & 0xff) << (iY << 3)
			| ((n >> 16 ^ mirZ >> 31) & 0xff) << (iZ << 3);
			// bit-flipping isn't exactly negation but the
			// < 1% error shouldn't be noticeable in normals.
		}
		return new BakedQuad(
			data, quad.getTintIndex(), orient(o, quad.getFace()),
			quad.getSprite(), quad.applyDiffuseLighting()
		);
	}

	public static final ModelProperty<List<PartModel>> PART_MODELS = new ModelProperty<>();

	/**the identity re-orientation */
	public static final int IDEN = 0x420;
	/**CCW 1/4 rotations around X-axis */
	public static final int[] ROT_X = {IDEN, 0x340, 0x530, 0x250};
	/**CCW 1/4 rotations around Y-axis */
	public static final int[] ROT_Y = {IDEN, 0x124, 0x521, 0x025};
	/**CCW 1/4 rotations around Z-axis */
	public static final int[] ROT_Z = {IDEN, 0x403, 0x431, 0x412};

	/**@param o sequence of re-orientations
	 * @return a concatenation of all re-orientations */
	public static int con(int... o) {
		if (o.length == 0) return IDEN;
		int r = o[0];
		for (int i = 1; i < o.length; i++)
			r = con(r, o[i]);
		return r;
	}

	/**@param p first re-orientation
	 * @param q second re-orientation
	 * @return a re-orientation that performs p followed by q*/
	public static int con(int p, int q) {
		return q >> (p << 1 & 12) & 7
			| (q >> (p >> 3 & 12) & 7) << 4
			| (q >> (p >> 7 & 12) & 7) << 8
			| q & 0x3000 ^ p & 0x3111;
	}

	/**@param o re-orientation
	 * @return a re-orientation that reverses o */
	public static int inv(int o) {
		return     (o & 1) << (o << 1 & 12)
		| (2 | o >> 4 & 1) << (o >> 3 & 12)
		| (4 | o >> 8 & 1) << (o >> 7 & 12);
	}

	public static Direction orient(int o, Direction dir) {
		int d = dir.getIndex();
		return Direction.byIndex(
			(o >> (((d >> 1) + 1) % 3 << 2) & 7) + 4 ^ (d ^ o >> 13) & 1
		);
	}

}