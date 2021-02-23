package cd4017be.lib.container;

import cd4017be.lib.text.TooltipUtil;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;

/**An {@link INamedContainerProvider} that has its name predefined to empty string.
 * @author CD4017BE */
public interface IUnnamedContainerProvider extends INamedContainerProvider {

	@Override
	default ITextComponent getDisplayName() { return TooltipUtil.EMPTY; }

}
