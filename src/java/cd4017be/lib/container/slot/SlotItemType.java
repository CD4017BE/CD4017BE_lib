package cd4017be.lib.container.slot;

import java.util.function.ToIntFunction;

import net.minecraft.world.item.ItemStack;
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
				if (item.getItem() == comp.getItem())
					return comp.getCount();
			return 0;
		};
	}

	@Override
	public boolean mayPlace(ItemStack item) {
		return !item.isEmpty() && allowed.applyAsInt(item) > 0;
	}

	@Override
	public int getMaxStackSize(ItemStack item) {
		return allowed.applyAsInt(item);
	}

}
