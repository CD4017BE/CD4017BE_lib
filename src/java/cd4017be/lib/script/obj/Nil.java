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
	public IOperand grR(IOperand x) {
		return Number.FALSE;
	}

	@Override
	public IOperand grL(IOperand x) {
		return Number.TRUE;
	}

	@Override
	public IOperand nlsR(IOperand x) {
		return x == NIL ? Number.TRUE : Number.FALSE;
	}

	@Override
	public IOperand nlsL(IOperand x) {
		return Number.TRUE;
	}

	@Override
	public String toString() {
		return "nil";
	}

}
