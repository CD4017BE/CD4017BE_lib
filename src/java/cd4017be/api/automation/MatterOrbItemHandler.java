package cd4017be.api.automation;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandler;

/**
 *
 * @author CD4017BE
 */
public class MatterOrbItemHandler {

	public static interface IMatterOrb {
		public int getMaxTypes(ItemStack item);
		public String getMatterTag(ItemStack item);
	}

	public static void addInformation(ItemStack item, List<String> list) {
		if (isMatterOrb(item)) {
			IMatterOrb orb = (IMatterOrb)item.getItem();
			list.add(String.format("Item storage: %d / %d types", getUsedTypes(item), orb.getMaxTypes(item)));
		}
	}

	public static int getUsedTypes(ItemStack item) {
		if (isMatterOrb(item)) {
			IMatterOrb orb = (IMatterOrb)item.getItem();
			String tag = orb.getMatterTag(item);
			createNBT(item, tag);
			return item.getTagCompound().getTagList(tag, 10).tagCount();
		} else return 0;
	}

	public static ItemStack getItem(ItemStack item, int s) {
		if (!isMatterOrb(item)) return null;
		IMatterOrb orb = (IMatterOrb)item.getItem();
		String tag = orb.getMatterTag(item);
		createNBT(item, tag);
		NBTTagList list = item.getTagCompound().getTagList(tag, 10);
		if (list.tagCount() <= s) return null;
		NBTTagCompound nbt = list.getCompoundTagAt(s);
		ItemStack stack = new ItemStack(Item.getItemById(nbt.getShort("i")), nbt.getInteger("n"), nbt.getShort("d"));
		if (nbt.hasKey("t")) stack.setTagCompound(nbt.getCompoundTag("t"));
		return stack;
	}

	public static ItemStack[] getAllItems(ItemStack item) {
		if (!isMatterOrb(item)) return new ItemStack[0];
		IMatterOrb orb = (IMatterOrb)item.getItem();
		String tag = orb.getMatterTag(item);
		createNBT(item, tag);
		NBTTagList list = item.getTagCompound().getTagList(tag, 10);
		ItemStack[] cont = new ItemStack[list.tagCount()];
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt = list.getCompoundTagAt(i);
			cont[i] = new ItemStack(Item.getItemById(nbt.getShort("i")), nbt.getInteger("n"), nbt.getShort("d"));
			if (nbt.hasKey("t")) cont[i].setTagCompound(nbt.getCompoundTag("t"));
		}
		return cont;
	}

	public static ItemStack decrStackSize(ItemStack item, int s, int n) {
		if (!isMatterOrb(item) || n <= 0) return null;
		IMatterOrb orb = (IMatterOrb)item.getItem();
		String tag = orb.getMatterTag(item);
		createNBT(item, tag);
		NBTTagList list = item.getTagCompound().getTagList(tag, 10);
		if (list.tagCount() <= s) return null;
		NBTTagCompound nbt = list.getCompoundTagAt(s);
		ItemStack stack = new ItemStack(Item.getItemById(nbt.getShort("i")), nbt.getInteger("n"), nbt.getShort("d"));
		if (nbt.hasKey("t")) stack.setTagCompound(nbt.getCompoundTag("t"));
		if (n >= stack.getCount()) {
			list.removeTag(s);
			return stack;
		} else {
			nbt.setInteger("n", stack.getCount() - n);
			stack.setCount(n);
			return stack;
		}
	}

	public static boolean canInsert(ItemStack item, ItemStack... stacks) {
		if (!isMatterOrb(item)) return false;
		else if (stacks == null || stacks.length == 0) return true;
		IMatterOrb orb = (IMatterOrb)item.getItem();
		String tag = orb.getMatterTag(item);
		createNBT(item, tag);
		NBTTagList list = item.getTagCompound().getTagList(tag, 10);
		int l = list.tagCount() + stacks.length, max = orb.getMaxTypes(item);
		for (int i = 0; i < stacks.length && l - i > max; i++) {
			boolean miss = true;
			for (int j = 0; j < l; j++) {
				NBTTagCompound nbt = list.getCompoundTagAt(j);
				if (nbt.getShort("i") == Item.getIdFromItem(stacks[i].getItem()) && nbt.getShort("d") == stacks[i].getItemDamage() && ((!nbt.hasKey("t") && stacks[i].getTagCompound() == null) || (stacks[i].getTagCompound() != null && stacks[i].getTagCompound().equals(nbt.getTag("t"))))) {
					miss = false;
					break;
				}
			}
			if (miss) return false;
		}
		return true;
	}

	public static ItemStack[] addItemStacks(ItemStack item, ItemStack... stacks) {
		if (!isMatterOrb(item)) return stacks;
		else if (stacks == null || stacks.length == 0) return new ItemStack[0];
		IMatterOrb orb = (IMatterOrb)item.getItem();
		String tag = orb.getMatterTag(item);
		createNBT(item, tag);
		NBTTagList list = item.getTagCompound().getTagList(tag, 10);
		int max = orb.getMaxTypes(item);
		int n = stacks.length + list.tagCount() - max;
		ItemStack[] remain = new ItemStack[n < 0 ? 0 : n];
		n = 0;
		for (ItemStack stack : stacks) {
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound nbt = list.getCompoundTagAt(i);
				if (nbt.getShort("i") == Item.getIdFromItem(stack.getItem()) && nbt.getShort("d") == stack.getItemDamage() && ((!nbt.hasKey("t") && stack.getTagCompound() == null) || (stack.getTagCompound() != null && stack.getTagCompound().equals(nbt.getTag("t"))))) {
					nbt.setInteger("n", nbt.getInteger("n") + stack.getCount());
					stack.setCount(0);
					break;
				}
			}
			if (stack.getCount() > 0 && list.tagCount() < max) {
				NBTTagCompound nbt = new NBTTagCompound();
				nbt.setShort("i", (short)Item.getIdFromItem(stack.getItem()));
				nbt.setInteger("n", stack.getCount());
				nbt.setShort("d", (short)stack.getItemDamage());
				if (stack.getTagCompound() != null) nbt.setTag("t", stack.getTagCompound());
				list.appendTag(nbt);
			} else if (stack.getCount() > 0) {
				remain[n++] = stack;
			}
		}
		return remain.length == n ? remain : Arrays.copyOf(remain, n);
	}

	public static boolean isMatterOrb(ItemStack item) {
		return item != null && item.getItem() instanceof IMatterOrb;
	}

	public static void createNBT(ItemStack item, String tag) {
		if (!item.hasTagCompound()) item.setTagCompound(new NBTTagCompound());
		if (!item.getTagCompound().hasKey(tag)) item.getTagCompound().setTag(tag, new NBTTagList());
	}

	public static class Access implements IItemHandler {
		public final InventoryPlayer inv;
		public int slot;
		public int tool;

		public Access(InventoryPlayer inv) {
			this.inv = inv;
			this.slot = 0;
			this.tool = inv.currentItem;
		}

		@Override
		public int getSlots() {
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int s) {
			if (s == 0) return null;
			ItemStack stack = getItem(inv.mainInventory.get(tool), slot);
			if (stack != null) stack.setCount(1);
			return stack;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean sim) {
			if (slot != 0) return stack;
			if (sim) return canInsert(inv.mainInventory.get(tool), stack) ? null : stack;
			ItemStack[] rem = addItemStacks(inv.mainInventory.get(tool), stack.copy());
			return rem.length == 0 ? null : rem[0];
		}

		@Override
		public ItemStack extractItem(int s, int amount, boolean simulate) {
			if (s == 0) return null;
			if (simulate) {
				ItemStack stack = getItem(inv.mainInventory.get(tool), slot);
				if(stack.getCount() > amount) stack.setCount(amount);
				return stack;
			} else return decrStackSize(inv.mainInventory.get(tool), slot, amount);
		}

	}

}
