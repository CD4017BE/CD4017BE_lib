package cd4017be.lib.item;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static cd4017be.lib.text.TooltipUtil.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;

/**Item that shows extra description in tooltip.
 * @author CD4017BE */
public class DocumentedItem extends Item {

	public static final TranslationTextComponent EXT_TOOLTIP_HINT = new TranslationTextComponent("cd4017be_lib.ext");

	private Supplier<Object[]> tooltipArgs;
	protected ItemGroup extraTab;

	public DocumentedItem(Properties p) {
		super(p);
	}

	/**Supply format arguments for translated tooltip
	 * @param tooltipArgs
	 * @return this */
	public DocumentedItem tooltipArgs(Supplier<Object[]> tooltipArgs) {
		this.tooltipArgs = tooltipArgs;
		return this;
	}

	public DocumentedItem tab(ItemGroup extraTab) {
		this.extraTab = extraTab;
		return this;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		addInformation(getDescriptionId(), tooltipArgs, tooltip);
		super.appendHoverText(stack, world, tooltip, flag);
	}

	@Override
	public ITextComponent getName(ItemStack stack) {
		return cTranslate(getDescriptionId(stack));
	}

	public static void addInformation(String key, Supplier<Object[]> tooltipArgs, List<ITextComponent> tooltip) {
		Style style = Style.EMPTY.withColor(TextFormatting.GRAY);
		String key1;
		if (hasTranslation(key1 = key + ".tip"))
			if (showShiftHint())
				tooltip.add(cTranslate(key1).setStyle(style));
			else tooltip.add(TOOLTIP_HINT);
		if (hasTranslation(key1 = key + ".ext"))
			if (showAltHint())
				tooltip.add(cFormat(key1,
					tooltipArgs == null ? new Object[0] : tooltipArgs.get()
				).setStyle(style));
			else tooltip.add(EXT_TOOLTIP_HINT);
	}

	@Override
	public Collection<ItemGroup> getCreativeTabs() {
		if (extraTab == null) return super.getCreativeTabs();
		return ImmutableList.of(category, extraTab);
	}

}
