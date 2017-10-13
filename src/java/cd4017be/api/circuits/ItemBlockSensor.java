package cd4017be.api.circuits;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
import cd4017be.lib.util.TooltipUtil;

public abstract class ItemBlockSensor extends DefaultItem implements ISensor {

	public double RangeSQ;

	public ItemBlockSensor(String id, float range) {
		super(id);
		this.RangeSQ = (double)(range * range);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		if (item.hasTagCompound()) list.add(TooltipUtil.formatLink(BlockPos.fromLong(item.getTagCompound().getLong("link")), EnumFacing.getFront(item.getTagCompound().getByte("side"))));
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
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		if (!player.isSneaking()) return EnumActionResult.PASS;
		NBTTagCompound nbt = stack.getTagCompound();
		if (nbt == null) stack.setTagCompound(nbt = new NBTTagCompound());
		nbt.setLong("link", pos.toLong());
		nbt.setByte("side", (byte)side.ordinal());
		return EnumActionResult.SUCCESS;
	}

	@Override
	public double measure(ItemStack sensor, World world, BlockPos src) {
		NBTTagCompound nbt = sensor.getTagCompound();
		if (nbt == null) return 0D;
		BlockPos pos = BlockPos.fromLong(nbt.getLong("link"));
		if (pos.getY() < 0 || pos.getY() >= 256 || pos.distanceSq(src) > RangeSQ || !world.isBlockLoaded(pos)) return nbt.getFloat("cache");
		EnumFacing side = EnumFacing.getFront(nbt.getByte("side"));
		float x = this.measure(sensor, nbt, world, pos, side);
		nbt.setFloat("cache", x);
		return x;
	}

	protected abstract float measure(ItemStack sensor, NBTTagCompound nbt, World world, BlockPos pos, EnumFacing side);

}
