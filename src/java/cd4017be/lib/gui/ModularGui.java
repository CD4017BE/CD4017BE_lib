package cd4017be.lib.gui;

import cd4017be.lib.Lib;
import cd4017be.lib.network.GuiNetworkHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import com.mojang.math.Matrix4f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fmlclient.gui.GuiUtils;
import net.minecraftforge.items.ItemHandlerHelper;

import static cd4017be.lib.gui.comp.IGuiComp.*;
import static cd4017be.lib.text.TooltipUtil.*;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import cd4017be.lib.container.AdvancedContainer;
import cd4017be.lib.container.slot.IFluidSlot;
import cd4017be.lib.container.slot.SlotHolo;
import cd4017be.lib.gui.comp.GuiCompGroup;

/**
 * AbstractContainerScreen based component manager template.
 * @see GuiCompGroup
 * @author CD4017BE
 */
@OnlyIn(Dist.CLIENT)
public class ModularGui<T extends AdvancedContainer> extends AbstractContainerScreen<T> {
	
	public static final ResourceLocation LIB_TEX = Lib.rl("textures/icons.png");

	/** whether to draw main title (&1) and player inventory title (&2) */
	protected byte drawTitles;
	private Slot lastClickSlot;
	public GuiCompGroup compGroup;
	public final Int2ObjectOpenHashMap<String> slotTooltips = new Int2ObjectOpenHashMap<>();

	/**
	 * Creates a new ModularGui instance<br>
	 * Note: the GuiFrame {@link #compGroup} is still null, it must be initialized before {@link #initGui()} is called!
	 * @param container container providing the state from server
	 */
	public ModularGui(T container, Inventory inv, Component name) {
		super(container, inv, name);
		if (container.hasPlayerInv()) {
			this.drawTitles |= 2;
			Slot slot = container.getSlot(container.playerInvStart());
			inventoryLabelX = slot.x;
			inventoryLabelY = slot.y - 13;
		}
	}

	public ModularGui<T> setComps(GuiCompGroup comps, boolean title) {
		this.compGroup = comps;
		if (title) this.drawTitles |= 1;
		return this;
	}

	@Override
	protected void init() {
		this.imageWidth = compGroup.w;
		this.imageHeight = compGroup.h;
		super.init();
		init(compGroup, leftPos, topPos);
		//Keyboard.enableRepeatEvents(true);
	}

	public void init(GuiCompGroup comp, int x, int y) {
		comp.init(width, height, 0, font);
		comp.position(x, y);
	}

	@Override
	public void removed() {
		super.removed();
		//Keyboard.enableRepeatEvents(false);
	}

	@Override
	public void render(PoseStack matrixStack, int mx, int my, float partialTicks) {
		renderBackground(matrixStack);
		super.render(matrixStack, mx, my, partialTicks);
		renderTooltip(matrixStack, mx, my);
	}

	@Override
	protected void renderTooltip(PoseStack matrixStack, int x, int y) {
		super.renderTooltip(matrixStack, x, y);
		if (hoveredSlot instanceof IFluidSlot) {
			IFluidSlot fslot = ((IFluidSlot)hoveredSlot);
			FluidStack stack = fslot.getFluid();
			ArrayList<String> info = new ArrayList<String>();
			info.add(stack != null ? stack.getDisplayName().getString() : translate("cd4017be.tankEmpty"));
			info.add(format("cd4017be.tankAmount", stack != null ? (double)stack.getAmount() / 1000D : 0D, (double)fslot.getCapacity() / 1000D));
			GuiUtils.drawHoveringText(matrixStack, convertText(info), x, y, width, height, -1, font);
		} else if (hoveredSlot != null && !(hoveredSlot.hasItem() && menu.getCarried().isEmpty())) {
			String s = slotTooltips.get(hoveredSlot.index);
			if (s != null)
				GuiUtils.drawHoveringText(matrixStack, convertText(translate(s)), x, y, width, height, -1, font);
		}
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		compGroup.drawOverlay(matrixStack, x, y);
	}

	@Override
	protected void renderLabels(PoseStack matrixStack, int x, int y) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		for (Slot slot : menu.slots)
			if (slot instanceof IFluidSlot) {
				IFluidSlot fslot = ((IFluidSlot)slot);
				FluidStack stack = fslot.getFluid();
				if (stack == null) continue;
				float h = (float)stack.getAmount() / (float)fslot.getCapacity();
				if (Float.isNaN(h) || h > 1F || h < 0F) h = 16F;
				else h *= 16F;
				drawFluid(matrixStack, stack, slot.x, slot.y + 16 - h, 16, h);
			}
		color(-1);
	}

	protected void drawFluid(PoseStack matrixStack, FluidStack stack, float x, float y, float w, float h) {
		Matrix4f mat = matrixStack.last().pose();
		FluidAttributes attr = stack.getFluid().getAttributes();
		TextureAtlasSprite tex = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
			.apply(attr.getStillTexture(stack));
		float u0 = tex.getU0(), v0 = tex.getV0(), u1 = tex.getU1(), v1 = tex.getV1();
		color(attr.getColor(stack));
		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(mat, x, y + h, 0F).uv(u0, v1).endVertex();
		bufferbuilder.vertex(mat, x + w, y + h, 0F).uv(u1, v1).endVertex();
		bufferbuilder.vertex(mat, x + w, y, 0F).uv(u1, v0).endVertex();
		bufferbuilder.vertex(mat, x, y, 0F).uv(u0, v0).endVertex();
		tessellator.end();
	}

	@Override
	protected void renderBg(
		PoseStack matrixStack, float partialTicks, int mx, int my
	) {
		compGroup.drawBackground(matrixStack, mx, my, partialTicks);
		if ((drawTitles & 1) != 0) font.draw(
				matrixStack, title, leftPos + titleLabelX, topPos + titleLabelY, 0x404040
			);
		if ((drawTitles & 2) != 0) font.draw(
				matrixStack, playerInventoryTitle,
				leftPos + inventoryLabelX, topPos + inventoryLabelY, 0x404040
			);
	}

	@Override
	public boolean mouseClicked(double x, double y, int b) {
		return compGroup.mouseIn((int)x, (int)y, b, A_DOWN)
			|| super.mouseClicked(x, y, b);
	}

	@Override
	public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
		if (compGroup.mouseIn((int)x, (int)y, b, A_HOLD)) return true;
		Slot slot = this.getSlotUnderMouse();
		ItemStack itemstack = menu.getCarried();
		if (slot instanceof SlotHolo && slot != lastClickSlot) {
			ItemStack slotstack = slot.getItem();
			if (itemstack.isEmpty() || slotstack.isEmpty() || ItemHandlerHelper.canItemStacksStack(itemstack, slotstack))
				this.slotClicked(slot, slot.index, b, ClickType.PICKUP);
			lastClickSlot = slot;
			return true;
		}
		lastClickSlot = slot;
		return super.mouseDragged(x, y, b, dx, dy);
	}

	@Override
	public boolean mouseReleased(double x, double y, int b) {
		lastClickSlot = null;
		return compGroup.mouseIn((int)x, (int)y, b, A_UP)
			|| super.mouseReleased(x, y, b);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return compGroup.keyIn((char)0, keyCode, A_DOWN) //TODO other key events
			|| super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		return compGroup.keyIn(c, GLFW.GLFW_KEY_UNKNOWN, A_DOWN)
			|| super.charTyped(c, modifiers);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double delta) {
		return compGroup.mouseIn((int)x, (int)y, (int)(Double.doubleToRawLongBits(delta) >> 63) | 1, A_SCROLL)
			|| super.mouseScrolled(x, y, delta);
	}

	public void drawFormatInfo(PoseStack stack, int x, int y, String key, Object... args) {
		this.renderComponentToolTip(stack, convertText(format(key, args)), x, y, font);
	}

	public void drawLocString(PoseStack stack, int x, int y, int h, int c, String s, Object... args) {
		String[] text = format(s, args).split("\n");
		for (String l : text) {
			this.font.draw(stack, l, x, y, c);
			y += h;
		}
	}

	public void drawStringCentered(PoseStack stack, String s, int x, int y, int c) {
		this.font.draw(stack, s, x - this.font.width(s) / 2, y, c);
	}

	/**
	 * draws a block overlay next to the GuiScreen that is useful to visualizes block faces.
	 * @param side face to highlight with an arrow
	 * @param type arrow variant
	 */
	public void drawSideConfig(PoseStack stack, Direction side, int type) {
		/* TODO reimplement
		GlStateManager.enableDepthTest();
		GlStateManager.disableLighting();
		this.fillGradient(stack, -64, 0, 0, 64, 0xff000000, 0xff000000);
		this.minecraft.textureManager.bindTexture(minecraft.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.translatef(-32, 32, 32);
		GlStateManager.scalef(16F, -16F, 16F);
		PlayerEntity player = container.inv.player;
		GlStateManager.rotatef(player.rotationPitch, 1, 0, 0);
		GlStateManager.rotatef(player.rotationYaw + 180, 0, 1, 0);
		GlStateManager.translatef(-0.5F, -0.5F, -0.5F);
		
		GlStateManager.pushMatrix();
		BlockPos pos = container.getPos();
		GlStateManager.translatef(-pos.getX(), -pos.getY(), -pos.getZ());
		BufferBuilder t = Tesselator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormat.BLOCK);
		renderBlock(player.world, pos, t);
		Tesselator.getInstance().draw();
		GlStateManager.popMatrix();
		
		if (side == null) {
			GlStateManager.popMatrix();
			return;
		}
		this.minecraft.textureManager.bindTexture(LIB_TEX);
		Vec3 p = Vec3.Def(0.5, 0.5, 0.5), a, b;
		switch(side) {
		case DOWN: a = Vec3.Def(0, -1, 0); break;
		case UP: a = Vec3.Def(0, 1, 0); break;
		case NORTH: a = Vec3.Def(0, 0, -1); break;
		case SOUTH: a = Vec3.Def(0, 0, 1); break;
		case WEST: a = Vec3.Def(-1, 0, 0); break;
		default: a = Vec3.Def(1, 0, 0);
		}
		Vector3d look = player.getLookVec();
		b = Vec3.Def(look.x, look.y, look.z).mult(a).norm();
		p = p.add(a.scale(0.5)).add(b.scale(-0.5));
		a = a.scale(1.5);
		final float tx = (float)(144 + 16 * type) / 256F, dtx = 16F / 256F, ty = 24F / 256F, dty = 8F / 256F;
		
		t.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX);
		t.pos(p.x + b.x, p.y + b.y, p.z + b.z).tex(tx, ty + dty).endVertex();
		t.pos(p.x + a.x + b.x, p.y + a.y + b.y, p.z + a.z + b.z).tex(tx + dtx, ty + dty).endVertex();
		t.pos(p.x + a.x, p.y + a.y, p.z + a.z).tex(tx + dtx, ty).endVertex();
		t.pos(p.x, p.y, p.z).tex(tx, ty).endVertex();
		Tesselator.getInstance().draw();
		GlStateManager.popMatrix();
		*/
	}

	protected void renderBlock(PoseStack stack, Level world, BlockPos pos, BufferBuilder t) {
		/* TODO reimplement
		BlockRendererDispatcher render = minecraft.getBlockRendererDispatcher();
		BlockState state = world.getBlockState(pos);
		if (state.getRenderType() != BlockRenderType.MODEL) state = Blocks.GLASS.getDefaultState();
		state = state.getActualState(world, pos);
		IBakedModel model = render.getModelForState(state);
		state = state.getBlock().getExtendedState(state, world, pos);
		render.getBlockModelRenderer().renderModel(world, model, state, pos, stack, t, false, new Random(), 0, 0, null);
		*/
	}

	public void renderFloatingItem(ItemStack stack, int x, int y, String altText){
		itemRenderer.blitOffset = 200.0F;
		itemRenderer.renderAndDecorateItem(stack, x, y);
		itemRenderer.renderGuiItemDecorations(font, stack, x, y, altText);
		itemRenderer.blitOffset = 0.0F;
	}

	/**
	 * posts a client side chat message
	 * @param msg message to post
	 */
	public void sendChat(String msg) {
		minecraft.player.sendMessage(new TextComponent(msg), null);
	}

	/**
	 * sends a packet to the server that is addressed to this GUI's data provider and contains a single byte of payload.<br>
	 * (convenience method for handling button events)
	 * @param c value to send
	 */
	public void sendCommand(int c) {
		FriendlyByteBuf buff = GuiNetworkHandler.preparePacket(menu);
		buff.writeByte(c);
		GuiNetworkHandler.GNH_INSTANCE.sendToServer(buff);
	}

	/**
	 * sends a packet to the server that is addressed to this GUI's data provider and contains the given values as payload.<br>
	 * (convenience method for handling button events)
	 * @param args data to send (supports: byte, short, int, long, float, double, String)
	 */
	public void sendPkt(Object... args) {
		FriendlyByteBuf buff = GuiNetworkHandler.preparePacket(menu);
		for (Object arg : args) {
			if (arg instanceof Byte) buff.writeByte((Byte)arg);
			else if (arg instanceof Short) buff.writeShort((Short)arg);
			else if (arg instanceof Integer) buff.writeInt((Integer)arg);
			else if (arg instanceof Long) buff.writeLong((Long)arg);
			else if (arg instanceof Float) buff.writeFloat((Float)arg);
			else if (arg instanceof Double) buff.writeDouble((Double)arg);
			else if (arg instanceof String) buff.writeUtf((String)arg);
			else throw new IllegalArgumentException();
		}
		GuiNetworkHandler.GNH_INSTANCE.sendToServer(buff);
	}

	public static void color(int c) {
		RenderSystem.enableBlend();
		RenderSystem.setShaderFogColor(
			(float)(c >> 16 & 0xff) / 255F,
			(float)(c >> 8 & 0xff) / 255F,
			(float)(c & 0xff) / 255F,
			(float)(c >> 24 & 0xff) / 255F
		);
	}

}
