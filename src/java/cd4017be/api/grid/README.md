# Microblock API
This api is used to implement small parts that share the same block location. And it allows them to interact with each other and with the surrounding world.

These parts must extend the `GridPart` class are stored and managed by a TileEntity that implements `IGridHost`.
That TileEntity can be accessed via `GridPart.host` if the part is added to a Block that is loaded and is on a server side world, otherwise the field is set to `null`. The state is changed by the Host TileEntity calling `GridPart.setHost( )` on its parts.

**CD4017BE_lib** provides an implementation of `IGridHost` with `cd4017be.lib.tileentity.Grid`.  
*Note that theoretically this API allows* `IGridHost` *to be implemented by something that is not a TileEntity (such as Entities or ItemStack Capabilities).*

## Registering Parts
GridParts don't use ForgeRegistry, instead they use Items that implement `IGridItem` to create and serialize instances (so they indirectly use the Item Registry). These items also typically handle the placement of their GridPart.
So to "register" a part, create and register an Item, let it implement `IGridItem` and return that item in `GridPart.item()`.

GridParts also define an ItemStack representation via `GridPart.asItemStack()` that is dropped by default when a part is removed and is used when disassembling or replicating Structures in a Microblock Workbench.

## Voxel Layers
The space inside a Microblock Structure containing one or more `GridPart`s is divided into a 4x4x4 voxel grid as well as two layers called "opaque" and "inner" layer. Each part may occupy one or more of the 4x4x4 voxels in any configuration (doesn't need to be a connected set of voxels) and may be part of the **inner layer**, the **opaque layer** or both layers. Parts are not allowed to have overlapping voxels with another part if both use a common layer.

Parts that use the **opaque layer** are considered to be solid and opaque so the voxels they occupy are used in determining occluded faces for rendering. Also if a player is interacting on a voxel that is occupied by two parts, the interaction is only passed to the part inside the **opaque layer**.

Part are in both layers by default unless they override their `GridPart.getLayer()` method. The return value of that method should be constant while the part is added to a structure (`GridPart.host != null`)!

The voxels occupied by a part are defined as long bitmap in `GridPart.bounds` (there are several static utility methods in `GridPart` to deal with these bitmaps). The GridHost will cache the combined bounds of all its parts (collision shape), so when changing the bounds of a part while it is loaded (`GridPart.host != null`), call `IGridHost.updateBounds()` afterwards to update the cache.

## Part Rendering
For static rendering, GridParts must override their `fillModel(JitBakedModel, long)` method and add their BakedQuads to the passed JitBakedModel. This method is called by the GridHost whenever it needs to refresh its block model. You can call `IGridHost.onPartChange()` on the server to cause part states be synchronized to the client and initiate a client chunk re-render with TileEntity model refresh.

For animations, frequently changing graphics or special render modes, there is also dynamic rendering that is only available to GridParts which implement `IDynamicPart`. The interface contains two methods to serialize and deserialize the part's fast changing state (call `IGridHost.updateDisplay()` when state changes server side) and one method to render the part like a TileEntityRenderer.

## Interacting with the world
GridParts can access the surrounding world via `IGridHost.world()` and `IGridHost.pos()`.

Parts can handle player interactions and hits by overriding `onInteract()`.
They can handle block updates by overriding `onBlockChange()` and/or `onTEChange()`.
To emit redstone signals, override `analogOutput()` and `connectRedstone()`
 and call `IGridHost.updateNeighbor()` to notify neighboring blocks when the signal changes.

*Note: The methods handling block updates and redstone are only called on parts whose bounds touch the corresponding adjacent block. Also everything only works server side except for player interactions.*

To access other parts in the same block, there is `IGridHost.findPart()`. While in theory you could check if adjacent TileEnties implement IGridHost and use `findPart()` to do all the interactions with other parts, this method should only be used for infrequent "maintenance operations". For communication, resource transfer and other frequent interactions between parts use the more efficient port system.

## The Port System
The port system automatically handles connections between GridParts on the same Block or on different Blocks and even to other TileEntity based machines that implement `IGridPortHolder`.

GridParts define their ports in `GridPart.ports[]`, where each short entry contains binary packed information about the position and type of the port (there are utility methods to create ports in a more human-readable way). The API currently supports 16 different port types:

- **ID: HandshakeType purpose**
- 0: `ISignalReceiver` 32-bit Redstone Signal
- 1: `IEnergyAccess` Energy Transfer (may also be used for `net.minecraftforge.energy.IEnergyStorage` or other electricity themed energy APIs)
- 2: `IInventoryAccess` Item Transport (may also be used for `net.minecraftforge.items.IItemHandler`)
- 3: `IFluidAccess` Fluid Transport (may also be used for `net.minecraftforge.fluids.capability.IFluidHandler`)
- 4: `IBlockSupplier` remote Block Interactions (may also be used for other types of P2P-tunnels)
- 5: `???` any type of pressurized gas (for example Mekanism Gases or PneumaticCraft compressed air)
- 6: `???` any type of mechanical power or force
- 7: `???` any type of optical transmission
- 8: `???` any type of magic power or particle beams
- 9-13: *unspecified*
- 14: `Object` arbitrary, potentially wired connection
- 15: `Object` arbitrary, direct only connection (no mod should ever add wires for this type)

ID 0-4 are used by **RedstoneControl 2** and ID 5-8 are use-case suggestions.
For your own HandshakeTypes that don't fit the given themes use ID 14 or 15 and distinguish types using `instanceof` checks.

During loading or when parts are added, all ports of matching descriptor are connected in pairs by exchanging a "handshake object". This handshake object is taken from the provider port's `getHandler(int port)` and passed to the master port's `setHandler(int port, Object handler)`. Whether a port is master or provider is defined via `isMaster(int port)` (`port` is the corresponding index in `GridPart.ports[]`).  
*Connection only works if one of the two ports is master and the other one is provider.*

Ports also connect between adjacent blocks if their positions touch and can even connect over longer distances via wires.
Wires are GridParts that define exactly two ports of same type and implement `IWire`.
*The API currently hides from parts, whether and what type of wires are involved in a given connection (this might change in the future).*

Parts should hold on to the handshake objects given to their master ports and use them for interaction until the ports are disconnected again: When parts get unloaded or removed (or parts are disconnected for other reasons), all connected master ports get `setHandler(int port, Object handler)` called with `handler = null`, telling them that the handshake object is no longer valid.  
*Note: Since the handler argument is of type java.lang.Object, it could be anything, so always instanceof-check if the handler is of the correct type and treat it like null if not!*