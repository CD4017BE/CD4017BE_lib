package cd4017be.api.recipes;

import java.util.function.Predicate;

import cd4017be.lib.script.obj.FilteredIterator.FilterableIterator;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Text;
import net.minecraftforge.oredict.OreDictionary;

/**
 * 
 * @author cd4017be
 *
 */
public class OreDictList implements FilterableIterator {

	final String[] ores;
	int idx = 0;

	public OreDictList() {
		this.ores = OreDictionary.getOreNames();
	}

	@Override
	public void set(IOperand obj) {
	}

	@Override
	public boolean hasNext() {
		return idx < ores.length;
	}

	@Override
	public IOperand next() {
		return new Text(ores[idx++]);
	}

	@Override
	public Object value() {
		return ores;
	}

	@Override
	public IOperand next(Predicate<Object> filter) {
		int i = idx;
		for (int l = ores.length; i < l; i++) {
			String s = ores[i];
			if (filter.test(s)) {
				idx = i + 1;
				return new Text(ores[i]);
			}
		}
		idx = i;
		return null;
	}

}
