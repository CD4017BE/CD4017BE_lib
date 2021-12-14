package cd4017be.lib.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static java.lang.Float.floatToIntBits;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cd4017be.lib.util.Orientation;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 
 * @author CD4017BE
 */
@OnlyIn(Dist.CLIENT)
public class Util {

	public static final FloatBuffer[] matrices = new FloatBuffer[24];

	static {
		Vector3i x1, y1, z1;
		FloatBuffer buff;
		for (Orientation o : Orientation.values()) {
			x1 = o.r.getNormal();
			y1 = o.u.getNormal();
			z1 = o.b.getNormal();
			buff = BufferUtils.createFloatBuffer(16);
			buff.put(new float[] {
				x1.getX(), x1.getY(), x1.getZ(), 0,
				y1.getX(), y1.getY(), y1.getZ(), 0,
				z1.getX(), z1.getY(), z1.getZ(), 0,
				0, 0, 0, 1
			});
			buff.flip();
			matrices[o.ordinal()] = buff;
		}
	}

	/**
	 * @param c color in 1: 0xAARRGGBB or 2: 0xAABBGGRR format
	 * @return the given color converted from format 1 to 2 or vice versa.
	 */
	public static int RGBtoBGR(int c) {
		return c & 0xff00ff00 | c << 16 & 0xff0000 | c >> 16 & 0xff;
	}

	public static void moveAndOrientToBlock(double x, double y, double z, Orientation o) {
		GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
		FloatBuffer mat = matrices[o.ordinal()];
		mat.rewind();
		GL11.glMultMatrixf(mat);
	}

	public static void rotateTo(Orientation o) {
		FloatBuffer mat = matrices[o.ordinal()];
		mat.rewind();
		GL11.glMultMatrixf(mat);
	}

	@Deprecated
	public static void luminate(TileEntity te, Direction side, int b) {
		throw new UnsupportedOperationException();
		/* TODO implement
		BlockPos pos = side == null ? te.getPos() : te.getPos().offset(side);
		World world = te.getWorld();
		BlockState state = world.getBlockState(pos);
		int l = world.getCombinedLight(pos, Math.max(state.getLightValue(world, pos), b));
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l & 0xffff, l >> 16);*/
	}

	public static int rotateNormal(int n, ModelRotation rot) {
		switch (rot) {
		//horizontal
		default:       return n;
		case X0_Y90:   return n & 0x00ff00 | n << 16 & 0xff0000 | ~n >> 16 & 0xff;
		case X0_Y180:  return n ^ 0xff00ff;
		case X0_Y270:  return n & 0x00ff00 | ~n << 16 & 0xff0000 | n >> 16 & 0xff;
		//face down
		case X90_Y0:   return n & 0x0000ff | ~n << 8 & 0xff0000 | n >> 8 & 0xff00;
		case X90_Y90:  return n << 16 & 0xff0000 | n >> 8 & 0xffff;
		case X90_Y180: return ~n & 0xff | n << 8 & 0xff0000 | n >> 8 & 0xff00;
		case X90_Y270: return ~n << 16 & 0xff0000 | (n^0xff00) >> 8 & 0xffff;
		//upside down
		case X180_Y0:  return n ^ 0xffff00;
		case X180_Y90: return ~n & 0xff00 | n << 16 & 0xff0000 | n >> 16 & 0xff;
		case X180_Y180:return n ^ 0xffff;
		case X180_Y270:return ~n & 0xff00 | ~n << 16 & 0xff0000 | ~n >> 16 & 0xff;
		//face up
		case X270_Y0:  return n & 0xff | n << 8 & 0xff0000 | ~n >> 8 & 0xff00;
		case X270_Y90: return n << 16 & 0xff0000 | ~n >> 8 & 0xffff;
		case X270_Y180:return ~n & 0xff | ~n << 8 & 0xff0000 | ~n >> 8 & 0xff00;
		case X270_Y270:return ~n << 16 & 0xff0000 | (n^0xff0000) >> 8 & 0xffff;
		}
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
			floatToIntBits(x), floatToIntBits(y), floatToIntBits(z), floatToIntBits(tx), floatToIntBits(ty),
			floatToIntBits(x + w), floatToIntBits(y), floatToIntBits(z), floatToIntBits(tx + tw), floatToIntBits(ty),
			floatToIntBits(x + w), floatToIntBits(y + h), floatToIntBits(z), floatToIntBits(tx + tw), floatToIntBits(ty + th),
			floatToIntBits(x), floatToIntBits(y + h), floatToIntBits(z), floatToIntBits(tx), floatToIntBits(ty + th)
		};
	}

	public static int[] texturedRect(Vector3d p, Vector3d w, Vector3d h, Vector2f t0, Vector2f t1, int color, int light) {
		return new int[] {
			floatToIntBits((float)(p.x            )), floatToIntBits((float)(p.y            )), floatToIntBits((float)(p.z            )), color, floatToIntBits(t0.x), floatToIntBits(t0.y), light,
			floatToIntBits((float)(p.x + w.x      )), floatToIntBits((float)(p.y + w.y      )), floatToIntBits((float)(p.z + w.z      )), color, floatToIntBits(t1.x), floatToIntBits(t0.y), light,
			floatToIntBits((float)(p.x + w.x + h.x)), floatToIntBits((float)(p.y + w.y + h.y)), floatToIntBits((float)(p.z + w.z + h.z)), color, floatToIntBits(t1.x), floatToIntBits(t1.y), light,
			floatToIntBits((float)(p.x       + h.x)), floatToIntBits((float)(p.y       + h.y)), floatToIntBits((float)(p.z       + h.z)), color, floatToIntBits(t0.x), floatToIntBits(t1.y), light,
		};
	}

	/**
	 * @param tex texture
	 * @param u
	 * @param v
	 * @return interpolated uv
	 */
	public static Vector2f getUV(TextureAtlasSprite tex, float u, float v) {
		return new Vector2f(tex.getU(u), tex.getV(v));
	}

	/**
	 * @param b vertex buffer
	 * @param from starting vertex index
	 * @param to end vertex index
	 * @return the vertex data
	 */
	@Deprecated
	public static int[] extractData(BufferBuilder b, int from, int to) {
		int l = b.getVertexFormat().getIntegerSize();
		from *= l; to *= l;
		int[] arr = new int [to - from];
		ByteBuffer bb = b.popNextBuffer().getSecond();
		int p = bb.position();
		bb.position(from * 4);
		IntBuffer ib = bb.asIntBuffer();
		ib.get(arr);
		bb.position(p);
		return arr;
	}

	/**
	 * render a tool-tip frame that is visible through blocks
	 * @param fr the FontRenderer to use
	 * @param x right offset (in text pixels)
	 * @param y down offset (in text pixels)
	 * @param tc text color
	 * @param bc background color
	 * @param lines text lines to draw
	 */
	@Deprecated
	public static void renderToolTip(FontRenderer fr, int x, int y, int tc, int bc, String... lines) {
		throw new UnsupportedOperationException();
		/* TODO implement
		GlStateManager.disableLighting();
		int width = 0, height = lines.length * fr.FONT_HEIGHT;
		int[] w = new int[lines.length];
		for (int i = 0; i < w.length; i++) {
			int l = fr.getStringWidth(lines[i]);
			w[i] = l;
			if (l > width) width = l;
		}
		int x0 = floatToIntBits(x - width / 2 - 5), x1 = floatToIntBits(x - width / 2), x2 = floatToIntBits(x + width / 2), x3 = floatToIntBits(x + width / 2 + 5);
		int y0 = floatToIntBits(y - height - 5), y1 = floatToIntBits(y - height), y2 = floatToIntBits(y), y3 = floatToIntBits(y + 5);
		int z0 = 0;
		int c1 = (bc & 0xff00ff00) | (bc >> 16 & 0xff) | (bc & 0xff) << 16, c0 = c1;// & 0xffffff;
		GlStateManager.enableBlend();
		GlStateManager.disableAlphaTest();
		GlStateManager.disableTexture();
		GlStateManager.depthFunc(GL11.GL_ALWAYS);
		BufferBuilder buff = Tessellator.getInstance().getBuffer();
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		buff.addVertexData(new int[]{ //background frame
				  x1, y2, z0, c1,  x2, y2, z0, c1,  x2, y1, z0, c1,  x1, y1, z0, c1, //center
				  x1, y1, z0, c1,  x2, y1, z0, c1,  x3, y0, z0, c0,  x0, y0, z0, c0, //up fade out
				  x2, y1, z0, c1,  x2, y2, z0, c1,  x3, y3, z0, c0,  x3, y0, z0, c0, //right fade out
				  x2, y2, z0, c1,  x1, y2, z0, c1,  x0, y3, z0, c0,  x3, y3, z0, c0, //down fade out
				  x1, y2, z0, c1,  x1, y1, z0, c1,  x0, y0, z0, c0,  x0, y3, z0, c0, //left fade out
			});
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture();
		for (int i = 0; i < w.length; i++)
			fr.drawString(lines[i], x - w[i] / 2, y - height + i * fr.FONT_HEIGHT, tc);
		GlStateManager.depthFunc(GL11.GL_LEQUAL);*/
	}

}
