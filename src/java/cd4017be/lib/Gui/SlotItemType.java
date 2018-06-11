package cd4017be.lib.Gui;

import java.util.function.ToIntFunction;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 *
 * @author CD4017BE
 */
public class SlotItemType extends SlotItemHandler {

	private final ToIntFunction<ItemStack> allowed;

	public SlotItemType(IItemHandler inv, int id, int x, int y, ToIntFunction<ItemStack> allowed) {
		super(inv, id, x, y);
		this.allowed = allowed;
	}

	public SlotItemType(IItemHandler inv, int id, int x, int y, ItemStack... allowed) {
		super(inv, id, x, y);
		this.allowed = (item) -> {
			for (ItemStack comp : allowed)
				if (item.getItem() == comp.getItem() && !(item.getHasSubtypes() && item.getMetadata() != comp.getMetadata()))
					return comp.getCount();
			return 0;
		};
	}

	@Override
	public boolean isItemValid(ItemStack item) {
		return !item.isEmpty() && allowed.applyAsInt(item) > 0;
	}

	@Override
	public int getItemStackLimit(ItemStack item) {
		return allowed.applyAsInt(item);
	}

}
