package cd4017be.lib.render;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4f;

import org.lwjgl.util.vector.Vector3f;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;

@SuppressWarnings("deprecation")
public class CombinedModel implements IBakedModel {

	public static final ModelRotation[] Rotations = {ModelRotation.X90_Y0, ModelRotation.X270_Y0, ModelRotation.X0_Y0, ModelRotation.X0_Y180, ModelRotation.X0_Y270, ModelRotation.X0_Y90};
	private final IBakedModel origin;
	private ArrayList<BakedQuad>[] quads = new ArrayList[7];
	
	public CombinedModel(IBakedModel origin)
	{
		this.origin = origin;
		for (int i = 0; i < quads.length; i++) quads[i] = new ArrayList<BakedQuad>();
		this.add(origin);
	}
	
	public CombinedModel add(IBakedModel model) {
		for (int i = 0; i < 6; i++) this.quads[i].addAll(model.getFaceQuads(EnumFacing.VALUES[i]));
		this.quads[6].addAll(model.getGeneralQuads());
		return this;
	}
	
	public CombinedModel addRotated(IBakedModel model, ModelRotation rot) {
		if (rot == ModelRotation.X0_Y0) return this.add(model);
		List<BakedQuad> list;
		int[] data;
		Vector3f v = new Vector3f();
		Matrix4f m = rot.getMatrix();
		for (int j = 0; j < quads.length; j++) {
			list = j < 6 ? model.getFaceQuads(rot.rotate(EnumFacing.VALUES[j])) : model.getGeneralQuads();
			if (list == null || list.isEmpty()) continue;
			for (BakedQuad q : list) {
				data = new int[28];
				System.arraycopy(q.getVertexData(), 0, data, 0, data.length);
				for (int i = 0; i < data.length; i+=7) {
					v.set(Float.intBitsToFloat(data[i]), Float.intBitsToFloat(data[i + 1]), Float.intBitsToFloat(data[i + 2]));
					ForgeHooksClient.transform(v, m);
					data[i] = Float.floatToRawIntBits(v.x);
					data[i + 1] = Float.floatToRawIntBits(v.y);
					data[i + 2] = Float.floatToRawIntBits(v.z);
				}
				quads[j].add(new BakedQuad(data, q.getTintIndex(), rot.rotate(q.getFace())));
			}
		}
		return this;
	}
	
	@Override
	public List<BakedQuad> getFaceQuads(EnumFacing face) {
		return quads[face.getIndex()];
	}

	@Override
	public List<BakedQuad> getGeneralQuads() {
		return quads[6];
	}

	@Override
	public boolean isAmbientOcclusion() {
		return origin.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return origin.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return origin.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return origin.getParticleTexture();
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return origin.getItemCameraTransforms();
	}

}
