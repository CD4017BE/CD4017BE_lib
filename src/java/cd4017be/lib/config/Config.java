package cd4017be.lib.config;

import java.io.File;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.loading.FMLPaths;

/**Simplifies definition and registration of forge config files.<br>
 * In overriding constructor define config entries using {@link ForgeConfigSpec.Builder} and finally call {@link #finish()}.<br>
 * Later use {@link #register(String, Type)} for registration.
 * @author CD4017BE */
public class Config {

	public static final File CONFIG_DIR;
	static {
		CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("cd4017be").toFile();
		//Workaround for config file creation failing in missing sub-directories
		CONFIG_DIR.mkdirs();
	}

	public final Type cfgType;
	private ForgeConfigSpec spec;

	protected Config(Type type) {
		this.cfgType = type;
	}

	protected void finish(ForgeConfigSpec.Builder builder) {
		this.spec = builder.build();
	}

	public void register(String name) {
		ModLoadingContext.get().registerConfig(cfgType, spec, "cd4017be/" + name + "-" + cfgType.name().toLowerCase() + ".toml");
	}

}
