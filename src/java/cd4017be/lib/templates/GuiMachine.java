/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;

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
					text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL)));
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
    }
    
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
    }
    
    public void drawLiquidTank(TankContainer tanks, int id, int x, int y, boolean s) 
    {
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
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
            WorldRenderer r = Tessellator.getInstance().getWorldRenderer();
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
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
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
        GL11.glColor4f(1, 1, 1, 1);
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
        GL11.glColor4f(1, 1, 1, 1);
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
    	GL11.glColor4f(1, 1, 1, 1);
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
				this.handleMouseClick(slot, slot.slotNumber, b, 0);
		} else super.mouseClickMove(x, y, b, t);
        lastClickSlot = slot;
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		lastClickSlot = null;
	}
    
}
