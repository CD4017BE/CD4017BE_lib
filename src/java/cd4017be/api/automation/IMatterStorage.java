/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.api.automation;

import java.util.ArrayList;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public interface IMatterStorage 
{
    public ItemStack getFirstItem();
    public ArrayList<ItemStack> getAllItems();
    public ItemStack removeItems(int n);
    public ArrayList<ItemStack> addItems(ArrayList<ItemStack> items);
}
