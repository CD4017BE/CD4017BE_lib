package cd4017be.lib.render;

import java.util.HashMap;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import cd4017be.lib.Gui.inWorld.InWorldUITile;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class InWorldUIRenderer extends TileEntitySpecialRenderer<InWorldUITile> {

	public static final InWorldUIRenderer instance = new InWorldUIRenderer();
	public static final HashMap<Class<?>, Gui<?>> registry = new HashMap<Class<?>, Gui<?>>();
	public static float TOOLTIP_SCALE = 1F/256F;

	public RenderItem itemRenderer;

	public static <T extends InWorldUITile> void register(Class<T> tile, Gui<T> gui) {
		registry.put(tile, gui);
		ClientRegistry.bindTileEntitySpecialRenderer(tile, instance);
	}

	@SuppressWarnings("unchecked")
	private <T extends InWorldUITile> Gui<T> getGui(T tile) {
		return (Gui<T>) registry.get(tile.getClass());
	}

	@Override
	public void render(InWorldUITile tile, double x, double y, double z, float t, int destroyStage, float alpha) {
		GlStateManager.pushMatrix();
		Util.moveAndOrientToBlock(x, y, z, tile.getOrientation());
		RayTraceResult target = tile.getAimTarget(rendererDispatcher.entity, t);
		Gui<InWorldUITile> r = getGui(tile);
		r.renderComponents(tile, target, t);
		if (target != null) {
			String s = r.getTooltip(tile, target.subHit);
			if (s != null) {
/*TODO verify generated code*/				Util.moveAndOrientToBlock(target.hitVec.x - 1, target.hitVec.y - 1, target.hitVec.z - 1, Orientation.fromFacing(target.sideHit));
				GlStateManager.scale(-TOOLTIP_SCALE, -TOOLTIP_SCALE, -TOOLTIP_SCALE);
				renderToolTip(0, -10, 0xffc0c0c0, 0x80100020, s.split("\n"));
			}
		}
		GlStateManager.popMatrix();
	}

	private void renderToolTip(int x, int y, int tc, int bc, String... lines) {
		GlStateManager.disableLighting();
		FontRenderer fr = getFontRenderer();
		int width = 0, height = lines.length * fr.FONT_HEIGHT;
		int[] w = new int[lines.length];
		for (int i = 0; i < w.length; i++) {
			int l = fr.getStringWidth(lines[i]);
			w[i] = l;
			if (l > width) width = l;
		}
		int x0 = Float.floatToIntBits(x - width / 2 - 5), x1 = Float.floatToIntBits(x - width / 2), x2 = Float.floatToIntBits(x + width / 2), x3 = Float.floatToIntBits(x + width / 2 + 5);
		int y0 = Float.floatToIntBits(y - height - 5), y1 = Float.floatToIntBits(y - height), y2 = Float.floatToIntBits(y), y3 = Float.floatToIntBits(y + 5);
		int z = 0;
		int c1 = (bc & 0xff00ff00) | (bc >> 16 & 0xff) | (bc & 0xff) << 16, c0 = c1;// & 0xffffff;
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.disableTexture2D();
		GlStateManager.depthFunc(GL11.GL_ALWAYS);
		BufferBuilder buff = Tessellator.getInstance().getBuffer();
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		buff.addVertexData(new int[]{ //background frame
				  x1, y2, z, c1,  x2, y2, z, c1,  x2, y1, z, c1,  x1, y1, z, c1, //center
				  x1, y1, z, c1,  x2, y1, z, c1,  x3, y0, z, c0,  x0, y0, z, c0, //up fade out
				  x2, y1, z, c1,  x2, y2, z, c1,  x3, y3, z, c0,  x3, y0, z, c0, //right fade out
				  x2, y2, z, c1,  x1, y2, z, c1,  x0, y3, z, c0,  x3, y3, z, c0, //down fade out
				  x1, y2, z, c1,  x1, y1, z, c1,  x0, y0, z, c0,  x0, y3, z, c0, //left fade out
			});
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
		for (int i = 0; i < w.length; i++)
			fr.drawString(lines[i], x - w[i] / 2, y - height + i * fr.FONT_HEIGHT, tc);
		GlStateManager.depthFunc(GL11.GL_LEQUAL);
	}

	@Override
	public void bindTexture(ResourceLocation location) {
		super.bindTexture(location);
	}

	public interface Gui<T extends InWorldUITile> {

		public void renderComponents(T tile, RayTraceResult aim, float t);

		public @Nullable String getTooltip(T tile, int comp);

	}

}
