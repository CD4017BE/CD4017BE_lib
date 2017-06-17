package cd4017be.lib.util;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class ItemFluidUtil {

	public static final String Tag_ItemList = "Items", Tag_ItemIndex = "ItIdx", Tag_FluidList = "Fluids", Tag_FluidIndex = "FlIdx";
	public static final Container CraftContDummy = new Container() {
		@Override
		public boolean canInteractWith(EntityPlayer var1) {
			return true;
		}
		@Override
		public void onCraftMatrixChanged(IInventory par1IInventory) {}
	};

	public static void loadInventory(NBTTagList list, ItemStack[] inv) {
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			int s = tag.getByte("slot") & 0xff;
			if (s < inv.length) inv[s] = new ItemStack(tag);
		}
	}

	public static NBTTagList saveInventory(ItemStack[] inv) {
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < inv.length; i++)
			if (inv[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				inv[i].writeToNBT(tag);
				tag.setByte("slot", (byte)i);
				list.appendTag(tag);
			}
		return list;
	}

	public static ItemStack[] loadItems(NBTTagList list) {
		ItemStack[] items = new ItemStack[list.tagCount()];
		for (int i = 0; i < items.length; i++)
			items[i] = new ItemStack(list.getCompoundTagAt(i));
		return items;
	}

	public static void loadItems(NBTTagList list, ItemStack[] items) {
		int m = Math.min(items.length, list.tagCount());
		for (int i = 0; i < m; i++)
			items[i] = new ItemStack(list.getCompoundTagAt(i));
	}

	public static NBTTagList saveItems(ItemStack[] items) {
		NBTTagList list = new NBTTagList();
		for (ItemStack item : items)
			if (item != null) {
				NBTTagCompound tag = new NBTTagCompound();
				item.writeToNBT(tag);
				list.appendTag(tag);
			}
		return list;
	}

	public static NBTTagList saveFluids(FluidStack[] fluids) {
		NBTTagList list = new NBTTagList();
		for (FluidStack fluid : fluids)
			if (fluid != null) {
				NBTTagCompound tag = new NBTTagCompound();
				fluid.writeToNBT(tag);
				list.appendTag(tag);
			}
		return list;
	}

	public static FluidStack[] loadFluids(NBTTagList list) {
		FluidStack[] fluids = new FluidStack[list.tagCount()];
		for (int i = 0; i < fluids.length; i++)
			fluids[i] = FluidStack.loadFluidStackFromNBT(list.getCompoundTagAt(i));
		return fluids;
	}

	public static InventoryCrafting craftingInventory(ItemStack[] grid, int size) {
		InventoryCrafting icr = new InventoryCrafting(CraftContDummy, size, size);
		int m = Math.min(grid.length, icr.getSizeInventory());
		for (int i = 0; i < m; i++)
			icr.setInventorySlotContents(i, grid[i]);
		return icr;
	}

	public static int transferItems(IItemHandler src, IItemHandler dst, IFilter<ItemStack, IItemHandler> extr, IFilter<ItemStack, IItemHandler> ins) {
		ItemStack stack;
		int n, m = 0;
		for (int i = 0; i < src.getSlots(); i++) {
			stack = src.extractItem(i, 65536, true);
			if (extr != null) stack = extr.getExtract(stack, src);
			if (stack == null) continue;
			if (ins != null && (stack = ItemHandlerHelper.copyStackWithSize(stack, ins.insertAmount(stack, dst))) == null) continue;
 			n = stack.stackSize;
			stack = ItemHandlerHelper.insertItemStacked(dst, stack, false);
 			if (stack != null) n -= stack.stackSize;
 			src.extractItem(i, n, false);
 			m += n;
		}
		return m;
	}

	public static int findStack(ItemStack item, IItemHandler inv, int p) {
		if (item == null) return -1;
		for (int i = p; i < inv.getSlots(); i++)
			if (ItemStack.areItemStacksEqual(item, inv.getStackInSlot(i))) return i;
		return -1;
	}

	public static ItemStack putInSlots(IItemHandler inv, ItemStack stack, int... slots) {
		for (int s : slots)
			if (inv.getStackInSlot(s) != null && (stack = inv.insertItem(s, stack, false)) == null)
				return null;
		for (int s : slots)
			if (inv.getStackInSlot(s) == null && (stack = inv.insertItem(s, stack, false)) == null)
				return null;
		return stack;
	}

	public static int drain(IItemHandler inv, ItemStack item) {
		int n = item.stackSize, m = 0;
		for (int i = 0; i < inv.getSlots() && m < n; i++) 
			if (item.isItemEqual(inv.getStackInSlot(i))) {
				ItemStack stack = inv.extractItem(i, n - m, false);
				if (stack != null) m += stack.stackSize;
			}
		return m;
	}

	public static int drain(IItemHandler inv, OreDictStack ore, ArrayList<ItemStack> buffer) {
		int n = ore.stacksize, m = 0;
		for (int i = 0; i < inv.getSlots() && m < n; i++) 
			if (ore.isEqual(inv.getStackInSlot(i))) {
				ItemStack stack = inv.extractItem(i, n - m, false);
				if (stack != null) {
					m += stack.stackSize;
					addToList(buffer, stack);
				}
			}
		return m;
	}

	public static void addToList(ArrayList<ItemStack> list, ItemStack item) {
		for (ItemStack stack : list)
			if (item.isItemEqual(stack)) {
				stack.stackSize += item.stackSize;
				return;
			}
		list.add(item);
	}

	public static void addToList(ArrayList<FluidStack> list, FluidStack fluid) {
		for (FluidStack stack : list)
			if (fluid.isFluidEqual(stack)) {
				stack.amount += fluid.amount;
				return;
			}
		list.add(fluid);
	}

	public static ItemStack drain(IItemHandler inv, int am) {
		boolean mss = am < 0;
		if (mss) am = 65536;
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.extractItem(i, am, true);
			if (stack == null) continue;
			stack.stackSize = mss ? stack.getMaxStackSize() : am;
			stack.stackSize = drain(inv, stack);
			return stack;
		}
		return null;
	}

	public static class StackedFluidAccess implements IFluidHandler {

		public final IFluidHandler acc;
		private final ItemStack item;
		private final int n;

		public StackedFluidAccess(ItemStack item) {
			this.n = item != null ? item.stackSize : 0;
			if (n > 0) {
				this.acc = FluidUtil.getFluidHandler(item);
				if (this.acc != null) item.stackSize = 1;
				this.item = item;
			} else {
				this.acc = null;
				this.item = null;
			}
		}

		public boolean valid() {
			return acc != null;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return acc.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (n > 1) resource = new FluidStack(resource, resource.amount / n);
			return acc.fill(resource, doFill) * n;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			if (n > 1) resource = new FluidStack(resource, resource.amount / n);
			FluidStack stack = acc.drain(resource, doDrain);
			if (stack != null) stack.amount *= n;
			return stack;
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			FluidStack stack = acc.drain(maxDrain / n, doDrain);
			if (stack != null) stack.amount *= n;
			return stack;
		}

		public ItemStack result() {
			item.stackSize *= n;
			return item.stackSize > 0 && item.getItem() != null ? item : null;
		}

	}

}
