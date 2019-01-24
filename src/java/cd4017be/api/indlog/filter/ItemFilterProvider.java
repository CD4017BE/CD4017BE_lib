package cd4017be.api.indlog.filter;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.IItemHandler;

/**
 * Implemented by Items that should act as filter for items.
 * @author cd4017be
 */
public interface ItemFilterProvider {

	/**
	 * @param stack the actual stack representing the filter or null if created directly from NBT
	 * @return a new filter instance provided by this item
	 */
	PipeFilter<ItemStack, IItemHandler> getItemFilter(@Nullable ItemStack stack);

	public static PipeFilter<ItemStack, IItemHandler> load(NBTTagCompound nbt) {
		Item item;
		if (nbt.hasKey("id", NBT.TAG_STRING)) item = Item.getByNameOrId(nbt.getString("id"));
		else item = Item.getByNameOrId("indlog:item_filter"); //backward compatibility
		if (item instanceof ItemFilterProvider) {
			PipeFilter<ItemStack, IItemHandler> filter = ((ItemFilterProvider)item).getItemFilter(null);
			if (filter != null) filter.deserializeNBT(nbt);
			return filter;
		} else return null;
	}

}
