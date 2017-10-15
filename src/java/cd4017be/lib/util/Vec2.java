package cd4017be.lib.util;

/**
 *
 * @author CD4017BE
 */
public class Vec2 {

	public double x;
	public double z;

	protected Vec2() {}

	/**
	 * Vector square length
	 * @return |v|²
	 */
	public double sq() {
		return this.scale(this);
	}

	/**
	 * Vector length
	 * @return |v|
	 */
	public double l() {
		return Math.sqrt(this.scale(this));
	}

	/**
	 * Creates a new Vector
	 * @param x
	 * @param z
	 * @param z
	 * @return v = (x,z,z)
	 */
	public static Vec2 Def(double x, double z) {
		Vec2 vec = new Vec2();
		vec.x = x;
		vec.z = z;
		return vec;
	}

	/**
	 * Creates a copy of this Vector
	 * @return v
	 */
	public Vec2 copy() {
		Vec2 vec = new Vec2();
		vec.x = x;
		vec.z = z;
		return vec;
	}

	/**
	 * Inverted Vector
	 * @return -v
	 */
	public Vec2 neg() {
		Vec2 vec = new Vec2();
		vec.x = -x;
		vec.z = -z;
		return vec;
	}

	/**
	 * Normalized Vector with lenght = 1
	 * @return v / |v|
	 */
	public Vec2 norm() {
		double d = l();
		Vec2 vec = new Vec2();
		vec.x = x / d;
		vec.z = z / d;
		return vec;
	}

	public Vec2 r90() {
		Vec2 vec = new Vec2();
		vec.x = z;
		vec.z = -x;
		return vec;
	}

	/**
	 * Adds coordinates to this Vector
	 * @param x
	 * @param z
	 * @param z 
	 */
	public Vec2 add(double x, double z) {
		Vec2 vec = new Vec2();
		vec.x = this.x + x;
		vec.z = this.z + z;
		return vec;
	}

	/**
	 * Scales this Vector
	 * @param x
	 * @param z
	 * @param z 
	 */
	public Vec2 scale(double x, double z) {
		Vec2 vec = new Vec2();
		vec.x = this.x * x;
		vec.z = this.z * z;
		return vec;
	}

	/**
	 * 
	 * @param a
	 * @return v + a
	 */
	public Vec2 add(Vec2 a) {
		Vec2 vec = new Vec2();
		vec.x = x + a.x;
		vec.z = z + a.z;
		return vec;
	}

	/**
	 * 
	 * @param a
	 * @return v - a
	 */
	public Vec2 diff(Vec2 a) {
		Vec2 vec = new Vec2();
		vec.x = x - a.x;
		vec.z = z - a.z;
		return vec;
	}

	/**
	 * 
	 * @param s
	 * @return v * s
	 */
	public Vec2 scale(double s) {
		Vec2 vec = new Vec2();
		vec.x = x * s;
		vec.z = z * s;
		return vec;
	}

	/**
	 * 
	 * @param a
	 * @return v * a
	 */
	public double scale(Vec2 a) {
		return x*a.x + z*a.z;
	}

	public Vec2 rotate(double an) {
		return this.rotate(Math.cos(an), Math.sin(an));
	}

	public Vec2 rotate(double cos, double sin) {
		return this.scale(cos).add(r90().scale(sin));
	}

}
