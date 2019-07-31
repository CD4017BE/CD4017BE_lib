package cd4017be.lib.templates;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;


/**
 * @author CD4017BE
 *
 */
public class BaseSound extends SoundEvent {

	public BaseSound(ResourceLocation id) {
		super(id);
		setRegistryName(id);
	}

}
