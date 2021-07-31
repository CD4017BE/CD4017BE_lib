package cd4017be.lib.gui.comp;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import cd4017be.lib.text.TooltipUtil;

/**
 * A component that shows a formatted tool-tip overlay when hovered.
 * @author CD4017BE
 *
 */
public class Tooltip extends GuiCompBase<GuiCompGroup> {

	private final Supplier<Object[]> params;
	public String tooltip;

	/**
	 * @param parent the gui-component container this will register to
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param x initial X-coord
	 * @param y initial Y-coord
	 * @param tooltip localization key of the tool-tip format string
	 * @param params format arguments supplier function
	 */
	public Tooltip(GuiCompGroup parent, int w, int h, int x, int y, @Nullable String tooltip, @Nullable Supplier<Object[]> params) {
		super(parent, w, h, x, y);
		this.tooltip = tooltip;
		this.params = params;
	}

	/**
	 * specifies this component to show a tool-tip overlay
	 * @param tooltip localization key of the tool-tip format string
	 * @return this
	 */
	public Tooltip tooltip(String tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	@Override
	public void drawOverlay(PoseStack stack, int mx, int my) {
		if (tooltip == null) return;
		parent.drawTooltip(stack, params == null ?
				TooltipUtil.translate(tooltip) :
				TooltipUtil.format(tooltip, params.get()), mx, my);
	}

}
