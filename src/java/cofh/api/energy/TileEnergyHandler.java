package cofh.api.energy;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEnergyHandler extends TileEntity
  implements IEnergyHandler
{
  protected EnergyStorage storage = new EnergyStorage(32000);

  public void readFromNBT(NBTTagCompound paramNBTTagCompound)
  {
    super.readFromNBT(paramNBTTagCompound);
    this.storage.readFromNBT(paramNBTTagCompound);
  }

  public void writeToNBT(NBTTagCompound paramNBTTagCompound)
  {
    super.writeToNBT(paramNBTTagCompound);
    this.storage.writeToNBT(paramNBTTagCompound);
  }

  public boolean canConnectEnergy(ForgeDirection paramForgeDirection)
  {
    return true;
  }

  public int receiveEnergy(ForgeDirection paramForgeDirection, int paramInt, boolean paramBoolean)
  {
    return this.storage.receiveEnergy(paramInt, paramBoolean);
  }

  public int extractEnergy(ForgeDirection paramForgeDirection, int paramInt, boolean paramBoolean)
  {
    return this.storage.extractEnergy(paramInt, paramBoolean);
  }

  public int getEnergyStored(ForgeDirection paramForgeDirection)
  {
    return this.storage.getEnergyStored();
  }

  public int getMaxEnergyStored(ForgeDirection paramForgeDirection)
  {
    return this.storage.getMaxEnergyStored();
  }
}