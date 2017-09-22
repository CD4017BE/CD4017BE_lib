package cd4017be.lib;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

import org.lwjgl.input.Keyboard;

import cd4017be.lib.util.TooltipUtil;

/**
 *
 * @author CD4017BE
 */
public class DefaultItem extends Item {

	public DefaultItem(String id) {
		super();
		this.setRegistryName(id);
		this.setUnlocalizedName(TooltipUtil.unlocalizedNameFor(this));
		GameRegistry.register(this);
		this.init();
	}

	protected void init() {
		BlockItemRegistry.registerItemStack(new ItemStack(this), "item." + this.getRegistryName().getResourcePath());
	}

	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) {
		String s = this.getUnlocalizedName(item) + ".tip";
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
			String s1 = TooltipUtil.getConfigFormat(s);
			if (!s1.equals(s)) list.addAll(Arrays.asList(s1.split("\n")));
		} else if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
			String sA = s + "A";
			String s1 = TooltipUtil.getConfigFormat(sA);
			if (!s1.equals(sA)) list.addAll(Arrays.asList(s1.split("\n")));
		} else {
			if (TooltipUtil.hasTranslation(s) || (hasSubtypes && TooltipUtil.hasTranslation(super.getUnlocalizedName() + ":i.tip"))) list.add(TooltipUtil.getShiftHint());
			if (TooltipUtil.hasTranslation(s + "A") || (hasSubtypes && TooltipUtil.hasTranslation(super.getUnlocalizedName() + ":i.tipA"))) list.add(TooltipUtil.getAltHint());
		}
		super.addInformation(item, player, list, b);
	}

	@Override
	public String getUnlocalizedName(ItemStack item) {
		String s = super.getUnlocalizedName(item);
		return this.hasSubtypes ? s + ":" + item.getItemDamage() : s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return TooltipUtil.translate(this.getUnlocalizedName(item) + ".name");
	}

}
