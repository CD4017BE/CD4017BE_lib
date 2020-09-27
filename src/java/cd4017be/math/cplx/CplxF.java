package cd4017be.math.cplx;

import cd4017be.math.Operand;

/** Mutable float complex number.
 * @author cd4017be */
public final class CplxF implements Operand.F<CplxF> {

	/** Re(this) */
	public float r;
	/** Im(this) */
	public float i;

	/** @return this = r + i*<b>i</b> */
	public CplxF set(float r, float i) {
		this.r = r;
		this.i = i;
		return this;
	}

	/** @return this = l * <b>e</b>^(a*<b>i</b>) */
	public CplxF setPol(double l, double a) {
		this.r = (float)(Math.cos(a) * l);
		this.i = (float)(Math.sin(a) * l);
		return this;
	}

	public CplxF set(CplxD o) {
		r = (float)o.r;
		i = (float)o.i;
		return this;
	}

	@Override
	public CplxF set(CplxF o) {
		r = o.r;
		i = o.i;
		return this;
	}

	@Override
	public CplxF sum(CplxF a, CplxF b) {
		r = a.r + b.r;
		i = a.i + b.i;
		return this;
	}

	@Override
	public CplxF dif(CplxF a, CplxF b) {
		r = a.r - b.r;
		i = a.i - b.i;
		return this;
	}

	@Override
	public CplxF prod(CplxF a, CplxF b) {
		return set(
			a.r * b.r - a.i * b.i,
			a.r * b.i + a.i * b.r
		);
	}

	@Override
	public CplxF quot(CplxF a, CplxF b) {
		float asq = 1F / b.asqF();
		return sca(prod(a, b), asq);
	}

	@Override
	public CplxF rem(CplxF a, CplxF b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CplxF sca(CplxF o, float s) {
		r = o.r * s;
		i = o.i * s;
		return this;
	}

	@Override
	public CplxF neg(CplxF o) {
		r = -o.r;
		i = -o.i;
		return this;
	}

	public CplxF conj() {
		i = -i;
		return this;
	}

	@Override
	public float asqF() {
		return r * r + i * i;
	}

	@Override
	public boolean equals(CplxF c) {
		return Float.floatToIntBits(r) == Float.floatToIntBits(c.r)
			&& Float.floatToIntBits(i) == Float.floatToIntBits(c.i);
	}

	@Override
	public int hashCode() {
		return (31 + Float.hashCode(i)) * 31 + Float.hashCode(r);
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj instanceof CplxF) return equals((CplxF)obj);
		if(obj instanceof CplxF) return ((CplxF)obj).equals(this);
		return false;
	}

	@Override
	public String toString() {
		return "(" + r + " + " + i + " i)";
	}

	@Override
	public CplxF clone() {
		return new CplxF().set(this);
	}

	/** @return 0 */
	public static CplxF C0F() {
		return new CplxF();
	}

	/** @return 1 */
	public static CplxF C1F() {
		return C_(1);
	}

	/** @return <b>i</b> */
	public static CplxF CiF() {
		return C_i(1);
	}

	/** @return i*<b>i</b> */
	public static CplxF C_i(float i) {
		CplxF c = new CplxF();
		c.i = i;
		return c;
	}

	/** @return r */
	public static CplxF C_(float r) {
		CplxF c = new CplxF();
		c.r = r;
		return c;
	}

	/** @return r + i*<b>i</b> */
	public static CplxF C_(float r, float i) {
		CplxF c = new CplxF();
		c.r = r;
		c.i = i;
		return c;
	}

	/** @return l * <b>e</b>^(a*<b>i</b>) */
	public static CplxF C_pol(double l, double a) {
		return new CplxF().setPol(l, a);
	}

}
