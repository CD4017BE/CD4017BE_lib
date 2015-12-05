/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib.templates;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 */
public class ComputerPeripheralProvider implements IPeripheralProvider
{
    private static boolean registered = false;
    public static final ComputerPeripheralProvider instance = new ComputerPeripheralProvider();
    
    public static void register()
    {
        if (!registered) {
            ComputerCraftAPI.registerPeripheralProvider(instance);
            registered = true;
        }
    }
    
    @Override
    public IPeripheral getPeripheral(World world, int x, int y, int z, int s) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof IPeripheral) return (IPeripheral)te;
        else return null;
    }
    
}
