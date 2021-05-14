package cd4017be.lib.config;

import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.fml.config.ModConfig.Type;

public class LibClient extends ModConfig {

	public final ConfigValue<String> tooltipEditPath;
	public final BooleanValue tooltipEditEnable;
	public final BooleanValue shiftForMainTooltip;
	public final IntValue itemModelCache;

	public LibClient() {
		super(Type.CLIENT);
		Builder b = new Builder();
		shiftForMainTooltip = b
		.comment("Whether the item's main tooltip requires holding down shift.")
		.define("info_need_shift", false);
		
		itemModelCache = b
		.comment("Maximum cache size for complex nbt dependent item models.")
		.defineInRange("item_model_cache", 64, 0, 65536);
		
		b.comment("The ingame localization editor allows writing translations for my mods by pressing F4 in game.")
		.push("lang_editor");
		tooltipEditPath = b
		.comment("The relative path from game dir to store the generated localization files.")
		.define("path", "../../src/resources/assets/cd4017be_lib/lang");
		tooltipEditEnable = b
		.comment("Set true to enable the ingame localization editor.")
		.worldRestart().define("enable", false);
		b.pop();
		
		finish(b);
	}

}
