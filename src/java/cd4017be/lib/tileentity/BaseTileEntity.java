package cd4017be.lib.tileentity;

import static java.lang.Double.NaN;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.network.INBTSynchronized;
import cd4017be.lib.util.Orientation;
import net.minecraft.block.Block;
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
	public boolean unloaded = true;
	protected boolean redraw, sent;

	public BaseTileEntity(TileEntityType<?> type) {
		super(type);
	}

	/** whether this TileEntity is currently not part of the loaded world and therefore shouldn't perform any actions */
	public boolean unloaded() {
		return unloaded;
	}

	public Chunk getChunk() {
		if (chunk == null)
			chunk = (Chunk)level.getChunk(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, ChunkStatus.FULL, false);
		return chunk;
	}

	/** Tells the game that this TileEntity has changed state that needs saving to disk. */
	public void saveDirty() {
		if (unloaded) return;
		Chunk c = getChunk();
		if(c != null) c.markUnsaved();
	}

	/** Tells the game that this TileEntity has changed state that needs to be sent to clients.
	 * @param redraw whether client should do render update as well */
	public void clientDirty(boolean redraw) {
		if(unloaded) return;
		if(sent) this.redraw = sent = false;
		this.redraw |= redraw;
		BlockState state = getBlockState();
		level.sendBlockUpdated(worldPosition, state, state, 2);
		saveDirty();
	}

	/** redraw flag from {@link #clientDirty(boolean)} */
	public static final int REDRAW = 16;

	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		storeState(nbt, SAVE);
		return super.save(nbt);
	}

	@Override
	public void load(BlockState state, CompoundNBT nbt) {
		super.load(state, nbt);
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
		super.load(state, nbt);
		loadState(nbt, CLIENT);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		int i = SYNC | (redraw ? REDRAW : 0);
		storeState(nbt, i);
		if(nbt.isEmpty()) return null;
		sent = true;
		return new SUpdateTileEntityPacket(worldPosition, i, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		CompoundNBT nbt = pkt.getTag();
		int i = pkt.getType();
		loadState(nbt, SYNC | i);
		if(redraw = (i & REDRAW) != 0) {
			requestModelDataUpdate();
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, 2);
		}
	}

	@Override
	public void onLoad() {
		if(level.isClientSide ? this instanceof ITickableServerOnly : this instanceof ITickableClientOnly)
			level.tickableBlockEntities.remove(this);
		if (!unloaded) clearCache();
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
	public void setRemoved() {
		super.setRemoved();
		chunk = null;
		unloaded = true;
		onUnload();
	}

	/**For {@link #getRenderBoundingBox()} to always fail the visibility check. */
	public static final AxisAlignedBB DONT_RENDER
	= new AxisAlignedBB(NaN, NaN, NaN, NaN, NaN, NaN);

	@Override
	@OnlyIn(Dist.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		// just skip all the ugly hard-coding in superclass
		return new AxisAlignedBB(worldPosition);
	}

	public Orientation orientation() {
		BlockState state = getBlockState();
		Block block = state.getBlock();
		if (block instanceof OrientedBlock)
			return state.getValue(((OrientedBlock<?>)block).orientProp);
		return Orientation.S12;
	}

	public @Nullable Chunk getChunk(BlockPos pos, boolean loadChunks) {
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		ChunkPos cp = getChunk().getPos();
		return cx == cp.x && cz == cp.z ? chunk
			: (Chunk)level.getChunk(cx, cz, ChunkStatus.FULL, loadChunks);
	}

	/** This method has faster performance than {@link World#getBlockState(BlockPos)}
	 * for <b>pos</b> within the same chunk.
	 * @param pos
	 * @param loadChunks whether to hot-load chunks if necessary
	 * @return the BlockState at <b>pos</b> */
	public BlockState getBlockState(BlockPos pos, boolean loadChunks) {
		if(World.isOutsideBuildHeight(pos)) return Blocks.VOID_AIR.defaultBlockState();
		Chunk c = getChunk(pos, loadChunks);
		return c != null ? c.getBlockState(pos) : Blocks.VOID_AIR.defaultBlockState();
	}

	/** This method has faster performance than {@link World#getBlockState(BlockPos)}
	 * for <b>pos</b> within the same chunk.
	 * @param pos
	 * @param loadChunks whether to hot-load chunks if necessary
	 * @return the TileEntity at <b>pos</b> */
	public @Nullable TileEntity getTileEntity(BlockPos pos, boolean loadChunks) {
		if(World.isOutsideBuildHeight(pos)) return null;
		Chunk c = getChunk(pos, loadChunks);
		return c != null ? c.getBlockEntity(pos, CreateEntityType.IMMEDIATE) : null;
	}

	public @Nullable TileEntity getNeighborTileEntity(Direction side, boolean loadChunks) {
		return getTileEntity(worldPosition.relative(side), loadChunks);
	}

	/**
	 * @param side side of this tile to get neighboring capability for
	 * @param cap the capability type
	 * @return capability */
	public <T> T getNeighborCapability(Direction side, Capability<T> cap, T empty) {
		TileEntity te = getTileEntity(worldPosition.relative(side), false);
		return te != null ? te.getCapability(cap, side.getOpposite()).orElse(empty) : empty;
	}

	public <T> boolean updateNeighborCapability(Direction side, Capability<T> cap, Consumer<T> cache, T empty) {
		TileEntity te = getTileEntity(worldPosition.relative(side), false);
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
