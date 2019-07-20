package cd4017be.lib.render.model;

import java.util.Arrays;

import cd4017be.lib.render.Util;
import cd4017be.lib.render.model.ModelContext.Quad;
import cd4017be.lib.script.Module;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.MathHelper;

/**
 * 
 * @author CD4017BE
 */
public class IntArrayModel {

	public static final VertexFormat FORMAT = DefaultVertexFormats.BLOCK;
	private static final float EPSILON = 0.001F;
	public final int[] vertexData;
	private float ofsX = 0, ofsY = 0, ofsZ = 0;
	private int brightness, color;

	public IntArrayModel(int[] data, int color, int brightness) {
		this.vertexData = data;
		this.color = color;
		this.brightness = brightness;
	}

	public IntArrayModel(int[] data) {this(data, 0xffffffff, 0x00f000f0);}
	public IntArrayModel(int n) {this(new int[n * 28]);}

	public IntArrayModel(ModelContext context) {
		this(context.quads[0].size());
		int j = 0;
		for (Quad quad : context.quads[0])
			for (double[] vert : quad.vertices) {
				vertexData[j++] = Float.floatToRawIntBits((float)vert[0] / 16F);	//X
				vertexData[j++] = Float.floatToRawIntBits((float)vert[1] / 16F);	//Y
				vertexData[j++] = Float.floatToRawIntBits((float)vert[2] / 16F);	//Z
				vertexData[j++] = (int)MathHelper.clamp(vert[5] * 255D, 0, 255)	//R
						| (int)MathHelper.clamp(vert[6] * 255D, 0, 255) << 8	//G
						| (int)MathHelper.clamp(vert[7] * 255D, 0, 255) << 16	//B
						| (int)MathHelper.clamp(vert[8] * 255D, 0, 255) << 24;	//A
				vertexData[j++] = Float.floatToRawIntBits((float)vert[3]);	//U
				vertexData[j++] = Float.floatToRawIntBits((float)vert[4]);	//V
				vertexData[j++] = brightness;
			}
	}

	public static TextureAtlasSprite[] getTextures(Module script) {
		IOperand var = script.read("textures");
		if (!(var instanceof Array)) return null;
		Object[] names = (Object[]) var.value();
		TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
		TextureAtlasSprite[] textures = new TextureAtlasSprite[names.length];
		for (int i = 0; i < textures.length; i++)
			textures[i] = map.getAtlasSprite((String)names[i]);
		return textures;
	}

	public IntArrayModel(ModelContext context, TextureAtlasSprite[] textures) {
		this(context.quads[0].size());
		int j = 0;
		for (Quad quad : context.quads[0]) {
			TextureAtlasSprite tex = textures == null ? null : textures[quad.tex];
			for (double[] vert : quad.vertices) {
				vertexData[j++] = Float.floatToRawIntBits((float)vert[0] / 16F);	//X
				vertexData[j++] = Float.floatToRawIntBits((float)vert[1] / 16F);	//Y
				vertexData[j++] = Float.floatToRawIntBits((float)vert[2] / 16F);	//Z
				vertexData[j++] = (int)MathHelper.clamp(vert[7] * 255D, 0, 255)	//B
						| (int)MathHelper.clamp(vert[6] * 255D, 0, 255) << 8	//G
						| (int)MathHelper.clamp(vert[5] * 255D, 0, 255) << 16	//R
						| (int)MathHelper.clamp(vert[8] * 255D, 0, 255) << 24;	//A
				vertexData[j++] = Float.floatToRawIntBits(tex == null ? (float)vert[3] : tex.getInterpolatedU(vert[3]));	//U
				vertexData[j++] = Float.floatToRawIntBits(tex == null ? (float)vert[4] : tex.getInterpolatedV(vert[4]));	//V
				vertexData[j++] = brightness;
			}
		}
	}

	public IntArrayModel origin(float x, float y, float z) {
		ofsX = x;
		ofsY = y;
		ofsZ = z;
		return this;
	}

	public void setBrightness(int l) {
		if (l == brightness) return;
		for (int i = 6; i < vertexData.length; i += 7)
			vertexData[i] = l;
		brightness = l;
	}

	public void setOffset(float dx, float dy, float dz) {
		dx -= ofsX;
		dy -= ofsY;
		dz -= ofsZ;
		if (dx*dx + dy*dy + dz*dz < EPSILON * EPSILON) return;
		for (int i = 0; i < vertexData.length; i += 5) {
			vertexData[i] = Float.floatToIntBits(dx + Float.intBitsToFloat(vertexData[i]));	//X
			vertexData[++i] = Float.floatToIntBits(dy + Float.intBitsToFloat(vertexData[i]));//Y
			vertexData[++i] = Float.floatToIntBits(dz + Float.intBitsToFloat(vertexData[i]));//Z
		}
		ofsX += dx;
		ofsY += dy;
		ofsZ += dz;
	}

	public void setOffset(float ofs, Axis axis) {
		int i;
		switch(axis) {
		case X:
			if (ofs == ofsX) return;
			ofs -= ofsX;
			ofsX += ofs;
			i = 0;
			break;
		case Y:
			if (ofs == ofsY) return;
			ofs -= ofsY;
			ofsY += ofs;
			i = 1;
			break;
		case Z:
			if (ofs == ofsZ) return;
			ofs -= ofsZ;
			ofsZ += ofs;
			i = 2;
			break;
		default: return;
		}
		for (; i < vertexData.length; i+=7)
			vertexData[i] = Float.floatToIntBits(ofs + Float.intBitsToFloat(vertexData[i]));
	}

	public void setColor(int c) {
		if (c == color) return;
		for (int i = 3; i < vertexData.length; i += 7)
			vertexData[i] = c;
		color = c;
	}

	public IntArrayModel withTexture(TextureAtlasSprite tex) {
		int[] data = Arrays.copyOf(vertexData, vertexData.length);
		for (int i = 4; i < data.length; i += 6) {
			data[i] = Float.floatToIntBits(tex.getInterpolatedU(Float.intBitsToFloat(data[i])));//U
			data[++i] = Float.floatToIntBits(tex.getInterpolatedV(Float.intBitsToFloat(data[i])));//V
		}
		return new IntArrayModel(data, color, brightness);
	}

	public IntArrayModel translated(float x, float y, float z) {
		x -= ofsX; y -= ofsY; z -= ofsZ;
		int[] data = Arrays.copyOf(vertexData, vertexData.length);
		for (int i = 0; i < data.length; i += 5) {
			data[i] = Float.floatToIntBits(x + Float.intBitsToFloat(data[i]));	//X
			data[++i] = Float.floatToIntBits(y + Float.intBitsToFloat(data[i]));//Y
			data[++i] = Float.floatToIntBits(z + Float.intBitsToFloat(data[i]));//Z
		}
		return new IntArrayModel(data, color, brightness);
	}

	public IntArrayModel rotated(Orientation o) {
		ModelRotation r = o.getModelRotation();
		int[] data = Arrays.copyOf(vertexData, vertexData.length);
		for (int i = 0; i < data.length; i += 7)
			Util.rotate(data, i, r);
		return new IntArrayModel(data, color, brightness);
	}

}
