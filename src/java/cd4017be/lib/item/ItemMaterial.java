package cd4017be.lib.item;

import java.util.Arrays;
import java.util.HashMap;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * @author CD4017BE
 */
public class ItemMaterial extends BaseItem {

	public HashMap<Integer, Variant> variants = new HashMap<Integer, Variant>();

	public ItemMaterial(String id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	protected void init() {}

	@Override
	public String getUnlocalizedName(ItemStack item) {
		Variant name = variants.get(item.getItemDamage());
		return this.getUnlocalizedName() + (name == null ? "" : ":" + name);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		Variant name = variants.get(item.getItemDamage());
		if (name != null) return name.getLocName();
		return TooltipUtil.translate(this.getUnlocalizedName());
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> subItems) {
		subItems.add(new ItemStack(this, 1, 0));
		int[] ids = new int[variants.size()];
		int n = 0;
		for (int i : variants.keySet()) ids[n++] = i;
		Arrays.sort(ids);
		for (int i : ids) subItems.add(new ItemStack(item, 1, i));
	}

	public void addMaterial(int id, String name, String model, String locName) {
		if (id <= 0 || id >= 32768 || variants.containsKey(id)) throw new IllegalArgumentException("Id already occupied or out of range!");
		variants.put(id, new Variant(name, locName, model == null ? null : new ResourceLocation(model)));
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, id), this.getRegistryName().getResourcePath() + "." + name);
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

		public String getLocName() {
			if (locName != null) return locName;
			return TooltipUtil.translate(getUnlocalizedName() + ":" + name + ".name");
		}

		@Override
		public String toString() {
			return name;
		}

	}

}
