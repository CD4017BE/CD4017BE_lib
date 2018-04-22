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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
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

	public BaseTileEntity() {}

	public BaseTileEntity(IBlockState state) {
		blockState = state;
		blockType = blockState.getBlock();
	}

	public IBlockState getBlockState() {
		if (blockState == null) {
			if (chunk == null) chunk = world.getChunkFromBlockCoords(pos);
			blockState = chunk.getBlockState(pos);
			blockType = blockState.getBlock();
		}	
		return blockState;
	}

	public Orientation getOrientation() {
		getBlockState();
		if (blockType instanceof OrientedBlock)
			return blockState.getValue(((OrientedBlock)blockType).orientProp);
		else return Orientation.N;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	/**
	 * Fire render(client) / data(server) update
	 */
	public void markUpdate() {
		getBlockState();
		world.notifyBlockUpdate(pos, blockState, blockState, 3);
	}

	@Override //cache chunk reference to speed this up
	public void markDirty() {
		if (chunk == null) {
			if (tileEntityInvalid || world == null) return;
			chunk = world.getChunkFromBlockCoords(pos);
		}
		chunk.markDirty();
	}

	@Override //just skip all the ugly hard-coding in superclass
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	@Override
	public void onLoad() {
		unloaded = false;
		setupData();
	}

	@Override
	public void onChunkUnload() {
		unloaded = true;
		chunk = null;
		clearData();
	}

	@Override
	public void validate() {
		tileEntityInvalid = unloaded = false;
		setupData();
	}

	@Override
	public void invalidate() {
		tileEntityInvalid = unloaded = true;
		chunk = null;
		clearData();
	}

	protected void setupData() {
	}

	protected void clearData() {
	}

	@Override
	public boolean invalid() {
		return unloaded;
	}

	@Override
	public ICapabilityProvider getTileOnSide(EnumFacing s) {
		return Utils.neighborTile(this, s);
	}

	public BlockPos pos() {
		return pos;
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
		return !player.isDead && !tileEntityInvalid && getDistanceSq(player.posX, player.posY, player.posZ) < 64;
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

}
