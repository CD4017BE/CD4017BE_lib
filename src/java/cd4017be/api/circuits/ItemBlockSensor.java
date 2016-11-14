package cd4017be.api.circuits;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import cd4017be.lib.DefaultItem;
import cd4017be.lib.TooltipInfo;

public abstract class ItemBlockSensor extends DefaultItem implements ISensor {

	private final double RangeSQ;

	public ItemBlockSensor(String id, float range) {
		super(id);
		this.RangeSQ = (double)(range * range);
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		if (item.hasTagCompound()) list.add(TooltipInfo.formatLink(BlockPos.fromLong(item.getTagCompound().getLong("link")), EnumFacing.getFront(item.getTagCompound().getByte("side"))));
		super.addInformation(item, player, list, b);
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return String.format(super.getItemStackDisplayName(item), item.hasTagCompound() ? item.getTagCompound().getFloat("cache") : 0F);
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if (!world.isRemote) measure(stack, world, entity.getPosition());
	}

	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!player.isSneaking()) return EnumActionResult.PASS;
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		nbt.setLong("link", pos.toLong());
		nbt.setByte("side", (byte)side.ordinal());
		return EnumActionResult.SUCCESS;
	}

	@Override
	public float measure(ItemStack sensor, World world, BlockPos src) {
		NBTTagCompound nbt = sensor.getTagCompound();
		if (nbt == null) return 0F;
		BlockPos pos = BlockPos.fromLong(nbt.getLong("link"));
		if (pos.getY() < 0 || pos.getY() >= 256 || pos.distanceSq(src) > RangeSQ || !world.isBlockLoaded(pos)) return nbt.getFloat("cache");
		EnumFacing side = EnumFacing.getFront(nbt.getByte("side"));
		float x = this.measure(sensor, nbt, world, pos, side);
		nbt.setFloat("cache", x);
		return x;
	}

	protected abstract float measure(ItemStack sensor, NBTTagCompound nbt, World world, BlockPos pos, EnumFacing side);

}
