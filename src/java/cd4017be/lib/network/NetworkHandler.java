package cd4017be.lib.network;

import java.util.Collection;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import cd4017be.lib.Lib;
import cd4017be.lib.util.DimPos;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fmllegacy.LogicalSidedProvider;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;
import net.minecraftforge.fmllegacy.network.event.EventNetworkChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author CD4017BE
 *
 */
public abstract class NetworkHandler implements Consumer<NetworkEvent>, IServerPacketReceiver, IPlayerPacketReceiver {

	private static final long LOG_INTERVAL = 1000;
	public static final Marker NETWORK = MarkerManager.getMarker("Network");

	public final EventNetworkChannel eventChannel;
	public final ResourceLocation channel;

	private long lastErr;
	private int errCount;

	/**
	 * @param channel packet channel id
	 */
	protected NetworkHandler(ResourceLocation channel) {
		this.channel = channel;
		this.eventChannel = NetworkRegistry.newEventChannel(channel, this::version, this::acceptClient, this::acceptServer);
		eventChannel.addListener(this);
	}

	protected abstract String version();

	protected boolean acceptClient(String version) {
		return version().equals(version);
	}

	protected boolean acceptServer(String version) {
		return version().equals(version);
	}

	@Override
	public void accept(NetworkEvent e) {
		if (e instanceof NetworkEvent.ChannelRegistrationChangeEvent) return;
		FriendlyByteBuf data = e.getPayload();
		Context c = e.getSource().get();
		String source = "UNKNOWN";
		try {
			switch(c.getDirection()) {
			case PLAY_TO_CLIENT:
				source = "SERVER";
				handleServerPacket(data);
				break;
			case PLAY_TO_SERVER:
				ServerPlayer player = c.getSender();
				source = "PLAYER \"" + player.getName() + "\"";
				handlePlayerPacket(data, player);
				break;
				//TODO login packets
			default:
				return;
			}
			c.setPacketHandled(true);
		} catch (Exception ex) {
			logError(data, source, ex);
		}
	}

	protected void logError(FriendlyByteBuf buf, String source, Exception e) {
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

	public static void printPacketData(StringBuilder sb, FriendlyByteBuf p) {
		int r = p.readerIndex(), l = p.writerIndex();
		sb.append("read ").append(r).append(" of ").append(l).append(" bytes [");
		for (int i = 0; i < l; i++)
			sb.append(String.format("%02X ", p.getUnsignedByte(i)));
		sb.setCharAt(sb.length() - 1, ']');
	}

	public Packet<?> packet2C(FriendlyByteBuf data) {
		return NetworkDirection.PLAY_TO_CLIENT.buildPacket(Pair.of(data, 0), channel).getThis();
	}

	/**
	 * send a the given packet from client to the server
	 * @param pkt payload
	 */
	@OnlyIn(Dist.CLIENT)
	public void sendToServer(FriendlyByteBuf pkt) {
		Minecraft.getInstance().getConnection().send(
			NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(pkt, 0), channel).getThis()
		);
	}

	/**
	 * send a the given packet from server to a player's client
	 * @param pkt payload
	 * @param player receiver
	 */
	public void sendToPlayer(FriendlyByteBuf pkt, ServerPlayer player) {
		player.connection.connection.send(packet2C(pkt));
	}

	/**
	 * send a the given packet from server to a multiple players' clients
	 * @param pkt payload
	 * @param players receivers
	 */
	public void sendToPlayers(FriendlyByteBuf pkt, Collection<ServerPlayer> players) {
		Packet<?> packet = packet2C(pkt);
		for (ServerPlayer player : players)
			player.connection.connection.send(packet);
	}

	/**
	 * send a the given packet from server to all players that stand within given range
	 * @param pkt payload
	 * @param pos target location
	 * @param range [m] maximum (straight) distance away
	 */
	public void sendToAllNear(FriendlyByteBuf pkt, DimPos pos, double range) {
		LogicalSidedProvider.INSTANCE.<MinecraftServer>get(LogicalSide.SERVER).getPlayerList().broadcast(
			null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range, pos.dim, packet2C(pkt)
		);
	}

}
