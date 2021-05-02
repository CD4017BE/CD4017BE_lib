package cd4017be.lib.render.model;

import java.util.Arrays;

import cd4017be.lib.util.Orientation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction.Axis;

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

	public IntArrayModel(int[] data) {this(data, 0xffffffff, 0);}
	public IntArrayModel(int n) {this(new int[n * 28]);}

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
			data[i] = Float.floatToIntBits(tex.getU(Float.intBitsToFloat(data[i])));//U
			data[++i] = Float.floatToIntBits(tex.getV(Float.intBitsToFloat(data[i])));//V
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
		throw new UnsupportedOperationException();
		/* TODO implement
		ModelRotation r = o.getModelRotation();
		int[] data = Arrays.copyOf(vertexData, vertexData.length);
		for (int i = 0; i < data.length; i += 7)
			Util.rotate(data, i, r);
		return new IntArrayModel(data, color, brightness);*/
	}

}
