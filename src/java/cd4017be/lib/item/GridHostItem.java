package cd4017be.lib.item;

import static net.minecraft.world.InteractionResultHolder.sidedSuccess;
import static net.minecraftforge.fmllegacy.network.NetworkHooks.openGui;

import cd4017be.lib.block.BlockTE;
import cd4017be.lib.container.ContainerGrid;
import cd4017be.lib.container.IUnnamedContainerProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

/**
 * @author CD4017BE */
public class GridHostItem extends TEModeledItem implements IUnnamedContainerProvider {

	public GridHostItem(BlockTE<?> id, Properties p) {
		super(id, p);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (hand == InteractionHand.OFF_HAND) return super.use(world, player, hand);
		if (!world.isClientSide) {
			int slot = player.getInventory().selected;
			openGui((ServerPlayer)player, this, pkt -> pkt.writeByte(slot));
		}
		return sidedSuccess(player.getItemInHand(hand), world.isClientSide);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ContainerGrid(id, inv, inv.selected);
	}

}
