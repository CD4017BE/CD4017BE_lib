package cd4017be.lib.script.obj;

import javax.script.ScriptException;

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
	public IOperand op(int code) {
		switch(code) {
		case len: new Number(value.length());
		case text: return this;
		default: return IOperand.super.op(code);
		}
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		switch(code) {
		case add: return new Text(value.concat(x.toString()));
		case index: {
			int l = value.length(),
				i0 = x.asIndex(),
				i1 = l;
			if (x instanceof Vector) {
				Vector v = (Vector)x;
				if (v.value.length >= 2)
					i1 = (int)v.value[1];
			}
			return new Text(value.substring(i0 < 0 ? 0 : i0, i1 > l ? l : i1));
		}
		case gr:
			if (x instanceof Text) {
				String s = ((Text)x).value;
				return value.length() > s.length() ? new Number(value.indexOf(s) + 1) : Number.FALSE;
			} else break;
		case nls:
			if (x instanceof Text)
				return new Number(value.indexOf(((Text)x).value) + 1);
			else break;
		}
		return x.opL(code, this);
	}

	@Override
	public IOperand opL(int code, IOperand x) {
		switch(code) {
		case add: return new Text(x.toString().concat(value));
		default: return IOperand.super.opL(code, x);
		}
	}

	@Override
	public void call(IOperand[] stack, int bot, int top) throws ScriptException {
		Object[] args = new Object[top - bot];
		for (int i = 0; i < args.length; i++)
			args[i] = stack[bot + i].value();
		stack[bot - 1] = new Text(String.format(value, args));
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
	public String toString() {
		return value;
	}

	@Override
	public Object value() {
		return value;
	}

}
