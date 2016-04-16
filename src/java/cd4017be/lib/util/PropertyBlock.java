package cd4017be.lib.util;

import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class PropertyBlock implements IUnlistedProperty<IBlockState> {

	private final String name;
	
	public PropertyBlock(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid(IBlockState value) {
		return true;
	}

	@Override
	public Class<IBlockState> getType() {
		return IBlockState.class;
	}

	@Override
	public String valueToString(IBlockState value) {
		return value == null ? "none" : value.getBlock().getUnlocalizedName() + ":" + value.getBlock().getMetaFromState(value);
	}

}
