package cd4017be.lib.Gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
	/** flag 1: special slot synchronizing, flag 2: hard inventory update required */
	private byte specialInvSync = 0;

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
				this.addSlotToContainer(new HidableSlot(player.inventory, i * 9 + j + 9, x + j * 18, y + i * 18));
		for (int i = 0; i < 9; i++)
			if (lockSel && i == player.inventory.currentItem)
				this.addSlotToContainer(new LockedSlot(player.inventory, i, x + i * 18, y + 58));
			else this.addSlotToContainer(new HidableSlot(player.inventory, i, x + i * 18, y + 58));
		if (armor) {
			this.addSlotToContainer(new HidableSlot(player.inventory, 40, x - 18, y + 58));
			for (int i = 0; i < 4; i++)
				this.addSlotToContainer(new HidableSlot(player.inventory, i + 36, x - 18, y - i * 18 + 36));
		}
	}

	public void addItemSlot(Slot slot) {
		this.addSlotToContainer(slot);
		if (slot instanceof GlitchSaveSlot) {
			specialInvSync |= 1;
			GlitchSaveSlot gss = (GlitchSaveSlot)slot;
			if(player.world.isRemote && gss.getItemHandler() instanceof IItemHandlerModifiable)
				((IItemHandlerModifiable)gss.getItemHandler()).setStackInSlot(gss.index, ItemStack.EMPTY);
		}
	}

	public void addTankSlot(TankSlot slot) {
		this.tankSlots.add(slot);
		this.fluidStacks.add((FluidStack)null);
		if (player.world.isRemote) slot.putStack(null);
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
					dos.writeCompoundTag(fluid == null ? null : fluid.writeToNBT(new NBTTagCompound()));
				}
		}
		int p = -1, n = 0;
		if ((specialInvSync & 1) != 0) {
			p = dos.writerIndex();
			dos.writeByte(n);
		}
		if ((specialInvSync & 2) != 0 && player instanceof EntityPlayerMP) {
			((EntityPlayerMP)player).isChangingQuantityOnly = false;
			specialInvSync &= 0xfd;
		}
		for (int i = 0; i < this.inventorySlots.size(); i++) {
			Slot slot = this.inventorySlots.get(i);
			ItemStack item1 = slot.getStack();
			ItemStack item0 = this.inventoryItemStacks.get(i);
			if (!ItemStack.areItemStacksEqual(item0, item1)) {
				this.inventoryItemStacks.set(i, item0 = item1.copy());
				if (slot instanceof GlitchSaveSlot) {
					dos.writeByte(i);
					if (item0.isEmpty()) {
						dos.writeShort(0);
					} else {
						dos.writeShort(Item.getIdFromItem(item0.getItem()));
						dos.writeInt(item0.getCount());
						dos.writeShort(item0.getMetadata());
						dos.writeCompoundTag(item0.getTagCompound());
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
					this.tankSlots.get(i).putStack(FluidStack.loadFluidStackFromNBT(dis.readCompoundTag()));
		if ((specialInvSync & 1) != 0)
			for (int n = dis.readUnsignedByte(); n > 0; n--) {
				int s = dis.readUnsignedByte();
				int id = dis.readShort();
				ItemStack item;
				if (id == 0) item = ItemStack.EMPTY;
				else {
					item = new ItemStack(Item.getItemById(id), dis.readInt(), dis.readShort());
					item.setTagCompound(dis.readCompoundTag());
				}
				Slot slot; IItemHandler acc;
				if (s < inventorySlots.size() && (slot = inventorySlots.get(s)) instanceof GlitchSaveSlot && (acc = ((GlitchSaveSlot)slot).getItemHandler()) instanceof IItemHandlerModifiable)
					((IItemHandlerModifiable)acc).setStackInSlot(((GlitchSaveSlot)slot).index, item);
			}
		} catch (IOException e) {e.printStackTrace(); return;}
		super.onDataUpdate(dis);
	}

	@Override
	public boolean mergeItemStack(ItemStack item, int ss, int se, boolean d) {
		ItemStack item1 = item.copy();
		if (item1.isStackable())
			for (int i = se - ss; i > 0 && item1.getCount() > 0; i--) {
				Slot slot = inventorySlots.get(d ? i + ss - 1 : se - i);
				ItemStack stack = slot.getStack();
				if (stack.isEmpty()) continue;
				if (slot instanceof GlitchSaveSlot) {
					GlitchSaveSlot gss = (GlitchSaveSlot)slot;
					if ((item1 = gss.getItemHandler().insertItem(gss.index, item1, false)).isEmpty()) {
						item.setCount(0);
						return true;
					}
				} else if (slot instanceof SlotHolo) {
					if (slot.isItemValid(item1) && ItemHandlerHelper.canItemStacksStack(stack, item1)) {
						stack.grow(item1.getCount());
						if (stack.getCount() > slot.getSlotStackLimit()) stack.setCount(slot.getSlotStackLimit());
						slot.putStack(stack);
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
				if (slot instanceof GlitchSaveSlot) {
					GlitchSaveSlot gss = (GlitchSaveSlot)slot;
					if ((item1 = gss.getItemHandler().insertItem(gss.index, item1, false)).getCount() == 0) {
						item.setCount(0);
						return true;
					}
				} else if (slot instanceof SlotHolo) {
					if (slot.isItemValid(item1)) {
						slot.putStack(item1);
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
		if (clickHandler == null || !clickHandler.transferStack(stack, id, this)) {
			int s, e;
			if (id < invPlayerS || id >= invPlayerE) {s = invPlayerS; e = invPlayerE;}
			else if (invPlayerS > 0) {s = 0; e = invPlayerS;}
			else return ItemStack.EMPTY;
			if(!mergeItemStack(stack, s, e, false)) return ItemStack.EMPTY;
		}
		if (stack.getCount() == item.getCount()) return ItemStack.EMPTY;
		slot.onSlotChange(stack, item);
		slot.onSlotChanged();
		slot.onTake(player, stack);
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
				if (curItem.getCount() > 0 && slot.isItemValid(curItem)) {
					if (item.getCount() > 0 && (b == 1 || ItemHandlerHelper.canItemStacksStack(item, curItem))) {
						item.grow(b == 1 ? 1 : curItem.getCount());
					} else {
						item = curItem.copy();
						if (b == 1) item.setCount(1);
					}
					if (item.getCount() > slot.getSlotStackLimit()) item.setCount(slot.getSlotStackLimit());
					slot.putStack(item);
				} else if (curItem.getCount() == 0 && item.getCount() != 0 && slot.canTakeStack(player)){
					slot.decrStackSize(b == 0 ? slot.getSlotStackLimit() : 1);
				} else return ItemStack.EMPTY;
				slot.onSlotChanged();
			} else if (m == ClickType.CLONE) quickSelect(player, item);
			return ItemStack.EMPTY;
		} else if (slot instanceof GlitchSaveSlot) {
			if (m == ClickType.CLONE) {
				quickSelect(player, item);
				return ItemStack.EMPTY;
			} else if (m != ClickType.PICKUP && m != ClickType.QUICK_MOVE)
				return ItemStack.EMPTY;
			boolean boost = m == ClickType.QUICK_MOVE;
			GlitchSaveSlot gss = (GlitchSaveSlot)slot;
			if (!gss.clientInteract) {
				if (player.world.isRemote) return ItemStack.EMPTY;
				specialInvSync |= 2;
			}
			IItemHandler acc = gss.getItemHandler();
			int p = gss.index;
			ItemStack curItem = player.inventory.getItemStack();
			if (curItem.getCount() > 0 && (item.isEmpty() || ItemHandlerHelper.canItemStacksStack(item, curItem))) {
				if (boost) {
					ItemStack rem = acc.insertItem(p, ItemHandlerHelper.copyStackWithSize(curItem, 65536), true);
					int n = 65536 - rem.getCount(), n1 = 0;
					if (n <= 0) return ItemStack.EMPTY;
					if (b == 0) {
						if (n < curItem.getCount()) curItem.shrink(n1 = n);
						else {
							n1 = curItem.getCount();
							player.inventory.setItemStack(ItemStack.EMPTY);
						}
					}
					if (n1 < n)
						n1 += getFromPlayerInv(ItemHandlerHelper.copyStackWithSize(curItem, n - n1), player.inventory);
					acc.insertItem(p, ItemHandlerHelper.copyStackWithSize(curItem, n1), false);
				} else {
					int n = b == 0 ? curItem.getCount() : 1;
					ItemStack rem = acc.insertItem(p, ItemHandlerHelper.copyStackWithSize(curItem, n), false);
					curItem.shrink(n - rem.getCount());
					if (curItem.getCount() <= 0) player.inventory.setItemStack(ItemStack.EMPTY);
				}
			} else if (item.getCount() > 0) {
				int n = boost ? (b == 0 ? item.getMaxStackSize() : 65536) : (b == 0 ? 1 : 8);
				if ((item = acc.extractItem(p, n, true)).getCount() == 0) return ItemStack.EMPTY;
				int rem = putInPlayerInv(item.copy(), player.inventory);
				acc.extractItem(p, item.getCount() - rem, false);
			}
			return ItemStack.EMPTY;
		} else if (clickHandler != null && clickHandler.slotClick(item.copy(), slot, b, m, this)) {
			return item;
		} else {
			ItemStack ret = super.slotClick(s, b, m, player);
			if (slot instanceof SlotItemHandler && slot.getStack() == item) slot.putStack(slot.getStack());
			if (slot instanceof SlotTank) return slot.getStack();
			return ret;
		}
	}

	private void quickSelect(EntityPlayer player, ItemStack item) {
		ItemStack stack = player.inventory.getItemStack();
		if (!stack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(item, stack)) return;
		item = ItemHandlerHelper.copyStackWithSize(item, item.getMaxStackSize() - stack.getCount());
		if (item.isEmpty()) return;
		int n = stack.getCount() + getFromPlayerInv(item, player.inventory);
		stack = ItemHandlerHelper.copyStackWithSize(item, player.capabilities.isCreativeMode ? item.getMaxStackSize() : n);
		player.inventory.setItemStack(stack);
	}

	public static int putInPlayerInv(ItemStack item, InventoryPlayer inv) {
		int x = item.getCount();
		int m = item.getMaxStackSize();
		int es = inv.mainInventory.size();
		for (int i = 0; i < inv.mainInventory.size(); i++) {
			ItemStack stack = inv.mainInventory.get(i);
			int n = stack.getCount();
			if (n > 0 && n < m && ItemHandlerHelper.canItemStacksStack(stack, item)) {
				if (x <= m - n) {
					stack.grow(x);
					return 0;
				} else {
					x -= m - n;
					stack.setCount(m);
				}
			} else if (n == 0 && i < es) es = i;
		}
		for (int i = es; i < inv.mainInventory.size(); i++)
			if (inv.mainInventory.get(i).isEmpty()) {
				if (x <= m) {
					item.setCount(x);
					inv.mainInventory.set(i, item);
					return 0;
				} else {
					x -= m;
					inv.mainInventory.set(i, ItemHandlerHelper.copyStackWithSize(item, m));
				}
			}
		return x;
	}

	public static int getFromPlayerInv(ItemStack item, InventoryPlayer inv) {
		int n = 0;
		for (int i = 0; i < inv.mainInventory.size(); i++) {
			ItemStack stack = inv.mainInventory.get(i);
			if (ItemHandlerHelper.canItemStacksStack(item, stack)) {
				n += stack.getCount();
				if (n <= item.getCount()) {
					inv.mainInventory.set(i, ItemStack.EMPTY);
					if (n == item.getCount()) return n;
				} else {
					stack.setCount(n - item.getCount());
					return item.getCount();
				}
			}
		}
		return n;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setAll(List<ItemStack> items) {
		int m = Math.min(items.size(), inventorySlots.size());
		for (int i = 0; i < m; ++i)
			inventorySlots.get(i).putStack(items.get(i));
	}

	public interface ISlotClickHandler {
		public boolean transferStack(ItemStack item, int s, TileContainer cont);
		public boolean slotClick(ItemStack item, Slot slot, int b, ClickType c, TileContainer cont);
	}

	public static class TankSlot {
		public final int xPos, yPos, tankNumber;
		public final byte size;
		public final ITankContainer inventory;

		public TankSlot(ITankContainer inv, int id, int x, int y, byte size) {
			this.inventory = inv;
			this.tankNumber = id;
			this.xPos = x;
			this.yPos = y;
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
