package cd4017be.lib.script.obj;

/**
 * 
 * @author cd4017be
 */
public class Vector implements IOperand {

	private boolean copied;
	public final double[] value;

	public Vector(int n) {
		this.value = new double[n];
	}

	public Vector(IOperand[] args) {
		int n = 0;
		for (IOperand op : args)
			n += op instanceof Vector ? ((Vector)op).value.length : 1;
		double[] value = new double[n];
		n = 0;
		for (IOperand op : args)
			if (op instanceof Vector) {
				double[] x = ((Vector)op).value;
				int l = x.length;
				System.arraycopy(x, 0, value, n, l);
				n += l;
			} else value[n++] = op.asDouble();
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
	public IOperand addR(IOperand x) {
		double[] a = value, c;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			if (dl < 0)
				return x.addR(this);
			Vector ret = of();
			c = ret.value;
			for (int i = 0; i < l; i++)
				c[i] = a[i] + b[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
			return ret;
		} else {
			Vector ret = of();
			c = ret.value;
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = a[i] + b;
			return ret;
		}
	}

	@Override
	public IOperand subR(IOperand x) {
		double[] a = value, c;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			if (dl < 0)
				return x.subL(this);
			Vector ret = of();
			c = ret.value;
			for (int i = 0; i < l; i++)
				c[i] = a[i] - b[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
			return ret;
		} else {
			Vector ret = of();
			c = ret.value;
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = a[i] - b;
			return ret;
		}
	}

	@Override
	public IOperand subL(IOperand x) {
		Vector ret = of();
		double[] a = value, c = ret.value;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			for (int i = 0; i < l; i++)
				c[i] = b[i] - a[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
		} else {
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = b - a[i];
		}
		return ret;
	}

	@Override
	public IOperand mulR(IOperand x) {
		double[] a = value, c;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			if (dl < 0)
				return x.mulR(this);
			Vector ret = of();
			c = ret.value;
			for (int i = 0; i < l; i++)
				c[i] = a[i] * b[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
			return ret;
		} else {
			Vector ret = of();
			c = ret.value;
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = a[i] * b;
			return ret;
		}
	}

	@Override
	public IOperand divR(IOperand x) {
		double[] a = value, c;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			if (dl < 0)
				return x.divL(this);
			Vector ret = of();
			c = ret.value;
			for (int i = 0; i < l; i++)
				c[i] = a[i] / b[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
			return ret;
		} else {
			Vector ret = of();
			c = ret.value;
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = a[i] / b;
			return ret;
		}
	}

	@Override
	public IOperand divL(IOperand x) {
		Vector ret = of();
		double[] a = value, c = ret.value;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			for (int i = 0; i < l; i++)
				c[i] = b[i] / a[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
		} else {
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = b / a[i];
		}
		return ret;
	}

	@Override
	public IOperand modR(IOperand x) {
		double[] a = value, c;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			if (dl < 0)
				return x.modL(this);
			Vector ret = of();
			c = ret.value;
			for (int i = 0; i < l; i++)
				c[i] = a[i] % b[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
			return ret;
		} else {
			Vector ret = of();
			c = ret.value;
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = a[i] % b;
			return ret;
		}
	}

	@Override
	public IOperand modL(IOperand x) {
		Vector ret = of();
		double[] a = value, c = ret.value;
		if (x instanceof Vector) {
			double[] b = ((Vector)x).value;
			int l = b.length, dl = a.length - l;
			for (int i = 0; i < l; i++)
				c[i] = b[i] % a[i];
			if (copied && dl > 0)
				System.arraycopy(a, l, c, l, dl);
		} else {
			double b = x.asDouble();
			for (int i = value.length - 1; i >= 0; i--)
				c[i] = b % a[i];
		}
		return ret;
	}

	@Override
	public IOperand neg() {
		Vector ret = of();
		double[] a = value, c = ret.value;
		for (int i = value.length - 1; i >= 0; i--)
			c[i] = -a[i];
		return ret;
	}

	@Override
	public IOperand inv() {
		Vector ret = of();
		double[] a = value, c = ret.value;
		for (int i = value.length - 1; i >= 0; i--)
			c[i] = 1.0 / a[i];
		return ret;
	}

	//TODO scalar & cross product

	@Override
	public IOperand len() {
		return new Number(value.length);
	}

	@Override
	public IOperand get(IOperand idx) {
		int i = idx.asIndex();
		if (i >= 0 && i < value.length)
			return new Number(value[i]);
		else return Number.FALSE;
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
