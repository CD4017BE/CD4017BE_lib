package cd4017be.lib.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**Simplifies definition and registration of forge config files.<br>
 * In overriding constructor define config entries using {@link ForgeConfigSpec.Builder} and finally call {@link #finish()}.<br>
 * Later use {@link #register(String, Type)} for registration.
 * @author CD4017BE */
public class Config {

	public final Type cfgType;
	private ForgeConfigSpec spec;

	protected Config(Type type) {
		this.cfgType = type;
	}

	protected void finish(ForgeConfigSpec.Builder builder) {
		this.spec = builder.build();
	}

	public void register(String name) {
		ModLoadingContext.get().registerConfig(cfgType, spec, name + "-" + cfgType.name().toLowerCase() + ".toml");
	}

}
