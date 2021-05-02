package cd4017be.math;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class MCConv {

	public static float[] blockRelVecF(Vector3d vec, BlockPos pos) {
		return new float[] {
			(float)(vec.x - pos.getX()),
			(float)(vec.y - pos.getY()),
			(float)(vec.z - pos.getZ())
		};
	}

	public static float[] dirVecF(Direction dir, float l) {
		return new float[] {
			dir.getStepX() * l,
			dir.getStepY() * l,
			dir.getStepZ() * l
		};
	}

	public static float[] intBitsToVec(int n, float[] out, int j, int[] rawIn, int i) {
		for (; n > 0; n--, j++, i++)
			out[j] = Float.intBitsToFloat(rawIn[i]);
		return out;
	}

	public static void vecToIntBits(int n, float[] in, int[] rawOut, int i) {
		for (int j = 0; j < n; j++, i++)
			rawOut[i] = Float.floatToRawIntBits(in[j]);
	}

}
