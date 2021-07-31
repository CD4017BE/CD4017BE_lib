package cd4017be.lib.tileentity;

import net.minecraft.world.level.block.state.BlockState;
import static cd4017be.lib.Content.GRID;
import static cd4017be.lib.Content.GRID1;
import static cd4017be.lib.network.Sync.*;
import static cd4017be.lib.tick.GateUpdater.GATE_UPDATER;
import static cd4017be.lib.tick.GateUpdater.TICK;
import static java.lang.Math.max;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Predicate;

import cd4017be.api.grid.*;
import cd4017be.lib.Content;
import cd4017be.lib.block.BlockTE;
import cd4017be.lib.block.BlockTE.*;
import cd4017be.lib.render.model.JitBakedModel;
import cd4017be.lib.render.model.TileEntityModel;
import cd4017be.lib.tick.IGate;
import cd4017be.lib.util.HashOutputStream;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.VoxelShape4x4x4;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.util.Constants.NBT;

/**@author CD4017BE */
public class Grid extends SyncTileEntity
implements IGridHost, ITEInteract, ITEShape, ITERedstone,
ITEBlockUpdate, ITEPickItem, IGate {

	private final ExtGridPorts extPorts = new ExtGridPorts(this);
	private final ArrayList<GridPart> parts = new ArrayList<>();
	public long opaque, inner;
	public IDynamicPart[] dynamicParts;
	private int tLoad;
	private boolean occlude, updateOccl;

	public Grid(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public VoxelShape getShape(CollisionContext context) {
		return new VoxelShape4x4x4(bounds()).shape();
	}

	@Override
	public InteractionResult onActivated(
		Player player, InteractionHand hand, BlockHitResult hit
	) {
		return onInteract(player, hand, hit);
	}

	@Override
	public void onClicked(Player player) {
		Vec3 from = player.getEyePosition(0);
		Vec3 to = from.add(player.getViewVector(0).scale(5));
		BlockHitResult hit = getShape(null).clip(from, to, worldPosition);
		if (hit != null) onInteract(player, null, hit);
	}

	@Override
	public void updateBounds() {
		long i = 0, o = 0;
		for (GridPart part : parts) {
			byte l = part.getLayer();
			if (l >= 0) o |= part.bounds;
			if (l <= 0) i |= part.bounds;
		}
		inner = i;
		if (opaque != (opaque = o) && !updateOccl) {
			updateOccl = true;
			if (level != null && !level.isClientSide)
				GATE_UPDATER.add(this);
		}
	}

	@Override
	public GridPart findPart(Predicate<GridPart> filter) {
		for (GridPart part : parts)
			if (filter.test(part))
				return part;
		return null;
	}

	@Override
	public void removePart(GridPart part) {
		if (!parts.remove(part) || unloaded) return;
		updateBounds();
		for (int i = 0; i < part.ports.length; i++) {
			short con = part.ports[i];
			if (extPorts.remove(con)) continue;
			if (part.isMaster(i)) part.setHandler(i, null);
			else {
				Port port = findPort(part, con);
				if (port != null) port.setHandler(null);
			}
		}
		updateRedstone(part);
		part.setHost(null);
		clientDirty(true);
	}

	@Override
	public void removeIfEmpty() {
		if (parts.isEmpty()) level.removeBlock(this.worldPosition, false);
	}

	@Override
	public boolean addPart(GridPart part) {
		if (part.host == this) return true;
		byte l = part.getLayer();
		if ((((l >= 0 ? opaque : 0) | (l <= 0 ? inner : 0)) & part.bounds) != 0) return false;
		if (l >= 0) opaque |= part.bounds;
		if (l <= 0) inner |= part.bounds;
		parts.add(part = part.setHost(this));
		connectPart(part);
		updateRedstone(part);
		clientDirty(true);
		return true;
	}

	private void connectPart(GridPart part) {
		if (extPorts.createWire(part, !unloaded)) return;
		for (int i = 0; i < part.ports.length; i++) {
			short con = part.ports[i];
			boolean master = part.isMaster(i);
			if (extPorts.createPort(con, master, !unloaded)) continue;
			Port port = findPort(part, con);
			if (port == null) continue;
			if (master) part.setHandler(i, port.getHandler());
			else port.setHandler(part.getHandler(i));
		}
	}

	private void updateRedstone(GridPart part) {
		for (int i = 0; i < 6; i++)
			if ((part.bounds & GridPart.FACES[i]) != 0) {
				Direction d = Direction.from3DDataValue(i);
				if (part.analogOutput(d.getOpposite()) > 0)
					updateNeighbor(d);
			}
	}

	@Override
	public void onPartChange() {
		clientDirty(true);
	}

	@Override
	public void storeState(CompoundTag nbt, int mode) {
		super.storeState(nbt, mode);
		ListTag list = new ListTag();
		for (GridPart part : parts) {
			CompoundTag tag = new CompoundTag();
			part.storeState(tag, mode);
			list.add(tag);
		}
		nbt.put("parts", list);
		if ((mode & SAVE) != 0) {
			try (HashOutputStream hos = new HashOutputStream()) {
				list.write(new DataOutputStream(hos));
				nbt.putInt("hash", hos.hashCode());
			} catch(IOException e) {/* never happens */}
			nbt.put("extIO", extPorts.serializeNBT());
		}
	}

	@Override
	public void loadState(CompoundTag nbt, int mode) {
		super.loadState(nbt, mode);
		ListTag list = nbt.getList("parts", NBT.TAG_COMPOUND);
		boolean empty = (mode & SAVE) != 0 || list.size() != parts.size();
		if (empty) parts.clear();
		boolean mod = empty;
		for (int i = 0; i < list.size(); i++) {
			GridPart part = empty ? null : parts.get(i);
			part = GridPart.load(part, list.getCompound(i), mode);
			if (part == null) continue;
			if (empty) parts.add(part);
			else mod |= parts.set(i, part) != part;
		}
		updateBounds();
		if ((mode & SAVE) != 0) {
			if (nbt.contains("extIO", NBT.TAG_LONG_ARRAY))
				extPorts.deserializeNBT((LongArrayTag)nbt.get("extIO"));
			//state loaded from item after placement:
			if (!unloaded) onLoad();
			else tLoad = TICK;
		}
		if (mod && (mode & (SYNC|CLIENT)) != 0)
			updateTerParts();
	}

	@Override
	public void onLoad() {
		if (!level.isClientSide) {
			parts.replaceAll(part -> part.setHost(this));
			parts.forEach(this::connectPart);
			extPorts.onLoad();
			clientDirty(true);
		}
		super.onLoad();
		requestModelDataUpdate();
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		if (level.isClientSide) return;
		for (GridPart part : parts) part.setHost(null);
		extPorts.onUnload();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		extPorts.onRemove();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public IModelData getModelData() {
		ModelDataMap data = TileEntityModel.MODEL_DATA_BUILDER.build();
		JitBakedModel model = JitBakedModel.make(data);
		long o = opaque;
		boolean open = (WALLS & ~o) != 0;
		if (!open) o |= ~BOUNDARY;
		for (GridPart part : parts)
			if (open || (part.bounds & BOUNDARY) != 0)
				part.fillModel(model, o);
		return data;
	}

	private static final long BOUNDARY = 0xffff_f99f_f99f_ffffL;
	public static final long WALLS = 0x0660_6996_6996_0660L;

	@Override
	public int redstoneSignal(Direction side, boolean strong) {
		if (strong || side == null) return 0;
		int i = 0;
		long b = GridPart.FACES[side.ordinal()^1];
		for (GridPart part : parts)
			if ((part.bounds & b) != 0)
				i = max(i, part.analogOutput(side));
		return i;
	}

	@Override
	public void onNeighborBlockChange(BlockPos from, Block block, boolean moving) {
		Direction dir = Utils.getSide(from, worldPosition);
		if (dir == null) return;
		long b = GridPart.FACES[dir.ordinal()];
		for (GridPart part : parts)
			if ((part.bounds & b) != 0)
				part.onBlockChange(level, from, dir);
	}

	@Override
	public void updateNeighbor(Direction d) {
		if (unloaded || TICK == tLoad) return;
		BlockPos pos1 = worldPosition.relative(d);
		getBlockState(pos1, true).neighborChanged(
			level, pos1, getBlockState().getBlock(), worldPosition, false
		);
	}

	@Override
	public Port findPort(GridPart except, short port) {
		long m = 0;
		boolean noWire = except == null;
		int port1 = port - 0x111;
		if ((port & 0x888) != 0) noWire = false;
		else m |= 1L << (port >> 1 & 3 | port >> 3 & 12 | port >> 5 & 48);
		if ((port1 & 0x888) != 0) noWire = false;
		else m |= 1L << (port1 >> 1 & 3 | port1 >> 3 & 12 | port1 >> 5 & 48);
		for (GridPart part : parts) {
			if ((part.bounds & m) == 0 || part == except) continue;
			if (noWire && part instanceof IWire) continue;
			int l = part.ports.length;
			for (int i = 0; i < l; i++)
				if (part.ports[i] == port)
					return new Port(part, i);
			if (l > 0) break;
		}
		return null;
	}

	@Override
	public Level world() {
		return level;
	}

	@Override
	public BlockPos pos() {
		return worldPosition;
	}

	@Override
	public ExtGridPorts extPorts() {
		return extPorts;
	}

	@Override
	public ItemStack getItem() {
		if (parts.isEmpty()) return ItemStack.EMPTY;
		ItemStack stack = new ItemStack(Content.grid);
		CompoundTag nbt = stack.getOrCreateTagElement(BlockTE.TE_TAG);
		storeState(nbt, SAVE);
		nbt.remove("extIO");
		return stack;
	}

	@Override
	public long bounds() {
		return opaque | inner;
	}

	/**Merge the other grid into this. Both TEs are usually unloaded when called.
	 * @param other
	 * @return can & did merge */
	public boolean merge(Grid other) {
		if ((inner & other.inner | opaque & other.opaque) != 0) return false;
		for (GridPart part : other.parts)
			if (part.merge(this)) parts.add(part);
		updateBounds();
		return true;
	}

	public boolean rotate(int steps) {
		for (GridPart part : parts)
			if (!part.canRotate()) return false;
		for (GridPart part : parts)
			part.rotate(steps);
		updateBounds();
		return true;
	}

	public boolean shift(Direction d, int n, Grid other) {
		if (other == null && (bounds() & GridPart.mask(d.ordinal(), n)) != 0)
			return false;
		for (GridPart part : parts)
			if (!part.canMove(d, n)) return false;
		for (int i = parts.size() - 1; i >= 0; i--) {
			GridPart part = parts.get(i);
			GridPart part1 = part.move(d, n);
			if (part1 == part) parts.remove(i);
			if (part1 != null && part1.merge(other))
				other.parts.add(part1);
		}
		return true;
	}

	private void updateTerParts() {
		int n = 0;
		long m = 0;
		for (int l = Math.min(parts.size(), 64), i = 0; i < l; i++)
			if (parts.get(i) instanceof IDynamicPart) {
				m |= 1L << i;
				n++;
			}
		IDynamicPart[] arr = n == 0 ? null
			: dynamicParts != null && dynamicParts.length == n ? dynamicParts
			: new IDynamicPart[n];
		for (int j = 0, i = 0; i < n; i++, j++) {
			while(m << ~j >= 0) j++;
			arr[i] = (IDynamicPart)parts.get(j);
		}
		dynamicParts = arr;
		update = false;
	}

	@Override
	protected byte writeSync(FriendlyByteBuf pkt, boolean init) {
		byte i = 0;
		for (GridPart part : parts)
			if (part instanceof IDynamicPart) {
				((IDynamicPart)part).writeSync(pkt, init);
				i++;
			}
		return i;
	}

	@Override
	protected void readSync(FriendlyByteBuf pkt, byte n) {
		if (dynamicParts != null && dynamicParts.length == n)
			for (IDynamicPart m : dynamicParts)
				m.readSync(pkt);
	}

	@Override
	public BlockEntityType<?> getType() {
		return level != null && level.isClientSide && dynamicParts != null
			? Content.GRID_TER : super.getType();
	}

	@Override
	public boolean evaluate() {
		updateOccl = false;
		if (unloaded) return false;
		long o = ~opaque;
		return occlude != (occlude =
			(o & 0xf99f_9009_9009_f99fL) == 0 &&
			((o & 0x0000_0660_0660_0000L) == 0 || isSealed(o))
		);
	}

	private static boolean isSealed(long bounds) {
		for (long f : GridPart.FACES)
			if ((GridPart.floodFill(bounds, bounds & f) & ~(f | 0x0000_0660_0660_0000L)) != 0)
				return false;
		return true;
	}

	@Override
	public void latchOut() {
		level.setBlock(worldPosition, (occlude ? GRID1 : GRID).defaultBlockState(), 2);
	}

}
