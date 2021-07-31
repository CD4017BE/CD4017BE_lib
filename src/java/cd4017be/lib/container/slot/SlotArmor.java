package cd4017be.lib.container.slot;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/** @author CD4017BE */
public class SlotArmor extends HidableSlot {

	private final EquipmentSlot type;
	private final Inventory inv;

	public SlotArmor(Inventory inv, int index, int x, int y, EquipmentSlot type) {
		super(inv, index, x, y);
		this.type = type;
		this.inv = inv;
		ResourceLocation loc;
		switch(type) {
		case CHEST: loc = InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE; break;
		case FEET: loc = InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS; break;
		case HEAD: loc = InventoryMenu.EMPTY_ARMOR_SLOT_HELMET; break;
		case LEGS: loc = InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS; break;
		case OFFHAND: loc = InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD; break;
		default: return;
		}
		setBackground(InventoryMenu.BLOCK_ATLAS, loc);
	}

	@Override
	public int getMaxStackSize() {
		return type == EquipmentSlot.OFFHAND ? 64 : 1;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return super.mayPlace(stack) && (
			type == EquipmentSlot.OFFHAND
			|| stack.getItem().canEquip(stack, type, inv.player)
		);
	}

	@Override
	public boolean mayPickup(Player player) {
		ItemStack stack = this.getItem();
		return super.mayPickup(player) && (
			type == EquipmentSlot.OFFHAND
			|| stack.isEmpty() || player.isCreative()
			|| !EnchantmentHelper.hasBindingCurse(stack)
		);
	}

}
