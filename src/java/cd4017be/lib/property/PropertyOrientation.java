package cd4017be.lib.property;

import java.util.Arrays;
import cd4017be.lib.util.Orientation;
import static cd4017be.lib.util.Orientation.*;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.EnumFacing;
import static net.minecraft.util.EnumFacing.*;

public abstract class PropertyOrientation extends PropertyEnum<Orientation> {

	protected PropertyOrientation(String name, Orientation... allowedValues) {
		super(name, Orientation.class, Arrays.asList(allowedValues));
	}

	public abstract Orientation getPlacementState(boolean sneak, int y, int p, EnumFacing f, float X, float Y, float Z);
	public abstract EnumFacing[] rotations();
	public abstract Orientation getRotatedState(Orientation state, EnumFacing side);

	public static final PropertyOrientation XY_12_ROT = new PropertyOrientation("orient", Bn, Bs, Bw, Be, Tn, Ts, Tw, Te, N, S, W, E) {

		@Override
		public Orientation getPlacementState(boolean sneak, int y, int p, EnumFacing f, float X, float Y, float Z) {
			if (sneak) {
				if (f == DOWN) return Z + X > 1F ? (Z > X ? Bs : Be) : (Z < X ? Bn : Bw);
				if (f == UP) return Z + X > 1F ? (Z > X ? Ts : Te) : (Z < X ? Tn : Tw);
				return Orientation.fromFacing(f.getOpposite());
			}
			return Orientation.values()[y | (p == 0 ? 0 : p < 0 ? 4 : 12)];
		}

		@Override
		public EnumFacing[] rotations() {return EnumFacing.VALUES;}

		@Override
		public Orientation getRotatedState(Orientation state, EnumFacing side) {
			int o = state.ordinal();
			int ofs = side.getAxisDirection() == AxisDirection.NEGATIVE ? 1 : 3;
			switch(side.getAxis()) {
			case Y:
				return Orientation.values()[(o + ofs & 3) | o & 12];
			case X:
				if ((o & 12) != 0) return (o >> 2 & 3) == ofs ? S : N;
				if ((o & 1) != 0) return state;
				return Orientation.values()[o & 3 | ((o & 3) == ofs - 1 ? 4 : 12)];
			case Z:
				if ((o & 12) != 0) return (o >> 2 & 3) == ofs ? E : W;
				if ((o & 1) == 0) return state;
				return Orientation.values()[o & 3 | ((o & 3) == ofs ? 4 : 12)];
			default: return state;
			}
		}

	};

	public static final PropertyOrientation ALL_AXIS = new PropertyOrientation("orient", Bn, Tn, N, S, W, E) {

		@Override
		public Orientation getPlacementState(boolean sneak, int y, int p, EnumFacing f, float X, float Y, float Z) {
			if (sneak) return Orientation.fromFacing(f.getOpposite());
			if (p == 0) return Orientation.values()[y];
			return p < 0 ? Bn : Tn;
		}

		@Override
		public EnumFacing[] rotations() {return EnumFacing.VALUES;}

		@Override
		public Orientation getRotatedState(Orientation state, EnumFacing side) {
			switch(state.front.ordinal() & 6 | (side.ordinal() & 6) << 4 | (state.front.ordinal() ^ side.ordinal()) & 1) {
			case 0x40: case 0x05: return S;
			case 0x41: case 0x04: return N;
			case 0x42: case 0x25: return Bn;
			case 0x43: case 0x24: return Tn;
			case 0x20: case 0x03: return W;
			case 0x21: case 0x02: return E;
			default: return state;
			}
		}

	};

	public static final PropertyOrientation HOR_AXIS = new PropertyOrientation("orient", N, S, W, E) {

		@Override
		public Orientation getPlacementState(boolean sneak, int y, int p, EnumFacing f, float X, float Y, float Z) {
			if (sneak) {
				if (f == DOWN || f == UP) return Z + X > 1F ? (Z > X ? S : E) : (Z < X ? N : W);
				return Orientation.fromFacing(f.getOpposite());
			}
			return Orientation.values()[y];
		}

		@Override
		public EnumFacing[] rotations() {return new EnumFacing[] {DOWN, UP};}

		@Override
		public Orientation getRotatedState(Orientation state, EnumFacing side) {
			int ofs = side == DOWN ? 1 : side == UP ? 3 : 0;
			return Orientation.values()[state.ordinal() + ofs & 3];
		}

	};

}
