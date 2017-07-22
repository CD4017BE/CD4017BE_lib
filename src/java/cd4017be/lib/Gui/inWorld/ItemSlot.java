package cd4017be.lib.Gui.inWorld;

import cd4017be.lib.render.InWorldUIRenderer;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class ItemSlot extends UIcomp {

	protected final IItemHandler inv;
	protected final int slot;

	public ItemSlot(AxisAlignedBB bounds, IItemHandler inv, int slot) {
		super(bounds);
		this.inv = inv;
		this.slot = slot;
	}

	@Override
	public boolean interact(RayTraceResult hit, EntityPlayer player, ClickType type) {
		ItemStack item = player.getHeldItemMainhand(), item1, item2;
		switch(type) {
		case use:
			item1 = inv.getStackInSlot(slot);
			if (!item1.isEmpty() && !ItemHandlerHelper.canItemStacksStack(item1, item))
				item2 = inv.extractItem(slot, item1.getMaxStackSize(), false);
			else item2 = ItemStack.EMPTY;
			item1 = inv.insertItem(slot, item, false);
			if (item1.getCount() != item.getCount()) {
				if (item1.isEmpty() && !item2.isEmpty()) {
					item1 = item2;
					item2 = ItemStack.EMPTY;
				}
				player.setHeldItem(EnumHand.MAIN_HAND, item1);
				break;
			}
			ItemFluidUtil.dropStack(item2, player);
		case hit:
			item1 = inv.getStackInSlot(slot);
			if (item1.isEmpty()) break;
			item1 = inv.extractItem(slot, item1.getMaxStackSize(), false);
			ItemFluidUtil.dropStack(item1, player);
			break;
		case scrollUp:
			if (item.isEmpty()) break;
			item.grow(inv.insertItem(slot, item.splitStack(1), false).getCount());
			player.setHeldItem(EnumHand.MAIN_HAND, item);
			break;
		case scrollDown:
			item1 = inv.extractItem(slot, 1, false);
			if (item1.isEmpty()) break;
			if (item.isEmpty()) item = item1;
			else if (ItemHandlerHelper.canItemStacksStack(item1, item)) item.grow(item1.getCount());
			else {
				ItemFluidUtil.dropStack(item, player);
				item = item1;
			}
			player.setHeldItem(EnumHand.MAIN_HAND, item);
			break;
		}
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void draw(InWorldUIRenderer tesr) {
		final double scale = 1.0;
		GlStateManager.pushMatrix();
		GlStateManager.translate((bounds.minX + bounds.maxX) * 0.5, (bounds.minY + bounds.maxY) * 0.5, (bounds.minZ + bounds.maxZ) * 0.5);
		GlStateManager.scale((bounds.maxX - bounds.minX) * scale, (bounds.maxY - bounds.minY) * scale, (bounds.maxZ - bounds.minZ) * scale);
		tesr.itemRenderer.renderItem(inv.getStackInSlot(slot), TransformType.FIXED);
		GlStateManager.popMatrix();
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void drawOverlay(InWorldUIRenderer tesr, RayTraceResult hit, EntityPlayer player) {
		GlStateManager.pushMatrix();
		GlStateManager.translate((bounds.minX + bounds.maxX) * 0.5, (bounds.minY + bounds.maxY) * 0.5, (bounds.minZ + bounds.maxZ) * 0.5);
		final double scale = 1.0 / 16.0;
		GlStateManager.scale((bounds.maxX - bounds.minX) * scale, (bounds.maxY - bounds.minY) * scale, (bounds.maxZ - bounds.minZ) * scale);
		GlStateManager.rotate(player.cameraPitch, 1, 0, 0);
		GlStateManager.rotate(player.cameraYaw, 0, 1, 0);
		GlStateManager.translate(-8, -8, -8);
		tesr.itemRenderer.renderItemOverlays(tesr.getFontRenderer(), inv.getStackInSlot(slot), 0, 0);
		GlStateManager.popMatrix();
	}

}
