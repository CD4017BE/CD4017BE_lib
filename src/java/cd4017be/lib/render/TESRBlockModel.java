package cd4017be.lib.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 * @deprecated replaced by {@link cd4017be.lib.render.model.RawModelData}
 */
@SideOnly(Side.CLIENT)
@Deprecated
public class TESRBlockModel implements IBakedModel {

	BakedQuad[][] quads = new BakedQuad[7][];
	TextureAtlasSprite texture;
	public TESRBlockModel(TextureAtlasSprite texture, boolean retexture, int color, int[]... data) {
		for (int i = 0; i < quads.length && i < data.length; i++) {
			if (data[i] != null) {
				quads[i] = new BakedQuad[data[i].length / 28];
				for (int j = 0; j < quads[i].length; j++) {
					int[] buff = new int[28];
					System.arraycopy(data[i], j * 28, buff, 0, 28);
					if (retexture) for (int k = 0; k < 28; k += 7) {
						buff[k + 4] = Float.floatToIntBits(texture.getInterpolatedU(Float.intBitsToFloat(buff[k + 4])));
						buff[k + 5] = Float.floatToIntBits(texture.getInterpolatedV(Float.intBitsToFloat(buff[k + 5])));
					}
					if (color != -1) for (int k = 0; k < 28; k += 7) buff[k + 3] &= color;
					quads[i][j] = new BakedQuad(buff, -1, null, texture);
				}
			}
		}
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		int i = side == null ? 0 : side.getIndex() + 1;
		return quads[i] == null ? Collections.<BakedQuad>emptyList() : Arrays.asList(quads[i]);
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
		return texture;
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return ItemCameraTransforms.DEFAULT;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}

}
