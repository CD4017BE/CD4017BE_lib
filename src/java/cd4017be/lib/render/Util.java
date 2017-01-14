package cd4017be.lib.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Util {

	public static final FloatBuffer[] matrices = new FloatBuffer[6];

	static {
		float[][] main = {
			{//BOTTOM
				 1, 0, 0, 0,
				 0, 0, 1, 0,
				 0,-1, 0, 0,
				 0, 0, 0, 1
			}, {//TOP
				 1, 0, 0, 0,
				 0, 0,-1, 0,
				 0, 1, 0, 0,
				 0, 0, 0, 1
			}, {//NORTH
				 1, 0, 0, 0,
				 0, 1, 0, 0,
				 0, 0, 1, 0,
				 0, 0, 0, 1
			}, {//SOUTH
				-1, 0, 0, 0,
				 0, 1, 0, 0,
				 0, 0,-1, 0,
				 0, 0, 0, 1
			}, {//WEST
				 0, 0,-1, 0,
				 0, 1, 0, 0,
				 1, 0, 0, 0,
				 0, 0, 0, 1
			}, {//EAST
				 0, 0, 1, 0,
				 0, 1, 0, 0,
				-1, 0, 0, 0,
				 0, 0, 0, 1
			}
		};
		for (int i = 0; i < matrices.length; i++) {
			matrices[i] = BufferUtils.createFloatBuffer(16);
			matrices[i].put(main[i]);
			matrices[i].flip();
		}
	}

	public static void moveAndOrientToBlock(double x, double y, double z, int dir) {
		GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
		FloatBuffer mat = matrices[dir];
		mat.rewind();
		GL11.glMultMatrix(mat);
	}

	public static final Util instance = new Util();
	public static int RenderFrame = 0;

	@SubscribeEvent
	public void renderTick(TickEvent.RenderTickEvent event) {
		RenderFrame++;
	}

}
