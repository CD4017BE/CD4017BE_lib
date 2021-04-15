package cd4017be.math;

import static java.lang.Float.*;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;

/**Utility for performing axis aligned rotations and reflections.
 * <p> Re-orientations are represented as integers in the following binary format:<br>
 * o = <b>mirX</b> & 0x1 | <b>dstX</b> & 0x6 | <b>mirY</b> & 0x10 | <b>dstY</b> & 0x60 |
 * <b>mirZ</b> & 0x100 | <b>dstZ</b> & 0x600 | <b>parity</b> & 0x1000 | <b>invFace</b> & 0x2000,
 * where <b>parity</b> = #perm(<b>dstX</b>, <b>dstY</b>, <b>dstZ</b>)
 * ^ <b>mirX</b> ^ <b>mirY</b> ^ <b>mirZ</b> ^ <b>invFace</b>. <br>
 * <b>invFace</b> inverts the surface normals for rendering to turn models inside out.</p>
 * @author CD4017BE */
public class Orient {

	/**the identity re-orientation */
	public static final int IDEN = 0x420;
	/**CCW 1/4 rotations around X, Y, Z axes */
	private static final int[] ROT = {
		IDEN, 0x340, 0x530, 0x250,
		IDEN, 0x124, 0x521, 0x025,
		IDEN, 0x403, 0x431, 0x412
	};
	/** rotations from north to each Direction */
	public static final int[] DIRS = {
		0x250, 0x340, 0x420, 0x521, 0x124, 0x025
	};
	/** mirror along X, Y, Z -axis or surface normal */
	public static final int[] MIRR = {
		0x1421, 0x1430, 0x1520, 0x3420
	};

	/**@param axis X = 0, Y = 1, Z = 2
	 * @param angle CCW angle in 90° steps
	 * @return a rotating re-orientation */
	public static int rot(int axis, int angle) {
		return ROT[axis << 2 | angle & 3];
	}

	/**@param dir destination direction
	 * @return a re-orientation rotating from north towards dir */
	public static int northTo(Direction dir) {
		return DIRS[dir.ordinal()];
	}

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

	public static int orient(int o, int d) {
		return ((o >> (((d >> 1) + 1) % 3 << 2) & 7) + 4 ^ (d ^ o >> 13) & 1) % 6;
	}

	/**transform a direction
	 * @param o re-orientation
	 * @param dir direction
	 * @return re-oriented direction */
	public static Direction orient(int o, Direction dir) {
		int d = dir.getIndex();
		return Direction.byIndex(
			(o >> (((d >> 1) + 1) % 3 << 2) & 7) + 4 ^ (d ^ o >> 13) & 1
		);
	}

	/**in-place transform a vector
	 * @param o re-orientation
	 * @param vec 3D-vector
	 * @return re-oriented vec */
	public static float[] orient(int o, float[] vec) {return orient(o, vec, vec);}

	/**transform a vector
	 * @param o re-orientation
	 * @param out output 3D-vector
	 * @param vec input 3D-vector
	 * @return out = re-orientation of vec
	 */
	public static float[] orient(int o, float[] out, float[] vec) {
		int i0 = floatToRawIntBits(vec[0]) ^ o      << 31;
		int i1 = floatToRawIntBits(vec[1]) ^ o >> 4 << 31;
		int i2 = floatToRawIntBits(vec[2]) ^ o >> 8 << 31;
		out[o >> 1 & 3] = intBitsToFloat(i0);
		out[o >> 5 & 3] = intBitsToFloat(i1);
		out[o >> 9 & 3] = intBitsToFloat(i2);
		return out;
	}

	/**@param o re-orientation
	 * @param vec base input coordinate offset
	 * @param x0 origin x
	 * @param y0 origin y
	 * @param z0 origin z
	 * @return vec = input coordinate offset for re-orienting vectors around the given origin */
	public static float[] origin(int o, float[] vec, float x0, float y0, float z0) {
		float[] p0 = {x0, -x0, y0, -y0, z0, -z0};
		vec[0] += p0[o      & 7] - x0;
		vec[1] += p0[o >> 4 & 7] - y0;
		vec[2] += p0[o >> 8 & 7] - z0;
		return vec;
	}

	/**transform a BakedQuad
	 * @param o re-orientation
	 * @param quad input BakedQuad
	 * @param ofs translation in input coordinate system (see {@link #origin()})
	 * @return a new re-oriented quad */
	public static BakedQuad orient(int o, BakedQuad quad, float... ofs) {
		int[] old = quad.getVertexData(), data = old.clone(); {
			int mirX = (o &   1) << 31, iX = o >> 1 & 3;
			int mirY = (o &  16) << 27, iY = o >> 5 & 3;
			int mirZ = (o & 256) << 23, iZ = o >> 9 & 3;
			int inv = o >> 16 & 16;
			for (int i = 0; i < 32; i+=8) { //re-orient vertices
				int j = i ^ i << 1 & inv;
				data[j+iX] = floatToIntBits(intBitsToFloat(old[i  ]) + ofs[0]) ^ mirX;
				data[j+iY] = floatToIntBits(intBitsToFloat(old[i+1]) + ofs[1]) ^ mirY;
				data[j+iZ] = floatToIntBits(intBitsToFloat(old[i+2]) + ofs[2]) ^ mirZ;
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

}
