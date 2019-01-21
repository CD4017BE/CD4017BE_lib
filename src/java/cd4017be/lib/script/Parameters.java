package cd4017be.lib.script;

import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Vector;
import cd4017be.lib.script.obj.Error;

/**
 * Represents function parameters given to calls of script or module functions.
 * @author CD4017BE
 */
public class Parameters {

	public final IOperand[] param;

	public Parameters(IOperand... param) {
		this.param = param;
	}

	/**
	 * @param i parameter index
	 * @return whether the given parameter exists
	 */
	public boolean has(int i) {
		return param.length > i;
	}

	/**
	 * @param i parameter index
	 * @return string value of given parameter
	 */
	public String getString(int i) {
		if (i >= param.length) throw num(i);
		IOperand o = param[i];
		if (o instanceof Text) return ((Text)o).value;
		throw ex("String", o, i);
	}

	/**
	 * @param i parameter index
	 * @return nummeric value of given parameter
	 */
	public double getNumber(int i) {
		if (i >= param.length) throw num(i);
		return param[i].asDouble();
	}

	/**
	 * @param i parameter index
	 * @return int value of given parameter
	 */
	public int getIndex(int i) {
		if (i >= param.length) throw num(i);
		return param[i].asIndex();
	}

	/**
	 * @param i parameter index
	 * @return boolean value of given parameter
	 */
	public boolean getBool(int i) {
		if (i >= param.length) throw num(i);
		try {return param[i].asBool();}
		catch(Error e) {throw ex("Boolean", e, i);}
	}

	/**
	 * @param i parameter index
	 * @return object array value of given parameter
	 */
	public Object[] getArray(int i) {
		if (i >= param.length) throw num(i);
		IOperand o = param[i];
		if (o instanceof Array) return ((Array)o).value();
		throw ex("Array", o, i);
	}

	/**
	 * @param i parameter index
	 * @return double array value of given parameter
	 */
	public double[] getVector(int i) {
		if (i >= param.length) throw num(i);
		IOperand o = param[i];
		if (o instanceof Vector) return ((Vector)o).value;
		throw ex("Vector", o, i);
	}

	/**
	 * @param i parameter index
	 * @param c value type
	 * @return value of given parameter
	 */
	public <T> T get(int i, Class<T> c) {
		if (i >= param.length) throw num(i);
		Object o = param[i].value();
		if (c.isInstance(o)) return c.cast(o);
		throw ex(c.getSimpleName(), o, i);
	}

	/**
	 * @param i parameter index
	 * @return raw value of given parameter
	 */
	public Object get(int i) {
		if (i >= param.length) throw num(i);
		return param[i].value();
	}

	/**
	 * @param i parameter start index
	 * @return array value of given parameter or all remaining parameters listed as array
	 */
	public Object[] getArrayOrAll(int i) {
		IOperand[] p = param;
		int l = p.length;
		if (i > l) throw num(i);
		if (l - i == 1 && p[i] instanceof Array)
			return ((Array)p[i]).value();
		Object[] arr = new Object[l - i];
		for (int j = 0; i < l; i++, j++)
			arr[j] = p[i].value();
		return arr;
	}

	/**
	 * @param i parameter start index
	 * @return double array value of given parameter or all remaining parameters listed as double array
	 */
	public double[] getVectorOrAll(int i) {
		IOperand[] p = param;
		int l = p.length;
		if (i > l) throw num(i);
		if (l - i == 1 && p[i] instanceof Vector)
			return ((Vector)p[i]).value;
		double[] vec = new double[l - i];
		for (int j = 0; i < l; i++, j++)
			vec[j] = p[i].asDouble();
		return vec;
	}

	public Object[] getArrayOrAll() {return getArrayOrAll(0);}
	public double[] getVectorOrAll() {return getVectorOrAll(0);}

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
