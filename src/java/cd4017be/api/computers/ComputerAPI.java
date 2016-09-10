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

import cd4017be.api.energy.EnergyAPI;
import cd4017be.api.energy.EnergyOpenComputers;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
@SuppressWarnings("rawtypes")
public class ComputerAPI {
	public static final ComputerAPI instance = new ComputerAPI();
	public static Class OCcomp, OCapi;
	public static Method CCevent;
	public static Method OCevent;
	private static boolean OCinstalled = false;
	
	@SuppressWarnings("unchecked")
	public static void register() {
		//OpenComputers
		OCinstalled = Loader.isModLoaded("OpenComputers");
		try {
			OCcomp = Class.forName("li.cil.oc.api.machine.Context");
			OCapi = Class.forName("li.cil.oc.api.API");
			FMLLog.log("CD4017BE_lib", Level.INFO, "OpenComputers API found");
		} catch (ClassNotFoundException e) {
			FMLLog.log("CD4017BE_lib", Level.INFO, "OpenComputers API not available!");
		}
		if (OCcomp != null)
			try {
				OCevent = OCcomp.getMethod("signal", String.class, Object[].class);
				EnergyAPI.handlers.add(1, new EnergyOpenComputers());//insert just after main to ensure it's called before RF.
			} catch (Exception e) {
				FMLLog.log("CD4017BE_lib", Level.ERROR, e, "can't get API methods:");
			}
	}

	public static boolean isOCinstalled() {
		return OCinstalled;
	}

	public static void sendEvent(Object obj, String name, Object... args) {
		if (obj == null) return;
		try {
			if (OCcomp.isInstance(obj)) OCevent.invoke(obj, name, args);
		} catch (Exception e) {
			FMLLog.log(Level.ERROR, e, "can't send event to computer!");
		}
	}

	public static Object newOCnode(TileEntity tile, String name, boolean power) {
		if (!OCinstalled) return null;
		return newOCnode1(tile, name, power);
	}

	public static double update(TileEntity tile, Object node, double energy) {
		if (node == null || !OCinstalled) return 0;
		return update1(tile, node, energy);
	}

	public static void removeOCnode(Object node) {
		if (!OCinstalled) return;
		removeOCnode1(node);
	}

	@Optional.Method(modid = "OpenComputers")
	public static void saveNode(Object node, NBTTagCompound nbt) {
		((Node)node).save(nbt);
	}

	@Optional.Method(modid = "OpenComputers")
	public static void readNode(Object node, NBTTagCompound nbt) {
		((Node)node).load(nbt);
	}

	@Optional.Method(modid = "OpenComputers")
	private static Object newOCnode1(TileEntity tile, String name, boolean power) {
		return tile instanceof Environment && API.network != null ? power ? API.network.newNode((Environment)tile, Visibility.Network).withComponent(name).withConnector().create() : API.network.newNode((Environment)tile, Visibility.Network).withComponent(name).create() : null;
	}

	@Optional.Method(modid = "OpenComputers")
	private static double update1(TileEntity tile, Object node, double energy) {
		if (!(node instanceof Component)) return 0;
		if (((Component)node).network() == null) Network.joinOrCreateNetwork(tile);
		if (node instanceof ComponentConnector) return energy - ((ComponentConnector)node).changeBuffer(energy * 0.001D) * 1000D;
		return 0;
	}

	@Optional.Method(modid = "OpenComputers")
	private static void removeOCnode1(Object node) {
		if (node != null) ((Node)node).remove();
	}

}
