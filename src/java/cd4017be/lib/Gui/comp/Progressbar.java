package cd4017be.lib.Gui.comp;

import java.util.function.DoubleSupplier;

import javax.annotation.Nonnull;

/**
 * A progress bar that may either fill or slide in horizontally or vertically or fill pixel by pixel.
 * @author CD4017BE
 *
 */
public class Progressbar extends Tooltip {

	private final DoubleSupplier get;
	public final byte type;
	public final int tx, ty;
	public final double f0, scale;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param tx texture X-coord
	 * @param ty texture Y-coord
	 * @param type design variant: can be {@link #H_FILL}, {@link #V_FILL}, {@link #H_SLIDE}, {@link #V_SLIDE} or {@link #PIXELS}.
	 * @param get progress supplier function
	 * @param min minimum value representing the empty bar
	 * @param max maximum value representing the full bar. (if {@code max < min} then the progress bar will fill from the opposite side)
	 */
	public Progressbar(GuiFrame parent, int w, int h, int x, int y, int tx, int ty, byte type, @Nonnull DoubleSupplier get, double min, double max) {
		super(parent, w, h, x, y, null, ()-> new Object[] {get.getAsDouble(), min, max});
		this.type = type;
		this.tx = tx;
		this.ty = ty;
		this.get = get;
		this.f0 = min;
		switch (type) {
		case H_FILL: case H_SLIDE: this.scale = (double)w / (max - min); break;
		case V_FILL: case V_SLIDE: this.scale = (double)h / (max - min); break;
		default: this.scale = (double)(w * h) / (max - min);
		}
	}

	/**
	 * Uses the default range 0.0 - 1.0 for progress.
	 * @see #Progressbar(GuiFrame, int, int, int, int, int, int, byte, DoubleSupplier, double, double)
	 */
	public Progressbar(GuiFrame parent, int w, int h, int x, int y, int tx, int ty, byte type, @Nonnull DoubleSupplier get) {
		this(parent, w, h, x, y, tx, ty, type, get, 0.0, 1.0);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		double f = scale * (get.getAsDouble() - f0);
		if (!(f > 0)) return;
		int n = (int)f;
		boolean rev = scale < 0;
		switch (type) {
		case H_FILL:
			if (n >= w) break;
			if (rev) parent.gui.drawTexturedModalRect(x + w - n, y, tx + w - n, ty, n, h);
			else parent.gui.drawTexturedModalRect(x, y, tx, ty, n, h);
			return;
		case V_FILL:
			if (n >= h) break;
			if (rev) parent.gui.drawTexturedModalRect(x, y, tx, ty, w, n);
			else parent.gui.drawTexturedModalRect(x, y + h - n, tx, ty + h - n, w, n);
			return;
		case H_SLIDE: parent.gui.drawTexturedModalRect(x, y, tx + n, ty, w, h); return;
		case V_SLIDE: parent.gui.drawTexturedModalRect(x, y, tx, ty + n, w, h); return;
		case PIXELS:
			int m = n / h; n %= h;
			if (m >= w) break;
			if (rev) {
				parent.gui.drawTexturedModalRect(x + w - m, y, tx + w - m, ty, m, h);
				int dx1 = w - m - 1, dy1 = h - n;
				parent.gui.drawTexturedModalRect(x + dx1, y + dy1, tx + dx1, ty + dy1, 1, n);
			} else {
				parent.gui.drawTexturedModalRect(x, y, tx, ty, m, h);
				parent.gui.drawTexturedModalRect(x + m, y, tx + m, ty, 1, n);
			} return;
		}
		parent.gui.drawTexturedModalRect(x, y, tx, ty, w, h);
	}

	/** design variant codes */
	public static final byte H_FILL = 0, V_FILL = 1, H_SLIDE = 2, V_SLIDE = 3, PIXELS = 4;

}
