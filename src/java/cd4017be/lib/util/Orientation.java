package cd4017be.lib.util;

import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3i;

import static net.minecraft.util.Direction.*;

import cd4017be.math.Orient;

/**
 * 
 * @author CD4017BE
 */
public enum Orientation implements IStringSerializable {
	S12(UP, SOUTH), S9(EAST, SOUTH), S6(DOWN, SOUTH), S3(WEST, SOUTH),
	W12(UP, WEST), W9(SOUTH, WEST), W6(DOWN, WEST), W3(NORTH, WEST),
	N12(UP, NORTH), N9(WEST, NORTH), N6(DOWN, NORTH), N3(EAST, NORTH),
	E12(UP, EAST), E9(NORTH, EAST), E6(DOWN, EAST), E3(SOUTH, EAST),
	UN(NORTH, UP), UE(EAST,  UP), US(SOUTH,  UP), UW(WEST, UP),
	DS(SOUTH,DOWN), DE(EAST, DOWN), DN(NORTH, DOWN), DW(WEST,DOWN);

	private static final Orientation[] VALUES = values(), INVERSE = {
		S12, S3, S6, S9,
		E12, UW, W6, DE,
		N12, N9, N6, N3,
		W12, DW, E6, UE,
		DS, E3, US, W9,
		UN, W3, DN, E9,
	};

	/** pointing right or along input X axis */
	public final Direction r;
	/** pointing up or along input Y axis */
	public final Direction u;
	/** pointing back or along input Z axis */
	public final Direction b;
	/** {@link Orient} equivalent */
	public final int o;

	private Orientation(Direction u, Direction b) {
		this.u = u;
		this.b = b;
		Vector3i vec = u.getNormal().cross(b.getNormal());
		this.r = Direction.fromNormal(vec.getX(), vec.getY(), vec.getZ());
		this.o = (r.getAxis().ordinal() << 1 | r.ordinal() & 1
		| u.getAxis().ordinal() << 5 | u.ordinal() << 4 & 0x10
		| b.getAxis().ordinal() << 9 | b.ordinal() << 8 & 0x100)
		^ 0x111;
	}

	public static Orientation byBack(Direction b) {
		for (int i = 0; i < 24; i+=4) {
			Orientation o = VALUES[i];
			if (o.b == b) return o;
		}
		return null;
	}

	public static Orientation byBackUp(Direction b, Direction u) {
		for (int i = 0; i < 24; i+=4) {
			Orientation o = VALUES[i];
			if (o.b != b) continue;
			if (o.u == u) return o;
			if ((o = VALUES[i+1]).u == u) return o;
			if ((o = VALUES[i+2]).u == u) return o;
			if ((o = VALUES[i+3]).u == u) return o;
			return null;
		}
		return null;
	}

	public static Orientation byBackRight(Direction b, Direction r) {
		for (int i = 0; i < 24; i+=4) {
			Orientation o = VALUES[i];
			if (o.b != b) continue;
			if (o.r == r) return o;
			if ((o = VALUES[i+1]).r == r) return o;
			if ((o = VALUES[i+2]).r == r) return o;
			if ((o = VALUES[i+3]).r == r) return o;
			return null;
		}
		return null;
	}

	public static Orientation byIndex(int i) {
		return VALUES[(i &= 31) < 24 ? i : i-8];
	}

	/**@return orientation that reverses this one */
	public Orientation inv() {
		return INVERSE[ordinal()];
	}

	public Orientation apply(Orientation orient) {
		return byBackUp(apply(orient.b), apply(orient.u));
	}

	public Direction apply(Direction dir) {
		switch(dir) {
		case DOWN: return u.getOpposite();
		case UP: return u;
		case NORTH: return b.getOpposite();
		case SOUTH: return b;
		case WEST: return r.getOpposite();
		default: return r;
		}
	}

	public VoxelShape apply(VoxelShape main) {
		throw new UnsupportedOperationException();
		// TODO implement
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}

}
