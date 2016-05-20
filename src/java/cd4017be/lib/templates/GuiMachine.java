/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TileContainer.TankSlot;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author CD4017BE
 */
public abstract class GuiMachine extends GuiContainer
{
	
	public class TextField {
		public final int maxL;
		public String text;
		public int cur;
		public TextField(String text, int max) {
			this.maxL = max;
			this.text = text;
			this.cur = text.length();
		}
		/**
		 * Draws text and cursor
		 * @param x screen x coord
		 * @param y screen y coord
		 * @param ct text color
		 * @param cc cursor color
		 */
		public void draw(int x, int y, int ct, int cc) {
			GuiMachine.this.drawVerticalLine(x - 1 + GuiMachine.this.fontRendererObj.getStringWidth(text.substring(0, cur)), y, y + 7, cc);
			GuiMachine.this.fontRendererObj.drawString(text, x, y, ct);
		}
		/**
		 * Call this to type text in
		 * @param c the char typed
		 * @param k the pressed key id
		 * @return -1 = continue, 0 = exit to previous, 1 = exit normal 2 = exit to next 
		 */
		public byte keyTyped(char c, int k) {
			try {
				if (k == Keyboard.KEY_LEFT && cur > 0) cur--;
				else if (k == Keyboard.KEY_RIGHT && cur < text.length()) cur++;
				else if (k == Keyboard.KEY_DELETE && cur < text.length()){
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} else if (k == Keyboard.KEY_BACK && cur > 0) {
					cur--;
					text = text.substring(0, cur).concat(text.substring(cur + 1));
				} else if (k == Keyboard.KEY_RETURN) {
					return 1;
				} else if (k == Keyboard.KEY_UP) {
					return 0;
				} else if (k == Keyboard.KEY_DOWN) {
					return 2;
				} else if (ChatAllowedCharacters.isAllowedCharacter(c) && cur < maxL){
					text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL - 1)));
					cur++;
				}
			} catch (IndexOutOfBoundsException e) {
				if (cur < 0) cur = 0;
				if (cur > text.length()) cur = text.length();
			}
			return -1;
		}
	}
    
    public GuiMachine(Container container)
    {
        super(container);
        if (inventorySlots instanceof TileContainer && ((TileContainer)inventorySlots).tileEntity instanceof AutomatedTile) 
        	this.tile = (AutomatedTile)((TileContainer)inventorySlots).tileEntity;
        else this.tile = null;
    }
    
    protected final AutomatedTile tile;
    protected int mouseX;
    protected int mouseY;
    protected int selTank = -1;
    
    @Override
    public void drawScreen(int mx, int my, float par3) 
    {
        this.mouseX = mx;
        this.mouseY = my;
        super.drawScreen(mx, my, par3);
    }
    
    public int GuiLeft()
    {
        return guiLeft;
    }
    
    public int GuiTop()
    {
        return guiTop;
    }
    
    public void drawStringCentered(String s, int x, int y, int c)
    {
        this.fontRendererObj.drawString(s, x - this.fontRendererObj.getStringWidth(s) / 2, y, c);
    }
    
    public void drawLocString(int x, int y, int h, int c, String s, Object... args)
    {
    	String[] text = TooltipInfo.format("gui.cd4017be." + s, args).split("\n");
    	for (String l : text) {
    		this.fontRendererObj.drawString(l, x, y, c);
    		y += h;
    	}
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
        if (selTank >= 0) drawTankInfo(selTank, mx - this.guiLeft, my - this.guiTop);
        selTank = -1;
        if (tile != null && this.isPointInRegion(tabsX, tabsY, 0 - tabsX, 81, mx, my)) {
            GlStateManager.color(1, 1, 1, 1);
        	GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
        	int s = (my - this.guiTop - tabsY - 9) / 9;
			mx -= this.guiLeft;
			if (s < 0 || mx >= 0) return;
			this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
			byte dir = 0;
			int id;
			if (FtabX <= mx) {
				id = mx - FtabX - 9;
				if (id >= 0) {
					id /= 9;
					dir = s < 6 ? tile.tanks.getConfig(s, id) : s != 6 ? (byte)4 : tile.tanks.isLocked(id) ? (byte)5 : (byte)6; 
					for (TankSlot slot : ((TileContainer)this.inventorySlots).tankSlots)
						if (slot.tankNumber == id)
							this.drawTexturedModalRect(slot.xDisplayPosition + (slot.bigSize ? 9 : 0), slot.yDisplayPosition + (s<6?44:36), 144 + dir * 16, 16, 16, s<6?8:16);
				}
			} else if (s < 6 && ItabX <= mx) {
				id = mx - ItabX - 9;
				if (id >= 0) {
					id /= 9;
					dir = tile.inventory.getConfig(tile.netData.longs[tile.inventory.netIdxLong], s, id);
					int i0 = tile.inventory.componets[id].s, i1 = tile.inventory.componets[id].e;
					for (Slot slot : this.inventorySlots.inventorySlots)
						if (slot.inventory == tile && slot.getSlotIndex() >= i0 && slot.getSlotIndex() < i1)
							this.drawTexturedModalRect(slot.xDisplayPosition, slot.yDisplayPosition, 144 + dir * 16, 0, 16, 16);
				}
			} else if (s < 6 && EtabX + 9 <= mx) {
				dir = (byte)(~tile.energy.con >> s & 1);
			}
			if(s < 6) this.drawSideCube(-64, tabsY + 63, s, dir);
		}
    }
    
    protected void drawSideCube(int x, int y, int s, byte dir) {
    	GlStateManager.enableDepth();
		this.drawGradientRect(x, y, x + 64, y + 64, 0xff000000, 0xff000000);
		this.mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
    	GL11.glPushMatrix();
    	GL11.glTranslatef(x + 32, y + 32, this.zLevel + 32);
    	GL11.glScalef(16F, -16F, 16F);
    	EntityPlayer player = ((TileContainer)this.inventorySlots).player;
    	GL11.glRotatef(player.rotationPitch, 1, 0, 0);
    	GL11.glRotatef(player.rotationYaw + 90, 0, 1, 0);
    	GL11.glTranslatef(-0.5F, -0.5F, 0.5F);
    	this.mc.getBlockRendererDispatcher().renderBlockBrightness(tile.getWorld().getBlockState(tile.getPos()), 1);
    	//GL11.glRotatef(-90, 0, 1, 0);
    	this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
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
    
    protected int tabsY = 7, EtabX = 0, FtabX = 0, ItabX = 0, tabsX = 0;
    
    @Override
	public void initGui() {
		super.initGui();
		if (tile == null) return;
		tabsX = 0;
		if (tile.tanks != null && tile.tanks.tanks.length > 0) FtabX = (tabsX -= 9 + tile.tanks.tanks.length * 9);
		if (tile.inventory != null && tile.inventory.componets.length > 0) ItabX = (tabsX -= 9 + tile.inventory.componets.length * 9);
		if (tile.energy != null) EtabX = (tabsX -= 18);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		if (this.inventorySlots instanceof TileContainer)
			for (TankSlot slot : ((TileContainer)this.inventorySlots).tankSlots)
				this.drawLiquidTank(slot.inventory, slot.tankNumber, slot.xDisplayPosition, slot.yDisplayPosition, slot.bigSize);
		if (FtabX < 0) this.drawLiquidConfig(tile, FtabX, tabsY);
		if (ItabX < 0) this.drawItemConfig(tile, ItabX, tabsY);
		if (EtabX < 0) this.drawEnergyConfig(tile, EtabX, tabsY);
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		if (FtabX < 0) this.clickLiquidConfig(tile, x - guiLeft - FtabX, y - guiTop - tabsY);
		if (ItabX < 0) this.clickItemConfig(tile, x - guiLeft - ItabX, y - guiTop - tabsY);
		if (EtabX < 0) this.clickEnergyConfig(tile, x - guiLeft - EtabX, y - guiTop - tabsY);
		super.mouseClicked(x, y, b);
	}

	public void drawLiquidTank(TankContainer tanks, int id, int x, int y, boolean s) 
    {
		GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        x += this.guiLeft;
        y += this.guiTop;
        this.drawTexturedModalRect(x - 1, y - 1, s?74:56, 0, s?36:18, 52);
        this.drawTexturedModalRect(x - 1, y + 51, s?74:56, 52 + 2 * (id & 3), s?36:18, 2);
        TextureAtlasSprite tex = null;
        if (tanks.getFluid(id) != null) {
            ResourceLocation res = tanks.getFluid(id).getFluid().getStill();
        	if (res != null) tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(res.toString());
        }
        if (tex != null) {
            this.mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            float n = 50F - (float)tanks.getAmount(id) / (float)tanks.tanks[id].cap * 50F;
            float u = tex.getMinU();
            float v = tex.getMinV();
            float u1 = tex.getMaxU();
            float v1 = tex.getMaxV();
            VertexBuffer r = Tessellator.getInstance().getBuffer();
            r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            r.pos(x, y + 50, this.zLevel).tex(u, v1).endVertex();
            r.pos(x + (s?34:16), y + 50, this.zLevel).tex(u1, v1).endVertex();
            r.pos(x + (s?34:16), y + n, this.zLevel).tex(u1, v).endVertex();
            r.pos(x, y + n, this.zLevel).tex(u, v).endVertex();
            Tessellator.getInstance().draw();
        }
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        this.drawTexturedModalRect(x + (s?17:-1), y - 1, 110, 0, 18, 52);
        if (this.isPointInRegion(x - guiLeft, y - guiTop, s?34:16, 50, mouseX, mouseY)){
            selTank = id;
        }
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }
    
    public void drawInfo(int x, int y, int w, int h, String... text)
    {
        if (this.isPointInRegion(x, y, w, h, mouseX, mouseY)) {
            if (text.length == 2 && text[0].equals("\\i")) {
                String s = TooltipInfo.getLocFormat("gui.cd4017be." + text[1]);
                if (s == null) return;
                text = s.split("\n");
            }
            this.drawHoveringText(Arrays.asList(text), mouseX - this.guiLeft, mouseY - this.guiTop, fontRendererObj);
        }
    }
    
    public void drawFormatInfo(int x, int y, int w, int h, String key, Object... args)
    {
    	if (this.isPointInRegion(x, y, w, h, mouseX, mouseY)) {
            this.drawHoveringText(Arrays.asList(TooltipInfo.format("gui.cd4017be." + key, args).split("\n")), mouseX - this.guiLeft, mouseY - this.guiTop, fontRendererObj);
        }
    }
    
    public void drawTankInfo(int id, int x, int y)
    {
        TankContainer tank = ((AutomatedTile)((TileContainer)this.inventorySlots).tileEntity).tanks;
        ArrayList<String> info = new ArrayList<String>();
        if (tank.getFluid(id) != null) info.add(tank.getFluid(id).getLocalizedName());
        else info.add("Empty");
        info.add(String.format("%s/%s ", Utils.formatNumber((float)tank.getAmount(id) / 1000F, 3), Utils.formatNumber((float)tank.tanks[id].cap / 1000F, 3)) + TooltipInfo.getFluidUnit());
        this.drawHoveringText(info, x, y, fontRendererObj);
    }
    
    public void drawLiquidConfig(AutomatedTile tile, int x, int y)
    {
        int s = tile.tanks.tanks.length;
        GlStateManager.color(1, 1, 1, 1);
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 0, 0, 9 + s * 9, 81);
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 0, 81, 9, 9);
        for (int j = 0; j < s; j++) {
        	this.drawTexturedModalRect(this.guiLeft + x + 9 + j * 9, this.guiTop + y, 18 + tile.tanks.tanks[j].dir * 9, 90, 9, 9);
        	for (int i = 0; i < 6; i++)
        	{
        		byte k = (byte)(tile.netData.longs[tile.tanks.netIdxLong] >> (2 * i + 16 * j) & 3);
        		this.drawTexturedModalRect(this.guiLeft + x + 9 + j * 9, this.guiTop + y + 9 + i * 9, 9 + k * 9, 81, 9, 9);
        	}
        	if ((tile.netData.longs[tile.tanks.netIdxLong] >> (12 + 16 * j) & 1) != 0)
        		this.drawTexturedModalRect(this.guiLeft + x + 9 + j * 9, this.guiTop + y + 63, 9, 81, 9, 9);
        }
    }
    
    public void drawItemConfig(AutomatedTile tile, int x, int y)
    {
        int s = tile.inventory.componets.length;
        GlStateManager.color(1, 1, 1, 1);
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 0, 0, 9 + s * 9, 63);
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 0, 90, 9, 9);
        for (int j = 0; j < s; j++) {
        	this.drawTexturedModalRect(this.guiLeft + x + 9 + j * 9, this.guiTop + y, 18 + tile.inventory.componets[j].d * 9, 90, 9, 9);
        	for (int i = 0; i < 6; i++)
	        {
	            byte k = (byte)(tile.netData.longs[tile.inventory.netIdxLong] >> (2 * i + 12 * j) & 3);
	            this.drawTexturedModalRect(this.guiLeft + x + 9 + j * 9, this.guiTop + y + 9 + i * 9, 9 + k * 9, 81, 9, 9);
	        }
        }
    }
    
    public void drawEnergyConfig(AutomatedTile tile, int x, int y)
    {
    	GlStateManager.color(1, 1, 1, 1);
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 0, 0, 18, 63);
        this.drawTexturedModalRect(this.guiLeft + x, this.guiTop + y, 0, 99, 9, 9);
        this.drawTexturedModalRect(this.guiLeft + x + 9, this.guiTop + y, 36, 90, 9, 9);
        for (int i = 0; i < 6; i++) {
        	byte k = (byte)(tile.energy.con >> i & 1);
    	    this.drawTexturedModalRect(this.guiLeft + x + 9, this.guiTop + y + 9 + i * 9, 18 - k * 9, 81, 9, 9);
        }
    }
    
    public void clickLiquidConfig(AutomatedTile tile, int rx, int ry)
    {   
        if (rx < 0 || rx >= 9 + 9 * tile.tanks.tanks.length || ry < 0 || ry >= 81) return;
        int x = rx / 9 - 1;
        int y = ry / 9 - 1;
        if (x >= 0 && y >= 0) {
        	byte cmd = 1;
        	byte d = (byte)(tile.netData.longs[tile.tanks.netIdxLong] >> (2 * y + 16 * x) & 3);
            if (y < 6) d = (byte)(d + 1 & 3);
            else if (y == 6) d ^= 1;
            else if (y == 7) cmd = 2;
            long cfg = tile.netData.longs[tile.tanks.netIdxLong] & ~(3L << (2 * y + 16 * x)) | ((long)d << (2 * y + 16 * x));
                PacketBuffer dos = tile.getPacketTargetData();
                dos.writeByte(cmd);
                if (cmd == 1) dos.writeLong(cfg);
                else if (cmd == 2) dos.writeByte(x);
                BlockGuiHandler.sendPacketToServer(dos);
        }
    }
    
    public void clickItemConfig(AutomatedTile tile, int rx, int ry)
    {
        if (rx < 0 || rx >= 9 + 9 * tile.inventory.componets.length || ry < 0 || ry >= 63) return;
        int x = rx / 9 - 1;
        int y = ry / 9 - 1;
        if (x >= 0 && y >= 0) {
            byte d = (byte)(tile.netData.longs[tile.inventory.netIdxLong] >> (2 * y + 12 * x) & 3);
            d = (byte)(d + 1 & 3);
            long cfg = tile.netData.longs[tile.inventory.netIdxLong] & ~(3L << (2 * y + 12 * x)) | ((long)d << (2 * y + 12 * x));
            	PacketBuffer dos = tile.getPacketTargetData();
                dos.writeByte(0);
                dos.writeLong(cfg);
                BlockGuiHandler.sendPacketToServer(dos);
        }
    }
    
    public void clickEnergyConfig(AutomatedTile tile, int rx, int ry)
    {
    	if (rx < 0 || rx >= 18 || ry < 0 || ry >= 63) return;
        int x = rx / 9 - 1;
        int y = ry / 9 - 1;
        if (x >= 0 && y >= 0) {
            tile.energy.con ^= 1 << y;
            	PacketBuffer dos = tile.getPacketTargetData();
                dos.writeByte(3);
                dos.writeByte(tile.energy.con);
                BlockGuiHandler.sendPacketToServer(dos);
        }
    }

    protected Slot getSlotAtPosition(int x, int y)
    {
        for (int k = 0; k < this.inventorySlots.inventorySlots.size(); ++k) {
            Slot slot = (Slot)this.inventorySlots.inventorySlots.get(k);
            if (this.isPointInRegion(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, x, y)) return slot;
        }
        return null;
    }
    
    private Slot lastClickSlot;

	@Override
	protected void mouseClickMove(int x, int y, int b, long t) 
	{
		Slot slot = this.getSlotAtPosition(x, y);
        ItemStack itemstack = this.mc.thePlayer.inventory.getItemStack();
        if (slot instanceof SlotHolo && slot != lastClickSlot) {
			if (itemstack == null || slot.getStack() == null || itemstack.isItemEqual(slot.getStack()))
				this.handleMouseClick(slot, slot.slotNumber, b, ClickType.PICKUP);
		} else super.mouseClickMove(x, y, b, t);
        lastClickSlot = slot;
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		lastClickSlot = null;
	}
    
}
