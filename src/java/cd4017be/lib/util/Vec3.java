package cd4017be.lib.util;

/**
 *
 * @author CD4017BE
 */
public class Vec3 extends Vec2 {
	/**
	 * Vector y-coord
	 */
	public double y;

	protected Vec3() {}

	public Vec3(Vec2 vec, double y) {
		this.x = vec.x;
		this.y = y;
		this.z = vec.z;
	}

	public boolean isNaN() {
		return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
	}

	/**
	 * Vector square length
	 * @return v²
	 */
	@Override
	public double sq() {
		return this.scale(this);
	}

	/**
	 * Vector length
	 * @return |v|
	 */
	@Override
	public double l() {
		return Math.sqrt(this.scale(this));
	}

	/**
	 * Creates a new Vector
	 * @param x
	 * @param y
	 * @param z
	 * @return v = (x,y,z)
	 */
	public static Vec3 Def(double x, double y, double z) {
		Vec3 vec = new Vec3();
		vec.x = x;
		vec.y = y;
		vec.z = z;
		return vec;
	}

	/**
	 * Creates a copy of this Vector
	 * @return v
	 */
	@Override
	public Vec3 copy() {
		Vec3 vec = new Vec3();
		vec.x = x;
		vec.y = y;
		vec.z = z;
		return vec;
	}

	/**
	 * Inverted Vector
	 * @return -v
	 */
	@Override
	public Vec3 neg() {
		Vec3 vec = new Vec3();
		vec.x = -x;
		vec.y = -y;
		vec.z = -z;
		return vec;
	}

	/**
	 * Normalized Vector with lenght = 1
	 * @return v / |v|
	 */
	@Override
	public Vec3 norm() {
		double d = l();
		Vec3 vec = new Vec3();
		vec.x = x / d;
		vec.y = y / d;
		vec.z = z / d;
		return vec;
	}

	/**
	 * Adds coordinates to this Vector
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vec3 add(double x, double y, double z) {
		Vec3 vec = new Vec3();
		vec.x = this.x + x;
		vec.y = this.y + y;
		vec.z = this.z + z;
		return vec;
	}

	/**
	 * Scales this Vector
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vec3 scale(double x, double y, double z) {
		Vec3 vec = new Vec3();
		vec.x = this.x * x;
		vec.y = this.y * y;
		vec.z = this.z * z;
		return vec;
	}

	/**
	 * @param a
	 * @return v + a
	 */
	public Vec3 add(Vec3 a) {
		Vec3 vec = new Vec3();
		vec.x = x + a.x;
		vec.y = y + a.y;
		vec.z = z + a.z;
		return vec;
	}

	/**
	 * @param a
	 * @return v - a
	 */
	public Vec3 diff(Vec3 a) {
		Vec3 vec = new Vec3();
		vec.x = x - a.x;
		vec.y = y - a.y;
		vec.z = z - a.z;
		return vec;
	}

	/**
	 * @param s
	 * @return v * s
	 */
	@Override
	public Vec3 scale(double s) {
		Vec3 vec = new Vec3();
		vec.x = x * s;
		vec.y = y * s;
		vec.z = z * s;
		return vec;
	}

	/**
	 * @param a
	 * @return v * a
	 */
	public double scale(Vec3 a) {
		return x*a.x + y*a.y + z*a.z;
	}

	/**
	 * @param a
	 * @return v x a
	 */
	public Vec3 mult(Vec3 a) {
		Vec3 vec = new Vec3();
		vec.x = y*a.z - z*a.y;
		vec.y = z*a.x - x*a.z;
		vec.z = x*a.y - y*a.x;
		return vec;
	}

	public Vec3 rotateX(double cos, double sin) {
		Vec3 vec = new Vec3();
		vec.x = x;
		vec.y = y * cos - z * sin;
		vec.z = z * cos + y * sin;
		return vec;
	}

	public Vec3 rotateY(double cos, double sin) {
		Vec3 vec = new Vec3();
		vec.x = x * cos + z * sin;
		vec.y = y;
		vec.z = z * cos - x * sin;
		return vec;
	}

	public Vec3 rotateZ(double cos, double sin) {
		Vec3 vec = new Vec3();
		vec.x = x * cos - y * sin;
		vec.y = y * cos + x * sin;
		vec.z = z;
		return vec;
	}

	/**
	 * anticlockwise rotated Vector around axis-Vector
	 * @param ax Axis-Vector (which MUST be normalized!)
	 * @param an Angle
	 * @return 
	 */
	public Vec3 rotate(Vec3 ax, double an) {
		return this.rotate(ax, Math.cos(an), Math.sin(an));
	}

	public Vec3 rotate(Vec3 ax, double cos, double sin) {
		Vec3 b = ax.scale(this.scale(ax));
		Vec3 a = this.diff(b);
		Vec3 c = a.mult(ax);
		c = c.scale(Math.sqrt(a.sq() / c.sq()));
		return a.scale(Math.cos(cos)).add(c.scale(Math.sin(sin))).add(b);
	}

	public net.minecraft.util.math.Vec3d toMinecraftVec() {
		return new net.minecraft.util.math.Vec3d(x, y, z);
	}

}
