package cd4017be.lib;

import cd4017be.api.Capabilities;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.api.energy.EnergyAPI;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.item.ItemMaterial;
import cd4017be.lib.render.ItemMaterialMeshDefinition;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.templates.NBTRecipe;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.FileUtil;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.RecipeSorter;

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

	public static ItemMaterial materials;

	public static final TabMaterials creativeTab = new TabMaterials(ID);

	public Lib() {
		MinecraftForge.EVENT_BUS.register(this);
		RecipeScriptContext.scriptRegistry.add(new Version(ConfigName, 0, "/assets/" + ID + "/config/core.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		FileUtil.initConfigDir(event);
		BlockGuiHandler.register();
		Capabilities.register();
		RecipeSorter.register(ID + ":shapedNBT", NBTRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shaped before:minecraft:shapeless");
		RecipeScriptContext.instance = new RecipeScriptContext();
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
		if (event.getSide().isClient()) clientPostInit();
	}

	@SideOnly(Side.CLIENT)
	private static void clientPostInit() {
		SpecialModelLoader.setMod(ID);
		BlockItemRegistry.registerRender(materials);
		BlockItemRegistry.registerRender(materials, new ItemMaterialMeshDefinition(materials));
	}

	@Mod.EventHandler
	public void afterStart(FMLServerAboutToStartEvent event) {
		//trash stuff that's not needed anymore
		RecipeScriptContext.instance = null;
		System.gc();
	}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> ev) {
		ev.getRegistry().register((materials = new ItemMaterial("m")).setCreativeTab(creativeTab));
		creativeTab.item = new ItemStack(materials);
	}

}
