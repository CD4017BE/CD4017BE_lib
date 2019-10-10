package cd4017be.api.rs_ctr.sensor;

import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.util.ResourceLocation;


/**
 * Simply reads the vanilla comparator value.
 * @author CD4017BE
 */
public class Comparator implements IBlockSensor {

	@Override
	public int readValue(BlockReference block) {
		return block.getState().getComparatorInputOverride(block.world(), block.pos);
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.none");
	}

	@Override
	public ResourceLocation getModel() {
		return null;
	}

}
