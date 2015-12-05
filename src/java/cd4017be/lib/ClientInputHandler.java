package cd4017be.lib;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ClientInputHandler 
{
	private static final Minecraft mc = Minecraft.getMinecraft();
	public static ClientInputHandler instance = new ClientInputHandler();
	private static boolean initialized = false;
	
	public static void init()
    {
		if (initialized) return;
        MinecraftForge.EVENT_BUS.register(instance);
        initialized = true;
    }
	
	@SubscribeEvent
	public void handleMouseInput(MouseEvent e)
	{
		if (mc.thePlayer != null && mc.gameSettings != null && mc.thePlayer.isSneaking()) {
			ItemStack item = mc.thePlayer.getCurrentEquippedItem();
	    	if (item != null && item.getItem() instanceof IScrollHandlerItem && (e.dwheel != 0 || (e.button > 1 && e.buttonstate))) {
	    		((IScrollHandlerItem)item.getItem()).onSneakScroll(item, mc.thePlayer, e.dwheel);
	    		e.setCanceled(true);
	    	}
		}
	}
	
	public interface IScrollHandlerItem
	{
		@SideOnly(Side.CLIENT)
		public void onSneakScroll(ItemStack item, EntityPlayer player, int scroll);
	} 
}
