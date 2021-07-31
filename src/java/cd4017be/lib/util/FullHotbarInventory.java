package cd4017be.lib.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Special version of Inventory where all 36 main inventory slots can be selected as currently held item in main hand.
 * Used for FakePlayers.
 * @author CD4017BE */
public class FullHotbarInventory extends Inventory {

	public FullHotbarInventory(Player playerIn) {
		super(playerIn);
	}

	@Override
	public ItemStack getSelected() {
		return selected >= 0 && selected < items.size()
			? items.get(selected) : ItemStack.EMPTY;
	}

}
