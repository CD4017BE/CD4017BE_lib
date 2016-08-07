package cd4017be.lib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
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
		data.initContainer(this);
	}

	@Override
	public void detectAndSendChanges() {
		PacketBuffer dos = BlockGuiHandler.getPacketTargetData(data.getPos());
		if (this.checkChanges(dos)) 
			for (IContainerListener crafter : this.listeners)
				BlockGuiHandler.sendPacketToPlayer((EntityPlayerMP)crafter, dos);
	}

	protected boolean checkChanges(PacketBuffer dos) {
		boolean send = false;
		if (refInts != null) {
			byte[] chng = new byte[(refInts.length + 7) / 8];
			int[] arr = data.getSyncVariables();
			for (int i = 0; i < refInts.length; i++)
				if (arr[i] != refInts[i]) chng[i >> 3] |= 1 << (i & 7);
			dos.writeBytes(chng);
			for (int i = 0; i < refInts.length; i++)
				if (arr[i] != refInts[i]) dos.writeInt(refInts[i] = arr[i]);
			for (byte b : chng)
				if (b != 0) {send = true; break;}
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
        			if ((c & 1) != 0) refInts[j] = dis.readInt();
        }
		data.updateClientChanges(this, dis);
    }

	@Override
	@SideOnly(Side.CLIENT)
	public void putStacksInSlots(ItemStack[] stack) {}

	public interface IGuiData {
		public void initContainer(DataContainer container);
		public boolean canPlayerAccessUI(EntityPlayer player);
		public BlockPos getPos();
		public int[] getSyncVariables();
		public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos);
		public void updateClientChanges(DataContainer container, PacketBuffer dis);
	}

}
