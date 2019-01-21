package cd4017be.lib.script.obj;

/**
 * Represents Strings in script
 * @author cd4017be
 */
public class Text implements IOperand {

	public String value;

	public Text(String value) {
		this.value = value;
	}

	public Text(String format, IOperand arg) {
		if (arg instanceof Array)
			this.value = String.format(format, (Object[])arg.value());
		else this.value = String.format(format, arg.value());
	}

	@Override
	public boolean asBool() {
		return !value.isEmpty();
	}

	@Override
	public int asIndex() {
		return 0;
	}

	@Override
	public IOperand addR(IOperand x) {
		return new Text(value + x.toString());
	}

	@Override
	public IOperand addL(IOperand x) {
		return new Text(x.toString() + value);
	}

	@Override
	public IOperand len() {
		return new Number(value.length());
	}

	@Override
	public IOperand get(IOperand idx) {
		int l = value.length(),
			i0 = idx.asIndex(),
			i1 = l;
		if (idx instanceof Vector) {
			Vector v = (Vector)idx;
			if (v.value.length >= 2)
				i1 = (int)v.value[1];
		}
		return new Text(value.substring(i0 < 0 ? 0 : i0, i1 > l ? l : i1));
	}

	@Override
	public void put(IOperand idx, IOperand val) {
		int l = value.length(),
			i0 = idx.asIndex(),
			i1 = i0;
		if (idx instanceof Vector) {
			Vector v = (Vector)idx;
			if (v.value.length >= 2)
				i1 = (int)v.value[1];
		}
		if (i0 < 0) i0 = 0;
		if (i1 > l) i1 = l;
		value = value.substring(0, i0).concat(val.toString()).concat(value.substring(i1));
	}

	@Override
	public IOperand grR(IOperand x) {
		if (x instanceof Text) {
			String s = ((Text)x).value;
			return value.length() > s.length() ? new Number(value.indexOf(s) + 1) : Number.FALSE;
		} else return x.grL(x);
	}

	@Override
	public IOperand nlsR(IOperand x) {
		if (x instanceof Text)
			return new Number(value.indexOf(((Text)x).value) + 1);
		else return x.nlsL(x);
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public Object value() {
		return value;
	}

}
