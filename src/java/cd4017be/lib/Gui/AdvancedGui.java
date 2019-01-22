package cd4017be.lib.Gui;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.Gui.TileContainer.TankSlot;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.mojang.realmsclient.gui.ChatFormatting;

/**
 *
 * @author CD4017BE
 * @deprecated Use new GUI system instead: {@link ModularGui}
 */
@Deprecated
public abstract class AdvancedGui extends GuiContainer {

	public static final ResourceLocation LIB_TEX = new ResourceLocation("cd4017be_lib", "textures/icons.png");
	public ResourceLocation MAIN_TEX;
	public int focus = -1, tabsX = 0, tabsY = 7, bgTexX = 0, bgTexY = 0, titleX, titleY;
	/**	1: background texture, 2: main title, 4: inventory title */
	protected byte drawBG = 7;
	public ArrayList<GuiComp<?>> guiComps = new ArrayList<GuiComp<?>>();
	private Slot lastClickSlot;

	public AdvancedGui(Container container) {
		super(container);
	}

	@Override
	public void initGui() {
		guiComps.clear();
		super.initGui();
		titleX = xSize / 2; titleY = 4;
		if (inventorySlots instanceof TileContainer) {
			TileContainer cont = (TileContainer)inventorySlots;
			for (TankSlot slot : cont.tankSlots)
				guiComps.add(new FluidTank(guiComps.size(), slot));
		}
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
		GlStateManager.translate(-guiLeft, -guiTop, 0);
		for (GuiComp<?> comp : guiComps)
			if (comp.enabled && comp.isInside(mx, my))
				comp.drawOverlay(mx, my);
		GlStateManager.popMatrix();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		if ((drawBG & 1) != 0) {
			mc.renderEngine.bindTexture(MAIN_TEX);
			this.drawTexturedModalRect(guiLeft, guiTop, bgTexX, bgTexY, xSize, ySize);
		}
		if ((drawBG & 4) != 0 && inventorySlots instanceof TileContainer) {
			TileContainer cont = (TileContainer)inventorySlots;
			if (cont.invPlayerS != cont.invPlayerE) {
				Slot pos = cont.inventorySlots.get(cont.invPlayerS);
				this.drawStringCentered(TooltipUtil.translate("container.inventory"), this.guiLeft + pos.xPos + 80, this.guiTop + pos.yPos - 12, 0x404040);
			}
		}
		if ((drawBG & 2) != 0 && inventorySlots instanceof DataContainer)
			this.drawStringCentered(((DataContainer)inventorySlots).data.getName(), guiLeft + titleX, guiTop + titleY, 0x404040);
		GlStateManager.color(1F, 1F, 1F, 1F);
		for (GuiComp<?> comp : guiComps)
			if (comp.enabled) comp.draw();
	}

	@Override
	protected void mouseClicked(int x, int y, int b) throws IOException {
		boolean doSuper = true;
		for (GuiComp<?> comp : guiComps) 
			if (comp.enabled && comp.isInside(x, y)) {
				if (comp.id != focus) this.setFocus(comp.id);
				doSuper = !comp.mouseIn(x, y, b, 0);
				if (!doSuper) break;
			}
		if (focus >= 0 && !guiComps.get(focus).isInside(x, y)) this.setFocus(-1);
		if (doSuper) super.mouseClicked(x, y, b);
	}

	@Override
	protected void mouseClickMove(int x, int y, int b, long t) {
		if (focus >= 0) guiComps.get(focus).mouseIn(x, y, b, 1);
		else {
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
		if (focus < 0 || !guiComps.get(focus).mouseIn(x, y, b, 2))
			super.mouseReleased(x, y, b);
		lastClickSlot = null;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (focus >= 0) guiComps.get(focus).keyTyped(typedChar, keyCode);
		else super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void handleMouseInput() throws IOException {
		int z = Mouse.getEventDWheel();
		if (z != 0) {
			if (z > 1) z = 1;
			else if (z < -1) z = -1;
			int x = Mouse.getEventX() * width / mc.displayWidth;
			int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
			for (GuiComp<?> comp : guiComps)
				if (comp.enabled && comp.isInside(x, y) && comp.mouseIn(x, y, -z, 3)) break;
		}
		super.handleMouseInput();
	}

	public void drawFormatInfo(int x, int y, String key, Object... args) {
		this.drawHoveringText(Arrays.asList(TooltipUtil.format("gui.cd4017be." + key, args).split("\n")), x, y, fontRenderer);
	}

	public void drawLocString(int x, int y, int h, int c, String s, Object... args) {
		String[] text = TooltipUtil.format("gui.cd4017be." + s, args).split("\n");
		for (String l : text) {
			this.fontRenderer.drawString(l, x, y, c);
			y += h;
		}
	}

	public void drawStringCentered(String s, int x, int y, int c) {
		this.fontRenderer.drawString(s, x - this.fontRenderer.getStringWidth(s) / 2, y, c);
	}

	protected void drawSideCube(int x, int y, int s, byte dir) {
		GlStateManager.enableDepth();
		GlStateManager.disableLighting();
		this.drawGradientRect(x, y, x + 64, y + 64, 0xff000000, 0xff000000);
		this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 32, y + 32, 32);
		GlStateManager.scale(16F, -16F, 16F);
		EntityPlayer player = ((DataContainer)this.inventorySlots).player;
		GlStateManager.rotate(player.rotationPitch, 1, 0, 0);
		GlStateManager.rotate(player.rotationYaw + 180, 0, 1, 0);
		GlStateManager.translate(-0.5F, -0.5F, -0.5F);
		IGuiData tile = ((DataContainer)this.inventorySlots).data;
		
		GlStateManager.pushMatrix();
		BlockPos pos = tile.pos();
		GlStateManager.translate(-pos.getX(), -pos.getY(), -pos.getZ());
		BufferBuilder t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		renderBlock(player.world, pos, t);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
		
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
		b = Vec3.Def(look.x, look.y, look.z).mult(a).norm();
		p = p.add(a.scale(0.5)).add(b.scale(-0.5));
		a = a.scale(1.5);
		final float tx = (float)(144 + 16 * dir) / 256F, dtx = 16F / 256F, ty = 24F / 256F, dty = 8F / 256F;
		
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		t.pos(p.x + b.x, p.y + b.y, p.z + b.z).tex(tx, ty + dty).endVertex();
		t.pos(p.x + a.x + b.x, p.y + a.y + b.y, p.z + a.z + b.z).tex(tx + dtx, ty + dty).endVertex();
		t.pos(p.x + a.x, p.y + a.y, p.z + a.z).tex(tx + dtx, ty).endVertex();
		t.pos(p.x, p.y, p.z).tex(tx, ty).endVertex();
		Tessellator.getInstance().draw();
		GL11.glPopMatrix();
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
		PacketBuffer buff = BlockGuiHandler.getPacketTargetData(((DataContainer)inventorySlots).data.pos());
		buff.writeByte(c);
		BlockGuiHandler.sendPacketToServer(buff);
	}

	/**
	 * sends a packet to the server that is addressed to this GUI's data provider and contains the given values as payload.<br>
	 * (convenience method for handling button events)
	 * @param args data to send (supports: byte, short, int, long, float, double, String)
	 */
	public void sendPkt(Object... args) {
		PacketBuffer buff = BlockGuiHandler.getPacketTargetData(((DataContainer)inventorySlots).data.pos());
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
		BlockGuiHandler.sendPacketToServer(buff);
	}

	public void setFocus(int id) {
		if (focus >= 0 && focus < guiComps.size()) guiComps.get(focus).unfocus();
		focus = id >= 0 && id < guiComps.size() && guiComps.get(id).focus() ? id : -1;
	}

	public void setEnabled(int id, boolean enable) {
		GuiComp<?> comp = guiComps.get(id);
		if (comp.enabled && !enable && focus == id) setFocus(-1);
		comp.enabled = enable;
	}

	protected Object getDisplVar(int id) {return null;}
	protected void setDisplVar(int id, Object obj, boolean send) {}

	public class GuiComp<V> {
		protected final Supplier<V> get;
		protected final Consumer<V> set;
		protected final Consumer<V> update;
		public final int id, px, py, w, h;
		public String tooltip;
		public boolean enabled = true;

		@SuppressWarnings("unchecked")
		public GuiComp(int id, int px, int py, int w, int h) {
			this.id = id;
			this.px = px + guiLeft;
			this.py = py + guiTop;
			this.w = w; this.h = h;
			this.get = () -> (V)getDisplVar(this.id);
			this.set = (v) -> setDisplVar(this.id, v, false);
			this.update = (v) -> setDisplVar(this.id, v, true);
		}

		public GuiComp(int id, int px, int py, int w, int h, Supplier<V> get, Consumer<V> set, Consumer<V> update) {
			this.id = id;
			this.px = px + guiLeft;
			this.py = py + guiTop;
			this.w = w; this.h = h;
			this.get = get;
			this.set = set;
			this.update = update;
		}

		/** @param s '#' gets replaced with state or prefix 'x*A+B;' (A,B are numbers) does linear transformation on numeric state and uses it as format argument */
		public GuiComp<V> setTooltip(String s) {
			this.tooltip = s;
			return this;
		}

		public boolean isInside(int x, int y) {
			return x >= px && x < px + w && y >= py && y < py + h;
		}

		public void drawOverlay(int mx, int my) {
			if (tooltip == null) return;
			String text;
			if (tooltip.startsWith("x*")) {
				int p = tooltip.indexOf('+', 2), q = tooltip.indexOf(';', p);
				float f = (Float)get.get() * Float.parseFloat(tooltip.substring(2, p)) + Float.parseFloat(tooltip.substring(p + 1, q));
				text = TooltipUtil.format("gui.cd4017be." + tooltip.substring(q + 1), f);
			} else {
				if (tooltip.endsWith("#")) text = tooltip.replace("#", get.get().toString());
				else text = tooltip;
				text = TooltipUtil.getConfigFormat("gui.cd4017be." + text);
			}
			drawHoveringText(Arrays.asList(text.split("\n")), mx, py + h + 12, fontRenderer);
		}

		public void draw() {}
		public void keyTyped(char c, int k) {}
		/** @param x absolute screen X
		 *  @param y absolute screen Y
		 *  @param b mouse button: 0=left 1=right 2=middle or +/-1 for scroll
		 *  @param d event type: 0=click 1=clickMove 2=release 3=scroll
		 *  @return consume event*/
		public boolean mouseIn(int x, int y, int b, int d) {return false;}
		public void unfocus() {}
		/** @return do focus */
		public boolean focus() {return false;}

	}

	public class Tooltip<V> extends GuiComp<V> {

		public Tooltip(int id, int px, int py, int w, int h, String tooltip) {
			super(id, px, py, w, h);
			setTooltip(tooltip);
		}

		public Tooltip(int id, int px, int py, int w, int h, String tooltip, Supplier<V> get) {
			super(id, px, py, w, h, get, null, null);
			setTooltip(tooltip);
		}

		@Override
		public void drawOverlay(int mx, int my) {
			Object obj = get.get();
			Object[] objA = obj instanceof Object[] ? (Object[])obj : new Object[]{obj};
			String s = tooltip.startsWith("\\") ? String.format(tooltip.substring(1), objA) : TooltipUtil.format("gui.cd4017be." + tooltip, objA);
			drawHoveringText(Arrays.asList(s.split("\n")), mx, my, fontRenderer);
		}

	}

	public class Text<V> extends GuiComp<V> {
		public String text;
		public int fh = 8, tc = 0xff404040;
		public boolean center = false;

		public Text(int id, int x, int y, int w, int h, String key) {
			super(id, x, y, w, h);
			this.text = key;
		}

		public Text(int id, int x, int y, int w, int h, String key, Supplier<V> get) {
			super(id, x, y, w, h, get, null, null);
			this.text = key;
		}

		public Text<V> font(int tc, int fh) {
			this.tc = tc;
			this.fh = fh;
			return this;
		}

		public Text<V> center() {
			this.center = true;
			return this;
		}

		@Override
		public void draw() {
			Object obj = get.get();
			Object[] objA = obj instanceof Object[] ? (Object[])obj : new Object[]{obj};
			String[] lines = (text.startsWith("\\") ? 
					String.format(text.substring(1), objA) : 
					TooltipUtil.format("gui.cd4017be." + (text.endsWith("#") ? text.replaceAll("#", ((Integer)obj).toString()) : text), objA)
				).split("\n");
			int y = py, x;
			for (String l : lines) {
				x = center ? px + (w - fontRenderer.getStringWidth(l)) / 2 : px;
				fontRenderer.drawString(l, x, y, tc);
				y += fh;
			}
			GlStateManager.color(1F, 1F, 1F, 1F);
		}

	}

	public class TextField extends GuiComp<String> {
		public final int maxL;
		public int tc = 0xff404040, cc = 0xff800000;
		public String text = "";
		public int cur;
		public boolean allowFormat = false;

		public TextField(int id, int x, int y, int w, int h, int max) {
			super(id, x, y, w, h);
			this.maxL = max;
		}

		public TextField(int id, int x, int y, int w, int h, int max, Supplier<String> get, Consumer<String> update) {
			super(id, x, y, w, h, get, null, update);
			this.maxL = max;
		}

		public TextField color(int text, int cursor) {
			this.tc = text; this.cc = cursor;
			return this;
		}

		public TextField allowFormat() {
			this.allowFormat = true;
			return this;
		}

		@Override
		public void draw() {
			String t;
			int ofs = 0;
			if (focus == id) {
				if (cur > text.length()) cur = text.length();
				int l = fontRenderer.getStringWidth(text);
				int k = fontRenderer.getStringWidth(text.substring(0, cur));
				if (l > w)
					if (k <= w/2) {
						t = fontRenderer.trimStringToWidth(text, w, false);
					} else if (l - k < w/2) {
						k = k - l + w;
						t = fontRenderer.trimStringToWidth(text, w, true);
						ofs = w - fontRenderer.getStringWidth(t);
					} else {
						k = w/2;
						t = fontRenderer.trimStringToWidth(text.substring(0, cur), k, true);
						ofs = k - fontRenderer.getStringWidth(t);
						t += fontRenderer.trimStringToWidth(text.substring(cur), w - k, false);
					}
				else t = text;
				drawVerticalLine(px - 1 + k, py + (h - 9) / 2, py + (h + 7) / 2, cc);
			} else {
				text = get.get();
				t = fontRenderer.trimStringToWidth(text, w, true);
				if (t.length() < text.length()) ofs = w - fontRenderer.getStringWidth(t);
			}
			fontRenderer.drawString(t, px + ofs, py + (h - 8) / 2, tc);
			GlStateManager.color(1, 1, 1, 1);
		}

		@Override
		public void keyTyped(char c, int k) {
			try {
				boolean ctr = isCtrlKeyDown() && !isShiftKeyDown() && !isAltKeyDown();
				switch(k) {
				case Keyboard.KEY_LEFT: if (cur > 0) cur--; break;
				case Keyboard.KEY_RIGHT: if (cur < text.length()) cur++; break;
				case Keyboard.KEY_DELETE: if (cur < text.length()) {
						text = text.substring(0, cur).concat(text.substring(cur + 1));
					} break;
				case Keyboard.KEY_BACK: if (cur > 0) {
						cur--;
						text = text.substring(0, cur).concat(text.substring(cur + 1));
					} break;
				case Keyboard.KEY_RETURN: setFocus(-1); break;
				case Keyboard.KEY_UP: setFocus(id - 1); break;
				case Keyboard.KEY_DOWN: setFocus(id + 1); break;
				case Keyboard.KEY_C: if (ctr) {
						setClipboardString(text);
						break;
					}
				case Keyboard.KEY_V: if (ctr) {
						String s = getClipboardString();
						text = text.substring(0, cur).concat(s).concat(text.substring(cur, text.length()));
						cur += s.length();
						if (text.length() > maxL) {
							text = text.substring(0, maxL);
							cur = maxL;
						}
						break;
					}
				case Keyboard.KEY_D: if (ctr) {
						text = "";
						break;
					}
				default: if (cur < maxL && (allowFormat && c == '\u00a7' || ChatAllowedCharacters.isAllowedCharacter(c))){
						text = text.substring(0, cur).concat("" + c).concat(text.substring(cur, Math.min(text.length(), maxL - 1)));
						cur++;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				if (cur < 0) cur = 0;
				if (cur > text.length()) cur = text.length();
			}
		}

		@Override
		public void unfocus() {
			update.accept(text);
		}

		@Override
		public boolean focus() {
			text = get.get();
			cur = text.length();
			return true;
		}

	}

	public class Slider extends GuiComp<Float> {
		public final int l, tx, ty, tw, th;
		public final boolean hor;
		public float scrollStep = 0.125F;

		public Slider(int id, int x, int y, int l, int texX, int texY, int texW, int texH, boolean hor) {
			super(id, x, y, hor?l:texW, hor?texH:l);
			this.hor = hor;
			this.l = l;
			this.tx = texX;
			this.ty = texY;
			this.tw = texW;
			this.th = texH;
		}

		public Slider(int id, int x, int y, int l, int texX, int texY, int texW, int texH, boolean hor, Supplier<Float> get, Consumer<Float> set, Consumer<Float> update) {
			super(id, x, y, hor?l:texW, hor?texH:l, get, set, update);
			this.hor = hor;
			this.l = l;
			this.tx = texX;
			this.ty = texY;
			this.tw = texW;
			this.th = texH;
		}

		public Slider scroll(float step) {
			scrollStep = step;
			return this;
		}

		@Override
		public void draw() {
			mc.renderEngine.bindTexture(MAIN_TEX);
			int f = (int)(get.get() * (float)l) - (hor?tw:th) / 2;
			drawTexturedModalRect(hor? px + f : px, hor? py : py + f, tx, ty, tw, th);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			float f = d == 3 ? get.get() + (float)b * scrollStep + 1e-5F : ((float)(hor? x - px : y - py) + 0.5F) / (float)l;
			if (f < 0) f = 0;
			else if (f > 1) f = 1;
			(d == 3 ? update : set).accept(f);
			if (d == 2) setFocus(-1);
			return true;
		}

		@Override
		public void unfocus() {
			if (update != null) update.accept(get.get());
		}

		@Override
		public boolean focus() {return true;}
	}

	public class NumberSel extends GuiComp<Integer> {
		public boolean hor = false, above = false;
		public int ts = 4, tc = 0xff404040, nb = 1, min, max, exp;
		public final String form;

		public NumberSel(int id, int px, int py, int w, int h, String form, int min, int max, int exp) {
			super(id, px, py, w, h);
			this.min = min;
			this.max = max;
			this.exp = exp;
			this.form = form;
		}

		public NumberSel(int id, int px, int py, int w, int h, String form, int min, int max, int exp, Supplier<Integer> get, Consumer<Integer> update) {
			super(id, px, py, w, h, get, null, update);
			this.min = min;
			this.max = max;
			this.exp = exp;
			this.form = form;
		}

		public NumberSel setup(int ts, int tc, int nb, boolean hor) {
			this.nb = nb;
			this.tc = tc;
			this.ts = ts / 2;
			this.hor = hor;
			return this;
		}

		public NumberSel around() {
			above = true;
			return this;
		}

		@Override
		public void draw() {
			String s = String.format(form, get.get());
			int x = px + (w - fontRenderer.getStringWidth(s)) / 2, y = py + (h - 8) / 2;
			fontRenderer.drawString(s, x, y, tc);
			GlStateManager.color(1F, 1F, 1F, 1F);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			int ofs;
			if (d == 3) ofs = -b * (isShiftKeyDown() ? exp : 1);
			else if (d == 0) {
				if (above) {
					int p = (px + w - 1 - x) * nb / w + b;
					y -= py + h / 2;
					if (y < -ts) {
						ofs = 1;
						for (int i = 0; i < p; i++) ofs *= exp;
					} else if (y >= ts) {
						ofs = -1;
						for (int i = 0; i < p; i++) ofs *= exp;
					} else ofs = 0;
				} else {
					int pw = (hor ? w : h) / 2, p = (hor ? x - px : py + h - 1 - y) - pw;
					if (p < -ts) {
						p = (-p - ts) * nb / (pw - ts) * 2 + b;
						ofs = -1;
						for (int i = 0; i < p; i++) ofs *= exp;
					} else if (p >= ts) {
						p = (p - ts) * nb / (pw - ts) * 2 + b;
						ofs = 1;
						for (int i = 0; i < p; i++) ofs *= exp;
					} else ofs = 0;
				}
			} else return true;
			if (ofs != 0)
				update.accept(Math.max(min, Math.min(max, get.get() + ofs)));
			return true;
		}

	}

	public class Button extends GuiComp<Object> {

		public final int states;
		public int tx, ty;

		public Button(int id, int px, int py, int w, int h, int states) {
			super(id, px, py, w, h);
			this.states = states;
		}

		public Button(int id, int px, int py, int w, int h, int states, Supplier<Object> get, Consumer<Object> update) {
			super(id, px, py, w, h, get, null, update);
			this.states = states;
		}

		public Button(int id, int px, int py, int w, int h, Consumer<Object> update) {
			super(id, px, py, w, h, null, null, update);
			this.states = -1;
		}

		public Button texture(int tx, int ty) {
			this.tx = tx;
			this.ty = ty;
			return this;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			super.drawOverlay(mx, my);
			Object o;
			if (states >= 0 && (o = get.get()) instanceof EnumFacing)
				drawSideCube(tabsX + guiLeft - 64, tabsY + guiTop + 63, ((EnumFacing)o).ordinal(), (byte)states);
		}

		@Override
		public void draw() {
			if (states < 0) return;
			Object o = get.get();
			int s = o instanceof EnumFacing ? ((EnumFacing)o).ordinal() : (Integer)o;
			mc.renderEngine.bindTexture(MAIN_TEX);
			drawTexturedModalRect(px, py, tx, ty + s * h, w, h);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d == 3) return true;
			update.accept(b);
			return b == 0;
		}

	}

	public class ProgressBar extends GuiComp<Float> {
		public final byte type;
		public final int tx, ty;

		/** @param type 0:horFrac, 1:vertFrac, 2:horShift, 3:vertShift, 4:precision */
		public ProgressBar(int id, int px, int py, int w, int h, int tx, int ty, byte type) {
			super(id, px, py, w, h);
			this.type = type;
			this.tx = tx;
			this.ty = ty;
		}

		/** @param type 0:horFrac, 1:vertFrac, 2:horShift, 3:vertShift, 4:precision */
		public ProgressBar(int id, int px, int py, int w, int h, int tx, int ty, byte type, Supplier<Float> get) {
			super(id, px, py, w, h, get, null, null);
			this.type = type;
			this.tx = tx;
			this.ty = ty;
		}

		@Override
		public void draw() {
			float f = get.get();
			if (Float.isNaN(f)) return;
			if (type < 2 || type == 4) {
				if (f > 1) f = 1;
				else if (f < -1) f = -1;
			}
			mc.renderEngine.bindTexture(MAIN_TEX);
			boolean v = (type & 1) != 0;
			if (type == 0 || type == 1) {
				int n = (int)((float)(v?h:w) * (f<0?-f:f));
				int dx = (!v && f<0)? w - n : 0, dy = (v && f>0)? h - n : 0;
				drawTexturedModalRect(px + dx, py + dy, tx + dx, ty + dy, v?w:n, v?n:h);
			} else if (type == 2 || type == 3) {
				int n = (int)((float)(v?h:w) * f);
				drawTexturedModalRect(px, py, tx + (v?0:n), ty + (v?n:0), w, h);
			} else if (type == 4) {
				int n = (int)((float)(w * h) * (f<0?-f:f)), m = n / h; n %= h;
				int dx = f<0 ? w - m : 0, dx1 = f<0 ? w - m - 1 : m, dy1 = f<0 ? h - n : 0;
				drawTexturedModalRect(px + dx, py, tx + dx, ty, m, h);
				drawTexturedModalRect(px + dx1, py + dy1, tx + dx1, ty + dy1, 1, n);
			}
		}

	}

	public class InfoTab extends GuiComp<Object> {

		final String[] headers, keys;
		int page = 0;

		public InfoTab(int id, int px, int py, int w, int h, String tooltip) {
			super(id, px, py, w, h);
			setTooltip(tooltip);
			headers = TooltipUtil.translate("gui.cd4017be." + tooltip).split("\n");
			keys = new String[headers.length];
			initHeader();
		}

		private void initHeader() {
			for (int i = 0; i < keys.length; i++) {
				String s = headers[i];
				int p = s.indexOf('@');
				if (p < 0) keys[i] = "gui.cd4017be." + tooltip + i;
				else {
					keys[i] = "gui.cd4017be." + s.substring(p + 1).trim();
					headers[i] = s.substring(0, p);
				}
			}
		}

		@Override
		public void drawOverlay(int mx, int my) {
			if (TooltipUtil.editor != null) {
				String[] s = TooltipUtil.translate("gui.cd4017be." + tooltip).split("\n");
				System.arraycopy(s, 0, headers, 0, Math.min(s.length, headers.length));
				initHeader();
			}
			ArrayList<String> list = new ArrayList<String>();
			String s = "";
			for (int i = 0; i < headers.length; i++) {
				String h = headers[i];
				if (!h.isEmpty() && h.charAt(0) == ChatFormatting.PREFIX_CODE) {
					s += h.substring(0, 2);
					h = h.substring(2);
				}
				if (i == page) s += "" + ChatFormatting.PREFIX_CODE + ChatFormatting.UNDERLINE.getChar();
				s += h + ChatFormatting.PREFIX_CODE + ChatFormatting.RESET.getChar() + " | ";
			}
			list.add(s.substring(0, s.length() - 3));
			for (String l : TooltipUtil.getConfigFormat(keys[page]).split("\n"))
				list.add(l);
			drawHoveringText(list, mx, my);
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d == 0) {
				if (b == 0) page++;
				else if (b == 1) page--;
				else return false;
			} else if (d == 3) page += b;
			else return false;
			page = Math.floorMod(page, headers.length);
			return true;
		}

	}

	public class FluidTank extends GuiComp<Object> {
		final TankSlot slot;

		public FluidTank(int id, TankSlot slot) {
			super(id, slot.xPos, slot.yPos, (slot.size >> 4 & 0xf) * 18 - 2, (slot.size & 0xf) * 18 - 2, null, null, null);
			this.slot = slot;
		}

		@Override
		public void drawOverlay(int mx, int my) {
			FluidStack stack = slot.getStack();
			ArrayList<String> info = new ArrayList<String>();
			info.add(stack != null ? stack.getLocalizedName() : TooltipUtil.translate("cd4017be.tankEmpty"));
			info.add(TooltipUtil.format("cd4017be.tankAmount", stack != null ? (double)stack.amount / 1000D : 0D, (double)slot.inventory.getCapacity(slot.tankNumber) / 1000D));
			drawHoveringText(info, mx, my, fontRenderer);
		}

		@Override
		public void draw() {
			GlStateManager.disableAlpha();
			ResourceLocation res;
			FluidStack stack = slot.getStack();
			if (stack != null && ((res = stack.getFluid().getStill(stack)) != null || (res = stack.getFluid().getFlowing(stack)) != null)) {
				mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				int c = slot.inventory.getCapacity(slot.tankNumber);
				int n = c == 0 || stack.amount >= c ? h : (int)((long)h * (long)stack.amount / (long)c);
				color(stack.getFluid().getColor(stack));
				drawTexturedModalRect(px, py + h - n, mc.getTextureMapBlocks().getAtlasSprite(res.toString()), w, n);
			}
			color(0xffffffff);
			mc.renderEngine.bindTexture(LIB_TEX);
			drawTexturedModalRect(px + w - 16, py, 110, 52 - h, 16, h);
			GlStateManager.disableBlend();
			GlStateManager.enableAlpha();
		}

		@Override
		public boolean mouseIn(int x, int y, int b, int d) {
			if (d == 0 && inventorySlots instanceof DataContainer) {
				FluidStack fluid = FluidUtil.getFluidContained(((DataContainer)inventorySlots).player.inventory.getItemStack());
				setDisplVar(id, fluid != null ? fluid.getFluid() : null, false);
			}
			return false;
		}

	}

}
