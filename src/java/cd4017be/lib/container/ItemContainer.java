package cd4017be.lib.container;

import cd4017be.lib.container.slot.HidableSlot;
import cd4017be.lib.network.StateSyncAdv;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

/**Container template for hand-held item GUIs.
 * @author CD4017BE */
public class ItemContainer extends AdvancedContainer {

	protected final Item item;
	protected final int slot;

	/**@param type
	 * @param id unique GUI session id
	 * @param inv player's inventory
	 * @param slot player inventory slot holding the target item
	 * @param item expected target item (GUI closes if changed)
	 * @param sync server -> client data synchronization handler
	 * @param idxCount object indices available for slot synchronization */
	public ItemContainer(
		ContainerType<?> type, int id,
		PlayerInventory inv, int slot, Item item,
		StateSyncAdv sync, int idxCount
	) {
		super(type, id, inv, sync, idxCount);
		this.slot = slot;
		this.item = item;
	}

	/**@return the ItemStack currently in the slot that's interacted with through the GUI */
	protected ItemStack getStack() {
		return inv.getItem(slot);
	}

	/**@return the Compound tag of the stack (gets created if needed)
	 * @see #getStack() */
	protected CompoundNBT getNBT(PlayerEntity player) {
		ItemStack stack = getStack();
		if (stack.getItem() != item) return new CompoundNBT();
		CompoundNBT nbt = stack.getTag();
		if (nbt == null) stack.setTag(nbt = new CompoundNBT());
		return nbt;
	}

	@Override
	public boolean stillValid(PlayerEntity player) {
		return player.isAlive() && getStack().getItem() == item;
	}

	@Override
	public void addPlayerInventory(int x, int y, boolean armor) {
		super.addPlayerInventory(x, y, armor);
		int s = slot;
		if (armor || s < 36) {
			s = (s < 9 ? s + 27 : s - 9) + playerInvStart();
			((HidableSlot)getSlot(s)).lock();
		}
	}

}
