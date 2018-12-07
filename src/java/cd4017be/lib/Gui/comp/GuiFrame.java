package cd4017be.lib.Gui.comp;

import javax.annotation.Nonnull;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

/**
 * A component group to be managed by a {@link ModularGui}. Optionally includes a texture context for rendering other components, a background icon (for window frames) and a title text.
 * May also be used to group other components together.
 * @author CD4017BE
 *
 */
public class GuiFrame extends GuiCompGroup {

	public final ModularGui gui;
	public ResourceLocation texture;
	public int bgX = 0, bgY = Integer.MIN_VALUE, titleY = 4;
	public float titleX = 0.5F;
	public boolean drawBG = false;
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
	 * sets the rendering context texture
	 * @param tex image resource path
	 * @return this
	 */
	public GuiFrame texture(ResourceLocation tex) {
		this.texture = tex;
		return this;
	}

	/**
	 * configures a background icon to be rendered
	 * @param tx texture X-coord
	 * @param ty texture Y-coord
	 * @return
	 */
	public GuiFrame background(int tx, int ty) {
		this.bgX = tx;
		this.bgY = ty;
		return this;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		if (texture != null) gui.mc.renderEngine.bindTexture(texture);
		if (bgY != Integer.MIN_VALUE) gui.drawTexturedModalRect(x, y, bgX, bgY, w, h);
		super.drawBackground(mx, my, t);
		if (title != null) {
			FontRenderer fr = gui.mc.fontRenderer;
			fr.drawString(title, x + (int)(titleX * (float)(w-fr.getStringWidth(title))), y + titleY, 0x404040);
		}
	}

	/**
	 * helper method to bind a given texture
	 * @param tex texture to bind
	 */
	public void bindTexture(ResourceLocation tex) {
		gui.mc.renderEngine.bindTexture(tex);
	}

}
