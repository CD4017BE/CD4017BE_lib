package cd4017be.lib.Gui.comp;

import javax.annotation.Nullable;

import cd4017be.lib.util.IndexedSet;

/**
 * {@link IGuiComp} implementation that holds other gui-components inside it.
 * @author CD4017BE
 */
public class GuiCompGroup extends IndexedSet<IGuiComp> implements IGuiComp {

	public final GuiCompGroup parent;
	public final int w, h;
	protected int x = 0, y = 0, focus = -1;
	protected boolean enabled = true;
	private int idx = -1;

	/**
	 * @param parent optional parent container to register with
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param comps expected number of sub components (the actual number may differ later)
	 */
	public GuiCompGroup(@Nullable GuiCompGroup parent, int w, int h, int comps) {
		super(new IGuiComp[comps]);
		this.parent = parent;
		this.w = w;
		this.h = h;
		if (parent != null) parent.add(this);
	}

	@Override
	public void setIdx(int idx) {this.idx = idx;}

	@Override
	public int getIdx() {return idx;}

	@Override
	public GuiCompGroup getParent() {return parent;}

	@Override
	public boolean enabled() {return enabled;}

	@Override
	public void setEnabled(boolean enable) {this.enabled = enable;}

	/**
	 * sets the position to absolute values
	 * @param px absolute X-coord
	 * @param py absolute Y-coord
	 */
	public void position(int px, int py) {
		if (px != x || py != y)
			move(px - x, py - y);
	}

	@Override
	public void move(int dx, int dy) {
		x += dx;
		y += dy;
		for(int i = 0; i < count; i++)
			array[i].move(dx, dy);
	}

	@Override
	public boolean isInside(int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public void drawOverlay(int mx, int my) {
		IGuiComp c;
		for(int i = 0; i < count; i++)
			if ((c = array[i]).enabled() && c.isInside(mx, my))
				c.drawOverlay(mx, my);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		IGuiComp c;
		for(int i = 0; i < count; i++)
			if ((c = array[i]).enabled())
				c.drawBackground(mx, my, t);
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		return focus >= 0 && focus < count && array[focus].keyIn(c, k, d);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (d == A_DOWN) {
			IGuiComp c;
			for(int i = 0; i < count; i++)
				if ((c = array[i]).enabled() && c.isInside(mx, my)) {
					if (c.getIdx() != focus) setFocus(c);
					if (c.mouseIn(x, y, b, d)) return true;
				}
			if (focus >= 0 && focus < count && !array[focus].isInside(mx, my)) setFocus(null);
		} else if (d == A_SCROLL) {
			IGuiComp c;
			for(int i = 0; i < count; i++)
				if ((c = array[i]).enabled() && c.isInside(mx, my) && c.mouseIn(mx, my, b, d))
					return true;
		} else return focus >= 0 && focus < count && array[focus].mouseIn(mx, my, b, d);
		return false;
	}

	@Override
	public void unfocus() {
		setFocus(null);
	}

	@Override
	public boolean focus() {
		return true;
	}

	/**
	 * moves the focus to the given component
	 * @param c the component to focus or null to focus none
	 */
	public void setFocus(IGuiComp c) {
		if (focus >= 0 && focus < count) array[focus].unfocus();
		focus = contains(c) && c.focus() ? c.getIdx() : -1;
	}

	/**
	 * moves the focus to the next component of given type
	 * @param type component class
	 */
	public void focusNext(Class<?extends IGuiComp> type) {
		for (int i = 1; i < count; i++) {
			IGuiComp c = array[(focus + i % count)];
			if (type.isInstance(c)) {
				setFocus(c);
				return;
			}
		}
	}

	/**
	 * moves the focus to the previous component of given type
	 * @param type component class
	 */
	public void focusPrev(Class<?extends IGuiComp> type) {
		for (int i = count - 1; i > 0; i--) {
			IGuiComp c = array[(focus + i % count)];
			if (type.isInstance(c)) {
				setFocus(c);
				return;
			}
		}
	}

}
