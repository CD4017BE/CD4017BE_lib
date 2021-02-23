package cd4017be.lib.gui.comp;

import static org.lwjgl.glfw.GLFW.*;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.client.gui.GuiUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

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
	public boolean allowFormat = false;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param max maximum number of characters
	 * @param get text supplier function
	 * @param set text consumer function
	 */
	public TextField(GuiCompGroup parent, int w, int h, int x, int y, int max, @Nonnull Supplier<String> get, @Nonnull Consumer<String> set) {
		super(parent, w, h, x, y, null, null);
		this.maxL = max;
		this.get = get;
		this.set = set;
	}

	/**
	 * changes the text and cursor color (default: dark grey & red)
	 * @param text text color in {@code 0xAARRGGBB} format
	 * @param cursor cursor color in {@code 0xAARRGGBB} format
	 * @return this
	 */
	public TextField color(int text, int cursor) {
		this.tc = text; this.cc = cursor;
		return this;
	}

	/**
	 * specifies that minecraft's text formatting code prefix character ({@code '�'}) should be allowed in this text field.
	 * @return this
	 */
	public TextField allowFormat() {
		this.allowFormat = true;
		return this;
	}

	@Override
	public void drawBackground(MatrixStack stack, int mx, int my, float ft) {//TODO support multiline
		parent.bindTexture(null);
		FontRenderer fr = parent.fontRenderer;
		String t;
		int ofs = 0;
		if (focused()) {
			if (cur > text.length()) cur = text.length();
			int l = fr.getStringWidth(text);
			int k = fr.getStringWidth(text.substring(0, cur));
			if (l > w)
				if (k <= w/2) {
					t = fr.func_238413_a_(text, w, false);
				} else if (l - k < w/2) {
					k = k - l + w;
					t = fr.func_238413_a_(text, w, true);
					ofs = w - fr.getStringWidth(t);
				} else {
					k = w/2;
					t = fr.func_238413_a_(text.substring(0, cur), k, true);
					ofs = k - fr.getStringWidth(t);
					t += fr.func_238413_a_(text.substring(cur), w - k, false);
				}
			else t = text;
			GuiUtils.drawGradientRect(stack.getLast().getMatrix(), (int)parent.zLevel, x - 1 + k, y + (h - fr.FONT_HEIGHT) / 2 + 1, x + k, y + (h + 7) / 2, cc, cc);
		} else {
			text = get.get();
			t = fr.func_238413_a_(text, w, true);
			if (t.length() < text.length()) ofs = w - fr.getStringWidth(t);
		}
		fr.drawString(stack, t, x + ofs, y + (h - 8) / 2, tc);
		GlStateManager.color4f(1, 1, 1, 1);
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		try {
			boolean ctr = Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown();
			switch(k) {
			case GLFW_KEY_LEFT: if (cur > 0) cur--; break;
			case GLFW_KEY_RIGHT: if (cur < text.length()) cur++; break;
			case GLFW_KEY_DELETE: if (cur < text.length()) {
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} break;
			case GLFW_KEY_BACKSPACE: if (cur > 0) {
					cur--;
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} break;
			case GLFW_KEY_ENTER: parent.setFocus(null); break;
			case GLFW_KEY_UP: parent.focusNext(TextField.class); break;
			case GLFW_KEY_DOWN: parent.focusPrev(TextField.class); break;
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
			default: if (cur < maxL && (allowFormat || c != '\u00a7')){
					text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL - 1)));
					cur++;
				} else return false;
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
		cur = text.length();
		return true;
	}

}
