package cd4017be.lib.event;

import cd4017be.lib.render.ModTextureMap;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.Event;


/**
 * A variant of {@link TextureStitchEvent} designated to non vanilla texture maps
 * @see ModTextureMap
 * @author CD4017BE
 */
public class ModTextureStitchEvent extends Event {

	private final ModTextureMap map;

	public ModTextureStitchEvent(ModTextureMap map) {
		this.map = map;
	}

	public ModTextureMap getMap() {
		return map;
	}

	/**
	 * Fired when the TextureMap is told to refresh it's stitched texture. 
	 * Called after the Stitched list is cleared, but before any objects
	 * add themselves to the list.
	 */
	public static class Pre extends ModTextureStitchEvent {
		public Pre(ModTextureMap map){ super(map); }
	}

	/**
	 * This event is fired once the texture map has loaded all textures and 
	 * stitched them together. All Icons should have there locations defined
	 * by the time this is fired.
	 */
	public static class Post extends ModTextureStitchEvent {
		public Post(ModTextureMap map){ super(map); }
	}

}
