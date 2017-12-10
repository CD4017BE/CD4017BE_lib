package cd4017be.lib.render.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.ModelContext.Quad;
import cd4017be.lib.script.Module;
import cd4017be.lib.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.FMLLog;

/**
 * 
 * @author CD4017BE
 */
public class RawModelData implements IModel {

	public static final ImmutableMap<TransformType, TRSRTransformation> DEFAULT_TRANSFORM;
	private static final String[] TRANSFORMS = {"none", "3PLefthand", "3PRighthand", "1PLefthand", "1PRighthand", "head", "gui", "ground", "fixed"};
	static {
		ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
		builder.put(TransformType.GUI, getTransform(new double[][] {{0D, 0D, 0D}, {0.625, 0.625, 0.625}, {30D, 225D, 0D}}));
		builder.put(TransformType.GROUND, getTransform(new double[][] {{0D, 3D, 0D}, {0.25, 0.25, 0.25}, {0D, 0D, 0D}}));
		builder.put(TransformType.FIXED, getTransform(new double[][] {{0D, 0D, 0D}, {0.5, 0.5, 0.5}, {0D, 0D, 0D}}));
		builder.put(TransformType.THIRD_PERSON_RIGHT_HAND, getTransform(new double[][] {{0D, 2.5, 0D}, {0.375, 0.375, 0.375}, {75D, 45D, 0D}}));
		builder.put(TransformType.THIRD_PERSON_LEFT_HAND, getTransform(new double[][] {{0D, 2.5, 0D}, {0.375, 0.375, 0.375}, {75D, 45D, 0D}}));
		builder.put(TransformType.FIRST_PERSON_RIGHT_HAND, getTransform(new double[][] {{0D, 0D, 0D}, {0.4, 0.4, 0.4}, {0D, 45D, 0D}}));
		builder.put(TransformType.FIRST_PERSON_LEFT_HAND, getTransform(new double[][] {{0D, 0D, 0D}, {0.4, 0.4, 0.4}, {0D, 225D, 0D}}));
		builder.put(TransformType.HEAD, TRSRTransformation.identity());
		builder.put(TransformType.NONE, TRSRTransformation.identity());
		DEFAULT_TRANSFORM = builder.build();
	}
	private final int[][] quads = new int[7][];
	private final boolean diffuseLight, gui3D;
	private final ResourceLocation[] textures;
	private final ImmutableMap<TransformType, TRSRTransformation> transform;

	/**
	 * loads a model that was rendered by a script
	 * @param script the already executed script
	 * @param context the context where the vertices were drawn into
	 */
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
					for (int k = 0; k < 4; k++) {
						double[] vert = quad.vertices[k];
						data[j++] = Float.floatToRawIntBits((float)vert[0] / 16F);	//X
						data[j++] = Float.floatToRawIntBits((float)vert[1] / 16F);	//Y
						data[j++] = Float.floatToRawIntBits((float)vert[2] / 16F);	//Z
						data[j++] = (int)MathHelper.clamp(vert[5] * 255D, 0, 255)		//R
								| (int)MathHelper.clamp(vert[6] * 255D, 0, 255) << 8	//G
								| (int)MathHelper.clamp(vert[7] * 255D, 0, 255) << 16	//B
								| (int)MathHelper.clamp(vert[8] * 255D, 0, 255) << 24;	//A
						data[j++] = Float.floatToRawIntBits((float)vert[3]);	//U
						data[j++] = Float.floatToRawIntBits((float)vert[4]);	//V
						data[j++] = getNormal(quad.vertices, k)	//Normal
								| quad.tex << 24;				//TexIdx
					}
				}
				this.quads[i] = data;
			}
		}
	}

	private static final String
			NBT_AMBIENT_OCCLUSION = "ambOcc",
			NBT_GUI_3D = "gui3d",
			NBT_TEXTURE = "textures",
			NBT_TRANSFORM = "transform",
			NBT_QUADS = "vertexData";

	/**
	 * loads a model in NBT format
	 * @param parent optional parent model to inherit from
	 * @param nbt Compound tag containing the model data
	 * @param name the models resource name (used for error logs)
	 */
	public RawModelData(RawModelData parent, NBTTagCompound nbt, String name) {
		diffuseLight = nbt.hasKey(NBT_AMBIENT_OCCLUSION, NBT.TAG_BYTE) ? nbt.getBoolean(NBT_AMBIENT_OCCLUSION) : parent == null || parent.diffuseLight;
		gui3D = nbt.hasKey(NBT_GUI_3D, NBT.TAG_BYTE) ? nbt.getBoolean(NBT_GUI_3D) : parent == null || parent.gui3D;
		if (nbt.hasKey(NBT_TRANSFORM, NBT.TAG_COMPOUND)) {
			NBTTagCompound comp = nbt.getCompoundTag(NBT_TRANSFORM);
			ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
			builder.putAll(parent != null ? parent.transform : DEFAULT_TRANSFORM);
			boolean edit = false;
			for (TransformType type : TransformType.values()) {
				String key = TRANSFORMS[type.ordinal()];
				if (comp.hasKey(key, NBT.TAG_LIST)) {
					NBTTagList list = comp.getTagList(key, NBT.TAG_FLOAT);
					Vector3f ofs = new Vector3f(list.getFloatAt(0), list.getFloatAt(1), list.getFloatAt(2));
					Vector3f scale = new Vector3f(list.getFloatAt(3), list.getFloatAt(4), list.getFloatAt(5));
					Vector3f rot = new Vector3f(list.getFloatAt(6), list.getFloatAt(7), list.getFloatAt(8));
					builder.put(type, new TRSRTransformation(ofs, null, scale, TRSRTransformation.quatFromXYZDegrees(rot)));
					edit = true;
				}
			}
			transform = edit ? builder.build() : parent != null ? parent.transform : DEFAULT_TRANSFORM;
		} else if (parent != null) transform = parent.transform;
		else transform = DEFAULT_TRANSFORM;
		NBTTagList list = nbt.getTagList(NBT_TEXTURE, NBT.TAG_STRING);
		int n = parent != null ? parent.textures.length : 0;
		textures = n > 0 ? Arrays.copyOf(parent.textures, n + list.tagCount()) : new ResourceLocation[list.tagCount()];
		if (list.tagCount() > 0) {
			for (int i = n; i < textures.length; i++)
				textures[i] = new ResourceLocation(list.getStringTagAt(i - n));
		}
		list = nbt.getTagList(NBT_QUADS, NBT.TAG_INT_ARRAY);
		for (int i = 0; i < quads.length; i++) {
			int[] arr = list.getIntArrayAt(i);
			if (arr.length % 28 != 0) {
				FMLLog.log("NBT-Model", Level.ERROR, "%s: vertex data entry %d has size %d which is not a multiple of 28!", name, i, arr.length);
				arr = Arrays.copyOf(arr, arr.length / 28 * 28);
			}
			for (int j = 6; j < arr.length; j += 28) {
				int t = arr[j];
				if (t < 0 || t >= textures.length) {
					FMLLog.log("NBT-Model", Level.ERROR, "%s: invalid texture index %d in quad %d:%d, model has only %d textures!", name, t, i, j / 28, textures.length);
					arr[j] = 0;
				}
			}
			quads[i] = arr;
			if (parent != null) {
				int[] arr1 = parent.quads[i];
				n = arr1.length;
				if (n > 0) {
					if (arr.length > 0) {
						arr1 = Arrays.copyOf(arr1, n + arr.length);
						System.arraycopy(arr, 0, arr1, n, arr.length);
					}
					quads[i] = arr1;
				}
			}
		}
	}

	private ImmutableMap<TransformType, TRSRTransformation> readTransf(Module script) {
		ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
		builder.putAll(DEFAULT_TRANSFORM);
		boolean edit = false;
		for (TransformType type : TransformType.values()) {
			TRSRTransformation t = getTransform(script.read("dsp_" + TRANSFORMS[type.ordinal()]));
			if (t != null) {
				builder.put(type, t);
				edit = true;
			}
		}
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
		int n = textures.length;
		TextureAtlasSprite[] textures = n > 0 ? 
				Utils.init(new TextureAtlasSprite[n], (i)-> bakedTextureGetter.apply(this.textures[i])) :
				new TextureAtlasSprite[] {bakedTextureGetter.apply(TextureMap.LOCATION_MISSING_TEXTURE)};
		Baked bm = new Baked(textures[0]);
		for (int i = 0; i < 7; i++) {
			int[] src = quads[i];
			BakedQuad[] dst = new BakedQuad[src.length / 28];
			for (int j = 0; j < dst.length; j++) {
				int k = j * 28;
				int[] data = Arrays.copyOfRange(src, k, k + 28);
				int t = data[6] >> 24 & 0xff;
				TextureAtlasSprite texture = textures[t % textures.length];
				for (int l = 0; l < 28; l += 7) {
					Util.rotate(data, l, orient);
					data[l + 4] = Float.floatToRawIntBits(texture.getInterpolatedU(Float.intBitsToFloat(data[l + 4])));	//U
					data[l + 5] = Float.floatToRawIntBits(texture.getInterpolatedV(Float.intBitsToFloat(data[l + 5])));	//V
					data[l + 6] = hasNormal ? Util.rotateNormal(data[l + 6], orient) : 0;
				}
				dst[j] = new BakedQuad(data, -1, FaceBakery.getFacingFromVertexData(data), texture, diffuseLight, format);
			}
			if (orient == ModelRotation.X0_Y0 || i == 0) bm.quads[i] = dst;
			else bm.quads[orient.rotateFace(EnumFacing.VALUES[i - 1]).ordinal() + 1] = dst;
		}
		return bm;
	}

	private int getNormal(double[][] vert, int i) {
		int i0 = (i + 3) & 3, i1 = (i + 1) & 3;
		double
			x = vert[i][0], x0 = vert[i0][0] - x, x1 = vert[i1][0] - x,
			y = vert[i][1], y0 = vert[i0][1] - y, y1 = vert[i1][1] - y,
			z = vert[i][2], z0 = vert[i0][2] - z, z1 = vert[i1][2] - z;
		x = y1 * z0 - z1 * y0;
		y = z1 * x0 - x1 * z0;
		z = x1 * y0 - y1 * x0;
		double d = 127F / Math.sqrt(x*x + y*y + z*z);
		return ((byte)(x * d) & 0xff) | ((byte)(y * d) & 0xff) << 8 | ((byte)(z * d) & 0xff) << 16;
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
