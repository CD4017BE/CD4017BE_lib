package cd4017be.lib.util;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import cd4017be.api.recipes.ItemOperand;
import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Number;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Represents an OreDictionary entry with stacksize.
 * @author CD4017BE
 */
public class OreDictStack implements IOperand {

	public String id;
	public int ID;
	public int stacksize;

	public OreDictStack(String id, int n) {
		this.id = id;
		this.stacksize = n;
		this.ID = OreDictionary.getOreID(id);
	}

	public OreDictStack(int id, int n) {
		this.ID = id;
		this.id = OreDictionary.getOreName(id);
		this.stacksize = n;
	}

	public static OreDictStack deserialize(String s) {
		int p = s.indexOf('*');
		short n = 1;
		if (p > 0) {
			try {n = Short.parseShort(s.substring(0, p));} catch (NumberFormatException e){}
			s = s.substring(p + 1);
			if (n <= 0) n = 1;
		}
		if (s.isEmpty()) return null;
		return new OreDictStack(s, n);
	}

	public static OreDictStack[] get(ItemStack item) {
		if (item.isEmpty()) return null;
		int[] i = OreDictionary.getOreIDs(item);
		OreDictStack[] stacks = new OreDictStack[i.length];
		for (int j = 0; j < i.length; j++) stacks[j] = new OreDictStack(i[j], item.getCount());
		return stacks;
	}

	public boolean isEqual(ItemStack item) {
		if (item.isEmpty()) return false;
		for (int id : OreDictionary.getOreIDs(item))
			if (id == ID) return true;
		return false;
	}

	public ItemStack[] getItems() {
		List<ItemStack> list = OreDictionary.getOres(id);
		ItemStack[] items = new ItemStack[list.size()];
		int n = 0;
		for (ItemStack item : list) {
			items[n] = item.copy();
			items[n++].setCount(stacksize);
		}
		return items;
	}

	public ItemStack asItem() {
		List<ItemStack> list = OreDictionary.getOres(id);
		if (list.isEmpty()) return ItemStack.EMPTY;
		ItemStack item = list.get(0).copy();
		item.setCount(stacksize);
		return item;
	}

	public OreDictStack copy() {
		return new OreDictStack(ID, stacksize);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof IOperand)
			return this.equals((IOperand)obj);
		if (obj instanceof OreDictStack) {
			OreDictStack stack = (OreDictStack)obj;
			return stack.ID == ID;
		} else return false;
	}

	@Override
	public int hashCode() {
		return ID;
	}

	@Override
	public String toString() {
		return "ore:" + id + (stacksize != 1 ? "*" + stacksize : "");
	}

	@Override
	public IOperand mulR(IOperand x) {
		return new OreDictStack(ID, x.asIndex());
	}

	@Override
	public IOperand mulL(IOperand x) {
		return new OreDictStack(ID, x.asIndex());
	}

	@Override
	public IOperand len() {
		return new Number(stacksize);
	}

	@Override
	public IOperand grR(IOperand x) {
		if (x instanceof ItemOperand) {
			ItemStack stack = ((ItemOperand)x).stack;
			if (!stack.isEmpty()) {
				int id = ID;
				for (int o : OreDictionary.getOreIDs(stack))
					if (o == id)
						return Number.TRUE;
			}
			return Number.FALSE;
		} else if (x instanceof OreDictStack) {
			String ore = ((OreDictStack)x).id;
			if (id.equals(ore)) return Number.FALSE;
			List<ItemStack>
					itemsT = OreDictionary.getOres(id, false),
					itemsO = OreDictionary.getOres(ore, false);
			for (ItemStack stack : itemsT) {
				if (stack.isEmpty()) continue;
				int meta = stack.getMetadata();
				Item item = stack.getItem();
				boolean wildcard = meta == OreDictionary.WILDCARD_VALUE;
				boolean miss = true;
				for (ItemStack stackO : itemsO)
					if (stackO.getItem() == item && (wildcard || stackO.getMetadata() == meta)) {
						miss = false;
						break;
					}
				if (miss) return Number.FALSE;
			}
			return Number.TRUE;
		} else return x.grR(this);
	}

	@Override
	public IOperand grL(IOperand x) {
		if (x instanceof ItemOperand)
			return Number.FALSE;
		return IOperand.super.grL(x);
	}

	@Override
	public IOperand nlsR(IOperand x) {
		return x instanceof OreDictStack ? ((OreDictStack)x).id.equals(id) ? Number.TRUE : Number.FALSE : grR(x);
	}

	@Override
	public IOperand nlsL(IOperand x) {
		return x instanceof OreDictStack ? ((OreDictStack)x).id.equals(id) ? Number.TRUE : Number.FALSE : grR(x);
	}

	@Override
	public OperandIterator iterator() throws Error {
		return new ItemIterator(OreDictionary.getOres(id).iterator());
	}

	@Override
	public boolean asBool() throws Error {
		return true;
	}

	@Override
	public Object value() {
		return id;
	}

	public static class ItemIterator implements OperandIterator {

		final boolean canSet;
		final Iterator<ItemStack> items;

		public ItemIterator(Iterator<ItemStack> items) {
			this.items = items;
			this.canSet = false;
		}

		public ItemIterator(ListIterator<ItemStack> items) {
			this.items = items;
			this.canSet = true;
		}

		@Override
		public boolean hasNext() {
			return items.hasNext();
		}

		@Override
		public IOperand next() {
			return new ItemOperand(items.next());
		}

		@Override
		public Object value() {
			return items;
		}

		@Override
		public void set(IOperand obj) {
			if (canSet && obj instanceof ItemOperand)
				((ListIterator<ItemStack>)items).set(((ItemOperand)obj).stack);
		}

	}

}
