package cd4017be.lib.item;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static cd4017be.lib.text.TooltipUtil.*;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;

/**Item that shows extra description in tooltip.
 * @author CD4017BE */
public class DocumentedItem extends Item {

	public static final TranslationTextComponent EXT_TOOLTIP_HINT = new TranslationTextComponent("cd4017be_lib.ext");

	private Supplier<Object[]> tooltipArgs;

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

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		addInformation(getTranslationKey(), tooltipArgs, tooltip);
		super.addInformation(stack, world, tooltip, flag);
	}

	@Override
	public ITextComponent getDisplayName(ItemStack stack) {
		return cTranslate(getTranslationKey(stack));
	}

	public static void addInformation(String key, Supplier<Object[]> tooltipArgs, List<ITextComponent> tooltip) {
		Style style = Style.EMPTY.setFormatting(TextFormatting.GRAY);
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

}
