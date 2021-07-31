package cd4017be.lib.container;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface IQuickTransferHandler {

	/**
	 * called to perform quick move on an item from the players inventory to the machine (or whatever).
	 * @param stack the stack to move
	 * @param cont the container
	 * @return whether transfer was successful
	 */
	boolean transfer(ItemStack stack, AdvancedContainer cont);
}