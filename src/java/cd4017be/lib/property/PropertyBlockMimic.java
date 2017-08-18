package cd4017be.lib.property;

import net.minecraft.block.state.IBlockState;

public class PropertyBlockMimic extends PropertyWrapObj<IBlockState> {

	public static final PropertyBlockMimic instance = new PropertyBlockMimic("minic");

	private PropertyBlockMimic(String name) {
		super(name, IBlockState.class);
	}

}
