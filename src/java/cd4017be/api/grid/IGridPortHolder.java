package cd4017be.api.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**Lets TileEntities have signal connections with
 * {@link GridPart}s or other {@link IGridPortHolder}s.
 * @author CD4017BE */
public interface IGridPortHolder extends IPortHolder {

	ExtGridPorts extPorts();

	Level world();

	BlockPos pos();

	@Override
	default boolean isMaster(int channel) {
		return extPorts().isMaster(channel);
	}

}
