package cd4017be.lib.Gui;

import java.util.ArrayList;
import java.util.List;

import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.network.IPlayerPacketReceiver;
import cd4017be.lib.network.IServerPacketReceiver;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.util.Utils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;


/**
 * A general purpose container implementation that delegates slot interaction to slots and handlers.<br>
 * Utilizes {@link StateSynchronizer} for server -> client synchronization.
 * @author CD4017BE
 */
public class AdvancedContainer extends Container implements IServerPacketReceiver, IPlayerPacketReceiver {

	public final IStateInteractionHandler handler;
	public final StateSynchronizer sync;
	public final EntityPlayer player;
	private final IntArrayList slotsToSync;
	public final ArrayList<IQuickTransferHandler> transferHandlers;
	private boolean sorted = true, hardInvUpdate = false;
	private int playerInvS, playerInvE;

	/**
	 * @param handler the StateInteractionHandler
	 * @param sync an instance of {@link StateSyncClient} on client side or {@link StateSyncServer} on server side
	 * @param player
	 */
	public AdvancedContainer(IStateInteractionHandler handler, StateSynchronizer sync, EntityPlayer player) {
		this.handler = handler;
		this.sync = sync;
		this.player = player;
		this.slotsToSync = new IntArrayList();
		this.transferHandlers = new ArrayList<>();
	}

	@Override
	public void detectAndSendChanges() {
		if (player.world.isRemote) {
			super.detectAndSendChanges();
			return;
		}
		StateSyncServer sss = (StateSyncServer)sync;
		sss.buffer.clear().writeInt(windowId);
		sss.setHeader();
		handler.writeState(sss.begin(), this);
		writeItems(sss);
		PacketBuffer pkt = sss.encodePacket();
		if (pkt != null)
			GuiNetworkHandler.GNH_INSTANCE.sendToPlayer(pkt, (EntityPlayerMP)player);
	}

	private void writeItems(StateSyncServer sss) {
		if (hardInvUpdate && player instanceof EntityPlayerMP) {
			((EntityPlayerMP)player).isChangingQuantityOnly = false;
			hardInvUpdate = false;
		}
		if (slotsToSync.isEmpty()) {
			super.detectAndSendChanges();
			return;
		}
		int n = slotsToSync.size(), o = sss.count - n;
		int[] syncList = slotsToSync.elements();
		if (!sorted) {
			IntArrays.quickSort(syncList, 0, n);
			sorted = true;
		}
		boolean init = sss.sendAll;
		for (int i = 0, l = inventorySlots.size(); i < l; i++) {
			Slot slot = inventorySlots.get(i);
			ItemStack itemN = slot.getStack();
			ItemStack itemO = inventoryItemStacks.get(i);
			send: {
				if (ItemStack.areItemStacksEqual(itemO, itemN)) break send;
				boolean send = !ItemStack.areItemStacksEqualUsingNBTShareTag(itemN, itemO);
				itemO = itemN.isEmpty() ? ItemStack.EMPTY : itemN.copy();
				inventoryItemStacks.set(i, itemO);
				if (!send) break send;
				int p = IntArrays.binarySearch(syncList, 0, n, i);
				if (p >= 0) sss.set(p + o, itemO);
				else for (IContainerListener listener : this.listeners)
					listener.sendSlotContents(this, i, itemO);
				continue;
			}
			if (init) {
				int p = IntArrays.binarySearch(syncList, 0, n, i);
				if (p >= 0) sss.set(p + o, itemN);
			}
		}
	}

	@Override
	public void addListener(IContainerListener listener) {
		if (listener instanceof EntityPlayerMP)
			((StateSyncServer)sync).setInitPkt();
		super.addListener(listener);
	}

	public void addPlayerInventory(int x, int y) {
		this.addPlayerInventory(x, y, false, false);
	}

	public void addPlayerInventory(int x, int y, boolean armor, boolean lockSel) {
		playerInvS = this.inventorySlots.size();
		playerInvE = playerInvS + (armor ? 41 : 36);
		InventoryPlayer inv = player.inventory;
		for (int i = 0; i < 3; i++) 
			for (int j = 0; j < 9; j++)
				this.addSlotToContainer(new HidableSlot(inv, i * 9 + j + 9, x + j * 18, y + i * 18));
		for (int i = 0; i < 9; i++)
			if (lockSel && i == inv.currentItem)
				this.addSlotToContainer(new LockedSlot(inv, i, x + i * 18, y + 58));
			else this.addSlotToContainer(new HidableSlot(inv, i, x + i * 18, y + 58));
		if (armor) {
			this.addSlotToContainer(new SlotOffhand(inv, 40, x - 18, y + 58));
			for (int i = 0; i < 4; i++)
				this.addSlotToContainer(new SlotArmor(inv, i + 36, x - 18, y - i * 18 + 36, EntityEquipmentSlot.values()[i + 2]));
		}
	}

	public void addItemSlot(Slot slot, boolean sync) {
		if (sync) {
			slotsToSync.add(inventorySlots.size());
			sorted = false;
			if (player.world.isRemote) {
				//TODO set client side slot to empty
			}
		}
		this.addSlotToContainer(slot);
	}

	@Override
	public void putStackInSlot(int slotID, ItemStack stack) {
		if (slotsToSync.contains(slotID)) return;
		Slot slot = inventorySlots.get(slotID);
		if (slot instanceof ISpecialSlot)
			((ISpecialSlot)slot).setStack(stack);
		else slot.putStack(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setAll(List<ItemStack> items) {
		int m = Math.min(items.size(), inventorySlots.size());
		for (int i = 0; i < m; ++i)
			putStackInSlot(i, items.get(i));
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		handler.handleAction(pkt, sender);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleServerPacket(PacketBuffer pkt) throws Exception {
		StateSyncClient ssc = ((StateSyncClient)sync).decodePacket(pkt);
		handler.readState(ssc, this);
		for (int i : slotsToSync) {
			Slot slot = inventorySlots.get(i);
			ItemStack stack0, stack = ssc.get(stack0 = slot.getStack());
			if (stack == stack0) continue;
			if (slot instanceof ISpecialSlot)
				((ISpecialSlot)slot).setStack(stack);
			else slot.putStack(stack);
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return handler.canInteract(playerIn, this);
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		handler.onCloseInteraction(playerIn, this);
	}

	public void hardInvUpdate() {
		this.hardInvUpdate = true;
	}

	public boolean hasPlayerInv() {
		return playerInvS != playerInvE;
	}

	public int playerInvStart() {
		return playerInvS;
	}

	//vanilla fixes

	@Override
	public boolean mergeItemStack(ItemStack item, int ss, int se, boolean d) {
		ItemStack item1 = item.copy();
		if (item1.isStackable())
			for (int i = se - ss; i > 0 && item1.getCount() > 0; i--) {
				Slot slot = inventorySlots.get(d ? i + ss - 1 : se - i);
				ItemStack stack = slot.getStack();
				if (stack.isEmpty()) continue;
				if (slot instanceof ISpecialSlot) {
					ISpecialSlot s = (ISpecialSlot)slot;
					if ((item1 = s.insertItem(item1, false)).isEmpty()) {
						item.setCount(0);
						return true;
					} else if (s.insertHereOnly(item1)) {
						item.setCount(item1.getCount());
						return true;
					}
				} else if (ItemHandlerHelper.canItemStacksStack(stack, item1)) {
					int j = stack.getCount() + item1.getCount();
					int mxs = Math.min(item1.getMaxStackSize(), slot.getSlotStackLimit());
					if (j <= mxs) {
						item.setCount(0);
						stack.setCount(j);
						slot.onSlotChanged();
						return true;
					} else if (stack.getCount() < mxs) {
						item1.shrink(mxs - stack.getCount());
						stack.setCount(mxs);
						slot.onSlotChanged();
					}
				}
			}
		if (item1.getCount() > 0)
			for (int i = se - ss; i > 0; i--) {
				Slot slot = inventorySlots.get(d ? i + ss - 1 : se - i);
				if (slot.getStack().getCount() != 0) continue;
				if (slot instanceof ISpecialSlot) {
					ISpecialSlot s = (ISpecialSlot)slot;
					if ((item1 = s.insertItem(item1, false)).getCount() == 0) {
						item.setCount(0);
						return true;
					} else if (s.insertHereOnly(item1)) {
						item.setCount(item1.getCount());
						return true;
					}
				} else if (slot.isItemValid(item1)) {
					int mxs = slot.getItemStackLimit(item1);
					if (item1.getCount() <= mxs) {
						slot.putStack(item1.copy());
						slot.onSlotChanged();
						item.setCount(0);
						return true;
					} else {
						slot.putStack(item1.splitStack(mxs));
						slot.onSlotChanged();
					}
				}
			}
		if (item1.getCount() != item.getCount()) {
			item.setCount(item1.getCount());
			return true;
		} else return false;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int id) {
		Slot slot = inventorySlots.get(id);
		if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
		ItemStack stack = slot.getStack();
		ItemStack item = stack.copy();
		if (id >= playerInvS && id < playerInvE) {
			for (IQuickTransferHandler h : transferHandlers)
				if (h.transfer(stack, this)) break;
		} else mergeItemStack(stack, playerInvS, playerInvE, false);
		if (stack.getCount() == item.getCount()) return ItemStack.EMPTY;
		slot.onSlotChange(stack, item);
		slot.onSlotChanged();
		slot.onTake(player, stack);
		return item;
	}

	@Override
	public ItemStack slotClick(int s, int b, ClickType m, EntityPlayer player) {
		Slot slot = s >= 0 && s < inventorySlots.size() ? inventorySlots.get(s) : null;
		if (slot instanceof ISpecialSlot) {
			ISpecialSlot ss = (ISpecialSlot)slot;
			return ss.onClick(b, m, player, this);
		} else return super.slotClick(s, b, m, player);
	}

	public interface IStateInteractionHandler {

		/**
		 * write current state to synchronizer
		 * @param state synchronizer
		 * @param cont the open container
		 */
		void writeState(StateSyncServer state, AdvancedContainer cont);

		/**
		 * update state from synchronizer
		 * @param state synchronizer
		 * @param cont the open container
		 */
		void readState(StateSyncClient state, AdvancedContainer cont);

		/**
		 * when a GUI action packet is received at server side
		 * @param pkt packet data
		 * @param sender the player performing the action
		 * @throws Exception potential decoding error
		 */
		default void handleAction(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {}

		/**
		 * @param player
		 * @param cont
		 * @return whether given player can interact with this state through the given container
		 */
		boolean canInteract(EntityPlayer player, AdvancedContainer cont);

		/**
		 * when a player closes a container that provided access to this state
		 * @param player
		 * @param cont
		 */
		default void onCloseInteraction(EntityPlayer player, AdvancedContainer cont) {}

		/**
		 * @return a block location this refers to (if any)
		 */
		default BlockPos pos() { return Utils.NOWHERE; }
	}

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

}
