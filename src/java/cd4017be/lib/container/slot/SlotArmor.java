package cd4017be.lib.container.slot;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** @author CD4017BE */
public class SlotArmor extends HidableSlot {

	private final EquipmentSlotType type;
	private final PlayerInventory inv;

	public SlotArmor(PlayerInventory inv, int index, int x, int y, EquipmentSlotType type) {
		super(inv, index, x, y);
		this.type = type;
		this.inv = inv;
		ResourceLocation loc;
		switch(type) {
		case CHEST: loc = PlayerContainer.EMPTY_ARMOR_SLOT_CHESTPLATE; break;
		case FEET: loc = PlayerContainer.EMPTY_ARMOR_SLOT_BOOTS; break;
		case HEAD: loc = PlayerContainer.EMPTY_ARMOR_SLOT_HELMET; break;
		case LEGS: loc = PlayerContainer.EMPTY_ARMOR_SLOT_LEGGINGS; break;
		case OFFHAND: loc = PlayerContainer.EMPTY_ARMOR_SLOT_SHIELD; break;
		default: return;
		}
		setBackground(PlayerContainer.BLOCK_ATLAS, loc);
	}

	@Override
	public int getMaxStackSize() {
		return type == EquipmentSlotType.OFFHAND ? 64 : 1;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return super.mayPlace(stack) && (
			type == EquipmentSlotType.OFFHAND
			|| stack.getItem().canEquip(stack, type, inv.player)
		);
	}

	@Override
	public boolean mayPickup(PlayerEntity player) {
		ItemStack stack = this.getItem();
		return super.mayPickup(player) && (
			type == EquipmentSlotType.OFFHAND
			|| stack.isEmpty() || player.isCreative()
			|| !EnchantmentHelper.hasBindingCurse(stack)
		);
	}

}
