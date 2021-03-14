package cd4017be.lib.script.obj;

import static cd4017be.lib.script.Parser.OP_NAMES;

import javax.annotation.Nonnull;

/**
 * Used to represents execution errors in the script.<br>
 * Usually rather returned as operation result than thrown.
 * @author cd4017be
 */
@SuppressWarnings("serial")
public class Error extends Exception implements IOperand {

	private static final int MAX_TRACE_DEPTH = 12;

	public final int depth;
	public final Error parent;
	public String message;

	public static Error of(Exception e) {
		if (e instanceof Error) return (Error)e;
		else return new Error(e.toString());
	}

	public Error(String message) {
		this.depth = 0;
		this.parent = null;
		this.message = message == null ? "" : message;
	}

	public Error(@Nonnull Error parent, String message) {
		if ((this.depth = parent.depth + 1) < MAX_TRACE_DEPTH)
			this.parent = parent;
		else this.parent = parent.parent;
		this.message = message == null ? "" : message;
	}

	/**
	 * change the error message
	 * @param message new error message
	 * @return this
	 */
	public Error reset(String message) {
		this.message = message;
		return this;
	}

	@Override
	public boolean asBool() {
		return false;
	}

	@Override
	public OperandIterator iterator() throws Error {
		throw new Error(this, "attempt to iterate over errored value:");
	}

	@Override
	public boolean isError() {
		return true;
	}

	@Override
	public IOperand op(int code) {
		return new Error(this, OP_NAMES[code] + "ERROR");
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		return new Error(this, "ERROR" + OP_NAMES[code] + "(" + x + ")");
	}

	@Override
	public IOperand opL(int code, IOperand x) {
		return new Error(this, "(" + x + ")" + OP_NAMES[code] + "ERROR");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(depth + ": " + message);
		if (depth >= MAX_TRACE_DEPTH)
			sb.append(MAX_TRACE_DEPTH - depth + 1).append(" more operations on ERROR ...");
		for (Error e = parent; e != null; e = e.parent)
			sb.append("\n").append(e.depth).append(": ").append(e.message);
		return sb.toString();
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public Object value() {
		return this;
	}

}
