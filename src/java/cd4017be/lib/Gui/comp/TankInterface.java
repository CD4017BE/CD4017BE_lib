package cd4017be.lib.Gui.comp;

import java.util.ArrayList;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;

import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiUtils;

/**
 * Displays the content of a fluid tank element.
 * @author CD4017BE
 *
 */
public class TankInterface extends GuiCompBase<GuiCompGroup> {

	private final TankSlot slot;
	private final IntConsumer handler;

	/**
	 * @param parent the gui-component container this will register to
	 * @param slot the slot providing access to the tank
	 * @param handler optional handler when a fluid container is dropped into this component (contained fluid, tank id).
	 */
	public TankInterface(GuiCompGroup parent, TankSlot slot, @Nullable IntConsumer handler) {
		super(parent, slot.xPos, slot.yPos, (slot.size >> 4 & 0xf) * 18 - 2, (slot.size & 0xf) * 18 - 2);
		this.slot = slot;
		this.handler = handler;
	}

	@Override
	public void drawOverlay(int mx, int my) {
		FluidStack stack = slot.getStack();
		ArrayList<String> info = new ArrayList<String>();
		info.add(stack != null ? stack.getLocalizedName() : TooltipUtil.translate("cd4017be.tankEmpty"));
		info.add(TooltipUtil.format("cd4017be.tankAmount", stack != null ? (double)stack.amount / 1000D : 0D, (double)slot.inventory.getCapacity(slot.tankNumber) / 1000D));
		parent.drawTooltip(info, mx, my);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		GlStateManager.disableAlpha();
		ResourceLocation res;
		FluidStack stack = slot.getStack();
		if (stack != null && ((res = stack.getFluid().getStill(stack)) != null || (res = stack.getFluid().getFlowing(stack)) != null)) {
			int c = slot.inventory.getCapacity(slot.tankNumber);
			int n = c == 0 || stack.amount >= c ? h : (int)((long)h * (long)stack.amount / (long)c);
			ModularGui.color(stack.getFluid().getColor(stack));
			Minecraft mc = Minecraft.getMinecraft();
			TextureAtlasSprite tex = mc.getTextureMapBlocks().getAtlasSprite(res.toString());
			float u = tex.getMinU(), v = tex.getMinV();
			parent.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			GuiScreen.drawModalRectWithCustomSizedTexture(x, y + h - n, u, v, w, n, tex.getMaxU() - u, tex.getMaxV() - v);
		}
		ModularGui.color(0xffffffff);
		parent.bindTexture(ModularGui.LIB_TEX);
		GuiUtils.drawTexturedModalRect(x + w - 16, y, 110, 52 - h, 16, h, parent.zLevel);
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
	}

	@Override
	public boolean mouseIn(int x, int y, int b, byte d) {
		if (d == A_DOWN && handler != null)
			handler.accept(slot.tankNumber);
		return false;
	}
}
