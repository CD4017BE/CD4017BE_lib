package cd4017be.lib.render.model;

import static cd4017be.lib.Lib.CFG_CLIENT;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**Allows feeding custom {@link IModelData} to a model for item rendering.
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class ModelDataItemOverride extends ItemOverrideList {

	public static final ModelDataItemOverride INSTANCE = new ModelDataItemOverride();
	private static final Int2ObjectLinkedOpenHashMap<IModelData> MODEL_CACHE
	= new Int2ObjectLinkedOpenHashMap<>();

	static {
		TileEntityModel.registerCacheInvalidate(MODEL_CACHE::clear);
	}

	/**@param hash identifier code of the model to look up
	 * @param generate function generating the model if not in cache
	 * @return cached (or generated) model data */
	public static IModelData getCached(int hash, Supplier<IModelData> generate) {
		IModelData data = MODEL_CACHE.getAndMoveToFirst(hash);
		if (data == null) {
			int max = CFG_CLIENT.itemModelCache.get();
			while (MODEL_CACHE.size() >= max) MODEL_CACHE.removeLast();
			MODEL_CACHE.putAndMoveToFirst(hash, data = generate.get());
		}
		return data;
	}

	@Override
	public IBakedModel resolve(
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
		public boolean useAmbientOcclusion() {
			return parent.useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return parent.isGui3d();
		}

		@Override
		public boolean usesBlockLight() {
			return parent.usesBlockLight();
		}

		@Override
		public boolean isCustomRenderer() {
			return parent.isCustomRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return parent.getParticleTexture(data);
		}

		@Override
		public ItemOverrideList getOverrides() {
			return parent.getOverrides();
		}

		@Override
		@SuppressWarnings("deprecation")
		public ItemCameraTransforms getTransforms() {
			return parent.getTransforms();
		}

	}

}
