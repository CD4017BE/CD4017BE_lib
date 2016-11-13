package cd4017be.lib.Gui;

import cd4017be.lib.templates.ITankContainer;

import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Fully automated Container supporting items and fluids
 * @author CD4017BE
 */
public class TileContainer extends DataContainer {
	/** assign this to do special slot click handling */
	public ISlotClickHandler clickHandler;
	public int invPlayerS = 0;
	public int invPlayerE = 0;
	public final ArrayList<TankSlot> tankSlots;
	public final ArrayList<FluidStack> fluidStacks;
	private boolean specialInvSync = false;

	public TileContainer(IGuiData tile, EntityPlayer player) {
		super(tile, player);
		this.tankSlots = new ArrayList<TankSlot>();
		this.fluidStacks = new ArrayList<FluidStack>();
	}

	public void addPlayerInventory(int x, int y) {
		this.addPlayerInventory(x, y, false, false);
	}
	
	public void addPlayerInventory(int x, int y, boolean armor, boolean lockSel) {
		invPlayerS = this.inventorySlots.size();
		invPlayerE = invPlayerS + (armor ? 41 : 36);
		for (int i = 0; i < 3; i++) 
			for (int j = 0; j < 9; j++)
				this.addSlotToContainer(new Slot(player.inventory, i * 9 + j + 9, x + j * 18, y + i * 18));
		for (int i = 0; i < 9; i++)
			if (lockSel && i == player.inventory.currentItem)
				this.addSlotToContainer(new LockedSlot(player.inventory, i, x + i * 18, y + 58));
			else this.addSlotToContainer(new Slot(player.inventory, i, x + i * 18, y + 58));
		if (armor) {
			this.addSlotToContainer(new Slot(player.inventory, 40, x - 18, y + 58));
			for (int i = 0; i < 4; i++)
				this.addSlotToContainer(new Slot(player.inventory, i + 36, x - 18, y - i * 18 + 36));
		}
	}

	public void addItemSlot(Slot slot) {
		this.addSlotToContainer(slot);
		if (slot instanceof GlitchSaveSlot) specialInvSync = true;
	}

	public void addTankSlot(TankSlot slot) {
		this.tankSlots.add(slot);
		this.fluidStacks.add((FluidStack)null);
	}

	@Override
	protected boolean checkChanges(PacketBuffer dos) {
		byte send = 0;
		if (!tankSlots.isEmpty()) {
			for (int i = 0; i < this.tankSlots.size(); i++) {
				FluidStack fluid1 = this.tankSlots.get(i).getStack();
				FluidStack fluid0 = this.fluidStacks.get(i);
				if ((fluid1 == null ^ fluid0 == null) || (fluid1 != null && !fluid1.isFluidStackIdentical(fluid0))) {
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
		}
		int p = -1, n = 0;
		if (specialInvSync) {
			p = dos.writerIndex();
			dos.writeByte(n);
		}
		for (int i = 0; i < this.inventorySlots.size(); i++) {
			Slot slot = this.inventorySlots.get(i);
			ItemStack item1 = slot.getStack();
			ItemStack item0 = this.inventoryItemStacks.get(i);
			if (!ItemStack.areItemStacksEqual(item0, item1)) {
				this.inventoryItemStacks.set(i, item0 = (item1 == null ? null : item1.copy()));
				if (slot instanceof GlitchSaveSlot) {
					dos.writeByte(i);
					dos.writeShort(Item.getIdFromItem(item0 != null ? item0.getItem() : null));
					if (item0 != null && item0.getItem() != null) {
						dos.writeInt(item0.stackSize);
						dos.writeShort(item0.getItemDamage());
						dos.writeNBTTagCompoundToBuffer(item0.getTagCompound());
					}
					n++;
				} else for (IContainerListener listener : this.listeners)
					listener.sendSlotContents(this, i, item0);
			}
		}
		if (n > 0) dos.setByte(p, n);
		return super.checkChanges(dos) || send != 0 || n > 0;
	}

	@Override
	public void onDataUpdate(PacketBuffer dis) {
		try {
		if (!this.tankSlots.isEmpty())
			for (byte c = dis.readByte(), i = 0; c != 0 && i < this.tankSlots.size(); c >>= 1, i++)
				if ((c & 1) != 0)
					this.tankSlots.get(i).putStack(FluidStack.loadFluidStackFromNBT(dis.readNBTTagCompoundFromBuffer()));
		if (specialInvSync)
			for (int n = dis.readUnsignedByte(); n > 0; n--) {
				int s = dis.readUnsignedByte();
				int id = dis.readShort();
				ItemStack item = id == 0 ? null : new ItemStack(Item.getItemById(id), dis.readInt(), dis.readShort(), dis.readNBTTagCompoundFromBuffer());
				Slot slot; IItemHandler acc;
				if (s < inventorySlots.size() && (slot = inventorySlots.get(s)) instanceof GlitchSaveSlot && (acc = ((GlitchSaveSlot)slot).getItemHandler()) instanceof IItemHandlerModifiable)
					((IItemHandlerModifiable)acc).setStackInSlot(((GlitchSaveSlot)slot).index, item);
			}
		} catch (IOException e) {e.printStackTrace(); return;}
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
						item = curItem.copy();
						if (b == 1) item.stackSize = 1;
					}
					if (item.stackSize > slot.getSlotStackLimit()) item.stackSize = slot.getSlotStackLimit();
					slot.putStack(item);
				} else if (curItem == null && item != null && slot.canTakeStack(player)){
					slot.decrStackSize(b == 0 ? slot.getSlotStackLimit() : 1);
				} else return null;
				slot.onSlotChanged();
			}
			return null;
		} else if (slot instanceof GlitchSaveSlot) {
			if (m != ClickType.PICKUP && m != ClickType.QUICK_MOVE) return null;
			boolean boost = m == ClickType.QUICK_MOVE;
			IItemHandler acc = ((GlitchSaveSlot)slot).getItemHandler();
			int p = ((GlitchSaveSlot)slot).index;
			ItemStack curItem = player.inventory.getItemStack();
			if (curItem != null) {
				if (boost) {
					ItemStack rem = acc.insertItem(p, ItemHandlerHelper.copyStackWithSize(curItem, 65536), true);
					int n = rem != null ? 65536 - rem.stackSize : 65536, n1 = 0;
					if (n <= 0) return null;
					if (b == 0) {
						if (n < curItem.stackSize) curItem.stackSize -= n1 = n;
						else {
							n1 = curItem.stackSize;
							player.inventory.setItemStack(null);
						}
					}
					if (n1 < n)
						n1 += getFromPlayerInv(ItemHandlerHelper.copyStackWithSize(curItem, n - n1), player.inventory);
					acc.insertItem(p, ItemHandlerHelper.copyStackWithSize(curItem, n1), false);
				} else {
					int n = b == 0 ? curItem.stackSize : 1;
					ItemStack rem = acc.insertItem(p, ItemHandlerHelper.copyStackWithSize(curItem, n), false);
					curItem.stackSize -= n - (rem != null ? rem.stackSize : 0);
					if (curItem.stackSize <= 0) player.inventory.setItemStack(null);
				}
			} else if (item != null) {
				int n = boost ? (b == 0 ? item.getMaxStackSize() : 65536) : (b == 0 ? 1 : 8);
				if ((item = acc.extractItem(p, n, true)) == null) return null;
				int rem = putInPlayerInv(item.copy(), player.inventory);
				acc.extractItem(p, item.stackSize - rem, false);
			}
			return null;
		} else if (clickHandler != null && clickHandler.slotClick(item == null ? null : item.copy(), slot, b, m, this)) {
			return item == null || item.stackSize <= 0 ? null : item;
		} else {
			ItemStack ret = super.slotClick(s, b, m, player);
			if (slot instanceof SlotItemHandler && slot.getStack() == item) slot.putStack(slot.getStack());
			return ret;
		}
	}

	public static int putInPlayerInv(ItemStack item, InventoryPlayer inv) {
		int m = item.getMaxStackSize();
		int es = inv.mainInventory.length;
		for (int i = 0; i < inv.mainInventory.length; i++) {
			ItemStack stack = inv.mainInventory[i];
			if (stack != null && stack.stackSize < m && stack.isItemEqual(item)) {
				if (item.stackSize <= m - stack.stackSize) {
					stack.stackSize += item.stackSize;
					return 0;
				} else {
					item.stackSize -= m - stack.stackSize;
					stack.stackSize = m;
				}
			} else if (stack == null && i < es) es = i;
		}
		for (int i = es; i < inv.mainInventory.length; i++)
			if (inv.mainInventory[i] == null) {
				if (item.stackSize <= m) {
					inv.mainInventory[i] = item;
					return 0;
				} else inv.mainInventory[i] = item.splitStack(m);
			}
		return item.stackSize;
	}

	public static int getFromPlayerInv(ItemStack item, InventoryPlayer inv) {
		int n = 0;
		for (int i = 0; i < inv.mainInventory.length; i++) {
			ItemStack stack = inv.mainInventory[i];
			if (item.isItemEqual(stack)) {
				n += stack.stackSize;
				if (n <= item.stackSize) {
					inv.mainInventory[i] = null;
					if (n == item.stackSize) return n;
				} else {
					stack.stackSize = n - item.stackSize;
					return item.stackSize;
				}
			}
		}
		return n;
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
		public final ITankContainer inventory;

		public TankSlot(ITankContainer inv, int id, int x, int y, byte size) {
			this.inventory = inv;
			this.tankNumber = id;
			this.xDisplayPosition = x;
			this.yDisplayPosition = y;
			this.size = size;
		}

		public FluidStack getStack() {
			return inventory.getTank(tankNumber);
		}

		public void putStack(FluidStack fluid) {
			inventory.setTank(tankNumber, fluid);
		}

	}

}
