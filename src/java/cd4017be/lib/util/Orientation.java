package cd4017be.lib.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.minecraft.util.EnumFacing.*;

import java.util.Arrays;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.client.renderer.block.model.ModelRotation;

public enum Orientation implements IStringSerializable {
	N(NORTH),	E(EAST),	S(SOUTH),	W(WEST),
	Bn(DOWN),	Be(DOWN),	Bs(DOWN),	Bw(DOWN),
	Rn(SOUTH),	Re(WEST),	Rs(NORTH),	Rw(EAST),
	Tn(UP),		Te(UP),		Ts(UP),		Tw(UP);

	public final EnumFacing front;

	private Orientation(EnumFacing front) {
		this.front = front;
	}

	@Override
	public String getName() {
		return name();
	}

	public EnumFacing rotate(EnumFacing dir) {
		if (dir == EnumFacing.NORTH) return front;
		if (dir.getAxis() != Axis.X) {
			if ((ordinal() & 4) != 0) dir = dir.rotateAround(Axis.X);
			if ((ordinal() & 8) != 0) dir = dir.getOpposite();
		}
		if (dir.getAxis() != Axis.Y) {
			if ((ordinal() & 1) != 0) dir = dir.rotateY();
			if ((ordinal() & 2) != 0) dir = dir.getOpposite();
		}
		return dir;
	}

	public AxisAlignedBB rotate(AxisAlignedBB box) {
		switch(ordinal() >> 2) {
		case 1: box = new AxisAlignedBB(box.minX, box.minZ, 1.0 - box.maxY, box.maxX, box.maxZ, 1.0 - box.minY); break;
		case 2: box = new AxisAlignedBB(box.minX, 1.0 - box.maxY, 1.0 - box.maxZ, box.maxX, 1.0 - box.minY, 1.0 - box.minZ); break;
		case 3: box = new AxisAlignedBB(box.minX, 1.0 - box.maxZ, box.minY, box.maxX, 1.0 - box.minZ, box.maxY); break;
		}
		switch(ordinal() & 3) {
		case 1: box = new AxisAlignedBB(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX); break;
		case 2: box = new AxisAlignedBB(1.0 - box.maxX, box.minY, 1.0 - box.maxZ, 1.0 - box.minX, box.maxY, 1.0 - box.minZ); break;
		case 3: box = new AxisAlignedBB(box.minZ, box.minY, 1.0 - box.maxX, box.maxZ, box.maxY, 1.0 - box.minX); break;
		}
		return box;
	}

	@SideOnly(Side.CLIENT)
	public ModelRotation getModelRotation() {
		return ModelRotation.values()[ordinal()];
	}

	public static final PropertyEnum<Orientation> XY_12_ROT = PropertyEnum.create("dir", Orientation.class, Arrays.asList(Bn, Bs, Bw, Be, Tn, Ts, Tw, Te, N, S, W, E));
	public static final PropertyEnum<Orientation> ALL_AXIS = PropertyEnum.create("dir", Orientation.class, Arrays.asList(Bn, Tn, N, S, W, E));
	public static final PropertyEnum<Orientation> HOR_AXIS = PropertyEnum.create("dir", Orientation.class, Arrays.asList(N, S, W, E));
	public static final PropertyEnum<Orientation> ALL_AXIS_INV = PropertyEnum.create("dir", Orientation.class, Arrays.asList(Bs, Ts, Rn, Rs, Rw, Re));
	public static final PropertyEnum<Orientation> HOR_AXIS_INV = PropertyEnum.create("dir", Orientation.class, Arrays.asList(Rn, Rs, Rw, Re));
}
