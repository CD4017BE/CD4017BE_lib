package cd4017be.lib.Gui;

import net.minecraft.inventory.IInventory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class SlotOffhand extends HidableSlot {

	public SlotOffhand(IInventory inv, int index, int x, int y) {
		super(inv, index, x, y);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getSlotTexture() {
		return "minecraft:items/empty_armor_slot_shield";
	}

}
