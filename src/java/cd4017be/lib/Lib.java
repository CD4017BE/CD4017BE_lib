package cd4017be.lib;

import org.apache.logging.log4j.Logger;

import cd4017be.api.Capabilities;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.api.energy.EnergyAPI;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.item.BaseItem;
import cd4017be.lib.item.ItemMaterial;
import cd4017be.lib.render.ItemMaterialMeshDefinition;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.FileUtil;
import cd4017be.lib.util.TooltipEditor;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
@Mod(modid = Lib.ID, useMetadata = true)
public class Lib {

	public static final String ID = "cd4017be_lib";
	public static final String ConfigName = "core";

	@Instance
	public static Lib instance;

	public static Logger LOG;

	public static ItemMaterial materials;
	public static BaseItem rrwi;

	public static final TabMaterials creativeTab = new TabMaterials(ID);

	public Lib() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOG = event.getModLog();
		FileUtil.initConfigDir(event);
		BlockGuiHandler.register();
		Capabilities.register();
		rrwi = new BaseItem("rrwi");
		(materials = new ItemMaterial("m")).setCreativeTab(creativeTab);
		creativeTab.item = new ItemStack(materials);
		//RecipeSorter.register(ID + ":shapedNBT", NBTRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shaped before:minecraft:shapeless");
		RecipeScriptContext.instance = new RecipeScriptContext(LOG);
		RecipeScriptContext.instance.setup();
		RecipeScriptContext.instance.run(ConfigName + ".PRE_INIT");
		ConfigConstants cfg = new ConfigConstants(RecipeScriptContext.instance.modules.get(ConfigName));
		EnergyAPI.init(cfg);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		ComputerAPI.register();
		RecipeScriptContext.instance.runAll("INIT");
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		RecipeScriptContext.instance.runAll("POST_INIT");
		TooltipUtil.addScriptVariables();
		if (event.getSide().isClient()) TooltipEditor.init();
	}

	@Mod.EventHandler
	public void afterStart(FMLServerAboutToStartEvent event) {
		//trash stuff that's not needed anymore
		RecipeScriptContext.instance = null;
		System.gc();
	}

	@Mod.EventHandler
	public void onShutdown(FMLServerStoppingEvent event) {
		TickRegistry.instance.clear();
		if (TooltipUtil.editor != null)
			TooltipUtil.editor.save();
	}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> ev) {
		ev.getRegistry().registerAll(rrwi, materials);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void registerMaterialModels(ModelRegistryEvent ev) {
		SpecialModelLoader.setMod(ID);
		BlockItemRegistry.registerRender(rrwi);
		BlockItemRegistry.registerRender(materials);
		BlockItemRegistry.registerRender(materials, new ItemMaterialMeshDefinition(materials));
	}

}
