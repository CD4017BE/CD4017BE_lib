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
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.common.property.IExtendedBlockState;

public class ModelPipe implements IModel {

	public static final String[] sides = {"B", "T", "N", "S", "W", "E"};
	
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
	public IFlexibleBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return new BakedPipe(format, path);
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}
	
	public static class BakedPipe implements ISmartBlockModel, IFlexibleBakedModel {

		private final VertexFormat format;
		private final String path;
		
		public BakedPipe(VertexFormat format, String path) {
			this.format = format;
			this.path = path;
		}
		
		@Override
		public List<BakedQuad> getFaceQuads(EnumFacing p_177551_1_) {
			return Collections.emptyList();
		}

		@Override
		public List<BakedQuad> getGeneralQuads() {
			return Collections.emptyList();
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
		public IBakedModel handleBlockState(IBlockState state) {
			if (state instanceof IExtendedBlockState) {
				IExtendedBlockState exts = (IExtendedBlockState)state;
				IBlockState block = exts.getValue(BlockPipe.COVER);
				if (block != null) {
					IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(block);
					if (model instanceof ISmartBlockModel) model = ((ISmartBlockModel)model).handleBlockState(block);
					return model;
				}
				ModelManager manager = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager();
				Byte type = exts.getValue(BlockPipe.CORE);
				CombinedModel model = new CombinedModel(manager.getModel(new ModelResourceLocation(path, "core" + (type == null ? "0" : type.toString()))));
				for (int i = 0; i < 6; i++) {
					type = exts.getValue(BlockPipe.CONS[i]);
					if (type != null && type >= 0) model.add(manager.getModel(new ModelResourceLocation(path, "con" + type.toString() + sides[i])));
				}
				return model;
			}
			return this;
		}

		@Override
		public VertexFormat getFormat() {
			return format;
		}
		
	}

}
