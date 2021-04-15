package cd4017be.lib.gui.comp;

import java.util.ArrayList;

import com.mojang.blaze3d.matrix.MatrixStack;

import cd4017be.lib.text.TooltipUtil;

/**
 * A multi-page quick documentation overlay that shows when hovered over.
 * @author CD4017BE
 *
 */
public class InfoTab extends GuiCompBase<GuiCompGroup> {

	private final String tooltip;
	private final String[] headers, keys;
	private int page = 0;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param tooltip localization key of the header line
	 */
	public InfoTab(GuiCompGroup parent, int w, int h, int x, int y, String tooltip) {
		super(parent, w, h, x, y);
		this.tooltip = tooltip;
		headers = TooltipUtil.translate(tooltip).split("\n");
		keys = new String[headers.length];
		initHeader();
	}

	private void initHeader() {
		for (int i = 0; i < keys.length; i++) {
			String s = headers[i];
			int p = s.indexOf('@');
			if (p < 0) keys[i] = tooltip + i;
			else {
				keys[i] = s.substring(p + 1).trim();
				headers[i] = s.substring(0, p);
			}
		}
	}

	@Override
	public void drawOverlay(MatrixStack stack, int mx, int my) {
		if (TooltipUtil.editor != null) {
			String[] s = TooltipUtil.translate(tooltip).split("\n");
			System.arraycopy(s, 0, headers, 0, Math.min(s.length, headers.length));
			initHeader();
		}
		ArrayList<String> list = new ArrayList<String>();
		String s = "";
		for (int i = 0; i < headers.length; i++) {
			String h = headers[i];
			if (!h.isEmpty() && h.charAt(0) == '§') {
				s += h.substring(0, 2);
				h = h.substring(2);
			}
			if (i == page) s += "§m";
			s += h + "§r | ";
		}
		list.add(s.substring(0, s.length() - 3));
		for (String l : TooltipUtil.translate(keys[page]).split("\n"))
			list.add(l);
		parent.drawTooltip(stack, list, mx, my);
	}

	@Override
	public boolean mouseIn(int x, int y, int b, byte d) {
		if (d == A_DOWN) {
			if (b == B_LEFT) page++;
			else if (b == B_RIGHT) page--;
			else return false;
		} else if (d == A_SCROLL) page += b;
		else return false;
		page = Math.floorMod(page, headers.length);
		return true;
	}

}
