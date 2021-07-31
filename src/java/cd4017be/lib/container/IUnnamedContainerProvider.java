package cd4017be.lib.container;

import cd4017be.lib.text.TooltipUtil;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;

/**An {@link MenuProvider} that has its name predefined to empty string.
 * @author CD4017BE */
public interface IUnnamedContainerProvider extends MenuProvider {

	@Override
	default Component getDisplayName() { return TooltipUtil.EMPTY; }

}
