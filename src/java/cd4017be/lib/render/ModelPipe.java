package cd4017be.lib.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cd4017be.lib.templates.BlockPipe;

import com.google.common.base.Function;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;

public class ModelPipe implements IModel {

	public static final String[] sides = {"b", "t", "n", "s", "w", "e"};
	
	public final ArrayList<ResourceLocation> dependencies;
	public final String path;
	public ModelPipe(String path, int Ncores, int Ncons) {
		this.path = path;
		this.dependencies = new ArrayList<ResourceLocation>();
		for (int i = 0; i < Ncores; i++)
			dependencies.add(new ModelResourceLocation(path, "core" + i));
		for (int i = 0; i < Ncons; i++)
			for (String side : sides)
				dependencies.add(new ModelResourceLocation(path, "con" + i + side));
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return dependencies;
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return new BakedPipe(path);
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}
	
	public static class BakedPipe implements IBakedModel {

		private final String path;
		
		public BakedPipe(String path) {
			this.path = path;
		}

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
			return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(new ModelResourceLocation(path, "core0")).getParticleTexture();
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
				ModelManager manager = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager();
				Byte type = exts.getValue(BlockPipe.CORE);
				ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>();
				if (type != null && type >= 0) quads.addAll(manager.getModel(new ModelResourceLocation(path, "core" + type.toString())).getQuads(state, side, rand));
				for (int i = 0; i < 6; i++) {
					type = exts.getValue(BlockPipe.CONS[i]);
					if (type != null && type >= 0) quads.addAll(manager.getModel(new ModelResourceLocation(path, "con" + type.toString() + sides[i])).getQuads(state, side, rand));
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
