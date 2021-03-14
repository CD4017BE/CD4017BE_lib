package cd4017be.lib.render.model;

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
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;

/**A Block Model that lets the TileEntity control what to render,
 * including raw BakedQuads via {@link ExtraQuadsProperty}
 * and/or part models via {@link PartModel}.<br>
 * 
 * JSON Format:<br>
 * "textures": {
 *     "particle": "texture:path"
 * },
 * "loader": "cd4017be_lib:te",
 * "parts": {
 *     "name": "model:path",
 *     ...
 * }
 * @author CD4017BE */
public class TileEntityModel implements IModelGeometry<TileEntityModel> {

	private final List<Pair<String, ResourceLocation>> partModels;

	public TileEntityModel(List<Pair<String, ResourceLocation>> partModels) {
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
		for (Pair<String, ResourceLocation> e : partModels)
			map.put(e.getLeft(), bakery.getBakedModel(e.getRight(), modelTransform, spriteGetter));
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
		set.add(owner.resolveTexture("particle"));
		for (Pair<String, ResourceLocation> e : partModels)
			set.addAll(modelGetter.apply(e.getRight()).getTextures(modelGetter, missingTextureErrors));
		return Collections.singletonList(null);
	}


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
		public List<BakedQuad>
		getQuads(BlockState state, Direction side, Random rand, IModelData data) {
			List<PartModel> parts = data.getData(PartModel.PART_MODELS);
			List<BakedQuad> quads = data.getData(ExtraQuadsProperty.of(side));
			if (parts == null) return quads == null ? Collections.emptyList() : quads;
			quads = new ArrayList<>(quads);
			for (PartModel part : parts) {
				IBakedModel m = partMap.get(part.name);
				if (m == null) continue;
				List<BakedQuad> list = m.getQuads(state, part.getSource(side), rand, data);
				if (!part.hasTransform())
					quads.addAll(list);
				else for (BakedQuad q : list)
					quads.add(part.transform(q));
			}
			return quads;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return smoothLight;
		}

		@Override
		public boolean isGui3d() {
			return gui3d;
		}

		@Override
		public boolean isSideLit() {
			return sideLit;
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
		public ItemOverrideList getOverrides() {
			return ModelDataItemOverride.INSTANCE;
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return transforms;
		}

	}


	public static class Loader implements IModelLoader<TileEntityModel> {

		public static final Loader INSTANCE = new Loader();

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager) {
		}

		@Override
		public TileEntityModel
		read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
			List<Pair<String, ResourceLocation>> parts;
			if (modelContents.has("parts")) {
				JsonObject obj = modelContents.get("parts").getAsJsonObject();
				parts = new ArrayList<>(obj.size());
				for (Entry<String, JsonElement> e : obj.entrySet())
					parts.add(Pair.of(e.getKey(), new ResourceLocation(e.getValue().getAsString())));
			} else parts = Collections.emptyList();
			return new TileEntityModel(parts);
		}

	}

}
