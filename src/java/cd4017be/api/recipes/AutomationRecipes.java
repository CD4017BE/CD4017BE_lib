package cd4017be.api.recipes;

import cd4017be.lib.templates.Inventory;
import cd4017be.lib.templates.TankContainer;
import cd4017be.lib.util.Obj2;
import cd4017be.lib.util.OreDictStack;
import cd4017be.lib.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 *
 * @author CD4017BE
 */
public class AutomationRecipes
{
	
	public static class LFRecipe
	{
		public FluidStack Linput;
		public Object[] Iinput;
		public FluidStack Loutput;
		public ItemStack[] Ioutput;
		public float energy;
		
		public LFRecipe(FluidStack Linput, Object[] Iinput, FluidStack Loutput, ItemStack[] Ioutput, float energy)
		{
			this.Linput = Linput;
			this.Loutput = Loutput;
			if (Iinput != null) {
				for (int i = 0; i < Iinput.length; i++)
					if (Iinput[i] instanceof String)
						Iinput[i] = OreDictStack.deserialize((String)Iinput[i]);
				this.Iinput = Iinput;
			} else this.Iinput = new Object[0];
			if (Ioutput != null) this.Ioutput = Ioutput;
			else this.Ioutput = new ItemStack[0];
			this.energy = energy;
		}
		
		private LFRecipe() {}
		
		public boolean matches(FluidStack liquid, ItemStack[] items)
		{
			if (Linput != null && (liquid == null || !liquid.containsFluid(Linput))) return false;
			else if (Iinput != null)
			{
				if (items == null) return false;
				for (Object item : Iinput)
				{
					if (item == null) continue;
					int n = getStacksize(item);
					for (ItemStack i : items)
						if (isItemEqual(i, item)) n -= i.stackSize;
					if (n > 0) return false;
				}
			}
			return true;
		}
		
		public LFRecipe copy()
		{
			LFRecipe recipe = new LFRecipe();
			recipe.Linput = Linput == null ? null : Linput.copy();
			recipe.Loutput = Loutput == null ? null : Loutput.copy();
			recipe.Iinput = new Object[Iinput.length];
			for (int i = 0; i < Iinput.length; i++)
				recipe.Iinput[i] = Iinput[i] == null ? null : Iinput[i] instanceof ItemStack ? ((ItemStack)Iinput[i]).copy() : ((OreDictStack)Iinput[i]).copy();
			recipe.Ioutput = new ItemStack[Ioutput.length];
			for (int i = 0; i < Ioutput.length; i++)
				recipe.Ioutput[i] = Ioutput[i] == null ? null : Ioutput[i].copy();
			recipe.energy = energy;
			return recipe;
		}
		
	}
	
	private static List<LFRecipe> lfRecipes = new ArrayList<LFRecipe>();
	
	public static List<LFRecipe> getAdvancedFurnaceRecipes()
	{
		return lfRecipes;
	}
	
	public static void addRecipe(LFRecipe recipe)
	{
		lfRecipes.add(recipe);
	}
	
	public static LFRecipe getRecipeFor(FluidStack liquid, ItemStack[] items)
	{
		Iterator<LFRecipe> iterator = lfRecipes.iterator();
		while(iterator.hasNext())
		{
			LFRecipe recipe = iterator.next();
			if (recipe.matches(liquid, items)) return recipe.copy();
		}
		return null;
	}
	
	public static LFRecipe readFromNBT(NBTTagCompound nbt)
	{
		NBTTagList list = nbt.getTagList("Items", 10);
		ItemStack[] items = new ItemStack[list.tagCount()];
		for (int id = 0; id < items.length; ++id)
		{
			NBTTagCompound tag = list.getCompoundTagAt(id);
			items[id] = new ItemStack(tag);
		}
		FluidStack liquid = FluidStack.loadFluidStackFromNBT(nbt);
		float energy = nbt.getFloat("Energy");
		return new LFRecipe(null, null, liquid, items, energy);
	}
	
	public static NBTTagCompound writeToNBT(LFRecipe recipe)
	{
		NBTTagCompound nbt = new NBTTagCompound();
		if (recipe.Ioutput != null)
		{
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < recipe.Ioutput.length; ++i)
			{
				if (recipe.Ioutput[i] != null)
				{
					NBTTagCompound tag = new NBTTagCompound();
					recipe.Ioutput[i].writeToNBT(tag);
					list.appendTag(tag);
				}
			}
			nbt.setTag("Items", list);
		}
		if (recipe.Loutput != null)
		{
			recipe.Loutput.writeToNBT(nbt);
		}
		nbt.setFloat("Energy", recipe.energy);
		return nbt;
	}
	
	public static boolean isItemEqual(ItemStack item, Object obj)
	{
		if (obj == null && item == null) return true;
		if (obj instanceof ItemStack) return Utils.itemsEqual(item, (ItemStack)obj);
		if (obj instanceof OreDictStack) return ((OreDictStack)obj).isEqual(item);
		return false;
	}
	
	public static int getStacksize(Object obj)
	{
		if (obj == null) return 0;
		if (obj instanceof ItemStack) return ((ItemStack)obj).stackSize;
		if (obj instanceof OreDictStack) return ((OreDictStack)obj).stacksize;
		return 0;
	}
	
	public static class CmpRecipe
	{
		public Object[] input;
		public ItemStack output;
		
		public CmpRecipe(Object[] in, ItemStack out)
		{
			input = in;
			for (int i = 0; i < input.length; i++)
				if (input[i] instanceof String)
					input[i] = OreDictStack.deserialize((String)input[i]);
			output = out;
		}
		
		public boolean matches(ItemStack[] inventory, int s, int e)
		{
			if (e - s < input.length) return false;
			for (int i = 0; i < input.length; i++) {
				if (input[i] == null) {
					if (inventory[s + i] != null) return false;
					else continue;
				} else if (input[i] instanceof ItemStack) {
					if (!Utils.itemsEqual((ItemStack)input[i], inventory[s + i]) || inventory[s + i].stackSize < ((ItemStack)input[i]).stackSize) return false;
					else continue;
				} else if (input[i] instanceof OreDictStack) {
					OreDictStack obj = (OreDictStack)input[i];
					if (inventory[s + i] == null || inventory[s + i].stackSize < obj.stacksize || !obj.isEqual(inventory[s + i])) return false;
				}
			}
			return true;
		}
	}
	
	private static List<CmpRecipe> cmpRecipes = new ArrayList<CmpRecipe>();
	
	public static List<CmpRecipe> getCompressorRecipes()
	{
		return cmpRecipes;
	}
	
	public static void addRecipe(CmpRecipe recipe)
	{
		cmpRecipes.add(recipe);
	}
	
	public static void addCmpRecipe(ItemStack out, Object... in)
	{
		cmpRecipes.add(new CmpRecipe(in, out));
	}
	
	public static CmpRecipe getRecipeFor(ItemStack[] inventory, int s, int e)
	{
		Iterator<CmpRecipe> iterator = cmpRecipes.iterator();
		while(iterator.hasNext())
		{
			CmpRecipe recipe = iterator.next();
			if (recipe.matches(inventory, s, e)) return recipe;
		}
		return null;
	}
	
	public static class CoolRecipe 
	{
		public Object in0;
		public Object out0;
		public Object in1;
		public Object out1;
		public float energy;
		
		public CoolRecipe(Object in0, Object out0, Object in1, Object out1, float energy)
		{
			this.in0 = in0;
			this.out0 = out0;
			this.in1 = in1;
			this.out1 = out1;
			this.energy = energy;
		}
		
		public boolean matches(ItemStack[] inv, int s, FluidStack in0, FluidStack in1)
		{
			if (this.in0 instanceof FluidStack && in0 != null) {
				if (!in0.containsFluid((FluidStack)this.in0)) return false;
			} else if (this.in0 instanceof ItemStack && in0 == null && inv[s] != null) {
				if (!inv[s].isItemEqual((ItemStack)this.in0) || inv[s].stackSize < ((ItemStack)this.in0).stackSize) return false;
			} else return false;
			if (this.in1 instanceof FluidStack && in1 != null) {
				if (!in1.containsFluid((FluidStack)this.in1)) return false;
			} else if (this.in1 instanceof ItemStack && in1 == null && inv[s + 1] != null) {
				if (!inv[s + 1].isItemEqual((ItemStack)this.in1) || inv[s + 1].stackSize < ((ItemStack)this.in1).stackSize) return false;
			} else return false;
			return true;
		}
		
		public void useRes(Inventory inv, int s0, int s1, TankContainer tanks, int t0, int t1)
		{
			if (this.in0 instanceof FluidStack) tanks.drain(t0, ((FluidStack)this.in0).amount, true);
			else if (this.in0 instanceof ItemStack){
				if (inv.items[s0].stackSize > ((ItemStack)this.in0).stackSize) inv.items[s0].shrink(((ItemStack)this.in0).stackSize);
				else inv.items[s0] = null;
			}
			if (this.in1 instanceof FluidStack) tanks.drain(t1, ((FluidStack)this.in1).amount, true);
			else if (this.in1 instanceof ItemStack){
				if (inv.items[s1].stackSize > ((ItemStack)this.in1).stackSize) inv.items[s1].shrink(((ItemStack)this.in1).stackSize);
				else inv.items[s1] = null;
			}
			this.in0 = null;
			this.in1 = null;
		}
		
		public boolean output(Inventory inv, int s0, int s1, TankContainer tanks, int t0, int t1)
		{
			if (this.out0 instanceof FluidStack) {
				((FluidStack)this.out0).amount -= tanks.fill(t0, (FluidStack)this.out0, true);
				if (((FluidStack)this.out0).amount <= 0) this.out0 = null;
			} else if (this.out0 instanceof ItemStack) {
				if (inv.items[s0] == null){
					inv.items[s0] = (ItemStack)this.out0;
					this.out0 = null;
				} else if (inv.items[s0].isItemEqual((ItemStack)this.out0)) {
					int n = inv.items[s0].getMaxStackSize() - inv.items[s0].stackSize;
					if (n > ((ItemStack)this.out0).stackSize) {
						inv.items[s0].grow(((ItemStack)this.out0).stackSize);
						this.out0 = null;
					} else {
						inv.items[s0].grow(n);
						((ItemStack)this.out0).shrink(n);
					}
				}
			}
			if (this.out1 instanceof FluidStack) {
				((FluidStack)this.out1).amount -= tanks.fill(t1, (FluidStack)this.out1, true);
				if (((FluidStack)this.out1).amount <= 0) this.out1 = null;
			} else if (this.out1 instanceof ItemStack) {
				if (inv.items[s1] == null){
					inv.items[s1] = (ItemStack)this.out1;
					this.out1 = null;
				} else if (inv.items[s1].isItemEqual((ItemStack)this.out1)) {
					int n = inv.items[s1].getMaxStackSize() - inv.items[s1].stackSize;
					if (n > ((ItemStack)this.out1).stackSize) {
						inv.items[s1].grow(((ItemStack)this.out1).stackSize);
						this.out1 = null;
					} else {
						inv.items[s1].grow(n);
						((ItemStack)this.out1).shrink(n);
					}
				}
			}
			return this.out0 == null && this.out1 == null;
		}
		
		public CoolRecipe copy()
		{
			Object in0 = this.in0 instanceof ItemStack ? ((ItemStack)this.in0).copy() : this.in0 instanceof FluidStack ? ((FluidStack)this.in0).copy() : null;
			Object in1 = this.in1 instanceof ItemStack ? ((ItemStack)this.in1).copy() : this.in1 instanceof FluidStack ? ((FluidStack)this.in1).copy() : null;
			Object out0 = this.out0 instanceof ItemStack ? ((ItemStack)this.out0).copy() : this.out0 instanceof FluidStack ? ((FluidStack)this.out0).copy() : null;
			Object out1 = this.out1 instanceof ItemStack ? ((ItemStack)this.out1).copy() : this.out1 instanceof FluidStack ? ((FluidStack)this.out1).copy() : null;
			return new CoolRecipe(in0, out0, in1, out1, energy);
		}
		
	}
	
	public static CoolRecipe readCoolRecipeFromNBT(NBTTagCompound nbt)
	{
		float energy = nbt.getFloat("Energy");
		Object out0 = null;
		Object out1 = null;
		if (nbt.hasKey("out0")) {
			NBTTagCompound tag = nbt.getCompoundTag("out0");
			if (tag.hasKey("FluidName")) out0 = FluidStack.loadFluidStackFromNBT(tag);
			else if (tag.hasKey("id")) out0 = new ItemStack(tag);
		}
		if (nbt.hasKey("out1")) {
			NBTTagCompound tag = nbt.getCompoundTag("out1");
			if (tag.hasKey("FluidName")) out1 = FluidStack.loadFluidStackFromNBT(tag);
			else if (tag.hasKey("id")) out1 = new ItemStack(tag);
		}
		return new CoolRecipe(null, out0, null, out1, energy);
	}
	
	public static NBTTagCompound writeCoolRecipeToNBT(CoolRecipe rcp)
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setFloat("Energy", rcp.energy);
		if (rcp.out0 != null) {
			NBTTagCompound tag = new NBTTagCompound();
			if (rcp.out0 instanceof FluidStack) ((FluidStack)rcp.out0).writeToNBT(tag);
			else if (rcp.out0 instanceof ItemStack) ((ItemStack)rcp.out0).writeToNBT(tag);
			nbt.setTag("out0", tag);
		}
		if (rcp.out1 != null) {
			NBTTagCompound tag = new NBTTagCompound();
			if (rcp.out1 instanceof FluidStack) ((FluidStack)rcp.out1).writeToNBT(tag);
			else if (rcp.out1 instanceof ItemStack) ((ItemStack)rcp.out1).writeToNBT(tag);
			nbt.setTag("out1", tag);
		}
		return nbt;
	}
	
	private static List<CoolRecipe> coolRecipes = new ArrayList<CoolRecipe>();
	
	public static List<CoolRecipe> getCoolerRecipes()
	{
		return coolRecipes;
	}
	
	public static void addRecipe(CoolRecipe recipe)
	{
		coolRecipes.add(recipe);
	}
	
	public static CoolRecipe getRecipeFor(ItemStack[] inv, int s, FluidStack in0, FluidStack in1) 
	{
		Iterator<CoolRecipe> iterator = coolRecipes.iterator();
		while(iterator.hasNext())
		{
			CoolRecipe recipe = iterator.next();
			if (recipe.matches(inv, s, in0, in1)) return recipe.copy();
		}
		return null;
	}
	
	public static class ElRecipe
	{
		public Object in;
		public Object out0;
		public Object out1;
		public float energy;
		
		public ElRecipe(Object in, Object out0, Object out1, float e)
		{
			this.in = in;
			this.out0 = out0;
			this.out1 = out1;
			this.energy = e;
		}
		
		public boolean matches(ItemStack[] inv, int s, FluidStack in)
		{
			if (this.in instanceof FluidStack && in != null) {
				if (!in.containsFluid((FluidStack)this.in)) return false;
			} else if (this.in instanceof ItemStack && in == null && inv[s] != null) {
				if (!inv[s].isItemEqual((ItemStack)this.in) || inv[s].stackSize < ((ItemStack)this.in).stackSize) return false;
			} else return false;
			return true;
		}
		
		public void useRes(Inventory inv, int s0, TankContainer tanks, int t0)
		{
			if (this.in instanceof FluidStack) tanks.drain(t0, ((FluidStack)this.in).amount, true);
			else if (this.in instanceof ItemStack){
				if (inv.items[s0].stackSize > ((ItemStack)this.in).stackSize) inv.items[s0].shrink(((ItemStack)this.in).stackSize);
				else inv.items[s0] = null;
			}
			this.in = null;
		}
		
		public boolean output(Inventory inv, int s0, int s1, TankContainer tanks, int t0, int t1)
		{
			if (this.out0 instanceof FluidStack) {
				((FluidStack)this.out0).amount -= tanks.fill(t0, (FluidStack)this.out0, true);
				if (((FluidStack)this.out0).amount <= 0) this.out0 = null;
			} else if (this.out0 instanceof ItemStack) {
				if (inv.items[s0] == null){
					inv.items[s0] = (ItemStack)this.out0;
					this.out0 = null;
				} else if (inv.items[s0].isItemEqual((ItemStack)this.out0)) {
					int n = inv.items[s0].getMaxStackSize() - inv.items[s0].stackSize;
					if (n > ((ItemStack)this.out0).stackSize) {
						inv.items[s0].grow(((ItemStack)this.out0).stackSize);
						this.out0 = null;
					} else {
						inv.items[s0].grow(n);
						((ItemStack)this.out0).shrink(n);
					}
				}
			}
			if (this.out1 instanceof FluidStack) {
				((FluidStack)this.out1).amount -= tanks.fill(t1, (FluidStack)this.out1, true);
				if (((FluidStack)this.out1).amount <= 0) this.out1 = null;
			} else if (this.out1 instanceof ItemStack) {
				if (inv.items[s1] == null){
					inv.items[s1] = (ItemStack)this.out1;
					this.out1 = null;
				} else if (inv.items[s1].isItemEqual((ItemStack)this.out1)) {
					int n = inv.items[s1].getMaxStackSize() - inv.items[s1].stackSize;
					if (n > ((ItemStack)this.out1).stackSize) {
						inv.items[s1].grow(((ItemStack)this.out1).stackSize);
						this.out1 = null;
					} else {
						inv.items[s1].grow(n);
						((ItemStack)this.out1).shrink(n);
					}
				}
			}
			return this.out0 == null && this.out1 == null;
		}
		
		public ElRecipe copy()
		{
			Object in = this.in instanceof ItemStack ? ((ItemStack)this.in).copy() : this.in instanceof FluidStack ? ((FluidStack)this.in).copy() : null;
			Object out0 = this.out0 instanceof ItemStack ? ((ItemStack)this.out0).copy() : this.out0 instanceof FluidStack ? ((FluidStack)this.out0).copy() : null;
			Object out1 = this.out1 instanceof ItemStack ? ((ItemStack)this.out1).copy() : this.out1 instanceof FluidStack ? ((FluidStack)this.out1).copy() : null;
			return new ElRecipe(in, out0, out1, energy);
		}
		
	}
	
	public static ElRecipe readElRecipeFromNBT(NBTTagCompound nbt)
	{
		float energy = nbt.getFloat("Energy");
		Object out0 = null;
		Object out1 = null;
		if (nbt.hasKey("out0")) {
			NBTTagCompound tag = nbt.getCompoundTag("out0");
			if (tag.hasKey("FluidName")) out0 = FluidStack.loadFluidStackFromNBT(tag);
			else if (tag.hasKey("id")) out0 = new ItemStack(tag);
		}
		if (nbt.hasKey("out1")) {
			NBTTagCompound tag = nbt.getCompoundTag("out1");
			if (tag.hasKey("FluidName")) out1 = FluidStack.loadFluidStackFromNBT(tag);
			else if (tag.hasKey("id")) out1 = new ItemStack(tag);
		}
		return new ElRecipe(null, out0, out1, energy);
	}
	
	public static NBTTagCompound writeElRecipeToNBT(ElRecipe rcp)
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setFloat("Energy", rcp.energy);
		if (rcp.out0 != null) {
			NBTTagCompound tag = new NBTTagCompound();
			if (rcp.out0 instanceof FluidStack) ((FluidStack)rcp.out0).writeToNBT(tag);
			else if (rcp.out0 instanceof ItemStack) ((ItemStack)rcp.out0).writeToNBT(tag);
			nbt.setTag("out0", tag);
		}
		if (rcp.out1 != null) {
			NBTTagCompound tag = new NBTTagCompound();
			if (rcp.out1 instanceof FluidStack) ((FluidStack)rcp.out1).writeToNBT(tag);
			else if (rcp.out1 instanceof ItemStack) ((ItemStack)rcp.out1).writeToNBT(tag);
			nbt.setTag("out1", tag);
		}
		return nbt;
	}
	
	private static List<ElRecipe> elRecipes = new ArrayList<ElRecipe>();
	
	public static List<ElRecipe> getElectrolyserRecipes()
	{
		return elRecipes;
	}
	
	public static void addRecipe(ElRecipe recipe)
	{
		elRecipes.add(recipe);
	}
	
	public static ElRecipe getRecipeFor(ItemStack[] inv, int s, FluidStack in) 
	{
		Iterator<ElRecipe> iterator = elRecipes.iterator();
		while(iterator.hasNext())
		{
			ElRecipe recipe = iterator.next();
			if (recipe.matches(inv, s, in)) return recipe.copy();
		}
		return null;
	}
	
	public static class GCRecipe 
	{
		public final ItemStack input;
		public final ItemStack output;
		public final int matter;
		
		public GCRecipe(ItemStack out, ItemStack in, int m) 
		{
			this.input = in;
			this.output = out;
			this.matter = m;
		}
		
		public boolean matches(ItemStack in)
		{
			return Utils.oresEqual(in, input) && in.stackSize >= input.stackSize;
		}
	}
	
	private static List<GCRecipe> gcRecipes = new ArrayList<GCRecipe>();
	
	public static List<GCRecipe> getGraviCondRecipes()
	{
		return gcRecipes;
	}
	
	public static void addRecipe(GCRecipe recipe)
	{
		gcRecipes.add(recipe);
	}
	
	public static GCRecipe getRecipeFor(ItemStack input) 
	{
		Iterator<GCRecipe> iterator = gcRecipes.iterator();
		while(iterator.hasNext())
		{
			GCRecipe recipe = iterator.next();
			if (recipe.matches(input)) return recipe;
		}
		return null;
	}
	
	public static void addItemCrushingRecipes(String oreType)
	{
		String[] names = OreDictionary.getOreNames();
		HashMap<String, String> dusts = new HashMap<String, String>();
		ArrayList<String> items = new ArrayList<String>();
		for (String name : names) {
			if (name.startsWith("dust")) {
				dusts.put(oreType + name.substring(4), name);
			} else if (name.startsWith(oreType)) {
				items.add(name);
			}
		}
		List<ItemStack> list;
		for (String item : items) {
			String dust = dusts.get(item);
			if (dust != null) {
				list = OreDictionary.getOres(dust);
				if (!list.isEmpty()) AutomationRecipes.addCmpRecipe(list.get(0), item);
			}
		}
	}

	public static HashMap<Fluid, Obj2<Integer, FluidStack>> radiatorRecipes = new HashMap<Fluid, Obj2<Integer, FluidStack>>();
	
	public static void addRadiatorRecipe(FluidStack in, FluidStack out) {
		radiatorRecipes.put(in.getFluid(), new Obj2<Integer, FluidStack>(in.amount, out));
	}

	public static List<BioEntry> bioList = new ArrayList<BioEntry>();
	public static float Lnutrients_healAmount = 20;

	public static List<BioEntry> getBioFuels() {
		return bioList;
	}

	public static int[] getLnutrients(ItemStack item) {
		if (item == null) return null;
		if (item.getItem() instanceof ItemFood) {
			ItemFood food = (ItemFood)item.getItem();
			return new int[]{(int)Math.ceil((food.getSaturationModifier(item) + food.getHealAmount(item)) * Lnutrients_healAmount), 0};
		} else {
			for (BioEntry entry : bioList)
				if (entry.matches(item)) return new int[]{entry.nutrients, entry.algae};
			return null;
		}
	}

	public static class BioEntry {
		public final Object item;
		public final int nutrients;
		public final int algae;

		public BioEntry(Object item, int n) {
			this(item, n, 0);
		}

		public BioEntry(Object item, int n, int a) {
			this.item = item;
			this.nutrients = n;
			this.algae = a;
		}

		@SuppressWarnings("rawtypes")
		public boolean matches(ItemStack item) {
			if (this.item instanceof ItemStack) return ((ItemStack)this.item).isItemEqual(item);
			else if (this.item instanceof OreDictStack){
				return ((OreDictStack)this.item).isEqual(item);
			} else if (this.item instanceof Class) {
				if (item.getItem() instanceof ItemBlock) return ((Class)this.item).isInstance(((ItemBlock)item.getItem()).block);
				else return ((Class)this.item).isInstance(item.getItem());
			} else return false;
		}
	}

}
