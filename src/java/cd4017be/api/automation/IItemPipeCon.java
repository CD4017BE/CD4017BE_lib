package cd4017be.api.automation;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public interface IItemPipeCon {

	public byte getItemConnectType(int s);
	public ItemStack insert(ItemStack item, EnumFacing side);

}
