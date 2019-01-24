package cd4017be.lib.script.obj;

import java.util.Iterator;

/**
 * This interface represents the script API for performing operations on values.
 * @author cd4017be
 */
public interface IOperand {

	/**
	 * @return a "copied" instance that won't allow changes to the state of this operand through operations other than {@link #put(IOperand, IOperand)}.
	 */
	default IOperand onCopy() {return this;}

	/**
	 * @return this operand expressed as boolean (for use in conditional branches)
	 * @throws Error if this operand can't be expressed as boolean.
	 */
	boolean asBool() throws Error;
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
	Object value();

	/**
	 * @return whether the state of this operand represents an error
	 */
	default boolean isError() {return false;}

	/**
	 * @param x right hand side operand
	 * @return this + x (delegates to {@code x.addL(this)} if not defined)
	 */
	default IOperand addR(IOperand x) {return x.addL(this);}
	/**
	 * @param x left hand side operand
	 * @return x + this
	 */
	default IOperand addL(IOperand x) {return new Error("undefined " + x + " + " + this);}

	/**
	 * @param x right hand side operand
	 * @return this - x (delegates to {@code x.subL(this)} if not defined)
	 */
	default IOperand subR(IOperand x) {return x.subL(this);}
	/**
	 * @param x left hand side operand
	 * @return x - this
	 */
	default IOperand subL(IOperand x) {return new Error("undefined " + x + " - " + this);}

	/**
	 * @param x right hand side operand
	 * @return this * x (delegates to {@code x.mulL(this)} if not defined)
	 */
	default IOperand mulR(IOperand x) {return x.mulL(this);}
	/**
	 * @param x left hand side operand
	 * @return x * this
	 */
	default IOperand mulL(IOperand x) {return new Error("undefined " + x + " * " + this);}

	/**
	 * @param x right hand side operand
	 * @return this / x (delegates to {@code x.divL(this)} if not defined)
	 */
	default IOperand divR(IOperand x) {return x.divL(this);}
	/**
	 * @param x left hand side operand
	 * @return x / this
	 */
	default IOperand divL(IOperand x) {return new Error("undefined " + x + " / " + this);}

	/**
	 * @param x right hand side operand
	 * @return this % x (delegates to {@code x.modL(this)} if not defined)
	 */
	default IOperand modR(IOperand x) {return x.modL(this);}
	/**
	 * @param x left hand side operand
	 * @return x % this
	 */
	default IOperand modL(IOperand x) {return new Error("undefined " + x + " % " + this);}

	/**
	 * @param x right hand side operand
	 * @return this ^ x (delegates to {@code x.powL(this)} if not defined)
	 */
	default IOperand powR(IOperand x) {return x.powL(this);}
	/**
	 * @param x left hand side operand
	 * @return x ^ this
	 */
	default IOperand powL(IOperand x) {return new Error("undefined " + x + " % " + this);}

	/**
	 * @param x right hand side operand
	 * @return this > x (delegates to {@code x.grL(this)} if not defined)
	 */
	default IOperand grR(IOperand x) {return x.grL(this);}
	/**
	 * @param x left hand side operand
	 * @return x > this
	 */
	default IOperand grL(IOperand x) {return Number.FALSE;}

	/**
	 * @param x right hand side operand
	 * @return this >= x (delegates to {@code x.nlsL(this)} if not defined)
	 */
	default IOperand nlsR(IOperand x) {return x.nlsL(this);}
	/**
	 * @param x left hand side operand
	 * @return x >= this
	 */
	default IOperand nlsL(IOperand x) {return equals(x) ? Number.TRUE : Number.FALSE;}

	/**
	 * @param x right hand side operand
	 * @return this & x
	 */
	default IOperand and(IOperand x) {
		try { return asBool() && x.asBool() ? Number.TRUE : Number.FALSE;}
		catch (Error e) {return e.reset(this + " & " + x);}
	}
	/**
	 * @param x right hand side operand
	 * @return this | x
	 */
	default IOperand or(IOperand x) {
		try { return asBool() || x.asBool() ? Number.TRUE : Number.FALSE;}
		catch (Error e) {return e.reset(this + " | " + x);}
	}
	/**
	 * @param x right hand side operand
	 * @return this ~& x
	 */
	default IOperand nand(IOperand x) {
		try { return asBool() && x.asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return e.reset(this + " ~& " + x);}
	}
	/**
	 * @param x right hand side operand
	 * @return this ~| x
	 */
	default IOperand nor(IOperand x) {
		try { return asBool() || x.asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return e.reset(this + " ~| " + x);}
	}
	/**
	 * @param x right hand side operand
	 * @return this ? x
	 */
	default IOperand xor(IOperand x) {
		try { return asBool() ^ x.asBool() ? Number.TRUE : Number.FALSE;}
		catch (Error e) {return e.reset(this + " ^ " + x);}
	}
	/**
	 * @param x right hand side operand
	 * @return this ~? x
	 */
	default IOperand xnor(IOperand x) {
		try { return asBool() && x.asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return e.reset(this + " ~^ " + x);}
	}

	/**
	 * @return -this
	 */
	default IOperand neg() {return new Error("undefined -" + this);}
	/**
	 * @return /this
	 */
	default IOperand inv() {return new Error("undefined /" + this);}
	/**
	 * @return ~this
	 */
	default IOperand not() {
		try {return asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return new Error("undefined ~" + this);}
	}
	/**
	 * @return #this
	 */
	default IOperand len() {return new Error("undefined #" + this);}

	/**
	 * @param idx index operand
	 * @return this:idx
	 */
	default IOperand get(IOperand idx) {return new Error("undefined " + this + ":" + idx);}
	/**
	 * this:idx = val
	 * @param idx index operand
	 * @param val assigned value
	 */
	default void put(IOperand idx, IOperand val) {}
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
	public interface OperandIterator extends Iterator<IOperand>, IOperand {

		@Override
		default boolean asBool() {
			return hasNext();
		}

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
}
