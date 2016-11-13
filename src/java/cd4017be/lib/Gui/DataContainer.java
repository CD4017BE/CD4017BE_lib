package cd4017be.lib.Gui;

import cd4017be.lib.BlockGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A container with no inventory for GUIs that only show data values.
 * @author CD4017BE
 */
public class DataContainer extends Container {

	public final EntityPlayer player;
	public final IGuiData data;
	/** assign this to automatically synchronize getSyncVariables() */
	public int[] refInts;
	/** free to use for anything */
	public Object extraRef;

	public DataContainer(IGuiData data, EntityPlayer player) {
		this.data = data;
		this.player = player;
	}

	private boolean firstTick = true;

	@Override
	public void detectAndSendChanges() {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(data.pos());
		if (this.checkChanges(dos)) 
			for (IContainerListener crafter : this.listeners)
				BlockGuiHandler.sendPacketToPlayer((EntityPlayerMP)crafter, dos);
	}

	protected boolean checkChanges(PacketBuffer dos) {
		boolean send = false;
		int[] arr = data.getSyncVariables();
		if (arr != null) {
			byte[] chng = new byte[(arr.length + 7) / 8];
			boolean init = refInts == null;
			if (init) refInts = new int[arr.length];
			for (int i = 0; i < refInts.length; i++)
				if (init || arr[i] != refInts[i]) {
					chng[i >> 3] |= 1 << (i & 7);
					send = true;
				}
			dos.writeBytes(chng);
			for (int i = 0; i < refInts.length; i++)
				if (init || arr[i] != refInts[i]) dos.writeInt(refInts[i] = arr[i]);
		}
		return data.detectAndSendChanges(this, dos) || send;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return data.canPlayerAccessUI(player);
	}

	public void onDataUpdate(PacketBuffer dis) {
		if (refInts != null) {
			byte[] chng = new byte[(refInts.length + 7) / 8];
			dis.readBytes(chng);
			for (int i = 0; i < chng.length; i++)
				for (int c = chng[i] & 0xff, j = i << 3; c != 0 && j < refInts.length; c >>= 1, j++)
					if ((c & 1) != 0) data.setSyncVariable(j, refInts[j] = dis.readInt());
		}
		data.updateClientChanges(this, dis);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void putStacksInSlots(ItemStack[] stack) {}

	public interface IGuiData {
		public void initContainer(DataContainer container);
		public boolean canPlayerAccessUI(EntityPlayer player);
		public BlockPos pos();
		public int[] getSyncVariables();
		public void setSyncVariable(int i, int v);
		public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos);
		public void updateClientChanges(DataContainer container, PacketBuffer dis);
		public String getName();
	}

}
