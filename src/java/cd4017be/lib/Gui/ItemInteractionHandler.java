package cd4017be.lib.Gui;

import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.network.StateSynchronizer.Builder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;


/**
 * Template class to handle GUI state synchronization for ItemStacks
 * @author CD4017BE
 */
public abstract class ItemInteractionHandler implements IStateInteractionHandler {

	/**the expected type of item in the slot */
	protected final Item item;
	/**slot in the player inventory the GUI interacts with */
	protected final int slot;

	/**
	 * @param item the expected type of item in slot
	 * @param slot the player inventory slot the GUI will interact with
	 */
	public ItemInteractionHandler(Item item, int slot) {
		this.item = item;
		this.slot = slot;
	}

	/**
	 * @param player reference to the player
	 * @return a new Container instance to open a GUI with
	 */
	public AdvancedContainer createContainer(EntityPlayer player) {
		Builder sb = StateSynchronizer.builder();
		initSync(sb);
		return new AdvancedContainer(this, sb.build(player.world.isRemote), player);
	}

	/**
	 * initialize variables to synchronize server -> client
	 * @param sb the synchronizer to register in
	 */
	protected abstract void initSync(Builder sb);

	/**
	 * @param player reference to the player
	 * @return the ItemStack currently in the slot that's interacted with through the GUI
	 */
	public ItemStack getStack(EntityPlayer player) {
		return player.inventory.getStackInSlot(slot);
	}

	/**
	 * @param player reference to the player
	 * @return the Compound tag of the stack (gets created if needed)
	 * @see #getStack(EntityPlayer)
	 */
	public NBTTagCompound getNBT(EntityPlayer player) {
		ItemStack stack = getStack(player);
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		return nbt;
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return !player.isDead && getStack(player).getItem() == item;
	}

}
