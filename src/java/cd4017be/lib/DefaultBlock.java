/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

/**
 *
 * @author CD4017BE
 */
public class DefaultBlock extends Block
{
    protected IIcon[] addTextures = null;
    protected String[] addTexNames = null;
    
    public DefaultBlock(String id, Material m, Class<? extends ItemBlock> item, String... tex)
    {
        super(m);
        this.setCreativeTab(CreativeTabs.tabBlock);
        this.setBlockName(id);
        BlockItemRegistry.registerBlock(this, id, item);
        if (tex != null && tex.length > 0) {
            this.setBlockTextureName(BlockItemRegistry.currentMod.concat(":").concat(tex[0]));
            if (tex.length > 1) {
                this.addTexNames = new String[tex.length - 1];
                for (int i = 0; i < this.addTexNames.length; i++) this.addTexNames[i] = BlockItemRegistry.currentMod.concat(":").concat(tex[i + 1]);
            }
        } else {
            this.setBlockTextureName(BlockItemRegistry.currentMod.concat(":").concat(id));
        }
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
	public String getLocalizedName() 
	{
		return StatCollector.translateToLocal(this.getUnlocalizedName().replaceFirst("tile.", "tile.cd4017be.") + ".name");
	}
    
}
