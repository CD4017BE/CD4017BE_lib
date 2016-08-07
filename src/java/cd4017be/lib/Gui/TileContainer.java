/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.Gui;

import cd4017be.lib.templates.TankContainer;

import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Fully automated Container supporting items and fluids
 * @author CD4017BE
 */
public class TileContainer extends DataContainer
{
	/** assign this to do special slot click handling */
	public ISlotClickHandler clickHandler;
	public int invPlayerS = 0;
	public int invPlayerE = 0;
	public ArrayList<TankSlot> tankSlots = new ArrayList<TankSlot>();
	public ArrayList<FluidStack> fluidStacks = new ArrayList<FluidStack>();

	public TileContainer(IGuiData tile, EntityPlayer player) {
		super(tile, player);
	}

	public void addPlayerInventory(int x, int y, boolean armor) {
		invPlayerS = this.inventorySlots.size();
		invPlayerE = invPlayerS + (armor ? 41 : 36);
		for (int i = 0; i < 3; i++) 
			for (int j = 0; j < 9; j++)
				this.addSlotToContainer(new Slot(player.inventory, i * 9 + j + 9, x + j * 18, y + i * 18));
		for (int i = 0; i < 9; i++)
			this.addSlotToContainer(new Slot(player.inventory, i, x + i * 18, y + 58));
		if (armor) {
			this.addSlotToContainer(new Slot(player.inventory, 40, x - 18, y + 58));
			for (int i = 0; i < 4; i++)
				this.addSlotToContainer(new Slot(player.inventory, i + 36, x - 18, y - i * 18 + 36));
		}
	}

	public Slot addItemSlot(Slot slot) {
		return this.addSlotToContainer(slot);
	}

	public void addTankSlot(TankSlot slot) {
		this.tankSlots.add(slot);
		this.fluidStacks.add((FluidStack)null);
	}

	@Override
	protected boolean checkChanges(PacketBuffer dos) {
		byte send = 0;
		for (int i = 0; i < this.inventorySlots.size(); i++) {
			ItemStack item1 = this.inventorySlots.get(i).getStack();
			ItemStack item0 = this.inventoryItemStacks.get(i);
			if (!ItemStack.areItemStacksEqual(item0, item1)) {
				this.inventoryItemStacks.set(i, item0 = item1 == null ? null : item1.copy());
				for (IContainerListener listener : this.listeners)
					listener.sendSlotContents(this, i, item0);
			}
		}
		for (int i = 0; i < this.tankSlots.size(); i++) {
			FluidStack fluid1 = this.tankSlots.get(i).getStack();
			FluidStack fluid0 = this.fluidStacks.get(i);
			if ((fluid1 == null ^ fluid0 == null) || (fluid1 != null && !fluid1.isFluidEqual(fluid0))) {
				this.fluidStacks.set(i, fluid1 == null ? null : fluid1.copy());
				send |= 1 << i;
			}
		}
		dos.writeByte(send);
		for (byte c = send, i = 0; c != 0; c >>= 1, i++)
			if ((c & 1) != 0) {
				FluidStack fluid = this.fluidStacks.get(i);
				dos.writeNBTTagCompoundToBuffer(fluid == null ? null : fluid.writeToNBT(new NBTTagCompound()));
			}
		return super.checkChanges(dos) || send != 0;
	}

	@Override
	public void onDataUpdate(PacketBuffer dis) {
		try {
		for (byte c = dis.readByte(), i = 0; c != 0 && i < this.tankSlots.size(); c >>= 1, i++)
			if ((c & 1) != 0)
				this.tankSlots.get(i).putStack(FluidStack.loadFluidStackFromNBT(dis.readNBTTagCompoundFromBuffer()));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		super.onDataUpdate(dis);
	}

	@Override
	public boolean mergeItemStack(ItemStack item, int ss, int se, boolean d) {
		return super.mergeItemStack(item, ss, se, d);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int id) {
		Slot slot = (Slot)inventorySlots.get(id);
		if (slot == null || !slot.getHasStack()) return null;
		ItemStack stack = slot.getStack();
		ItemStack item = stack.copy();
		if (clickHandler == null || !clickHandler.transferStack(stack, id, this)) {
			int s, e;
			if (id < invPlayerS || id >= invPlayerE) {s = invPlayerS; e = invPlayerE;}
			else if (invPlayerS > 0) {s = 0; e = invPlayerS;}
			else return null;
			if(!super.mergeItemStack(stack, s, e, false)) return null;
		}
		if (stack.stackSize == item.stackSize) return null;
		slot.onSlotChange(stack, item);
		if (stack.stackSize == 0) slot.putStack((ItemStack)null);
		else slot.onSlotChanged();
		slot.onPickupFromSlot(player, stack);
		return item;
	}

	@Override
	public ItemStack slotClick(int s, int b, ClickType m, EntityPlayer player) {
		Slot slot = s >= 0 && s < inventorySlots.size() ? inventorySlots.get(s) : null;
		if (slot == null) return super.slotClick(s, b, m, player);
		ItemStack item = slot.getStack();
		if (slot instanceof SlotHolo) {
			if (m == ClickType.PICKUP) {
				ItemStack curItem = player.inventory.getItemStack();
				if (curItem != null && slot.isItemValid(curItem)) {
					if (item != null && item.isItemEqual(curItem)) {
						item.stackSize += b == 1 ? 1 : curItem.stackSize;
					} else {
						curItem = curItem.copy();
				   		if (b == 1) curItem.stackSize = 1;
				   		slot.putStack(curItem);
					}
				} else if (curItem == null && item != null && slot.canTakeStack(player)){
					slot.decrStackSize(b == 0 ? slot.getSlotStackLimit() : 1);
				} else return null;
				slot.onSlotChanged();
			}
			return null;
		} else if (clickHandler != null && clickHandler.slotClick(item == null ? null : item.copy(), slot, b, m, this)) {
			return item == null || item.stackSize <= 0 ? null : item;
		} else return super.slotClick(s, b, m, player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void putStacksInSlots(ItemStack[] stack) {
		for (int i = 0; i < stack.length && i < this.inventorySlots.size(); ++i) 
			this.inventorySlots.get(i).putStack(stack[i]);
	}

	public interface ISlotClickHandler {
		public boolean transferStack(ItemStack item, int s, TileContainer cont);
		public boolean slotClick(ItemStack item, Slot slot, int b, ClickType c, TileContainer cont);
	}

	public static class TankSlot {
		public final int xDisplayPosition, yDisplayPosition, tankNumber;
		public final byte size;
		public final TankContainer inventory;

		public TankSlot(TankContainer inv, int id, int x, int y, byte size) {
			this.inventory = inv;
			this.tankNumber = id;
			this.xDisplayPosition = x;
			this.yDisplayPosition = y;
			this.size = size;
		}

		public FluidStack getStack() {
			return inventory.fluids[tankNumber];
		}

		public void putStack(FluidStack fluid) {
			inventory.fluids[tankNumber] = fluid;
		}

	}

}
