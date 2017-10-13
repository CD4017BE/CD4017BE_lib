package cd4017be.lib.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cd4017be.lib.templates.BlockPipe;

import java.util.function.Function;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**@deprecated replaced by MultipartModel */
@SideOnly(Side.CLIENT)
@Deprecated
public class ModelPipe implements IModel {

	/** core0, core1, ..., con0[B, T, N, S, W, E], con1[...], ... */
	public final IBakedModel[] models;
	/** core0, core1, ..., con0, con1, ... */
	public final ResourceLocation[] dependencies;
	public final int Ncores, Ncons;
	
	public final String path;
	public ModelPipe(String path, int Ncores, int Ncons) {
		this.path = path;
		this.Ncores = Ncores;
		this.Ncons = Ncons;
		this.dependencies = new ResourceLocation[Ncores + Ncons];
		this.models = new IBakedModel[Ncores + 6 * Ncons];
		for (int i = 0; i < Ncores; i++)
			dependencies[i] = new ModelResourceLocation(path, "core" + i);
		for (int i = 0; i < Ncons; i++)
			dependencies[Ncores + i] = new ModelResourceLocation(path, "con" + i);
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return ImmutableList.copyOf(dependencies);
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		IModel model;
		for (int i = 0; i < dependencies.length; i++) {
			model = ModelLoaderRegistry.getModelOrLogError(dependencies[i], "missing pipe model component:");
			if (i < Ncores) {
				models[i] = model.bake(state, format, bakedTextureGetter);
			} else {
				int j = (i - Ncores) * 6 + Ncores;
				models[j] = model.bake(ModelRotation.X90_Y0, format, bakedTextureGetter);
				models[j+1] = model.bake(ModelRotation.X270_Y0, format, bakedTextureGetter);
				models[j+2] = model.bake(ModelRotation.X0_Y0, format, bakedTextureGetter);
				models[j+3] = model.bake(ModelRotation.X0_Y180, format, bakedTextureGetter);
				models[j+4] = model.bake(ModelRotation.X0_Y270, format, bakedTextureGetter);
				models[j+5] = model.bake(ModelRotation.X0_Y90, format, bakedTextureGetter);
			}
		}
		return new BakedPipe();
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}
	
	public class BakedPipe implements IBakedModel {

		@Override
		public boolean isAmbientOcclusion() {
			return false;
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return models[0].getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return null;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			if (state instanceof IExtendedBlockState) {
				IExtendedBlockState exts = (IExtendedBlockState)state;
				IBlockState block = exts.getValue(BlockPipe.COVER);
				IBakedModel model;
				if (block != null) {
					model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(block);
					return model.getQuads(block, side, rand);
				}
				//ModelManager manager = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager();
				Byte type = exts.getValue(BlockPipe.CORE);
				ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>();
				//if (type != null && type >= 0) quads.addAll(manager.getModel(new ModelResourceLocation(path, "core" + type.toString())).getQuads(state, side, rand));
				if (type != null && type >= 0 && type < Ncores) quads.addAll(models[type].getQuads(state, side, rand));
				for (int i = 0; i < 6; i++) {
					type = exts.getValue(BlockPipe.CONS[i]);
					//if (type != null && type >= 0) quads.addAll(manager.getModel(new ModelResourceLocation(path, "con" + type.toString() + sides[i])).getQuads(state, side, rand));
					if (type != null && type >= 0 && type < Ncons) quads.addAll(models[Ncores + i + type * 6].getQuads(state, side, rand));
				}
				return quads;
			}
			return Collections.emptyList();
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}
		
	}

}
