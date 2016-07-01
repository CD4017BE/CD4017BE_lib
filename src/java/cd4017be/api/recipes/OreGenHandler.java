package cd4017be.api.recipes;

import java.util.ArrayList;
import java.util.Random;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.util.VecN;

public class OreGenHandler implements IRecipeHandler, IWorldGenerator{
	
	ArrayList<OreGen> generators;
	
	public OreGenHandler() {
		generators = new ArrayList<OreGen>();
		GameRegistry.registerWorldGenerator(this, 0);
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		for (OreGen gen : generators) 
			gen.generate(world, random, new BlockPos(chunkX << 4, 0, chunkZ << 4));
	}

	@Override
	public boolean addRecipe(Object... param) {
		if (param.length < 5 || !(param[1] instanceof String && param[2] instanceof ItemStack && param[3] instanceof Double && param[4] instanceof VecN)) return false;
		ItemStack is = (ItemStack)param[2];
		Item i = is.getItem();
		VecN vec = (VecN)param[4];
		if (!(i instanceof ItemBlock && vec.x.length >= 3)) return false;
		IBlockState out = ((ItemBlock)i).block.getStateFromMeta(i.getMetadata(is.getMetadata()));
		Block in = Block.getBlockFromName((String)param[1]);
		if (in == null) in = Blocks.stone;
		generators.add(new OreGen(out, is.stackSize, ((Double)param[3]).intValue(), (int)vec.x[0], (int)vec.x[1], (int)vec.x[2], BlockMatcher.forBlock(in)));
		return true;
	}

	class OreGen extends WorldGenMinable{
		final int numV, hgt;
		final float min, max;
		
		public OreGen(IBlockState state, int numB, int numV, int minH, int mainH, int maxH, Predicate<IBlockState> target) {
			super(state, numB, target);
			this.numV = numV;
			this.min = mainH - minH;
			this.max = maxH - mainH;
			this.hgt = mainH;
		}

		@Override
		public boolean generate(World worldIn, Random rand, BlockPos position) {
			boolean side = max < min;
			int r;
			float f;
			for (int i = 0; i < numV; i++) {
				r = rand.nextInt();//split into: x[0...15], z[0...15], f[-4095...4095] more dense towards 0
				f = (float)((r & 0xfff) + (r >> 12 & 0xfff) - 4095) / 4095F;
				if (side) {
					f *= min;
					if (f > max) f = max - (f - max) / (min - max) * (min + max);
				} else {
					f *= max;
					if (f > min) f = min - (f - min) / (max - min) * (max + min);
					f = -f;
				}
				super.generate(worldIn, rand, position.add(r >> 24 & 0xf, (int)Math.floor(f) + hgt, r >> 28 & 0xf));
			}
			return true;
		}
	}
	
}
