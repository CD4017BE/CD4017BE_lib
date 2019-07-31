package cd4017be.api.rs_ctr.sensor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cd4017be.api.rs_ctr.com.BlockReference;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Remote Comparator API for special sensor augments.<dl>
 * To add your own sensors simply write an implementation for this interface and register an instance of it alongside one or more ItemStacks using {@link SensorRegistry#register(java.util.function.Function, ItemStack...)}.<br>
 * Also implement {@link INBTSerializable} if this contains state to synchronize.
 * @author CD4017BE
 */
public interface IBlockSensor {

	/**
	 * perform the scan operation on a given BlockReference
	 * @param block the block face to scan (chunk is guaranteed to be loaded).
	 * @return a signal value for the given block
	 */
	int readValue(@Nonnull BlockReference block);

	/**
	 * called when the BlockReference supplied to the comparator changes
	 * @param block the new block
	 */
	default void onRefChange(@Nullable BlockReference block, IHost host) { }

	/**
	 * @return text displayed when aimed at
	 */
	String getTooltipString();

	/**
	 * @return a model to render on top of the remote comparator
	 * @see SensorRegistry#RENDERER
	 */
	@Nullable ResourceLocation getModel();

	/**
	 * The device hosting the sensor
	 * @author CD4017BE
	 */
	public interface IHost {
		/**
		 * call this whenever the sensor's {@link INBTSerializable} state was modified.
		 */
		void syncSensorState();
	}

}
