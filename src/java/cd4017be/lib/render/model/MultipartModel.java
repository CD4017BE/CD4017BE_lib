package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import java.util.function.Function;

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
	public final Map<IBlockState, ? extends ResourceLocation> baseMap;
	public final Block block;
	public final boolean multiLayer;
	public ItemOverrideList itemHandler = ItemOverrideList.NONE;

	public MultipartModel(MultipartBlock block) {
		this(block, stateMap(block, block.getBaseState()), block.renderMultilayer(), new IModelProvider[block.numModules]);
		for (int i = 0; i < block.numModules; i++) {
			Class<?> type = block.moduleType(i);
			if (type == Boolean.class)
				modelProvider[i] = new ProviderList(new ModelResourceLocation(block.getRegistryName(), block.moduleVariant(i)));
			else if (type == IBlockState.class)
				modelProvider[i] = BlockMimicModel.provider;
		}
	}

	public static Map<IBlockState, ResourceLocation> stateMap(Block block, IProperty<?> prop) {
		ResourceLocation loc = block.getRegistryName();
		HashMap<IBlockState, ResourceLocation> map = new HashMap<>();
		if (prop == null) {
			ModelResourceLocation mloc = new ModelResourceLocation(loc, "base");
			for (IBlockState state : block.getBlockState().getValidStates())
				map.put(state, mloc);
		} else for (IBlockState state : block.getBlockState().getValidStates())
			map.put(state, new ModelResourceLocation(loc, "base" + state.getValue(prop)));
		return map;
	}

	public MultipartModel(Block block, Map<IBlockState, ? extends ResourceLocation> baseMap, boolean multiLayer, IModelProvider... providers) {
		this.block = block;
		this.baseMap = baseMap;
		this.multiLayer = multiLayer;
		this.modelProvider = providers;
	}

	public MultipartModel setProvider(int i, IModelProvider p) {
		this.modelProvider[i] = p;
		return this;
	}

	public MultipartModel setPipeVariants(int n) {
		MultipartBlock block = (MultipartBlock)this.block;
		for (int i = 0; i < block.moduleCount(); i++) {
			Class<?> type = block.moduleType(i);
			if (type == Byte.class && modelProvider[i] == null) {
				ResourceLocation[] locs = new ResourceLocation[n];
				for (int j = 0; j < n; j++)
					locs[j] = new ModelResourceLocation(block.getRegistryName(), block.moduleVariant(i) + j);
				modelProvider[i] = new ProviderList(locs);
			}
		}
		return this;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		HashSet<ResourceLocation> set = new HashSet<>();
		set.addAll(baseMap.values());
		for (IModelProvider provider : modelProvider) {
			Collection<ResourceLocation> c = provider.getDependencies();
			if (c != null) set.addAll(c);
		}
		return set;
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
		for (IModelProvider provider : modelProvider)
			provider.bake(format, textureGetter);
		Map<IBlockState, IBakedModel> baked = new HashMap<>();
		for (Entry<IBlockState, ? extends ResourceLocation> e : baseMap.entrySet()) {
			IModel model = ModelLoaderRegistry.getModelOrLogError(e.getValue(), "missing");
			baked.put(e.getKey(), model.bake(model.getDefaultState(), format, textureGetter));
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

		public final Map<IBlockState, IBakedModel> base;
		public final IBakedModel main;

		private BakedMultipart(Map<IBlockState, IBakedModel> base) {
			this.base = base;
			this.main = base.get(((Block)block).getDefaultState());
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			ArrayList<BakedQuad> list = new ArrayList<BakedQuad>();
			render: {
				BlockRenderLayer layer = multiLayer ? MinecraftForgeClient.getRenderLayer() : null;
				if (state instanceof IExtendedBlockState) {
					IModularTile tile = ((IExtendedBlockState)state).getValue(MultipartBlock.moduleRef);
					if (tile == null || side == null && tile.isOpaque())
						break render;
					for (int i = 0; i < modelProvider.length; i++)
						modelProvider[i].getQuads(list, tile.getModuleState(i), layer, state, side, rand);
					state = ((IExtendedBlockState)state).getClean();
				}
				if (layer != null && layer != BlockRenderLayer.CUTOUT)
					break render;
				IBakedModel model = base.get(state);
				if (model == null)
					break render;
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

	}

	public interface IModelProvider {
		@Deprecated
		default IBakedModel getModelFor(Object val) {return null;}
		@Deprecated
		default IBakedModel getModelFor(Object val, @Nonnull BlockRenderLayer layer) {
			return layer == null || layer == BlockRenderLayer.CUTOUT ? getModelFor(val) : null;
		}
		default void getQuads(List<BakedQuad> quads, Object val, @Nonnull BlockRenderLayer layer, IBlockState state, EnumFacing side, long rand) {
			IBakedModel model = getModelFor(val, layer);
			if (model != null) quads.addAll(model.getQuads(state, side, rand));
		}
		Collection<ResourceLocation> getDependencies();
		void bake(VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter);
	}

	public static class ProviderList implements IModelProvider {

		private final ResourceLocation[] models;
		private final IBakedModel[] baked;

		public ProviderList(ResourceLocation... models) {
			this.models = models;
			this.baked = new IBakedModel[models.length];
		}

		@Override
		public void getQuads(List<BakedQuad> quads, Object val, BlockRenderLayer layer, IBlockState state, EnumFacing side, long rand) {
			if (layer != null && layer != BlockRenderLayer.CUTOUT) return;
			int i;
			if (val instanceof Number) i = ((Number)val).intValue();
			else if (val instanceof Enum) i = ((Enum<?>)val).ordinal();
			else if (val instanceof Boolean && !(Boolean)val) return;
			else i = 0;
			if (i < 0 || i >= baked.length) return;
			quads.addAll(baked[i].getQuads(state, side, rand));
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Arrays.asList(models);
		}

		@Override
		public void bake(VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
			for (int i = 0; i < models.length; i++) {
				IModel model = ModelLoaderRegistry.getModelOrLogError(models[i], "missing");
				baked[i] = model.bake(model.getDefaultState(), format, textureGetter);
			}
		}

	}

}
