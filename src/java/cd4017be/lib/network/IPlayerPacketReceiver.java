package cd4017be.lib.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

/**
 * implemented by TileEntities or Containers to receive data packets send by a players client
 * @author CD4017BE
 */
public interface IPlayerPacketReceiver {
	/**
	 * Handle a data packet from given player
	 * @param pkt contained payload
	 * @param sender the player who sent this
	 * @throws Exception potential decoding errors
	 */
	public void handlePlayerPacket(PacketBuffer pkt, EntityPlayerMP sender) throws Exception;
}
