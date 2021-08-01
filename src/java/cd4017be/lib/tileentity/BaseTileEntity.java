package cd4017be.lib.tileentity;

import static java.lang.Double.NaN;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.network.INBTSynchronized;
import cd4017be.lib.util.Orientation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import static cd4017be.lib.network.Sync.*;

/** @author CD4017BE */
public class BaseTileEntity extends BlockEntity implements INBTSynchronized {

	private LevelChunk chunk;
	public boolean unloaded = true;
	protected boolean redraw, sent;

	public BaseTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** whether this BlockEntity is currently not part of the loaded world and therefore shouldn't perform any actions */
	public boolean unloaded() {
		return unloaded;
	}

	public LevelChunk getChunk() {
		if (chunk == null)
			chunk = (LevelChunk)level.getChunk(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, ChunkStatus.FULL, false);
		return chunk;
	}

	/** Tells the game that this BlockEntity has changed state that needs saving to disk. */
	public void saveDirty() {
		if (unloaded) return;
		LevelChunk c = getChunk();
		if(c != null) c.markUnsaved();
	}

	/** Tells the game that this BlockEntity has changed state that needs to be sent to clients.
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
	public CompoundTag save(CompoundTag nbt) {
		storeState(nbt, SAVE);
		return super.save(nbt);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		loadState(nbt, SAVE);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();
		storeState(nbt, CLIENT);
		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundTag nbt) {
		super.load(nbt);
		loadState(nbt, CLIENT);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		CompoundTag nbt = new CompoundTag();
		int i = SYNC | (redraw ? REDRAW : 0);
		storeState(nbt, i);
		if(nbt.isEmpty()) return null;
		sent = true;
		return new ClientboundBlockEntityDataPacket(worldPosition, i, nbt);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag nbt = pkt.getTag();
		int i = pkt.getType();
		loadState(nbt, SYNC | i);
		if(redraw = (i & REDRAW) != 0) {
			requestModelDataUpdate();
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, 2);
		}
	}

	protected void clearCache() {}

	/**{@inheritDoc}<br>
	 * WARNING: only called server side! */
	@Override
	public void onLoad() {
		if (!unloaded) clearCache();
		unloaded = false;
	}

	/** Called when this BlockEntity is removed from the world be it by breaking, replacement or chunk unloading. */
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
	public static final AABB DONT_RENDER
	= new AABB(NaN, NaN, NaN, NaN, NaN, NaN);

	@Override
	@OnlyIn(Dist.CLIENT)
	public AABB getRenderBoundingBox() {
		// just skip all the ugly hard-coding in superclass
		return new AABB(worldPosition);
	}

	public Orientation orientation() {
		BlockState state = getBlockState();
		Block block = state.getBlock();
		if (block instanceof OrientedBlock)
			return state.getValue(((OrientedBlock<?>)block).orientProp);
		return Orientation.S12;
	}

	public @Nullable LevelChunk getChunk(BlockPos pos, boolean loadChunks) {
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		ChunkPos cp = getChunk().getPos();
		return cx == cp.x && cz == cp.z ? chunk
			: (LevelChunk)level.getChunk(cx, cz, ChunkStatus.FULL, loadChunks);
	}

	/** This method has faster performance than {@link Level#getBlockState(BlockPos)}
	 * for <b>pos</b> within the same chunk.
	 * @param pos
	 * @param loadChunks whether to hot-load chunks if necessary
	 * @return the BlockState at <b>pos</b> */
	public BlockState getBlockState(BlockPos pos, boolean loadChunks) {
		if(level.isOutsideBuildHeight(pos)) return Blocks.VOID_AIR.defaultBlockState();
		LevelChunk c = getChunk(pos, loadChunks);
		return c != null ? c.getBlockState(pos) : Blocks.VOID_AIR.defaultBlockState();
	}

	/** This method has faster performance than {@link Level#getBlockState(BlockPos)}
	 * for <b>pos</b> within the same chunk.
	 * @param pos
	 * @param loadChunks whether to hot-load chunks if necessary
	 * @return the BlockEntity at <b>pos</b> */
	public @Nullable BlockEntity getTileEntity(BlockPos pos, boolean loadChunks) {
		if(level.isOutsideBuildHeight(pos)) return null;
		LevelChunk c = getChunk(pos, loadChunks);
		return c != null ? c.getBlockEntity(pos, EntityCreationType.IMMEDIATE) : null;
	}

	public @Nullable BlockEntity getNeighborTileEntity(Direction side, boolean loadChunks) {
		return getTileEntity(worldPosition.relative(side), loadChunks);
	}

	/**
	 * @param side side of this tile to get neighboring capability for
	 * @param cap the capability type
	 * @return capability */
	public <T> T getNeighborCapability(Direction side, Capability<T> cap, T empty) {
		BlockEntity te = getTileEntity(worldPosition.relative(side), false);
		return te != null ? te.getCapability(cap, side.getOpposite()).orElse(empty) : empty;
	}

	public <T> boolean updateNeighborCapability(Direction side, Capability<T> cap, Consumer<T> cache, T empty) {
		BlockEntity te = getTileEntity(worldPosition.relative(side), false);
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
		CompoundTag nbt = new CompoundTag();
		storeState(nbt, ITEM);
		return makeDefaultDrops(nbt);
	}
	
	protected List<ItemStack> makeDefaultDrops(CompoundTag tag) {
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

	/** Indicates that the implementing BlockEntity should receive server side update ticks.
	 * @author CD4017BE */
	public interface TickableServer {
		void tickServer(Level world, BlockPos pos, BlockState state);
	}


	/** Indicates that the implementing BlockEntity should receive client side update ticks.
	 * @author CD4017BE */
	public interface TickableClient {
		void tickClient(Level world, BlockPos pos, BlockState state);
	}

}
