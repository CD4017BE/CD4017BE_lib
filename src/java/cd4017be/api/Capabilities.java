package cd4017be.api;

import java.util.concurrent.Callable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

/**
 * 
 * @author CD4017BE
 */
public class Capabilities {

	private static final Exception NotSupported = new Exception("You can't put this capability on everything you want! Modder, please inform yourself about how it works and use a constructor!");

	public static void register() {
	}

	public static <T>  void registerIntern(Class<T> cap) {
		CapabilityManager.INSTANCE.register(cap, new EmptyStorage<T>(), new EmptyCallable<T>());
	}

	public static class EmptyStorage<T> implements Capability.IStorage<T> {
		@Override
		public NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side) {
			return new NBTTagCompound();
		}
		@Override
		public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt) {}
	}

	public static class EmptyCallable<T> implements Callable<T> {
		@Override
		public T call() throws Exception {
			throw NotSupported;
		}
	}

}
