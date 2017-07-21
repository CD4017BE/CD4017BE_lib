package cd4017be.lib.render.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.ModelContext.Quad;
import cd4017be.lib.script.Module;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

public class RawModelData implements IModel {

	private final int[][] quads = new int[7][];
	private final boolean diffuseLight, gui3D;
	private final ResourceLocation[] textures;
	private final ImmutableMap<TransformType, TRSRTransformation> transform;
	
	public RawModelData(Module script, ModelContext context) {
		diffuseLight = read(script.read("ambientOcclusion"), true);
		gui3D = read(script.read("isGui3D"), true);
		textures = readTextures(script.read("textures"));
		transform = readTransf(script);
		for (int i = 0; i < this.quads.length; i++) {
			List<Quad> list = context.quads[i];
			if (list != null) {
				int[] data = new int[28 * list.size()];
				int j = 0;
				for (Quad quad : list) {
					for (double[] vert : quad.vertices) {
						data[j++] = Float.floatToRawIntBits((float)vert[0]);	//X
						data[j++] = Float.floatToRawIntBits((float)vert[1]);	//Y
						data[j++] = Float.floatToRawIntBits((float)vert[2]);	//Z
						data[j++] = (int)MathHelper.clamp(vert[7] * 255D, 0, 255)		//B
								| (int)MathHelper.clamp(vert[6] * 255D, 0, 255) << 8	//G
								| (int)MathHelper.clamp(vert[5] * 255D, 0, 255) << 16	//R
								| (int)MathHelper.clamp(vert[8] * 255D, 0, 255) << 24;	//A
						data[j++] = Float.floatToRawIntBits((float)vert[3]);	//U
						data[j++] = Float.floatToRawIntBits((float)vert[4]);	//V
						data[j++] = quad.tex;	//temporary used to store texture id
					}
				}
				this.quads[i] = data;
			}
		}
	}

	private ImmutableMap<TransformType, TRSRTransformation> readTransf(Module script) {
		ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
		TRSRTransformation t;
		if ((t = getTransform(script.read("dsp_head"))) != null) builder.put(TransformType.HEAD, t);
		if ((t = getTransform(script.read("dsp_gui"))) != null) builder.put(TransformType.GUI, t);
		if ((t = getTransform(script.read("dsp_ground"))) != null) builder.put(TransformType.GROUND, t);
		if ((t = getTransform(script.read("dsp_fixed"))) != null) builder.put(TransformType.FIXED, t);
		if ((t = getTransform(script.read("dsp_3PRighthand"))) != null) builder.put(TransformType.THIRD_PERSON_RIGHT_HAND, t);
		if ((t = getTransform(script.read("dsp_3PLefthand"))) != null) builder.put(TransformType.THIRD_PERSON_LEFT_HAND, t);
		if ((t = getTransform(script.read("dsp_1PRighthand"))) != null) builder.put(TransformType.FIRST_PERSON_RIGHT_HAND, t);
		if ((t = getTransform(script.read("dsp_1PLefthand"))) != null) builder.put(TransformType.FIRST_PERSON_LEFT_HAND, t);
		return builder.build();
	}

	private TRSRTransformation getTransform(Object par) {
		if (!(par instanceof Object[])) return null;
		Object[] arr = (Object[])par;
		if (arr.length < 3) return null;
		return new TRSRTransformation(read(arr[0]), TRSRTransformation.quatFromXYZDegrees(read(arr[2])), read(arr[1]), null);
	}

	private Vector3f read(Object o) {
		if (o instanceof double[]) {
			double[] vec = (double[])o;
			return vec.length < 3 ? null : new Vector3f((float)vec[0], (float)vec[1], (float)vec[2]);
		} else return null;
	}

	private boolean read(Object val, boolean def) {
		return val instanceof Boolean ? (boolean)val : def;
	}

	private ResourceLocation[] readTextures(Object par) {
		if (!(par instanceof Object[])) return null;
		Object[] arr = (Object[])par;
		ResourceLocation[] textures = new ResourceLocation[arr.length];
		for (int i = 0; i < arr.length; i++)
			textures[i] = new ResourceLocation((String)arr[i]);
		return textures;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Arrays.asList(textures);
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		if (format != DefaultVertexFormats.BLOCK && format != DefaultVertexFormats.ITEM) return null;
		TextureAtlasSprite[] textures = new TextureAtlasSprite[this.textures.length];
		for (int i = 0; i < textures.length; i++) textures[i] = bakedTextureGetter.apply(this.textures[i]);
		Baked bm = new Baked(textures[0]);
		for (int i = 0; i < 7; i++) {
			int[] src = quads[i];
			BakedQuad[] dst = new BakedQuad[src.length / 28];
			for (int j = 0; j < dst.length; j++) {
				int k = j * 28;
				int[] data = Arrays.copyOfRange(src, k, k + 28);
				TextureAtlasSprite texture = textures[data[6]];
				for (int l = 0; l < 28; l += 7) {
					if (state instanceof ModelRotation) Util.rotate(data, l, (ModelRotation)state);
					data[l + 4] = Float.floatToRawIntBits(texture.getInterpolatedU(Float.intBitsToFloat(data[l + 4])));	//U
					data[l + 5] = Float.floatToRawIntBits(texture.getInterpolatedV(Float.intBitsToFloat(data[l + 5])));	//V
					data[l + 6] = 0; //TODO normals for Item format
				}
				dst[j] = new BakedQuad(data, -1, FaceBakery.getFacingFromVertexData(data), texture, diffuseLight, format);
			}
			bm.quads[i] = dst;
		}
		return bm;
	}

	@Override
	public IModelState getDefaultState() {
		return ModelRotation.X0_Y0;
	}

	public class Baked implements IPerspectiveAwareModel {

		private final BakedQuad[][] quads = new BakedQuad[7][];
		private final TextureAtlasSprite particle;

		private Baked(TextureAtlasSprite texture) {
			particle = texture;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			int i = side == null ? 0 : side.getIndex() + 1;
			return quads[i] == null ? Collections.<BakedQuad>emptyList() : Arrays.asList(quads[i]);
		}

		@Override
		public boolean isAmbientOcclusion() {
			return diffuseLight;
		}

		@Override
		public boolean isGui3d() {
			return gui3D;
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
			return null;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
			return IPerspectiveAwareModel.MapWrapper.handlePerspective(this, transform, cameraTransformType);
		}

	}

}
