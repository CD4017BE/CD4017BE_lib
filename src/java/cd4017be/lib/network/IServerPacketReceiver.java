package cd4017be.lib.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * implemented by TileEntities or Containers to receive data packets send by the server
 * @author CD4017BE
 */
public interface IServerPacketReceiver {
	/**
	 * Handle a data packet from server
	 * @param pkt contained payload
	 * @throws Exception potential decoding errors
	 */
	@SideOnly(Side.CLIENT)
	public void handleServerPacket(PacketBuffer pkt) throws Exception;
}
