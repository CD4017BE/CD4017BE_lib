package cd4017be.lib.network;

import java.util.ArrayDeque;

import cd4017be.lib.Lib;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Provides GUIs and handles their network communication.<dl>
 * To open GUIs with this, simply let your Blocks, Entities and Items implement the corresponding {@link IGuiHandlerBlock}, {@link IGuiHandlerEntity} or {@link IGuiHandlerItem}.
 * And for network communication let your Containers implement {@link IServerPacketReceiver} and {@link IPlayerPacketReceiver}.
 * @author CD4017BE
 */
public class GuiNetworkHandler extends NetworkHandler {

	/**the instance */
	public static GuiNetworkHandler GNH_INSTANCE;

	private static final int MAX_QUEUED = 16;
	private ArrayDeque<FriendlyByteBuf> packetQueue = new ArrayDeque<FriendlyByteBuf>(MAX_QUEUED);

	public static void register() {
		if (GNH_INSTANCE == null) GNH_INSTANCE = new GuiNetworkHandler(Lib.rl("ui"));
	}

	private GuiNetworkHandler(ResourceLocation channel) {
		super(channel);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleServerPacket(FriendlyByteBuf pkt) throws Exception {
		@SuppressWarnings("resource")
		AbstractContainerMenu container = Minecraft.getInstance().player.containerMenu;
		int curId = container.containerId;
		int id = pkt.markReaderIndex().readInt();
		if ((curId == id || curId == 0) && container instanceof IServerPacketReceiver) {
			((IServerPacketReceiver)container).handleServerPacket(pkt);
			if (pkt.readableBytes() > 0) {
				StringBuilder sb = new StringBuilder("Packet > GUI: ");
				printPacketData(sb, pkt);
				Lib.LOG.info(NETWORK, sb.toString());
			}
		} else if (id > curId) {//packet received too early, schedule it for later processing when GUI is opened
			if (packetQueue.size() >= MAX_QUEUED) {
				packetQueue.remove();
				Lib.LOG.warn(NETWORK, "GUI packet queue overflow!");
			}
			pkt.resetReaderIndex();
			packetQueue.add(pkt);
		} else Lib.LOG.warn(NETWORK, "received packet for invalid GUI {} @CLIENT, expected id {} ({})", id, container.containerId, container.getClass());
	}

	@Override
	public void handlePlayerPacket(FriendlyByteBuf pkt, ServerPlayer sender) throws Exception {
		int id = pkt.readInt();
		AbstractContainerMenu container = sender.containerMenu;
		if (container.containerId == id && container instanceof IPlayerPacketReceiver) {
			((IPlayerPacketReceiver)container).handlePlayerPacket(pkt, sender);
			if (pkt.readableBytes() > 0) {
				StringBuilder sb = new StringBuilder("Packet > SERVER: ");
				printPacketData(sb, pkt);
				Lib.LOG.info(NETWORK, sb.toString());
			}
		} else Lib.LOG.warn(NETWORK, "received packet for invalid GUI {} @SERVER, expected id {} ({})", id, container.containerId, container.getClass());
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void onGuiOpened(InitGuiEvent.Post event) {
		if (event.getGui() instanceof AbstractContainerScreen)
			for (int i = packetQueue.size(); i > 0; i--) {
				FriendlyByteBuf buf = packetQueue.remove();
				try {handleServerPacket(buf);}
				catch (Exception e) {logError(buf, "QUEUED", e);}
			}
	}

	/**
	 * @param container the container involved in GUI communication
	 * @return a new FriendlyByteBuf with prepared header
	 */
	public static FriendlyByteBuf preparePacket(AbstractContainerMenu container) {
		FriendlyByteBuf pkt = new FriendlyByteBuf(Unpooled.buffer());
		pkt.writeInt(container.containerId);
		return pkt;
	}

	@Override
	protected String version() {
		return "0";
	}

}
