package cd4017be.lib.script;

/**
 * 
 * @author CD4017BE
 */
public class Parameters {

	public final Object[] param;

	public Parameters(Object... param) {
		this.param = param;
	}

	public String getString(int i) {
		if (i >= param.length) throw num(i);
		Object o = param[i];
		if (o instanceof String) return (String)o;
		throw ex("String", o, i);
	}

	public double getNumber(int i) {
		if (i >= param.length) throw num(i);
		Object o = param[i];
		if (o instanceof Double) return (Double)o;
		throw ex("Number", o, i);
	}

	public boolean getBool(int i) {
		if (i >= param.length) throw num(i);
		Object o = param[i];
		if (o instanceof Double) return (Boolean)o;
		throw ex("Boolean", o, i);
	}

	public Object[] getArray(int i) {
		if (i >= param.length) throw num(i);
		Object o = param[i];
		if (o instanceof Object[]) return (Object[])o;
		throw ex("Array", o, i);
	}

	public double[] getVector(int i) {
		if (i >= param.length) throw num(i);
		Object o = param[i];
		if (o instanceof double[]) return (double[])o;
		throw ex("Vector", o, i);
	}

	public <T> T get(int i, Class<T> c) {
		if (i >= param.length) throw num(i);
		Object o = param[i];
		if (c.isInstance(o)) return c.cast(o);
		throw ex(c.getSimpleName(), o, i);
	}

	public Object get(int i) {
		if (i >= param.length) throw num(i);
		return param[i];
	}

	public Object[] getArrayOrAll() {
		if (param.length != 1) return param;
		Object o = param[0];
		if(o instanceof Object[]) return (Object[])o;
		return param;
	}

	public double[] getVectorOrAll() {
		if (param.length == 1) {
			Object o = param[0];
			if (o instanceof double[]) return (double[])o;
		}
		double[] vec = new double[param.length];
		for (int i = 0; i < vec.length; i++)
			vec[i] = getNumber(i);
		return vec;
	}

	private IllegalArgumentException num(int pos) {
		return new IllegalArgumentException(String.format("Too few arguments, expected at least %d", pos + 1));
	}

	private IllegalArgumentException ex(String exp, Object got, int pos) {
		if (got instanceof Exception) {
			Exception e = (Exception)got;
			return new IllegalArgumentException(String.format("Evaluation of paramter %s @ %d returned an error:\n%s: %s", exp, pos, e.getClass().getSimpleName(), e.getMessage()), e);
		}
		return new IllegalArgumentException(String.format("expected %s @ %d , got %s", exp, pos, got == null ? "null" : got.getClass().getSimpleName()));
	}

}
