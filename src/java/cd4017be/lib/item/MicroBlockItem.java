package cd4017be.lib.item;

import static cd4017be.api.grid.GridPart.L_OUTER;
import static net.minecraft.world.phys.Vec3.upFromBottomCenterOf;

import java.util.ArrayList;

import cd4017be.api.grid.GridPart;
import cd4017be.api.grid.IGridHost;
import cd4017be.lib.part.MicroBlock;
import cd4017be.lib.render.MicroBlockFace;
import cd4017be.lib.render.model.*;
import cd4017be.lib.text.TooltipUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelDataMap;

/**@author CD4017BE */
public class MicroBlockItem extends GridItem implements IModelDataItem {

	public MicroBlockItem(Properties p) {
		super(p);
	}

	@Override
	public GridPart createPart() {
		return new MicroBlock();
	}

	private ItemStack of(Block block) {
		return wrap(new ItemStack(block), block.defaultBlockState(), 1);
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
		if (this.allowdedIn(group)) {
			items.add(of(Blocks.STONE));
			items.add(of(Blocks.OAK_PLANKS));
			items.add(of(Blocks.REDSTONE_BLOCK));
		}
	}

	@Override
	public Component getName(ItemStack stack) {
		ItemStack cont = ItemStack.of(stack.getOrCreateTag());
		return TooltipUtil.cFormat(getDescriptionId(), cont.getHoverName().getString());
	}

	@Override
	public InteractionResult onInteract(
		IGridHost grid, ItemStack stack, Player player,
		InteractionHand hand, BlockHitResult hit
	) {
		if (hand == null) return InteractionResult.PASS;
		int pos = IGridHost.target(hit, false);
		if (pos < 0 || grid.getPart(pos, L_OUTER) != null)
			pos = IGridHost.target(hit, true);
		if (pos < 0) return InteractionResult.PASS;
		if (player.level.isClientSide) return InteractionResult.CONSUME;
		
		CompoundTag tag = stack.getOrCreateTag();
		BlockState block = getBlock(new BlockPlaceContext(
			player, hand, ItemStack.of(tag), hit
		));
		if (block == null) return InteractionResult.FAIL;
		MicroBlock part = (MicroBlock)grid.findPart(
			p -> p instanceof MicroBlock && ((MicroBlock)p).block == block
		);
		if (!(
			part != null ? part.addVoxel(pos)
			: grid.addPart(new MicroBlock(block, tag, 1L << pos))
		)) return InteractionResult.FAIL;
		if (!player.isCreative()) stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	public ItemStack convert(ItemStack stack, Level world, BlockPos pos) {
		BlockState state = getBlock(new BlockPlaceContext(
			world, null, InteractionHand.MAIN_HAND, stack,
			new BlockHitResult(
				upFromBottomCenterOf(pos, 1),
				Direction.UP, pos, false
			)
		));
		return state != null ? wrap(stack, state, 64) : ItemStack.EMPTY;
	}

	public static BlockState getBlock(BlockPlaceContext context) {
		ItemStack stack = context.getItemInHand();
		if (!(stack.getItem() instanceof BlockItem)) return null;
		Block block = ((BlockItem)stack.getItem()).getBlock();
		BlockState state = block.getStateForPlacement(context);
		return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
			? state : null;
	}

	public ItemStack wrap(ItemStack stack, BlockState state, int n) {
		ItemStack ret = new ItemStack(this, n);
		CompoundTag nbt = stack.save(ret.getOrCreateTag());
		nbt.putByte("Count", (byte)1);
		nbt.putInt("state", Block.getId(state));
		return ret;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public ModelDataMap getModelData(ItemStack stack, ClientLevel world, LivingEntity entity) {
		BlockState state = Block.stateById(stack.getOrCreateTag().getInt("state"));
		if (state.getBlock() == Blocks.AIR) //so the item is not invisible in recipes
			state = Blocks.STONE.defaultBlockState();
		ModelDataMap data = TileEntityModel.MODEL_DATA_BUILDER.build();
		ArrayList<BakedQuad> quads = JitBakedModel.make(data).inner();
		float[] ofs = {.25F, .25F, .25F}, size = {.5F, .5F, .5F};
		for (MicroBlockFace f : MicroBlockFace.facesOf(state))
			if (f != null) quads.add(f.makeRect(ofs, size));
		return data;
	}

}
