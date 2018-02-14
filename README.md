The CD4017BE_lib is a java library that is used by my Minecraft mods. It contains:
- basic shared code like Network Handlers and basic Block/Item types.
- code templates for specific objects and utility methods that are used very often in my mods.
- mod APIs used to let my mods interact with each other and with other mods.

Mods using it:
- Inductive Automation [GitHub](https://github.com/CD4017BE/InductiveAutomation) [Curse](https://minecraft.curseforge.com/projects/inductive-automation)
- Automated Redstone [GitHub](https://github.com/CD4017BE/AutomatedRedstone) [Curse](https://minecraft.curseforge.com/projects/automated-redstone)
- Inductive Logistics [GitHub](https://github.com/CD4017BE/InductiveLogistics) [Curse](https://minecraft.curseforge.com/projects/inductive-logistics)

Links:
- [CD4017BE_lib on Curse](http://minecraft.curseforge.com/projects/cd4017be-library)
- [Downloads](https://github.com/CD4017BE/CD4017BE_lib/releases)

## Project Setup for MC 1.11.2 and newer
For each minecraft version a separate Forge project is used which handles all the dependecies. This allows fast switching between minecraft versions by simple branch checkouts. Both this and the Forge projects must be located in a common parent directory.
- Setup a fresh forge gradle project for each minecraft version you want to use in a folder named `Forge-1.11.2`, `Forge-1.12.2`, etc.
- Clone this repository into a folder named `CD4017BE_lib` which is located in the same directory as the Forge folder.
- Checkout the respective branch for the minecraft version (`master-1.11.2`, `master-1.12.2`, etc.). 
- Copy the `build.gradle`, `gradle.properties` and `src/resources/cd4017be_lib_at.cfg` files over into the forge project and replace the existing ones.
- run `gradlew setupDecompWorkspace` in both this project and the Forge project

When using eclipse:
- run `gradlew eclipse` **only** in the Forge projects.
- import this project and all the Forge projects into your eclipse workspace
- for the Forge projects you then just have to `Configure Build Path` and under `Order and Export` select everything so that the main project has access to the minecraft code and all the dependencies
