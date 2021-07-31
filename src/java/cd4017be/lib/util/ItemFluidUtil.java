package cd4017be.lib.util;

import static net.minecraftforge.fluids.FluidAttributes.BUCKET_VOLUME;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidAttributes;
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

	public static boolean canSlotStack(ItemStack slot, ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (slot.isEmpty()) return true;
		return slot.sameItem(stack) && (slot.hasTag()
			? slot.getTag().equals(stack.getTag()) && slot.areCapsCompatible(stack)
			: !stack.hasTag()
		);
	}

	public static void loadInventory(ListTag list, ItemStack[] inv) {
		Arrays.fill(inv, ItemStack.EMPTY);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompound(i);
			int s = tag.getByte("slot") & 0xff;
			if (s < inv.length) inv[s] = ItemStack.of(tag);
		}
	}

	public static ListTag saveInventory(ItemStack[] inv) {
		ListTag list = new ListTag();
		for (int i = 0; i < inv.length; i++)
			if (!inv[i].isEmpty()) {
				CompoundTag tag = new CompoundTag();
				inv[i].save(tag);
				tag.putByte("slot", (byte)i);
				list.add(tag);
			}
		return list;
	}

	public static ItemStack[] loadItems(ListTag list) {
		ItemStack[] items = new ItemStack[list.size()];
		for (int i = 0; i < items.length; i++)
			items[i] = ItemStack.of(list.getCompound(i));
		return items;
	}

	public static void loadItems(ListTag list, ItemStack[] items) {
		int m = Math.min(items.length, list.size());
		for (int i = 0; i < m; i++)
			items[i] = ItemStack.of(list.getCompound(i));
		Arrays.fill(items, m, items.length, ItemStack.EMPTY);
	}

	public static ListTag saveItems(ItemStack[] items) {
		ListTag list = new ListTag();
		for (ItemStack item : items)
			if (!item.isEmpty()) {
				CompoundTag tag = new CompoundTag();
				item.save(tag);
				list.add(tag);
			}
		return list;
	}

	/**
	 * writes an ItemStack to NBT using 32 bit stacksize resolution
	 * @param item
	 * @return
	 */
	public static CompoundTag saveItemHighRes(ItemStack item) {
		CompoundTag nbt = new CompoundTag();
		item.save(nbt);
		nbt.remove("Count");
		nbt.putInt("Num", item.getCount());
		return nbt;
	}

	/**
	 * loads an ItemStack from NBT using 32 bit stacksize resolution
	 * @param nbt
	 * @return
	 */
	public static ItemStack loadItemHighRes(CompoundTag nbt) {
		ItemStack item = ItemStack.of(nbt);
		item.setCount(nbt.getInt("Num"));
		return item;
	}

	/**Like {@link FriendlyByteBuf#writeItemStack(ItemStack)} but stores
	 * stacksize with 32-bit precision instead of just 8-bit.
	 * @param buf packet to write
	 * @param stack ItemStack to serialize */
	public static void writeItemHighRes(FriendlyByteBuf buf, ItemStack stack) {
		if (stack.isEmpty()) buf.writeBoolean(false);
		else {
			buf.writeBoolean(true);
			Item item = stack.getItem();
			buf.writeVarInt(Item.getId(item));
			buf.writeVarInt(stack.getCount());
			buf.writeNbt(
				item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()
					? stack.getShareTag() : null
			);
		}
	}

	/**Like {@link FriendlyByteBuf#readItemStack()} but loads
	 * stacksize with 32-bit precision instead of just 8-bit.
	 * @param buf packet to write
	 * @return deserialized ItemStack
	 * @throws IOException */
	public static ItemStack readItemHighRes(FriendlyByteBuf buf) throws IOException {
		if (!buf.readBoolean()) return ItemStack.EMPTY;
		int i = buf.readVarInt();
		int j = buf.readVarInt();
		ItemStack itemstack = new ItemStack(Item.byId(i), j);
		itemstack.readShareTag(buf.readNbt());
		return itemstack;
	}

	public static ListTag saveFluids(FluidStack[] fluids) {
		ListTag list = new ListTag();
		for (FluidStack fluid : fluids)
			if (fluid != null) {
				CompoundTag tag = new CompoundTag();
				fluid.writeToNBT(tag);
				list.add(tag);
			}
		return list;
	}

	public static FluidStack[] loadFluids(ListTag list) {
		FluidStack[] fluids = new FluidStack[list.size()];
		for (int i = 0; i < fluids.length; i++)
			fluids[i] = FluidStack.loadFluidStackFromNBT(list.getCompound(i));
		return fluids;
	}

	public static void readBytes(byte[] dst, byte[] src, byte fallback) {
		int sl = src.length, dl = dst.length;
		System.arraycopy(src, 0, dst, 0, Math.min(sl, dl));
		if (sl < dl) Arrays.fill(dst, sl, dl, fallback);
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
			if (ItemStack.matches(item, inv.getStackInSlot(i))) return i;
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
		ItemEntity ei = new ItemEntity(entity.level, entity.getX(), entity.getY(), entity.getZ(), stack);
		entity.level.addFreshEntity(ei);
	}

	/**
	 * drops an ItemStack at the given block position
	 * @param stack dropped stack
	 * @param world the world
	 * @param pos position to drop at
	 */
	public static void dropStack(ItemStack stack, Level world, BlockPos pos) {
		if (stack.isEmpty()) return;
		ItemEntity ei = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		ei.setDefaultPickUpDelay();
		world.addFreshEntity(ei);
	}

	public static void writeFluidStack(FriendlyByteBuf buf, FluidStack stack) {
		buf.writeFluidStack(stack);
	}

	public static FluidStack readFluidStack(FriendlyByteBuf buf) throws IOException {
		return buf.readFluidStack();
	}

	public static CompoundTag createTag(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		if (nbt == null) stack.setTag(nbt = new CompoundTag());
		return nbt;
	}

	public static CompoundTag createTag(CompoundTag nbt, String key) {
		if (nbt.contains(key, NBT.TAG_COMPOUND)) return nbt.getCompound(key);
		CompoundTag tag = new CompoundTag();
		nbt.put(key, tag);
		return tag;
	}

	public static Item item(String id) {
		return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
	}

	public static FluidStack drainFluid(Level world, BlockPos pos, Predicate<FluidStack> filter) {
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (!(block instanceof BucketPickup)) return FluidStack.EMPTY;
		FluidState fstate = state.getFluidState();
		if (!fstate.isSource() || !filter.test(new FluidStack(fstate.getType(), BUCKET_VOLUME)))
			return FluidStack.EMPTY;
		ItemStack stack = ((BucketPickup)block).pickupBlock(world, pos, state);
		Item item = stack.getItem();
		return !stack.isEmpty() && item instanceof BucketItem ?
			new FluidStack(((BucketItem)item).getFluid(), BUCKET_VOLUME)
			: FluidStack.EMPTY;
	}

	public static boolean placeFluid(Level world, BlockPos pos, FluidStack stack) {
		Fluid fluid = stack.getFluid();
		FluidAttributes attr = fluid.getAttributes();
		if (fluid == Fluids.EMPTY || !attr.canBePlacedInWorld(world, pos, stack)) return false;
		BlockState blockstate = world.getBlockState(pos);
		Block block = blockstate.getBlock();
		if (block instanceof LiquidBlockContainer)
			return ((LiquidBlockContainer)block).placeLiquid(
				world, pos, blockstate,
				attr.getStateForPlacement(world, pos, stack)
			);
		if (!world.isEmptyBlock(pos)) {
			Material material = blockstate.getMaterial();
			if (material.isSolid() && !blockstate.canBeReplaced(fluid)) return false;
			if (!material.isLiquid()) world.destroyBlock(pos, true);
		}
		return world.setBlock(pos, attr.getBlock(world, pos, fluid.defaultFluidState()), 11);
	}

/*
	public static class ItemType {
		public final ItemStack[] types;
		public final boolean meta;
		public final boolean nbt;
		public final int[] ores;
		/**
		 * An ItemType that matches all items
		 */
/*		public ItemType() 
		{
			this.types = null;
			this.ores = null;
			this.meta = false;
			this.nbt = false;
		}
		/**
		 * An ItemType that matches only the exact given items
		 * @param types the items to match
		 */
/*		public ItemType(ItemStack... types)
		{
			this.types = types;
			this.ores = null;
			this.meta = true;
			this.nbt = true;
		}
		/**
		 * This ItemType matches the given items with special flags
		 * @param meta Metadata flag (false = ignore different metadata)
		 * @param nbt NBT-data flag (false = ignore different NBT-data)
		 * @param ore OreDictionary flag (true = also matches if equal ore types)
		 * @param types the items to match
		 */
/*		public ItemType(boolean meta, boolean nbt, boolean ore, ItemStack... types)
		{
			this.types = types;
			this.meta = meta;
			this.nbt = nbt;
			if (ore) {
				Set<Integer> list = new HashSet<Integer>();
				for (int i = 0; i < types.length; i++)
					for (int j : OreDictionary.getOreIDs(types[i])) 
						list.add(j);
				ores = new int[list.size()];
				int n = 0;
				for (int i : list) ores[n++] = i;
			} else ores = null;
		}
		
		public boolean matches(ItemStack item) 
		{
			return getMatch(item) >= 0;
		}
		
		public int getMatch(ItemStack item)
		{
			if (item.isEmpty()) return -1;
			else if (types == null) return -1;
			for (int i = 0; i < types.length; i++) {
				ItemStack type = types[i];
				if (item.getItem() == type.getItem() && 
					(!meta || item.getItemDamage() == type.getItemDamage()) &&
					(!nbt || ItemStack.areItemStackTagsEqual(item, type)))
					return i;
			}
			if (ores == null) return -1;
			for (int o : OreDictionary.getOreIDs(item))
				for (int i = 0; i < ores.length; i++)
					if (ores[i] == o) return i;
			return -1;
		}
		
	}
*/

}
