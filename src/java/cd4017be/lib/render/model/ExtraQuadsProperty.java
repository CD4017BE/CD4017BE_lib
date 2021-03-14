package cd4017be.lib.render.model;

import java.util.List;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.ModelProperty;

/**Lets a TileEntity pass arbitrary BakedQuads to the {@link TileEntityModel}.
 * @author CD4017BE */
public class ExtraQuadsProperty extends ModelProperty<List<BakedQuad>> {
	public static final ExtraQuadsProperty EXTRA_QUADS = new ExtraQuadsProperty(null);
	private static final ExtraQuadsProperty[] SIDE_QUADS = new ExtraQuadsProperty[6];
	static {
		for (Direction d : Direction.values())
			SIDE_QUADS[d.getIndex()] = new ExtraQuadsProperty(d);
	}

	public final Direction side;

	private ExtraQuadsProperty(Direction side) {
		this.side = side;
	}

	public static ExtraQuadsProperty of(Direction side) {
		return side == null ? EXTRA_QUADS : SIDE_QUADS[side.getIndex()];
	}
}