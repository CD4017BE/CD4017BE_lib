The CD4017BE_lib is a Minecraft mod that serves as java library for my other mods. It contains:
- `cd4017be.lib.render.model`: utilities to simplify static TileEntity rendering (`TileEntityModel`) and an alternate model format `ScriptModel` using my own scripting language (`cd4017be.lib.script.*`) that is more convenient for complex item & block models.
- `cd4017be.lib.network`: network handlers and automatic synchronization of fields annotated with `@Sync` to world save, client or GUIs.
- `cd4017be.lib.gui.*`: a modular GUI system containing many often used elements such as buttons, text fields, progress bars, dynamic formatted text, etc. that use function objects to poll and modify values.
- `cd4017be.lib.tick.*`: arbitrary object ticking pipeline based on Forge ServerTickEvent.
- `cd4017be.lib.text.TooltipUtil`: String translation utilities that wrap around LanguageMap to add features like the in-game TooltipEditor.
- `cd4017be.lib.util.Utils`, `cd4017be.lib.util.ItemFluidUtil`: often used utility methods.
- `cd4017be.math.*`: linear algebra with float[] vectors, complex numbers and other math utilities.
- several template implementations for Block, Item, TileEntity, Container, GridPart, IItemHandler, IFluidHandler and more.
- `cd4017be.api.*`: mod APIs used to let my mods interact with each other and with other mods.
- `cd4017be.api.grid.*`: my own 4x4x4 sub-grid [Microblock API](src/java/cd4017be/api/grid/README.md) and a connection system for interaction between GridParts.

Despite named a Library, it also adds a few Blocks and Items to the game:
- **Creative Lab Item/Fluid/Energy Supply**: Spawns or consumes resources and offers counting statistics. Very useful for testing stuff, only obtainable in creative mode.
- **Microblock Structure**: The host block for my microblock API (implements IGridHost).
- **Block Bits**: Miniature version of blocks to build with inside Microblock Structures. *These have no special properties except for Redstone Block Bits emitting a redstone signal.*
- **Microblock Workbench**: Used to craft Block Bits and disassemble or replicate Microblock Structures.

*Note: This is as of MC 1.16.5, features greatly vary between releases for different Minecraft versions as new features get added over time and no longer functioning / obsolete content got removed during porting.*

**Mods using it in MC 1.16.5:**
- Redstone Control 2 [GitHub](https://github.com/CD4017BE/RedstoneControl2)

**Mods using older versions:**
- Redstone Control [GitHub](https://github.com/CD4017BE/RedstoneControl) [Curse Forge](https://www.curseforge.com/minecraft/mc-mods/redstone-control) *MC 1.11 - 1.12*
- Inductive Logistics [GitHub](https://github.com/CD4017BE/InductiveLogistics) [Curse Forge](https://minecraft.curseforge.com/projects/inductive-logistics) *MC 1.11 - 1.12*
- Vertically Stacked Dimensions [GitHub](https://github.com/CD4017BE/VerticallyStackedDimensions) [Curse Forge](https://www.curseforge.com/minecraft/mc-mods/vertically-stacked-dimensions) *MC 1.12*
- Automated Redstone [GitHub](https://github.com/CD4017BE/AutomatedRedstone) [Curse Forge](https://minecraft.curseforge.com/projects/automated-redstone) *MC 1.7 - 1.12*
- Inductive Automation [GitHub](https://github.com/CD4017BE/InductiveAutomation) [Curse Forge](https://minecraft.curseforge.com/projects/inductive-automation) *MC 1.7 - 1.10*
- Kinetic Engineering [GitHub](https://github.com/CD4017BE/ThermokineticEngineering) *MC 1.12*

## Server/Client installation
Simply put the downloaded `.jar` file of the mod in your `mods` folder. Requires Forge Modloader to be installed in your Minecraft instance.

Latest development downloads can be found in [packages](https://github.com/CD4017BE/CD4017BE_lib/packages/71046).
Stable, more thoroughly tested downloads available on [Curse Forge](https://www.curseforge.com/minecraft/mc-mods/cd4017be-library/files).

Mods depending on CD4017BE_lib may require specific versions.

**Versioning system:** CD4017BE_lib- `MinecraftVersion` - `MinecraftPort` . `ApiVersion` . `FeatureVersion` . `BuildNumber`
- `MinecraftVersion` & `MinecraftPort` change when porting to new MC versions, usually many features added and removed, no compatibility.
- `ApiVersion` increments if any API is modified in a non backwards compatible way.
- `FeatureVersion` increments if features are added or modified in a fully backwards compatible way.
- `BuildNumber` increments with each build, usually just bug fixes, fully interchangeable.

## Using this mod as dependency [1.16.5]
If you want to use the features of this library in your own mods (for example to integrate with my microblock system), add the following to your gradle build script:

```
repositories {
	maven {
		url = "https://maven.pkg.github.com/cd4017be/cd4017be_lib"
		credentials {
			username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
			password = project.findProperty("gpr.key") ?: System.getenv("PASSWORD")
		}
	}
}
dependencies {
	compile fg.deobf("com.cd4017be.lib:CD4017BE_lib:${mc_version}-${lib_version}")
}
```
You can define `mc_version=1.16.5` and `lib_version=...` in your `gradle.properties`.

Note: GitHub packages requires credentials to download files. You can set them either as environment variables for running gradle or supply them as project variable arguments to the gradle command: `gradlew -Pgpr.user=USER -Pgpr.key=KEY ...`  
`USER` is your Github account name and `KEY` should be an [access token](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token) created for that account with read access to GitHub packages.

## Project setup for MC 1.16.5
For tinkering with the library code itself.
- clone this repository
- checkout the master-1.16.5 branch (or any other branch for your minecraft version).
- follow the standard Forge Gradle mod project installations steps for your IDE (*Only tested for Eclipse so far*):

```
If you prefer to use Eclipse:
1. Run the following command: "gradlew genEclipseRuns" (./gradlew genEclipseRuns if you are on Mac/Linux)
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run "gradlew eclipse" to generate the project.
(Current Issue)
4. Open Project > Run/Debug Settings > Edit runClient and runServer > Environment
5. Edit MOD_CLASSES to show [modid]%%[Path]; 2 times rather then the generated 4.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: "gradlew genIntellijRuns" (./gradlew genIntellijRuns if you are on Mac/Linux)
4. Refresh the Gradle Project in IDEA if required.
```
