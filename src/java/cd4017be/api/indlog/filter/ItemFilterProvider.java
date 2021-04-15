package cd4017be.api.indlog.filter;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

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

	public static PipeFilter<ItemStack, IItemHandler> load(CompoundNBT nbt) {
		Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("id")));
		if (item instanceof ItemFilterProvider) {
			PipeFilter<ItemStack, IItemHandler> filter = ((ItemFilterProvider)item).getItemFilter(null);
			if (filter != null) filter.deserializeNBT(nbt);
			return filter;
		} else return null;
	}

}
