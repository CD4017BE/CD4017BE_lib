/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

/**
 *
 * @author CD4017BE
 */
public class AreaProtect implements ForgeChunkManager.LoadingCallback
{
    public static byte permissions = 1;
    public static byte chunkloadPerm = 1;
    public static byte maxChunksPBlock = 24;
	public static AreaProtect instance = new AreaProtect();
	private static boolean registered = false;
	private static Object mod;
	
	public static void register(Object mod) {
		if (!registered) {
			AreaProtect.mod = mod;
			MinecraftForge.EVENT_BUS.register(instance);
			ForgeChunkManager.setForcedChunkLoadingCallback(mod, instance);
		}
		registered = true;
	}
	
    @SubscribeEvent
    public void handlePlayerInteract(PlayerInteractEvent event)
    {
        if (permissions < 0) return;
    	if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_BLOCK) {
            ProtectLvl pl = this.getPlayerAccess(event.entityPlayer.getName(), event.entityPlayer.worldObj, event.pos.getX() >> 4, event.pos.getZ() >> 4);
            if (pl != ProtectLvl.Free && !(pl == ProtectLvl.Protected && event.action == Action.RIGHT_CLICK_BLOCK && event.entityPlayer.getCurrentEquippedItem() == null)) {
                event.setCanceled(true);
            }
        } else if (event.action == Action.RIGHT_CLICK_AIR) {
            ProtectLvl pl = this.getPlayerAccess(event.entityPlayer.getName(), event.entityPlayer.worldObj, (int)Math.floor(event.entityPlayer.posX) >> 4, (int)Math.floor(event.entityPlayer.posZ) >> 4);
            if (pl != ProtectLvl.Free && pl != ProtectLvl.Protected) {
                event.setCanceled(true);
            }
        }
    }
    
    public HashMap<Integer, ArrayList<IAreaConfig>> loadedSS = new HashMap<Integer, ArrayList<IAreaConfig>>();
    public HashMap<Integer, ArrayList<Ticket>> usedTickets = new HashMap<Integer, ArrayList<Ticket>>();
    
    /**
     * @param name
     * @param world
     * @param chunkX
     * @param chunkZ
     * @return the restriction level for given username at given position.
     */
    public ProtectLvl getPlayerAccess(String name, World world, int chunkX, int chunkZ)
    {
    	if (permissions < 0) return ProtectLvl.Free;
    	int ac = 0;
    	ArrayList<IAreaConfig> list = loadedSS.get(world.provider.getDimensionId());
    	if (list == null) return ProtectLvl.Free;
    	for (IAreaConfig cfg : list) {
            ac = Math.max(ac, cfg.getProtectLvlFor(name, chunkX, chunkZ));
        }
        return ProtectLvl.getLvl(ac);
    }
    
    public boolean isOperationAllowed(String player, World world, int cx, int cz)
    {
    	if (permissions < 0) return true;
    	ArrayList<IAreaConfig> list = loadedSS.get(world.provider.getDimensionId());
    	if (list == null) return true;
    	for (IAreaConfig cfg : list) {
            if (cfg.getProtectLvlFor(player, cx, cz) != 0) return false;
        }
        return true;
    }
    
    public boolean isOperationAllowed(String player, World world, int x0, int x1, int z0, int z1)
    {
    	if (permissions < 0) return true;
    	for (int cx = x0 >> 4; cx < x1 >> 4; cx++)
    		for (int cz = z0 >> 4; cz < z1 >> 4; cz++)
    			if (!isOperationAllowed(player, world, cx, cz))
    				return false;
    	return true;
    }
    
    public boolean isInteractingAllowed(String player, World world, int cx, int cz)
    {
    	if (permissions < 0) return true;
    	ArrayList<IAreaConfig> list = loadedSS.get(world.provider.getDimensionId());
    	if (list == null) return true;
    	for (IAreaConfig cfg : list) {
            if (cfg.getProtectLvlFor(player, cx, cz) > 1) return false;
        }
        return true;
    }
    
    public void loadSecuritySys(IAreaConfig config)
    {
    	int[] pos = config.getPosition();
    	ArrayList<IAreaConfig> list = loadedSS.get(pos[3]);
        if (list == null) {
        	loadedSS.put(pos[3], list = new ArrayList<IAreaConfig>());
        }
    	list.add(config);
    	ArrayList<Ticket> tickets = usedTickets.get(pos[3]);
    	if (tickets != null) {
    		NBTTagCompound tag;
    		for (Ticket t : tickets) {
    			tag = t.getModData();
    			if (tag.getInteger("bx") == pos[0] && tag.getInteger("by") == pos[1] && tag.getInteger("bz") == pos[2]) {
    				config.setTicket(t);
    				break;
    			}
    		}
    	}
    }
    
    public void removeChunkLoader(IAreaConfig config)
    {
    	int d = config.getPosition()[3];
    	ArrayList<Ticket> list = usedTickets.get(d);
    	Ticket t = config.getTicket();
    	if (t != null) {
    		config.setTicket(null);
    		ForgeChunkManager.releaseTicket(t);
    		if (list != null) {
    			list.remove(t);
    			if (list.isEmpty()) usedTickets.remove(d);
            }
    	}
    }
    
    public void supplyTicket(IAreaConfig config, World world)
    {
    	int[] p = config.getPosition();
    	ArrayList<Ticket> list = usedTickets.get(p[3]);
    	NBTTagCompound tag;
    	if (list != null) for (Ticket t : list) {
    		tag = t.getModData();
    		if (tag.getInteger("bx") == p[0] && tag.getInteger("by") == p[1] && tag.getInteger("bz") == p[2]) {
    			config.setTicket(t);
    			return;
    		}
    	}
    	Ticket t = ForgeChunkManager.requestTicket(mod, world, Type.NORMAL);
    	if (t == null) return;
    	tag = t.getModData();
    	tag.setInteger("bx", p[0]);
    	tag.setInteger("by", p[1]);
    	tag.setInteger("bz", p[2]);
    	t.setChunkListDepth(maxChunksPBlock);
    	config.setTicket(t);
    }
    
    public void unloadSecuritySys(IAreaConfig config)
    {
    	int d = config.getPosition()[3];
    	ArrayList<IAreaConfig> list = loadedSS.get(d);
    	if (list != null) {
    		list.remove(config);
    		if (list.isEmpty()) loadedSS.remove(d);
    	}
    }

	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) 
	{
		ArrayList<Ticket> list = usedTickets.get(world.provider.getDimensionId());
        if (list == null) {
        	usedTickets.put(world.provider.getDimensionId(), list = new ArrayList<Ticket>());
        }
        list.addAll(tickets);
	}
    
}
