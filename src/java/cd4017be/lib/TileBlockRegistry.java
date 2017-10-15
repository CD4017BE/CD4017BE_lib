/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;

import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.TileBlock;
import cd4017be.lib.tileentity.ModTileEntity;
import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

/**
 *
 * @author CD4017BE
 */
@Deprecated
public class TileBlockRegistry 
{
	
	public static class TileBlockEntry {
		public final TileBlock block;
		public Class<? extends ModTileEntity> tileEntity;
		public Class<? extends Container> container;
		@SideOnly(Side.CLIENT)
		public Class<? extends GuiContainer> gui;
		
		public TileBlockEntry(TileBlock block)
		{
			this.block = block;
		}
	}
	
	private static HashMap<Block, TileBlockEntry> registry = new HashMap<Block, TileBlockEntry>();
	
	@SideOnly(Side.CLIENT)
	/**
	 * Registers given GuiContainer for given Block
	 * @param id
	 * @param gui
	 */
	public static void registerGui(Block id, Class<? extends GuiContainer> gui)
	{
		TileBlockEntry entry = registry.get(id);
		if (entry != null) {
			if (entry.gui != null) FMLLog.warning("CD4017BE-modlib: GuiContainer %1$s overrrides already registered GuiContainer %2$s for Block-ID %3$d !", gui.getName(), entry.gui.getName(), id);
			entry.gui = gui;
		} else FMLLog.warning("CD4017BE-modlib: Failed to register GuiContainer %1$s because Block-ID %2$s is not registered!", gui.getName(), id);
	}
	
	/**
	 * Registers a Block with given TileEntity, Container and display name.
	 * @param block
	 * @param tileEntity
	 * @param container
	 */
	public static void register(TileBlock block, Class<? extends ModTileEntity> tileEntity, Class<? extends Container> container)
	{
		if (container != null && !IGuiData.class.isAssignableFrom(tileEntity)) throw new IllegalArgumentException(String.format("%s doesn't implement %s !", tileEntity.getName(), IGuiData.class.getName()));
		TileBlockEntry entry = new TileBlockEntry(block);
		entry.tileEntity = tileEntity;
		block.tileEntity = tileEntity;
		GameRegistry.registerTileEntity(tileEntity, block.getUnlocalizedName());
		entry.container = container;
		registry.put(block, entry);
	}
	
	public static TileBlockEntry getBlockEntry(Block b)
	{
		return registry.get(b);
	}
	
}
