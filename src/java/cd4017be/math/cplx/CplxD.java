package cd4017be.math.cplx;

import cd4017be.math.Operand;

/** Mutable double complex number
 * @author cd4017be */
public final class CplxD implements Operand.D<CplxD> {

	/** Re(this) */
	public double r;
	/** Im(this) */
	public double i;

	/** @return this = r + i*<b>i</b> */
	public CplxD set(double r, double i) {
		this.r = r;
		this.i = i;
		return this;
	}

	/** @return this = l * <b>e</b>^(a*<b>i</b>) */
	public CplxD setPol(double l, double a) {
		this.r = Math.cos(a) * l;
		this.i = Math.sin(a) * l;
		return this;
	}

	public CplxD set(CplxF o) {
		r = o.r;
		i = o.i;
		return this;
	}

	@Override
	public CplxD set(CplxD o) {
		r = o.r;
		i = o.i;
		return this;
	}

	@Override
	public CplxD sum(CplxD a, CplxD b) {
		r = a.r + b.r;
		i = a.i + b.i;
		return this;
	}

	@Override
	public CplxD dif(CplxD a, CplxD b) {
		r = a.r - b.r;
		i = a.i - b.i;
		return this;
	}

	@Override
	public CplxD prod(CplxD a, CplxD b) {
		return set(
			a.r * b.r - a.i * b.i,
			a.r * b.i + a.i * b.r
		);
	}

	@Override
	public CplxD quot(CplxD a, CplxD b) {
		double asq = 1F / b.asqD();
		return sca(prod(a, b), asq);
	}

	@Override
	public CplxD rem(CplxD a, CplxD b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CplxD sca(CplxD o, double s) {
		r = o.r * s;
		i = o.i * s;
		return this;
	}

	@Override
	public CplxD neg(CplxD o) {
		r = -o.r;
		i = -o.i;
		return this;
	}

	@Override
	public double asqD() {
		return r * r + i * i;
	}

	/** @return this = Re(this) - Im(this) <b>i</b> */
	public CplxD conj() {
		i = -i;
		return this;
	}

	/** @return this = <b>e</b>^(this) */
	public CplxD exp() {
		double s = Math.exp(r);
		this.r = Math.cos(i) * s;
		this.i = Math.sin(i) * s;
		return this;
	}

	/** @return this = ln(this) */
	public CplxD ln() {
		return set(Math.log(asqD()) * .5, ang());
	}

	/** @return Im(ln(this)) */
	public double ang() {
		return Math.atan2(i, r);
	}

	@Override
	public int hashCode() {
		return (31 + Float.hashCode((float)i)) * 31 + Float.hashCode((float)r);
	}

	@Override
	public boolean equals(CplxD c) {
		return Double.doubleToLongBits(c.r) == Double.doubleToLongBits(r)
			&& Double.doubleToLongBits(c.i) == Double.doubleToLongBits(i);
	}

	public boolean equals(CplxF c) {
		return Double.doubleToLongBits(c.r) == Double.doubleToLongBits(r)
			&& Double.doubleToLongBits(c.i) == Double.doubleToLongBits(i);
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj instanceof CplxD) return equals((CplxD)obj);
		if(obj instanceof CplxF) return equals((CplxF)obj);
		return false;
	}

	@Override
	public String toString() {
		return "(" + r + " + " + i + " i)";
	}

	@Override
	public CplxD clone() {
		return new CplxD().set(this);
	}

	/** @return 0 */
	public static CplxD C0D() {
		return new CplxD();
	}

	/** @return 1 */
	public static CplxD C1D() {
		return C_(1);
	}

	/** @return <b>i</b> */
	public static CplxD CiD() {
		return C_i(1);
	}

	/** @return i*<b>i</b> */
	public static CplxD C_i(double i) {
		CplxD c = new CplxD();
		c.i = i;
		return c;
	}

	/** @return r */
	public static CplxD C_(float r) {
		CplxD c = new CplxD();
		c.r = r;
		return c;
	}

	/** @return r + i*<b>i</b> */
	public static CplxD C_(float r, float i) {
		CplxD c = new CplxD();
		c.r = r;
		c.i = i;
		return c;
	}

	/** @return l * <b>e</b>^(a*<b>i</b>) */
	public static CplxD C_pol(double l, double a) {
		return new CplxD().setPol(l, a);
	}

}
