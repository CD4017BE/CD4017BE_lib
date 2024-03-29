package cd4017be.lib.render.model;

import static cd4017be.lib.render.model.JitBakedModel.JIT_BAKED_MODEL;
import static cd4017be.lib.render.model.ScriptModel.resolveTex;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.*;
import net.minecraftforge.client.model.geometry.IModelGeometry;

/**A Block Model that lets the TileEntity control what to render,
 * including custom BakedQuads via {@link JitBakedModel}
 * and/or part models via {@link PartModel}.
 * For convenience and to reduce file clutter, part models can
 * also be defined directly as {@link ScriptModel} via in-line code.<br>
 * 
 * JSON Format:<pre>
 * "parent": "parent/model",
 * "textures": {
 *     "particle": "texture:path",
 *     "script_tex_name": "texture:path",
 *     ...
 * },
 * "loader": "cd4017be_lib:te",
 * "parts": {
 *     "name1": "model:path",
 *     "name2": ["script", "code"],
 *     ...
 * }</pre>
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class TileEntityModel implements IModelGeometry<TileEntityModel> {

	private final List<Pair<String, IModelGeometry<?>>> partModels;

	public TileEntityModel(List<Pair<String, IModelGeometry<?>>> partModels) {
		this.partModels = partModels;
	}

	@Override
	public IBakedModel bake(
		IModelConfiguration owner, ModelBakery bakery,
		Function<RenderMaterial, TextureAtlasSprite> spriteGetter,
		IModelTransform modelTransform, ItemOverrideList overrides,
		ResourceLocation modelLocation
	) {
		HashMap<String, IBakedModel> map = new HashMap<>(partModels.size());
		for (Pair<String, IModelGeometry<?>> e : partModels)
			map.put(e.getLeft(), e.getRight().bake(
				owner, bakery, spriteGetter,
				modelTransform, overrides, modelLocation
			));
		return new Baked(
			map, owner.isSideLit(), owner.isShadedInGui(), owner.useSmoothLighting(),
			spriteGetter.apply(owner.resolveTexture("particle")), owner.getCameraTransforms()
		);
	}

	@Override
	public Collection<RenderMaterial> getTextures(
		IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter,
		Set<com.mojang.datafixers.util.Pair<String, String>> missingTextureErrors
	) {
		Set<RenderMaterial> set = Sets.newHashSet();
		set.add(resolveTex("particle", owner, missingTextureErrors));
		for (Pair<String, IModelGeometry<?>> e : partModels)
			set.addAll(e.getRight().getTextures(owner, modelGetter, missingTextureErrors));
		return set;
	}


	@OnlyIn(Dist.CLIENT)
	public static class Baked implements IDynamicBakedModel {

		final Map<String, IBakedModel> partMap;
		final TextureAtlasSprite particle;
		final ItemCameraTransforms transforms;
		final boolean sideLit, gui3d, smoothLight;

		public Baked(
			Map<String, IBakedModel> partMap, boolean sideLit, boolean gui3d,
			boolean smoothLight, TextureAtlasSprite particle,
			ItemCameraTransforms transforms
		) {
			this.partMap = partMap;
			this.particle = particle;
			this.sideLit = sideLit;
			this.gui3d = gui3d;
			this.smoothLight = smoothLight;
			this.transforms = transforms;
		}

		@Override
		@SuppressWarnings("deprecation")
		public List<BakedQuad>
		getQuads(BlockState state, Direction side, Random rand, IModelData data) {
			List<PartModel> parts = data.getData(PartModel.PART_MODELS);
			IBakedModel jitModel = data.getData(JIT_BAKED_MODEL);
			if (parts == null)
				return jitModel == null ? Collections.emptyList()
					: jitModel.getQuads(state, side, rand);
			ArrayList<BakedQuad> quads = new ArrayList<>();
			for (PartModel part : parts) {
				IBakedModel m = partMap.get(part.name);
				if (m == null) continue;
				List<BakedQuad> list = m.getQuads(state, part.getSource(side), rand, data);
				if (!part.hasTransform())
					quads.addAll(list);
				else for (BakedQuad q : list)
					quads.add(part.transform(q));
			}
			if (jitModel != null)
				quads.addAll(jitModel.getQuads(state, side, rand));
			return quads;
		}

		@Override
		public IModelData getModelData(
			IBlockDisplayReader world, BlockPos pos, BlockState state, IModelData tileData
		) {
			IBakedModel model = tileData.getData(JIT_BAKED_MODEL);
			if (model != null)
				model.getModelData(world, pos, state, tileData);
			return tileData;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return smoothLight;
		}

		@Override
		public boolean isGui3d() {
			return gui3d;
		}

		@Override
		public boolean usesBlockLight() {
			return sideLit;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return particle;
		}

		@Override
		@SuppressWarnings("deprecation")
		public TextureAtlasSprite getParticleTexture(IModelData data) {
			IBakedModel model = data.getData(JIT_BAKED_MODEL);
			if (model != null) {
				TextureAtlasSprite tex = model.getParticleIcon();
				if (tex != null) return tex;
			}
			return getParticleIcon();
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ModelDataItemOverride.INSTANCE;
		}

		@Override
		public ItemCameraTransforms getTransforms() {
			return transforms;
		}

	}

	@OnlyIn(Dist.CLIENT)
	public static class Loader implements IModelLoader<TileEntityModel> {

		public static final Loader INSTANCE = new Loader();

		private ArrayList<Runnable> invalidateCaches = new ArrayList<>();

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager) {
			for (Runnable r : invalidateCaches) r.run();
		}

		@Override
		public TileEntityModel
		read(JsonDeserializationContext context, JsonObject modelContents) {
			List<Pair<String, IModelGeometry<?>>> parts;
			if (modelContents.has("parts")) {
				JsonObject obj = modelContents.get("parts").getAsJsonObject();
				parts = new ArrayList<>(obj.size());
				for (Entry<String, JsonElement> e : obj.entrySet()) {
					IModelGeometry<?> m;
					JsonElement v = e.getValue();
					if (v.isJsonArray())
						m = ScriptModel.Loader.INSTANCE.compileModel(
							new JsonArrayLineReader(v.getAsJsonArray()),
							e.getKey()
						);
					else m = new ModelRef(v.getAsString());
					parts.add(Pair.of(e.getKey(), m));
				}
			} else parts = Collections.emptyList();
			return new TileEntityModel(parts);
		}

	}

	/**@param cache a function to run when models reload */
	public static void registerCacheInvalidate(Runnable cache) {
		Loader.INSTANCE.invalidateCaches.add(cache);
	}

	public static final ModelDataMap.Builder MODEL_DATA_BUILDER = new ModelDataMap.Builder()
	.withProperty(PartModel.PART_MODELS)
	.withProperty(JIT_BAKED_MODEL);

}
