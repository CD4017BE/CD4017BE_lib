## Script wrapper modules for recipe APIs from different mods
Syntax `moduleName.methodName(<parType> parName, ...);`
where:
- `<item>` is an ItemStack (created using `it(...)` or `nil` for empty stack)
- `<fluid>` is a FluidStack (created using `fl(...)`)
- `<oreId>` is a String that defines an OreDictionary entry
- `<num>` is a Number
- `[...]` is an Array
- `<a|b>` may be either `a` or `b`

### Immersive Engineering
Module name: `IE`

Methods for adding recipes:
- Coke Oven `addCoke(<item> out, <item|oreId> in, <num> time, <num> creosoteOut)`
- Blast Furnace `addBlast(<item> out, <item|oreId> in, <num> time, <item> slagOut)`
- Alloy Smelter `addAlloy(<item> out, <item|oreId> inA, <item|oreId> inB, <num> time)`
- Arc Furnace `addArc(<item> mainOut, <item|oreId> mainIn, <item> slagOut, <num> time, <num> power, [<item|oreId>... addIn])`
- Crusher `addCrush(<item> out, <item|oreId> in, <num> energy)`
- Metal Press `addPress(<item> out, <item|oreId> in, <item> mold, <num> energy)`
- Bottling Machine `addBottle(<item> out, <item|oreId> in, <fluid> fluidIn)`
- Fermenter `addFerment(<fluid> out, <item> itemOut, <item|oreId> in, <num> energy)`
- Squeezer `addSqueeze(<fluid> out, <item> itemOut, <item|oreId> in, <num> energy)`
- Refinery `addRefine(<fluid> out, <fluid> inA, <fluid> inB, <num> energy)`
- Mixer `addMix(<fluid> out, <fluid> in, [<item|oreId>... itemsIn], <num> energy)`

Methods for removing all recipes that produce a given output:
- Coke Oven `remCoke(<item> out)`
- Blast Furnace `remBlast(<item> out)`
- Alloy Smelter `remAlloy(<item> out)`
- Arc Furnace `remArc(<item> mainOut)`
- Crusher `remCrush(<item> out)`
- Metal Press `remPress(<item> out)`
- Bottling Machine `remBottle(<item> out)`

Parameters:
- `time` = processing time in ticks
- `power` = consumed power in RF/tick
- `energy` = consumed energy in RF
- `creosoteOut` = amount of produced creosote oil in mB