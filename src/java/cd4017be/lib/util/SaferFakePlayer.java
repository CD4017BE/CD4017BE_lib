package cd4017be.lib.util;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;


/**
 * This should fix problems with unsupported methods (involving network communication attempts) that were overseen by Forge's FakePlayer implementation.
 * @author CD4017BE
 */
public class SaferFakePlayer extends FakePlayer {

	/**
	 * @param world
	 * @param name
	 */
	public SaferFakePlayer(WorldServer world, GameProfile name) {
		super(world, name);
	}

	@Override public void displayGui(IInteractionObject guiOwner) {}
	@Override public void displayGUIChest(IInventory chestInventory) {}
	@Override public void displayVillagerTradeGui(IMerchant villager) {}
	@Override public void displayGuiCommandBlock(TileEntityCommandBlock commandBlock) {}
	@Override public boolean isPotionApplicable(PotionEffect potioneffectIn) {return false;}
	@Override public void openEditSign(TileEntitySign signTile) {}
	@Override public void openGuiHorseInventory(AbstractHorse horse, IInventory inventoryIn) {}
	@Override public void openBook(ItemStack stack, EnumHand hand) {}
	@Override public void setPositionAndUpdate(double x, double y, double z) {}
	@Override public SleepResult trySleep(BlockPos bedLocation) {return SleepResult.OTHER_PROBLEM;}
	@Override public boolean startRiding(Entity entityIn, boolean force) {return false;}
	@Override public void updateCraftingInventory(Container containerToSend, NonNullList<ItemStack> itemsList) {}

}
