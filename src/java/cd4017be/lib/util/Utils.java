/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.util;

import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Set;

import cd4017be.lib.ModTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

/**
 *
 * @author CD4017BE
 */
public class Utils 
{
	public static EnumFacing[][] AXIS_Rad = {
		{EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH},
		{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST},
		{EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP}
	};
	
	public static final BlockPos NOWHERE = new BlockPos(0, -1, 0);
	public static final byte IN = -1, OUT = 1, ACC = 0;
	
	public static boolean itemsEqual(ItemStack item0, ItemStack item1)
	{
		return (item0 == null && item1 == null) || (item0 != null && item1 != null && item0.isItemEqual(item1) && ItemStack.areItemStackTagsEqual(item0, item1));
	}
	
	public static boolean oresEqual(ItemStack item0, ItemStack item1)
	{
		if (itemsEqual(item0, item1)) return true;
		else
		{
			int[] ids = OreDictionary.getOreIDs(item0);
			for (int id1 : OreDictionary.getOreIDs(item1))
				for (int id0 : ids) if (id0 == id1) return true;
			return false;
		}
	}
	
	public static boolean fluidsEqual(FluidStack fluid0, FluidStack fluid1, boolean am)
	{
		if (fluid0 == null && fluid1 == null) return true;
		else if (fluid0 != null && fluid1 != null) {
			return am ? fluid0.isFluidStackIdentical(fluid1) : fluid0.isFluidEqual(fluid1);
		} else return false;
	}

	/**
	 * Get all slots of the inventory that can be accessed from given side
	 * @param inv the inventory to access
	 * @param side the side to access from
	 * @return the accessible slots
	 */
	public static int[] accessibleSlots(IInventory inv, int side) {
		if (inv instanceof ISidedInventory) {
			return ((ISidedInventory) inv).getSlotsForFace(EnumFacing.VALUES[side]);
		} else {
			int[] s = new int[inv.getSizeInventory()];
			for (int i = 0; i < s.length; i++) {
				s[i] = i;
			}
			return s;
		}
	}

	/**
	 * Fills items into the given slots of an inventory
	 * @param inv the inventory to fill in
	 * @param side the side to fill from
	 * @param s the slots that should be filled
	 * @param items the items to fill
	 * @return the remaining items that could not be filled
	 */
	public static ItemStack[] fill(IInventory inv, int side, int[] s, ItemStack... items) {
		ISidedInventory si = inv instanceof ISidedInventory ? (ISidedInventory) inv : null;
		boolean action = false;
		int pn = -1;
		for (int i = 0; i < s.length; i++) {
			ItemStack stack = inv.getStackInSlot(s[i]);
			if (stack == null) {
				if (pn < 0) {
					pn = i;
				}
			} else if (si == null || si.canInsertItem(s[i], stack, EnumFacing.VALUES[side])) {
				stack = stack.copy();
				boolean f = false;
				int m = Math.min(stack.getMaxStackSize(), inv.getInventoryStackLimit());
				for (int j = 0; j < items.length && stack.stackSize < m; j++) {
					if (items[j] != null && Utils.itemsEqual(items[j], stack)) {
						if (items[j].stackSize <= m - stack.stackSize) {
							stack.stackSize += items[j].stackSize;
							items[j] = null;
						} else {
							items[j].stackSize -= m - stack.stackSize;
							stack.stackSize = m;
						}
						f = true;
					}
				}
				if (f) {
					inv.setInventorySlotContents(s[i], stack);
					action = true;
				}
			}
		}
		ItemStack[] array = new ItemStack[items.length];
		int n = 0;
		if (pn < 0) {
			pn = s.length;
		}
		for (int i = 0; i < items.length; i++) {
			int m = items[i] == null ? 0 : Math.min(items[i].getMaxStackSize(), inv.getInventoryStackLimit());
			while (items[i] != null && pn < s.length) {
				if (inv.getStackInSlot(s[pn]) == null && (si == null || si.canInsertItem(s[pn], items[i], EnumFacing.VALUES[side]))) {
					if (items[i].stackSize <= m) {
						inv.setInventorySlotContents(s[pn], items[i]);
						items[i] = null;
					} else {
						inv.setInventorySlotContents(s[pn], items[i].splitStack(m));
					}
					action = true;
				}
				pn++;
			}
			if (items[i] != null) {
				array[n++] = items[i];
			}
		}
		items = new ItemStack[n];
		if (action) {
			inv.markDirty();
		}
		System.arraycopy(array, 0, items, 0, n);
		return items;
	}
	
	public static ItemStack fillStack(IInventory inv, int side, int[] s, ItemStack item)
	{
		ISidedInventory si = inv instanceof ISidedInventory ? (ISidedInventory) inv : null;
		int m = Math.min(item.getMaxStackSize(), inv.getInventoryStackLimit());
		boolean action = false;
		int pn = -1;
		for (int i = 0; i < s.length && item != null; i++) {
			ItemStack stack = inv.getStackInSlot(s[i]);
			if (stack == null) {
				if (pn < 0) {
					pn = i;
				}
			} else if (stack.stackSize < m && stack.isItemEqual(item) && (si == null || si.canInsertItem(s[i], item, EnumFacing.VALUES[side]))) {
				if (item.stackSize <= m - stack.stackSize) {
					stack.stackSize += item.stackSize;
					item = null;
				} else {
					item.stackSize -= m - stack.stackSize;
					stack.stackSize = m;
				}
				inv.setInventorySlotContents(s[i], stack);
				action = true;
			}
		}
		if (pn < 0) {
			pn = s.length;
		}
		while (item != null && pn < s.length) {
			if (inv.getStackInSlot(s[pn]) == null && (si == null || si.canInsertItem(s[pn], item, EnumFacing.VALUES[side]))) {
				if (item.stackSize <= m) {
				  	inv.setInventorySlotContents(s[pn], item);
				   	item = null;
				} else {
				   	inv.setInventorySlotContents(s[pn], item.splitStack(m));
				}
				action = true;
			}
			pn++;
		}
		if (action) inv.markDirty();
		return item;
	}
	
	public static Obj2<int[], ItemStack[]> getFilledSlots(IInventory inv, int[] slots, int side, boolean fill)
	{
		ISidedInventory invS = inv instanceof ISidedInventory ? (ISidedInventory)inv : null;
		int[] outS = new int[slots.length];
		ItemStack[] outI = new ItemStack[slots.length];
		int n = 0;
		ItemStack item;
		for (int s : slots) {
			item = inv.getStackInSlot(s);
			if (item != null && (!fill || item.stackSize < item.getMaxStackSize()) && (invS == null || (fill ? invS.canExtractItem(s, item, EnumFacing.VALUES[side]) : invS.canInsertItem(s, item, EnumFacing.VALUES[side])))) {
				outS[n] = s; 
				outI[n++] = item;
			}
		}
		if (n == 0) return null;
		Obj2<int[], ItemStack[]> ret = new Obj2<int[], ItemStack[]>(new int[n], new ItemStack[n]);
		System.arraycopy(outS, 0, ret.objA, 0, n);
		System.arraycopy(outI, 0, ret.objB, 0, n);
		return ret;
	}
	
	public static int getEmptySlot(IInventory inv, int[] slots, int side, ItemStack type)
	{
		ISidedInventory invS = inv instanceof ISidedInventory ? (ISidedInventory)inv : null;
		ItemStack item;
		for (int s : slots) {
			item = inv.getStackInSlot(s);
			if (item == null && (invS == null || invS.canInsertItem(s, item, EnumFacing.VALUES[side]))) return s;
		}
		return -1;
	}

	/**
	 * Transfer items from one inventory to another
	 * @param src the source inventory
	 * @param sideS the source access side
	 * @param sS the slots to use as source (may be modified)
	 * @param dst the destination inventory
	 * @param sideD the destination access side
	 * @param sD the slots to use as destination
	 * @param type the type of items to transfer
	 */
	public static void transfer(IInventory src, int sideS, int[] sS, IInventory dst, int sideD, int[] sD, ItemType type) {
		ISidedInventory srcS = src instanceof ISidedInventory ? (ISidedInventory) src : null;
		ISidedInventory dstS = dst instanceof ISidedInventory ? (ISidedInventory) dst : null;
		boolean done;
		for (int i : sS) {
			ItemStack curItem = src.getStackInSlot(i);
			if (curItem != null && type.matches(curItem) && (srcS == null || srcS.canExtractItem(i, curItem, EnumFacing.VALUES[sideS]))) {
				int m = Math.min(curItem.getMaxStackSize(), dst.getInventoryStackLimit());
				int p = -1;
				done = false;
				for (int j : sD) {
					ItemStack stack = dst.getStackInSlot(j);
					if (stack == null && p == -1 && (dstS == null || dstS.canInsertItem(j, curItem, EnumFacing.VALUES[sideD]))) p = j;
					else if (Utils.itemsEqual(curItem, stack) && stack.stackSize < m) {
						done = true;
						int n = m - stack.stackSize;
						ItemStack item = src.decrStackSize(i, n);
						stack = dst.getStackInSlot(j);
						if (item != null) item.stackSize += stack == null ? 0 : stack.stackSize;
						else break;
						dst.setInventorySlotContents(j, item);
						curItem = src.getStackInSlot(i);
						if (curItem == null) break;
					}
				}
				if (p >= 0) {
					dst.setInventorySlotContents(p, src.decrStackSize(i, m));
					done = true;
				}
				if (done) {
					src.markDirty();
					dst.markDirty();
					return;
				}
			}
		}
	}

	/**
	 * Drains only from one stack of the given slots of the inventory that has the highest stacksize and matches type
	 * @param inv the inventory to drain
	 * @param side the side to drain from
	 * @param s the slots that should be drained
	 * @param type the alowed type
	 * @param am the total amount of items to drain
	 * @return
	 */
	public static ItemStack drainStack(IInventory inv, int side, int[] s, ItemType type, int am) {
		ISidedInventory si = inv instanceof ISidedInventory ? (ISidedInventory) inv : null;
		int max = 0;
		int pos = -1;
		for (int i = 0; i < s.length; i++) {
			ItemStack stack = inv.getStackInSlot(s[i]);
			if (stack != null && type.matches(stack) && (si == null || si.canExtractItem(s[i], stack, EnumFacing.VALUES[side]) && stack.stackSize > max)) {
				pos = s[i];
				max = stack.stackSize;
			}
		}
		if (pos >= 0) {
			ItemStack stack = inv.decrStackSize(pos, am);
			inv.markDirty();
			return stack;
		} else {
			return null;
		}
	}

	/**
	 * Drains items from the given slots of the inventory
	 * @param inv the inventory to drain
	 * @param side the side to drain from
	 * @param s the slots that should be drained
	 * @param am the total amout of items to drain
	 * @param type the alowed type
	 * @return the drained items (these may form stacks of more than maxStackSize)
	 */
	public static ItemStack[] drain(IInventory inv, int side, int[] s, ItemType type, int am) {
		ISidedInventory si = inv instanceof ISidedInventory ? (ISidedInventory) inv : null;
		ItemStack[] array = new ItemStack[s.length];
		int n = 0;
		for (int i : s) {
			ItemStack stack = inv.getStackInSlot(i);
			if (type.matches(stack) && (si == null || si.canExtractItem(i, stack, EnumFacing.VALUES[side]))) {
				ItemStack item = inv.decrStackSize(i, am);
				am -= item.stackSize;
				for (int j = 0; j < array.length; j++) {
					if (array[j] == null) {
						array[j] = item;
						if (j > n) {
							n = j;
						}
						break;
					} else if (Utils.itemsEqual(array[j], item)) {
						array[j].stackSize += item.stackSize;
						break;
					}
				}
				if (am <= 0) {
					break;
				}
			}
		}
		ItemStack[] items = new ItemStack[n];
		System.arraycopy(array, 0, items, 0, n);
		return items;
	}

	public static TileEntity getTileOnSide(ModTileEntity tileEntity, byte s) {
		if (tileEntity == null) {
			return null;
		}
		int x = tileEntity.getPos().getX();
		int y = tileEntity.getPos().getY();
		int z = tileEntity.getPos().getZ();
		if (s == 0) {
			y--;
		} else if (s == 1) {
			y++;
		} else if (s == 2) {
			z--;
		} else if (s == 3) {
			z++;
		} else if (s == 4) {
			x--;
		} else if (s == 5) {
			x++;
		}
		return tileEntity.getLoadedTile(new BlockPos(x, y, z));
	}
	
	public static class ItemType {
		public final ItemStack[] types;
		public final boolean meta;
		public final boolean nbt;
		public final int[] ores;
		/**
		 * An ItemType that matches all items
		 */
		public ItemType() 
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
		public ItemType(ItemStack... types)
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
		public ItemType(boolean meta, boolean nbt, boolean ore, ItemStack... types)
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
			if (item == null) return false;
			else if (types == null) return true;
			for (ItemStack type : types) {
				if (type == null) continue;
				if (item.getItem() == type.getItem() && 
					(!meta || item.getItemDamage() == type.getItemDamage()) &&
					(!nbt || ItemStack.areItemStackTagsEqual(item, type)))
					return true;
			}
			if (ores == null) return false;
			for (int o : OreDictionary.getOreIDs(item))
				for (int i : ores)
					if (i == o) return true;
			return false;
		}
		
		public int getMatch(ItemStack item)
		{
			if (item == null) return -1;
			else if (types == null) return -1;
			for (int i = 0; i < types.length; i++) {
				ItemStack type = types[i];
				if (type == null) continue;
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
	
	private static final String[] DecScale  = {"a", "f", "p", "n", "u", "m", "", "k", "M", "G", "T", "P", "E"};
	private static final int ofsDecScale = 6;
	
	/**
	 * @param x number
	 * @param w significant digits
	 * @param c clip below exponent of 10
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w, int c)
	{
		double s = Math.signum(x);
		if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) return "" + x;
		int o = (int)Math.floor(Math.log10(x * s)) + 3 * ofsDecScale;
		int p = (o + c) / 3;
		int n = w - o + p * 3 - 1;
		if (p < 0) return "0";
		else if (p > DecScale.length) return "" + (s == -1 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		x *= Math.pow(0.001, p - ofsDecScale);
		String tex = String.format("%." + n + "f", x) + DecScale[p];
		String ds = "" + DecimalFormatSymbols.getInstance().getDecimalSeparator();
		if (tex.contains(ds)) {
			while(tex.endsWith("0")) tex = tex.substring(0, tex.length() - 1);
			if (tex.endsWith(ds)) tex = tex.substring(0, tex.length() - 1);
		}
		return tex;
	}
	
	/**
	 * @param x number
	 * @param w max fractal digits
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w) {
		String tex = String.format("%." + w + "f", x);
		String ds = "" + DecimalFormatSymbols.getInstance().getDecimalSeparator();
		if (tex.contains(ds)) {
			while(tex.endsWith("0")) tex = tex.substring(0, tex.length() - 1);
			if (tex.endsWith(ds)) tex = tex.substring(0, tex.length() - 1);
		}
		return tex;
	}
	
	public static FluidStack getFluid(World world, BlockPos pos, boolean sourceOnly)
	{
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block == Blocks.AIR) return null;
		else if (block instanceof IFluidBlock) {
			FluidStack fluid = ((IFluidBlock)block).drain(world, pos, false);
			if (!sourceOnly && fluid == null) return new FluidStack(((IFluidBlock)block).getFluid(), 0);
			else return fluid;
		}
		boolean source = state == block.getDefaultState();
		if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) return source || !sourceOnly ? new FluidStack(FluidRegistry.WATER, source ? 1000 : 0) : null;
		else if (block == Blocks.LAVA|| block == Blocks.FLOWING_LAVA) return source || !sourceOnly ? new FluidStack(FluidRegistry.LAVA, source ? 1000 : 0) : null;
		else return null;
	}
	
	public static FluidStack getFluid(ItemStack item)
	{
		if (item == null) return null;
		FluidStack fluid;
		if (item.getItem() instanceof IFluidContainerItem) fluid = ((IFluidContainerItem)item.getItem()).getFluid(item);
		else  fluid = FluidContainerRegistry.getFluidForFilledItem(item);
		if (fluid != null) fluid.amount *= item.stackSize;
		return fluid;
	}
	
	public static Obj2<ItemStack, FluidStack> drainFluid(ItemStack item, int am)
	{
		if (item == null || item.stackSize == 0) return new Obj2<ItemStack, FluidStack>();
		Obj2<ItemStack, FluidStack> ret = new Obj2<ItemStack, FluidStack>(item.copy(), null);
		int n = item.stackSize;
		am /= n;
		if (am <= 0) return ret;
		if(item.getItem() instanceof IFluidContainerItem) {
			IFluidContainerItem cont = (IFluidContainerItem)item.getItem();
			ret.objB = cont.drain(ret.objA, am, true);
			if (ret.objB != null) ret.objB.amount *= n;
			if (ret.objA.stackSize <= 0) ret.objA = null;
		} else {
			ret.objB = FluidContainerRegistry.getFluidForFilledItem(item);
			if (ret.objB != null && ret.objB.amount <= am) {
				ret.objA = FluidContainerRegistry.drainFluidContainer(item);
				if (ret.objA != null) ret.objA.stackSize *= n;
				ret.objB.amount *= n;
			} else ret.objB = null;
		}
		return ret;
	}
	
	public static Obj2<ItemStack, Integer> fillFluid(ItemStack item, FluidStack fluid)
	{
		if (item == null || item.stackSize == 0) return new Obj2<ItemStack, Integer>(null, 0);
		Obj2<ItemStack, Integer> ret = new Obj2<ItemStack, Integer>(item.copy(), 0);
		int n = item.stackSize;
		if (fluid == null || fluid.amount < n) return ret;
		FluidStack stack = fluid.copy();
		stack.amount /= n;
		if(item.getItem() instanceof IFluidContainerItem) {
			IFluidContainerItem cont = (IFluidContainerItem)item.getItem();
			ret.objB = cont.fill(ret.objA, stack, true) * n;
			if (ret.objA.stackSize <= 0) ret.objA = null;
		} else if (FluidContainerRegistry.isEmptyContainer(item)){
			ret.objB = FluidContainerRegistry.getContainerCapacity(stack, item);
			if (ret.objB != 0 && ret.objB <= stack.amount) {
				ret.objA = FluidContainerRegistry.fillFluidContainer(stack, item);
				if (ret.objA != null) ret.objA.stackSize *= n;
				ret.objB *= n;
			} else ret.objB = 0;
		}
		return ret;
	}
	
	public static int findStack(ItemStack item, IInventory inv, int[] s, int start)
	{
		if (item == null) return -1;
		for (int i = start; i < s.length; i++) {
			if (itemsEqual(item, inv.getStackInSlot(s[i]))) return i;
		}
		return -1;
	}
	
	public static int mod(int a, int b)
	{
		return a < 0 ? b - (-a - 1) % b - 1 : a % b;
	}
	
	public static int div(int a, int b)
	{
		return a < 0 ? -1 - (-a - 1) / b: a / b;
	}
	
	public static byte getLookDir(Entity entity)
	{
		if (entity.rotationPitch < -45.0F) return 0;
		if (entity.rotationPitch > 45.0F) return 1;
		int d = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		if (d == 0) return 2;
		if (d == 1) return 5;
		if (d == 2) return 3;
		return 4;
	}
	
}
