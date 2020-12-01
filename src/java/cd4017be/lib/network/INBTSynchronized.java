package cd4017be.lib.network;

import net.minecraft.nbt.NBTTagCompound;

/** 
 * @author CD4017BE */
public interface INBTSynchronized {

	/**make this save it's state to given data.
	 * @param nbt serialized nbt data
	 * @param mode the type of data to store
	 */
	default void storeState(NBTTagCompound nbt, int mode) {
		Synchronizer.of(this.getClass()).writeNBT(this, nbt, mode);
	}

	/**make this load it's state from given data.
	 * @param nbt serialized nbt data
	 * @param mode the type of data to load
	 */
	default void loadState(NBTTagCompound nbt, int mode) {
		Synchronizer.of(this.getClass()).readNBT(this, nbt, mode);
	}

}
