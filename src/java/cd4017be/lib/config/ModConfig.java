package cd4017be.lib.config;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import cd4017be.lib.gui.ConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**Simplifies definition and registration of forge config files.<br>
 * In overriding constructor define config entries using {@link ForgeConfigSpec.Builder} and finally call {@link #finish()}.<br>
 * Later use {@link #register(String, Type)} for registration.
 * @author CD4017BE */
public class ModConfig implements Supplier<BiFunction<Minecraft, Screen, Screen>>{

	public final Type cfgType;
	public ForgeConfigSpec spec;

	protected ModConfig(Type type) {
		this.cfgType = type;
	}

	protected void finish(ForgeConfigSpec.Builder builder) {
		this.spec = builder.build();
	}

	public void register(String name) {
		ModLoadingContext.get().registerConfig(cfgType, spec, name + "-" + cfgType.name().toLowerCase() + ".toml");
	}

	@Override
	public BiFunction<Minecraft, Screen, Screen> get() {
		return spec.isLoaded() ? this::openGui : null;
	}

	@OnlyIn(Dist.CLIENT)
	public Screen openGui(Minecraft mc, Screen parent) {
		if (!spec.isLoaded()) return parent;
		return new ConfigGui(parent, new StringTextComponent(cfgType.name()), spec);
	}

}
