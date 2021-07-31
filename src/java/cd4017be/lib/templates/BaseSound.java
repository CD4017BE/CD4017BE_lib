package cd4017be.lib.templates;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;


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
