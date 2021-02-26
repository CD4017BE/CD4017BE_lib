package cd4017be.lib.Gui;

import java.io.IOException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandlerModifiable;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.util.TooltipUtil;

/**
 * 
 * @author CD4017BE
 */
public class ItemGuiData implements IGuiData {

	protected final Item item;
	private ItemStack lastState = ItemStack.EMPTY;
	protected IInventoryItem inv;

	public ItemGuiData(Item item) {
		this.item = item;
	}

	@Override
	public void initContainer(DataContainer container) {}

	@Override
	public boolean canPlayerAccessUI(EntityPlayer player) {
		ItemStack item = player.inventory.mainInventory.get(player.inventory.currentItem);
		return item.getItem() == this.item;
	}

	@Override
	public BlockPos pos() {return new BlockPos(0, -1, 0);}

	@Override
	public int[] getSyncVariables() {return null;}

	@Override
	public void setSyncVariable(int i, int v) {}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		ItemStack item = container.player.inventory.mainInventory.get(container.player.inventory.currentItem);
		if (item.isEmpty() || container instanceof TileContainer) return false;
		if (!ItemStack.areItemStacksEqual(lastState, item)) {
			dos.writeItemStack(lastState = item.copy());
			return true;
		} else return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		if (dis.readableBytes() > 0) try {
			ItemStack item = dis.readItemStack();
			if (item.getItem() == this.item) container.player.inventory.mainInventory.set(container.player.inventory.currentItem, item);
		} catch (IOException e) {}
	}

	public static void updateInventory(EntityPlayer player, int slot) {
		if (player.inventory.currentItem == slot && player.openContainer instanceof TileContainer) {
			TileContainer cont = (TileContainer)player.openContainer;
			if (cont.data instanceof ItemGuiData) {
				ItemGuiData data = (ItemGuiData)cont.data;
				if (data.inv != null) data.inv.update();
			}
		}
	}

	@Override
	public String getName() {
		return TooltipUtil.translate(item.getUnlocalizedName().replace("item.", "gui.") + ".name");
	}

	public interface IInventoryItem extends IItemHandlerModifiable {
		void update();
	}

}
