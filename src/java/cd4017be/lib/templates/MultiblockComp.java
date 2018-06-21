package cd4017be.lib.templates;

import cd4017be.api.IAbstractTile;

/**
 * @param <C> should be the class extending this, so that {@code (C)this} won't throw a ClassCastException.
 * @param <N> the type of {@link SharedNetwork} this operates with.
 * @author CD4017BE
 * @deprecated temporary class for backward compatibility
 */
@Deprecated
public abstract class MultiblockComp<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> extends NetworkNode<C, N, IAbstractTile> {

	/**
	 * @param tile
	 */
	public MultiblockComp(IAbstractTile tile) {
		super(tile);
	}

}
