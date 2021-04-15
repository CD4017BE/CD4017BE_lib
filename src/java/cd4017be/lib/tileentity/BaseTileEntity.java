package cd4017be.lib.tileentity;

import java.util.function.Consumer;

import javax.annotation.Nullable;
import cd4017be.lib.network.INBTSynchronized;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.*;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.Chunk.CreateEntityType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import static cd4017be.lib.network.Sync.*;

/** @author CD4017BE */
public class BaseTileEntity extends TileEntity implements INBTSynchronized {

	private Chunk chunk;
	protected boolean unloaded = true;
	protected boolean redraw;

	public BaseTileEntity(TileEntityType<?> type) {
		super(type);
	}

	/** whether this TileEntity is currently not part of the loaded world and therefore shouldn't perform any actions */
	public boolean unloaded() {
		return unloaded;
	}

	public Chunk getChunk() {
		if (chunk == null)
			chunk = (Chunk)world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
		return chunk;
	}

	/** Tells the game that this TileEntity has changed state that needs saving to disk. */
	public void saveDirty() {
		Chunk c = getChunk();
		if(c != null) c.markDirty();
	}

	/** Tells the game that this TileEntity has changed state that needs to be sent to clients.
	 * @param redraw whether client should do render update as well */
	public void clientDirty(boolean redraw) {
		if(unloaded) return;
		this.redraw |= redraw;
		BlockState state = getBlockState();
		world.notifyBlockUpdate(pos, state, state, 2);
		saveDirty();
	}

	/** redraw flag from {@link #clientDirty(boolean)} */
	public static final int REDRAW = 16;

	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		storeState(nbt, SAVE);
		return super.write(nbt);
	}

	@Override
	public void read(BlockState state, CompoundNBT nbt) {
		super.read(state, nbt);
		loadState(nbt, SAVE);
	}

	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = super.getUpdateTag();
		storeState(nbt, CLIENT);
		return nbt;
	}

	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT nbt) {
		super.handleUpdateTag(state, nbt);
		//cachedBlockState = state;
		loadState(nbt, CLIENT);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		int i = SYNC | (redraw ? REDRAW : 0);
		storeState(nbt, i);
		if(nbt.isEmpty()) return null;
		redraw = false;
		return new SUpdateTileEntityPacket(pos, i, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		CompoundNBT nbt = pkt.getNbtCompound();
		int i = pkt.getTileEntityType();
		loadState(nbt, SYNC | i);
		if(redraw = (i & REDRAW) != 0) {
			requestModelDataUpdate();
			BlockState state = getBlockState();
			world.notifyBlockUpdate(pos, state, state, 2);
		}
	}

	@Override
	public void onLoad() {
		if(world.isRemote ? this instanceof ITickableServerOnly : this instanceof ITickableClientOnly)
			world.tickableTileEntities.remove(this);
		unloaded = false;
	}

	/** Called when this TileEntity is removed from the world be it by breaking, replacement or chunk unloading. */
	protected void onUnload() {}

	@Override
	public void onChunkUnloaded() {
		chunk = null;
		unloaded = true;
		onUnload();
		invalidateCaps();
	}

	@Override
	public void remove() {
		super.remove();
		chunk = null;
		unloaded = true;
		onUnload();
	}

	@Override // just skip all the ugly hard-coding in superclass
	@OnlyIn(Dist.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	public @Nullable Chunk getChunk(BlockPos pos, boolean loadChunks) {
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		ChunkPos cp = getChunk().getPos();
		return cx == cp.x && cz == cp.z ? chunk
			: (Chunk)world.getChunk(cx, cz, ChunkStatus.FULL, loadChunks);
	}

	/** This method has faster performance than {@link World#getBlockState(BlockPos)}
	 * for <b>pos</b> within the same chunk.
	 * @param pos
	 * @param loadChunks whether to hot-load chunks if necessary
	 * @return the BlockState at <b>pos</b> */
	public BlockState getBlockState(BlockPos pos, boolean loadChunks) {
		if(World.isOutsideBuildHeight(pos)) return Blocks.VOID_AIR.getDefaultState();
		Chunk c = getChunk(pos, loadChunks);
		return c != null ? c.getBlockState(pos) : Blocks.VOID_AIR.getDefaultState();
	}

	/** This method has faster performance than {@link World#getBlockState(BlockPos)}
	 * for <b>pos</b> within the same chunk.
	 * @param pos
	 * @param loadChunks whether to hot-load chunks if necessary
	 * @return the TileEntity at <b>pos</b> */
	public @Nullable TileEntity getTileEntity(BlockPos pos, boolean loadChunks) {
		if(World.isOutsideBuildHeight(pos)) return null;
		Chunk c = getChunk(pos, loadChunks);
		return c != null ? c.getTileEntity(pos, CreateEntityType.IMMEDIATE) : null;
	}

	public @Nullable TileEntity getNeighborTileEntity(Direction side, boolean loadChunks) {
		return getTileEntity(pos.offset(side), loadChunks);
	}

	/**
	 * @param side side of this tile to get neighboring capability for
	 * @param cap the capability type
	 * @return capability */
	public <T> T getNeighborCapability(Direction side, Capability<T> cap, T empty) {
		TileEntity te = getTileEntity(pos.offset(side), false);
		return te != null ? te.getCapability(cap, side.getOpposite()).orElse(empty) : empty;
	}

	public <T> boolean updateNeighborCapability(Direction side, Capability<T> cap, Consumer<T> cache, T empty) {
		TileEntity te = getTileEntity(pos.offset(side), false);
		if (te == null) {
			cache.accept(empty);
			return false;
		}
		LazyOptional<T> oc = te.getCapability(cap, side);
		cache.accept(oc.orElse(empty));
		if (oc.isPresent())
			oc.addListener(op -> {
				if (!updateNeighborCapability(side, cap, cache, empty))
					onNeighbourRemoved(side);
			});
		return true;
	}

	protected void onNeighbourRemoved(Direction side) {}

	/*
	public Orientation getOrientation() {
		BlockState state = getBlockState();
		Block block = state.getBlock();
		if (block instanceof OrientedBlock)
			return state.getValue(((OrientedBlock)block).orientProp);
		else return Orientation.N;
	}
	
	protected List<ItemStack> makeDefaultDrops() {
		CompoundNBT nbt = new CompoundNBT();
		storeState(nbt, ITEM);
		return makeDefaultDrops(nbt);
	}
	
	protected List<ItemStack> makeDefaultDrops(CompoundNBT tag) {
		getBlockState();
		ItemStack item = new ItemStack(blockType, 1, blockType.damageDropped(blockState));
		item.setTag(tag);
		ArrayList<ItemStack> list = new ArrayList<ItemStack>(1);
		list.add(item);
		return list;
	}
	
	public boolean canPlayerAccessUI(PlayerEntity player) {
		getBlockState();
		return player.isAlive() && !unloaded && getDistanceSq(player.posX, player.posY, player.posZ) < 64;
	}
	
	public String getName() {
		return TooltipUtil.translate(this.getBlockType().getTranslationKey().replace("tile.", "gui.").concat(".name"));
	}
	*/

	/** Indicates that the implementing TileEntity should only receive server side update ticks.
	 * @author CD4017BE */
	public interface ITickableServerOnly extends ITickableTileEntity {}


	/** Indicates that the implementing TileEntity should only receive client side update ticks.
	 * @author CD4017BE */
	public interface ITickableClientOnly extends ITickableTileEntity {}

}
