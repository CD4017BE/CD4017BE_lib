package cd4017be.lib.tileentity;

import java.util.ArrayList;
import java.util.List;

import cd4017be.api.IAbstractTile;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
public class BaseTileEntity extends TileEntity implements IAbstractTile {

	private IBlockState blockState;
	private Chunk chunk;
	/** whether this TileEntity is currently not part of the loaded world and therefore shouldn't perform any actions */
	protected boolean unloaded = true;
	protected boolean redraw;

	public BaseTileEntity() {}

	public BaseTileEntity(IBlockState state) {
		blockState = state;
		blockType = blockState.getBlock();
	}

	public IBlockState getBlockState() {
		if (blockState == null) {
			blockState = chunk.getBlockState(pos);
			blockType = blockState.getBlock();
		}	
		return blockState;
	}

	public Chunk getChunk() {
		return chunk;
	}

	public Orientation getOrientation() {
		IBlockState state = getBlockState();
		if (blockType instanceof OrientedBlock)
			return state.getValue(((OrientedBlock)blockType).orientProp);
		else return Orientation.N;
	}

	/**@Deprecated use {@link #markDirty(int)} with {@link #SYNC} instead */
	@Deprecated
	public void markUpdate() {
		if (unloaded) return;
		IBlockState state = getBlockState();
		world.notifyBlockUpdate(pos, state, state, 3);
	}

	@Override //a "little" shortcut for better performance
	public void markDirty() {
		if (chunk != null) chunk.markDirty();
	}

	/**chunk save data: contains the full TileEntity state */
	public static final int SAVE = 0;
	/**client chunk data: contains the full TileEntity state relevant for the client */
	public static final int CLIENT = 1;
	/**client sync data: contains only the TileEntity state that likely changed since last synchronization */
	public static final int SYNC = 2;
	/**{@link #SYNC} with a hint for the client to re-render the block */
	public static final int REDRAW = 3;
	/**itemstack data: contains the break/place persistent state of the TileEntity */
	public static final int ITEM = 4;

	/**
	 * marks that the state of this TileEntity changed
	 * @param mode type of data changed: {@link #REDRAW} => {@link #SYNC} => {@link #SAVE}
	 */
	public void markDirty(int mode) {
		if (unloaded) return;
		switch(mode) {
		case REDRAW:
			redraw = true;
		case SYNC:
			IBlockState state = getBlockState();
			world.notifyBlockUpdate(pos, state, state, 2);
		case SAVE:
			chunk.markDirty();
		}
	}

	/**
	 * make this TileEntity save it's state to given data.
	 * @param nbt serialized nbt data
	 * @param mode the type of data to store: {@link #ITEM} <= {@link #SAVE} => {@link #CLIENT} => {@link #SYNC}
	 */
	protected void storeState(NBTTagCompound nbt, int mode) {
	}

	/**
	 * make this TileEntity load it's state from given data.
	 * @param nbt serialized nbt data
	 * @param mode the type of data to load: {@link #ITEM} <= {@link #SAVE} => {@link #CLIENT} => {@link #SYNC}
	 */
	protected void loadState(NBTTagCompound nbt, int mode) {
	}

	@Override
	@Deprecated
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		storeState(nbt, SAVE);
		return super.writeToNBT(nbt);
	}

	@Override
	@Deprecated
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		loadState(nbt, SAVE);
	}

	@Override
	@Deprecated
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound nbt = new NBTTagCompound();
		storeState(nbt, CLIENT);
		return this.writeToNBT(nbt); //TODO change to super.getUpdateTag();
	}

	@Override
	@Deprecated
	public void handleUpdateTag(NBTTagCompound nbt) {
		this.readFromNBT(nbt); //TODO change to super.handleUpdateTag(nbt);
		loadState(nbt, CLIENT);
	}

	@Override
	@Deprecated
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		storeState(nbt, SYNC);
		if (nbt.hasNoTags()) return null;
		if (redraw) {
			nbt.setBoolean("", true);
			redraw = false;
		}
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	@Deprecated
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound nbt = pkt.getNbtCompound();
		redraw = nbt.getBoolean("");
		loadState(nbt, SYNC);
		if (redraw)
			world.markBlockRangeForRenderUpdate(pos, pos);
	}

	@Override
	public void onLoad() {
		if (world.isRemote ? this instanceof ITickableServerOnly : this instanceof ITickableClientOnly)
			world.tickableTileEntities.remove(this);
		chunk = world.getChunkFromBlockCoords(pos);
		setupData();
		unloaded = false;
	}

	/**
	 * Called when this TileEntity is removed from the world be it by breaking, replacement or chunk unloading.
	 */
	protected void onUnload() {
		unloaded = true;
		chunk = null;
		clearData();
	}

	@Override
	public void onChunkUnload() {
		onUnload();
	}

	@Override
	public void invalidate() {
		tileEntityInvalid = true;
		onUnload();
	}

	/**@Deprecated override {@link #onLoad()} instead */
	@Deprecated
	protected void setupData() {
	}

	/**@Deprecated override {@link #onUnLoad()} instead*/
	@Deprecated
	protected void clearData() {
	}

	@Override
	public boolean invalid() {
		return unloaded;
	}

	@Override
	public void setWorld(World worldIn) {
		super.setWorld(worldIn);
		if (chunk == null && worldIn.isBlockLoaded(pos))
			chunk = world.getChunkFromBlockCoords(pos);
	}

	@Override
	public TileEntity getTileOnSide(EnumFacing s) {
		return Utils.neighborTile(this, s);
	}

	public BlockPos pos() {
		return pos;
	}

	@Override //just skip all the ugly hard-coding in superclass
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	protected List<ItemStack> makeDefaultDrops() {
		NBTTagCompound nbt = new NBTTagCompound();
		storeState(nbt, ITEM);
		return makeDefaultDrops(nbt);
	}

	protected List<ItemStack> makeDefaultDrops(NBTTagCompound tag) {
		getBlockState();
		ItemStack item = new ItemStack(blockType, 1, blockType.damageDropped(blockState));
		item.setTagCompound(tag);
		ArrayList<ItemStack> list = new ArrayList<ItemStack>(1);
		list.add(item);
		return list;
	}

	public boolean canPlayerAccessUI(EntityPlayer player) {
		return !player.isDead && !unloaded && getDistanceSq(player.posX, player.posY, player.posZ) < 64;
	}

	public String getName() {
		return TooltipUtil.translate(this.getBlockType().getUnlocalizedName().replace("tile.", "gui.").concat(".name"));
	}

	@Override
	public void updateContainingBlockInfo() {
		super.updateContainingBlockInfo();
		blockState = null;
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return newState.getBlock() != oldState.getBlock();
	}

	@Override
	public boolean isClient() {
		return world.isRemote;
	}

	/**
	 * Indicates that the implementing TileEntity should only receive server side update ticks.
	 * @author CD4017BE
	 */
	public interface ITickableServerOnly extends ITickable {}

	/**
	 * Indicates that the implementing TileEntity should only receive client side update ticks.
	 * @author CD4017BE
	 */
	public interface ITickableClientOnly extends ITickable {}

}
