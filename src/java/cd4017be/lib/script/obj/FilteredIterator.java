package cd4017be.lib.script.obj;

import java.util.function.Predicate;

import cd4017be.lib.script.obj.IOperand.OperandIterator;

/**
 * 
 * @author cd4017be
 *
 */
public class FilteredIterator implements OperandIterator {

	final OperandIterator it;
	final Predicate<Object> filter;
	IOperand last;

	public FilteredIterator(OperandIterator it, Predicate<Object> filter) {
		this.it = it;
		this.filter = filter;
	}

	@Override
	public boolean hasNext() {
		if (it instanceof FilterableIterator) {
			FilterableIterator it = (FilterableIterator)this.it;
			return (last = it.next(filter)) != null;
		}
		while(it.hasNext()) {
			IOperand next = it.next();
			if (filter.test(next.value())) {
				last = next;
				return true;
			}
		}
		return false;
	}

	@Override
	public IOperand next() {
		return last;
	}

	@Override
	public Object value() {
		return this;
	}

	@Override
	public void set(IOperand obj) {
		it.set(obj);
	}

	public interface FilterableIterator extends OperandIterator {
		IOperand next(Predicate<Object> filter);
	}

}
