package cd4017be.lib.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/** Special version of PlayerInventory where all 36 main inventory slots can be selected as currently held item in main hand.
 * Used for FakePlayers.
 * @author CD4017BE */
public class FullHotbarInventory extends PlayerInventory {

	public FullHotbarInventory(PlayerEntity playerIn) {
		super(playerIn);
	}

	@Override
	public ItemStack getSelected() {
		return selected >= 0 && selected < items.size()
			? items.get(selected) : ItemStack.EMPTY;
	}

}
