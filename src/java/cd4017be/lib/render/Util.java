package cd4017be.lib.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cd4017be.lib.util.Orientation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Util {

	public static final FloatBuffer[] matrices = new FloatBuffer[16];

	static {
		Vec3d X = new Vec3d(1, 0, 0),
			Y = new Vec3d(0, 1, 0),
			Z = new Vec3d(0, 0, 1);
		Vec3d x1, y1, z1;
		FloatBuffer buff;
		for (Orientation o : Orientation.values()) {
			x1 = o.rotate(X);
			y1 = o.rotate(Y);
			z1 = o.rotate(Z);
			buff = BufferUtils.createFloatBuffer(16);
			buff.put(new float[] {
				(float)x1.xCoord, (float)x1.yCoord, (float)x1.zCoord, 0,
				(float)y1.xCoord, (float)y1.yCoord, (float)y1.zCoord, 0,
				(float)z1.xCoord, (float)z1.yCoord, (float)z1.zCoord, 0,
				0, 0, 0, 1
			});
			buff.flip();
			matrices[o.ordinal()] = buff;
		}
	}

	@Deprecated
	public static void moveAndOrientToBlock(double x, double y, double z, int dir) {
		GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
		FloatBuffer mat = matrices[dir];
		mat.rewind();
		GL11.glMultMatrix(mat);
	}

	public static void moveAndOrientToBlock(double x, double y, double z, Orientation o) {
		GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
		FloatBuffer mat = matrices[o.ordinal()];
		mat.rewind();
		GL11.glMultMatrix(mat);
	}

	public static void rotateTo(Orientation o) {
		FloatBuffer mat = matrices[o.ordinal()];
		mat.rewind();
		GL11.glMultMatrix(mat);
	}

	public static void luminate(TileEntity te, EnumFacing side, int b) {
		BlockPos pos = side == null ? te.getPos() : te.getPos().offset(side);
		World world = te.getWorld();
		IBlockState state = world.getBlockState(pos);
		int l = world.getCombinedLight(pos, Math.max(state.getLightValue(world, pos), b));
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l & 0xffff, l >> 16);
	}

	public static void rotate(int[] data, int px, ModelRotation rot) {
		int py = px + 1, pz = px + 2, i;
		switch (rot) {
		//horizontal
		case X0_Y0: return;
		case X0_Y90:
			i = data[pz];
			data[pz] = data[px];
			data[px] = neg(i);
			return;
		case X0_Y180:
			data[px] = neg(data[px]);
			data[pz] = neg(data[pz]);
			return;
		case X0_Y270:
			i = data[px];
			data[px] = data[pz];
			data[pz] = neg(i);
			return;
		//face down
		case X90_Y0:
			i = data[py];
			data[py] = data[pz];
			data[pz] = neg(i);
			return;
		case X90_Y90:
			i = data[py];
			data[py] = data[pz];
			data[pz] = data[px];
			data[px] = i;
			return;
		case X90_Y180:
			i = data[py];
			data[py] = data[pz];
			data[pz] = i;
			data[px] = neg(data[px]);
			return;
		case X90_Y270:
			i = data[px];
			data[px] = neg(data[py]);
			data[py] = data[pz];
			data[pz] = neg(i);
			return;
		//upside down
		case X180_Y0:
			data[py] = neg(data[py]);
			data[pz] = neg(data[pz]);
			return;
		case X180_Y90:
			data[py] = neg(data[py]);
			i = data[pz];
			data[pz] = data[px];
			data[px] = i;
			return;
		case X180_Y180:
			data[px] = neg(data[px]);
			data[py] = neg(data[py]);
			return;
		case X180_Y270:
			data[py] = neg(data[py]);
			i = data[px];
			data[px] = neg(data[pz]);
			data[pz] = neg(i);
			return;
		//face up
		case X270_Y0:
			i = data[pz];
			data[pz] = data[py];
			data[py] = neg(i);
			return;
		case X270_Y90:
			i = data[py];
			data[py] = neg(data[pz]);
			data[pz] = data[px];
			data[px] = neg(i);
			return;
		case X270_Y180:
			i = data[pz];
			data[pz] = neg(data[py]);
			data[py] = neg(i);
			data[px] = neg(data[px]);
			return;
		case X270_Y270:
			i = data[pz];
			data[pz] = neg(data[px]);
			data[px] = data[py];
			data[py] = neg(i);
			return;
		}
	}

	private static int neg(int i) {
		return Float.floatToRawIntBits(1F - Float.intBitsToFloat(i));
	}
	
	public static final Util instance = new Util();
	public static int RenderFrame = 0;

	private Util() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void renderTick(TickEvent.RenderTickEvent event) {
		RenderFrame++;
	}

	public static int[] texturedRect(float x, float y, float z, float w, float h, float tx, float ty, float tw, float th) {
		return new int[] {
			Float.floatToIntBits(x), Float.floatToIntBits(y), Float.floatToIntBits(z), Float.floatToIntBits(tx), Float.floatToIntBits(ty),
			Float.floatToIntBits(x + w), Float.floatToIntBits(y), Float.floatToIntBits(z), Float.floatToIntBits(tx + tw), Float.floatToIntBits(ty),
			Float.floatToIntBits(x + w), Float.floatToIntBits(y + h), Float.floatToIntBits(z), Float.floatToIntBits(tx + tw), Float.floatToIntBits(ty + th),
			Float.floatToIntBits(x), Float.floatToIntBits(y + h), Float.floatToIntBits(z), Float.floatToIntBits(tx), Float.floatToIntBits(ty + th)
		};
	}

}
