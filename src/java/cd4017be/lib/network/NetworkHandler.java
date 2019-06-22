package cd4017be.lib.network;

import java.util.Collection;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import cd4017be.lib.Lib;
import cd4017be.lib.util.DimPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
public abstract class NetworkHandler implements IServerPacketReceiver, IPlayerPacketReceiver {

	private static final long LOG_INTERVAL = 1000;
	public static final Marker NETWORK = MarkerManager.getMarker("Network");

	public final FMLEventChannel eventChannel;
	public final String channel;

	private long lastErr;
	private int errCount;

	/**
	 * @param channel packet channel id
	 */
	protected NetworkHandler(String channel) {
		this.channel = channel;
		(this.eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channel)).register(this);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onPacketFromServer(ClientCustomPacketEvent event) {
		FMLProxyPacket packet = event.getPacket();
		if (!packet.channel().equals(channel)) return;
		PacketBuffer buf = new PacketBuffer(packet.payload());
		try {
			handleServerPacket(buf);
		} catch (Exception e) {
			logError(buf, "SERVER", e);
		}
	}

	@SubscribeEvent
	public void onPacketFromClient(ServerCustomPacketEvent event) {
		FMLProxyPacket packet = event.getPacket();
		if (!packet.channel().equals(channel) || !(event.getHandler() instanceof NetHandlerPlayServer)) return;
		EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).player;
		ByteBuf b = packet.payload();
		PacketBuffer buf = b instanceof PacketBuffer ? (PacketBuffer)b : new PacketBuffer(b);
		try {
			handlePlayerPacket(buf, player);
		} catch (Exception e) {
			logError(buf, "PLAYER \"" + player.getName() + "\"", e);
		}
	}

	protected void logError(PacketBuffer buf, String source, Exception e) {
		long t = System.currentTimeMillis();
		if (t - lastErr < LOG_INTERVAL) errCount++;
		else {
			lastErr = t;
			if (errCount > 0) {
				Lib.LOG.error(NETWORK, "suppressed {} additional packet processing errors within the last {}ms", errCount, t - lastErr);
				errCount = 0;
			}
			StringBuilder sb = new StringBuilder("failed processing packet from ").append(source).append(" @").append(channel).append(": \n");
			printPacketData(sb, buf);
			Lib.LOG.error(NETWORK, sb, e);
		}
	}

	public static void printPacketData(StringBuilder sb, PacketBuffer p) {
		int r = p.readerIndex(), l = p.writerIndex();
		sb.append("read ").append(r).append(" of ").append(l).append(" bytes [");
		for (int i = 0; i < l; i++)
			sb.append(String.format("%02X ", p.getUnsignedByte(i)));
		sb.setCharAt(sb.length() - 1, ']');
	}

	/**
	 * send a the given packet from client to the server
	 * @param pkt payload
	 */
	public void sendToServer(PacketBuffer pkt) {
		eventChannel.sendToServer(new FMLProxyPacket(pkt, channel));
	}

	/**
	 * send a the given packet from server to a player's client
	 * @param pkt payload
	 * @param player receiver
	 */
	public void sendToPlayer(PacketBuffer pkt, EntityPlayerMP player) {
		eventChannel.sendTo(new FMLProxyPacket(pkt, channel), player);
	}

	/**
	 * send a the given packet from server to a multiple players' clients
	 * @param pkt payload
	 * @param players receivers
	 */
	public void sendToPlayers(PacketBuffer pkt, Collection<EntityPlayerMP> players) {
		FMLProxyPacket packet = new FMLProxyPacket(pkt, channel);
		for (EntityPlayerMP player : players)
			eventChannel.sendTo(packet, player);
	}

	/**
	 * send a the given packet from server to all players that stand within given range
	 * @param pkt payload
	 * @param pos target location
	 * @param range [m] maximum (strait) distance away
	 */
	public void sendToAllNear(PacketBuffer pkt, DimPos pos, double range) {
		eventChannel.sendToAllAround(new FMLProxyPacket(pkt, channel),
			new TargetPoint(pos.dimId, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range));
	}

}
