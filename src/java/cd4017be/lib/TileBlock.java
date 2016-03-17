/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.TileBlockRegistry.TileBlockEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 * @author CD4017BE
 */
public class TileBlock extends DefaultBlock implements ITileEntityProvider
{
    public static final Orientation[] Orientations = new Orientation[4];
    static {
    	for(byte i = 0; i < Orientations.length - 1; i++) Orientations[i + 1] = new Orientation(i);
    }
	public static class Orientation implements IProperty<Integer>{
		public static final String[] namesAll = {"BN", "BS", "BW", "BE", "N", "S", "W", "E", "TN", "TS", "TW", "TE"};
		public static final String[] namesHor = {"--", "--", "--", "--", "N", "S", "W", "E", "--", "--", "--", "--"};
		public static final String[] namesVert = {"--", "--", "B", "T", "N", "S", "W", "E", "--", "--", "--", "--"};
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
	}
	
	protected static int tmpType;
	/**
     * @param type 0xf = {0 = none; 1 = NSWE; 2 = BTNSWE; 3 = NSWE + BT rotated}; 
     * 0x10 = redstoneOut 
     * 0x20 = nonOpaque
     * 0x40 = differentDrops
     */
	public static TileBlock create(String id, Material m, Class<? extends ItemBlock> item, int type) {
		tmpType = (type & 15) % Orientations.length;
		TileBlock block = new TileBlock(id, m, item, type);
		tmpType = 0;
		return block;
	}
	
    /**
     * @param type 0xf = {0 = none; 1 = NSWE; 2 = BTNSWE; 3 = NSWE + BT rotated}; 
     * 0x10 = redstoneOut 
     * 0x20 = nonOpaque
     * 0x40 = differentDrops
     */
    protected TileBlock(String id, Material m, Class<? extends ItemBlock> item, int type)
    {
        super(id, m, item);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.orient = Orientations[tmpType];
        this.redstone = (type & 16) != 0;
        this.opaque = (type & 32) == 0;
        this.drop = (type & 64) == 0;
        this.renderType = 3;
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

	protected void addProperties(ArrayList<IProperty> main)
    {
		if (tmpType > 0) main.add(Orientations[tmpType]);
    }
	
	@Override
	protected BlockState createBlockState() {
		ArrayList<IProperty> main = new ArrayList<IProperty>();
		this.addProperties(main);
		return new BlockState(this, main.toArray(new IProperty[main.size()]));
	}

	@Override
    public TileEntity createNewTileEntity(World world, int id) 
    {
        TileBlockEntry entry = TileBlockRegistry.getBlockEntry(this);
        if (entry != null && entry.tileEntity != null) {
            try {
                return entry.tileEntity.newInstance();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
                return null;
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
                return null;
            }
        } else return null;
    }
    
    public int machineId;
    public boolean registered;
    public final Orientation orient;
    private boolean redstone;
    private boolean opaque;
    private int renderType;
    private boolean drop;

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing s, float X, float Y, float Z) {
		TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).onActivated(player, s, X, Y, Z);
        else return false;
	}

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) 
    {
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onClicked(player);
    }

    @Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block b) {
    	TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onNeighborBlockChange(b);
	}

    @Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
    	TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).breakBlock();
        super.breakBlock(world, pos, state);
	}

	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos npos) {
		TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onNeighborTileChange(npos);
	}

	@Override
    public boolean canProvidePower() 
    {
        return redstone;
    }
    
    @Override
	public boolean isNormalCube() {
		if (opaque) return super.isNormalCube();
		else if (!super.isNormalCube()) return false;
		else return this.maxX == 1 && this.minX == 0 && this.maxY == 1 && this.minY == 0 && this.maxZ == 1 && this.minZ == 0;
	}

	@Override
	public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
		return super.isNormalCube(world, pos);
	}

    @Override
	public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing s, float X, float Y, float Z, int m, EntityLivingBase placer) {
    	if (orient == null) return this.getStateFromMeta(m);
    	if (placer.isSneaking()) {
    		if (orient.type == 0 && (s == EnumFacing.DOWN || s == EnumFacing.UP)) return this.blockState.getBaseState().withProperty(orient, Z + X > 1F ? (Z > X ? 5 : 7) : (Z < X ? 4 : 6));
        	if (orient.type >= 2 && s == EnumFacing.DOWN) return this.blockState.getBaseState().withProperty(orient, Z + X > 1F ? (Z > X ? 1 : 3) : (Z < X ? 0 : 2));
        	if (orient.type >= 2 && s == EnumFacing.UP) return this.blockState.getBaseState().withProperty(orient, Z + X > 1F ? (Z > X ? 9 : 11) : (Z < X ? 8 : 10));
        	return this.blockState.getBaseState().withProperty(orient, (s.getIndex()^1) + 2);
    	}
    	int h = placer.rotationPitch > 40 ? 1 : placer.rotationPitch < -35 ? -1 : 0;
    	if (orient.type == 1 && h != 0) return this.blockState.getBaseState().withProperty(orient, h < 0 ? 2 : 3);
    	int d = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
    	if (d > 0) d = (d + 1) % 3 + 1;
    	if (orient.type < 2 || h == 0) return this.blockState.getBaseState().withProperty(orient, 4 + d);
    	if (h < 0) return this.blockState.getBaseState().withProperty(orient, d);
    	return this.blockState.getBaseState().withProperty(orient, 8 + d);
	}

	@Override
	public int getWeakPower(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing s) {
		if (!redstone) return 0;
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).redstoneLevel(s.getIndex() ^ 1, false);
        else return 0;
	}

	@Override
	public int getStrongPower(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing s) {
		if (!redstone) return 0;
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).redstoneLevel(s.getIndex() ^ 1, true);
        else return 0;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack item) {
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onPlaced(entity, item);
	}

	@Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, Entity entity) 
    {
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onEntityCollided(entity);
    }

    @Override
    public boolean isOpaqueCube() 
    {
        return opaque;
    }

    @Override
    public boolean canBeReplacedByLeaves(IBlockAccess world, BlockPos pos) 
    {
        return false;
    }
    
    public void setRenderType(int t)
    {
        renderType = t;
    }
    
    @Override
    public int getRenderType() 
    {
        return renderType;
    }

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
		if (drop) super.harvestBlock(world, player, pos, state, te);
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (!drop) super.harvestBlock(world, player, pos, state, null);
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		if (drop) return super.getDrops(world, pos, state, fortune);
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).dropItem(state, fortune);
        else return super.getDrops(world, pos, state, fortune);
	}
	
	@SideOnly(Side.CLIENT)
	private EnumWorldBlockLayer blockLayer = EnumWorldBlockLayer.SOLID;
	
	@SideOnly(Side.CLIENT)
	public void setBlockLayer(EnumWorldBlockLayer layer) {
		this.blockLayer = layer;
	}
	
    @SideOnly(Side.CLIENT)
    public EnumWorldBlockLayer getBlockLayer() {
        return this.blockLayer;
    }
    
}
