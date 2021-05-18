package cd4017be.lib.script.obj;

import static cd4017be.lib.script.Parser.OP_NAMES;

import javax.script.ScriptException;

/**
 * This interface represents the script API for performing operations on values.
 * @author cd4017be
 */
public interface IOperand {

	public static final byte 
	eq = 0, neq = 1, xor = 2, xnor = 3, ls = 4, nls = 5, gr = 6, ngr = 7,
	and = 8, nand = 9, or = 10, nor = 11, add = 12, sub = 13, mul = 14, div = 15,
	mod = 16, pow = 17, lsh = 18, rsh = 19, index = 20, ref = 21, len = 22, text = 23;

	/**
	 * @return a "copied" instance that won't allow changes to the state of this operand through operations other than {@link #put(IOperand, IOperand)}.
	 */
	default IOperand onCopy() {return this;}

	/**@return this operand expressed as boolean (for use in conditional branches) */
	boolean asBool();
	/**
	 * @return this operand expressed as int (for use in array/list indexing)
	 */
	default int asIndex() {return -1;}
	/**
	 * @return this operand expressed as double (for use in nummerical operations)
	 */
	default double asDouble() {return Double.NaN;}
	/**
	 * @return the regular java object value this operand represents
	 */
	default Object value() {return this;}

	/**
	 * @return whether the state of this operand represents an error
	 */
	default boolean isError() {return false;}

	/**@param code unary prefix operator code
	 * @return op(this) */
	default IOperand op(int code) {
		return new Error("undefined " + OP_NAMES[code] + this);
	}

	/**@param code binary operator code
	 * @param x right-hand operand
	 * @return op(this, x) */
	default IOperand opR(int code, IOperand x) {
		return x.opL(code, this);
	}

	/**Fallback if {@link #opR x.opR(code, this)} not supported.
	 * @param code binary operator code
	 * @param x left-hand operand
	 * @return op(x, this) */
	default IOperand opL(int code, IOperand x) {
		return new Error("undefined " + x + " " + OP_NAMES[code] + " " + this);
	}

	/**
	 * this:idx = val
	 * @param idx index operand
	 * @param val assigned value
	 */
	default void put(IOperand idx, IOperand val) {}

	default IOperand get(String member) {
		return new Error(this + " has no member " + member);
	}

	default void set(String member, IOperand va) {}

	/**Call this operand as a function and store return value at stack[bot - 1]
	 * @param stack current operand stack
	 * @param bot stack index of first argument
	 * @param top stack index after last argument
	 * @throws ScriptException */
	default void call(IOperand[] stack, int bot, int top) throws ScriptException {
		throw new ScriptException(this + " is not callable!");
	}
	/**
	 * @return an iterator for this object (for use in loops)
	 * @throws Error if iteration not possible
	 */
	default OperandIterator iterator() throws Error {return NULL_IT;}

	/**
	 * Operand specific version of {@link Object#equals(Object)}
	 * @param obj right hand side operand
	 * @return this == obj
	 */
	default boolean equals(IOperand obj) {
		Object o1 = value(), o2 = obj.value();
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	/**
	 * For iteration over IOperands in script loops.
	 * @author cd4017be
	 */
	interface OperandIterator extends IOperand {

		@Override
		default boolean asBool() {
			return hasNext();
		}

		boolean hasNext();
		IOperand next();
		/**
		 * replaces the value last returned by {@link #next()} with the given value
		 * @param obj new operand value
		 */
		void set(IOperand obj);

		/**
		 * resets this iterator back to start (for reuse)
		 */
		default void reset() {}

	}

	/**
	 * Default operand iterator that immediately finishes before returning any elements
	 */
	public static final OperandIterator NULL_IT = new OperandIterator() {
		@Override
		public IOperand next() {return null;}
		@Override
		public boolean hasNext() {return false;}
		@Override
		public void set(IOperand obj) {}
		@Override
		public Object value() {return null;}
	};

	@FunctionalInterface
	public interface IFunction extends IOperand {

		@Override
		default boolean asBool() {
			return true;
		}

		@Override
		void call(IOperand[] stack, int bot, int top) throws ScriptException;
	}


	public static double[] vec(IOperand[] stack, int bot, int top, int len) throws ScriptException {
		double[] v;
		if (top - bot == 1 && stack[bot] instanceof Vector)
			v = ((Vector)stack[bot]).value;
		else if (top >= bot) {
			v = new double[top - bot];
			for (int i = 0; bot < top; i++, bot++)
				v[i] = stack[bot].asDouble();
		} else throw new ScriptException("negative arguments available for vector");
		if (len >= 0 && v.length != len)
			throw new ScriptException("expected vector of length " + len + ", got " + v.length);
		return v;
	}

	public static void check(int n, int min, int max) throws ScriptException {
		if (n < min) throw new ScriptException("too few arguments provided: got " + n + ", expected at least " + min);
		if (n > max) throw new ScriptException("too many arguments provided: got " + n + ", expected at most " + max);
	}

	public static <T extends IOperand> T get(IOperand[] stack, int bot, int i, Class<T> type) throws ScriptException {
		try {
			return type.cast(stack[bot + i]);
		} catch(ClassCastException e) {
			throw new ScriptException("expected " + type.getSimpleName() + " @ arg " + i + ", got " + stack[bot + i]);
		}
	}
}
