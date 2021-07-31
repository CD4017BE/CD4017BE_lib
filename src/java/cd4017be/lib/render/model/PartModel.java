package cd4017be.lib.render.model;

import java.util.List;

import cd4017be.math.Orient;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelProperty;

/**ModelProperty for {@link TileEntityModel} to render a part model.
 * @author CD4017BE */
public class PartModel {

	public final String name;
	private final float[] ofs;
	private int orient = Orient.IDEN;

	/**@param name of a part specified in model.json */
	public PartModel(String name) {
		this.name = name;
		this.ofs = new float[3];
	}

	/**@param x model relative X-offset
	 * @param y model relative Y-offset
	 * @param z model relative Z-offset
	 * @return this (before {@link #orient()}) */
	public PartModel offset(float x, float y, float z) {
		ofs[0] = x;
		ofs[1] = y;
		ofs[2] = z;
		return this;
	}

	/**@param o re-orientation to apply
	 * @param x0 absolute X-origin
	 * @param y0 absolute Y-origin
	 * @param z0 absolute Z-origin
	 * @return this */
	public PartModel orient(int o, float x0, float y0, float z0) {
		this.orient = o;
		Orient.origin(o, ofs, x0, y0, z0);
		return this;
	}

	public boolean hasTransform() {
		return orient != Orient.IDEN || ofs[0] != 0 || ofs[1] != 0 || ofs[2] != 0;
	}

	public Direction getSource(Direction d) {
		return d == null ? null : Orient.orient(Orient.inv(orient), d);
	}

	public BakedQuad transform(BakedQuad quad) {
		return Orient.orient(orient, quad, ofs);
	}

	public static final ModelProperty<List<PartModel>> PART_MODELS = new ModelProperty<>();

}