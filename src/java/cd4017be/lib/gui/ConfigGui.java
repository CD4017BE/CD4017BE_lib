package cd4017be.lib.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.Button.ITooltip;
import net.minecraft.util.text.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public class ConfigGui extends Screen {

	final Screen parent;
	final ForgeConfigSpec spec;
	final Object[] entries;

	public ConfigGui(Screen parent, ITextComponent title, ForgeConfigSpec spec) {
		this(parent, title, spec, spec.getValues());
	}

	public ConfigGui(
		Screen parent, ITextComponent title,
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
		addButton(new Button(
			x, height - 27, 200, 20, DialogTexts.GUI_DONE, b -> onClose()
		));
	}

	@Override
	public void
	render(MatrixStack ms, int mx, int my, float t) {
		renderBackground(ms);
		font.draw(ms, title, (width - font.width(title)) / 2, 10, -1);
		super.render(ms, mx, my, t);
	}

	private void add(int x, int y, Entry e) {
		String key = e.getKey();
		Object obj = e.getValue();
		if (obj instanceof UnmodifiableConfig) {
			UnmodifiableConfig cfg = (UnmodifiableConfig)obj;
			ITextComponent title = new StringTextComponent(key + " ->");
			addButton(new Button(x, y, 200, 20, title,
				b -> minecraft.setScreen(new ConfigGui(this, title, spec, cfg))
			));
			return;
		}
		if (!(obj instanceof ConfigValue)) return;
		ConfigValue<?> cv = (ConfigValue<?>)obj;
		ValueSpec vspec = spec.get(cv.getPath());
		Style style = Style.EMPTY;
		if (vspec.needsWorldRestart())
			style = style.withColor(Color.fromRgb(0xffff0000));
		ITooltip tooltip = Button.NO_TOOLTIP;
		if (vspec.getComment() != null) {
			ITextComponent text = new StringTextComponent(vspec.getComment());
			tooltip = (b, ms, mx, my)-> renderTooltip(ms, text, mx, my);
		}
		if (obj instanceof BooleanValue) {
			BooleanValue bv = (BooleanValue)obj;
			ITextComponent yes = new StringTextComponent(key + " = true").setStyle(style);
			ITextComponent no = new StringTextComponent(key + " = false").setStyle(style);
			addButton(new Button(x, y, 200, 20, bv.get() ? yes : no, b -> {
				boolean state = !bv.get();
				bv.set(state);
				b.setMessage(state ? yes : no);
			}, tooltip));
			return;
		}
		ITextComponent title = new StringTextComponent(
			font.plainSubstrByWidth(key + " = " + cv.get(), 198)
		).setStyle(style);
		addButton(new Button(x, y, 200, 20, title,
			b -> minecraft.setScreen(new EditValueScreen<>(this, vspec, cv, key))
		, tooltip));
	}

}
