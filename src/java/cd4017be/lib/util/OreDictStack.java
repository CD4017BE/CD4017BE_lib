package cd4017be.lib.util;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class OreDictStack {
	
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
		if (item == null || item.getItem() == null) return null;
		int[] i = OreDictionary.getOreIDs(item);
		OreDictStack[] stacks = new OreDictStack[i.length];
		for (int j = 0; j < i.length; j++) stacks[j] = new OreDictStack(i[j], item.getCount());
		return stacks;
	}
	
	public boolean isEqual(ItemStack item) {
		if (item == null || item.getItem() == null) return false;
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
		if (list.isEmpty()) return null;
		ItemStack item = list.get(0).copy();
		item.setCount(stacksize);
		return item;
	}

	public OreDictStack copy() {
		return new OreDictStack(ID, stacksize);
	}

	/**
	 * Symmetric and transitive conditions only matched if given obj is a OreDictStack!<br>
	 * Otherwise also checks match against ItemStack and String objects
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		else if (obj instanceof OreDictStack) {
			OreDictStack stack = (OreDictStack)obj;
			return stack.ID == ID && stack.stacksize == stacksize;
		} else if (obj instanceof ItemStack) {
			ItemStack stack = (ItemStack)obj;
			return isEqual(stack) && stack.getCount() == stacksize;
		} else if (obj instanceof String) {
			return id.equals(obj);
		} else return false;
	}

}
