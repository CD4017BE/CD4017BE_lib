package cd4017be.lib.render.model;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.data.IModelData;

public interface IModelDataItem {
	IModelData getModelData(ItemStack stack, ClientWorld world, LivingEntity livingEntity);
}