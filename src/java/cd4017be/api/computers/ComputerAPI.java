/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.api.computers;

import java.lang.reflect.Method;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

import org.apache.logging.log4j.Level;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Optional;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 */
@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheralProvider", modid = "ComputerCraft")
public class ComputerAPI implements IPeripheralProvider
{
    private static boolean registered = false;
    public static final ComputerAPI instance = new ComputerAPI();
    public static Class CCcomp, CCapi, CCperProv;
    public static Class OCcomp;
    public static Method CCevent;
    public static Method OCevent;
    
    public static void register()
    {
    	if (registered) return;
        //ComputerCraft
    	try {
        	CCapi = Class.forName("dan200.computercraft.api.ComputerCraftAPI");
			CCcomp = Class.forName("dan200.computercraft.api.peripheral.IComputerAccess");
			CCperProv = Class.forName("dan200.computercraft.api.peripheral.IPeripheralProvider");
			FMLLog.log("CD4017BE_lib", Level.INFO, "ComputerCraft API found");
		} catch (ClassNotFoundException e) {
			FMLLog.log("CD4017BE_lib", Level.INFO, "ComputerCraft API not available!");
			CCapi = null;
			CCcomp = null;
			CCperProv = null;
		}
        if (CCapi != null)
			try {
				CCapi.getMethod("registerPeripheralProvider", CCperProv).invoke(null, instance);
				CCevent = CCcomp.getMethod("queueEvent", String.class, Object[].class);
			} catch (Exception e) {
				FMLLog.log("CD4017BE_lib", Level.ERROR, e, "can't get API methods:");
			}
        //OpenComputers
        try {
        	OCcomp = Class.forName("li.cil.oc.api.machine.Context");
        	FMLLog.log("CD4017BE_lib", Level.INFO, "OpenComputers API found");
        } catch (ClassNotFoundException e) {
        	FMLLog.log("CD4017BE_lib", Level.INFO, "OpenComputers API not available!");
        }
        if (OCcomp != null)
			try {
				OCevent = OCcomp.getMethod("signal", String.class, Object[].class);
			} catch (Exception e) {
				FMLLog.log("CD4017BE_lib", Level.ERROR, e, "can't get API methods:");
			}
        
    	registered = true;
    }
    
    public static void sendEvent(Object obj, String name, Object... args)
    {
    	if (obj == null) return;
    	try {
    		if (OCcomp.isInstance(obj)) OCevent.invoke(obj, name, args);
        	else if (CCcomp.isInstance(obj)) CCevent.invoke(obj, name, args);
		} catch (Exception e) {
			FMLLog.log(Level.ERROR, e, "can't send event to computer!");
		}
    }
    
    public static Object newOCnode(TileEntity tile, String name, boolean power)
    {
    	return tile instanceof Environment && API.network != null ? power ? API.network.newNode((Environment)tile, Visibility.Network).withComponent(name).withConnector().create() : API.network.newNode((Environment)tile, Visibility.Network).withComponent(name).create() : null;
    }
    
    public static double update(TileEntity tile, Object node, double energy)
    {
    	if (node == null || !(node instanceof Component)) return 0;
    	if (((Component)node).network() == null) Network.joinOrCreateNetwork(tile);
        if (node instanceof ComponentConnector) return energy - ((ComponentConnector)node).changeBuffer(energy * 0.001D) * 1000D;
        return 0;
    }
    
    public static void removeOCnode(Object node)
    {
    	if (node != null) ((Node)node).remove();
    }
    
    @Optional.Method(modid = "ComputerCraft")
    //@Override
    public IPeripheral getPeripheral(World world, BlockPos pos, int s) 
    {
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof IPeripheral) return (IPeripheral)te;
        else return null;
    }
    
}
