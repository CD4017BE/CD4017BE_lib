package cd4017be.lib.render.model;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**Allows feeding custom {@link IModelData} to a model for item rendering.
 * @author CD4017BE */
public class ModelDataItemOverride extends ItemOverrideList {

	public static final ModelDataItemOverride INSTANCE = new ModelDataItemOverride();

	@Override
	public IBakedModel getOverrideModel(
		IBakedModel model, ItemStack stack, ClientWorld world, LivingEntity livingEntity
	) {
		Item item = stack.getItem();
		if (!(item instanceof IModelDataItem)) return model;
		IModelData data = ((IModelDataItem)item).getModelData(stack, world, livingEntity);
		if (data == EmptyModelData.INSTANCE) return model;
		return new BakedModelDataWrapper(model, data);
	}


	public static class BakedModelDataWrapper implements IBakedModel {

		protected final IBakedModel parent;
		protected final IModelData data;

		public BakedModelDataWrapper(IBakedModel parent, IModelData data) {
			this.parent = parent;
			this.data = data;
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
			return parent.getQuads(state, side, rand, data);
		}

		@Override
		public boolean isAmbientOcclusion() {
			return parent.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return parent.isGui3d();
		}

		@Override
		public boolean isSideLit() {
			return parent.isSideLit();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return parent.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return parent.getParticleTexture(data);
		}

		@Override
		public ItemOverrideList getOverrides() {
			return parent.getOverrides();
		}

		@Override
		@SuppressWarnings("deprecation")
		public ItemCameraTransforms getItemCameraTransforms() {
			return parent.getItemCameraTransforms();
		}

	}

}
