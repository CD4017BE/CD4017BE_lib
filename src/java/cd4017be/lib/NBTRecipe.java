package cd4017be.lib;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * @author CD4017BE
 */
public class NBTRecipe extends ShapedOreRecipe {

	private final String[] nbtVars;
	private final byte[] addTypes;

	/**
	 * Like a normal ShapedOreRecipe.
	 * plus: The specified NBT-Tags will be applied to the recipe result. <>
	 * Format for nbtTypes: "#tagname, +tagname, ..." <> apply types: {# = override value, + = add values, < = min value, > = max value, = = clear all nbt unless tags are equal on all ingreds}
	 * @param out Recipe output
	 * @param nbtTypes NBT-Tags
	 * @param recipe 
	 */
	public NBTRecipe(ItemStack out, String nbtTypes, Object... recipe) {
		super(out, recipe);
		this.nbtVars = nbtTypes.split(",");
		this.addTypes = new byte[this.nbtVars.length];
		for (int i = 0; i < nbtVars.length; i++) {
			String s = nbtVars[i].trim();
			if (s.startsWith("#")) addTypes[i] = 0; //override
			else if (s.startsWith("+")) addTypes[i] = 1; //add
			else if (s.startsWith(">")) addTypes[i] = 3; //max value
			else if (s.startsWith("<")) addTypes[i] = 4; //min value
			else if (s.startsWith("=")) addTypes[i] = 5; //only use when equal
			else {
				addTypes[i] = 0; //override
				nbtVars[i] = s;
				continue;
			}
			nbtVars[i] = s.substring(1);
		}
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		ItemStack out = this.getRecipeOutput().copy();
		if (!out.hasTagCompound()) out.setTagCompound(new NBTTagCompound());
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if (stack.hasTagCompound())
				for (int j = 0; j < nbtVars.length; j++)
					if (stack.getTagCompound().hasKey(nbtVars[j]) && !applyTag(out.getTagCompound(), stack.getTagCompound().getTag(nbtVars[j]), j, out.getCount())) {
						out.setTagCompound(null);
						return out;
					}
		}
		return out;
	}

	private boolean applyTag(NBTTagCompound nbt, NBTBase tag, int idx, int stacksize) {
		String var = nbtVars[idx];
		byte type = addTypes[idx];
		if (type == 0){
			if (!nbt.hasKey(var)) nbt.setTag(var, tag);
		} else if (type == 5) {
			if (!nbt.hasKey(var)) nbt.setTag(var, tag);
			else return nbt.getTag(var).equals(tag);
		} else if (tag instanceof NBTTagByte) {
			nbt.setByte(var, (byte)this.applyValue(nbt.getByte(var), ((NBTTagByte)tag).getByte(), type, stacksize));
		} else if (tag instanceof NBTTagShort) {
			nbt.setShort(var, (short)this.applyValue(nbt.getShort(var), ((NBTTagShort)tag).getShort(), type, stacksize));
		} else if (tag instanceof NBTTagInt) {
			nbt.setInteger(var, (int)this.applyValue(nbt.getInteger(var), ((NBTTagInt)tag).getInt(), type, stacksize));
		} else if (tag instanceof NBTTagLong) {
			nbt.setLong(var, (long)this.applyValue(nbt.getLong(var), ((NBTTagLong)tag).getLong(), type, stacksize));
		} else if (tag instanceof NBTTagFloat) {
			nbt.setFloat(var, (float)this.applyValue(nbt.getFloat(var), ((NBTTagFloat)tag).getFloat(), type, stacksize));
		} else if (tag instanceof NBTTagDouble) {
			nbt.setDouble(var, this.applyValue(nbt.getDouble(var), ((NBTTagDouble)tag).getDouble(), type, stacksize));
		} else nbt.setTag(var, tag);
		return true;
	}

	private double applyValue(double old, double v, byte type, int stacksize) {
		switch (type) {
			case 1: return old + v / (double)stacksize;
			case 2: return Math.max(old, v);
			case 3: return Math.min(old, v);
			default: return v;
		}
	}

}
