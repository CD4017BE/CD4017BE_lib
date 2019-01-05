package cd4017be.lib.script.obj;

import java.util.Iterator;

/**
 * 
 * @author cd4017be
 */
public interface IOperand {

	default IOperand onCopy() {return this;}
	boolean asBool() throws Error;
	default int asIndex() {return -1;}
	default double asDouble() {return Double.NaN;}
	default boolean isError() {return false;}
	
	default IOperand addR(IOperand x) {return x.addL(this);}
	default IOperand addL(IOperand x) {return new Error("undefined " + x + " + " + this);}
	default IOperand subR(IOperand x) {return x.subL(this);}
	default IOperand subL(IOperand x) {return new Error("undefined " + x + " - " + this);}
	default IOperand mulR(IOperand x) {return x.mulL(this);}
	default IOperand mulL(IOperand x) {return new Error("undefined " + x + " * " + this);}
	default IOperand divR(IOperand x) {return x.divL(this);}
	default IOperand divL(IOperand x) {return new Error("undefined " + x + " / " + this);}
	default IOperand modR(IOperand x) {return x.modL(this);}
	default IOperand modL(IOperand x) {return new Error("undefined " + x + " % " + this);}
	default IOperand ls(IOperand x) {return new Error("undefined " + this + " < " + x);}
	default IOperand nls(IOperand x) {return new Error("undefined " + this + " >= " + x);}
	default IOperand gr(IOperand x) {return new Error("undefined " + this + " > " + x);}
	default IOperand ngr(IOperand x) {return new Error("undefined " + this + " <= " + x);}
	default IOperand and(IOperand x) {
		try { return asBool() && x.asBool() ? Number.TRUE : Number.FALSE;}
		catch (Error e) {return e.reset(this + " & " + x);}
	}
	default IOperand or(IOperand x) {
		try { return asBool() || x.asBool() ? Number.TRUE : Number.FALSE;}
		catch (Error e) {return e.reset(this + " | " + x);}
	}
	default IOperand nand(IOperand x) {
		try { return asBool() && x.asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return e.reset(this + " ~& " + x);}
	}
	default IOperand nor(IOperand x) {
		try { return asBool() || x.asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return e.reset(this + " ~| " + x);}
	}
	default IOperand xor(IOperand x) {
		try { return asBool() ^ x.asBool() ? Number.TRUE : Number.FALSE;}
		catch (Error e) {return e.reset(this + " ^ " + x);}
	}
	default IOperand xnor(IOperand x) {
		try { return asBool() && x.asBool() ? Number.FALSE : Number.TRUE;}
		catch (Error e) {return e.reset(this + " ~^ " + x);}
	}
	default IOperand neg() {return new Error("undefined -" + this);}
	default IOperand inv() {return new Error("undefined /" + this);}
	default IOperand len() {return new Error("undefined #" + this);}
	default IOperand get(IOperand idx) {return new Error("undefined " + this + ":" + idx);}
	default void put(IOperand idx, IOperand val) {}
	default OperandIterator iterator() throws Error {return NULL_IT;}
	Object value();
	default boolean equals(IOperand obj) {
		Object o1 = value(), o2 = obj.value();
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	public interface OperandIterator extends Iterator<IOperand>, IOperand {

		@Override
		default boolean asBool() {
			return hasNext();
		}

		void set(IOperand obj);

		default void reset() {}

	}

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
