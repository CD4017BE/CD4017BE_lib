/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.TileBlockRegistry.TileBlockEntry;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CD4017BE
 */
public class TileBlock extends DefaultBlock implements ITileEntityProvider
{
    
    public static final int[] TypeTexAm = { 1, 2, 3, 4,
                                            2, 2, 3, 6,
                                            6, 6, 6, 6,
                                            6, 6, 6, 6};
    
    /**
     * @param type 0xf = {0 = standart; 1 = du,s; 2 = d,u,s; 3 = d,u,f,s placeHor; 4 = f,s placeHor; 5 = fb,s placeAlong; 6 = f,b,s placeAlong; 7 = allFaces}; 
     * 0x10 = redstoneOut 
     * 0x20 = nonOpaque
     * 0x40 = differentDrops
     */
    public TileBlock(String id, Material m, Class<? extends ItemBlock> item, int type, String... tex)
    {
        super(id, m, item, getTextureNames(id, type, tex));
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.type = (byte)(type & 15);
        this.redstone = (type & 16) != 0;
        this.opaque = (type & 32) == 0;
        this.drop = (type & 64) == 0;
        this.renderType = 0;
        
    }
    
    private static String[] getTextureNames(String id, int type, String[] tex)
    {
    	if (tex != null && tex.length > 0) return tex;
    	int n = TypeTexAm[type & 15];
    	tex = new String[n];    
    	for (int i = 0; i < n; i++)  tex[i] = id.concat(Integer.toHexString(i));
    	return tex;
    }
    
    public IIcon getIconN(int n)
    {
        if (addTextures == null || n <= 0 || n > addTextures.length) return this.blockIcon;
        else return addTextures[n - 1];
    }

    @Override
    public void registerBlockIcons(IIconRegister register) 
    {
        this.blockIcon = register.registerIcon(this.textureName);
        if (this.addTexNames != null) {
            this.addTextures = new IIcon[this.addTexNames.length];
            for (int i = 0; i < this.addTextures.length; i++)
            this.addTextures[i] = register.registerIcon(this.addTexNames[i]);
        }
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
    private byte type;
    private boolean redstone;
    private boolean opaque;
    private int renderType;
    private boolean drop;
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float X, float Y, float Z)
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).onActivated(player, s, X, Y, Z);
        else return false;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onClicked(player);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block b) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onNeighborBlockChange(b);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onNeighborTileChange(tileX, tileY, tileZ);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block b, int a) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).breakBlock();
        super.breakBlock(world, x, y, z, b, a);
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
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
    	return super.isNormalCube(world, x, y, z);
	}

	@Override
    public IIcon getIcon(int s, int m) 
    {
        return this.getIconN(this.getTextureIdx(s, m));
    }
    
    protected int getTextureIdx(int s, int m) 
    {
        if (type == 0)
        {
            return 0;
        } else
        if (type == 1)
        {
            return s < 2 ? 0 : 1;
        } else
        if (type == 2)
        {
            return s == 1 ? 0 : s == 0 ? 1 : 2;
        } else
        if (type == 3)
        {
            if (m == 0) m = 3;
        	return s == 1 ? 0 : s == 0 ? 1 : s == m ? 2 : 3;
        } else
        if (type == 4 || type == 5)
        {
            return s == m ? 0 : 1;
        } else
        if (type == 6)
        {
            return s == m ? 0 : (s ^ 1) == m ? 1 : 2;
        } else
        {
            return s;
        }
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int s) 
    {
        if (!redstone) return 0;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).redstoneLevel(s ^ 1, true);
        else return 0;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int s) 
    {
        if (!redstone) return 0;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).redstoneLevel(s ^ 1, false);
        else return 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack item) 
    {
    	if (type > 2 && type < 7 && !(entity.isSneaking() && type >= 5)) {
            int s = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            int m = 0;
            if (s == 0)
            {
                m = 2;
            } else
            if (s == 1)
            {
                m = 5;
            } else
            if (s == 2)
            {
                m = 3;
            } else
            if (s == 3)
            {
                m = 4;
            }
            if (type == 5 || type == 6)
            {
                if (entity.rotationPitch > 40) m = 1;
                else if (entity.rotationPitch < -35) m = 0;
            }
            world.setBlockMetadataWithNotify(x, y, z, m, 0x3);
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onPlaced(entity, item);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int s, float X, float Y, float Z, int m) 
    {
        return type == 5 || type == 6 ? s ^ 1 : m;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onEntityCollided(entity);
    }

    @Override
    public boolean isOpaqueCube() 
    {
        return opaque;
    }

    @Override
    public boolean canBeReplacedByLeaves(IBlockAccess world, int x, int y, int z) 
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
    public void onBlockHarvested(World par1World, int x, int y, int z, int par5, EntityPlayer par6EntityPlayer) 
    {
        if (!drop)super.harvestBlock(par1World, par6EntityPlayer, x, y, z, par5);
    }

    @Override
    public void harvestBlock(World par1World, EntityPlayer par2EntityPlayer, int par3, int par4, int par5, int par6) 
    {
        if (drop)super.harvestBlock(par1World, par2EntityPlayer, par3, par4, par5, par6);
    }
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) 
    {
        if (drop) return super.getDrops(world, x, y, z, metadata, fortune);
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null && te instanceof ModTileEntity) return ((ModTileEntity)te).dropItem(metadata, fortune);
        else return super.getDrops(world, x, y, z, metadata, fortune);
    }
    
}
