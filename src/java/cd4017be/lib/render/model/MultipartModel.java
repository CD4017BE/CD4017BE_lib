package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import java.util.function.Function;

import cd4017be.lib.block.IMultipartBlock;
import cd4017be.lib.block.MultipartBlock;
import cd4017be.lib.block.MultipartBlock.IModularTile;
import cd4017be.lib.render.IHardCodedModel;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
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
	public final IMultipartBlock block;
	private final ResourceLocation[] baseModels;
	public ItemOverrideList itemHandler = ItemOverrideList.NONE;

	public MultipartModel(IMultipartBlock block) {
		this.block = block;
		ResourceLocation loc = ((Block)block).getRegistryName();
		IProperty<?> base = block.getBaseState();
		
		
		if (base == null || base.getValueClass() == Boolean.class) {
			this.baseModels = new ResourceLocation[] {new ModelResourceLocation(loc, "base")};
		} else {
			Collection<?> states = base.getAllowedValues();
			this.baseModels = new ResourceLocation[states.size()];
			int i = 0;
			for (Object o : states)
				baseModels[i++] = new ModelResourceLocation(loc, "base" + o);
		}
		int n = block.moduleCount();
		this.modelProvider = new IModelProvider[n];
		for (int i = 0; i < n; i++) {
			Class<?> type = block.moduleType(i);
			if (type == Boolean.class)
				modelProvider[i] = new ProviderList(new ModelResourceLocation(loc, block.moduleVariant(i)));
			else if (type == IBlockState.class)
				modelProvider[i] = BlockMimicModel.provider;
		}
	}

	public MultipartModel setPipeVariants(int n) {
		for (int i = 0; i < block.moduleCount(); i++) {
			Class<?> type = block.moduleType(i);
			if (type == Byte.class && modelProvider[i] == null) {
				ResourceLocation[] locs = new ResourceLocation[n];
				for (int j = 0; j < n; j++)
					locs[j] = new ModelResourceLocation(((Block)block).getRegistryName(), block.moduleVariant(i) + j);
				modelProvider[i] = new ProviderList(locs);
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
		for (IModelProvider provider : modelProvider)
			provider.bake(state, format, textureGetter);
		IProperty<?> base = block.getBaseState();
		Map<Object, IBakedModel> baked;
		if (base == null) {
			IModel model = ModelLoaderRegistry.getModelOrLogError(baseModels[0], "missing");
			baked = Collections.singletonMap(null, model.bake(model.getDefaultState(), format, textureGetter));
		} else {
			baked = new HashMap<>();
			int i = 0;
			for (Object val : block.getBaseState().getAllowedValues()) {
				IModel model = ModelLoaderRegistry.getModelOrLogError(baseModels[i++], "missing");
				baked.put(val, model.bake(model.getDefaultState(), format, textureGetter));
			}
		}
		return new BakedMultipart(baked);
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}

	@Override
	public void onReload() {}

	public class BakedMultipart implements IBakedModel {

		public final Map<Object, IBakedModel> base;
		public final IBakedModel main;

		private BakedMultipart(Map<Object, IBakedModel> base) {
			this.base = base;
			this.main = base.get(block.getBaseState() == null ? null : ((Block)block).getDefaultState().getValue(block.getBaseState()));
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			ArrayList<BakedQuad> list = new ArrayList<BakedQuad>();
			BlockRenderLayer layer = block.renderMultilayer() ? MinecraftForgeClient.getRenderLayer() : null;
			boolean render = true;
			int n = block.moduleCount();
			if (state instanceof IExtendedBlockState) {
				IExtendedBlockState exState = (IExtendedBlockState) state;
				IModularTile tile = exState.getValue(MultipartBlock.moduleRef);
				if (render = tile != null && !(side == null && tile.isOpaque()))
					for (int i = 0; i < n; i++) {
						Object val = tile.getModuleState(i);
						IBakedModel model = modelProvider[i].getModelFor(state, val, layer);
						if (model != null) list.addAll(model.getQuads(state, side, rand));
					}
			}
			if (render && (layer == null || layer == BlockRenderLayer.CUTOUT)) {
				IProperty<?> p = block.getBaseState();
				IBakedModel model = p == null ? main : base.get(state.getValue(p));
				if (model != null)
					list.addAll(model.getQuads(state, side, rand));
			}
			return list;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return main.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return main.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return main.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return ItemCameraTransforms.DEFAULT;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return itemHandler;
		}

		public IMultipartBlock getOwner() {
			return block;
		}

	}

	public interface IModelProvider {
		public IBakedModel getModelFor(Object val);
		public default IBakedModel getModelFor(Object val, @Nonnull BlockRenderLayer layer) {
			return layer == null || layer == BlockRenderLayer.CUTOUT ? getModelFor(val) : null;
		}
		public default IBakedModel getModelFor(IBlockState state, Object val, @Nonnull BlockRenderLayer layer) {
			return getModelFor(val, layer);
		}
		public Collection<ResourceLocation> getDependencies();
		public void bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter);
	}

	public class ProviderList implements IModelProvider {

		private final ResourceLocation[] models;
		private final IBakedModel[] baked;

		public ProviderList(ResourceLocation... models) {
			this.models = models;
			this.baked = new IBakedModel[models.length];
		}
		
		@Override
		public IBakedModel getModelFor(Object val) {
			int i;
			if (val instanceof Number) i = ((Number)val).intValue();
			else if (val instanceof Enum) i = ((Enum<?>)val).ordinal();
			else if (val instanceof Boolean && !(Boolean)val) return null;
			else i = 0;
			return i >= 0 && i < baked.length ? baked[i] : null;
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
