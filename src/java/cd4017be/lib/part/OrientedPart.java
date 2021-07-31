package cd4017be.lib.part;

import static cd4017be.lib.network.Sync.ALL;
import static cd4017be.lib.network.Sync.Type.Enum;

import com.mojang.blaze3d.vertex.PoseStack;

import cd4017be.api.grid.GridPart;
import cd4017be.lib.network.Sync;
import cd4017be.lib.render.GridModels;
import cd4017be.lib.render.model.JitBakedModel;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author CD4017BE */
public abstract class OrientedPart extends GridPart {

	@Sync(to=ALL, type=Enum)
	public Orientation orient;
	@Sync(to=ALL)
	public byte pos;

	public OrientedPart(int ports) {
		super(ports);
	}

	public void set(int pos, Orientation orient) {
		this.pos = (byte)pos;
		this.orient = orient;
	}

	protected void setPort(int i, int pos, Direction dir, int type) {
		ports[i] = port(orient, pos, dir, type);
	}

	protected void setBounds(int p0, int p1) {
		bounds = bounds(pos(p0, orient), pos(p1, orient));
	}

	public static short port(Orientation orient, int pos, Direction dir, int type) {
		return port(pos(pos, orient), orient.apply(dir), type);
	}

	public static int pos(int pos, Orientation orient) {
		int o = orient.o;
		pos ^= (o & 1 | o >> 2 & 4 | o >> 4 & 16) * 3;
		return (pos & 3) << (o & 6)
		| (pos >> 2 & 3) << (o >> 4 & 6)
		| (pos >> 4 & 3) << (o >> 8 & 6);
	}

	/**@return whether the part touches the front adjacent block */
	protected boolean onEdge() {
		return (bounds & FACES[orient.b.ordinal()^1]) != 0;
	}

	@Override
	public void loadState(CompoundTag nbt, int mode) {
		super.loadState(nbt, mode);
		set(pos, orient);
	}

	@OnlyIn(Dist.CLIENT)
	protected ResourceLocation model() {
		return new ModelResourceLocation(item().getRegistryName(), "inventory");
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void fillModel(JitBakedModel model, long opaque) {
		GridModels.putCube(model(), model, bounds, opaque, pos, orient.o);
	}

	@OnlyIn(Dist.CLIENT)
	protected void transform(PoseStack ms) {
		ms.last().pose().multiply(orient.mat4);
		ms.last().normal().mul(orient.mat3);
		ms.translate((pos & 3) * .25F, (pos >> 2 & 3) * .25F, (pos >> 4 & 3) * .25F);
	}

	@Override
	public boolean canRotate() {
		return true;
	}

	@Override
	public void rotate(int steps) {
		Orientation o;
		switch(steps & 3) {
		case 1: o = Orientation.W12; break;
		case 2: o = Orientation.N12; break;
		case 3: o = Orientation.E12; break;
		default: return;
		}
		set(pos, o.apply(orient));
	}

	@Override
	public boolean canMove(Direction d, int n) {
		long m = mask(d.ordinal(), n);
		return (bounds & m) == 0 || (bounds & ~m) == 0;
	}

	@Override
	public GridPart move(Direction d, int n) {
		d = orient.inv().apply(d);
		int x = (pos      & 3) + d.getStepX() * n;
		int y = (pos >> 2 & 3) + d.getStepY() * n;
		int z = (pos >> 4 & 3) + d.getStepZ() * n;
		set(x & 3 | (y & 3) << 2 | (z & 3) << 4 | pos & 0xc0, orient);
		return ((x | y | z) & -4) == 0 ? null : this;
	}

}
