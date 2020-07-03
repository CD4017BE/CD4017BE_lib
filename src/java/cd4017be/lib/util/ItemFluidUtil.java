package cd4017be.lib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import javax.annotation.Nonnull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author CD4017BE
 */
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
		Arrays.fill(inv, ItemStack.EMPTY);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			int s = tag.getByte("slot") & 0xff;
			if (s < inv.length) inv[s] = new ItemStack(tag);
		}
	}

	public static NBTTagList saveInventory(ItemStack[] inv) {
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < inv.length; i++)
			if (!inv[i].isEmpty()) {
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
		Arrays.fill(items, m, items.length, ItemStack.EMPTY);
	}

	public static NBTTagList saveItems(ItemStack[] items) {
		NBTTagList list = new NBTTagList();
		for (ItemStack item : items)
			if (!item.isEmpty()) {
				NBTTagCompound tag = new NBTTagCompound();
				item.writeToNBT(tag);
				list.appendTag(tag);
			}
		return list;
	}

	/**
	 * writes an ItemStack to NBT using 32 bit stacksize resolution
	 * @param item
	 * @return
	 */
	public static NBTTagCompound saveItemHighRes(ItemStack item) {
		NBTTagCompound nbt = new NBTTagCompound();
		item.writeToNBT(nbt);
		nbt.removeTag("Count");
		nbt.setInteger("Num", item.getCount());
		return nbt;
	}

	/**
	 * loads an ItemStack from NBT using 32 bit stacksize resolution
	 * @param nbt
	 * @return
	 */
	public static ItemStack loadItemHighRes(NBTTagCompound nbt) {
		ItemStack item = new ItemStack(nbt);
		item.setCount(nbt.getInteger("Num"));
		return item;
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
			if ((n = (stack = src.extractItem(i, 65536, true)).getCount()) == 0) continue;
			if (extr != null && (n = (stack = extr.getExtract(stack, src)).getCount()) == 0) continue;
			if (ins != null) {
				if ((n = ins.insertAmount(stack, dst)) == 0) continue;
				stack = ItemHandlerHelper.copyStackWithSize(stack, n);
			}
 			if ((n -= ItemHandlerHelper.insertItemStacked(dst, stack, false).getCount()) > 0) {
 				src.extractItem(i, n, false);
 				m += n;
 			}
		}
		return m;
	}

	public static int findStack(ItemStack item, IItemHandler inv, int p) {
		if (item.isEmpty()) return -1;
		for (int i = p; i < inv.getSlots(); i++)
			if (ItemStack.areItemStacksEqual(item, inv.getStackInSlot(i))) return i;
		return -1;
	}

	public static ItemStack putInSlots(IItemHandler inv, ItemStack stack, int... slots) {
		for (int s : slots)
			if (inv.getStackInSlot(s).getCount() > 0 && (stack = inv.insertItem(s, stack, false)).getCount() == 0)
				return stack;
		for (int s : slots)
			if (inv.getStackInSlot(s).getCount() == 0 && (stack = inv.insertItem(s, stack, false)).getCount() == 0)
				break;
		return stack;
	}

	public static int drain(IItemHandler inv, ItemStack type, int am) {
		int m = 0;
		for (int i = 0; i < inv.getSlots() && m < am; i++) 
			if (ItemHandlerHelper.canItemStacksStack(type, inv.getStackInSlot(i))) {
				ItemStack stack = inv.extractItem(i, am - m, false);
				m += stack.getCount();
			}
		return m;
	}

	public static int drain(IItemHandler inv, OreDictStack ore, ArrayList<ItemStack> buffer) {
		int n = ore.stacksize, m = 0;
		for (int i = 0; i < inv.getSlots() && m < n; i++) 
			if (ore.isEqual(inv.getStackInSlot(i))) {
				ItemStack stack = inv.extractItem(i, n - m, false);
				int x = stack.getCount();
				if (x > 0) {
					m += x;
					addToList(buffer, stack);
				}
			}
		return m;
	}

	public static void addToList(ArrayList<ItemStack> list, ItemStack item) {
		for (ItemStack stack : list)
			if (ItemHandlerHelper.canItemStacksStack(item, stack)) {
				stack.grow(item.getCount());
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
			if (stack.getCount() == 0) continue;
			return ItemHandlerHelper.copyStackWithSize(stack, drain(inv, stack, mss ? stack.getMaxStackSize() : am));
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack drain(IItemHandler inv, ToIntFunction<ItemStack> filter) {
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.extractItem(i, 1, true);
			int m;
			if (stack.getCount() > 0 && (m = filter.applyAsInt(stack)) > 0) {
				int n = inv.extractItem(i, m, false).getCount();
				while (n < m && ++i < inv.getSlots())
					if (ItemHandlerHelper.canItemStacksStack(inv.getStackInSlot(i), stack))
						n += inv.extractItem(i, m, false).getCount();
				stack.setCount(n);
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static final IFluidTankProperties[] NO_TANKS = new IFluidTankProperties[0];

	/** @return acc.{@link IFluidHandler#getTankProperties() getTankProperties()} or {@link #NO_TANKS} */
	public static @Nonnull IFluidTankProperties[] listTanks(IFluidHandler acc) {
		IFluidTankProperties[] p = acc.getTankProperties();
		return p == null ? NO_TANKS : p;
	}

	public static class StackedFluidAccess implements IFluidHandler {

		public final IFluidHandlerItem acc;
		private final int n;

		public StackedFluidAccess(ItemStack item) {
			this.n = item.getCount();
			if (n > 0) {
				this.acc = FluidUtil.getFluidHandler(ItemHandlerHelper.copyStackWithSize(item, 1));
			} else {
				this.acc = null;
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
			ItemStack item = acc.getContainer();
			item.setCount(item.getCount() * n);
			return item;
		}

	}

	/**
	 * drops an ItemStack at the position of given entity
	 * @param stack dropped stack
	 * @param entity entity to drop at
	 */
	public static void dropStack(ItemStack stack, Entity entity) {
		if (stack.isEmpty()) return;
		EntityItem ei = new EntityItem(entity.world, entity.posX, entity.posY, entity.posZ, stack);
		entity.world.spawnEntity(ei);
	}

	/**
	 * drops an ItemStack at the given block position
	 * @param stack dropped stack
	 * @param world the world
	 * @param pos position to drop at
	 */
	public static void dropStack(ItemStack stack, World world, BlockPos pos) {
		if (stack.isEmpty()) return;
		EntityItem ei = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
		world.spawnEntity(ei);
	}

	public static void writeFluidStack(PacketBuffer buf, FluidStack stack) {
		if (stack == null) {
			buf.writeString("");
			return;
		}
		buf.writeString(FluidRegistry.getFluidName(stack));
		buf.writeInt(stack.amount);
		buf.writeCompoundTag(stack.tag);
	}

	public static FluidStack readFluidStack(PacketBuffer buf) throws IOException {
		String s = buf.readString(32767);
		if (s.isEmpty()) return null;
		int n = buf.readInt();
		NBTTagCompound tag = buf.readCompoundTag();
		Fluid fluid = FluidRegistry.getFluid(s);
		if (fluid == null) return null;
		FluidStack stack = new FluidStack(fluid, n);
		stack.tag = tag;
		return stack;
	}

	public static NBTTagCompound createTag(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		return nbt;
	}

	public static NBTTagCompound createTag(NBTTagCompound nbt, String key) {
		if (nbt.hasKey(key, NBT.TAG_COMPOUND)) return nbt.getCompoundTag(key);
		NBTTagCompound tag = new NBTTagCompound();
		nbt.setTag(key, tag);
		return tag;
	}
}
