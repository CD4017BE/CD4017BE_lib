package cd4017be.lib;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

import org.lwjgl.input.Keyboard;

import cd4017be.lib.util.TooltipUtil;

/**
 *
 * @author CD4017BE
 */
public class DefaultItemBlock extends ItemBlock
{
	
	public DefaultItemBlock(Block id) {
		super(id);
		this.setRegistryName(id.getRegistryName());
		//TODO fix registration
		this.init();
	}
	
	protected void init() {
		BlockItemRegistry.registerItemStack(new ItemStack(this), "tile." + this.getRegistryName().getResourcePath());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		String s = this.getUnlocalizedName(item) + ".tip";
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
			String s1 = TooltipUtil.getConfigFormat(s);
			if (!s1.equals(s)) list.addAll(Arrays.asList(s1.split("\n")));
		} else if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
			String sA = s + "A";
			String s1 = TooltipUtil.getConfigFormat(sA);
			if (!s1.equals(sA)) list.addAll(Arrays.asList(s1.split("\n")));
		} else {
			if (TooltipUtil.hasTranslation(s) || (hasSubtypes && TooltipUtil.hasTranslation(getBlock().getUnlocalizedName() + ":i.tip"))) list.add(TooltipUtil.getShiftHint());
			if (TooltipUtil.hasTranslation(s + "A") || (hasSubtypes && TooltipUtil.hasTranslation(getBlock().getUnlocalizedName() + ":i.tipA"))) list.add(TooltipUtil.getAltHint());
		}
		super.addInformation(item, player, list, b);
	}

	@Override
	public String getUnlocalizedName(ItemStack item) {
		String s = this.getBlock().getUnlocalizedName();
		return this.hasSubtypes ? s + ":" + item.getItemDamage() : s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return TooltipUtil.translate(this.getUnlocalizedName(item) + ".name");
	}
	
}
