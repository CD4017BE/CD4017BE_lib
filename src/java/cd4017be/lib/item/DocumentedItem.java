package cd4017be.lib.item;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import static cd4017be.lib.text.TooltipUtil.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;

/**Item that shows extra description in tooltip.
 * @author CD4017BE */
public class DocumentedItem extends Item {

	public static final TranslatableComponent EXT_TOOLTIP_HINT = new TranslatableComponent("cd4017be_lib.ext");

	private Supplier<Object[]> tooltipArgs;
	protected CreativeModeTab extraTab;

	public DocumentedItem(Properties p) {
		super(p);
	}

	/**Supply format arguments for translated tooltip
	 * @param tooltipArgs format argument supplier
	 * @return this */
	public DocumentedItem tooltipArgs(Supplier<Object[]> tooltipArgs) {
		this.tooltipArgs = tooltipArgs;
		return this;
	}

	/**Supply format arguments for translated tooltip
	 * @param tooltipArgs config values
	 * @return this */
	public DocumentedItem tooltipArgs(ConfigValue<?>... tooltipArgs) {
		return tooltipArgs(new ConfigArgs(tooltipArgs));
	}

	public DocumentedItem tab(CreativeModeTab extraTab) {
		this.extraTab = extraTab;
		return this;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
		addInformation(getDescriptionId(), tooltipArgs, tooltip);
		super.appendHoverText(stack, world, tooltip, flag);
	}

	@Override
	public Component getName(ItemStack stack) {
		return cTranslate(getDescriptionId(stack));
	}

	public static void addInformation(String key, Supplier<Object[]> tooltipArgs, List<Component> tooltip) {
		Style style = Style.EMPTY.withColor(ChatFormatting.GRAY);
		String key1;
		if (hasTranslation(key1 = key + ".tip"))
			if (showShiftHint())
				for (String s : translate(key1).split("\n"))
					tooltip.add(convert(s).setStyle(style));
			else tooltip.add(TOOLTIP_HINT);
		if (hasTranslation(key1 = key + ".ext"))
			if (showAltHint())
				for (String s : format(key1,
					tooltipArgs == null ? new Object[0] : tooltipArgs.get()
				).split("\n"))
					tooltip.add(convert(s).setStyle(style));
			else tooltip.add(EXT_TOOLTIP_HINT);
	}

	@Override
	public Collection<CreativeModeTab> getCreativeTabs() {
		if (extraTab == null) return super.getCreativeTabs();
		return ImmutableList.of(category, extraTab);
	}


	public static class ConfigArgs implements Supplier<Object[]> {

		private final ConfigValue<?>[] cfg;

		public ConfigArgs(ConfigValue<?>... cfg) {
			this.cfg = cfg;
		}

		@Override
		public Object[] get() {
			Object[] args = new Object[cfg.length];
			for (int i = 0; i < args.length; i++)
				args[i] = cfg[i].get();
			return args;
		}
	}

}
