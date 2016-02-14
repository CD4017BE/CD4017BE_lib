/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.util.Utils;

import java.io.IOException;
import java.util.BitSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

/**
 * 
 * @author CD4017BE
 */
public class TileEntityData 
{
    
    public TileEntityData(TileEntityData ref)
    {
        longs = new long[ref.longs.length];
        ints = new int[ref.ints.length];
        floats = new float[ref.floats.length];
        fluids = new FluidStack[ref.fluids.length];
        variables = ref.variables;
    }
    
    /**
     * Some variables that are synchronized to the client while Gui open.
     * @param b number of long variables
     * @param i number of integer variables
     * @param f number of float variables
     * @param l number of FluidStack variables
     */
    public TileEntityData(int b, int i, int f, int l)
    {
        longs = new long[b];
        ints = new int[i];
        floats = new float[f];
        fluids = new FluidStack[l];
        variables = b + i + f + l;
    }
    
    public final int variables;
    
    public final long[] longs;
    public final int[] ints;
    public final float[] floats;
    public final FluidStack[] fluids;
    
    public BitSet detectChanges(TileEntityData old)
    {
        BitSet reg = new BitSet(variables);
        int var = 0;
        for (int i = 0; i < longs.length; i++, var++)
            if (longs[i] != old.longs[i])
            {reg.set(var); old.longs[i] = longs[i];}
        for (int i = 0; i < ints.length; i++, var++)
            if (ints[i] != old.ints[i]) 
            {reg.set(var); old.ints[i] = ints[i];}
        for (int i = 0; i < floats.length; i++, var++)
            if (floats[i] != old.floats[i]) 
            {reg.set(var); old.floats[i] = floats[i];}
        for (int i = 0; i < fluids.length; i++, var++)
            if (!Utils.fluidsEqual(fluids[i], old.fluids[i], true)) 
            {reg.set(var); old.fluids[i] = fluids[i] == null ? null : fluids[i].copy();}
        return reg;
    }
    
    public void writeData(PacketBuffer dos, BitSet reg) throws IOException
    {
        writeBitsToStream(reg, variables, dos);
        int var = 0;
        for (int i = 0; i < longs.length; i++, var++)
            if (reg.get(var)) dos.writeLong(longs[i]);
        for (int i = 0; i < ints.length; i++, var++)
            if (reg.get(var)) dos.writeInt(ints[i]);
        for (int i = 0; i < floats.length; i++, var++)
            if (reg.get(var)) dos.writeFloat(floats[i]);
        for (int i = 0; i < fluids.length; i++, var++)
            if (reg.get(var)) {
                NBTTagCompound nbt = new NBTTagCompound();
                if (fluids[i] != null) fluids[i].writeToNBT(nbt);
                else nbt.setString("FluidName", "null");
                dos.writeNBTTagCompoundToBuffer(nbt);
            }
    }
    
    public void readData(PacketBuffer data) throws IOException
    {
        BitSet reg = new BitSet();
        readBitsFromStream(reg, variables, data);
        int var = 0;
        for (int i = 0; i < longs.length; i++, var++)
            if (reg.get(var)) longs[i] = data.readLong();
        for (int i = 0; i < ints.length; i++, var++)
            if (reg.get(var)) ints[i] = data.readInt();
        for (int i = 0; i < floats.length; i++, var++)
            if (reg.get(var)) floats[i] = data.readFloat();
        for (int i = 0; i < fluids.length; i++, var++)
            if (reg.get(var)) {
                fluids[i] = FluidStack.loadFluidStackFromNBT(data.readNBTTagCompoundFromBuffer());
            }
    }
    
    public static void writeBitsToStream(BitSet set, int l, PacketBuffer dos) throws IOException
    {
        byte[] data = new byte[(l + 7) / 8];
        int n = 0;
        for (int i = 0; i < data.length; i++)
        	for (int j = 1; j < 256; j <<= 1, n++)
        		if (set.get(n)) data[i] |= j;
        dos.writeBytes(data);
    }
    
    public static void readBitsFromStream(BitSet set, int l, PacketBuffer dis) throws IOException
    {
        byte[] data = new byte[(l + 7) / 8];
        dis.readBytes(data);
        int n = 0;
        for (int i = 0; i < data.length; i++)
        	for (int j = 1; j < 256; j <<= 1, n++)
        		if ((data[i] & j) != 0) set.set(n);
    }
    
}
