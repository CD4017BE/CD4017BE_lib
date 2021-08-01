package cd4017be.lib.gui.comp;

import javax.annotation.Nonnull;

import cd4017be.lib.gui.ModularGui;
import cd4017be.lib.text.TooltipUtil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmlclient.gui.GuiUtils;

/**
 * A component group to be managed by a {@link ModularGui}. Optionally includes a texture context for rendering other components, a background icon (for window frames) and a title text.
 * May also be used to group other components together.
 * @author CD4017BE
 *
 */
public class GuiFrame extends GuiCompGroup {

	public final ModularGui<?> gui;
	public ResourceLocation bgTexture;
	public int bgX, bgY, titleY = 4;
	public float titleX = 0.5F;
	public String title = null;
	protected GuiCompGroup extension;

	/**
	 * @param parent parent container to register with
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param comps expected number of sub components
	 */
	public GuiFrame(@Nonnull GuiFrame parent, int w, int h, int comps) {
		super(parent, w, h, comps);
		this.gui = parent.gui;
		this.fontRenderer = parent.fontRenderer;
	}

	/**
	 * @param gui the ModularGui this is owned by
	 * @param w width in pixels
	 * @param h height in pixels
	 * @param comps expected number of sub components
	 */
	public GuiFrame(@Nonnull ModularGui<?> gui, int w, int h, int comps) {
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
		this.title = TooltipUtil.translate(title);
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

	public void extendBy(GuiCompGroup comp) {
		this.extension = comp;
		if (comp.parent == this)
			comp.inheritRender();
	}

	@Override
	public boolean isInside(int mx, int my) {
		return super.isInside(mx, my) || extension != null && extension.isInside(mx, my);
	}

	@Override
	public void drawBackground(PoseStack stack, int mx, int my, float t) {
		if (parent != null) parent.drawNow();
		if (bgTexture != null) {
			bound = false;
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			ModularGui.color(-1);
			bindTexture(bgTexture);
			GuiUtils.drawTexturedModalRect(stack, x, y, bgX, bgY, w, h, zLevel);
		}
		if (title != null)
			fontRenderer.draw(stack, title, x + (int)(titleX * (float)(w - fontRenderer.width(title))), y + titleY, 0x404040);
		super.drawBackground(stack, mx, my, t);
	}

	@Override
	public boolean focus() {
		return true;
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		return super.mouseIn(mx, my, b, d) || parent != null && parent.focus == getIdx();
	}

}
