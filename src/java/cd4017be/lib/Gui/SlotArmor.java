package cd4017be.lib.Gui;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class SlotArmor extends HidableSlot {

	private final EntityEquipmentSlot type;
	private final InventoryPlayer inv;

	public SlotArmor(
		InventoryPlayer inv, int index, int x, int y, EntityEquipmentSlot type
	) {
		super(inv, index, x, y);
		this.type = type;
		this.inv = inv;
	}

	@Override
	public int getSlotStackLimit() {
		return 1;
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return stack.getItem().isValidArmor(stack, type, inv.player);
	}

	@Override
	public boolean canTakeStack(EntityPlayer player) {
		ItemStack stack = this.getStack();
		return (
			stack.isEmpty() || player.isCreative()
			|| !EnchantmentHelper.hasBindingCurse(stack)
		) && super.canTakeStack(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getSlotTexture() {
		return ItemArmor.EMPTY_SLOT_NAMES[type.getIndex()];
	}
}
