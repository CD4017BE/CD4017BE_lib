package cd4017be.lib.templates;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * @author CD4017BE
 */
public class NBTRecipe extends ShapedRecipe {

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
	public NBTRecipe(ResourceLocation name, String group, int w, int h, NonNullList<Ingredient> in, ItemStack out, String nbtTypes) {
		super(name, group, w, h, in, out);
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
	public ItemStack getCraftingResult(CraftingInventory inv) {
		ItemStack out = this.getRecipeOutput().copy();
		if (!out.hasTag()) out.setTag(new CompoundNBT());
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if (stack.hasTag())
				for (int j = 0; j < nbtVars.length; j++)
					if (stack.getTag().contains(nbtVars[j]) && !applyTag(out.getTag(), stack.getTag().get(nbtVars[j]), j, out.getCount())) {
						out.setTag(null);
						return out;
					}
		}
		return out;
	}

	private boolean applyTag(CompoundNBT nbt, INBT tag, int idx, int stacksize) {
		String var = nbtVars[idx];
		byte type = addTypes[idx];
		if (type == 0){
			if (!nbt.contains(var)) nbt.put(var, tag);
		} else if (type == 5) {
			if (!nbt.contains(var)) nbt.put(var, tag);
			else return nbt.get(var).equals(tag);
		} else if (tag instanceof ByteNBT) {
			nbt.putByte(var, (byte)this.applyValue(nbt.getByte(var), ((ByteNBT)tag).getByte(), type, stacksize));
		} else if (tag instanceof ShortNBT) {
			nbt.putShort(var, (short)this.applyValue(nbt.getShort(var), ((ShortNBT)tag).getShort(), type, stacksize));
		} else if (tag instanceof IntNBT) {
			nbt.putInt(var, (int)this.applyValue(nbt.getInt(var), ((IntNBT)tag).getInt(), type, stacksize));
		} else if (tag instanceof LongNBT) {
			nbt.putLong(var, (long)this.applyValue(nbt.getLong(var), ((LongNBT)tag).getLong(), type, stacksize));
		} else if (tag instanceof FloatNBT) {
			nbt.putFloat(var, (float)this.applyValue(nbt.getFloat(var), ((FloatNBT)tag).getFloat(), type, stacksize));
		} else if (tag instanceof DoubleNBT) {
			nbt.putDouble(var, this.applyValue(nbt.getDouble(var), ((DoubleNBT)tag).getDouble(), type, stacksize));
		} else nbt.put(var, tag);
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
