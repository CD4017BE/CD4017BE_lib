package cd4017be.lib.render.model;

import javax.annotation.Nullable;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;

public interface IModelDataItem {
	@OnlyIn(Dist.CLIENT)
	IModelData getModelData(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity);
}