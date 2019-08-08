package cd4017be.lib.templates;

import javax.annotation.Nullable;

import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author CD4017BE
 */
@SuppressWarnings("deprecation")
public class Cover {

	/**the ItemStack used to cover */
	public ItemStack stack;
	/**the block state of the cover */
	public IBlockState state;
	/**whether the cover is fully opaque, so the block itself doesn't need to render or update its visuals */
	public boolean opaque;

	/**
	 * handles player right-click on the coverable block
	 * @param tile coverable TileEntity
	 * @param player
	 * @param hand
	 * @param item
	 * @param s
	 * @param X
	 * @param Y
	 * @param Z
	 * @return whether this consumed the event
	 */
	public boolean interact(BaseTileEntity tile, EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		if (stack != null) {
			if (player.isCreative() && item.isEmpty() && player.isSneaking()) return hit(tile, player);
			return false;
		}
		item = player.getHeldItem(hand = EnumHand.OFF_HAND);
		if (player.isSneaking() || item.isEmpty() || !(item.getItem() instanceof ItemBlock)) return false;
		World world = tile.getWorld();
		BlockPos pos = tile.getPos();
		ItemBlock ib = (ItemBlock)item.getItem();
		int m = ib.getMetadata(item.getMetadata());
		IBlockState state = ib.getBlock().getStateForPlacement(world, pos, s, X, Y, Z, m, player, hand);
		if (!isBlockValid(tile, state)) return false;
		this.stack = ItemHandlerHelper.copyStackWithSize(item, 1);
		this.state = state;
		this.opaque = state.isOpaqueCube();
		if (!player.isCreative()) {
			item.grow(-1);
			player.setHeldItem(hand, item);
		}
		world.notifyNeighborsRespectDebug(pos, tile.getBlockType(), true);
		if (state.getLightValue() > 0 || state.getLightOpacity(world, pos) > 0) world.checkLight(pos);
		tile.markDirty(BaseTileEntity.REDRAW);
		return true;
	}

	/**
	 * handles player left-click on the coverable block
	 * @param tile coverable TileEntity
	 * @param player
	 * @return whether this consumed the event
	 */
	public boolean hit(BaseTileEntity tile, EntityPlayer player) {
		if (stack == null) return false;
		if (!player.isCreative()) ItemFluidUtil.dropStack(stack, player);
		World world = tile.getWorld();
		BlockPos pos = tile.getPos();
		boolean checkLight = state.getLightValue() > 0 || state.getLightOpacity(world, pos) > 0;
		stack = null;
		state = null;
		opaque = false;
		world.notifyNeighborsRespectDebug(pos, tile.getBlockType(), true);
		if (checkLight) world.checkLight(pos);
		tile.markDirty(BaseTileEntity.REDRAW);
		return true;
	}

	public static boolean isBlockValid(@Nullable TileEntity tile, IBlockState state) {
		if (state.getBlock().hasTileEntity(state)) return false;
		return state.getMaterial().blocksMovement();
	}

	/**
	 * load data from save or sync-packet
	 * @param nbt data
	 * @param k tag base name
	 * @param packetReceiver the TileEntity if it received a server -> client sync-packet, otherwise null
	 */
	public void readNBT(NBTTagCompound nbt, String k, @Nullable TileEntity packetReceiver) {
		if (nbt.hasKey(k + "I", Constants.NBT.TAG_COMPOUND))
			stack = new ItemStack(nbt.getCompoundTag(k + "I"));
		else stack = null;
		String name = nbt.getString(k + "B");
		state = null;
		if (!name.isEmpty()) {
			Block block = Block.getBlockFromName(name);
			if (block != null) {
				state = block.getStateFromMeta(nbt.getByte(k + "m") & 0xf);
				if (!isBlockValid(null, state)) state = null;
			}
		}
		opaque = state != null && state.isOpaqueCube();
		if (packetReceiver != null) packetReceiver.getWorld().checkLight(packetReceiver.getPos());
	}

	/**
	 * load data to save or sync-packet
	 * @param nbt data
	 * @param k tag base name
	 * @param packetSync whether this is for a server -> client sync-packet
	 */
	public void writeNBT(NBTTagCompound nbt, String k, boolean packetSync) {
		if (!packetSync && stack != null) nbt.setTag(k + "I", stack.writeToNBT(new NBTTagCompound()));
		if (state != null) {
			Block block = state.getBlock();
			nbt.setString(k + "B", block.getRegistryName().toString());
			nbt.setByte(k + "m", (byte)block.getMetaFromState(state));
		}
	}

	@SuppressWarnings("unchecked")
	public <M> M module() {
		return (M)state;
	}

}
