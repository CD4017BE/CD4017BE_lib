package cd4017be.lib.Gui.comp;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import static org.lwjgl.input.Keyboard.*;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.ObjIntConsumer;
import org.apache.commons.lang3.ArrayUtils;

/** Lets the user select from a list of Strings. The list becomes scrollable if
 * it contains more elements than can be displayed at once.
 * @author cd4017be */
public class GuiList extends GuiCompBase<GuiCompGroup>
implements DoubleSupplier, DoubleConsumer {

	public String[] elements = ArrayUtils.EMPTY_STRING_ARRAY;
	public final int n;
	public int scroll, sel = -1, tc = 0xff404040, sc = 0x7f00c0c0;
	private final ObjIntConsumer<GuiList> action;

	/** @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height per element in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param n maximum number of elements displayed at once
	 * @param action select action handler: function(this, index) */
	public GuiList(
		GuiCompGroup parent, int w, int h, int x, int y, int n,
		ObjIntConsumer<GuiList> action
	) {
		super(parent, w, h * n, x, y);
		this.n = n;
		this.action = action;
	}

	/** @param elements the elements to display in this list
	 * @return this */
	public GuiList setElements(String... elements) {
		this.elements = elements;
		sel = elements.length > 0 ? 0 : -1;
		scroll = 0;
		return this;
	}

	/** changes the text and selection color (default: dark grey & transparent cyan)
	 * @param text text color in {@code 0xAARRGGBB} format
	 * @param sel selection color in {@code 0xAARRGGBB} format
	 * @return this */
	public GuiList color(int text, int sel) {
		this.tc = text;
		this.sc = sel;
		return this;
	}

	/**
	 * @param w width of the knob
	 * @param h height of the knob
	 * @param tx texture X-coord of the knob
	 * @param ty texture Y-coord of the knob
	 * @return a scrollbar created for this list
	 */
	public Slider scrollbar(int w, int h, int tx, int ty) {
		return new Slider(
			parent, w, h, this.h - 2, x + this.w + 1 - parent.x, y - parent.y + 1, tx, ty, false,
			this, this, null, 0, 1
		).scroll(-.125F);
	}

	@Override
	public void accept(double value) {
		scroll((int)Math.round(value * (elements.length - n)));
	}

	@Override
	public double getAsDouble() {
		double r = elements.length - n;
		return r <= 0 ? Double.NaN : scroll / r;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		FontRenderer fr = parent.fontRenderer;
		int y = this.y, dy = h / n;
		for(int l = Math.min(n + scroll, elements.length), i = scroll; i < l; i++, y += dy) {
			String s = elements[i];
			if (fr.getStringWidth(s) > w)
				s = fr.trimStringToWidth(s, w - fr.getStringWidth(" ..."), false) + " ...";
			fr.drawString(s, x, y, tc);
		}
		int i = sel - scroll;
		if(i >= 0 && i < n) {
			y = this.y + i * dy;
			Gui.drawRect(x, y, x + w, y + dy, sc);
		}
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		if(elements.length == 0 || d != A_DOWN) return true;
		switch(k) {
		case KEY_UP:
			if(--sel < 0) sel = elements.length - 1;
			select();
			break;
		case KEY_DOWN:
			if(++sel >= elements.length) sel = 0;
			select();
			break;
		case KEY_PRIOR:
			if((sel -= n) > 0)
				select();
		case KEY_HOME:
			sel = 0;
			select();
			break;
		case KEY_NEXT:
			if((sel += n) < elements.length)
				select();
		case KEY_END:
			sel = elements.length - 1;
			select();
			break;
		case KEY_LEFT:
			action.accept(this, -1);
			break;
		case KEY_RETURN:
		case KEY_RIGHT:
			action.accept(this, sel);
			break;
		case KEY_TAB:
		case KEY_ESCAPE:
			parent.setFocus(null);
			break;
		default:
			if(c == 0) break;
			search: {
				c = Character.toLowerCase(c);
				for(int i = sel + 1; i < elements.length; i++)
					if(firstChar(elements[i]) == c) {
						sel = i;
						select();
						break search;
					}
				for(int i = 0; i < sel; i++)
					if(firstChar(elements[i]) == c) {
						sel = i;
						select();
						break search;
					}
			}
		}
		return true;
	}

	private static char firstChar(String s) {
		int l = s.length();
		for(int i = 0; i < l; i++) {
			char c = s.charAt(i);
			if(Character.isWhitespace(c)) continue;
			if(c != '\u00a7') return Character.toLowerCase(c);
			i++;
		}
		return 0;
	}

	private void select() {
		if(sel < scroll) scroll = sel;
		else if(sel >= scroll + n) scroll = sel - n + 1;
	}

	private void scroll(int p) {
		int r = elements.length - n;
		if(p < 0 || r <= 0)
			scroll = 0;
		else if(p > r)
			scroll = r;
		else scroll = p;
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if(d == A_SCROLL)
			scroll(scroll - b);
		if(d == A_DOWN) {
			int i = scroll + (my - y) * n / h;
			if(i == sel) action.accept(this, i);
			else if(i < elements.length) sel = i;
		}
		return true;
	}

	@Override
	public boolean focus() {
		return true;
	}

}
