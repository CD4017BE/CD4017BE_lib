package cd4017be.lib.Gui.comp;

/**
 * Basic {@link IGuiComp} template for components managed by {@link GuiCompGroup} containers.
 * @author CD4017BE
 *
 */
public class GuiCompBase<G extends GuiCompGroup> implements IGuiComp {

	public final G parent;
	public final int w, h;
	protected int x = 0, y = 0;
	protected boolean enabled = true;
	private int idx = -1;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 */
	public GuiCompBase(G parent, int w, int h, int x, int y) {
		this.parent = parent;
		this.w = w;
		this.h = h;
		this.x = x;
		this.y = y;
		parent.add(this);
	}

	@Override
	public void setIdx(int idx) {this.idx = idx;}

	@Override
	public int getIdx() {return idx;}

	@Override
	public G getParent() {return parent;}

	@Override
	public boolean enabled() {return enabled;}

	@Override
	public void setEnabled(boolean enable) {this.enabled = enable;}

	@Override
	public void move(int dx, int dy) {
		x += dx;
		y += dy;
	}

	public boolean isInside(int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/**
	 * @return whether this gui-component is currently focused
	 */
	public boolean focused() {
		return idx == parent.focus;
	}

}
