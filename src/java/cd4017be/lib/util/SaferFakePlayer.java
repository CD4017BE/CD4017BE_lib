package cd4017be.lib.util;

import java.util.Set;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;


/**
 * This should fix problems with unsupported methods (involving network communication attempts) that were overseen by Forge's FakePlayer implementation.
 * @author CD4017BE
 */
public class SaferFakePlayer extends FakePlayer {

	/**
	 * @param world
	 * @param name
	 */
	public SaferFakePlayer(WorldServer world, GameProfile name) {
		super(world, name);
		setNetHandler(this);
	}

	@Override public void displayGui(IInteractionObject guiOwner) {}
	@Override public void displayGUIChest(IInventory chestInventory) {}
	@Override public void displayVillagerTradeGui(IMerchant villager) {}
	@Override public void displayGuiCommandBlock(TileEntityCommandBlock commandBlock) {}
	@Override public boolean isPotionApplicable(PotionEffect potioneffectIn) {return false;}
	@Override public void openEditSign(TileEntitySign signTile) {}
	@Override public void openGuiHorseInventory(AbstractHorse horse, IInventory inventoryIn) {}
	@Override public void openBook(ItemStack stack, EnumHand hand) {}
	@Override public void setPositionAndUpdate(double x, double y, double z) {}
	@Override public SleepResult trySleep(BlockPos bedLocation) {return SleepResult.OTHER_PROBLEM;}
	@Override public boolean startRiding(Entity entityIn, boolean force) {return false;}
	@Override public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {}
	@Override public String getPlayerIP() {return "";}

	private static void setNetHandler(SaferFakePlayer player) {
		if (FAKE_NET_HANDLER != null) {
			player.connection = FAKE_NET_HANDLER;
			return;
		}
		FAKE_NET_HANDLER = new NetHandlerPlayServer(null, new NetworkManager(EnumPacketDirection.SERVERBOUND), player) {
			@Override public void sendPacket(Packet<?> packetIn) {}
			@Override public void update() {}
			@Override public void disconnect(ITextComponent textComponent) {}
			@Override public void setPlayerLocation(double x, double y, double z, float yaw, float pitch, Set<EnumFlags> relativeSet) {}
			@Override public void onDisconnect(ITextComponent reason) {}
		};
		FAKE_NET_HANDLER.player = null; //player was only provided to prevent NPE in constructor.
	}

	/**dummy net handler shared across all FakePlayer instances. Only used to avoid issues with attempts to send network packets to the FakePlayer */
	private static NetHandlerPlayServer FAKE_NET_HANDLER;

}
