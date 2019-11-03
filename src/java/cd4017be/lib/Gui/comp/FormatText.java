package cd4017be.lib.Gui.comp;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import cd4017be.lib.util.TooltipUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Displays a formatted and localized (optional) text that may span over multiple lines.<br>
 * The format arguments are provided via a Supplier function.
 * @author CD4017BE
 *
 */
public class FormatText extends GuiCompBase<GuiCompGroup> {

	private final Supplier<Object[]> params;
	public String text;
	public int tc = 0xff404040;
	public float align = 0.5F;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param format either a localization key or when prefixed with {@code '\'} the actual format string.
	 * @param params format arguments supplier
	 */
	public FormatText(GuiCompGroup parent, int w, int h, int x, int y, String format, @Nullable Supplier<Object[]> params) {
		super(parent, w, h, x, y);
		this.params = params;
		this.text = format;
	}

	/**
	 * changes the text color (default is dark grey)
	 * @param tc color in {@code 0xAARRGGBB} format
	 * @return this
	 */
	public FormatText color(int tc) {
		this.tc = tc;
		return this;
	}

	/**
	 * Changes the text alignment
	 * @param x relative alignment: 0.0 = left, 0.5 = middle, 1.0 = right
	 * @return this
	 */
	public FormatText align(float x) {
		this.align = x;
		return this;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		Object[] obj = params == null ? new Object[0] : params.get();
		String lines = TooltipUtil.format(text, obj);
		parent.bindTexture(null);
		parent.drawNow();
		FontRenderer fr = parent.fontRenderer;
		int y = this.y;
		for (String s : lines.split("\n")) {
			int x = this.x + (align == 0 ? 0 : (int)Math.round((float)(w - fr.getStringWidth(s)) * align));
			fr.drawString(s, x, y, tc);
			y += fr.FONT_HEIGHT;
		}
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

}
