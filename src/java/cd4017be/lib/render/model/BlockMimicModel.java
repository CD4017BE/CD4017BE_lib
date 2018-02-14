package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import java.util.function.Function;

import cd4017be.lib.property.PropertyBlockMimic;
import cd4017be.lib.render.IHardCodedModel;
import cd4017be.lib.render.model.MultipartModel.IModelProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;

/**
 * 
 * @author CD4017BE
 */
public class BlockMimicModel implements IModel, IBakedModel, IHardCodedModel {

	public static final BlockMimicModel instance = new BlockMimicModel();
	public static final IModelProvider provider = new ProviderBlockMimic();

	private BlockMimicModel() {}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		if (state instanceof IExtendedBlockState) {
			IExtendedBlockState ext = (IExtendedBlockState) state;
			IBlockState block = ext.getValue(PropertyBlockMimic.instance);
			if (block != null) {
				Minecraft mc = Minecraft.getMinecraft();
				IBakedModel model = mc.getBlockRendererDispatcher()
						.getModelForState(block instanceof IExtendedBlockState ? ((IExtendedBlockState)block).getClean() : block);
				BlockColors bc = mc.getBlockColors();
				List<BakedQuad> quads = model.getQuads(block, side, rand);
				boolean edit = false;
				for (int i = 0; i < quads.size(); i++) {
					BakedQuad q = quads.get(i);
					if (q.hasTintIndex()) {
						int c = bc.colorMultiplier(block, null, null, q.getTintIndex()) | 0xff000000;
						if (c != -1) {
							int[] data = q.getVertexData().clone();
							for (int j = 3; j < 28; j+=7) data[j] = c;
							if (!edit) {
								quads = new ArrayList<BakedQuad>(quads);
								edit = true;
							}
							quads.set(i, new BakedQuad(data, -1, q.getFace(), q.getSprite(), q.shouldApplyDiffuseLighting(), q.getFormat()));
						}
					}
				}
				return quads;
			}
		}
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
		return null;
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return null;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Collections.emptyList();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return this;
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}

	@Override
	public void onReload() {}

	private static class ProviderBlockMimic implements IModelProvider {

		@Override
		public IBakedModel getModelFor(Object val) {
			return val instanceof IBlockState ? instance : null;
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return null;
		}

		@Override
		public void bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
		}

	}

}
