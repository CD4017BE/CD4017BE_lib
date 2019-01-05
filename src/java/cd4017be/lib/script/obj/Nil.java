package cd4017be.lib.script.obj;

/**
 * 
 * @author cd4017be
 */
public class Nil implements IOperand {

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

}
