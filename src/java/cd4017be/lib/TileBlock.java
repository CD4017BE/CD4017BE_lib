package cd4017be.lib;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 * @deprecated replaced by AdvancedBlock & OrientedBlock
 */
@Deprecated
public class TileBlock extends DefaultBlock implements ITileEntityProvider
{
	public static final Orientation[] Orientations = new Orientation[4];
	static {
		for(byte i = 0; i < Orientations.length - 1; i++) Orientations[i + 1] = new Orientation(i);
	}
	public static class Orientation implements IProperty<Integer>{
		public static final String[] namesAll = {"bn", "bs", "bw", "be", "n", "s", "w", "e", "tn", "ts", "tw", "te"};
		public static final String[] namesHor = {"--", "--", "--", "--", "n", "s", "w", "e", "--", "--", "--", "--"};
		public static final String[] namesVert = {"--", "--", "b", "t", "n", "s", "w", "e", "--", "--", "--", "--"};
		public final byte type;
		public final ArrayList<Integer> values;
		private Orientation(byte type) {
			this.type = type;
			int i = type == 0 ? 4 : type==1 ? 2 : 0, j = type==0 || type==1 ? 8 : 12;
			values = new ArrayList<Integer>(j - i);
			for (;i < j; i++) values.add(i);
		}
		@Override
		public String getName() {
			return "orient";
		}
		@Override
		public Collection<Integer> getAllowedValues() {
			return values;
		}
		@Override
		public Class<Integer> getValueClass() {
			return Integer.class;
		}
		@Override
		public String getName(Integer v) {
			return type == 0 ? namesHor[v] : type == 1 ? namesVert[v] : namesAll[v];
		}
		
		public boolean inRange(Integer i)
		{
			return type == 0 ? i >= 4 && i < 8 : type == 1 ? i >= 2 && i < 8 : i >= 0 && i < 8;
		}
		@Override
		public Optional<Integer> parseValue(String value) {
			String[] array = type == 0 ? namesHor : type == 1 ? namesVert : namesAll; 
			for (int i = 0; i < array.length; i++) 
				if (array[i].equals(value)) return Optional.of(i);
			return Optional.absent();
		}
	}
	
	protected static int tmpType;
	protected AxisAlignedBB[] boundingBox;
	/**
	 * @param type 0xf = {0 = none; 1 = NSWE; 2 = BTNSWE; 3 = NSWE + BT rotated}; 
	 * 0x10 = redstoneOut 
	 * 0x20 = nonOpaque
	 * 0x40 = differentDrops
	 */
	public static TileBlock create(String id, Material m, SoundType sound, int type) {
		tmpType = (type & 15) % Orientations.length;
		TileBlock block = new TileBlock(id, m, sound, type);
		tmpType = 0;
		return block;
	}
	
	/**
	 * @param type 0xf = {0 = none; 1 = NSWE; 2 = BTNSWE; 3 = NSWE + BT rotated}; 
	 * 0x10 = redstoneOut 
	 * 0x20 = nonOpaque
	 * 0x40 = differentDrops
	 * 0x80 = fullBlock
	 */
	protected TileBlock(String id, Material m, SoundType sound, int type)
	{
		super(id, m);
		this.setCreativeTab(CreativeTabs.DECORATIONS);
		this.setSoundType(sound);
		this.orient = Orientations[tmpType];
		this.redstone = (type & 16) != 0;
		this.opaque = (type & 32) == 0;
		this.drop = (type & 64) == 0;
		this.fullBlock = (type & 128) == 0;
		this.renderType = EnumBlockRenderType.MODEL;
		this.boundingBox = new AxisAlignedBB[]{FULL_BLOCK_AABB};
		if (orient != null) this.setDefaultState(this.blockState.getBaseState().withProperty(this.orient, orient.values.get(0)));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		if (orient != null && orient.inRange(meta))
			return this.blockState.getBaseState().withProperty(orient, meta);
		else return this.getDefaultState();
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return orient != null ? state.getValue(orient) : 0;
	}

	@SuppressWarnings("rawtypes")
	protected void addProperties(ArrayList<IProperty> main)
	{
		if (tmpType > 0) main.add(Orientations[tmpType]);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected BlockStateContainer createBlockState() {
		ArrayList<IProperty> main = new ArrayList<IProperty>();
		this.addProperties(main);
		return new BlockStateContainer(this, main.toArray(new IProperty[main.size()]));
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return tileEntity != null;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		if (tileEntity != null) {
			try {
				try {
					return tileEntity.getConstructor(IBlockState.class).newInstance(state);
				} catch (NoSuchMethodException e) {
					return tileEntity.newInstance();
				}
			} catch (InstantiationException ex) {
				ex.printStackTrace();
				return null;
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
				return null;
			} catch (InvocationTargetException ex) {
				ex.printStackTrace();
				return null;
			}
		} else return null;
	}
	
	public int machineId;
	public boolean registered;
	public final Orientation orient;
	public Class<? extends TileEntity> tileEntity;
	protected boolean redstone;
	protected boolean opaque;
	protected EnumBlockRenderType renderType;
	protected boolean drop;

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing s, float X, float Y, float Z) {
		ItemStack item = player.getHeldItem(hand);
		if (tileEntity == null) return false;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).onActivated(player, hand, item, s, X, Y, Z);
		return false;
	}

	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
		if (tileEntity == null) return;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onClicked(player);
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block b, BlockPos src) {
		if (tileEntity == null) return;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onNeighborBlockChange(b, src);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (tileEntity == null) return;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).breakBlock();
		super.breakBlock(world, pos, state);
	}

	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos npos) {
		if (tileEntity == null) return;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onNeighborTileChange(npos);
	}

	@Override
	public boolean canProvidePower(IBlockState state) 
	{
		return redstone;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		AxisAlignedBB box = getBoundingBox(state, world, pos);
		if (box == FULL_BLOCK_AABB) return true;
		switch (side) {
		case DOWN: return box.minY == FULL_BLOCK_AABB.minY;
		case UP: return box.maxY == FULL_BLOCK_AABB.maxY;
		case NORTH: return box.minZ == FULL_BLOCK_AABB.minZ;
		case SOUTH: return box.maxZ == FULL_BLOCK_AABB.maxZ;
		case WEST: return box.minX == FULL_BLOCK_AABB.minX;
		case EAST: return box.maxX == FULL_BLOCK_AABB.maxX;
		default: return true;
		}
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing s, float X, float Y, float Z, int m, EntityLivingBase placer, EnumHand hand) {
		if (orient == null) return this.getStateFromMeta(m);
		if (placer.isSneaking()) {
			if (orient.type == 0 && (s == EnumFacing.DOWN || s == EnumFacing.UP)) return this.blockState.getBaseState().withProperty(orient, Z + X > 1F ? (Z > X ? 5 : 7) : (Z < X ? 4 : 6));
			if (orient.type >= 2 && s == EnumFacing.DOWN) return this.blockState.getBaseState().withProperty(orient, Z + X > 1F ? (Z > X ? 1 : 3) : (Z < X ? 0 : 2));
			if (orient.type >= 2 && s == EnumFacing.UP) return this.blockState.getBaseState().withProperty(orient, Z + X > 1F ? (Z > X ? 9 : 11) : (Z < X ? 8 : 10));
			return this.blockState.getBaseState().withProperty(orient, (s.getIndex()^1) + 2);
		}
		int h = placer.rotationPitch > 40 ? 1 : placer.rotationPitch < -35 ? -1 : 0;
		if (orient.type == 1 && h != 0) return this.blockState.getBaseState().withProperty(orient, h < 0 ? 2 : 3);
		int d = MathHelper.floor((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		if (d > 0) d = (d + 1) % 3 + 1;
		if (orient.type < 2 || h == 0) return this.blockState.getBaseState().withProperty(orient, 4 + d);
		if (h < 0) return this.blockState.getBaseState().withProperty(orient, d);
		return this.blockState.getBaseState().withProperty(orient, 8 + d);
	}

	@Override
	public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return !redstone;
	}

	@Override
	public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing s) {
		if (!redstone || tileEntity == null) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).redstoneLevel(s.getIndex() ^ 1, false);
		else return 0;
	}

	@Override
	public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing s) {
		if (!redstone || tileEntity == null) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).redstoneLevel(s.getIndex() ^ 1, true);
		else return 0;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack item) {
		if (tileEntity == null) return;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onPlaced(entity, item);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
		if (tileEntity == null) return;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onEntityCollided(entity);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) 
	{
		return opaque;
	}

	@Override
	public boolean canBeReplacedByLeaves(IBlockState state, IBlockAccess world, BlockPos pos) 
	{
		return false;
	}
	
	public void setRenderType(EnumBlockRenderType t)
	{
		renderType = t;
	}
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) 
	{
		return renderType;
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
		if (drop) super.harvestBlock(world, player, pos, state, te, stack);
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (!drop) super.harvestBlock(world, player, pos, state, null, player.getHeldItemMainhand());
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		if (drop || tileEntity == null) return super.getDrops(world, pos, state, fortune);
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).dropItem(state, fortune);
		else return super.getDrops(world, pos, state, fortune);
	}
	
	private BlockRenderLayer blockLayer = BlockRenderLayer.SOLID;
	
	public void setBlockLayer(BlockRenderLayer layer) {
		this.blockLayer = layer;
	}
	
	public BlockRenderLayer getBlockLayer() {
		return this.blockLayer;
	}

	public TileBlock setBlockBounds(AxisAlignedBB box) {
		if (orient == null) boundingBox[0] = box;
		else
			switch(orient.type) {
			case 2: break;//TODO support advanced orient mode
			default: boundingBox = new AxisAlignedBB[]{
				null, null,
				new AxisAlignedBB(box.minX, box.minZ, 1.0 - box.maxY, box.maxX, box.maxZ, 1.0 - box.minY),
				new AxisAlignedBB(box.minX, 1.0 - box.maxZ, box.minY, box.maxX, 1.0 - box.minZ, box.maxY),
				box,
				new AxisAlignedBB(1.0 - box.maxX, box.minY, 1.0 - box.maxZ, 1.0 - box.minX, box.maxY, 1.0 - box.minZ),
				new AxisAlignedBB(box.minZ, box.minY, 1.0 - box.maxX, box.maxZ, box.maxY, 1.0 - box.minX),
				new AxisAlignedBB(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX)
			};
			}
		return this;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return orient == null || boundingBox.length == 1 ? boundingBox[0] : boundingBox[state.getValue(orient)];
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return createTileEntity(world, getStateFromMeta(meta));
	}

	
}
