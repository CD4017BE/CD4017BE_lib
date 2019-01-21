package cd4017be.lib.script.obj;

import java.util.function.Predicate;

/**
 * 
 * @author cd4017be
 *
 */
public class PredicateWrap<T> implements IOperand {

	final Predicate<T> filter;
	final Class<T> type;

	public static PredicateWrap<Object> of(Predicate<Object> filter) {
		return new PredicateWrap<Object>(filter, Object.class);
	}

	public PredicateWrap(Predicate<T> filter, Class<T> type) {
		this.filter = filter;
		this.type = type;
	}

	@Override
	public boolean asBool() throws Error {
		return true;
	}

	@Override
	public Object value() {
		return filter;
	}

	@Override
	public IOperand grR(IOperand x) {
		Object obj = x.value();
		return type.isInstance(obj) && filter.test(type.cast(obj)) ? Number.TRUE : Number.FALSE;
	}

	@Override
	public IOperand grL(IOperand x) {
		return Number.FALSE;
	}

	@Override
	public IOperand nlsR(IOperand x) {
		Object obj = x.value();
		return filter.equals(obj) || type.isInstance(obj) && filter.test(type.cast(obj)) ? Number.TRUE : Number.FALSE;
	}

	@Override
	public IOperand nlsL(IOperand x) {
		return Number.FALSE;
	}

}
