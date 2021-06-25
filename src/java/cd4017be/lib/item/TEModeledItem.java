package cd4017be.lib.item;

import cd4017be.lib.block.BlockTE;
import cd4017be.lib.render.model.IModelDataItem;
import cd4017be.lib.render.model.ModelDataItemOverride;
import cd4017be.lib.render.model.TileEntityModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**Item that provides data from a stored TileEntity to {@link TileEntityModel} for rendering.
 * @author CD4017BE */
public class TEModeledItem extends DocumentedBlockItem implements IModelDataItem {

	public TEModeledItem(BlockTE<?> id, Properties p) {
		super(id, p);
	}

	/**@param <T> return cast type
	 * @param stack
	 * @return new TileEntity loaded from stack nbt */
	@SuppressWarnings("unchecked")
	public <T extends TileEntity> T tileEntity(ItemStack stack) {
		BlockTE<T> block = (BlockTE<T>)getBlock();
		T te = block.tileType.create();
		te.load(block.defaultBlockState(), stack.getOrCreateTagElement(BlockTE.TE_TAG));
		return te;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public IModelData getModelData(
		ItemStack stack, ClientWorld world, LivingEntity entity
	) {
		CompoundNBT nbt = stack.getTagElement(BlockTE.TE_TAG);
		if (nbt == null) return EmptyModelData.INSTANCE;
		return ModelDataItemOverride.getCached(
			nbt.getInt("hash"), ()-> tileEntity(stack).getModelData()
		);
	}

}
