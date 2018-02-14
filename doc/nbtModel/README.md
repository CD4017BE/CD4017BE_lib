Due to the significant limitations of basic JSON-models, this mod contains its own special model loader which uses the NBT-format instead and brings some enhancements:

- multiple models can be stored in one file (sharing their texture locations)
- models contain raw vertex data, so they are not limited to just cuboid shapes
- models can be parameterized which makes them highly flexible and reusable:
- selected vertices can be moved and/or scaled.
- selected uv coordinates can be moved and/or scaled
- the indices of applied textures can be rearranged.
- the color of the model can be defined by a parameter.

Detailed information about the NBT data structure used to store the models is given in `modelFormat.txt`

## Using NBT-Models for blocks and items
This mod will automatically try to load NBT-models instead of JSON-models for registered mod domains (= my mods) if the block or item model name starts with a `.`.
An optional name after a second `.` specifies which sub model is used. Optional parameters are separated by `,` and enclosed in `()`. For blocks a `#<orient>` suffix also rotates the model to the specified orientation.

Example1: `modid:block/.myblock.somepart(0xff80ff,2.5,8)#bn` would load the sub model `somepart` from `assets/modid/models/block/myblock.nbt` with the parameters `0xff80ff` (probably a color), `2.5` (maybe some scale factor) and `8` (perhaps texture index offset) and it would be rotated front face (north) down.

Example2: `modid:item/.myitem` would load the sub model `model` (default) from `assets/modid/models/item/myblock.nbt` with no parameters and no rotation.

## Creating models with Blender
Models can be directly exported from **Blender** when installing the python script `NBTModelScript.py` as blender module.

Each mesh object in the scene is stored as sub model in a compound tag with the same name the object had in blender. When only having one object, it's recommended to name it `model` because that's used as default when the sub model is unspecified.
The following object/mesh data will be included in the model:

- The mesh vertices with coordinates relative to the object's origin.
- The uv-mapping, where the used textures are converted in to resource location strings like so: `.../assets/<modid>/textures/<filepath>.png` -> `<modid>:<filepath>`.
- If a mesh has vertex colors applied, these are by default exported with only one color per face. If all vertices of each face should have individual colors, the vertex color set must be named `all`. *However for unknown reasons the minecraft item renderer would still render all vertices on a quad in the same color.*
- Cullfaces are automatically applied for faces where all vertices lay outside of a slightly shrinked 1m³ cube.