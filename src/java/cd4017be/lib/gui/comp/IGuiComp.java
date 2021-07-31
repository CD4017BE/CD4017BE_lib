package cd4017be.lib.gui.comp;

import com.mojang.blaze3d.vertex.PoseStack;

import cd4017be.lib.util.IndexedSet.IndexedElement;

/**
 * Gui-component that supports rendering a background and an overlay when hovered and reacts to mouse and keyboard inputs.
 * @author CD4017BE
 *
 */
public interface IGuiComp extends IndexedElement {

	/**
	 * @return the container holding this
	 */
	GuiCompGroup getParent();
	
	/**
	 * @return whether this gui element is enabled
	 */
	boolean enabled();
	
	/**
	 * @param enable whether this gui element should be enabled
	 */
	void setEnabled(boolean enable);

	/**
	 * @param mx
	 * @param my
	 * @return whether given point is inside
	 */
	boolean isInside(int mx, int my);
	
	/**
	 * @param dx delta X
	 * @param dy delta Y
	 */
	void move(int dx, int dy);

	/**
	 * render things in foreground when hovered by cursor
	 * @param mx cursor X
	 * @param my cursor Y
	 * @param t frame time
	 */
	default void drawOverlay(PoseStack stack, int mx, int my) {}

	/**
	 * render background
	 * @param mx cursor X
	 * @param my cursor Y
	 * @param t frame time
	 */
	default void drawBackground(PoseStack stack, int mx, int my, float t) {}

	/**
	 * @param c char typed
	 * @param k key-id
	 * @param d event type: 0=pressed 1=released
	 * @return consume event
	 */
	default boolean keyIn(char c, int k, byte d) {return false;}

	/**
	 * @param mx absolute screen X
	 * @param my absolute screen Y
	 * @param b mouse button: 0=left 1=right 2=middle or +/-1 for scroll
	 * @param d event type: 0=click 1=clickMove 2=release 3=scroll
	 * @return consume event
	 */
	default boolean mouseIn(int mx, int my, int b, byte d) {return false;}

	/**
	 * unfocuses this gui element
	 */
	default void unfocus() {}

	/**
	 * attempts to focus this gui element
	 * @return do focus
	 */
	default boolean focus() {return false;}

	/** Action and button ID constants */
	public static final byte
	A_DOWN = 0, A_HOLD = 1, A_UP = 2, A_SCROLL = 3,
	B_LEFT = 0, B_RIGHT = 1, B_MID = 2;

}
