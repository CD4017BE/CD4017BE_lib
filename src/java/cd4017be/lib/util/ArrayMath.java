package cd4017be.lib.util;

public class ArrayMath {

	public static double[] add(double[] a, double[] b) {
		double[] c = new double[Math.min(a.length, b.length)];
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] + b[i];
		return c;
	}

	public static double[] sub(double[] a, double[] b) {
		double[] c = new double[Math.min(a.length, b.length)];
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] - b[i];
		return c;
	}

	public static double[] mul(double[] a, double[] b) {
		double[] c = new double[Math.min(a.length, b.length)];
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] * b[i];
		return c;
	}

	public static double[] div(double[] a, double[] b) {
		double[] c = new double[Math.min(a.length, b.length)];
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] / b[i];
		return c;
	}

	public static double[] ofs(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] + b;
		return c;
	}

	public static double[] neg(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < c.length; i++)
			c[i] = b - a[i];
		return c;
	}

	public static double[] sca(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] * b;
		return c;
	}

	public static double[] inv(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < c.length; i++)
			c[i] = b / a[i];
		return c;
	}

}
