package cd4017be.math;

import static java.lang.Math.abs;

/**Performs linear algebra algorithms on N-dimensional vectors that are represented via arrays.
 * <br> Methods take the vector length as explicit argument, so the JIT is more likely to in-line calls and unroll loops.
 * Also no index checks are performed.
 * @author CD4017BE */
public class Linalg {

	/**@return (x...) */
	public static float[] vec(float... x) {
		return x;
	}

	/**vector in-place addition
	 * @return a[i] += b[i] for i < n */
	public static float[] add(int n, float[] a, float... b) {return sum(n, a, a, b);}

	/**vector addition
	 * @return i < n : out[i] = a[i] + b[i] */
	public static float[] sum(int n, float[] out, float[] a, float[] b) {
		for (int i = 0; i < n; i++)
			out[i] = a[i] + b[i];
		return out;
	}

	/**vector in-place subtract
	 * @return i < n : a[i] -= b[i] */
	public static float[] sub(int n, float[] a, float... b) {return dif(n, a, a, b);}

	/**vector reverse in-place subtract
	 * @return i < n : a[i] = b[i] - a[i] */
	public static float[] rsub(int n, float[] a, float[] b) {return dif(n, a, b, a);}

	/**vector subtract
	 * @return i < n : out[i] = a[i] - b[i] */
	public static float[] dif(int n, float[] out, float[] a, float[] b) {
		for (int i = 0; i < n; i++)
			out[i] = a[i] - b[i];
		return out;
	}

	/**vector in-place diagonal add with scalar
	 * @return i < n : a[i] += x */
	public static float[] dadd(int n, float[] a, float x) {return dadd(n, a, a, x);}

	/**vector diagonal add with scalar
	 * @return i < n : out[i] = a[i] + x */
	public static float[] dadd(int n, float[] out, float[] a, float x) {
		for (int i = 0; i < n; i++) a[i] += x;
		return a;
	}

	/**vector in-place multiply with scalar
	 * @return i < n : a[i] *= x */
	public static float[] sca(int n, float[] a, float x) {return sca(n, a, a, x);}

	/**vector multiply with scalar
	 * @return i < n : out[i] = a[i] * x */
	public static float[] sca(int n, float[] out, float[] a, float x) {
		for (int i = 0; i < n; i++)
			out[i] = a[i] * x;
		return out;
	}

	/**vector dot-product
	 * @return Σ i < n : a[i] * b[i] */
	public static float dot(int n, float[] a, float[] b) {
		float x = a[0] * b[0];
		for (int i = 1; i < n; i++)
			x += a[i] * b[i];
		return x;
	}

	/**vector square length
	 * @return Σ i < n : a[i]² */
	public static float lenSq(int n, float[] a) {
		return dot(n, a, a);
	}

	/**vector length
	 * @return sqrt(Σ i < n : a[i]²) */
	public static float len(int n, float[] a) {
		return (float)Math.sqrt(dot(n, a, a));
	}

	/**normalize vector in-place to length = 1
	 * @return i < n : a[i] /= |a| */
	public static float[] norm(int n, float[] a) {
		return sca(n, a, a, 1F / len(n, a));
	}

	/**normalize vector to length = 1
	 * @return i < n : out[i] = a[i] / |a| */
	public static float[] norm(int n, float[] out, float[] a) {
		return sca(n, out, a, 1F / len(n, a));
	}

	/**reciprocal vector in-place
	 * @return i < n : a[i] *= s / |a|² */
	public static float[] rec(int n, float[] a, float s) {
		return sca(n, a, a, s / dot(n, a, a));
	}

	/**reciprocal vector
	 * @return i < n : out[i] = a[i] * s / |a|² */
	public static float[] rec(int n, float[] out, float[] a, float s) {
		return sca(n, out, a, s / dot(n, a, a));
	}

	/**negate vector in-place
	 * @return i < n : a[i] = -a[i] */
	public static float[] neg(int n, float[] a) {return neg(n, a, a);}

	/**negate vector
	 * @return i < n : out[i] = -a[i] */
	public static float[] neg(int n, float[] out, float[] a) {
		for (int i = 0; i < n; i++)
			out[i] = -a[i];
		return out;
	}

	/**3D-vector in-place cross product
	 * @return a = a x b */
	public static float[] cross(float[] a, float[] b) {return cross(a, a, b);}

	/**3D-vector reverse in-place cross product
	 * @return a = b x a */
	public static float[] rcross(float[] a, float[] b) {return cross(a, b, a);}

	/**3D-vector cross product
	 * @return a = a x b */
	public static float[] cross(float[] out, float[] a, float[] b) {
		float r0 = a[1] * b[2] - a[2] * b[1];
		float r1 = a[2] * b[0] - a[0] * b[2];
		out[2] = a[0] * b[1] - a[1] * b[0];
		out[1] = r1; out[0] = r0;
		return out;
	}

	/**vector in-place element-wise product
	 * @return i < n : a[i] *= b[i] */
	public static float[] mul(int n, float[] a, float[] b) {return prod(n, a, a, b);}

	/**vector element-wise product
	 * @return i < n : out[i] = a[i] * b[i] */
	public static float[] prod(int n, float[] out, float[] a, float[] b) {
		for (int i = 0; i < n; i++)
			out[i] = a[i] * b[i];
		return out;
	}

	/**vector element-wise quotient
	 * @return i < n : out[i] = a[i] / b[i] */
	public static float[] quot(int n, float[] out, float[] a, float[] b) {
		for (int i = 0; i < n; i++)
			out[i] = a[i] / b[i];
		return out;
	}

	public static boolean allInRange(int n, float[] a, float min, float max) {
		for (int i = 0; i < n; i++)
			if (a[i] < min || a[i] >= max)
				return false;
		return true;
	}

	public static float[] col(int n, float[] out, float[][] mat, int j) {
		for (int i = 0; i < n; i++)
			out[i] = mat[i][j];
		return out;
	}

	public static float[] subsca(int n, float[] a, float[] b, float x) {
		for (int i = 0; i < n; i++)
			a[i] -= b[i] * x;
		return a;
	}

	public static void solveGauss(float[][] mat, int m, int n) {
		for (int i = 0; i < m; i++) {
			//find row with largest diagonal entry
			float x = mat[i][i];
			int k = i;
			for(int j = i + 1; j < m; j++) {
				float y = mat[j][i];
				if (abs(y) > abs(x)) {
					x = y;
					k = j;
				}
			}
			//swap rows
			float[] row = mat[k];
			if (k != i) {
				mat[k] = mat[i];
				mat[i] = row;
			}
			//re-scale row
			if (x == 0) {
				//System is undefined, so just pretend that there is another
				//[0.., 1, 0..] row outside the matrix that we can "swap" with.
				row[i] = 1;
				for (int j = i + 1; j < n; j++) row[j] = 0;
				continue;
			} else if (x != 1)
				sca(n, row, 1F / x);
			//eliminate column
			for (int j = 0; j < m; j++) {
				if (j == i) continue;
				float[] row1 = mat[j];
				if ((x = row1[i]) == 0) continue;
				subsca(n, row1, row, x);
			}
		}
	}

}
