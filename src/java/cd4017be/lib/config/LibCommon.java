package cd4017be.lib.config;

import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.fml.config.ModConfig.Type;

public class LibCommon extends ModConfig {

	public final IntValue packet_chain_threshold;

	public LibCommon() {
		super(Type.COMMON);
		Builder b = new Builder();
		packet_chain_threshold = b
		.comment("Network packets send within the same tick are combined if smaller that this amount of bytes.")
		.defineInRange("packet_chain_threshold", 255, 0, 255);
		finish(b);
	}

}
