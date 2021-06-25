package cd4017be.lib.item;

import static net.minecraft.util.ActionResult.sidedSuccess;
import static net.minecraftforge.fml.network.NetworkHooks.openGui;

import cd4017be.lib.block.BlockTE;
import cd4017be.lib.container.ContainerGrid;
import cd4017be.lib.container.IUnnamedContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * @author CD4017BE */
public class GridHostItem extends TEModeledItem implements IUnnamedContainerProvider {

	public GridHostItem(BlockTE<?> id, Properties p) {
		super(id, p);
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (hand == Hand.OFF_HAND) return super.use(world, player, hand);
		if (!world.isClientSide) {
			int slot = player.inventory.selected;
			openGui((ServerPlayerEntity)player, this, pkt -> pkt.writeByte(slot));
		}
		return sidedSuccess(player.getItemInHand(hand), world.isClientSide);
	}

	@Override
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerGrid(id, inv, inv.selected);
	}

}
