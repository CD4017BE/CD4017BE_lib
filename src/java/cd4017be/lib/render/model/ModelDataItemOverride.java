package cd4017be.lib.render.model;

import static cd4017be.lib.Lib.CFG_CLIENT;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**Allows feeding custom {@link IModelData} to a model for item rendering.
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class ModelDataItemOverride extends ItemOverrides {

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
	public BakedModel resolve(
		BakedModel model, ItemStack stack, ClientLevel world, LivingEntity livingEntity, int i
	) {
		Item item = stack.getItem();
		if (!(item instanceof IModelDataItem)) return model;
		IModelData data = ((IModelDataItem)item).getModelData(stack, world, livingEntity);
		if (data == EmptyModelData.INSTANCE) return model;
		return new BakedModelDataWrapper(model, data);
	}


	public static class BakedModelDataWrapper implements BakedModel {

		protected final BakedModel parent;
		protected final IModelData data;

		public BakedModelDataWrapper(BakedModel parent, IModelData data) {
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
			return parent.getParticleIcon(data);
		}

		@Override
		public ItemOverrides getOverrides() {
			return parent.getOverrides();
		}

		@Override
		@SuppressWarnings("deprecation")
		public ItemTransforms getTransforms() {
			return parent.getTransforms();
		}

	}

}
