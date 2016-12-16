package cd4017be.api.circuits;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Items that implement this can be used as sensor module for the Advanced Block Sensor / Comparator
 * @author CD4017BE
 */
public interface ISensor {
	/**
	 * Perform a measurement on a Block
	 * @param sensor the sensor module ItemStack
	 * @param world the World
	 * @param pos sensor block location
	 * @return the result 
	 */
	public double measure(ItemStack sensor, World world, BlockPos pos);
}
