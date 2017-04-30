This mod contains a powerful config script. It is used by my mods to define things like crafting recipes in an easily editable and compact way *(JSON is not compact)*.


# Script Syntax
I created a user defined language package for syntax highlighting in Notepad++.
Therefore it's recommended to use Notepad++ to edit these script files and install the language package:

1.  in Notepad++ select: `Language` -> `define your language...`
2.  click `import`
3.  select the file `doc/recipeScript/recipeScript.xml` in this repository (needs to be downloaded)
4.  now if you open `doc/recipeScript/exampleScript.rcp` it should show syntax highlighting (otherwise you have to manually select `recipeScript` under `Language`)

The syntax is demonstrated in that example Script

# Variable Types
**Internal Types:**

*   Null Object: literal = `nil`
*   Boolean: literal = `false true`, comparators: `a<b a>b a<=b a>=b obj1==obj2 obj1~=obj2`, operators: `a&b(and) a|b(or) a^b(xor) a~&b(nand) a~|b(nor) a~^b(xnor)`, conditions: `if(b1){...}else if(b2)...else{...}`
*   Number: literals = {basic:`1234567890`, scientific:`-1.5e7`, fraction:`/64`, +/-infinity/NaN:`/0 -/0 NaN`}, operators = `a+b a*b a-b a/b a%b -a /a`, iteration from 0 to n-1 = `for(i : n){...}`
*   Vector: creation/concatenation = `[num1, num2, ..., vec1, ...]#`, size: `#vec`, element n = `vec:n`, operators = `a+b a*b a-b a/b -a /a n+a n-a n*a n/a`, iteration over elements = `for(num : vec){...}`
*   String: literal = `"some text"`, concatenation = `[string1, string2, ...]$`, substring = `string:[from, to]`, length = `#string`, formatting = `$"formatExpression" objectToFormat`, replacement = `repl(string, regexSearch, regexRepl)`
*   Array: creation = `[obj1, obj2, ...]`, size = `#arr`, element n = `arr:n`, iteration over elements = `for(obj : arr){...}`

Console output: `print(text)`

**Recipe Handler types:**

*   ItemStack: creation = `it(name) it(name, amount) it(name, amount, meta) it(itemStack, amount)`, check = `hasit(name)`
*   FluidStack: creation = `fl(name, amount)`, check = `hasfl(name)`
*   OreDictStack: creation = `ore(name) ore(name, amount)`, list items [array] = `ores(name)`
*   OreDictionary iterator: usage = `for(entryName : listore(regexFilter)){...}`
*   Recipe list iterator: usage = `for(recipe : list(handlerName, regexFilter)){...}`, recipe removal: `for(recipe : ...) {recipe = nil;}`
*   ItemStack matcher: usage = `something == isit(compareItem)`, a damage value of 32768 will ignore damage, a stack size <= 0 will ignore stack size, nbt is always ignored
*   FluidStack matcher: usage = `something == isfl(compareFluid)`, an amount <= 0 will ignore amount, nbt is always ignored
*   OreDictStack already has a built in matcher so you can just do `something == compareOre`

Checking if a mod is loaded: `hasmod(modName)`

# Recipe Handlers
The **add** function is the most important function as it is used for all recipe declarations (recipe has a bit wider meaning here). First argument defines which handler to address, the following arguments are then handler specific [types in brackets].

**Built in handlers:**

*   `add("item", [num]metaId, [string]name);` *see creating crafting materials*
*   `add("fluidCont", [Fluid]content, [Item]full, [Item]empty);` registers a pair of item stacks as fluid container (empty can be nil)
*   `add("worldgen", [string]targetBlock, [Item]oreBlock, [num]veinsPerChunk, [vec]spawnHeights);` adds oreBlock to world generation replacing targetBlock,  
 `oreBlock = it([string]name, [num]blocksPerVein)`, `spawnHeights = [min, best, max]#`
*   `add("ore", [string]name, [Item]items...);` register items in OreDictionary as name
*   `add("fuel", [Item]item, [num]burnTicks);` register item as furnace fuel
*   `add("smelt", [Item]ingred, [Item]result);` add a furnace smelting recipe
*   `add("shapeless", [Item]result, [Item]ingreds...);` add a shapeless crafting recipe
*   `add("shaped", [Item]result, [string]pattern, [Item]ingreds...);` add a shaped crafting recipe, `pattern = "iii/iii/iii"` with 'i' = ingredient index (0-8), '/' = grid line separator
*   `add("shapedNBT", [string]nbtPattern, [Item]result, [string]rcpPattern, [Item]ingreds...);` shaped crafting recipe with special NBT processing:  
 `nbtPattern = "+tagName1, #tagName2, >tagName3, <tagName4, ..."` with '+' = add values of numeric tag from all ingredients together, '<' = take from ingredient with smallest tag value, '>' = take largest value, '#' = copy tag from first ingredient that has it

**Inductive Automation handlers:**

*   `add("advFurn", [arr]ingreds, [arr]results, [num]energy);` add an Advanced Furnace recipe (ingredients and results may contain a fluid stack and up to 3 item stacks)
*   `add("compAs", [Item]result, [Item|Ore|nil]ingreds...x4);` add a Compression Assembler recipe (takes 4 ingredient arguments)
*   `add("electr", [Item|Fluid|Ore]ingred, [Item|Fluid]result...x2, [num]energy);` add an Electrolyser recipe
*   `add("heatRad", [Fluid]in, [Fluid]out);` add a Heat Radiator recipe
*   `add("cool", coolantIn, coolantOut, materialIn, materialOut, [num]energy);` add a Decompression Cooler recipe (arguments are [Fluid|Item])
*   `add("trash", [Item]result, [Item]ingred, [num]mass);` add a Gravitational Condenser recipe
*   `add("algae", [Item]item, [num]nutrient, [num]algae);` register item for fermenting in Bio Reactor

# Compiling / Execution
During startup it will automatically try to compile all files in the folder '<mcDir>/config/cd4017be/' that end with '.rcp' as config scripts if some were edited since last startup (checked via *last edited date*). If that is successful a file named 'compiledScripts.dat' is created and the scripts are run during the launch stages. It will also always recompile the scripts if this file is missing. The reasons for first compiling instead of direct interpreting is that its faster this way and that the old compiled version is used as backup in case a new compiling fails.

By default there is a script file for all installed mods using this feature. If a mod is updated and that update comes with a newer config version it will automatically overwrite the old script file (creating a backup before). It's possible to prevent that by significantly increasing the script's version number but that's not recommended because for example new features would be without recipe then.

Optionally you can create an additional script file named 'core.rcp' for your own purposes.

All global variable assignments outside of functions are already done during compile time. In case these assignments require actual code and not just directly assign values by literals, these have to be put within `{...}` brackets. You can even put complicated code in there but no function calls are allowed.
Scripts are later executed via the `PRE_INIT()`, `INIT()` and `POST_INIT()` functions that run during the different mod initialization phases. These functions can be defined in all script files that are registered by the individual mods (including core.cfg). Other arbitrary script files you might add won't be executed automatically, but you can call their functions from within another script:

	INIT() {
		...
		myAdditionalScript.someFunction(args...);
		...
	}

# Creating your own crafting materials
This mod adds one item that is used to represent all crafting materials my other mods could ever need, using its up to 32768 different damage-metadata states that a minecraft item can have.

The `add("item", id, "[unlocName]")` function allows you register some of these possible states with an unlocalized name for your own purposes. As `id` use some value > 1024 (to avoid collisions with existing items) and `[unlocName]` can be set to whatever name you want, it's also used for getting the item stacks with `it("m.[unlocName]")`.

This makes that item variant show up in this mod's Crafting Materials creative tab and allows using it for crafting recipes. However if you look at it ingame you will notice that it only has that black purple default texture and something like "item.cd4017be.m:[unlocName].name" as displayed name because all the assets are still missing. You can add them easily by creating a **Resource Pack** that contains the following files:

*   `pack.mcmeta` (every resource pack needs that, see example)
*   `assets/cd4017be_lib/lang/en_US.lang` where you put the item display names and optionally some tool tips by writing these lines for every item:  
 `item.cd4017be.m:[unlocName].name=Your Localized Display Name`  
 `item.cd4017be.m:[unlocName].tip=Optional SHIFT tool tip`  
 `item.cd4017be.m:[unlocName].tipA=Optional ALT tool tip`
*   `assets/cd4017be_lib/models/item/m/[unlocName].json` a JSON render model for every registered item.
*   `assets/cd4017be_lib/textures/...` to put your textures if you want to use your own instead of referring to already existing ones (from minecraft or other mods).

Also check out the ExampleResourcePack.

**Using script variables in tool tips:**
Simply put `\<varName>` into a tool tip entry and it will be replaced by the value of the global variable 'varName' in your script as string (with some fancy magnitude order formatting for numbers).
If that variable is an array you can also use `\<varName:index>` to get the array element at 'index'.  
*This feature is mainly used internally to make something like machine power consumption show accurate values in tool tips based on config settings*

Note: If your .lang file uses `#PARSE_ESCAPES` in its first line then you have to double the backslashes (doing something like `\\<varName>` instead).