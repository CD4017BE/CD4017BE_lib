package cd4017be.lib.gui.comp;

import static org.lwjgl.glfw.GLFW.*;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.client.gui.GuiUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import cd4017be.lib.text.TooltipUtil;

/**
 * A text field
 * @author CD4017BE
 *
 */
public class TextField extends Tooltip {

	private final Supplier<String> get;
	private final Consumer<String> set;
	public final int maxL;
	public int tc = 0xff404040, cc = 0xff800000;
	public String text = "";
	public int cur = 0;
	public boolean allowFormat = false, multiline = false;

	/**@param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param max maximum number of characters
	 * @param get text supplier function
	 * @param set text consumer function */
	public TextField(GuiCompGroup parent, int w, int h, int x, int y, int max, @Nonnull Supplier<String> get, @Nonnull Consumer<String> set) {
		super(parent, w, h, x, y, null, null);
		this.maxL = max;
		this.get = get;
		this.set = set;
	}

	/**changes the text and cursor color (default: dark grey & red)
	 * @param text text color in {@code 0xAARRGGBB} format
	 * @param cursor cursor color in {@code 0xAARRGGBB} format
	 * @return this */
	public TextField color(int text, int cursor) {
		this.tc = text; this.cc = cursor;
		return this;
	}

	/**specifies that minecraft's text formatting code prefix
	 * character ({@code '§'}) should be allowed in this text field.
	 * @return this */
	public TextField allowFormat() {
		this.allowFormat = true;
		return this;
	}

	/**Enables editing multiple lines separated by '\n'
	 * @return this */
	public TextField multiline() {
		this.multiline = true;
		return this;
	}

	@Override
	public void drawBackground(MatrixStack stack, int mx, int my, float ft) {
		parent.bindTexture(null);
		FontRenderer fr = parent.fontRenderer;
		boolean focused = focused();
		if (!focused) {
			text = get.get();
			if (text == null) text = "";
		}
		String[] lines = multiline ? text.split("\n", -1) : new String[] {text};
		int l1 = lines.length, ln = Math.min(Math.max(h / fr.lineHeight, 1), l1);
		int y = this.y + (h - fr.lineHeight * ln >> 1) + 1;
		if (focused()) {
			if (cur > text.length()) cur = text.length();
			int row = 0, col = cur, l0 = 0, ofs = 0;
			String t;
			if (multiline) {
				for (String line : lines) {
					int l = line.length();
					if (col <= l) break;
					col -= l + 1;
					row++;
				}
				t = lines[row];
				if (l1 > ln)
					if (l1 - row <= ln/2) l0 = l1 - ln;
					else if (row > ln/2) l0 = row - ln/2;
			} else t = text;
			int l = fr.width(t);
			int k = fr.width(t.substring(0, col));
			if (l > w)
				if (l - k < w/2) ofs = l - w;
				else if (k > w/2) ofs = k - w/2;
			k += x - ofs;
			int y1 = y + fr.lineHeight * (row - l0);
			GuiUtils.drawGradientRect(stack.last().pose(), (int)parent.zLevel, k - 1, y1, k, y1 + 7, cc, cc);
			for (int i = 0; i < ln; i++, y += fr.lineHeight) {
				t = fr.plainSubstrByWidth(lines[i + l0], w + ofs);
				int x = this.x;
				if (ofs > 0) {
					String t0 = t;
					int w = fr.width(t) - ofs;
					t = w <= 0 ? "" : fr.plainSubstrByWidth(t, w, true);
					x += fr.width(t0.substring(0, t0.length() - t.length())) - ofs;
				}
				fr.draw(stack, t, x, y, tc);
			}
		} else for (int i = 0; i < ln; i++, y += fr.lineHeight)
			fr.draw(stack, fr.plainSubstrByWidth(lines[i], w), x, y, tc);
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		try {
			boolean shift = Screen.hasShiftDown();
			boolean ctr = Screen.hasControlDown() && !shift && !Screen.hasAltDown();
			switch(k) {
			case GLFW_KEY_LEFT: if (cur > 0) cur--; break;
			case GLFW_KEY_RIGHT: if (cur < text.length()) cur++; break;
			case GLFW_KEY_DELETE:
				if (cur < text.length()) {
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} break;
			case GLFW_KEY_BACKSPACE:
				if (cur > 0) {
					cur--;
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} break;
			case GLFW_KEY_UP:
				if (multiline)
					cur = Math.max(text.lastIndexOf('\n', cur - 1), 0);
				else parent.focusNext(TextField.class);
				break;
			case GLFW_KEY_DOWN:
				if (multiline) {
					cur = text.indexOf('\n', cur + 1);
					if (cur < 0) cur = text.length();
				} else parent.focusPrev(TextField.class);
				break;
			case GLFW_KEY_ENTER:
				if (!(multiline && shift)) {
					parent.setFocus(null);
					break;
				}
				c = '\n';
			case GLFW_KEY_C: if (ctr) {
					TooltipUtil.setClipboardString(text);
					break;
				}
			case GLFW_KEY_V: if (ctr) {
					String s = TooltipUtil.getClipboardString();
					text = text.substring(0, cur).concat(s).concat(text.substring(cur, text.length()));
					cur += s.length();
					if (text.length() > maxL) {
						text = text.substring(0, maxL);
						cur = maxL;
					}
					break;
				}
			case GLFW_KEY_D: if (ctr) {
					text = "";
					break;
				}
			case GLFW_KEY_UNKNOWN:
				if (c == 0 || cur >= maxL || !allowFormat && c == '\u00a7')
					return false;
				text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL - 1)));
				cur++;
			}
		} catch (IndexOutOfBoundsException e) {
			if (cur < 0) cur = 0;
			if (cur > text.length()) cur = text.length();
		}
		return true;
	}

	@Override
	public void unfocus() {
		set.accept(text);
	}

	@Override
	public boolean focus() {
		text = get.get();
		if (text == null) text = "";
		cur = text.length();
		return true;
	}

}
