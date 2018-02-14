package cd4017be.lib.render;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import cd4017be.lib.render.model.IntArrayModel;
import cd4017be.lib.render.model.ModelContext;
import cd4017be.lib.render.model.ModelVariant;
import cd4017be.lib.render.model.NBTModel;
import cd4017be.lib.render.model.ParamertisedVariant;
import cd4017be.lib.render.model.RawModelData;
import cd4017be.lib.render.model.TextureReplacement;
import cd4017be.lib.script.Module;
import cd4017be.lib.script.Script;
import cd4017be.lib.util.Orientation;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 *
 */
@SideOnly(Side.CLIENT)
public class SpecialModelLoader implements ICustomModelLoader {

	public static final String SCRIPT_PREFIX = "models/block/_", NBT_PREFIX = "models/block/.", NBT_PREFIX_IT = "models/item/.";
	public static final SpecialModelLoader instance = new SpecialModelLoader();
	public static final StateMapper stateMapper = new StateMapper();
	private static String mod = "";

	public static void setMod(String name) {
		mod = name;
		instance.mods.add(name);
	}

	public static void registerFluid(Fluid fluid) {
		Block block = fluid.getBlock();
		if (block == null || !mod.equals(block.getRegistryName().getResourceDomain())) return;
		ModelFluid model = new ModelFluid(fluid);
		instance.models.put(new ResourceLocation(mod, "models/block/" + fluid.getName()), model);
		ModelLoader.setCustomStateMapper(fluid.getBlock(), stateMapper);
	}

	public static void registerBlockModel(Block block, IModel model) {
		String[] name = block.getRegistryName().toString().split(":");
		instance.models.put(new ResourceLocation(name[0], "models/block/" + name[1]), model);
	}

	public static void registerItemModel(Item item, IModel model) {
		String[] name = item.getRegistryName().toString().split(":");
		instance.models.put(new ResourceLocation(name[0], "models/item/" + name[1]), model);
	}

	public static <T extends TileEntity> void registerTESR(Class<T> tile, TileEntitySpecialRenderer<T> tesr) {
		ClientRegistry.bindTileEntitySpecialRenderer(tile, tesr);
		if (tesr instanceof IModeledTESR) instance.tesrs.add((IModeledTESR)tesr);
	}

	private IResourceManager resourceManager;
	private HashMap<String, ModelContext> scriptModels = new HashMap<String, ModelContext>();
	public HashMap<ResourceLocation, IModel> models = new HashMap<ResourceLocation, IModel>();
	public HashSet<String> mods = new HashSet<String>();
	public ArrayList<IModeledTESR> tesrs = new ArrayList<IModeledTESR>();

	private SpecialModelLoader() {
		ModelLoaderRegistry.registerLoader(this);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void bakeModels(ModelBakeEvent event) {
		for (IModeledTESR tesr : tesrs) tesr.bakeModels(resourceManager);
		for (ModelContext cont : scriptModels.values())
			for (Iterator<Entry<String, Module>> it = cont.modules.entrySet().iterator(); it.hasNext();) {
				Entry<String, Module> e = it.next();
				Module m = e.getValue();
				if (m instanceof Script && !e.getKey().startsWith("tesr.")) it.remove();
			}
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		for (Iterator<IModel> it = models.values().iterator(); it.hasNext();) {
			IModel m = it.next();
			if (m instanceof IHardCodedModel) ((IHardCodedModel)m).onReload();
			else it.remove();
		}
		scriptModels.clear();
	}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		if (mods.contains(modelLocation.getResourceDomain())) {
			String path = modelLocation.getResourcePath();
			return path.startsWith(SCRIPT_PREFIX) || path.startsWith(NBT_PREFIX) || path.startsWith(NBT_PREFIX_IT) || models.containsKey(modelLocation);
		} else return false;
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws Exception {
		IModel model = models.get(modelLocation);
		if (model != null) return model;
		String path = modelLocation.getResourcePath();
		int p;
		if ((p = path.indexOf('$')) >= 0) {
			model = loadModel(new ResourceLocation(modelLocation.getResourceDomain(), path.substring(0, p)));
			model = new TextureReplacement(model, new ResourceLocation(path.substring(p + 1)));
		} else if (path.startsWith(NBT_PREFIX) || path.startsWith(NBT_PREFIX_IT)) {
			ParamertisedVariant v = ParamertisedVariant.parse(path);
			String filePath = v.splitPath();
			if (v.isBase())
				model = new NBTModel(CompressedStreamTools.read(new DataInputStream(resourceManager.getResource(new ResourceLocation(modelLocation.getResourceDomain(), filePath.replaceAll("\\.", "") + ".nbt")).getInputStream())));
			else
				model = new ModelVariant(loadModel(new ResourceLocation(modelLocation.getResourceDomain(), filePath)), v);
		} else if ((p = path.indexOf('#')) >= 0) {
			String s = path.substring(p + 1);
			Orientation o = Orientation.valueOf(s.substring(0, 1).toUpperCase() + s.substring(1));
			model = loadModel(new ResourceLocation(modelLocation.getResourceDomain(), path.substring(0, p)));
			model = new ModelVariant(model, o.getModelRotation());
		} else if (path.startsWith(SCRIPT_PREFIX))
			model = loadScriptModel(modelLocation);
		if (model != null) models.put(modelLocation, model);
		return model;
	}

	private IModel loadScriptModel(ResourceLocation modelLocation) throws Exception {
		String domain = modelLocation.getResourceDomain();
		String scriptName = modelLocation.getResourcePath().substring(SCRIPT_PREFIX.length());
		int p = scriptName.indexOf('.');
		String methodName;
		if (p >= 0) {
			methodName = scriptName.substring(p + 1);
			scriptName = scriptName.substring(0, p);
		} else methodName = "main()";
		
		ModelContext cont = scriptModels.get(domain);
		if (cont == null) {
			scriptModels.put(domain, cont = new ModelContext(new ResourceLocation(domain, "models/block/")));
		}
		Module script = cont.getOrLoad(scriptName, resourceManager);
		cont.run(script, methodName);
		return new RawModelData(script, cont);
	}

	public static IntArrayModel loadTESRModel(String domain, String name) throws Exception {
		int p = name.indexOf('.');
		String methodName;
		if (p >= 0) {
			methodName = name.substring(p + 1);
			name = name.substring(0, p);
		} else methodName = "main()";
		ModelContext cont = instance.scriptModels.get(domain);
		if (cont == null) {
			instance.scriptModels.put(domain, cont = new ModelContext(new ResourceLocation(domain, "models/block/")));
		}
		Module script = cont.getOrLoad("tesr." + name, instance.resourceManager);
		cont.run(script, methodName);
		return new IntArrayModel(cont, IntArrayModel.getTextures(script));
	}

	public static class StateMapper implements IStateMapper {
		@Override
		public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block block) {
			HashMap<IBlockState, ModelResourceLocation> map = new HashMap<IBlockState, ModelResourceLocation>();
			ModelResourceLocation loc = new ModelResourceLocation(block.getRegistryName(), "normal");
			for (IBlockState state : block.getBlockState().getValidStates()) map.put(state, loc);
			return map;
		}
	}

}
