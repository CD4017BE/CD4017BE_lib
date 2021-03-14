package cd4017be.lib.script.obj;

import static cd4017be.lib.script.obj.Number.*;

import java.util.Arrays;

/**
 * Represents a vector (array of numbers) in script
 * @author cd4017be
 */
public class Vector implements IOperand {

	private boolean copied;
	public final double[] value;

	public Vector(int n) {
		this.value = new double[n];
	}

	public Vector(IOperand[] args, final int from, final int to) {
		int n = 0;
		for (int i = from; i < to; i++)
			n += args[i] instanceof Vector ?
				((Vector)args[i]).value.length : 1;
		double[] value = new double[n];
		n = 0;
		for (int i = from; i < to; i++) {
			IOperand op = args[i];
			if (op instanceof Vector) {
				double[] x = ((Vector)op).value;
				int l = x.length;
				System.arraycopy(x, 0, value, n, l);
				n += l;
			} else value[n++] = op.asDouble();
		}
		this.value = value;
	}

	private Vector of() {
		if (copied) return new Vector(value.length);
		return this;
	}

	@Override
	public boolean asBool() {
		return value.length > 0;
	}

	@Override
	public int asIndex() {
		return value.length == 0 ? 0 : (int)value[0];
	}

	@Override
	public IOperand onCopy() {
		copied = true;
		return this;
	}

	@Override
	public IOperand op(int code) {
		switch(code) {
		case len:
			return new Number(value.length);
		case ls: {
			double y = Double.POSITIVE_INFINITY;
			for (double x : value)
				if (x < y) y = x;
			return new Number(y);
		} case gr: {
			double y = Double.NEGATIVE_INFINITY;
			for (double x : value)
				if (x > y) y = x;
			return new Number(y);
		} case add: {
			double y = 0;
			for (double x : value) y += x;
			return new Number(y);
		} case mul: {
			double y = 0;
			for (double x : value) y *= x;
			return new Number(y);
		} case and: {
			double y = -0D;
			for (double x : value) y = and(y, x);
			return new Number(y);
		} case nand: {
			double y = -0D;
			for (double x : value) y = and(y, x);
			return new Number(-y);
		} case or: {
			double y = 0D;
			for (double x : value) y = or(y, x);
			return new Number(y);
		} case nor: {
			double y = 0D;
			for (double x : value) y = or(y, x);
			return new Number(-y);
		} case xor: {
			double y = 0D;
			for (double x : value) y = xor(y, x);
			return new Number(y);
		} case xnor: {
			double y = -0D;
			for (double x : value) y = xor(y, x);
			return new Number(y);
		}
		case sub: {
			Vector ret = of();
			double[] a = value, c = ret.value;
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = -a[i];
			return ret;
		} case div: {
			Vector ret = of();
			double[] a = value, c = ret.value;
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = 1.0 / a[i];
			return ret;
		} case eq:
			if (value.length > 0) {
				double y = value[0];
				for (int i = 1; i < value.length; i++)
					if (value[i] != y)
						return Number.FALSE;
			}
			return Number.TRUE;
		case neq:
			for (int i = 0, l = value.length - 1; i < l; i++) {
				double x = value[i];
				for (int j = i + 1; j <= l; j++)
					if (value[j] != x)
						return Number.FALSE;
			}
			return Number.TRUE;
		case text: {
			double y = 0D;
			for (double x : value) y += x;
			return new Number(y / value.length);
		}
		default:
			return IOperand.super.op(code);
		}
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		if (code == index) {
			int i = x.asIndex();
			if (i >= 0 && i < value.length)
				return new Number(value[i]);
			else return Number.FALSE;
		} else if (code == pow) {
			if (x instanceof Vector) {
				double[] a = value, b = ((Vector)x).value;
				double y = 0;
				for(int i = Math.min(a.length, b.length) - 1; i >= 0; i--)
					y += a[i] * b[i];
				return new Number(y);
			}
		}
		if (x instanceof Vector) {
			double[] a = value, b = ((Vector)x).value;
			Vector r0 = b.length < a.length ? (Vector)x : this, ret = r0.of();
			vectorOp(code, a, b, ret.value);
			return ret;
		} else if (x instanceof Number) {
			Vector ret = of();
			scalarOp(code, value, ((Number)x).value, ret.value);
			return ret;
		} else return x.opL(code, this);
	}

	@Override
	public IOperand opL(int code, IOperand x) {
		if (x instanceof Number) {
			Vector ret = of();
			scalarOp(code, ((Number)x).value, value, ret.value);
			return ret;
		} else return IOperand.super.opL(code, x);
	}

	private static void scalarOp(int code, double[] a, double b, double[] c) {
		int l = c.length;
		switch(code) {
		case add: for (int i = 0; i < l; i++) c[i] = a[i] + b; break;
		case sub: for (int i = 0; i < l; i++) c[i] = a[i] - b; break;
		case mul: for (int i = 0; i < l; i++) c[i] = a[i] * b; break;
		case div: for (int i = 0; i < l; i++) c[i] = a[i] / b; break;
		case mod: for (int i = 0; i < l; i++) c[i] = a[i] % b; break;
		case and: for (int i = 0; i < l; i++) c[i] = and(a[i], b); break;
		case nand: for (int i = 0; i < l; i++) c[i] = -and(a[i], b); break;
		case or: for (int i = 0; i < l; i++) c[i] = or(a[i], b); break;
		case nor: for (int i = 0; i < l; i++) c[i] = -or(a[i], b); break;
		case xor: for (int i = 0; i < l; i++) c[i] = xor(a[i], b); break;
		case xnor: for (int i = 0; i < l; i++) c[i] = -xor(a[i], b); break;
		case lsh: for (int i = 0; i < l; i++) c[i] = Math.scalb(a[i], (int)b); break;
		case rsh: for (int i = 0; i < l; i++) c[i] = Math.scalb(a[i], -(int)b); break;
		case eq: for (int i = 0; i < l; i++) c[i] = a[i] == b ? -0D : 0D; break;
		case neq: for (int i = 0; i < l; i++) c[i] = a[i] != b ? -0D : 0D; break;
		case gr: for (int i = 0; i < l; i++) c[i] = a[i] > b ? -0D : 0D; break;
		case ngr: for (int i = 0; i < l; i++) c[i] = a[i] <= b ? -0D : 0D; break;
		case ls: for (int i = 0; i < l; i++) c[i] = a[i] < b ? -0D : 0D; break;
		case nls: for (int i = 0; i < l; i++) c[i] = a[i] >= b ? -0D : 0D; break;
		default: Arrays.fill(c, Double.NaN);
		}
	}

	private static void scalarOp(int code, double a, double[] b, double[] c) {
		int l = c.length;
		switch(code) {
		case add: for (int i = 0; i < l; i++) c[i] = a + b[i]; break;
		case sub: for (int i = 0; i < l; i++) c[i] = a - b[i]; break;
		case mul: for (int i = 0; i < l; i++) c[i] = a * b[i]; break;
		case div: for (int i = 0; i < l; i++) c[i] = a / b[i]; break;
		case mod: for (int i = 0; i < l; i++) c[i] = a % b[i]; break;
		case and: for (int i = 0; i < l; i++) c[i] = and(a, b[i]); break;
		case nand: for (int i = 0; i < l; i++) c[i] = -and(a, b[i]); break;
		case or: for (int i = 0; i < l; i++) c[i] = or(a, b[i]); break;
		case nor: for (int i = 0; i < l; i++) c[i] = -or(a, b[i]); break;
		case xor: for (int i = 0; i < l; i++) c[i] = xor(a, b[i]); break;
		case xnor: for (int i = 0; i < l; i++) c[i] = -xor(a, b[i]); break;
		case lsh: for (int i = 0; i < l; i++) c[i] = Math.scalb(a, (int)b[i]); break;
		case rsh: for (int i = 0; i < l; i++) c[i] = Math.scalb(a, -(int)b[i]); break;
		case eq: for (int i = 0; i < l; i++) c[i] = a == b[i] ? -0D : 0D; break;
		case neq: for (int i = 0; i < l; i++) c[i] = a != b[i] ? -0D : 0D; break;
		case gr: for (int i = 0; i < l; i++) c[i] = a > b[i] ? -0D : 0D; break;
		case ngr: for (int i = 0; i < l; i++) c[i] = a <= b[i] ? -0D : 0D; break;
		case ls: for (int i = 0; i < l; i++) c[i] = a < b[i] ? -0D : 0D; break;
		case nls: for (int i = 0; i < l; i++) c[i] = a >= b[i] ? -0D : 0D; break;
		default: Arrays.fill(c, Double.NaN);
		}
	}

	private static void vectorOp(int code, double[] a, double[] b, double[] c) {
		int l = c.length;
		switch(code) {
		case add: for (int i = 0; i < l; i++) c[i] = a[i] + b[i]; break;
		case sub: for (int i = 0; i < l; i++) c[i] = a[i] - b[i]; break;
		case mul: for (int i = 0; i < l; i++) c[i] = a[i] * b[i]; break;
		case div: for (int i = 0; i < l; i++) c[i] = a[i] / b[i]; break;
		case mod: for (int i = 0; i < l; i++) c[i] = a[i] % b[i]; break;
		case and: for (int i = 0; i < l; i++) c[i] = and(a[i], b[i]); break;
		case nand: for (int i = 0; i < l; i++) c[i] = -and(a[i], b[i]); break;
		case or: for (int i = 0; i < l; i++) c[i] = or(a[i], b[i]); break;
		case nor: for (int i = 0; i < l; i++) c[i] = -or(a[i], b[i]); break;
		case xor: for (int i = 0; i < l; i++) c[i] = xor(a[i], b[i]); break;
		case xnor: for (int i = 0; i < l; i++) c[i] = -xor(a[i], b[i]); break;
		case lsh: for (int i = 0; i < l; i++) c[i] = Math.scalb(a[i], (int)b[i]); break;
		case rsh: for (int i = 0; i < l; i++) c[i] = Math.scalb(a[i], -(int)b[i]); break;
		case eq: for (int i = 0; i < l; i++) c[i] = a[i] == b[i] ? -0D : 0D; break;
		case neq: for (int i = 0; i < l; i++) c[i] = a[i] != b[i] ? -0D : 0D; break;
		case gr: for (int i = 0; i < l; i++) c[i] = a[i] > b[i] ? -0D : 0D; break;
		case ngr: for (int i = 0; i < l; i++) c[i] = a[i] <= b[i] ? -0D : 0D; break;
		case ls: for (int i = 0; i < l; i++) c[i] = a[i] < b[i] ? -0D : 0D; break;
		case nls: for (int i = 0; i < l; i++) c[i] = a[i] >= b[i] ? -0D : 0D; break;
		case len:
			if (l == 3) {
				double ax = a[0], ay = a[1], az = a[2],
					bx = b[0], by = b[1], bz = b[2];
				c[0] = ay * bz - az * by;
				c[1] = az * bx - ax * bz;
				c[2] = ax * by - ay * bx;
				break;
			}
		default: Arrays.fill(c, Double.NaN);
		}
	}

	@Override
	public void put(IOperand idx, IOperand val) {
		int i = idx.asIndex();
		if (i >= 0 && i < value.length)
			value[i] = val.asDouble();
	}

	@Override
	public OperandIterator iterator() {
		return new Iterator();
	}

	@Override
	public Object value() {
		return value;
	}

	@Override
	public String toString() {
		return Arrays.toString(value);
	}

	class Iterator implements OperandIterator {

		int cur = 0;

		@Override
		public boolean hasNext() {
			return cur < value.length;
		}

		@Override
		public IOperand next() {
			return new Number(value[cur++]);
		}

		@Override
		public void set(IOperand e) {
			value[cur - 1] = e.asDouble();
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
