package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

import cd4017be.lib.block.MultipartBlock;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.render.IHardCodedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;

/**
 * 
 * @author CD4017BE
 */
public class MultipartModel implements IModel, IHardCodedModel {

	public final IModelProvider[] modelProvider;
	public final ResourceLocation[] baseModels;
	public final MultipartBlock block;
	public ItemOverrideList itemHandler = ItemOverrideList.NONE;

	public MultipartModel(MultipartBlock block) {
		this.block = block;
		if (block.baseState == null) {
			baseModels = new ResourceLocation[] {new ModelResourceLocation(block.getRegistryName(), "base")};
		} else {
			Collection<Integer> states = block.baseState.getAllowedValues();
			this.baseModels = new ResourceLocation[states.size()];
			for (int s : states) baseModels[s] = new ModelResourceLocation(block.getRegistryName(), "base" + s);
		}
		this.modelProvider = new IModelProvider[block.numModules];
		for (int i = 0; i < block.numModules; i++) {
			Class<?> type = block.moduleType(i);
			if (type == Boolean.class)
				modelProvider[i] = new ProviderBool(new ModelResourceLocation(block.getRegistryName(), block.moduleVariant(i)));
			else if (type == IBlockState.class)
				modelProvider[i] = BlockMimicModel.provider;
		}
	}

	public MultipartModel setPipeVariants(int n) {
		for (int i = 0; i < block.numModules; i++) {
			Class<?> type = block.moduleType(i);
			if (type == Byte.class && modelProvider[i] == null) {
				ResourceLocation[] locs = new ResourceLocation[n];
				for (int j = 0; j < n; j++)
					locs[j] = new ModelResourceLocation(block.getRegistryName(), block.moduleVariant(i) + j);
				modelProvider[i] = new ProviderByte(locs);
			}
		}
		return this;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>();
		for (ResourceLocation res : baseModels) list.add(res);
		for (IModelProvider provider : modelProvider) {
			Collection<ResourceLocation> c = provider.getDependencies();
			if (c != null) list.addAll(c);
		}
		return list;
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
		IBakedModel[] models = new IBakedModel[baseModels.length];
		for (int i = 0; i < baseModels.length; i++) {
			IModel model = ModelLoaderRegistry.getModelOrLogError(baseModels[i], "missing base Model " + i);
			models[i] = model.bake(state, format, textureGetter);
		}
		for (IModelProvider provider : modelProvider)
			provider.bake(state, format, textureGetter);
		return new BakedMultipart(models);
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}

	@Override
	public void onReload() {}

	public class BakedMultipart implements IBakedModel {

		public final IBakedModel[] base;

		private BakedMultipart(IBakedModel[] base) {
			this.base = base;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			ArrayList<BakedQuad> list = new ArrayList<BakedQuad>();
			BlockRenderLayer layer = block.renderMultilayer ? MinecraftForgeClient.getRenderLayer() : null;
			boolean render = true;
			if (state instanceof IExtendedBlockState) {
				IExtendedBlockState exState = (IExtendedBlockState) state;
				IModularTile tile = exState.getValue(MultipartBlock.moduleRef);
				if (render = tile != null && !(side == null && tile.isOpaque()))
					for (int i = 0; i < block.numModules; i++) {
						Object val = tile.getModuleState(i);
						IBakedModel model = layer == null ? modelProvider[i].getModelFor(val) : modelProvider[i].getModelFor(val, layer);
						if (model != null) list.addAll(model.getQuads(state, side, rand));
					}
			}
			if (render && (layer == null || layer == BlockRenderLayer.CUTOUT)) list.addAll(base[block.baseState == null ? 0 : state.getValue(block.baseState)].getQuads(state, side, rand));
			return list;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return base[0].isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return base[0].isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return base[0].getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return null;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return itemHandler;
		}

		public MultipartBlock getOwner() {
			return block;
		}

	}

	public interface IModelProvider {
		public IBakedModel getModelFor(Object val);
		public default IBakedModel getModelFor(Object val, @Nonnull BlockRenderLayer layer) {
			return layer == BlockRenderLayer.CUTOUT ? getModelFor(val) : null;
		}
		public Collection<ResourceLocation> getDependencies();
		public void bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter);
	}

	public class ProviderBool implements IModelProvider {

		private final ResourceLocation model;
		private IBakedModel baked;

		public ProviderBool(ResourceLocation model) {
			this.model = model;
		}
		
		@Override
		public IBakedModel getModelFor(Object val) {
			return val == Boolean.TRUE ? baked : null;
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Arrays.asList(model);
		}

		@Override
		public void bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
			IModel model = ModelLoaderRegistry.getModelOrLogError(this.model, "missing");
			baked = model.bake(model.getDefaultState(), format, textureGetter);
		}

	}

	public class ProviderByte implements IModelProvider {

		private final ResourceLocation[] models;
		private final IBakedModel[] baked;

		public ProviderByte(ResourceLocation... models) {
			this.models = models;
			this.baked = new IBakedModel[models.length];
		}
		
		@Override
		public IBakedModel getModelFor(Object val) {
			if (!(val instanceof Byte)) return null;
			byte var = (Byte)val;
			return var >= 0 && var < models.length ? baked[var] : null;
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Arrays.asList(models);
		}

		@Override
		public void bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
			for (int i = 0; i < models.length; i++) {
				IModel model = ModelLoaderRegistry.getModelOrLogError(models[i], "missing");
				baked[i] = model.bake(model.getDefaultState(), format, textureGetter);
			}
		}

	}

}
