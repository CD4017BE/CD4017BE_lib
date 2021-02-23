package cd4017be.lib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 
 * @author CD4017BE
 */
public class ItemFluidUtil {

	public static final String Tag_ItemList = "Items", Tag_ItemIndex = "ItIdx", Tag_FluidList = "Fluids", Tag_FluidIndex = "FlIdx";

	public static void loadInventory(ListNBT list, ItemStack[] inv) {
		Arrays.fill(inv, ItemStack.EMPTY);
		for (int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int s = tag.getByte("slot") & 0xff;
			if (s < inv.length) inv[s] = ItemStack.read(tag);
		}
	}

	public static ListNBT saveInventory(ItemStack[] inv) {
		ListNBT list = new ListNBT();
		for (int i = 0; i < inv.length; i++)
			if (!inv[i].isEmpty()) {
				CompoundNBT tag = new CompoundNBT();
				inv[i].write(tag);
				tag.putByte("slot", (byte)i);
				list.add(tag);
			}
		return list;
	}

	public static ItemStack[] loadItems(ListNBT list) {
		ItemStack[] items = new ItemStack[list.size()];
		for (int i = 0; i < items.length; i++)
			items[i] = ItemStack.read(list.getCompound(i));
		return items;
	}

	public static void loadItems(ListNBT list, ItemStack[] items) {
		int m = Math.min(items.length, list.size());
		for (int i = 0; i < m; i++)
			items[i] = ItemStack.read(list.getCompound(i));
		Arrays.fill(items, m, items.length, ItemStack.EMPTY);
	}

	public static ListNBT saveItems(ItemStack[] items) {
		ListNBT list = new ListNBT();
		for (ItemStack item : items)
			if (!item.isEmpty()) {
				CompoundNBT tag = new CompoundNBT();
				item.write(tag);
				list.add(tag);
			}
		return list;
	}

	/**
	 * writes an ItemStack to NBT using 32 bit stacksize resolution
	 * @param item
	 * @return
	 */
	public static CompoundNBT saveItemHighRes(ItemStack item) {
		CompoundNBT nbt = new CompoundNBT();
		item.write(nbt);
		nbt.remove("Count");
		nbt.putInt("Num", item.getCount());
		return nbt;
	}

	/**
	 * loads an ItemStack from NBT using 32 bit stacksize resolution
	 * @param nbt
	 * @return
	 */
	public static ItemStack loadItemHighRes(CompoundNBT nbt) {
		ItemStack item = ItemStack.read(nbt);
		item.setCount(nbt.getInt("Num"));
		return item;
	}

	/**Like {@link PacketBuffer#writeItemStack(ItemStack)} but stores
	 * stacksize with 32-bit precision instead of just 8-bit.
	 * @param buf packet to write
	 * @param stack ItemStack to serialize */
	public static void writeItemHighRes(PacketBuffer buf, ItemStack stack) {
		if (stack.isEmpty()) buf.writeBoolean(false);
		else {
			buf.writeBoolean(true);
			Item item = stack.getItem();
			buf.writeVarInt(Item.getIdFromItem(item));
			buf.writeVarInt(stack.getCount());
			buf.writeCompoundTag(
				item.isDamageable(stack) || item.shouldSyncTag()
					? stack.getShareTag() : null
			);
		}
	}

	/**Like {@link PacketBuffer#readItemStack()} but loads
	 * stacksize with 32-bit precision instead of just 8-bit.
	 * @param buf packet to write
	 * @return deserialized ItemStack
	 * @throws IOException */
	public static ItemStack readItemHighRes(PacketBuffer buf) throws IOException {
		if (!buf.readBoolean()) return ItemStack.EMPTY;
		int i = buf.readVarInt();
		int j = buf.readVarInt();
		ItemStack itemstack = new ItemStack(Item.getItemById(i), j);
		itemstack.readShareTag(buf.readCompoundTag());
		return itemstack;
	}

	public static ListNBT saveFluids(FluidStack[] fluids) {
		ListNBT list = new ListNBT();
		for (FluidStack fluid : fluids)
			if (fluid != null) {
				CompoundNBT tag = new CompoundNBT();
				fluid.writeToNBT(tag);
				list.add(tag);
			}
		return list;
	}

	public static FluidStack[] loadFluids(ListNBT list) {
		FluidStack[] fluids = new FluidStack[list.size()];
		for (int i = 0; i < fluids.length; i++)
			fluids[i] = FluidStack.loadFluidStackFromNBT(list.getCompound(i));
		return fluids;
	}

	/*public static CraftingInventory craftingInventory(ItemStack[] grid, int size) {
		CraftingInventory icr = new CraftingInventory(CraftContDummy, size, size);
		int m = Math.min(grid.length, icr.getSizeInventory());
		for (int i = 0; i < m; i++)
			icr.setInventorySlotContents(i, grid[i]);
		return icr;
	}*/

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
				stack.grow(fluid.getAmount());
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

	/*public static class StackedFluidAccess implements IFluidHandler {

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
		public int fill(FluidStack resource, boolean doFill) {
			if (n > 1) resource = new FluidStack(resource, resource.getAmount() / n);
			return acc.fill(resource, doFill) * n;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			if (n > 1) resource = new FluidStack(resource, resource.getAmount() / n);
			FluidStack stack = acc.drain(resource, doDrain);
			if (stack != null) stack.getAmount() *= n;
			return stack;
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			FluidStack stack = acc.drain(maxDrain / n, doDrain);
			if (stack != null) stack.getAmount() *= n;
			return stack;
		}

		public ItemStack result() {
			ItemStack item = acc.getContainer();
			item.setCount(item.getCount() * n);
			return item;
		}

	}*/

	/**
	 * drops an ItemStack at the position of given entity
	 * @param stack dropped stack
	 * @param entity entity to drop at
	 */
	public static void dropStack(ItemStack stack, Entity entity) {
		if (stack.isEmpty()) return;
		ItemEntity ei = new ItemEntity(entity.world, entity.getPosX(), entity.getPosY(), entity.getPosZ(), stack);
		entity.world.addEntity(ei);
	}

	/**
	 * drops an ItemStack at the given block position
	 * @param stack dropped stack
	 * @param world the world
	 * @param pos position to drop at
	 */
	public static void dropStack(ItemStack stack, World world, BlockPos pos) {
		if (stack.isEmpty()) return;
		ItemEntity ei = new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
		world.addEntity(ei);
	}

	public static void writeFluidStack(PacketBuffer buf, FluidStack stack) {
		buf.writeFluidStack(stack);
	}

	public static FluidStack readFluidStack(PacketBuffer buf) throws IOException {
		return buf.readFluidStack();
	}

	public static CompoundNBT createTag(ItemStack stack) {
		CompoundNBT nbt = stack.getTag();
		if (nbt == null) stack.setTag(nbt = new CompoundNBT());
		return nbt;
	}

	public static CompoundNBT createTag(CompoundNBT nbt, String key) {
		if (nbt.contains(key, NBT.TAG_COMPOUND)) return nbt.getCompound(key);
		CompoundNBT tag = new CompoundNBT();
		nbt.put(key, tag);
		return tag;
	}

	public static Item item(String id) {
		return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
	}

}
