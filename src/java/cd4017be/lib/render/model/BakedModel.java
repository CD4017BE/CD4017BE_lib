package cd4017be.lib.render.model;

import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.model.TRSRTransformation;

public class BakedModel implements IBakedModel {

	public final ImmutableMap<TransformType, TRSRTransformation> transform;
	public final List<BakedQuad>[] quads;
	public final TextureAtlasSprite particle;
	public final boolean diffuse, gui3d;

	@SuppressWarnings("unchecked")
	public BakedModel(TextureAtlasSprite texture, ImmutableMap<TransformType, TRSRTransformation> transform, boolean diffuse, boolean gui3d) {
		this.transform = transform;
		this.particle = texture;
		this.diffuse = diffuse;
		this.gui3d = gui3d;
		this.quads = new List[7];
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		return quads[side == null ? 0 : side.getIndex() + 1];
	}

	@Override
	public boolean isAmbientOcclusion() {
		return diffuse;
	}

	@Override
	public boolean isGui3d() {
		return gui3d;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return particle;
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return ItemCameraTransforms.DEFAULT;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.NONE;
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
		return Pair.of(this, transform.get(cameraTransformType).getMatrix());
	}

}
