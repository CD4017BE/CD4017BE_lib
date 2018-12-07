package cd4017be.lib.Gui.comp;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

/**
 * A button than can have different states with different icon textures (optional) and automatically cycles through a fixed number of states on mouse clicks (optional).<br>
 * The state is handled as integer via Supplier and Consumer functions.
 * @author CD4017BE
 *
 */
public class Button extends Tooltip {

	private final IntSupplier get;
	private final IntConsumer set;
	public final int states;
	public int tx = 0, ty = Integer.MIN_VALUE;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param states number of states, the button will cycle through: {@code 0 ... states-1}
	 * @param get state supplier
	 * @param set state consumer. If states == 0, this will receive the encoded click operation instead: {@link IGuiComp#B_LEFT}, {@link IGuiComp#B_RIGHT}, {@link IGuiComp#B_MID}, {@link #B_SCROLL_UP} or {@link #B_SCROLL_DOWN}.
	 */
	public Button(GuiFrame parent, int w, int h, int x, int y, int states, @Nullable IntSupplier get, @Nullable IntConsumer set) {
		super(parent, w, h, x, y, null, get == null ? null : ()-> new Object[] {get.getAsInt()});
		this.states = states;
		this.get = get;
		this.set = set;
	}

	/**
	 * enables drawing a background texture that depends on state
	 * @param tx texture X-coord
	 * @param ty texture Y-coord ({@code ty + height * state})
	 * @return this
	 */
	public Button texture(int tx, int ty) {
		this.tx = tx;
		this.ty = ty;
		return this;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		if (ty == Integer.MIN_VALUE) return;
		int s = get == null ? 0 : get.getAsInt();
		parent.gui.drawTexturedModalRect(x, y, tx, ty + s * h, w, h);
	}

	@Override
	public boolean mouseIn(int x, int y, int b, byte d) {
		if (set == null || d == A_HOLD || d == A_UP) return false;
		if (states <= 0 || get == null)
			set.accept(d == A_SCROLL ? b - 2 : b);
		else {
			if (d != A_SCROLL) b = b == 0 ? 1 : -1;
			set.accept(Math.floorMod(b + get.getAsInt(), states));
		}
		return true;
	}

	/** click operation codes for scrolling */
	public static final byte B_SCROLL_UP = -1, B_SCROLL_DOWN = -3;

}
