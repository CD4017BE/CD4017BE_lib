package cd4017be.lib.script.obj;

import java.util.function.LongBinaryOperator;

/**
 * Represents both numbers and booleans as double precision floating point numbers.
 * Where booleans are reprensented as a probability 0.0 - 1.0 and also treated as such in the logical operations.
 * @author cd4017be
 */
public class Number implements IOperand {

	/** The number representing boolean "true" */
	public static final Number TRUE = new Number(-0.0).onCopy();
	/** The number representing boolean "false" */
	public static final Number FALSE = new Number(0.0).onCopy();
	/** The number representing NaN */
	public static final Number NAN = new Number(Double.NaN).onCopy();

	private boolean copied;
	public double value;

	public Number(double value) {
		this.value = value;
	}

	private Number of(double value) {
		if (copied) return new Number(value);
		this.value = value;
		return this;
	}

	@Override
	public Number onCopy() {
		copied = true;
		return this;
	}

	@Override
	public boolean asBool() {
		return Double.doubleToRawLongBits(value) < 0;
	}

	@Override
	public int asIndex() {
		return (int)value;
	}

	@Override
	public double asDouble() {
		return value;
	}

	@Override
	public boolean isError() {
		return Double.isNaN(value);
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public IOperand op(int code) {
		double v = value;
		switch(code) {
		case sub: return of(-v);
		case add: return of(Math.abs(v));
		case div: return of(1.0 / v);
		case mod: return of(v - Math.floor(v));
		case len: return of(Math.rint(v));
		case pow: return of(Math.sqrt(v));
		case nls: return of(Math.ceil(v));
		case ngr: return of(Math.floor(v));
		case ls: return of(Math.nextDown(v));
		case gr: return of(Math.nextUp(v));
		case lsh: return of(Math.exp(v));
		case rsh: return of(Math.log(v));
		case or: return of(Math.cos(v));
		case nor: return of(Math.acos(v));
		case and: return of(Math.sin(v));
		case nand: return of(Math.asin(v));
		case xor: return of(Math.tan(v));
		case xnor: return of(Math.atan(v));
		case eq: return of(v >= 1.0 ? 1.0 : v <= 0.0 ? 0.0 : v);
		case neq: return of(v >= 1.0 ? 0.0 : v <= 0.0 ? 1.0 : 1.0 - v);
		case text: return of(Math.toDegrees(v));
		case index: return of(Math.toRadians(v));
		case ref: return of(Math.signum(v));
		case mul:
		default: return IOperand.super.op(code);
		}
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		if (!(x instanceof Number)) return x.opL(code, this);
		double a = value, b = ((Number)x).value;
		switch(code) {
		case add: return of(a + b);
		case sub: return of(a - b);
		case mul: return of(a * b);
		case div: return of(a / b);
		case mod: return of(a % b);
		case pow: return of(Math.pow(a, b));
		case eq:  return a == b ? TRUE : FALSE;
		case neq: return a != b ? TRUE : FALSE;
		case gr:  return a > b ? TRUE : FALSE;
		case nls: return a >= b ? TRUE : FALSE;
		case ls:  return a < b ? TRUE : FALSE;
		case ngr: return a <= b ? TRUE : FALSE;
		case lsh: return of(Math.scalb(a, (int)b));
		case rsh: return of(Math.scalb(a, -(int)b));
		case and: return of(and(a, b));
		case nand:return of(-and(a, b));
		case or:  return of(or(a, b));
		case nor: return of(-or(a, b));
		case xor: return of(xor(a, b));
		case xnor:return of(-xor(a, b));
		case len: return of(Math.hypot(a, b));
		case index: return of(Math.atan2(b, a));
		case ref:
		case text:
		default: return x.opL(code, this);
		}
	}

	static double and(double a, double b) {
		return bitwise(a, b, (x, y)-> x & y);
	}

	static double or(double a, double b) {
		return bitwise(a, b, (x, y)-> x | y);
	}

	static double xor(double a, double b) {
		return bitwise(a, b, (x, y)-> x ^ y);
	}

	static double bitwise(double a, double b, LongBinaryOperator op) {
		long la = Double.doubleToRawLongBits(a), lb = Double.doubleToRawLongBits(b);
		int expA = (int)(la >> 52) & 0x7ff, expB = (int)(lb >> 52) & 0x7ff;
		if (expA == 0x7ff || expB == 0x7ff) return Double.NaN;
		la = la & 0x000f_ffff_ffff_ffffL | (expA == 0 ? 0L : 0x0010_0000_0000_0000L) ^ la >> 63;
		lb = lb & 0x000f_ffff_ffff_ffffL | (expB == 0 ? 0L : 0x0010_0000_0000_0000L) ^ lb >> 63;
		int exp = expA - expB;
		if      (exp < 0) la >>= -exp > 63 || expB == 0x7ff ? 63 : -exp;
		else if (exp > 0) lb >>=  exp > 63 || expA == 0x7ff ? 63 :  exp;
		exp = Math.max(expA, expB);
		la = op.applyAsLong(la, lb);
		lb = la ^ la >> 63;
		if (lb != 0) {
			expA = Long.numberOfLeadingZeros(lb) - 11;
			if ((exp -= expA) <= 0) lb <<= expA + exp;
			else lb = (long)exp << 52 | lb << expA & 0x000f_ffff_ffff_ffffL;
		}
		return Double.longBitsToDouble(lb | la & 0x8000_0000_0000_0000L);
	}

	public static void main(String[] args) {
		double a = 0x5p-1026, b = 0x6p-1026;
		System.out.println("and: " + bitwise(a, b, (x, y)-> x & y));
		System.out.println("or: " + bitwise(a, b, (x, y)-> x | y));
		System.out.println("xor: " + bitwise(a, b, (x, y)-> x ^ y));
		System.out.println("nand: " + bitwise(a, b, (x, y)-> ~(x & y)));
		System.out.println("nor: " + bitwise(a, b, (x, y)-> ~(x | y)));
		System.out.println("nxor: " + bitwise(a, b, (x, y)-> ~(x ^ y)));
	}

	@Override
	public IOperand opL(int code, IOperand x) {
		switch(code) {
		
		default:
			return IOperand.super.opL(code, x);
		}
	}

	/*
	@Override
	public IOperand and(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).asNumBool() : x.asBool() ? 1.0 : 0.0;
			double a = asNumBool();
			return of(a*b);
		} catch(Error e) {return e.reset(value + " & ERROR");}
	}

	@Override
	public IOperand or(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).asNumBool() : x.asBool() ? 1.0 : 0.0;
			double a = asNumBool();
			return of(a + b - a*b);
		} catch(Error e) {return e.reset(value + " | ERROR");}
	}

	@Override
	public IOperand nand(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).asNumBool() : x.asBool() ? 1.0 : 0.0;
			double a = asNumBool();
			return of(1.0 - a*b);
		} catch(Error e) {return e.reset(value + " ~& ERROR");}
	}

	@Override
	public IOperand nor(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).asNumBool() : x.asBool() ? 1.0 : 0.0;
			double a = asNumBool();
			return of((1.0-a) * (1.0-b));
		} catch(Error e) {return e.reset(value + " ~| ERROR");}
	}

	@Override
	public IOperand xor(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).asNumBool() : x.asBool() ? 1.0 : 0.0;
			double a = asNumBool();
			return of(a + b - 2*a*b);
		} catch(Error e) {return e.reset(value + " ^ ERROR");}
	}

	@Override
	public IOperand xnor(IOperand x) {
		try {
			double b = x instanceof Number ? 1.0 - ((Number)x).asNumBool() : x.asBool() ? 0.0 : 1.0;
			double a = asNumBool();
			return of(a + b - 2*a*b);
		} catch(Error e) {return e.reset(value + " ~^ ERROR");}
	}
*/

	@Override
	public OperandIterator iterator() {
		return new IntIterator((int)Math.ceil(value));
	}

	@Override
	public Object value() {
		return value;
	}

	static class IntIterator implements OperandIterator {

		final int max;
		int cur;

		public IntIterator(int max) {
			this.max = max;
			this.cur = 0;
		}

		@Override
		public boolean hasNext() {
			return cur < max;
		}

		@Override
		public Number next() {
			return new Number(cur++);
		}

		@Override
		public void set(IOperand e) {
		}

		@Override
		public void reset() {
			cur = 0;
		}

		@Override
		public Object value() {
			return this;
		}

	}

}
