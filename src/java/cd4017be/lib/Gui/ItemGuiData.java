package cd4017be.lib.Gui;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import cd4017be.lib.Gui.DataContainer.IGuiData;

public class ItemGuiData implements IGuiData {

	protected final Item item;
	private ItemStack lastState;

	public ItemGuiData(Item item) {
		this.item = item;
	}

	@Override
	public void initContainer(DataContainer container) {}

	@Override
	public boolean canPlayerAccessUI(EntityPlayer player) {
		ItemStack item = player.inventory.mainInventory[player.inventory.currentItem];
		return item != null && item.getItem() == this.item;
	}

	@Override
	public BlockPos pos() {return new BlockPos(0, -1, 0);}

	@Override
	public int[] getSyncVariables() {return null;}

	@Override
	public void setSyncVariable(int i, int v) {}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		ItemStack item = container.player.inventory.mainInventory[container.player.inventory.currentItem];
		if (item == null || container instanceof TileContainer) return false;
		if (!ItemStack.areItemStacksEqual(lastState, item)) {
			dos.writeItemStackToBuffer(lastState = item.copy());
			return true;
		} else return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		if (dis.readableBytes() > 0) try {
			ItemStack item = dis.readItemStackFromBuffer();
			if (item.getItem() == this.item) container.player.inventory.mainInventory[container.player.inventory.currentItem] = item;
		} catch (IOException e) {}
	}

}
