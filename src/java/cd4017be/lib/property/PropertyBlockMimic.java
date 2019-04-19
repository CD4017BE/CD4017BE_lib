package cd4017be.lib.property;

import net.minecraft.block.state.IBlockState;

/**
 * 
 * @author CD4017BE
 */
public class PropertyBlockMimic extends PropertyWrapObj<IBlockState> {

	public static final PropertyBlockMimic instance = new PropertyBlockMimic("minic");

	public PropertyBlockMimic(String name) {
		super(name, IBlockState.class);
	}

}
