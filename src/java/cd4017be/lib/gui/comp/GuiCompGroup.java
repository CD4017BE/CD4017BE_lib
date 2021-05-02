package cd4017be.lib.gui.comp;

import static cd4017be.lib.text.TooltipUtil.convertText;
import static net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText;

import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import cd4017be.lib.util.IndexedSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.fml.client.gui.GuiUtils;

/**
 * {@link IGuiComp} implementation that holds other gui-components inside it.
 * @author CD4017BE
 */
public class GuiCompGroup extends IndexedSet<IGuiComp> implements IGuiComp {

	public final GuiCompGroup parent;
	public final int w, h;
	protected int x = 0, y = 0, focus = -1;
	protected boolean enabled = true, inheritRender = false;
	private int idx = -1;
	
	public int screenWidth, screenHeight, texW, texH;
	public float zLevel;
	public FontRenderer fontRenderer;
	public Tessellator tessellator;
	public ResourceLocation mainTex;
	protected boolean bound, drawing;

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

	/**
	 * specifies that this component group should use the same texture and tessellator as it's parent group.
	 * @return this
	 */
	public GuiCompGroup inheritRender() {
		this.inheritRender = true;
		this.texture(parent.mainTex, parent.texW, parent.texH);
		return this;
	}

	/**
	 * initialize this GuiComp for rendering
	 * @param sw total width of underlying screen
	 * @param sh total height of underlying screen
	 * @param fr font renderer to draw text
	 */
	public void init(int sw, int sh, float z, FontRenderer fr) {
		this.screenWidth = sw;
		this.screenHeight = sh;
		this.zLevel = z;
		this.fontRenderer = fr;
		IGuiComp c;
		for(int i = 0; i < count; i++)
			if ((c = array[i]) instanceof GuiCompGroup)
				((GuiCompGroup)c).init(sw, sh, z, fr);
	}

	/**
	 * sets this component group's default texture image
	 * @param tex texture resource location
	 * @param w width of image
	 * @param h height of image
	 */
	public void texture(ResourceLocation tex, int w, int h) {
		this.mainTex = tex;
		this.texW = w;
		this.texH = h;
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
	public boolean add(IGuiComp e) {
		if (!super.add(e)) return false;
		if (x != 0 || y != 0) e.move(x, y);
		return true;
	}

	@Override
	public boolean isInside(int mx, int my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public void drawOverlay(MatrixStack stack, int mx, int my) {
		IGuiComp c;
		for(int i = count - 1; i >= 0; i--)
			if ((c = array[i]).enabled() && c.isInside(mx, my)) {
				c.drawOverlay(stack, mx, my);
				if (c instanceof GuiCompGroup) return;
			}
	}

	@Override
	public void drawBackground(MatrixStack stack, int mx, int my, float t) {
		if (!inheritRender) {
			if (parent != null) parent.bound = false;
			bound = false;
		}
		IGuiComp c;
		for(int i = 0; i < count; i++)
			if ((c = array[i]).enabled())
				c.drawBackground(stack, mx, my, t);
		drawNow();
	}

	@Override
	public boolean keyIn(char c, int k, byte d) {
		return focus >= 0 && focus < count && array[focus].keyIn(c, k, d);
	}

	@Override
	public boolean mouseIn(int mx, int my, int b, byte d) {
		if (d == A_DOWN) {
			IGuiComp c;
			for(int i = count - 1; i >= 0 ; i--)
				if ((c = array[i]).enabled() && c.isInside(mx, my)) {
					if (c.getIdx() != focus) setFocus(c);
					if (c.mouseIn(mx, my, b, d)) return true;
				}
			if (focus >= 0 && focus < count && !array[focus].isInside(mx, my)) setFocus(null);
		} else if (d == A_SCROLL) {
			IGuiComp c;
			for(int i = count - 1; i >= 0 ; i--)
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

	/**
	 * helper method to bind a given texture
	 * @param tex texture to bind
	 */
	public void bindTexture(ResourceLocation tex) {
		if (inheritRender) parent.bindTexture(tex);
		if (tex != mainTex) bound = false;
		else if (bound) return;
		else bound = true;
		if (tex != null) Minecraft.getInstance().textureManager.bind(tex);
	}

	/**
	 * @return a local render buffer instance for drawing rectangular shapes with this component group's default texture image (at the end of current background draw)
	 */
	public BufferBuilder getDraw() {
		if (inheritRender) {
			BufferBuilder b = parent.getDraw();
			tessellator = parent.tessellator;
			return b;
		}
		if (tessellator == null) tessellator = new Tessellator(256 * 5);
		BufferBuilder b = tessellator.getBuilder();
		if (!drawing) {
			b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			drawing = true;
		}
		return b;
	}

	/**
	 * draws queued vertices immediately instead of waiting till the end of current background draw cycle.
	 */
	public void drawNow() {
		if (inheritRender) parent.drawNow();
		else if (drawing) {
			GlStateManager._color4f(1F, 1F, 1F, 1F);
			GlStateManager._enableBlend();
			bindTexture(mainTex);
			tessellator.end();
			drawing = false;
		}
	}

	/**
	 * draw a textured rectangle using this component group's default texture image
	 * @param x screen x-coord
	 * @param y screen y-coord
	 * @param tx image x-coord
	 * @param ty image y-coord
	 * @param w width in pixels
	 * @param h height in pixels
	 */
	public void drawRect(MatrixStack stack, int x, int y, int tx, int ty, int w, int h) {
		Matrix4f mat = stack.last().pose();
		BufferBuilder b = getDraw();
		int X = x + w, Y = y + h;
		float u = (float)tx / (float)texW, U = (float)(tx + w) / (float)texW,
				v = (float)ty / (float)texH, V = (float)(ty + h) / (float)texH,
				z = zLevel;
		b.vertex(mat, x, Y, z).uv(u, V).endVertex();
		b.vertex(mat, X, Y, z).uv(U, V).endVertex();
		b.vertex(mat, X, y, z).uv(U, v).endVertex();
		b.vertex(mat, x, y, z).uv(u, v).endVertex();
	}

	/**
	 * draw a tooltip overlay
	 * @param text text lines
	 * @param mx mouse X position
	 * @param my mouse Y position
	 * @see GuiUtils#drawHoveringText(List, int, int, int, int, int, FontRenderer)
	 */
	public void drawTooltip(MatrixStack stack, List<String> text, int mx, int my) {
		drawHoveringText(stack, convertText(text), mx, my, screenWidth, screenHeight, -1, fontRenderer);
	}

	/**
	 * draw a tooltip overlay
	 * @param text text, where lines are separated by the '\n' character
	 * @param mx mouse X position
	 * @param my mouse Y position
	 * @see GuiUtils#drawHoveringText(List, int, int, int, int, int, FontRenderer)
	 */
	public void drawTooltip(MatrixStack stack, String text, int mx, int my) {
		drawHoveringText(stack, convertText(text), mx, my, screenWidth, screenHeight, -1, fontRenderer);
	}

}
