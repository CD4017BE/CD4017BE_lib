package cd4017be.lib.render;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;

import cd4017be.lib.event.ModTextureStitchEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;


/**
 * A non vanilla {@link TextureMap}.
 * The only difference is that this one fires {@link ModTextureStitchEvent} instead of the regular {@link TextureStitchEvent} to not screw up bad mods.
 * @author CD4017BE
 */
public class ModTextureMap extends TextureMap {

	private static final Logger LOGGER = LogManager.getLogger();
	private final java.util.Deque<ResourceLocation> loadingSprites = new java.util.ArrayDeque<>();
	private final java.util.Set<ResourceLocation> loadedSprites = new java.util.HashSet<>();

	/**
	 * @param basePath
	 * @param iconCreator
	 */
	public ModTextureMap(String basePath, @Nullable ITextureMapPopulator iconCreator) {
		super(basePath, iconCreator);
	}

	@Override
	public void loadSprites(IResourceManager resourceManager, ITextureMapPopulator iconCreatorIn) {
		mapRegisteredSprites.clear();
		MinecraftForge.EVENT_BUS.post(new ModTextureStitchEvent.Pre(this));
		iconCreatorIn.registerSprites(this);
		initMissingImage();
		deleteGlTexture();
		loadTextureAtlas(resourceManager);
	}

	@Override
	public void loadTextureAtlas(IResourceManager resourceManager) {
		int i = Minecraft.getGLMaximumTextureSize();
		Stitcher stitcher = new Stitcher(i, i, 0, getMipmapLevels());
		mapUploadedSprites.clear();
		listAnimatedSprites.clear();
		int j = Integer.MAX_VALUE;
		int k = 1 << getMipmapLevels();
		FMLLog.log.info("Max texture size: {}", i);
		ProgressBar bar = ProgressManager.push("Texture stitching", mapRegisteredSprites.size());
		loadedSprites.clear();

		for (Entry<String, TextureAtlasSprite> entry : mapRegisteredSprites.entrySet()) {
			final ResourceLocation location = new ResourceLocation(entry.getKey());
			bar.step(location.toString());
			j = loadTexture(stitcher, resourceManager, location, entry.getValue(), bar, j, k);
		}
		
		ProgressManager.pop(bar);
		
		missingImage.generateMipmaps(getMipmapLevels());
		stitcher.addSprite(missingImage);
		bar = ProgressManager.push("Texture creation", 2);
		
		bar.step("Stitching");
		stitcher.doStitch();
		
		LOGGER.info("Created: {}x{} {}-atlas", Integer.valueOf(stitcher.getCurrentWidth()), Integer.valueOf(stitcher.getCurrentHeight()), getBasePath());
		bar.step("Allocating GL texture");
		TextureUtil.allocateTextureImpl(getGlTextureId(), getMipmapLevels(), stitcher.getCurrentWidth(), stitcher.getCurrentHeight());
		Map<String, TextureAtlasSprite> map = Maps.<String, TextureAtlasSprite>newHashMap(mapRegisteredSprites);
		
		ProgressManager.pop(bar);
		bar = ProgressManager.push("Texture mipmap and upload", stitcher.getStichSlots().size());
		
		for (TextureAtlasSprite textureatlassprite1 : stitcher.getStichSlots()) {
			bar.step(textureatlassprite1.getIconName());
			String s = textureatlassprite1.getIconName();
			map.remove(s);
			mapUploadedSprites.put(s, textureatlassprite1);
			
			try {
				TextureUtil.uploadTextureMipmap(textureatlassprite1.getFrameTextureData(0), textureatlassprite1.getIconWidth(), textureatlassprite1.getIconHeight(), textureatlassprite1.getOriginX(), textureatlassprite1.getOriginY(), false, false);
			} catch (Throwable throwable) {
				CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Stitching texture atlas");
				CrashReportCategory crashreportcategory = crashreport.makeCategory("Texture being stitched together");
				crashreportcategory.addCrashSection("Atlas path", getBasePath());
				crashreportcategory.addCrashSection("Sprite", textureatlassprite1);
				throw new ReportedException(crashreport);
			}
			
			if (textureatlassprite1.hasAnimationMetadata())
				listAnimatedSprites.add(textureatlassprite1);
		}
		
		for (TextureAtlasSprite textureatlassprite2 : map.values())
			textureatlassprite2.copyFrom(missingImage);
		
		MinecraftForge.EVENT_BUS.post(new ModTextureStitchEvent.Post(this));
		ProgressManager.pop(bar);
	}

	private int loadTexture(Stitcher stitcher, IResourceManager resourceManager, ResourceLocation location, TextureAtlasSprite textureatlassprite, net.minecraftforge.fml.common.ProgressManager.ProgressBar bar, int j, int k) {
		if (loadedSprites.contains(location)) return j;
		ResourceLocation resourcelocation = getResourceLocation(textureatlassprite);
		IResource iresource = null;
		
		for(ResourceLocation loading : loadingSprites) {
			if(location.equals(loading)) {
				final String error = "circular model dependencies, stack: [" + com.google.common.base.Joiner.on(", ").join(loadingSprites) + "]";
				FMLClientHandler.instance().trackBrokenTexture(resourcelocation, error);
			}
		}
		loadingSprites.addLast(location);
		try {
			for (ResourceLocation dependency : textureatlassprite.getDependencies()) {
				if (!mapRegisteredSprites.containsKey(dependency.toString()))
					registerSprite(dependency);
				TextureAtlasSprite depSprite = mapRegisteredSprites.get(dependency.toString());
				j = loadTexture(stitcher, resourceManager, dependency, depSprite, bar, j, k);
			}
			if (textureatlassprite.hasCustomLoader(resourceManager, resourcelocation)) {
				if (textureatlassprite.load(resourceManager, resourcelocation, l -> mapRegisteredSprites.get(l.toString())))
					return j;
			} else try {
				PngSizeInfo pngsizeinfo = PngSizeInfo.makeFromResource(resourceManager.getResource(resourcelocation));
				iresource = resourceManager.getResource(resourcelocation);
				boolean flag = iresource.getMetadata("animation") != null;
				textureatlassprite.loadSprite(pngsizeinfo, flag);
			} catch (RuntimeException runtimeexception) {
				FMLClientHandler.instance().trackBrokenTexture(resourcelocation, runtimeexception.getMessage());
				return j;
			} catch (IOException ioexception) {
				FMLClientHandler.instance().trackMissingTexture(resourcelocation);
				return j;
			} finally {
				IOUtils.closeQuietly((Closeable)iresource);
			}
			
			j = Math.min(j, Math.min(textureatlassprite.getIconWidth(), textureatlassprite.getIconHeight()));
			int j1 = Math.min(Integer.lowestOneBit(textureatlassprite.getIconWidth()), Integer.lowestOneBit(textureatlassprite.getIconHeight()));
			
			if (j1 < k) {
				// FORGE: do not lower the mipmap level, just log the problematic textures
				LOGGER.warn("Texture {} with size {}x{} will have visual artifacts at mip level {}, it can only support level {}. Please report to the mod author that the texture should be some multiple of 16x16.", resourcelocation, Integer.valueOf(textureatlassprite.getIconWidth()), Integer.valueOf(textureatlassprite.getIconHeight()), Integer.valueOf(MathHelper.log2(k)), Integer.valueOf(MathHelper.log2(j1)));
			}
			
			if (generateMipmaps(resourceManager, textureatlassprite))
				stitcher.addSprite(textureatlassprite);
			return j;
		} finally {
			loadingSprites.removeLast();
			loadedSprites.add(location);
		}
	}

}
