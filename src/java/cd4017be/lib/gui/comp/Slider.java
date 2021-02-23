package cd4017be.lib.gui.comp;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;

/**
 * A slider that can be dragged either horizontally or vertically.
 * @author CD4017BE
 *
 */
public class Slider extends Tooltip {

	private final DoubleSupplier get;
	private final DoubleConsumer set;
	private final Runnable update;
	public final int l, tx, ty, tw, th;
	public final boolean hor;
	public final double min, max;
	public double scrollStep;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w knob width in pixels
	 * @param h knob height in pixels
	 * @param l slider length in pixels 
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param tx texture X-coord
	 * @param ty texture Y-coord
	 * @param hor whether the slider is moved horizontally
	 * @param get state supplier function
	 * @param set state consumer function
	 * @param update optional notification function (when slider is released by cursor)
	 * @param min minimum value (left or bottom)
	 * @param max maximum value (right or top)
	 */
	public Slider(GuiCompGroup parent, int w, int h, int l, int x, int y, int tx, int ty, boolean hor, @Nonnull DoubleSupplier get, @Nonnull DoubleConsumer set, @Nullable Runnable update, double min, double max) {
		super(parent, hor?l:w, hor?h:l, x, y, null, ()-> new Object[] {get.getAsDouble(), min, max});
		this.hor = hor;
		this.l = l - (hor?w:h);
		this.tx = tx;
		this.ty = ty;
		this.tw = w;
		this.th = h;
		this.get = get;
		this.set = set;
		this.update = update;
		this.min = min;
		this.max = max;
		this.scrollStep = (max - min) / 8.0;
	}

	/**
	 * Uses default range 0.0 - 1.0
	 * @see #Slider(GuiFrame, int, int, int, int, int, int, int, boolean, DoubleSupplier, DoubleConsumer, Runnable, double, double)
	 */
	public Slider(GuiFrame parent, int w, int h, int l, int x, int y, int tx, int ty, boolean hor, @Nonnull DoubleSupplier get, @Nonnull DoubleConsumer set, @Nullable Runnable update) {
		this(parent, w, h, l, x, y, tx, ty, hor, get, set, update, 0.0, 1.0);
	}

	/**
	 * specifies the scroll step
	 * @param step value to increment/decrement per scroll unit
	 * @return this
	 */
	public Slider scroll(float step) {
		scrollStep = step;
		return this;
	}

	@Override
	public void drawBackground(MatrixStack stack, int mx, int my, float t) {
		int n = (int)Math.round((get.getAsDouble() - min) / (max - min) * (double)l);
		if (hor) parent.drawRect(stack, x + n, y, tx, ty, tw, th);
		else parent.drawRect(stack, x, y + n, tx, ty, tw, th);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (d == A_SCROLL) {
			double f = get.getAsDouble() + (double)b * scrollStep;
			double min = Math.min(this.min, this.max);
			double max = Math.max(this.min, this.max);
			if (f < min) f = min;
			else if (f > max) f = max;
			set.accept(f);
			if (update != null) update.run();
			return true;
		}
		double f = 0.5 * (hor ? (double)(2 * (mx - x) - tw) / (double)l : (double)(2 * (my - y) - th) / (double)l);
		if (f < 0.0) f = 0.0;
		else if (f > 1.0) f = 1.0;
		set.accept(min + (max - min) * f);
		if (d == A_UP) parent.setFocus(null);
		return true;
	}

	@Override
	public void unfocus() {
		if (update != null) update.run();
	}

	@Override
	public boolean focus() {
		return true;
	}

}
