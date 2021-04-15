package cd4017be.api.indlog.filter;

import cd4017be.lib.util.IFilter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * The filter API for InductiveLogistic's pipe system.
 * @param <Obj> Resource to filter
 * @param <Inv> Inventory that provides that Resource
 * @see {@link FluidFilterProvider}, {@link ItemFilterProvider}
 * @author CD4017BE
 */
public interface PipeFilter<Obj, Inv> extends IFilter<Obj, Inv>, INBTSerializable<CompoundNBT> {

	/**
	 * @param obj resource to check
	 * @return whether this filter generally accepts the given resource
	 */
	boolean matches(Obj obj);

	/**
	 * @param rs current redstone signal
	 * @return whether external interaction is enabled
	 */
	boolean active(boolean rs);

	/**
	 * @return whether this filter might block on default routing (return false at {@link IFilter#transfer(Obj)})
	 */
	boolean blocking();

	/**
	 * @return whether this filter (when active) would act the same as if there was no filter at all
	 */
	boolean noEffect();

	/**
	 * @return connector priority (for Warp Pipes)
	 */
	byte priority();

	/**
	 * @return the Item providing this filter
	 */
	Item item();

	/**
	 * @return an ItemStack representing this filter
	 */
	default ItemStack getItemStack() {
		ItemStack stack = new ItemStack(item());
		stack.setTag(serializeNBT());
		return stack;
	}

	/**
	 * @return the NBT data representing this filter (must have the string-tag 'id' set to the item registry name)
	 */
	default CompoundNBT writeNBT() {
		CompoundNBT nbt = serializeNBT();
		ResourceLocation id = item().getRegistryName();
		if (id != null) nbt.putString("id", id.toString());
		return nbt;
	}

}
