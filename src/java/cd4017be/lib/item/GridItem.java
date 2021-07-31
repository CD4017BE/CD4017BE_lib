package cd4017be.lib.item;

import cd4017be.api.grid.IGridHost;
import cd4017be.api.grid.IGridItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**@author CD4017BE */
public abstract class GridItem extends DocumentedItem implements IGridItem {

	public GridItem(Properties p) {
		super(p);
	}

	public InteractionResult useOn(UseOnContext context) {
		return placeAndInteract(context);
	}

	@Override
	public boolean
	doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
		return world.getBlockEntity(pos) instanceof IGridHost;
	}

	@Override
	public boolean canAttackBlock(
		BlockState state, Level world, BlockPos pos, Player player
	) {
		if (!world.isClientSide && player.isCreative())
			world.getBlockState(pos).attack(world, pos, player);
		return false;
	}

}
