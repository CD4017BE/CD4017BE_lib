package cd4017be.lib.block;

import java.util.ArrayList;
import java.util.List;

import cd4017be.api.IAbstractTile;
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
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BaseTileEntity extends TileEntity implements IAbstractTile {

	private IBlockState blockState;

	public BaseTileEntity() {}

	public BaseTileEntity(IBlockState state) {
		blockState = state;
	}

	public IBlockState getBlockState() {
		if (blockState == null) {
			blockState = world.getBlockState(pos);
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

	@Override //just skip all the ugly hard-coding in superclass
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	@Override
	public void onChunkUnload() {
		//make sure that possible reference holders don't think this TileEntity still exists.
		tileEntityInvalid = true;
	}

	@Override
	public boolean invalid() {
		return tileEntityInvalid;
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

}
