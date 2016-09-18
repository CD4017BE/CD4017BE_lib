/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 *
 * @author CD4017BE
 */
public class Inventory implements IItemHandlerModifiable
{
	/**	bits[0-59 6*5*2]: side * comp * access */
	public long sideCfg = 0;
	public final ItemStack[] items;
	public final Group[] groups;
	public final IAccessHandler handler;
	public int shift = 0;

	/**
	 * @param l amount of item slots
	 * @param g amount of accessible slot groups (max 5), use group() to define them.
	 * @param handler implement this for detailed in/out control or set null to use default
	 */
	public Inventory(int l, int g, IAccessHandler handler) {
		if (g > 5 || g > l) throw new IllegalArgumentException("Too many slot groups! " + g + " / " + (l < 5 ? l : 5));
		this.items = new ItemStack[l];
		this.groups = new Group[g];
		this.handler = handler == null ? new DefaultAccessHandler() : handler;
	}

	/**
	 * Set the properties of a slot group. You must call this method for all of them, otherwise you may get NullPointerExceptions!
	 * Also it's not recommended to let slot groups overlap.
	 * @param i group index to set (0...c-1)
	 * @param s start slot index (inclusive)
	 * @param e end slot index (exclusive)
	 * @param dir preferred direction: -1 input, 0 none, 1 output
	 * @return this for construction convenience
	 */
	public Inventory group(int i, int s, int e, int dir) {
		groups[i] = new Group(i, s, e, dir);
		return this;
	}

	/**
	 * call each tick to update automation
	 * @param tile the TileEntity owning this
	 */
	public void update(AutomatedTile tile) {
		int cfg;
		IItemHandler access;
		for (byte s = 0; s < 6; s++) {
			cfg = (int)(sideCfg >> (s * 10));
			if((cfg & 0x15) == (cfg & 0x2a) >> 1) continue; //all connections are blocked or passive
			TileEntity te = Utils.getTileOnSide(tile, s);
			if (te == null || !te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[s^1])) continue;
			access = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[s^1]);
			for (int g = 0; g < groups.length; g++, cfg >>= 2) 
				if ((cfg & 3) == 1 && groups[g].dir == -1)
					transferStack(access, new Access(g), shift);
				else if ((cfg & 3) == 2 && groups[g].dir == 1) 
					transferStack(new Access(g), access, shift);
		}
		shift++; //Integer overflow after 3.4 Years continuous operation -> won't happen
	}

	/**
	 * Moves one stack from src to dest inventory
	 * @param src source
	 * @param dest destination
	 * @param shift periodically changing slot offset to ensure items that can't be inserted at destination don't block the whole process.
	 */
	public static void transferStack(IItemHandler src, IItemHandler dest, int shift) {
		int slot = -1, empty = -1;
		ItemStack item = null, stack;
		int ls = src.getSlots();
		for (int i = 0; i < ls; i++) //find an item to extract
			if ((item = src.extractItem(slot = (i + shift) % ls, 65536, true)) != null) break;
		if (item == null) return; //none found
		if (slot >= (ls = shift % ls)) //if we could have found this item in a lower slot, use that instead to keep inventory clean
			for (int i = 0; i < ls; i++) 
				if (item.isItemEqual(src.getStackInSlot(i)) && (stack = src.extractItem(i, 65536, true)) != null) {
					slot = i; item = stack; break;
				}
		int n = item.stackSize, ld = dest.getSlots();
		for (int i = 0; i < ld; i++) //first try to insert into slots with existing items
			if (dest.getStackInSlot(i) == null) empty = empty < 0 ? i : empty; //save location of first empty slot
			else if ((item = dest.insertItem(i, item, false)) == null) break;
		if (empty >= 0 && item != null) //then insert into empty slots if any
			for (int i = empty; i < ld; i++)
				if (dest.getStackInSlot(i) == null && (item = dest.insertItem(i, item, false)) == null) break;
		if (item != null) n -= item.stackSize;
		if (n > 0) src.extractItem(slot, n, false); //extract the transported amount from source
	}

	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setLong(name + "Cfg", sideCfg);
		for (int i = 0; i < items.length; i++) 
			if (items[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				items[i].writeToNBT(tag);
				nbt.setTag(name + Integer.toHexString(i), tag);
			}
	}

	public void readFromNBT(NBTTagCompound nbt, String name) {
		sideCfg = nbt.getLong(name + "Cfg");
		for (int i = 0; i < items.length; i++) {
			String tagName = name + Integer.toHexString(i);
			items[i] = nbt.hasKey(tagName, 10) ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(tagName)) : null;
		}
	}

	public byte getConfig(int s, int id) {
		return (byte)(sideCfg >> (10 * s + 2 * id) & 3);
	}

	@Override
	public int getSlots() {
		return items.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return items[i];
	}

	@Override
	public ItemStack insertItem(int i, ItemStack stack, boolean sim) {
		ItemStack item;
		int m = handler.insertAm(-1, i, item = items[i], stack);
		if (m <= 0) return stack;
		if (!sim) {
			if (item == null) item = ItemHandlerHelper.copyStackWithSize(stack, m);
			else item.stackSize += m;
			handler.setSlot(-1, i, item);
		}
		return (m = stack.stackSize - m) > 0 ? ItemHandlerHelper.copyStackWithSize(stack, m) : null;
	}

	@Override
	public ItemStack extractItem(int i, int m, boolean sim) {
		ItemStack item;
		if ((m = handler.extractAm(-1, i, item = items[i], m)) <= 0) return null;
		if (!sim) {
			if (item.stackSize <= m) handler.setSlot(-1, i, null);
			else {
				item.stackSize -= m;
				handler.setSlot(-1, i, item);
			}
		}
		return ItemHandlerHelper.copyStackWithSize(item, m);
	}

	@Override
	public void setStackInSlot(int i, ItemStack stack) {
		handler.setSlot(-1, i, stack);
	}

	public class Group {

		public final int idx, s, e;
		public final byte dir;

		private Group(int idx, int s, int e, int d) {
			this.idx = idx;
			this.s = s;
			this.e = e;
			this.dir = (byte)d;
		}

	}

	public class Access implements IItemHandler {

		final int[] slots;
		/** bits[0-2]: groupId, bit[6]: insert, bit[7]: extract */
		final byte[] dir;

		public Access(EnumFacing s) {
			int cfg = s != null ? (int)(sideCfg >> (s.ordinal() * 10)) & 0x3ff : 0x3ff, cfg1 = cfg;
			int n = 0;
			for (Group g : groups) 
				if (((cfg1 >>= 2) & 3) != 0) n += g.e - g.s;
			slots = new int[n];
			dir = new byte[n];
			n = 0;
			byte d;
			for (Group g : groups)
				if (((cfg >>= 2) & 3) != 0) {
					d = (byte)(g.idx | cfg << 6);
					for (int i = g.s; i < g.e; i++) {
						slots[n] = i; dir[n++] = d;
					}
				}
		}

		public Access(int g) {
			Group group = groups[g];
			slots = new int[group.e - group.s];
			dir = new byte[slots.length];
			byte d = (byte)(g | 0xc0);
			for (int i = group.s, n = 0; i < group.e; i++, n++) {
				slots[n] = i; dir[n] = d;
			}
		}

		@Override
		public int getSlots() {
			return slots.length;
		}

		@Override
		public ItemStack getStackInSlot(int i) {
			return items[slots[i]];
		}

		@Override
		public ItemStack insertItem(int i, ItemStack stack, boolean sim) {
			ItemStack item;
			int d = dir[i], s, m;
			if ((d & 0x40) == 0 || (m = handler.insertAm(d & 7, s = slots[i], item = items[s], stack)) <= 0) return stack;
			if (!sim) {
				if (item == null) item = ItemHandlerHelper.copyStackWithSize(stack, m);
				else item.stackSize += m;
				handler.setSlot(d & 0x47, s, item);
			}
			return (m = stack.stackSize - m) > 0 ? ItemHandlerHelper.copyStackWithSize(stack, m) : null;
		}

		@Override
		public ItemStack extractItem(int i, int m, boolean sim) {
			ItemStack item;
			int d = dir[i], s;
			if ((d & 0x80) == 0 || (m = handler.extractAm(d & 7, s = slots[i], item = items[s], m)) <= 0) return null;
			if (!sim) {
				if (item.stackSize == m) item = null;
				else item.stackSize -= m;
				handler.setSlot(d & 0x87, s, item);
			}
			return ItemHandlerHelper.copyStackWithSize(item, m);
		}
	}

	public interface IAccessHandler {
		/**
		 * @param slot group index
		 * @param s slot index
		 * @param item current item in slot
		 * @param insert item to insert
		 * @return amount to move into the slot
		 */
		public int insertAm(int g, int s, ItemStack item, ItemStack insert);
		/**
		 * @param g slot group index
		 * @param s slot index
		 * @param item current item in slot
		 * @param extract requested extract amount
		 * @return amount to remove from the slot
		 */
		public int extractAm(int g, int s, ItemStack item, int extract);
		/**
		 * set the slot to a new item (your implementation has to do this)
		 * @param g slot group index with bit 6 on insert or bit 7 on extract. for GUI access it's -1
		 * @param s slot index
		 * @param item item to set
		 */
		public void setSlot(int g, int s, ItemStack item);
	}

	public class DefaultAccessHandler implements IAccessHandler {
		@Override
		public int insertAm(int g, int s, ItemStack item, ItemStack insert) {
			int m = Math.min(insert.getMaxStackSize() - (item == null ? 0 : item.stackSize), insert.stackSize); 
			return item == null || ItemHandlerHelper.canItemStacksStack(item, insert) ? m : 0;
		}
		@Override
		public int extractAm(int g, int s, ItemStack item, int extract) {
			return item == null ? 0 : item.stackSize < extract ? item.stackSize : extract;
		}
		@Override
		public void setSlot(int g, int s, ItemStack item) {
			Inventory.this.items[s] = item;
		}
	}

}
