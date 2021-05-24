package cd4017be.lib.config;

import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.fml.config.ModConfig.Type;

/**@author CD4017BE */
public class LibServer extends ModConfig {

	public final BooleanValue canCutBlocks, canReplicate;

	public LibServer() {
		super(Type.SERVER);
		Builder b = new Builder();
		b.push("assembler");
		canCutBlocks = b
		.comment("Enables the Assembler's Block cutting feature that allows building with Block Bits.")
		.define("block_cutting", true);
		canReplicate = b
		.comment("Enables the Assembler's Microblock Structure replication feature.")
		.define("replicating", true);
		b.pop();
		finish(b);
	}

}
