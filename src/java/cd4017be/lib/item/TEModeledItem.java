package cd4017be.lib.item;

import cd4017be.lib.block.BlockTE;
import cd4017be.lib.render.model.IModelDataItem;
import cd4017be.lib.render.model.ModelDataItemOverride;
import cd4017be.lib.render.model.TileEntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**Item that provides data from a stored BlockEntity to {@link TileEntityModel} for rendering.
 * @author CD4017BE */
public class TEModeledItem extends DocumentedBlockItem implements IModelDataItem {

	public TEModeledItem(BlockTE<?> id, Properties p) {
		super(id, p);
	}

	/**@param <T> return cast type
	 * @param stack
	 * @return new BlockEntity loaded from stack nbt */
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> T tileEntity(ItemStack stack) {
		BlockTE<T> block = (BlockTE<T>)getBlock();
		T te = block.tileType.create(BlockPos.ZERO, block.defaultBlockState());
		te.load(stack.getOrCreateTagElement(BlockTE.TE_TAG));
		return te;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public IModelData getModelData(
		ItemStack stack, ClientLevel world, LivingEntity entity
	) {
		CompoundTag nbt = stack.getTagElement(BlockTE.TE_TAG);
		if (nbt == null) return EmptyModelData.INSTANCE;
		return ModelDataItemOverride.getCached(
			nbt.getInt("hash"), ()-> tileEntity(stack).getModelData()
		);
	}

}
