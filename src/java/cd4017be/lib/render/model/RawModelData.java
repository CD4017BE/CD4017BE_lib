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

	public static final ImmutableMap<TransformType, TRSRTransformation> DEFAULT_TRANSFORM;
	static {
		ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
		builder.put(TransformType.GUI, getTransform(new double[][] {{0D, 0D, 0D}, {0.625, 0.625, 0.625}, {30D, 225D, 0D}}));
		builder.put(TransformType.GROUND, getTransform(new double[][] {{0D, 3D, 0D}, {0.25, 0.25, 0.25}, {0D, 0D, 0D}}));
		builder.put(TransformType.FIXED, getTransform(new double[][] {{0D, 0D, 0D}, {0.5, 0.5, 0.5}, {0D, 0D, 0D}}));
		builder.put(TransformType.THIRD_PERSON_RIGHT_HAND, getTransform(new double[][] {{0D, 2.5, 0D}, {0.375, 0.375, 0.375}, {75D, 45D, 0D}}));
		builder.put(TransformType.THIRD_PERSON_LEFT_HAND, getTransform(new double[][] {{0D, 2.5, 0D}, {0.375, 0.375, 0.375}, {75D, 45D, 0D}}));
		builder.put(TransformType.FIRST_PERSON_RIGHT_HAND, getTransform(new double[][] {{0D, 0D, 0D}, {0.4, 0.4, 0.4}, {0D, 45D, 0D}}));
		builder.put(TransformType.FIRST_PERSON_LEFT_HAND, getTransform(new double[][] {{0D, 0D, 0D}, {0.4, 0.4, 0.4}, {0D, 225D, 0D}}));
		DEFAULT_TRANSFORM = builder.build();
	}
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
					if (quad.tex < 0 || quad.tex >= textures.length) throw new IllegalStateException("invalid texture index " + quad.tex + " !");
					for (double[] vert : quad.vertices) {
						data[j++] = Float.floatToRawIntBits((float)vert[0] / 16F);	//X
						data[j++] = Float.floatToRawIntBits((float)vert[1] / 16F);	//Y
						data[j++] = Float.floatToRawIntBits((float)vert[2] / 16F);	//Z
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
		builder.putAll(DEFAULT_TRANSFORM);
		boolean edit = false;
		if ((t = getTransform(script.read("dsp_head"))) != null) {builder.put(TransformType.HEAD, t); edit = true;}
		if ((t = getTransform(script.read("dsp_gui"))) != null) {builder.put(TransformType.GUI, t); edit = true;}
		if ((t = getTransform(script.read("dsp_ground"))) != null) {builder.put(TransformType.GROUND, t); edit = true;}
		if ((t = getTransform(script.read("dsp_fixed"))) != null) {builder.put(TransformType.FIXED, t); edit = true;}
		if ((t = getTransform(script.read("dsp_3PRighthand"))) != null) {builder.put(TransformType.THIRD_PERSON_RIGHT_HAND, t); edit = true;}
		if ((t = getTransform(script.read("dsp_3PLefthand"))) != null) {builder.put(TransformType.THIRD_PERSON_LEFT_HAND, t); edit = true;}
		if ((t = getTransform(script.read("dsp_1PRighthand"))) != null) {builder.put(TransformType.FIRST_PERSON_RIGHT_HAND, t); edit = true;}
		if ((t = getTransform(script.read("dsp_1PLefthand"))) != null) {builder.put(TransformType.FIRST_PERSON_LEFT_HAND, t); edit = true;}
		return edit ? builder.build() : DEFAULT_TRANSFORM;
	}

	private static TRSRTransformation getTransform(Object par) {
		if (!(par instanceof Object[])) return null;
		Object[] arr = (Object[])par;
		if (arr.length < 3) return null;
		Vector3f scale = read(arr[1]);
		Vector3f ofs = read(arr[0]);
		ofs.scale(1F/16F);
		return new TRSRTransformation(ofs, null, scale, TRSRTransformation.quatFromXYZDegrees(read(arr[2])));
	}

	private static Vector3f read(Object o) {
		if (o instanceof double[]) {
			double[] vec = (double[])o;
			return vec.length < 3 ? null : new Vector3f((float)vec[0], (float)vec[1], (float)vec[2]);
		} else return null;
	}

	private boolean read(Object val, boolean def) {
		return val instanceof Boolean ? (boolean)val : def;
	}

	private ResourceLocation[] readTextures(Object par) {
		if (!(par instanceof Object[])) throw new IllegalStateException("'textures' variable not defined! At least one texture required for particles.");
		Object[] arr = (Object[])par;
		if (arr.length == 0) throw new IllegalStateException("'textures' variable is empty array! At least one texture required for particles.");
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
		boolean hasNormal = format == DefaultVertexFormats.ITEM;
		if (format != DefaultVertexFormats.BLOCK && !hasNormal) return null;
		ModelRotation orient = state instanceof ModelRotation ? (ModelRotation)state : ModelRotation.X0_Y0;
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
					Util.rotate(data, l, orient);
					data[l + 4] = Float.floatToRawIntBits(texture.getInterpolatedU(Float.intBitsToFloat(data[l + 4])));	//U
					data[l + 5] = Float.floatToRawIntBits(texture.getInterpolatedV(Float.intBitsToFloat(data[l + 5])));	//V
					data[l + 6] = 0;
				}
				if (hasNormal) genNormals(data);
				dst[j] = new BakedQuad(data, -1, FaceBakery.getFacingFromVertexData(data), texture, diffuseLight, format);
			}
			if (orient == ModelRotation.X0_Y0 || i == 0) bm.quads[i] = dst;
			else bm.quads[orient.rotateFace(EnumFacing.VALUES[i - 1]).ordinal() + 1] = dst;
		}
		return bm;
	}

	private void genNormals(int[] data) {
		float[] pos = new float[12];
		for (int i = 0; i < 4; i++) {
			pos[i] = Float.intBitsToFloat(data[i * 7]);
			pos[i | 4] = Float.intBitsToFloat(data[i * 7 + 1]);
			pos[i | 8] = Float.intBitsToFloat(data[i * 7 + 2]);
		}
		for (int i = 0; i < 4; i++) {
			int i0 = (i + 3) & 3, i1 = (i + 1) & 3;
			float x0 = pos[i0] - pos[i],
				y0 = pos[i0|4] - pos[i|4],
				z0 = pos[i0|8] - pos[i|8],
				x1 = pos[i1] - pos[i],
				y1 = pos[i1|4] - pos[i|4],
				z1 = pos[i1|8] - pos[i|8];
			float x = y1 * z0 - z1 * y0,
				y = z1 * x0 - x1 * z0,
				z = x1 * y0 - y1 * x0;
			float d = 127F / (float)Math.sqrt(x*x + y*y + z*z);
			data[i * 7 + 6] = ((byte)(x * d) & 0xff) | ((byte)(y * d) & 0xff) << 8 | ((byte)(z * d) & 0xff) << 16;
		}
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

}
