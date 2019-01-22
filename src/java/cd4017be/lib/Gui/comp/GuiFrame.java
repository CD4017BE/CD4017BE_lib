package cd4017be.lib.Gui.comp;

import javax.annotation.Nonnull;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * A component group to be managed by a {@link ModularGui}. Optionally includes a texture context for rendering other components, a background icon (for window frames) and a title text.
 * May also be used to group other components together.
 * @author CD4017BE
 *
 */
public class GuiFrame extends GuiCompGroup {

	public final ModularGui gui;
	public ResourceLocation bgTexture;
	public int bgX, bgY, titleY = 4;
	public float titleX = 0.5F;
	public String title = null;

	/**
	 * @param parent parent container to register with
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param comps expected number of sub components
	 */
	public GuiFrame(@Nonnull GuiFrame parent, int w, int h, int comps) {
		super(parent, w, h, comps);
		this.gui = parent.gui;
	}

	/**
	 * @param gui the ModularGui this is owned by
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param comps expected number of sub components
	 */
	public GuiFrame(@Nonnull ModularGui gui, int w, int h, int comps) {
		super(null, w, h, comps);
		this.gui = gui;
	}

	/**
	 * sets a title to be rendered at the top.
	 * @param title a localization key or the plain text prefixed with {@code '\'}.
	 * @param relX horizontal alignment: 0 = left, 1 = right, 0.5 = center
	 * @return this
	 */
	public GuiFrame title(String title, float relX) {
		this.title = title.startsWith("\\") ? title.substring(1) : TooltipUtil.translate(title);
		this.titleX = relX;
		return this;
	}

	/**
	 * configures a background icon to be rendered (also sets that icon to be the default component texture)
	 * @param tex background texture
	 * @param tx texture X-coord
	 * @param ty texture Y-coord
	 * @return
	 */
	public GuiFrame background(ResourceLocation tex, int tx, int ty) {
		this.bgTexture = tex;
		this.bgX = tx;
		this.bgY = ty;
		texture(tex, 256, 256);
		return this;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		if (bgTexture != null) {
			bindTexture(bgTexture);
			GuiUtils.drawTexturedModalRect(x, y, bgX, bgY, w, h, zLevel);
		}
		super.drawBackground(mx, my, t);
		if (title != null)
			fontRenderer.drawString(title, x + (int)(titleX * (float)(w - fontRenderer.getStringWidth(title))), y + titleY, 0x404040);
	}

}
