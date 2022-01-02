package cd4017be.math;

import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;
import net.minecraft.util.math.MathHelper;

/**Faster but less accurate versions of common mathematical functions.
 * @author CD4017BE */
public class FastFunc {

	private static final float LN2 = 0.6931472F * 0x1p-23F, _LN2 = 1F / LN2;

	/**Faster but less accurate alternative to {@link Math#exp(double)}.
	 * @param x Note: NaN is treated as Infinity.
	 * @return e^x with up to +- 0.3% relative error */
	public static float fastExp(float x) {
		int i = (int)(x * _LN2);
		if (i < 0xc1000000) return 0F;
		if (i >= 0x40000000) return Float.POSITIVE_INFINITY;
		return intBitsToFloat(i - logOfsParabola(i));
	}

	/**Faster but less accurate alternative to {@link Math#log(double)}.
	 * @param x Note: NaN is treated as Infinity.
	 * @return ln(x) with up to +- 0.006 absolute error */
	public static float fastLog(float x) {
		int i = floatToRawIntBits(x);
		if (i <= 0) return Float.NEGATIVE_INFINITY;
		if (i >= 0x7f800000) return Float.POSITIVE_INFINITY;
		return (float)(i + logOfsParabola(i)) * LN2;
	}

	/**@param x a fixed point number << 23
	 * @return m * (1 - m) / 3 - 127 as fixed point number << 23,
	 * where m = fractionalPart(x) */
	private static int logOfsParabola(int x) {
		long m = x & 0x7fffff;
		m *= (0x800000 - m) * 0x55555555L >>> 23;
		return (int)(m >>> 32) - 0x3f800000;
	}

	public static float fastSin(float x) {
		return MathHelper.sin(x);
	}

	public static float fastCos(float x) {
		return MathHelper.cos(x);
	}

}
