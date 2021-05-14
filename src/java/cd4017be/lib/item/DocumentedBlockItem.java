package cd4017be.lib.item;

import static cd4017be.lib.text.TooltipUtil.cTranslate;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import cd4017be.lib.item.DocumentedItem.ConfigArgs;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/**BlockItem that shows extra description in tooltip.
 * @author CD4017BE */
public class DocumentedBlockItem extends BlockItem {

	private Supplier<Object[]> tooltipArgs;

	public DocumentedBlockItem(Block id, Properties p) {
		super(id, p);
		this.setRegistryName(id.getRegistryName());
	}

	/**Supply format arguments for translated tooltip
	 * @param tooltipArgs
	 * @return this */
	public DocumentedBlockItem tooltipArgs(Supplier<Object[]> tooltipArgs) {
		this.tooltipArgs = tooltipArgs;
		return this;
	}

	/**Supply format arguments for translated tooltip
	 * @param tooltipArgs config values
	 * @return this */
	public DocumentedBlockItem tooltipArgs(ConfigValue<?>... tooltipArgs) {
		return tooltipArgs(new ConfigArgs(tooltipArgs));
	}

	@Override
	public ITextComponent getName(ItemStack stack) {
		return cTranslate(getDescriptionId(stack));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		DocumentedItem.addInformation(getDescriptionId(), tooltipArgs, tooltip);
		super.appendHoverText(stack, world, tooltip, flag);
	}

}
