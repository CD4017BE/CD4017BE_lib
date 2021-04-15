package cd4017be.lib.item;

import java.util.Arrays;
import java.util.HashMap;
import cd4017be.lib.text.TooltipUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * 
 * @author CD4017BE
 * @deprecated might not work anymore
 */
public class ItemMaterial extends DocumentedItem {

	public HashMap<Integer, Variant> variants = new HashMap<Integer, Variant>();

	public ItemMaterial(Properties p) {
		super(p);
		//this.setHasSubtypes(true);
	}

	@Override
	public String getTranslationKey(ItemStack item) {
		Variant name = variants.get(item.getDamage());
		return this.getTranslationKey() + (name == null ? "" : ":" + name);
	}

	@Override
	public ITextComponent getDisplayName(ItemStack item) {
		Variant name = variants.get(item.getDamage());
		if (name != null) return name.getLocName();
		return TooltipUtil.cTranslate(this.getTranslationKey());
	}

	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
		if (!isInGroup(group)) return;
		items.add(new ItemStack(this));
		int[] ids = new int[variants.size()];
		int n = 0;
		for (int i : variants.keySet()) ids[n++] = i;
		Arrays.sort(ids);
		for (int i : ids) {
			ItemStack stack = new ItemStack(this, 1);
			stack.setDamage(i);
			items.add(stack);
		}
	}

	public void addMaterial(int id, String name, String model, String locName) {
		if (id <= 0 || id >= 32768 || variants.containsKey(id)) throw new IllegalArgumentException("Id already occupied or out of range!");
		variants.put(id, new Variant(name, locName, model == null ? null : new ResourceLocation(model)));
	}

	public class Variant {

		public final String name, locName;
		public final ResourceLocation model;

		public Variant(String name, String locName, ResourceLocation model) {
			super();
			this.name = name;
			this.locName = locName;
			this.model = model != null ? model : new ResourceLocation(getRegistryName().toString() + "/" + name);
		}

		public ITextComponent getLocName() {
			if (locName != null) return TooltipUtil.convert(locName);
			return TooltipUtil.cTranslate(getTranslationKey() + ":" + name + ".name");
		}

		@Override
		public String toString() {
			return name;
		}

	}

}
