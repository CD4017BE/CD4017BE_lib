/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TooltipInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 *
 * @author CD4017BE
 */
public abstract class GuiMachine extends GuiContainer
{
    
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

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) 
    {
        if (selTank >= 0) drawTankInfo(selTank, mx - this.guiLeft, my - this.guiTop);
        selTank = -1;
    }
    
    public void drawLiquidTank(TankContainer tanks, int id, int x, int y, boolean s) 
    {
        GL11.glColor4f(1, 1, 1, 1);
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        x += this.guiLeft;
        y += this.guiTop;
        this.drawTexturedModalRect(x - 1, y - 1, s?74:56, 0, s?36:18, 52);
        this.drawTexturedModalRect(x - 1, y + 51, s?74:56, 52 + 2 * (id & 3), s?36:18, 2);
        IIcon tex = null;
        if (tanks.getFluid(id) != null) {
            tex = tanks.getFluid(id).getFluid().getIcon(tanks.getFluid(id));
            if (tex == null){
                Block block = tanks.getFluid(id).getFluid().getBlock();
                if (block != null) tex = block.getBlockTextureFromSide(1);
            }
        }
        if (tex != null) {
            this.mc.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            float n = 50F - (float)tanks.getAmount(id) / (float)tanks.tanks[id].cap * 50F;
            float u = tex.getMinU();
            float v = tex.getMinV();
            float u1 = tex.getMaxU();
            float v1 = tex.getMaxV();
            Tessellator var9 = Tessellator.instance;
            var9.startDrawingQuads();
            var9.addVertexWithUV(x, y + 50, this.zLevel, u, v1);
            var9.addVertexWithUV(x + (s?34:16), y + 50, this.zLevel, u1, v1);
            var9.addVertexWithUV(x + (s?34:16), y + n, this.zLevel, u1, v);
            var9.addVertexWithUV(x, y + n, this.zLevel, u, v);
            var9.draw();
        }
        this.mc.renderEngine.bindTexture(new ResourceLocation("lib", "textures/icons.png"));
        this.drawTexturedModalRect(x + (s?17:-1), y - 1, 110, 0, 18, 52);
        if (this.func_146978_c(x - guiLeft, y - guiTop, s?34:16, 50, mouseX, mouseY)){
            selTank = id;
        }
    }
    
    public void drawInfo(int x, int y, int w, int h, String... text)
    {
        if (this.func_146978_c(x, y, w, h, mouseX, mouseY)) {
            if (text.length == 2 && text[0].equals("\\i")) {
                String s = TooltipInfo.getLocFormat("gui.cd4017be." + text[1]);
                if (s == null) return;
                text = s.split("\n");
            }
            this.drawHoveringText(Arrays.asList(text), mouseX - this.guiLeft, mouseY - this.guiTop, fontRendererObj);
        }
    }
    
    public void drawTankInfo(int id, int x, int y)
    {
        TankContainer tank = ((AutomatedTile)((TileContainer)this.inventorySlots).tileEntity).tanks;
        ArrayList<String> info = new ArrayList<String>();
        if (tank.getFluid(id) != null) info.add(tank.getFluid(id).getFluid().getLocalizedName());
        else info.add("Empty");
        info.add(tank.getAmount(id) + "/" + tank.tanks[id].cap + "L");
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
            try {
                ByteArrayOutputStream bos = tile.getPacketTargetData();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.writeByte(cmd);
                if (cmd == 1) dos.writeLong(cfg);
                else if (cmd == 2) dos.writeByte(x);
                BlockGuiHandler.sendPacketToServer(bos);
            } catch (IOException e) {}
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
            try {
                ByteArrayOutputStream bos = tile.getPacketTargetData();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.writeByte(0);
                dos.writeLong(cfg);
                BlockGuiHandler.sendPacketToServer(bos);
            } catch (IOException e) {}
        }
    }
    
    public void clickEnergyConfig(AutomatedTile tile, int rx, int ry)
    {
    	if (rx < 0 || rx >= 18 || ry < 0 || ry >= 63) return;
        int x = rx / 9 - 1;
        int y = ry / 9 - 1;
        if (x >= 0 && y >= 0) {
            tile.energy.con ^= 1 << y;
            try {
                ByteArrayOutputStream bos = tile.getPacketTargetData();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.writeByte(3);
                dos.writeByte(tile.energy.con);
                BlockGuiHandler.sendPacketToServer(bos);
            } catch (IOException e) {}
        }
    }

    protected Slot getSlotAtPosition(int x, int y)
    {
        for (int k = 0; k < this.inventorySlots.inventorySlots.size(); ++k) {
            Slot slot = (Slot)this.inventorySlots.inventorySlots.get(k);
            if (this.func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, x, y)) return slot;
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
	protected void mouseMovedOrUp(int x, int y, int b) 
	{
		super.mouseMovedOrUp(x, y, b);
		if (b == -1) lastClickSlot = null;
	}
    
}
