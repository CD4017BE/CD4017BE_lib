package cd4017be.lib.render;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.Level;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.FMLLog;

public class SpecialModelLoader implements ICustomModelLoader {

	public static final SpecialModelLoader instance = new SpecialModelLoader();
	public static final StateMapper stateMapper = new StateMapper();
	private static String mod = "";
	
	public static void setMod(String name) {
		mod = name;
		instance.mods.add(name);
	}
	
	public static void registerFluid(Fluid fluid) {
		ModelFluid model = new ModelFluid(fluid);
		instance.models.put(new ResourceLocation(mod, "models/block/" + fluid.getName()), model);
		ModelLoader.setCustomStateMapper(fluid.getBlock(), stateMapper);
	}
	
	public static void registerBlockModel(Block block, IModel model) {
		String[] name = block.getRegistryName().toString().split(":");
		instance.models.put(new ResourceLocation(name[0], "models/block/" + name[1]), model);
		//ModelLoader.setCustomStateMapper(block, stateMapper);
	}
	
	public static void registerTESRModel(String path) {
		instance.tesrRegistry.add(path);
	}
	
	public HashMap<ResourceLocation, IModel> models = new HashMap<ResourceLocation, IModel>();
	public HashSet<String> mods = new HashSet<String>();
	public HashSet<String> tesrRegistry = new HashSet<String>();
	private HashMap<ResourceLocation, String> tesrModelCode = new HashMap<ResourceLocation, String>();
	public HashMap<String, int[]> tesrModelData = new HashMap<String, int[]>();
	private IResourceManager resourceManager;
	
	private SpecialModelLoader() {
		ModelLoaderRegistry.registerLoader(this);
	}
	
	public String loadTESRModelSourceCode(ResourceLocation res) throws IOException {
		String code = tesrModelCode.get(res);
		if (code != null) return code;
		InputStreamReader isr = new InputStreamReader(resourceManager.getResource(res).getInputStream());
		String s = "";
		int n;
		char[] buff = new char[256];
		while((n = isr.read(buff)) > 0) s += String.valueOf(buff, 0, n);
		tesrModelCode.put(res, s);
		return s;
	}
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		tesrModelData.clear();
		for (String s : tesrRegistry) {
			try {
				ResourceLocation res = new ResourceLocation(s + ".tesr");
				String code = this.loadTESRModelSourceCode(res);
				tesrModelData.put(s, TESRModelParser.bake(code, res));
			} catch (Exception e) {
				FMLLog.log("cd4017be_lib", Level.ERROR, e, "unable to load TESR model %s :", s);
			}
		}
		tesrModelCode.clear();
	}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return mods.contains(modelLocation.getResourceDomain()) && 
				models.containsKey(modelLocation);
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws IOException {
		return models.get(modelLocation);
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
