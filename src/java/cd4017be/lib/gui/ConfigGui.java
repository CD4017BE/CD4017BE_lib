package cd4017be.lib.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnTooltip;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public class ConfigGui extends Screen {

	final Screen parent;
	final ForgeConfigSpec spec;
	final Object[] entries;

	public ConfigGui(Screen parent, Component title, ForgeConfigSpec spec) {
		this(parent, title, spec, spec.getValues());
	}

	public ConfigGui(
		Screen parent, Component title,
		ForgeConfigSpec spec, UnmodifiableConfig config
	) {
		super(title);
		this.parent = parent;
		this.spec = spec;
		this.entries = config.entrySet().toArray();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	protected void init() {
		int i = 0, y = 40;
		for (int x = width / 2 - 208; i < entries.length - 1; i+=2, y+=25) {
			add(x, y, (Entry)entries[i]);
			add(x + 216, y, (Entry)entries[i+1]);
		}
		int x = width / 2 - 100;
		if (i < entries.length) add(x - 108, y, (Entry)entries[i]);
		addRenderableWidget(new Button(
			x, height - 27, 200, 20, CommonComponents.GUI_DONE, b -> onClose()
		));
	}

	@Override
	public void
	render(PoseStack ms, int mx, int my, float t) {
		renderBackground(ms);
		font.draw(ms, title, (width - font.width(title)) / 2, 10, -1);
		super.render(ms, mx, my, t);
	}

	private void add(int x, int y, Entry e) {
		String key = e.getKey();
		Object obj = e.getValue();
		if (obj instanceof UnmodifiableConfig) {
			UnmodifiableConfig cfg = (UnmodifiableConfig)obj;
			Component title = new TextComponent(key + " ->");
			addRenderableWidget(new Button(x, y, 200, 20, title,
				b -> minecraft.setScreen(new ConfigGui(this, title, spec, cfg))
			));
			return;
		}
		if (!(obj instanceof ConfigValue)) return;
		ConfigValue<?> cv = (ConfigValue<?>)obj;
		ValueSpec vspec = spec.get(cv.getPath());
		Style style = Style.EMPTY;
		if (vspec.needsWorldRestart())
			style = style.withColor(TextColor.fromRgb(0xffff0000));
		OnTooltip tooltip = Button.NO_TOOLTIP;
		if (vspec.getComment() != null) {
			Component text = new TextComponent(vspec.getComment());
			tooltip = (b, ms, mx, my)-> renderTooltip(ms, text, mx, my);
		}
		if (obj instanceof BooleanValue) {
			BooleanValue bv = (BooleanValue)obj;
			Component yes = new TextComponent(key + " = true").setStyle(style);
			Component no = new TextComponent(key + " = false").setStyle(style);
			addRenderableWidget(new Button(x, y, 200, 20, bv.get() ? yes : no, b -> {
				boolean state = !bv.get();
				bv.set(state);
				b.setMessage(state ? yes : no);
			}, tooltip));
			return;
		}
		Component title = new TextComponent(
			font.plainSubstrByWidth(key + " = " + cv.get(), 198)
		).setStyle(style);
		addRenderableWidget(new Button(x, y, 200, 20, title,
			b -> minecraft.setScreen(new EditValueScreen<>(this, vspec, cv, key))
		, tooltip));
	}

}
