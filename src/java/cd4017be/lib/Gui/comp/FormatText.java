package cd4017be.lib.Gui.comp;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import cd4017be.lib.util.TooltipUtil;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Displays a formatted and localized (optional) text that may span over multiple lines.<br>
 * The format arguments are provided via a Supplier function.
 * @author CD4017BE
 *
 */
public class FormatText extends GuiCompBase<GuiFrame> {

	private final Supplier<Object[]> params;
	public String text;
	public int tc = 0xff404040;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param format either a localization key or when prefixed with {@code '\'} the actual format string.
	 * @param params format arguments supplier
	 */
	public FormatText(GuiFrame parent, int w, int h, int x, int y, String format, @Nullable Supplier<Object[]> params) {
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

	@Override
	public void drawBackground(int mx, int my, float t) {
		Object[] obj = params == null ? new Object[0] : params.get();
		String lines = (text.startsWith("\\") ? 
				String.format(text.substring(1), obj) : 
				TooltipUtil.format(text, obj)
			);
		parent.gui.mc.fontRenderer.drawSplitString(lines, x, y, w, tc);
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

}
