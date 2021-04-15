package cd4017be.api.recipes;

import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.script.Parameters;

/**
 * 
 * @author CD4017BE
 */
public class OreGenHandler implements IRecipeHandler {

	//ArrayList<OreGen> generators;

	public OreGenHandler() {
		//generators = new ArrayList<OreGen>();
		//GameRegistry.registerWorldGenerator(this, 0);
	}
/*
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		BlockPos pos = new BlockPos(chunkX << 4, 0, chunkZ << 4);
		int dim = world.provider.getDimension();
		for (OreGen gen : generators)
			if ((gen.dim == null || gen.dim.test(dim)) && TerrainGen.generateOre(world, random, gen, pos, EventType.CUSTOM))
				gen.generate(world, random, pos);
	}*/

	@Override
	public void addRecipe(Parameters p) {
		/*
		ItemStack is = p.get(2, ItemStack.class);
		Item i = is.getItem();
		if (!(i instanceof ItemBlock)) throw new IllegalArgumentException("supplied item has no registered block equivalent");
		double[] vec = p.getVector(4);
		if (vec.length != 3) throw new IllegalArgumentException("height parameter must have 3 elements");
		BlockState out = ((ItemBlock)i).getBlock().getStateFromMeta(i.getMetadata(is.getMetadata()));
		Block in = Block.getBlockFromName(p.getString(1));
		if (in == null) throw new IllegalArgumentException("block type does not exists");
		double dim = p.has(5) ? p.getNumber(5) : Double.NaN;
		generators.add(new OreGen(out, is.getCount(), (int)p.getNumber(3), (int)vec[0], (int)vec[1], (int)vec[2], BlockMatcher.forBlock(in), Double.isNaN(dim) ? null : (d)-> d == (int)dim));
		*/
	}
/*
	class OreGen extends WorldGenMinable {
		final int numV, hgt;
		final float min, max;
		final IntPredicate dim;
		
		public OreGen(IBlockState state, int numB, int numV, int minH, int mainH, int maxH, Predicate<IBlockState> target, IntPredicate dim) {
			super(state, numB, target);
			this.numV = numV;
			this.min = mainH - minH;
			this.max = maxH - mainH;
			this.hgt = mainH;
			this.dim = dim;
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
	}*/

}
