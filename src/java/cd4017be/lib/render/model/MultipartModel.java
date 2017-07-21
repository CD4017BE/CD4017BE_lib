package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.google.common.base.Function;

import cd4017be.lib.block.MultipartBlock;
import cd4017be.lib.property.PropertyBoolean;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class MultipartModel implements IModel, IHardCodedModel {

	public final IModelProvider[] modelProvider;
	public final ResourceLocation baseModel;
	public final MultipartBlock block;

	public MultipartModel(MultipartBlock block) {
		this.block = block;
		this.baseModel = new ModelResourceLocation(block.getRegistryName(), "base");
		this.modelProvider = new IModelProvider[block.modules.length];
		for (int i = 0; i < block.modules.length; i++) {
			IUnlistedProperty<?> prop = block.modules[i];
			if (prop instanceof PropertyBoolean)
				modelProvider[i] = new ProviderBool(new ModelResourceLocation(block.getRegistryName(), prop.getName()));
		}
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>();
		list.add(baseModel);
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
		IModel model = ModelLoaderRegistry.getModelOrLogError(baseModel, "missing base Model");
		for (IModelProvider provider : modelProvider) provider.bake(state, format, textureGetter);
		return new BakedMultipart(model.bake(state, format, textureGetter));
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}

	public class BakedMultipart implements IBakedModel {

		private final IBakedModel base;

		private BakedMultipart(IBakedModel base) {
			this.base = base;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			ArrayList<BakedQuad> list = new ArrayList<BakedQuad>();
			list.addAll(base.getQuads(state, side, rand));
			if (state instanceof IExtendedBlockState) {
				IExtendedBlockState exState = (IExtendedBlockState) state;
				for (int i = 0; i < block.modules.length; i++) {
					IBakedModel model = modelProvider[i].getModelFor(exState.getValue(block.modules[i]));
					if (model != null) list.addAll(model.getQuads(state, side, rand));
				}
			}
			return list;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return base.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return base.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return base.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return null;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}

	}

	public interface IModelProvider {
		public IBakedModel getModelFor(Object val);
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

	@Override
	public void onReload() {}

}
