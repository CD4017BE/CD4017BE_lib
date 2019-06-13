package cd4017be.lib.Gui;

import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Vec3;

import java.io.IOException;
import java.util.Arrays;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.items.ItemHandlerHelper;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import static cd4017be.lib.Gui.comp.IGuiComp.*;

/**
 * GuiContainer based component manager template.
 * @see GuiCompGroup
 * @author CD4017BE
 */
public class ModularGui extends GuiContainer {
	
	public static final ResourceLocation LIB_TEX = new ResourceLocation("cd4017be_lib", "textures/icons.png");

	/** whether to draw the player inventory title */
	protected boolean drawInvTitle;
	private Slot lastClickSlot;
	public GuiCompGroup compGroup;
	public final AdvancedContainer container;

	/**
	 * Creates a new ModularGui instance<br>
	 * Note: the GuiFrame {@link #compGroup} is still null, it must be initialized before {@link #initGui()} is called!
	 * @param container container providing the state from server
	 */
	public ModularGui(AdvancedContainer container) {
		super(container);
		this.container = container;
		this.drawInvTitle = container.hasPlayerInv();
	}

	@Override
	public void initGui() {
		this.xSize = compGroup.w;
		this.ySize = compGroup.h;
		super.initGui();
		compGroup.init(width, height, zLevel, fontRenderer);
		compGroup.position(guiLeft, guiTop);
		Keyboard.enableRepeatEvents(true);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableDepth();
		GlStateManager.disableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.pushMatrix();
		GlStateManager.translate(-guiLeft, -guiTop, 0);//undo transformation done by GuiContainer's draw
		compGroup.drawOverlay(mx, my);
		GlStateManager.popMatrix();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		compGroup.drawBackground(mouseX, mouseY, partialTicks);
		if (drawInvTitle) {
			Slot pos = container.inventorySlots.get(container.playerInvStart());
			this.drawStringCentered(TooltipUtil.translate("container.inventory"), this.guiLeft + pos.xPos + 80, this.guiTop + pos.yPos - 12, 0x404040);
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		if (!compGroup.mouseIn(x, y, b, A_DOWN))
			super.mouseClicked(x, y, b);
	}

	@Override
	protected void mouseClickMove(int x, int y, int b, long t) {
		if (!compGroup.mouseIn(x, y, b, A_HOLD)) {
			Slot slot = this.getSlotUnderMouse();
			ItemStack itemstack = this.mc.player.inventory.getItemStack();
			if (slot instanceof SlotHolo && slot != lastClickSlot) {
				ItemStack slotstack = slot.getStack();
				if (itemstack.isEmpty() || slotstack.isEmpty() || ItemHandlerHelper.canItemStacksStack(itemstack, slotstack))
					this.handleMouseClick(slot, slot.slotNumber, b, ClickType.PICKUP);
			} else super.mouseClickMove(x, y, b, t);
			lastClickSlot = slot;
		}
	}

	@Override
	protected void mouseReleased(int x, int y, int b) {
		if (!compGroup.mouseIn(x, y, b, A_UP))
			super.mouseReleased(x, y, b);
		lastClickSlot = null;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (!compGroup.keyIn(typedChar, keyCode, A_DOWN)) //TODO other key events
			super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void handleMouseInput() throws IOException {
		int z = Mouse.getEventDWheel();
		if (z != 0) {
			if (z > 1) z = 1;
			else if (z < -1) z = -1;
			int x = Mouse.getEventX() * width / mc.displayWidth;
			int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
			compGroup.mouseIn(x, y, z, A_SCROLL);
		}
		super.handleMouseInput();
	}

	public void drawFormatInfo(int x, int y, String key, Object... args) {
		this.drawHoveringText(Arrays.asList(TooltipUtil.format(key, args).split("\n")), x, y, fontRenderer);
	}

	public void drawLocString(int x, int y, int h, int c, String s, Object... args) {
		String[] text = TooltipUtil.format(s, args).split("\n");
		for (String l : text) {
			this.fontRenderer.drawString(l, x, y, c);
			y += h;
		}
	}

	public void drawStringCentered(String s, int x, int y, int c) {
		this.fontRenderer.drawString(s, x - this.fontRenderer.getStringWidth(s) / 2, y, c);
	}

	/**
	 * draws a block overlay next to the GuiScreen that is useful to visualizes block faces.
	 * @param side face to highlight with an arrow
	 * @param type arrow variant
	 */
	public void drawSideConfig(EnumFacing side, int type) {
		GlStateManager.enableDepth();
		GlStateManager.disableLighting();
		this.drawGradientRect(-64, 0, 0, 64, 0xff000000, 0xff000000);
		this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.translate(-32, 32, 32);
		GlStateManager.scale(16F, -16F, 16F);
		EntityPlayer player = container.player;
		GlStateManager.rotate(player.rotationPitch, 1, 0, 0);
		GlStateManager.rotate(player.rotationYaw + 180, 0, 1, 0);
		GlStateManager.translate(-0.5F, -0.5F, -0.5F);
		
		GlStateManager.pushMatrix();
		BlockPos pos = container.handler.pos();
		GlStateManager.translate(-pos.getX(), -pos.getY(), -pos.getZ());
		BufferBuilder t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		renderBlock(player.world, pos, t);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
		
		if (side == null) {
			GlStateManager.popMatrix();
			return;
		}
		this.mc.renderEngine.bindTexture(LIB_TEX);
		Vec3 p = Vec3.Def(0.5, 0.5, 0.5), a, b;
		switch(side) {
		case DOWN: a = Vec3.Def(0, -1, 0); break;
		case UP: a = Vec3.Def(0, 1, 0); break;
		case NORTH: a = Vec3.Def(0, 0, -1); break;
		case SOUTH: a = Vec3.Def(0, 0, 1); break;
		case WEST: a = Vec3.Def(-1, 0, 0); break;
		default: a = Vec3.Def(1, 0, 0);
		}
		Vec3d look = player.getLookVec();
		b = Vec3.Def(look.x, look.y, look.z).mult(a).norm();
		p = p.add(a.scale(0.5)).add(b.scale(-0.5));
		a = a.scale(1.5);
		final float tx = (float)(144 + 16 * type) / 256F, dtx = 16F / 256F, ty = 24F / 256F, dty = 8F / 256F;
		
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		t.pos(p.x + b.x, p.y + b.y, p.z + b.z).tex(tx, ty + dty).endVertex();
		t.pos(p.x + a.x + b.x, p.y + a.y + b.y, p.z + a.z + b.z).tex(tx + dtx, ty + dty).endVertex();
		t.pos(p.x + a.x, p.y + a.y, p.z + a.z).tex(tx + dtx, ty).endVertex();
		t.pos(p.x, p.y, p.z).tex(tx, ty).endVertex();
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
	}

	protected void renderBlock(IBlockAccess world, BlockPos pos, BufferBuilder t) {
		BlockRendererDispatcher render = mc.getBlockRendererDispatcher();
		IBlockState state = world.getBlockState(pos);
		if (state.getRenderType() != EnumBlockRenderType.MODEL) state = Blocks.GLASS.getDefaultState();
		state = state.getActualState(world, pos);
		IBakedModel model = render.getModelForState(state);
		state = state.getBlock().getExtendedState(state, world, pos);
		render.getBlockModelRenderer().renderModel(world, model, state, pos, t, false);
	}

	public void drawItemStack(ItemStack stack, int x, int y, String altText){
		zLevel = 200.0F;
		itemRender.zLevel = 200.0F;
		net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
		if (font == null) font = fontRenderer;
		this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
		this.itemRender.renderItemOverlayIntoGUI(font, stack, x, y, altText);
		this.zLevel = 0.0F;
		this.itemRender.zLevel = 0.0F;
	}

	/**
	 * posts a client side chat message
	 * @param msg message to post
	 */
	public void sendChat(String msg) {
		mc.player.sendMessage(new TextComponentString(msg));
	}

	public static void color(int c) {
		GlStateManager.enableBlend();
		GlStateManager.color((float)(c >> 16 & 0xff) / 255F, (float)(c >> 8 & 0xff) / 255F, (float)(c & 0xff) / 255F, (float)(c >> 24 & 0xff) / 255F);
	}

	/**
	 * sends a packet to the server that is addressed to this GUI's data provider and contains a single byte of payload.<br>
	 * (convenience method for handling button events)
	 * @param c value to send
	 */
	public void sendCommand(int c) {
		PacketBuffer buff = GuiNetworkHandler.preparePacket(container);
		buff.writeByte(c);
		GuiNetworkHandler.GNH_INSTANCE.sendToServer(buff);
	}

	/**
	 * sends a packet to the server that is addressed to this GUI's data provider and contains the given values as payload.<br>
	 * (convenience method for handling button events)
	 * @param args data to send (supports: byte, short, int, long, float, double, String)
	 */
	public void sendPkt(Object... args) {
		PacketBuffer buff = GuiNetworkHandler.preparePacket(container);
		for (Object arg : args) {
			if (arg instanceof Byte) buff.writeByte((Byte)arg);
			else if (arg instanceof Short) buff.writeShort((Short)arg);
			else if (arg instanceof Integer) buff.writeInt((Integer)arg);
			else if (arg instanceof Long) buff.writeLong((Long)arg);
			else if (arg instanceof Float) buff.writeFloat((Float)arg);
			else if (arg instanceof Double) buff.writeDouble((Double)arg);
			else if (arg instanceof String) buff.writeString((String)arg);
			else throw new IllegalArgumentException();
		}
		GuiNetworkHandler.GNH_INSTANCE.sendToServer(buff);
	}

}
