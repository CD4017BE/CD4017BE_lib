package cd4017be.lib;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 
 * @author CD4017BE
 * @deprecated not fully implemented
 */
public class ClientInputHandler {

	private static final Minecraft mc = Minecraft.getInstance();
	public static ClientInputHandler instance = new ClientInputHandler();
	private static boolean initialized = false;

	public static void init() {
		if (initialized) return;
		MinecraftForge.EVENT_BUS.register(instance);
		initialized = true;
	}

	@SubscribeEvent
	public void handleMouseInput(InputEvent e) {
		/*
		if (mc.player != null && mc.gameSettings != null && mc.player.isSneaking()) {
			ItemStack item = mc.player.getHeldItemMainhand();//TODO add scroll handling for In-World UI blocks
			if (item.getItem() instanceof IScrollHandlerItem && (e.getDwheel() != 0 || (e.getButton() > 1 && e.isButtonstate()))) {
				((IScrollHandlerItem)item.getItem()).onSneakScroll(item, mc.player, e.getDwheel());
				e.setCanceled(true);
			}
		}*/
	}

	/**@deprecated not fully implemented */
	public interface IScrollHandlerItem {
		@OnlyIn(Dist.CLIENT)
		public void onSneakScroll(ItemStack item, PlayerEntity player, int scroll);
	}

}
