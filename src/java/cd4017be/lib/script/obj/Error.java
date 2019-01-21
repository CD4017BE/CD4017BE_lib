package cd4017be.lib.script.obj;

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
	public boolean asBool() throws Error {
		throw new Error(this, "attempt to decide on errored value:");
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
	public IOperand addR(IOperand x) {
		return new Error(this, "ERROR + (" + x + ")");
	}

	@Override
	public IOperand addL(IOperand x) {
		return new Error(this, "(" + x + ") + ERROR");
	}

	@Override
	public IOperand subR(IOperand x) {
		return new Error(this, "ERROR - (" + x + ")");
	}

	@Override
	public IOperand subL(IOperand x) {
		return new Error(this, "(" + x + ") - ERROR");
	}

	@Override
	public IOperand mulR(IOperand x) {
		return new Error(this, "ERROR * (" + x + ")");
	}

	@Override
	public IOperand mulL(IOperand x) {
		return new Error(this, "(" + x + ") * ERROR");
	}

	@Override
	public IOperand divR(IOperand x) {
		return new Error(this, "ERROR / (" + x + ")");
	}

	@Override
	public IOperand divL(IOperand x) {
		return new Error(this, "(" + x + ") / ERROR");
	}

	@Override
	public IOperand modR(IOperand x) {
		return new Error(this, "ERROR % (" + x + ")");
	}

	@Override
	public IOperand modL(IOperand x) {
		return new Error(this, "(" + x + ") % ERROR");
	}

	@Override
	public IOperand neg() {
		return new Error(this, "-ERROR");
	}

	@Override
	public IOperand inv() {
		return new Error(this, "/ERROR");
	}

	@Override
	public IOperand grR(IOperand x) {
		return new Error(this, "ERROR > (" + x + ")");
	}

	@Override
	public IOperand grL(IOperand x) {
		return new Error(this, "ERROR < (" + x + ")");
	}

	@Override
	public IOperand nlsR(IOperand x) {
		return new Error(this, "ERROR >= (" + x + ")");
	}

	@Override
	public IOperand nlsL(IOperand x) {
		return new Error(this, "ERROR <= (" + x + ")");
	}

	@Override
	public IOperand and(IOperand x) {
		return new Error(this, "ERROR & (" + x + ")");
	}

	@Override
	public IOperand or(IOperand x) {
		return new Error(this, "ERROR | (" + x + ")");
	}

	@Override
	public IOperand nand(IOperand x) {
		return new Error(this, "ERROR ~& (" + x + ")");
	}

	@Override
	public IOperand nor(IOperand x) {
		return new Error(this, "ERROR ~| (" + x + ")");
	}

	@Override
	public IOperand xor(IOperand x) {
		return new Error(this, "ERROR ^ (" + x + ")");
	}

	@Override
	public IOperand xnor(IOperand x) {
		return new Error(this, "ERROR ~^ (" + x + ")");
	}

	@Override
	public IOperand len() {
		return new Error(this, "#ERROR");
	}

	@Override
	public IOperand get(IOperand idx) {
		return new Error(this, "ERROR:(" + idx + ")");
	}

	@Override
	public String toString() {
		return message;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(depth + ": " + message);
		if (depth >= MAX_TRACE_DEPTH)
			sb.append(MAX_TRACE_DEPTH - depth + 1).append(" more operations on ERROR ...");
		for (Error e = parent; e != null; e = e.parent)
			sb.append("\n").append(e.depth).append(": ").append(e.message);
		return sb.toString();
	}

	@Override
	public Object value() {
		return this;
	}

}
