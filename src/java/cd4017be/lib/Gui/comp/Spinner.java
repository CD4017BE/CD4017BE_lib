package cd4017be.lib.Gui.comp;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * A widget used to configure a number using different increment & decrement buttons.
 * @author CD4017BE
 *
 */
public class Spinner extends GuiFrame {

	private final DoubleSupplier get;
	private final DoubleConsumer set;
	public final double min, max;
	public double clickScale = 8.0;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param hor whether the buttons should arrange right and left next to the number instead of above and below it.
	 * @param format format string used to display the number (either localization key or literal prefixed by {@code '\'}).
	 * @param get state supplier function
	 * @param set state consumer function
	 * @param min minimum value
	 * @param max maximum value
	 * @param steps step sizes of the different increment/decrement buttons (in order left->right or outer->inner).
	 */
	public Spinner(GuiFrame parent, int w, int h, int x, int y, boolean hor, String format, DoubleSupplier get, DoubleConsumer set, double min, double max, double... steps) {
		super(parent, w, h, 2 * steps.length + 2);
		this.get = get;
		this.set = set;
		this.min = min;
		this.max = max;
		int l = steps.length;
		if (hor) {
			int i = 0;
			for (double d : steps) {
				new Button(this, 5, h, i++ * 5, 0, 0, null, (b)-> click(b, d));
				new Button(this, 5, h, w - i * 5, 0, 0, null, (b)-> click(b, -d));
			}
			new FormatText(this, l * 5, 0, w - l * 10, h, format, this::formatInfo);
		} else {
			int i = 0;
			for (double d : steps) {
				new Button(this, w / l, 5, i * w / l, 0, 0, null, (b)-> click(b, d));
				new Button(this, w / l, 5, i++ * w / l, h - 5, 0, null, (b)-> click(b, -d));
			}
			new FormatText(this, 0, 5, w, h - 10, format, this::formatInfo);
		}
		position(x, y);
		parent.add(this);
	}

	/**
	 * specifies this component to show a tool-tip overlay
	 * @param tooltip localization key of the tool-tip format string
	 * @return this
	 */
	public Spinner tooltip(String tooltip) {
		new Tooltip(this, w, h, x, y, tooltip, this::formatInfo);
		return this;
	}

	/**
	 * specifies the click type multiplier
	 * @param scale step size multiplier when using right- instead of left-click.
	 * @return this
	 */
	public Spinner clickScale(double scale) {
		this.clickScale = scale;
		return this;
	}

	private Object[] formatInfo() {
		return new Object[] {get.getAsDouble(), min, max};
	}

	private void click(int b, double step) {
		if (b == Button.B_SCROLL_DOWN) step = -step;
		else if (b != Button.B_SCROLL_UP) {
			if (b != B_LEFT) step *= clickScale;
			if (b == B_MID) step *= clickScale;
		}
		double f = get.getAsDouble() + step;
		set.accept(f < min ? min : f > max ? max : f);
	}

}
