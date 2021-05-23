package cd4017be.lib.block;

import cd4017be.lib.property.PropertyOrientation;
import cd4017be.lib.util.Orientation;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.*;

/**
 * 
 * @author CD4017BE
 */
public class OrientedBlock<T extends TileEntity> extends BlockTE<T> {

	private static PropertyOrientation TEMP;
	public final PropertyOrientation orientProp;
	VoxelShape[] shapes;

	/**
	 * @param prop orientation type
	 */
	public OrientedBlock(Properties p, int flags, PropertyOrientation prop) {
		super(setTemp(p, prop), flags);
		this.orientProp = prop;
	}

	private static Properties setTemp(Properties p, PropertyOrientation prop) {
		TEMP = prop;
		return p;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TEMP);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return defaultBlockState().setValue(orientProp, orientProp.getPlacementState(context));
	}

	@Override
	public VoxelShape
	getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return shapes != null ? shapes[state.getValue(orientProp).ordinal()]
			: super.getShape(state, world, pos, context);
	}

	public OrientedBlock<T> setShape(VoxelShape main) {
		shapes = new VoxelShape[16];
		for (Orientation o : orientProp.getPossibleValues())
			shapes[o.ordinal()] = o.apply(main);
		return this;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		if (rot == Rotation.NONE) return state;
		int i = state.getValue(orientProp).ordinal();
		Orientation o = Orientation.values()[i & 12 | rot.rotate(i & 3, 4)];
		return orientProp.getPossibleValues().contains(o) ? state.setValue(orientProp, o) : state;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE) return state;
		int i = state.getValue(orientProp).ordinal();
		Orientation o = Orientation.values()[i & 12 | mirror.mirror(i & 3, 4)];
		return orientProp.getPossibleValues().contains(o) ? state.setValue(orientProp, o) : state;
	}

}
