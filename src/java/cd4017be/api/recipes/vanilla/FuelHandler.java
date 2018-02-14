package cd4017be.api.recipes.vanilla;

import java.util.HashMap;

import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.script.Parameters;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.IFuelHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Handles adding of furnace fuels
 * @author CD4017BE
 */
public class FuelHandler implements IRecipeHandler, IFuelHandler {

	private final HashMap<Integer, Integer> fuelList;

	public FuelHandler() {
		fuelList = new HashMap<Integer, Integer>();
		GameRegistry.registerFuelHandler(this);
	}

	int key(ItemStack item) {
		return Item.getIdFromItem(item.getItem()) & 0xffff | (item.getItemDamage() & 0xffff) << 16;
	}

	@Override
	public void addRecipe(Parameters p) {
		fuelList.put(key(p.get(1, ItemStack.class)), (int)p.getNumber(2));
	}

	@Override
	public int getBurnTime(ItemStack fuel) {
		Integer val = fuelList.get(key(fuel));
		return val == null ? 0 : val;
	}
}