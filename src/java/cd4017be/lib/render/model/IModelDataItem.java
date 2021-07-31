package cd4017be.lib.render.model;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;

public interface IModelDataItem {
	@OnlyIn(Dist.CLIENT)
	IModelData getModelData(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity livingEntity);
}