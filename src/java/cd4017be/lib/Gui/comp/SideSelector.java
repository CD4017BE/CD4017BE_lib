package cd4017be.lib.Gui.comp;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;

/**
 * A button used to select a block face (for side configuration of machines). Shows the block preview on the side of the screen when hovered.
 * @author CD4017BE
 *
 */
public class SideSelector extends Tooltip {

	private final Supplier<EnumFacing> get;
	private final Predicate<EnumFacing> filter;
	private final Consumer<EnumFacing> set;
	public int type = 3, tx = 0, ty = Integer.MIN_VALUE;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param get side state supplier function (can be all 6 directions + null for none/invalid)
	 * @param filter optional filter function to restrict the set of allowed sides.
	 * @param set side state consumer function
	 */
	public SideSelector(GuiFrame parent, int w, int h, int x, int y, @Nonnull Supplier<EnumFacing> get, @Nullable Predicate<EnumFacing> filter, @Nullable Consumer<EnumFacing> set) {
		super(parent, w, h, x, y, null, ()-> new Object[] {get.get()});
		this.get = get;
		this.filter = filter;
		this.set = set;
	}

	/**
	 * specifies the arrow type (default is bidirectional)
	 * @param t arrow type: {@link #T_NONE}, {@link #T_IN}, {@link #T_OUT} or {@link #T_BIDI}
	 * @return this
	 */
	public SideSelector type(byte t) {
		this.type = t;
		return this;
	}

	/**
	 * specifies the button texture
	 * @param tx texture X-coord
	 * @param ty texture Y-coord ({@code ty + height * sideId})
	 * @return
	 */
	public SideSelector texture(int tx, int ty) {
		this.tx = tx;
		this.ty = ty;
		return this;
	}

	@Override
	public void drawOverlay(int mx, int my) {
		super.drawOverlay(mx, my);
		parent.gui.drawSideConfig(get.get(), type);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		if (ty == Integer.MIN_VALUE) return;
		EnumFacing s = get.get();
		parent.gui.drawTexturedModalRect(x, y, tx, ty + (s == null ? -1 : s.ordinal()) * h, w, h);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (set == null || d == A_HOLD || d == A_UP) return false;
		if (d != A_SCROLL) b = b == 0 ? 1 : 6;
		else b += 7;
		EnumFacing s = get.get();
		do {
			int i = ((s == null ? 6 : s.ordinal()) + b) % 7;
			s = i < EnumFacing.VALUES.length ? EnumFacing.VALUES[i] : null;
		} while(filter != null && !filter.test(s));
		set.accept(s);
		return true;
	}

	/** displayed arrow type code */
	public static final byte T_NONE = 0, T_IN = 1, T_OUT = 2, T_BIDI = 3;

}
