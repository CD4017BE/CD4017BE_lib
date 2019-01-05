package cd4017be.lib.script.obj;

import java.util.Random;

/**
 * Represents both numbers and booleans as double precision floating point numbers.
 * Where booleans are reprensented as a probability 0.0 - 1.0 and also treated as such in the logical operations.
 * @author cd4017be
 */
public class Number implements IOperand {

	/** The number representing boolean "true" */
	public static final Number TRUE = new Number(1.0);
	/** The number representing boolean "false" */
	public static final Number FALSE = new Number(0.0);
	/** The number representing an unknown boolean state (50% probability to be either true or false) */
	public static final Number UNKNOWN = new Number(0.5);
	/** The number representing NaN */
	public static final Number NAN = new Number(Double.NaN);

	private static final Random RANDOM = new Random();

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
	public boolean asBool() {
		double v = value;
		return v >= 1.0 || v > 0 && v > RANDOM.nextDouble();
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
	public IOperand addR(IOperand x) {
		return x instanceof Number ? of(value + ((Number)x).value) : x.addR(this);
	}

	@Override
	public IOperand subR(IOperand x) {
		return x instanceof Number ? of(value - ((Number)x).value) : x.subL(this);
	}

	@Override
	public IOperand mulR(IOperand x) {
		return x instanceof Number ? of(value * ((Number)x).value) : x.mulR(this);
	}

	@Override
	public IOperand divR(IOperand x) {
		return x instanceof Number ? of(value / ((Number)x).value) : x.divL(this);
	}

	@Override
	public IOperand modR(IOperand x) {
		return x instanceof Number ? of(value % ((Number)x).value) : x.modL(this);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof Number && ((Number)obj).value == value;
	}

	@Override
	public IOperand ls(IOperand x) {
		if (x instanceof Number) {
			double v = ((Number)x).value;
			return value < v ? TRUE : value >= v ? FALSE : UNKNOWN;
		} else return UNKNOWN;
	}

	@Override
	public IOperand nls(IOperand x) {
		if (x instanceof Number) {
			double v = ((Number)x).value;
			return value >= v ? TRUE : value < v ? FALSE : UNKNOWN;
		} else return UNKNOWN;
	}

	@Override
	public IOperand gr(IOperand x) {
		if (x instanceof Number) {
			double v = ((Number)x).value;
			return value > v ? TRUE : value <= v ? FALSE : UNKNOWN;
		} else return UNKNOWN;
	}

	@Override
	public IOperand ngr(IOperand x) {
		if (x instanceof Number) {
			double v = ((Number)x).value;
			return value <= v ? TRUE : value > v ? FALSE : UNKNOWN;
		} else return UNKNOWN;
	}

	@Override
	public IOperand and(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).value : x.asBool() ? 1.0 : 0.0;
			double a = value;
			return of(a*b);
		} catch(Error e) {return e.reset(value + " & ERROR");}
	}

	@Override
	public IOperand or(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).value : x.asBool() ? 1.0 : 0.0;
			double a = value;
			return of(a + b - a*b);
		} catch(Error e) {return e.reset(value + " | ERROR");}
	}

	@Override
	public IOperand nand(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).value : x.asBool() ? 1.0 : 0.0;
			double a = value;
			return of(1.0 - a*b);
		} catch(Error e) {return e.reset(value + " ~& ERROR");}
	}

	@Override
	public IOperand nor(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).value : x.asBool() ? 1.0 : 0.0;
			double a = value;
			return of((1.0-a) * (1.0-b));
		} catch(Error e) {return e.reset(value + " ~| ERROR");}
	}

	@Override
	public IOperand xor(IOperand x) {
		try {
			double b = x instanceof Number ? ((Number)x).value : x.asBool() ? 1.0 : 0.0;
			double a = value, c = a*b;
			return of((1.0 - c) * (a + b - c));
		} catch(Error e) {return e.reset(value + " ^ ERROR");}
	}

	@Override
	public IOperand xnor(IOperand x) {
		try {
			double b = x instanceof Number ? 1.0 - ((Number)x).value : x.asBool() ? 0.0 : 1.0;
			double a = value, c = a*b;
			return of((1.0 - c) * (a + b - c));
		} catch(Error e) {return e.reset(value + " ~^ ERROR");}
	}

	@Override
	public IOperand neg() {
		return of(-value);
	}

	@Override
	public IOperand inv() {
		return of(1.0 / value);
	}

	@Override
	public IOperand len() {
		return of(Math.abs(value));
	}

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
