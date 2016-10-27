package cd4017be.api;

import java.util.concurrent.Callable;

import cd4017be.api.automation.PipeEnergy;
import cd4017be.api.circuits.IntegerComp;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class Capabilities {
	private static final Exception NotSupported = new Exception("You can't put this capability on everything you want! Modder, please inform yourself about how it works and use a constructor!");
	
	/** Capability for InductiveAutomation's electric cables */
	@CapabilityInject(PipeEnergy.class)
	public static Capability<PipeEnergy> ELECTRIC_CAPABILITY = null;

	/** Capability for AutomatedRedstone's 32-bit redstone cables */
	@CapabilityInject(IntegerComp.class)
	public static Capability<IntegerComp> RS_INTEGER_CAPABILITY = null;

	public static void register() {
		CapabilityManager.INSTANCE.register(PipeEnergy.class, new Capability.IStorage<PipeEnergy>() {
			@Override
			public NBTBase writeNBT(Capability<PipeEnergy> cap, PipeEnergy pipe, EnumFacing s) {
				NBTTagCompound nbt = new NBTTagCompound();
				pipe.writeToNBT(nbt, "");
				return nbt;
			}
			@Override
			public void readNBT(Capability<PipeEnergy> cap, PipeEnergy pipe, EnumFacing s, NBTBase nbt) {
				pipe.readFromNBT((NBTTagCompound)nbt, "");
			}
		}, new Callable<PipeEnergy>() {
			@Override
			public PipeEnergy call() throws Exception {throw NotSupported;}
		});
		
		CapabilityManager.INSTANCE.register(IntegerComp.class, new Capability.IStorage<IntegerComp>() {
			@Override
			public NBTBase writeNBT(Capability<IntegerComp> cap, IntegerComp pipe, EnumFacing s) {
				NBTTagCompound nbt = new NBTTagCompound();
				pipe.writeToNBT(nbt);
				return nbt;
			}
			@Override
			public void readNBT(Capability<IntegerComp> cap, IntegerComp pipe, EnumFacing s, NBTBase nbt) {
				pipe.readFromNBT((NBTTagCompound)nbt);
			}
		}, new Callable<IntegerComp>(){
			@Override
			public IntegerComp call() throws Exception {throw NotSupported;}
		});
	}
}
