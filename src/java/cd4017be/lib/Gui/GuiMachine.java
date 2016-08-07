/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.Gui;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author CD4017BE
 */
public abstract class GuiMachine extends GuiContainer
{
	public static final ResourceLocation LIB_TEX = new ResourceLocation("lib", "textures/icons.png");
	public ResourceLocation MAIN_TEX;
	public int focus = -1, tabsX = 0, tabsY = 7, bgTexX = 0, bgTexY = 0;
	public ArrayList<GuiComp> guiComps = new ArrayList<GuiComp>();
	private Slot lastClickSlot;

	public GuiMachine(Container container) {
		super(container);
	}

	@Override
	public void initGui() {
		super.initGui();
		if (inventorySlots instanceof TileContainer) {
			TileContainer cont = (TileContainer)inventorySlots;
			for (TankSlot slot : cont.tankSlots)
				guiComps.add(new FluidTank(guiComps.size(), slot));
			if (cont.data instanceof AutomatedTile) {
				AutomatedTile tile = (AutomatedTile)cont.data;
				if (tile.tanks != null) guiComps.add(new FluidSideCfg(guiComps.size(), tabsX -= tile.tanks.tanks.length * 9 + 9, tabsY, tile));
				if (tile.inventory != null) guiComps.add(new ItemSideCfg(guiComps.size(), tabsX -= tile.inventory.groups.length * 9 + 9, tabsY, tile));
				if (tile.energy != null) guiComps.add(new EnergySideCfg(guiComps.size(), tabsX - 18, tabsY, tile));
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableDepth();
		GlStateManager.disableAlpha();
		GlStateManager.enableBlend();
		for (GuiComp comp : guiComps)
			if (comp.isInside(mx, my)) {
				comp.drawOverlay(mx, my);
				break;
			}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		mc.renderEngine.bindTexture(MAIN_TEX);
		this.drawTexturedModalRect(guiLeft, guiTop, bgTexX, bgTexY, xSize, ySize);
		for (GuiComp comp : guiComps) comp.draw();
		if (inventorySlots instanceof TileContainer) {
			TileContainer cont = (TileContainer)inventorySlots;
			if (cont.invPlayerS != cont.invPlayerE) {
				Slot pos = cont.inventorySlots.get(cont.invPlayerS);
				this.drawStringCentered(I18n.translateToLocal("container.inventory"), this.guiLeft + pos.xDisplayPosition + 80, this.guiTop + pos.yDisplayPosition - 14, 0x404040);
			}
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		for (GuiComp comp : guiComps) 
			if (comp.isInside(x, y)) {
				if (comp.id != focus) this.setFocus(comp.id);
				comp.mouseIn(x, y, b, 0);
				break;
			}
		if (focus >= 0 && !guiComps.get(focus).isInside(x, y)) this.setFocus(-1);
		super.mouseClicked(x, y, b);
	}

	@Override
	protected void mouseClickMove(int x, int y, int b, long t) {
		if (focus >= 0) guiComps.get(focus).mouseIn(x, y, b, 1);
		else {
			Slot slot = this.getSlotUnderMouse();
			ItemStack itemstack = this.mc.thePlayer.inventory.getItemStack();
			if (slot instanceof SlotHolo && slot != lastClickSlot) {
				if (itemstack == null || slot.getStack() == null || itemstack.isItemEqual(slot.getStack()))
					this.handleMouseClick(slot, slot.slotNumber, b, ClickType.PICKUP);
			} else super.mouseClickMove(x, y, b, t);
			lastClickSlot = slot;
		}
	}

	@Override
	protected void mouseReleased(int x, int y, int b) {
		if (focus >= 0) guiComps.get(focus).mouseIn(x, y, b, 2);
		super.mouseReleased(x, y, b);
		lastClickSlot = null;
	}

	public void drawFormatInfo(int x, int y, String key, Object... args) {
		this.drawHoveringText(Arrays.asList(TooltipInfo.format("gui.cd4017be." + key, args).split("\n")), x, y, fontRendererObj);
	}

	public void drawLocString(int x, int y, int h, int c, String s, Object... args) {
		String[] text = TooltipInfo.format("gui.cd4017be." + s, args).split("\n");
		for (String l : text) {
			this.fontRendererObj.drawString(l, x, y, c);
			y += h;
		}
	}

	public void drawStringCentered(String s, int x, int y, int c) {
		this.fontRendererObj.drawString(s, x - this.fontRendererObj.getStringWidth(s) / 2, y, c);
	}

	protected void drawSideCube(int x, int y, int s, byte dir) {
		GlStateManager.enableDepth();
		this.drawGradientRect(x, y, x + 64, y + 64, 0xff000000, 0xff000000);
		this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GL11.glPushMatrix();
		GL11.glTranslatef(x + 32, y + 32, this.zLevel + 32);
		GL11.glScalef(16F, -16F, 16F);
		EntityPlayer player = ((TileContainer)this.inventorySlots).player;
		GL11.glRotatef(player.rotationPitch, 1, 0, 0);
		GL11.glRotatef(player.rotationYaw + 90, 0, 1, 0);
		GL11.glTranslatef(-0.5F, -0.5F, 0.5F);
		TileEntity tile = (TileEntity)((DataContainer)this.inventorySlots).data;
		this.mc.getBlockRendererDispatcher().renderBlockBrightness(tile.getWorld().getBlockState(tile.getPos()), 1);
		//GL11.glRotatef(-90, 0, 1, 0);
		this.mc.renderEngine.bindTexture(LIB_TEX);
		Vec3 p = Vec3.Def(0.5, 0.5, 0.5), a, b;
		switch(s) {
		case 0: a = Vec3.Def(0, -1, 0); break;
		case 1: a = Vec3.Def(0, 1, 0); break;
		case 2: a = Vec3.Def(0, 0, -1); break;
		case 3: a = Vec3.Def(0, 0, 1); break;
		case 4: a = Vec3.Def(-1, 0, 0); break;
		default: a = Vec3.Def(1, 0, 0);
		}
		Vec3d look = player.getLookVec();
		b = Vec3.Def(look.xCoord, look.yCoord, look.zCoord).mult(a).norm();
		p = p.add(a.scale(0.5)).add(b.scale(-0.5));
		a = a.scale(1.5);
		final float tx = (float)(144 + 16 * dir) / 256F, dtx = 16F / 256F, ty = 24F / 256F, dty = 8F / 256F;
		
		VertexBuffer t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		t.pos(p.x + b.x, p.y + b.y, p.z + b.z).tex(tx, ty + dty).endVertex();
		t.pos(p.x + a.x + b.x, p.y + a.y + b.y, p.z + a.z + b.z).tex(tx + dtx, ty + dty).endVertex();
		t.pos(p.x + a.x, p.y + a.y, p.z + a.z).tex(tx + dtx, ty).endVertex();
		t.pos(p.x, p.y, p.z).tex(tx, ty).endVertex();
		Tessellator.getInstance().draw();
		GL11.glPopMatrix();
	}

	public void setFocus(int id) {
		if (focus >= 0 && focus < guiComps.size()) guiComps.get(focus).unfocus();
		focus = id > 0 && id < guiComps.size() && guiComps.get(id).focus() ? id : -1;
	}

	protected Object getDisplVar(int id) {return null;}
	protected void setDisplVar(int id, Object obj, boolean send) {}

	public class GuiComp {
		public final int id, px, py, w, h;
		public String tooltip;
		public GuiComp(int id, int px, int py, int w, int h) {
			this.id = id;
			this.px = px + guiLeft;
			this.py = py + guiTop;
			this.w = w; this.h = h;
		}
		public GuiComp setTooltip(String s) {
			this.tooltip = s;
			return this;
		}
		public boolean isInside(int x, int y) {
			return x >= px && x < px + w && y >= py && y < py + w;
		}
		public void drawOverlay(int mx, int my) {
			if (tooltip == null) return;
			String text;
			if (tooltip.startsWith("x*")) {
				int p = tooltip.indexOf('+', 2), q = tooltip.indexOf(';', p);
				float f = (Float)getDisplVar(id) * Float.parseFloat(tooltip.substring(2, p)) + Float.parseFloat(tooltip.substring(p + 1, q));
				text = TooltipInfo.format(tooltip.substring(p + 1), f);
			} else text = TooltipInfo.getLocFormat("gui.cd4017be." + tooltip.replace("#", getDisplVar(id).toString()));
			drawHoveringText(Arrays.asList(text.split("\n")), mx - guiLeft, py + h - guiTop, fontRendererObj);
		}
		public void draw() {}
		public void keyTyped(char c, int k) {}
		public void mouseIn(int x, int y, int b, int d) {}
		public void unfocus() {}
		public boolean focus() {return false;}
	}

	public class TextField extends GuiComp{
		public final int maxL;
		public int tc = 0xff404040, cc = 0xff800000;
		public String text;
		public int cur;

		public TextField(int id, int x, int y, int w, int h, int max) {
			super(id, x, y, w, h);
			this.maxL = max;
		}

		public TextField color(int text, int cursor) {
			this.tc = text; this.cc = cursor;
			return this;
		}

		@Override
		public void draw() {
			if (focus == id) {
				if (cur > text.length()) cur = text.length();
				drawVerticalLine(px - 1 + fontRendererObj.getStringWidth(text.substring(0, cur)), py, py + 7, cc);
			} else text = (String)getDisplVar(id);
			fontRendererObj.drawString(text, px, py, tc);
		}

		@Override
		public void keyTyped(char c, int k) {
			try {
				if (k == Keyboard.KEY_LEFT && cur > 0) cur--;
				else if (k == Keyboard.KEY_RIGHT && cur < text.length()) cur++;
				else if (k == Keyboard.KEY_DELETE && cur < text.length()){
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} else if (k == Keyboard.KEY_BACK && cur > 0) {
					cur--;
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} else if (k == Keyboard.KEY_RETURN) {
					setFocus(-1);
				} else if (k == Keyboard.KEY_UP) {
					setFocus(id - 1);
				} else if (k == Keyboard.KEY_DOWN) {
					setFocus(id + 1);
				} else if (ChatAllowedCharacters.isAllowedCharacter(c) && cur < maxL){
					text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL - 1)));
					cur++;
				}
			} catch (IndexOutOfBoundsException e) {
				if (cur < 0) cur = 0;
				if (cur > text.length()) cur = text.length();
			}
		}

		@Override
		public void unfocus() {
			setDisplVar(id, text, true);
		}

		@Override
		public boolean focus() {
			cur = text.length();
			return true;
		}

	}

	public class Slider extends GuiComp {
		public final int l, tx, ty, tw, th;
		public final boolean hor;

		public Slider(int id, int x, int y, int l, int texX, int texY, int texW, int texH, boolean hor) {
			super(id, x, y, hor?l:texW, hor?texH:l);
			this.hor = hor;
			this.l = l;
			this.tx = texX;
			this.ty = texY;
			this.tw = texW;
			this.th = texH;
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			int f = (int)((Float)getDisplVar(id) * (float)l - 0.5F * (float)(hor?tw:th));
			drawTexturedModalRect(hor? px + f : px, hor? py : py + f, tx, ty, tw, th);
		}

		@Override
		public void mouseIn(int x, int y, int b, int d) {
			float f = ((float)(hor? x - px : y - py) + 0.5F) / (float)l;
			if (f < 0) f = 0;
			else if (f > 1) f = 1;
			setDisplVar(id, f, false);
			if (d == 2) setFocus(-1);
		}

		@Override
		public void unfocus() {
			setDisplVar(id, null, true);
		}

		@Override
		public boolean focus() {return true;}

	}

	public class Button extends GuiComp {

		public final int states;
		public int tx, ty;

		public Button(int id, int px, int py, int w, int h, int states) {
			super(id, px, py, w, h);
			this.states = states;
		}

		public Button texture(int tx, int ty) {
			this.tx = tx;
			this.ty = ty;
			return this;
		}

		@Override
		public void draw() {
			if (states == 1) return;
			int s = (Integer)getDisplVar(id);
			mc.renderEngine.bindTexture(MAIN_TEX);
			drawTexturedModalRect(px, py, tx, ty + s * h, w, h);
		}

		@Override
		public void mouseIn(int x, int y, int b, int d) {
			setDisplVar(id, b, true);
		}

	}

	public class ProgressBar extends GuiComp {
		/** 0:horFrac, 1:vertFrac, 2:horShift, 3:vertShift, 4:precision*/
		public final byte type;
		public final int tx, ty;

		public ProgressBar(int id, int px, int py, int w, int h, int tx, int ty, byte type) {
			super(id, px, py, w, h);
			this.type = type;
			this.tx = tx;
			this.ty = ty;
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			float f = (Float)getDisplVar(id);
			boolean v = (type & 1) != 0;
			if (type == 0 || type == 1) {
				int n = (int)((float)(v?h:w) * (f<0?-f:f));
				int dx = (!v && f<0)? w - n : 0, dy = (v && f>0)? h - n : 0;
				drawTexturedModalRect(px + dx, py + dy, tx + dx, ty + dy, v?w:n, v?n:h);
			} else if (type == 2 || type == 3) {
				int n = (int)((float)(v?h:w) * f);
				drawTexturedModalRect(px, py, v? tx : tx + n, v? ty + n : ty, n, n);
			} else if (type == 4) {
				int n = (int)((float)(w * h) * f), m = n / h;
				drawTexturedModalRect(px, py, tx, ty, m, h);
				drawTexturedModalRect(px + m, py, tx + m, ty, 1, n % h);
			}
		}

	}

	public class EnergySideCfg extends GuiComp {
		final AutomatedTile tile;

		public EnergySideCfg(int id, int px, int py, AutomatedTile tile) {
			super(id, px, py, 18, 63);
			this.tile = tile;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int s = (my - py - 9) / 9;
			if (s >= 0 && mx >= px + 9)
				drawSideCube(-64, py + 63, s, (tile.energy.sideCfg >> s & 1) != 0 ? (byte)3 : 0);
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px, py, 0, 0, 18, 63);
			drawTexturedModalRect(px, py, 0, 99, 9, 9);
			drawTexturedModalRect(px + 9, py, 36, 90, 9, 9);
			for (int i = 0; i < 6; i++)
				drawTexturedModalRect(px + 9, py + 9 + i * 9, 18 - (tile.energy.sideCfg >> i & 1) * 9, 81, 9, 9);
		}

		@Override
		public void mouseIn(int x, int y, int b, int d) {
			if (x >= px + 9 && (y -= py + 9) >= 0) {
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(3);
				dos.writeByte(tile.energy.sideCfg ^= 1 << (y / 9));
				BlockGuiHandler.sendPacketToServer(dos);
			}
		}

	}

	public class FluidSideCfg extends GuiComp {
		final AutomatedTile tile;

		public FluidSideCfg(int id, int px, int py, AutomatedTile tile) {
			super(id, px, py, 9 * tile.tanks.tanks.length + 9, 81);
			this.tile = tile;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int s = (my - py - 9) / 9;
			int i = (mx - px - 9) / 9;
			if (i >= 0 && s >= 0) {
				mc.renderEngine.bindTexture(LIB_TEX);
				byte dir = s < 6 ? tile.tanks.getConfig(s, i) : s != 6 ? (byte)4 : tile.tanks.isLocked(i) ? (byte)5 : (byte)6; 
				for (TankSlot slot : ((TileContainer)inventorySlots).tankSlots)
					if (slot.tankNumber == i)
						drawTexturedModalRect(slot.xDisplayPosition + (slot.size >> 4 & 0xf) * 9 - 9, slot.yDisplayPosition + (slot.size & 0xf) * 18 - (s<6?10:18), 144 + dir * 16, 16, 16, s<6?8:16);
				if(s < 6) drawSideCube(-64, py + 63, s, dir);
			}
		}

		@Override
		public void draw() {
			int s = tile.tanks.tanks.length;
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px, py, 0, 0, 9 + s * 9, 81);
			drawTexturedModalRect(px, py, 0, 81, 9, 9);
			for (int j = 0; j < s; j++) {
				drawTexturedModalRect(px + 9 + j * 9, py, 18 + tile.tanks.tanks[j].dir * 9, 90, 9, 9);
				for (int i = 0; i < 6; i++)
					drawTexturedModalRect(px + 9 + j * 9, py + 9 + i * 9, 9 + (int)(tile.tanks.sideCfg >> (8 * i + 2 * j) & 3) * 9, 81, 9, 9);
				if ((tile.tanks.sideCfg >> (48 + j) & 1) != 0)
					drawTexturedModalRect(px + 9 + j * 9, py + 63, 9, 81, 9, 9);
			}
		}

		@Override
		public void mouseIn(int x, int y, int b, int d) {
			x = (x - px) / 9 - 1;
			y = (y - py) / 9 - 1;
			if (x >= 0 && y >= 0) {
				PacketBuffer dos = tile.getPacketTargetData();
				if (y == 7) dos.writeByte(2).writeByte(x);
				else if (y == 6) dos.writeByte(1).writeLong(tile.tanks.sideCfg ^= 1 << (48 + x));
				else {
					int p = y * 8 + x * 2;
					long sp = 3L << p;
					dos.writeByte(1).writeLong(tile.tanks.sideCfg = (tile.tanks.sideCfg & ~sp) | (tile.tanks.sideCfg + (b == 0 ? 1L << p : sp) & sp));
				}
				BlockGuiHandler.sendPacketToServer(dos);
			}
		}

	}

	public class ItemSideCfg extends GuiComp {
		final AutomatedTile tile;

		public ItemSideCfg(int id, int px, int py, AutomatedTile tile) {
			super(id, px, py, 9 * tile.inventory.groups.length + 9, 63);
			this.tile = tile;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			int s = (my - py - 9) / 9;
			int i = (mx - px - 9) / 9;
			if (s >= 0 && i >= 0) {
				mc.renderEngine.bindTexture(LIB_TEX);
				byte dir = tile.inventory.getConfig(s, id);
				int i0 = tile.inventory.groups[id].s, i1 = tile.inventory.groups[id].e;
				for (Slot slot : inventorySlots.inventorySlots)
					if (slot.getSlotIndex() >= i0 && slot.getSlotIndex() < i1)
						drawTexturedModalRect(slot.xDisplayPosition, slot.yDisplayPosition, 144 + dir * 16, 0, 16, 16);
				drawSideCube(-64, py + 63, s, dir);
			}
		}

		@Override
		public void draw() {
			int s = tile.inventory.groups.length;
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px, py, 0, 0, 9 + s * 9, 63);
			drawTexturedModalRect(px, py, 0, 90, 9, 9);
			for (int j = 0; j < s; j++) {
				drawTexturedModalRect(px + 9 + j * 9, py, 18 + tile.inventory.groups[j].dir * 9, 90, 9, 9);
				for (int i = 0; i < 6; i++)
					drawTexturedModalRect(px + 9 + j * 9, py + 9 + i * 9, 9 + (int)(tile.inventory.sideCfg >> (10 * i + 2 * j) & 3) * 9, 81, 9, 9);
			}
		}

		@Override
		public void mouseIn(int x, int y, int b, int d) {
			x = (x - px) / 9 - 1;
			y = (y - py) / 9 - 1;
			if (x >= 0 && y >= 0) {
				int p = y * 10 + x * 2;
				long sp = 3L << p;
				PacketBuffer dos = tile.getPacketTargetData();
				dos.writeByte(0);
				dos.writeLong(tile.inventory.sideCfg = (tile.inventory.sideCfg & ~sp) | (tile.inventory.sideCfg + (b == 0 ? 1L << p : sp) & sp));
				BlockGuiHandler.sendPacketToServer(dos);
			}
		}

	}

	public class FluidTank extends GuiComp {
		final TankSlot slot;

		public FluidTank(int id, TankSlot slot) {
			super(id, slot.xDisplayPosition + guiLeft, slot.yDisplayPosition + guiTop, (slot.size >> 4 & 0xf) * 18 - 2, (slot.size & 0xf) * 18 - 2);
			this.slot = slot;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			FluidStack stack = slot.getStack();
			ArrayList<String> info = new ArrayList<String>();
			info.add(stack != null ? stack.getLocalizedName() : "Empty");
			info.add(String.format("%s/%s ", Utils.formatNumber(stack != null ? (float)stack.amount / 1000F : 0F, 3), Utils.formatNumber((float)slot.inventory.tanks[slot.tankNumber].cap / 1000F, 3)) + TooltipInfo.getFluidUnit());
			drawHoveringText(info, px, py, fontRendererObj);
		}

		@Override
		public void draw() {
			GlStateManager.disableAlpha();
			GlStateManager.enableBlend();
			ResourceLocation res;
			FluidStack stack = slot.getStack();
			if (stack != null && (res = stack.getFluid().getStill()) != null) {
				mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				int n = (int)((long)h * (long)stack.amount / (long)slot.inventory.tanks[slot.tankNumber].cap);
				drawTexturedModalRect(px, py + h - n, mc.getTextureMapBlocks().getAtlasSprite(res.toString()), w, n);
			}
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px + w - 16, py, 110, 0, 18, 52);
			GlStateManager.disableBlend();
			GlStateManager.enableAlpha();
		}

	}

}
