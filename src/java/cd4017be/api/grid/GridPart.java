package cd4017be.api.grid;

import static net.minecraftforge.registries.ForgeRegistries.ITEMS;

import cd4017be.lib.network.INBTSynchronized;
import cd4017be.lib.render.model.JitBakedModel;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.math.Orient;
import it.unimi.dsi.fastutil.shorts.ShortArrays;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**Parent class for all microblock devices.
 * @author CD4017BE */
public abstract class GridPart implements IPortHolder, INBTSynchronized {

	public IGridHost host;
	/** filled & 1 << (x + 4*y + 16*z) */
	public long bounds;
	/** x & 0x000f | y & 0x00f0 | z & 0x0f00 | type & 0xf000 */
	public short[] ports;

	public GridPart(int ports) {
		this.ports = ports == 0 ? ShortArrays.EMPTY_ARRAY : new short[ports];
	}

	/**@return the {@link IGridItem} that creates this part. */
	public abstract Item item();

	/**@return the ItemStack dropped on removal. */
	public ItemStack asItemStack() {
		return new ItemStack(item());
	}

	/**Called server side when adding, removing, loading or unloading a part.
	 * @param host the new host when loaded/added or null when unloaded/removed
	 * @return usually this, but may be different part replacing it */
	public GridPart setHost(IGridHost host) {
		this.host = host;
		return this;
	}

	/**@param b new bounds (must not overlap with other parts) */
	public void setBounds(long b) {
		long old = bounds;
		bounds = b;
		host.updateBounds();
		for (int i = 0; i < 6; i++) {
			long f = FACES[i];
			if ((old & f) != 0 ^ (b & f) == 0) continue;
			Direction d = Direction.from3DDataValue(i^1);
			if (analogOutput(d) > 0)
				host.updateNeighbor(d.getOpposite());
		}
		host.onPartChange();
	}

	/**@return the layer occupied by the part:
	 * {@link #L_OUTER} parts can overlap with {@link #L_INNER} parts.
	 * {@link #L_OUTER} and {@link #L_FULL} are considered opaque. */
	public byte getLayer() {
		return L_FULL;
	}

	@Override
	public void storeState(CompoundTag nbt, int mode) {
		INBTSynchronized.super.storeState(nbt, mode);
		nbt.putString("id", item().getRegistryName().toString());
	}

	/**@param player
	 * @param hand used hand or null if left click
	 * @param hit original ray trace hit
	 * @param pos hit voxel pos
	 * @return action result */
	public InteractionResult onInteract(Player player, InteractionHand hand, BlockHitResult hit, int pos) {
		if (hand != null) return InteractionResult.PASS;
		if (!player.level.isClientSide && player.getMainHandItem().getItem() instanceof IGridItem) {
			IGridHost host = this.host;
			host.removePart(this);
			host.removeIfEmpty();
			if (!player.isCreative())
				ItemFluidUtil.dropStack(asItemStack(), player);
		}
		return InteractionResult.CONSUME;
	}

	/**@param model used to render the grid block
	 * @param opaque voxels for visibility testing */
	@OnlyIn(Dist.CLIENT)
	public abstract void fillModel(JitBakedModel model, long opaque);

	/**@param side the neighbor block is powered from
	 * @return redstone level emitted on touching outer faces */
	public int analogOutput(Direction side) {
		return 0;
	}

	/**when an adjacent block changes
	 * @param world
	 * @param pos changed block's postion
	 * @param dir side of this grid */
	public void onBlockChange(Level world, BlockPos pos, Direction dir) {}

	/**when an adjacent TileEntity changes
	 * @param world
	 * @param pos changed TE's postion
	 * @param dir side of this grid */
	public void onTEChange(Level world, BlockPos pos, Direction dir) {}

	/**When replicated or disassembled in a Microblock Workbench.
	 * Parts should clear their contents here to prevent resource duplication.
	 * @param world
	 * @param pos for dropping items and such
	 * @return whether data has changed */
	public boolean dissassemble(Level world, BlockPos pos) {return false;}

	/**When merging two grid blocks together
	 * @param other grid to merge in
	 * @return whether to add this part */
	public boolean merge(IGridHost other) {
		return true;
	}

	/**@return whether this part can rotate in the horizontal plane */
	public boolean canRotate() { return false; }

	/**@param steps rotation steps along north -> east -> south -> west */
	public void rotate(int steps) {
		long b = bounds, b1 = 0;
		int i, idx, idz;
		switch(steps & 3) {
		case 1: i = 3; idx = 16; idz = -65; break;
		case 2: i = 51; idx = -1; idz = -12; break;
		case 3: i = 48; idx = -16; idz = 65; break;
		default: return;
		}
		for (int z = 0; z < 4; z++, b >>>= 12, i += idz)
			for (int x = 0; x < 4; x++, b >>>= 1, i += idx)
				b1 |= (b & 0x1111) << i;
		bounds = b1;
	}

	/**@param d direction
	 * @param n grid steps 1...3
	 * @return whether this part can be moved */
	public boolean canMove(Direction d, int n) {return false;}

	/**@param d direction
	 * @param n grid steps 1...3
	 * @return part moved to adjacent block: null = all in old block,
	 * this = all in next block, new = split over two blocks */
	public GridPart move(Direction d, int n) {
		int id = d.ordinal(), s = step(id);
		long b0 = bounds, b1 = b0 & mask(id, n); b0 &= ~b1;
		if ((id & 1) == 0) {
			b0 >>>= s * n;
			b1 <<= s * (4 - n);
		} else {
			b0 <<= s * n;
			b1 >>>= s * (4 - n);
		}
		if (b0 == 0) {
			bounds = b1;
			return this;
		}
		bounds = b0;
		if (b1 == 0) return null;
		return copy(b1);
	}

	protected GridPart copy(long bounds) {
		return null;
	}

	/**@param pos x & 0x03 | y & 0x0c | z & 0x30
	 * @param dir side of the cell
	 * @param type type & 7 | master & 8
	 * @return x & 0x000f | y & 0x00f0 | z & 0x0f00 | type & 0xf000 */
	public static short port(int pos, Direction dir, int type) {
		return (short)((pos << 1 & 6 | pos << 3 & 0x60 | pos << 5 & 0x600 | type << 12)
		+ 0x111 + (dir.getStepX() | dir.getStepY() << 4 | dir.getStepZ() << 8));
	}

	/**@param p0 first corner index
	 * @param p1 second corner index
	 * @return bounds with the given cuboid of voxels filled */
	public static long bounds(int p0, int p1) {
		long b = 1L << p0;
		if (p1 == p0) return b;
		int i = (p1 & 3) - (p0 & 3);
		for (;i > 0; i--) b |= b << 1;
		for (;i < 0; i++) b |= b >>> 1;
		i = (p1 & 12) - (p0 & 12) >> 2;
		for (;i > 0; i--) b |= b << 4;
		for (;i < 0; i++) b |= b >>> 4;
		i = (p1 & 48) - (p0 & 48) >> 4;
		for (;i > 0; i--) b |= b << 16;
		for (;i < 0; i++) b |= b >>> 16;
		return b;
	}

	/**@param b bounds to fill
	 * @param f initial fill
	 * @return connected region containing f */
	public static long floodFill(long b, long f) {
		f &= b;
		while(f != (f |= outline(f) & b));
		return f;
	}

	public static long outline(long b) {
		return
		  b << 1 & 0xeeee_eeee_eeee_eeeeL
		| b >> 1 & 0x7777_7777_7777_7777L
		| b << 4 & 0xfff0_fff0_fff0_fff0L
		| b >> 4 & 0x0fff_0fff_0fff_0fffL
		| b <<16 & 0xffff_ffff_ffff_0000L
		| b >>16 & 0x0000_ffff_ffff_ffffL;
	}

	/**@param dir BTNSWE index
	 * @return x->1, y->4, z->16 */
	public static int step(int dir) {
		return 0xb424 >>> dir * 3 & 0x15;
	}

	/**@param dir BTNSWE index
	 * @param n steps
	 * @return voxel mask */
	public static long mask(int dir, int n) {
		long m = FACES[dir];
		if (n == 1) return m;
		int s = step(dir);
		if ((dir & 1) != 0) m >>>= s * (n - 1);
		while(--n > 0) m |= m << s;
		return m;
	}

	public static float[] vec(int pos) {
		return new float[] {pos & 3, pos >> 2 & 3, pos >> 4 & 3};
	}

	public static void rotate(short[] ports, int steps) {
		int o = Orient.rot(1, steps), sign = (o & 0x111) * 15;
		for (int i = 0; i < ports.length; i++) {
			int p = ports[i];
			p = (p ^ sign) - (sign & 0x777);
			ports[i] = (short)(p & 0xf000
				| (p      & 15) << (o << 1 & 12)
				| (p >> 4 & 15) << (o >> 3 & 12)
				| (p >> 8 & 15) << (o >> 7 & 12)
			);
		}
	}

	public static void move(short[] ports, Direction d, int n, boolean newBlock) {
		if (ports.length == 0) return;
		int ofs = (
			  d.getStepX() * n + 4 << 1
			| d.getStepY() * n + 4 << 5
			| d.getStepZ() * n + 4 << 9
		) & 0x777;
		int m = (d.ordinal() & 1 ^ (newBlock ? 0 : 1))
			<< d.getAxis().ordinal() * 4 + 3;
		for (int i = 0; i < ports.length; i++)
			ports[i] = (short)(ports[i] + ofs ^ m);
	}

	/**@param part optional old part to reuse
	 * @param nbt data
	 * @param mode sync mode
	 * @return loaded part */
	public static GridPart load(GridPart part, CompoundTag nbt, int mode) {
		Item item = ITEMS.getValue(new ResourceLocation(nbt.getString("id")));
		if (part == null || part.item() != item)
			part = item instanceof IGridItem ? ((IGridItem)item).createPart() : null;
		if (part != null) part.loadState(nbt, mode);
		return part;
	}

	public static BlockState GRID_HOST_BLOCK;

	/** layers */
	public static final byte L_OUTER = 1, L_INNER = -1, L_FULL = 0;

	public static final long[] FACES = {
		0x000f_000f_000f_000fL,
		0xf000_f000_f000_f000L,
		0x0000_0000_0000_ffffL,
		0xffff_0000_0000_0000L,
		0x1111_1111_1111_1111L,
		0x8888_8888_8888_8888L
	};

}