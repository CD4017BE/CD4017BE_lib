package cd4017be.lib.block;

import net.minecraft.block.properties.IProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author cd4017be
 *
 */
public interface IMultipartBlock {

	IProperty<?> getBaseState();

	int moduleCount();

	@SideOnly(Side.CLIENT)
	Class<?> moduleType(int i);

	@SideOnly(Side.CLIENT)
	String moduleVariant(int i);

	boolean renderMultilayer();

}
