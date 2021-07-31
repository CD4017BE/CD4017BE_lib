package cd4017be.lib.part;

import static cd4017be.lib.Content.microblock;
import cd4017be.api.grid.*;
import cd4017be.lib.network.Sync;
import cd4017be.lib.render.MicroBlockFace;
import cd4017be.lib.render.model.JitBakedModel;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**@author CD4017BE */
public class MicroBlock extends GridPart {

	public BlockState block;
	private CompoundTag tag;

	public MicroBlock() {
		super(0);
	}

	public MicroBlock(BlockState block, CompoundTag tag, long bounds) {
		this();
		this.block = block;
		this.tag = tag;
		this.bounds = bounds;
	}

	@Override
	public void storeState(CompoundTag nbt, int mode) {
		super.storeState(nbt, mode);
		nbt.putLong("m", bounds);
		nbt.putInt("s", Block.getId(block));
		if ((mode & Sync.SAVE) != 0)
			nbt.put("t", tag);
	}

	@Override
	public void loadState(CompoundTag nbt, int mode) {
		super.loadState(nbt, mode);
		bounds = nbt.getLong("m");
		block = Block.stateById(nbt.getInt("s"));
		tag = nbt.getCompound("t");
	}

	@Override
	public Object getHandler(int port) {return null;}

	@Override
	public void setHandler(int port, Object handler) {}

	@Override
	public boolean isMaster(int channel) {
		return false;
	}

	@Override
	public Item item() {
		return microblock;
	}

	@Override
	public ItemStack asItemStack() {
		ItemStack stack = super.asItemStack();
		stack.setTag(tag);
		stack.setCount(Long.bitCount(bounds));
		return stack;
	}

	@Override
	public byte getLayer() {
		return L_OUTER;
	}

	@Override
	public InteractionResult
	onInteract(Player player, InteractionHand hand, BlockHitResult hit, int pos) {
		if (hand != null) return InteractionResult.PASS;
		if (!player.level.isClientSide && player.getMainHandItem().getItem() instanceof IGridItem) {
			long b = bounds & ~(1L << pos);
			if (b == 0) {
				IGridHost host = this.host;
				host.removePart(this);
				host.removeIfEmpty();
			} else setBounds(b);
			if (!player.isCreative()) {
				ItemStack stack = new ItemStack(item());
				stack.setTag(tag);
				ItemFluidUtil.dropStack(stack, player);
			}
		}
		return InteractionResult.CONSUME;
	}

	public boolean addVoxel(int pos) {
		if (host.getPart(pos, L_OUTER) != null) return false;
		setBounds(bounds | 1L << pos);
		return true;
	}

	@Override
	public int analogOutput(Direction side) {
		return block.getSignal(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, side);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void fillModel(JitBakedModel model, long opaque) {
		MicroBlockFace.drawVoxels(model, block, bounds, opaque);
	}

	@Override
	public boolean merge(IGridHost host) {
		MicroBlock part = (MicroBlock)host.findPart(
			p -> p instanceof MicroBlock && ((MicroBlock)p).block == block
		);
		if (part == null) return true;
		part.bounds |= bounds;
		return false;
	}

	@Override
	public boolean canMove(Direction d, int n) {
		return true;
	}

	@Override
	protected GridPart copy(long bounds) {
		return new MicroBlock(block, tag, bounds);
	}

	@Override
	public boolean canRotate() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void rotate(int steps) {
		block = block.rotate(Rotation.values()[steps]);
		super.rotate(steps);
	}

}
