package cd4017be.lib.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;


public class EditValueScreen<T> extends Screen {

	final Screen parent;
	final ConfigValue<T> value;
	final Class<T> type;
	final ITextComponent info;
	TextFieldWidget edit;

	@SuppressWarnings("unchecked")
	protected EditValueScreen(Screen parent, ValueSpec vspec, ConfigValue<T> value, String name) {
		super(new StringTextComponent(name));
		this.parent = parent;
		this.value = value;
		this.type = (Class<T>)vspec.getClazz();
		String info = vspec.getComment();
		this.info = info == null ? null : new StringTextComponent(info);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@SuppressWarnings("unchecked")
	public void setValue() {
		String s = edit.getValue();
		try {
			T val;
			if (type == Integer.class)
				val = (T)Integer.valueOf(s);
			else if (type == Long.class)
				val = (T)Long.valueOf(s);
			else if (type == Double.class)
				val = (T)Double.valueOf(s);
			else if (type == String.class)
				val = (T)s;
			else return; //TODO List & Enum
			value.set(val);
			onClose();
		} catch (NumberFormatException e) {
			edit.setValue(value.get().toString());
		}
	}

	@Override
	protected void init() {
		edit = addButton(new TextFieldWidget(font, width / 2 - 108, height / 2, 216, 20, title));
		edit.setValue(value.get().toString());
		addButton(new Button(
			width / 2 - 108, height / 2 + 40, 100, 20, DialogTexts.GUI_CANCEL, b -> onClose()
		));
		addButton(new Button(
			width / 2  + 8, height / 2 + 40, 100, 20, DialogTexts.GUI_DONE, b -> setValue()
		));
	}

	@Override
	public void render(MatrixStack ms, int mx, int my, float t) {
		renderBackground(ms);
		font.draw(ms, title, (width - font.width(title)) / 2, 10, -1);
		int x = width / 2 - 120, y = 30;
		for (IReorderingProcessor rop : font.split(info, 240))
			font.draw(ms, rop, x, y += font.lineHeight, 0xffc0c0c0);
		super.render(ms, mx, my, t);
	}

}
