package cd4017be.api.indlog.filter;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 
 * @author cd4017be
 * @param <Obj>
 * @param <Inv>
 */
public abstract class FilterBase<Obj, Inv> implements PipeFilter<Obj, Inv> {

	/** &1=invert; &2=force; &60=extraCfg; &64=invertRS; &128=redstone */
	public byte mode;
	public byte priority;

	@Override
	public boolean active(boolean rs) {
		return (mode & 128) == 0 || (rs ^ (mode & 64) != 0);
	}

	@Override
	public boolean blocking() {
		return (mode & 2) != 0;
	}

	@Override
	public byte priority() {
		return priority;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("mode", mode);
		nbt.setByte("prior", priority);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		mode = nbt.getByte("mode");
		priority = nbt.getByte("prior");
	}

}
