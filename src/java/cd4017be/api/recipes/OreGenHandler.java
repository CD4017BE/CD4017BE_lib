package cd4017be.api.recipes;

import java.util.ArrayList;
import java.util.Random;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
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
import cd4017be.lib.script.Parameters;

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
	public void addRecipe(Parameters p) {
		ItemStack is = p.get(2, ItemStack.class);
		Item i = is.getItem();
		if (!(i instanceof ItemBlock)) throw new IllegalArgumentException("supplied item has no registered block equivalent");
		double[] vec = p.getVector(4);
		if (vec.length != 3) throw new IllegalArgumentException("height parameter must have 3 elements");
		IBlockState out = ((ItemBlock)i).block.getStateFromMeta(i.getMetadata(is.getMetadata()));
		Block in = Block.getBlockFromName(p.getString(1));
		if (in == null) throw new IllegalArgumentException("block type does not exists");
		generators.add(new OreGen(out, is.getCount(), (int)p.getNumber(3), (int)vec[0], (int)vec[1], (int)vec[2], BlockMatcher.forBlock(in)));
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
