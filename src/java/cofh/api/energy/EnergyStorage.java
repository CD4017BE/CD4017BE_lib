package cofh.api.energy;

import net.minecraft.nbt.NBTTagCompound;

public class EnergyStorage
  implements IEnergyStorage
{
  protected int energy;
  protected int capacity;
  protected int maxReceive;
  protected int maxExtract;

  public EnergyStorage(int paramInt)
  {
    this(paramInt, paramInt, paramInt);
  }

  public EnergyStorage(int paramInt1, int paramInt2)
  {
    this(paramInt1, paramInt2, paramInt2);
  }

  public EnergyStorage(int paramInt1, int paramInt2, int paramInt3)
  {
    this.capacity = paramInt1;
    this.maxReceive = paramInt2;
    this.maxExtract = paramInt3;
  }

  public EnergyStorage readFromNBT(NBTTagCompound paramNBTTagCompound)
  {
    this.energy = paramNBTTagCompound.getInteger("Energy");

    if (this.energy > this.capacity) {
      this.energy = this.capacity;
    }
    return this;
  }

  public NBTTagCompound writeToNBT(NBTTagCompound paramNBTTagCompound)
  {
    if (this.energy < 0) {
      this.energy = 0;
    }
    paramNBTTagCompound.setInteger("Energy", this.energy);
    return paramNBTTagCompound;
  }

  public void setCapacity(int paramInt)
  {
    this.capacity = paramInt;

    if (this.energy > paramInt)
      this.energy = paramInt;
  }

  public void setMaxTransfer(int paramInt)
  {
    setMaxReceive(paramInt);
    setMaxExtract(paramInt);
  }

  public void setMaxReceive(int paramInt)
  {
    this.maxReceive = paramInt;
  }

  public void setMaxExtract(int paramInt)
  {
    this.maxExtract = paramInt;
  }

  public int getMaxReceive()
  {
    return this.maxReceive;
  }

  public int getMaxExtract()
  {
    return this.maxExtract;
  }

  public void setEnergyStored(int paramInt)
  {
    this.energy = paramInt;

    if (this.energy > this.capacity)
      this.energy = this.capacity;
    else if (this.energy < 0)
      this.energy = 0;
  }

  public void modifyEnergyStored(int paramInt)
  {
    this.energy += paramInt;

    if (this.energy > this.capacity)
      this.energy = this.capacity;
    else if (this.energy < 0)
      this.energy = 0;
  }

  public int receiveEnergy(int paramInt, boolean paramBoolean)
  {
    int i = Math.min(this.capacity - this.energy, Math.min(this.maxReceive, paramInt));

    if (!paramBoolean) {
      this.energy += i;
    }
    return i;
  }

  public int extractEnergy(int paramInt, boolean paramBoolean)
  {
    int i = Math.min(this.energy, Math.min(this.maxExtract, paramInt));

    if (!paramBoolean) {
      this.energy -= i;
    }
    return i;
  }

  public int getEnergyStored()
  {
    return this.energy;
  }

  public int getMaxEnergyStored()
  {
    return this.capacity;
  }
}