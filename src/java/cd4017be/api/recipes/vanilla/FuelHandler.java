package cd4017be.api.recipes.vanilla;

import java.util.HashMap;

import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.script.Parameters;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles adding of furnace fuels
 * @author CD4017BE
 */
public class FuelHandler implements IRecipeHandler {

	private final HashMap<Integer, Integer> fuelList;

	public FuelHandler() {
		fuelList = new HashMap<Integer, Integer>();
		MinecraftForge.EVENT_BUS.register(this);
	}

	int key(ItemStack item) {
		return Item.getIdFromItem(item.getItem()) & 0xffff | (item.getDamage() & 0xffff) << 16;
	}

	@Override
	public void addRecipe(Parameters p) {
		fuelList.put(key(p.get(1, ItemStack.class)), (int)p.getNumber(2));
	}

	@SubscribeEvent
	public void getBurnTime(FurnaceFuelBurnTimeEvent ev) {
		Integer val = fuelList.get(key(ev.getItemStack()));
		if (val != null) ev.setBurnTime(val);
	}
}