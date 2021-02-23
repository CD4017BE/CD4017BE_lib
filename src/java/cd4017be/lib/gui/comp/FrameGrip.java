package cd4017be.lib.gui.comp;

import com.mojang.blaze3d.matrix.MatrixStack;

import cd4017be.lib.text.TooltipUtil;

/**
 * A Gui component that allows the user click-drag to move the component's parent group around.
 * @author CD4017BE
 */
public class FrameGrip extends GuiCompBase<GuiCompGroup> {

	/**
	 * @param parent the gui-component container to move around
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 */
	public FrameGrip(GuiCompGroup parent, int w, int h, int x, int y) {
		super(parent, w, h, x, y);
	}

	@Override
	public void drawOverlay(MatrixStack stack, int mx, int my) {
		parent.drawTooltip(stack, TooltipUtil.translate("gui.cd4017be.move"), mx, my);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (b == B_LEFT && d == A_HOLD) {
			int dx = mx - x - w / 2;
			int dy = my - y - h / 2;
			if (dx != 0 || dy != 0)
				parent.move(dx, dy);
			return true;
		} else if (d == A_UP) unfocus();
		return false;
	}

	@Override
	public boolean focus() {
		return true;
	}

}
