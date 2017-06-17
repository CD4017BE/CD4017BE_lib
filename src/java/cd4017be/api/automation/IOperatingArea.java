package cd4017be.api.automation;

import cd4017be.api.IAbstractTile;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

/**
 *
 * @author CD4017BE
 */
public interface IOperatingArea extends IAbstractTile {

	public int[] getOperatingArea();

	public void updateArea(int[] area);

	public IItemHandler getUpgradeSlots();

	public int[] getBaseDimensions();

	public boolean remoteOperation(BlockPos pos);

	public IOperatingArea getSlave();

	public static final short[] mult = {1, 4, 1};
	public static final short[] base = {1, 1, 2};
	public static class Handler {

		public static boolean renderArea(IOperatingArea mach) {
			return mach.getUpgradeSlots().getStackInSlot(0) != null;
		}

		public static int[] maxSize(IOperatingArea mach) {
			int[] dim = mach.getBaseDimensions();
			ItemStack item = mach.getUpgradeSlots().getStackInSlot(1);
			int n = item == null ? base[0] : base[0] + mult[0] * item.getCount();
			return new int[]{dim[0] * n / base[0], Math.min(dim[1] * n / base[0], 256), dim[2] * n / base[0]};
		}

		public static int maxRange(IOperatingArea mach) {
			IItemHandler upgr = mach.getUpgradeSlots();
			int[] dim = mach.getBaseDimensions();
			if (dim[3] == Integer.MAX_VALUE) return dim[3];
			ItemStack item = upgr.getStackInSlot(1);
			int n = item == null ? base[0] : base[0] + mult[0] * item.getCount();
			item = upgr.getStackInSlot(2);
			n *= (item == null ? base[1] : base[1] + mult[1] * item.getCount());
			return dim[3] * n / base[0] / base[1];
		}

		public static int Umax(IOperatingArea mach) {
			int[] dim = mach.getBaseDimensions();
			ItemStack item = mach.getUpgradeSlots().getStackInSlot(3);
			int n = item == null ? base[2] : base[2] + mult[2] * item.getCount();
			return dim[4] * n / base[2];
		}

		public static boolean setCorrectArea(IOperatingArea tile, int[] area, boolean correct) {
			TileEntity te = (TileEntity)tile;
			int maxD = maxRange(tile);
			int[] maxS = maxSize(tile);
			if (area[3] < area[0]) {int k = area[0]; area[0] = area[3]; area[3] = k;}
			if (area[4] < area[1]) {int k = area[1]; area[1] = area[4]; area[4] = k;}
			if (area[5] < area[2]) {int k = area[2]; area[2] = area[5]; area[5] = k;}
			int sx = area[3] - area[0] - maxS[0];
			int sy = area[4] - area[1] - maxS[1];
			int sz = area[5] - area[2] - maxS[2];
			int dx0 = area[0] - te.getPos().getX() - 1;
			int dy0 = area[1] - te.getPos().getY() - 1;
			int dz0 = area[2] - te.getPos().getZ() - 1;
			int dx1 = te.getPos().getX() - area[3];
			int dy1 = te.getPos().getY() - area[4];
			int dz1 = te.getPos().getZ() - area[5];
			if (sx <= 0 && sy <= 0 && sz <= 0 && dx0 <= maxD && dy0 <= maxD && dz0 <= maxD && dx1 <= maxD && dy1 <= maxD && dz1 <= maxD) {
				tile.updateArea(area);
				return true;
			} else if (!correct) return false;
			
			boolean cx = false, cy = false, cz = false;
			if (dx0 > maxD) {
				area[0] -= dx0 - maxD;
				if (dx0 - maxD + sx > 0) area[3] = area[0] + maxS[0];
			} else if (dx1 > maxD) {
				area[3] += dx1 - maxD;
				if (dx1 - maxD + sx > 0) area[0] = area[3] - maxS[0];
			} else if (sx > 0) {
				if(dx1 > dx0) area[0] = area[3] - maxS[0];
				else area[3] = area[0] + maxS[0];
			} else cx = true;
			if (dy0 > maxD) {
				area[1] -= dy0 - maxD;
				if (dy0 - maxD + sy > 0) area[4] = area[1] + maxS[1];
			} else if (dy1 > maxD) {
				area[4] += dy1 - maxD;
				if (dy1 - maxD + sy > 0) area[1] = area[4] - maxS[1];
			} else if (sy > 0) {
				if (dy1 > dy0) area[1] = area[4] - maxS[1];
				else area[4] = area[1] + maxS[1];
			} else cy = true;
			if (dz0 > maxD) {
				area[2] -= dz0 - maxD;
				if (dz0 - maxD + sz > 0) area[5] = area[2] + maxS[2];
			} else if (dz1 > maxD) {
				area[5] += dz1 - maxD;
				if (dz1 - maxD + sz > 0) area[2] = area[5] - maxS[2];
			} else if (sz > 0) {
				if (dz1 > dz0) area[2] = area[5] - maxS[2];
				else area[5] = area[2] + maxS[2];
			} else cz = true;
			tile.updateArea(area);
			return cx && cy && cz;
		}
	}
}
