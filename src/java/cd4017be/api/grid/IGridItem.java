package cd4017be.api.grid;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**Implemented by {@link Item}s that create or interact with {@link GridPart}s.
 * @author CD4017BE */
public interface IGridItem {

	/**@param grid
	 * @param stack
	 * @param player
	 * @param hand used hand or null if left click
	 * @param hit original ray trace hit
	 * @return action result */
	InteractionResult onInteract(
		IGridHost grid, ItemStack stack, Player player,
		InteractionHand hand, BlockHitResult hit
	);

	default InteractionResult placeAndInteract(UseOnContext itemContext) {
		BlockPlaceContext context = new BlockPlaceContext(itemContext);
		BlockPos pos = new BlockPos(context.getClickLocation().add(
			Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.125)
		));
		Level world = context.getLevel();
		BlockEntity te = world.getBlockEntity(pos);
		if (!(te instanceof IGridHost)) {
			if (!context.canPlace())
				return InteractionResult.FAIL;
			if (!world.setBlock(pos, GridPart.GRID_HOST_BLOCK, 11))
				return InteractionResult.FAIL;
			te = world.getBlockEntity(pos);
			if (!(te instanceof IGridHost))
				return InteractionResult.FAIL;
		}
		return onInteract(
			(IGridHost)te, context.getItemInHand(), context.getPlayer(), context.getHand(),
			new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, false)
		);
	}

	/**@return a new GridPart for deserialization.
	 * Items that only want to interact with grids
	 * but not represent parts may return null. */
	@Nullable GridPart createPart();

}
