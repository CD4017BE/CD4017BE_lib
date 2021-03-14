package cd4017be.lib.script.obj;

/**
 * Instance: {@link #NIL}
 * @author cd4017be
 */
public class Nil implements IOperand {

	/**
	 * The script specific null object.
	 */
	public static final Nil NIL = new Nil();
	private static final Text TEXT = new Text("nil");

	/**@return {@link #NIL} if o == null, otherwise o */
	public static IOperand of(IOperand o) {
		return o == null ? NIL : o;
	}

	private Nil() {}

	@Override
	public boolean asBool() {
		return false;
	}

	@Override
	public Object value() {
		return null;
	}

	@Override
	public IOperand op(int code) {
		switch(code) {
		case text:
			return TEXT;
		default:
			return IOperand.super.op(code);
		}
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		switch(code) {
		case gr:
			return Number.FALSE;
		case nls:
			return x == NIL ? Number.TRUE : Number.FALSE;
		default:
			return IOperand.super.opR(code, x);
		}
	}

	@Override
	public IOperand opL(int code, IOperand x) {
		switch(code) {
		case gr:
			return Number.TRUE;
		case nls:
			return Number.TRUE;
		default:
			return IOperand.super.opL(code, x);
		}
	}

	@Override
	public String toString() {
		return "nil";
	}

}
