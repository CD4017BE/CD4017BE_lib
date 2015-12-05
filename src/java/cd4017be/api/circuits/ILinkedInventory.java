package cd4017be.api.circuits;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;

public interface ILinkedInventory extends ISidedInventory 
{
	public int[] getLinkPos();
	public IInventory getLinkInv();
	public byte getLinkDir();
}
