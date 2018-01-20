package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Vector3f;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import cd4017be.lib.render.Util;
import cd4017be.lib.script.Parameters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * 
 * @author cd4017be
 */
public class NBTModel implements IModel {

	private static final String
		NBT_AMBIENT_OCCLUSION = "ambOcc",
		NBT_GUI_3D = "gui3d",
		NBT_TRANSFORM = "item_transf",
		NBT_VERTEX_TRANSFORM = "vert_transf",
		NBT_UV_TRANSFORM = "uv_transf",
		NBT_TEX_TRANSFORM = "tex_remap",
		NBT_PARTICLE = "particle_tex",
		NBT_COLOR = "hasColor",
		NBT_CULLFACE = "cullfaces",
		NBT_TEXTURE = "textures",
		NBT_VERTEX = "vertices",
		NBT_UV = "uvs",
		NBT_QUAD = "quads",
		NBT_MASK = "mask";

	private final NBTTagCompound data;
	private final boolean coreVertices, coreUVs, diffuseLight, gui3D;
	private final ResourceLocation[] textures;
	private final ImmutableMap<TransformType, TRSRTransformation> transform;

	public NBTModel(NBTTagCompound data) {
		this.data = data;
		this.coreVertices = data.hasKey(NBT_VERTEX, NBT.TAG_INT_ARRAY);
		this.coreUVs = data.hasKey(NBT_UV, NBT.TAG_INT_ARRAY);
		this.diffuseLight = !data.hasKey(NBT_AMBIENT_OCCLUSION, NBT.TAG_BYTE) || data.getBoolean(NBT_AMBIENT_OCCLUSION);
		this.gui3D = !data.hasKey(NBT_GUI_3D, NBT.TAG_BYTE) || data.getBoolean(NBT_GUI_3D);
		if (data.hasKey(NBT_TRANSFORM, NBT.TAG_COMPOUND)) {
			NBTTagCompound comp = data.getCompoundTag(NBT_TRANSFORM);
			ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
			builder.putAll(RawModelData.DEFAULT_TRANSFORM);
			boolean edit = false;
			for (TransformType type : TransformType.values()) {
				String key = RawModelData.TRANSFORMS[type.ordinal()];
				if (comp.hasKey(key, NBT.TAG_LIST)) {
					NBTTagList list = comp.getTagList(key, NBT.TAG_FLOAT);
					Vector3f ofs = new Vector3f(list.getFloatAt(0), list.getFloatAt(1), list.getFloatAt(2));
					Vector3f scale = new Vector3f(list.getFloatAt(3), list.getFloatAt(4), list.getFloatAt(5));
					Vector3f rot = new Vector3f(list.getFloatAt(6), list.getFloatAt(7), list.getFloatAt(8));
					builder.put(type, new TRSRTransformation(ofs, null, scale, TRSRTransformation.quatFromXYZDegrees(rot)));
					edit = true;
				}
			}
			transform = edit ? builder.build() : RawModelData.DEFAULT_TRANSFORM;
			data.removeTag(NBT_TRANSFORM);
		} else transform = RawModelData.DEFAULT_TRANSFORM;
		NBTTagList list = data.getTagList(NBT_TEXTURE, NBT.TAG_STRING);
		int n = list.tagCount();
		if (n < 1) n = 1;
		this.textures = new ResourceLocation[n];
		for (int i = 0; i < n; i++)
			textures[i] = new ResourceLocation(list.getStringTagAt(i));
		data.removeTag(NBT_TEXTURE);
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Arrays.asList(textures);
	}

	public IntArrayModel bakeTESR(ParamertisedVariant variant) {
		int[][] vdata = getModelData(variant);
		int[] vertices = vdata[0], uvs = vdata[1], faces = vdata[2], texidx = vdata[3];
		int cmode = vdata[4][0], c = vdata[4][1];
		TextureAtlasSprite[] cache = new TextureAtlasSprite[textures.length];
		TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
		Function<ResourceLocation, TextureAtlasSprite> texGetter = (rl)-> map.getAtlasSprite(rl.toString());
		int o = cmode == 0 ? 7 : cmode == 1 ? 4 : 3, n = faces.length / o;
		int[] vertexData = new int[n * 28];
		int l = 0;
		for (int i = 0; i < n; i++) {
			int j = i * o;
			int vi = faces[j++], uvi = faces[j++], nti = faces[j++];
			if (cmode == 1) c = faces[j];
			TextureAtlasSprite tex = getTexture(cache, nti >> 24 & 0xff, texidx, texGetter);
			for (int k, m = 0; m < 4; m++, vi >>= 8, uvi >>= 8) {
				k = (vi & 0xff) * 3; 
				vertexData[l++] = vertices[k++];	//X
				vertexData[l++] = vertices[k++];	//Y
				vertexData[l++] = vertices[k];		//Z
				vertexData[l++] = cmode == 0 ? faces[j++] : c;	//color
				k = (uvi & 0xff) * 2;
				vertexData[l++] = Float.floatToRawIntBits(tex.getInterpolatedU(Float.intBitsToFloat(uvs[k++])));	//U
				vertexData[l++] = Float.floatToRawIntBits(tex.getInterpolatedV(Float.intBitsToFloat(uvs[k])));		//V
				vertexData[l++] = 0;	//normal
			}
		}
		return new IntArrayModel(vertexData);
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		boolean hasNormal = format == DefaultVertexFormats.ITEM;
		if (format != DefaultVertexFormats.BLOCK && !hasNormal) return null;
		ParamertisedVariant pstate = state instanceof ParamertisedVariant ? (ParamertisedVariant)state : state instanceof ModelRotation ? new ParamertisedVariant((ModelRotation)state) : ParamertisedVariant.BASE;
		NBTTagCompound tag = data.getCompoundTag(pstate.subModel);
		int[][] vdata = getModelData(pstate);
		int[] vertices = vdata[0], uvs = vdata[1], faces = vdata[2], texidx = vdata[3];
		int cmode = vdata[4][0], c = vdata[4][1];
		TextureAtlasSprite[] cache = new TextureAtlasSprite[textures.length];
		BakedModel model = new BakedModel(getTexture(cache, tag.getByte(NBT_PARTICLE), texidx, bakedTextureGetter), transform, diffuseLight, gui3D);
		byte[] cf = tag.getByteArray(NBT_CULLFACE);
		
		List<BakedQuad>[] quads = model.quads;
		for (int i = 0; i < quads.length; i++) quads[i] = new ArrayList<BakedQuad>();
		int o = cmode == 0 ? 7 : cmode == 1 ? 4 : 3, n = faces.length / o;
		for (int i = 0; i < n; i++) {
			int j = i * o;
			int vi = faces[j++], uvi = faces[j++], nti = faces[j++];
			if (cmode == 1) c = faces[j];
			TextureAtlasSprite tex = getTexture(cache, nti >> 24 & 0xff, texidx, bakedTextureGetter);
			nti = hasNormal ? Util.rotateNormal(nti & 0xffffff, pstate.orient) : 0;
			int[] vertexData = new int[28];
			for (int k, l = 0; l < 28; vi >>= 8, uvi >>= 8) {
				k = (vi & 0xff) * 3; 
				vertexData[l++] = vertices[k++];	//X
				vertexData[l++] = vertices[k++];	//Y
				vertexData[l++] = vertices[k];		//Z
				vertexData[l++] = cmode == 0 ? faces[j++] : c;	//color
				k = (uvi & 0xff) * 2;
				vertexData[l++] = Float.floatToRawIntBits(tex.getInterpolatedU(Float.intBitsToFloat(uvs[k++])));	//U
				vertexData[l++] = Float.floatToRawIntBits(tex.getInterpolatedV(Float.intBitsToFloat(uvs[k])));		//V
				vertexData[l++] = nti;	//normal
			}
			quads[i < cf.length && (j = cf[i] & 0xff) < 6 ? j + 1 : 0].add(
					new BakedQuad(vertexData, -1, FaceBakery.getFacingFromVertexData(vertexData), tex, diffuseLight, format));
		}
		for (int i = 0; i < quads.length; i++)
			if (quads[i].isEmpty())
				quads[i] = Collections.emptyList();
		return model;
	}

	private int[][] getModelData(ParamertisedVariant variant) {
		NBTTagCompound tag = data.getCompoundTag(variant.subModel);
		int[] vertices = (coreVertices ? data : tag).getIntArray(NBT_VERTEX);
		int[] uvs = (coreUVs ? data : tag).getIntArray(NBT_UV);
		int[] remap = null;
		byte colorMode = tag.getByte(NBT_COLOR);
		int color = 0xffffffff;
		if (variant.params != null) {
			NBTTagList vt = tag.getTagList(NBT_TEX_TRANSFORM, NBT.TAG_COMPOUND);
			if (vt.hasNoTags()) vt = data.getTagList(NBT_TEX_TRANSFORM, NBT.TAG_COMPOUND);
			if (!vt.hasNoTags()) {
				remap = new int[textures.length];
				for (int i = 0; i < remap.length; i++) remap[i] = i;
				for (NBTBase e : vt) {
					NBTTagCompound nbt = (NBTTagCompound)e;
					int ofs;
					try {
						ofs = nbt.getByte("shift") * (int)variant.params.getNumber(nbt.getByte("par"));
					} catch(IllegalArgumentException ex) {continue;}
					for (byte src : nbt.getByteArray("ids")) {
						int i = src & 0xff;
						if (i >= remap.length) continue;
						int j = ofs + (src & 0xff);
						if (j >= 0 && j < remap.length)
							remap[i] = j;
					}
				}
			}
			vt = tag.getTagList(NBT_UV_TRANSFORM, NBT.TAG_COMPOUND);
			if (coreUVs && vt.hasNoTags())
				vt = data.getTagList(NBT_UV_TRANSFORM, NBT.TAG_COMPOUND);
			if (!vt.hasNoTags())
				uvs = transform(uvs, 2, vt, variant.params);
			vt = tag.getTagList(NBT_VERTEX_TRANSFORM, NBT.TAG_COMPOUND);
			if (coreVertices && vt.hasNoTags())
				vt = data.getTagList(NBT_VERTEX_TRANSFORM, NBT.TAG_COMPOUND);
			boolean flag = vt.hasNoTags();
			if (!flag) vertices = transform(vertices, 3, vt, variant.params);
			ModelRotation orient = variant.orient;
			if (orient != ModelRotation.X0_Y0) {
				if (flag) vertices = vertices.clone();
				for (int i = 0; i < vertices.length; i += 3)
					Util.rotate(vertices, i, orient);
			}
			if (colorMode >= 0) {
				try {
					String s = variant.params.getString(colorMode);
					if (s.startsWith("0x")) color = Integer.parseUnsignedInt(s.substring(2), 16) | 0xff000000;
				} catch(IllegalArgumentException e) {}
				colorMode = -1;
			}
		}
		return new int[][] {vertices, uvs, tag.getIntArray(NBT_QUAD), remap, new int[] {colorMode + 3, color}};
	}

	private TextureAtlasSprite getTexture(TextureAtlasSprite[] cache, int i, int[] remap, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		if (i > cache.length || i < 0) i = 0;
		TextureAtlasSprite tex = cache[i];
		if (tex == null) cache[i] = tex = bakedTextureGetter.apply(textures[remap == null ? i : remap[i]]);
		return tex;
	}

	private int[] transform(int[] data, int dim, NBTTagList transf, Parameters param) {
		data = data.clone();
		for (NBTBase tag : transf) {
			NBTTagCompound nbt = (NBTTagCompound)tag;
			float[] ofs = new float[dim], scale = new float[dim];
			if (!nbt.hasKey("s_", NBT.TAG_LIST))
				for (int i = 0; i < dim; i++) scale[i] = 1F;
			for (String k : nbt.getKeySet()) {
				float[] arr;
				if (k.startsWith("s")) arr = scale;
				else if (k.startsWith("o")) arr = ofs;
				else continue;
				String p = k.substring(1);
				float arg;
				if (p.equals("_")) arg = 1F;
				else try {
					arg = (float)param.getNumber(Integer.parseInt(p));
				} catch (IllegalArgumentException e) { arg = 0; }
				if (arg != 0) {
					NBTTagList vec = nbt.getTagList(k, NBT.TAG_FLOAT);
					for (int i = 0; i < dim; i++)
						arr[i] += vec.getFloatAt(i) * arg;
				}
			}
			BitSet mask = BitSet.valueOf(nbt.getByteArray(NBT_MASK));
			for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1)) {
				int k = i * dim;
				if (k + dim > data.length) break;
				for (int j = 0; j < dim; j++, k++)
					data[k] = Float.floatToIntBits(ofs[j] + scale[j] * Float.intBitsToFloat(data[k]));
			}
		}
		return data;
	}

	@Override
	public IModelState getDefaultState() {
		return ParamertisedVariant.BASE;
	}

}
