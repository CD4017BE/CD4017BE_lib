package cd4017be.lib.script;

import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Vector;
import cd4017be.lib.script.obj.Error;

/**
 * 
 * @author CD4017BE
 */
public class Parameters {

	public final IOperand[] param;

	public Parameters(IOperand... param) {
		this.param = param;
	}

	public boolean has(int i) {
		return param.length > i;
	}

	public String getString(int i) {
		if (i >= param.length) throw num(i);
		IOperand o = param[i];
		if (o instanceof Text) return ((Text)o).value;
		throw ex("String", o, i);
	}

	public double getNumber(int i) {
		if (i >= param.length) throw num(i);
		return param[i].asDouble();
	}

	public int getIndex(int i) {
		if (i >= param.length) throw num(i);
		return param[i].asIndex();
	}

	public boolean getBool(int i) {
		if (i >= param.length) throw num(i);
		try {return param[i].asBool();}
		catch(Error e) {throw ex("Boolean", e, i);}
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
		Object o = param[i].value();
		if (c.isInstance(o)) return c.cast(o);
		throw ex(c.getSimpleName(), o, i);
	}

	public Object get(int i) {
		if (i >= param.length) throw num(i);
		return param[i].value();
	}

	public Object[] getArrayOrAll() {
		if (param.length != 1) return param;
		IOperand o = param[0];
		if(o instanceof Array) return (Object[])o.value();
		return new Array(param).value();
	}

	public double[] getVectorOrAll() {
		if (param.length == 1) {
			IOperand o = param[0];
			if (o instanceof Vector) return ((Vector)o).value;
		}
		double[] vec = new double[param.length];
		for (int i = 0; i < vec.length; i++)
			vec[i] = param[i].asDouble();
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
