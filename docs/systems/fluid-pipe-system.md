# Fluid Pipe System (MK2)

This document is the architectural reference for the mod's MK2 fluid
network: how ducts find each other, how providers and receivers
balance per-tick transfers, and how the network bridges to vanilla
Forge `IFluidHandler` / Fabric `Storage<FluidVariant>` capabilities.

Looking for a behaviour checklist? See
[`docs/fluid-pipe-parity-test-matrix.md`](../fluid-pipe-parity-test-matrix.md).

> The system is a Java port of HBM 1.7.10's `FluidNetMK2` /
> `UniNodespace`. References to "1.7.10" in the code refer to the
> original implementation.

## 1. Layered model

```
Block layer        — FluidDuctBlock (multipart blockstate, shape, painting UI)
Block-entity layer — FluidDuctBlockEntity (per-block fluid type, ticking)
Topology layer     — UniNodespace + GenNode / FluidNode (per-position graph)
Network layer      — FluidNet (per Fluid, transfer scheduler)
Adapter layer      — ForgeFluidHandlerAdapter (bridges vanilla machines)
```

Key types under `com.hbm_m.api.fluids`:

| Type | Role |
| --- | --- |
| `IFluidConnectorMK2` | Base interface — "can this BE connect to that fluid on that side?" |
| `IFluidPipeMK2`      | Connector + node factory for a 6-direction pipe-shaped BE. |
| `IFluidProviderMK2`  | Source — `getFluidAvailable` / `useUpFluid` / pressure range. |
| `IFluidReceiverMK2`  | Sink — `getDemand` / `transferFluid` / pressure / `ConnectionPriority`. |
| `IFluidStandardTransceiverMK2` | Hybrid sink/source used by the Forge adapter. |
| `FluidNode`          | A node in `UniNodespace` for a given `(Fluid, BlockPos)`. |
| `FluidNet`           | A connected network of nodes for one `Fluid`. |
| `FluidNetProvider`   | Per-`Fluid` singleton used as the `NodeKey` discriminator. |

Topology lives under `com.hbm_m.api.network`:

| Type | Role |
| --- | --- |
| `UniNodespace`       | Per-dimension `Map<NodeKey, GenNode<?>>` and tick driver. |
| `GenNode<N>`         | Base node, owns a `NodeNet<?, ?, ?>` and its connection descriptors. |
| `NodeDirPos`         | `(BlockPos, Direction)` connection descriptor used for handshake. |
| `INetworkProvider`   | Factory that creates the matching `NodeNet`. |

## 2. Fluid identity (canonicalisation)

`FluidNetProvider.forFluid(fluid)` is intentionally lenient about what
counts as "the same network":

1. `FlowingFluid` instances collapse to their `getSource()`.
2. Any fluid that `VanillaFluidEquivalence.isWater(fluid)` returns
   `true` for collapses to vanilla `Fluids.WATER`. The same holds for
   `Fluids.LAVA`.
3. Otherwise the fluid is its own key.

This means a duct painted with the mod's water variant, the vanilla
flowing water and a third-party water-equivalent fluid all share one
network. `NodeKey` compares providers by **identity**, so canonicalisation
is enforced at the only entry point that creates providers.

To extend this for your own equivalent fluids, edit
`VanillaFluidEquivalence` rather than adding ad-hoc checks elsewhere.

## 3. The duct block entity

`FluidDuctBlockEntity` is the canonical implementation of
`IFluidPipeMK2`. Its responsibilities:

1. **Persist its fluid type.** `NBT_FLUID_TYPE = "FluidType"` stores
   the `ResourceLocation` of the painted fluid; `Fluids.EMPTY` means
   the duct is unpainted and inert.
2. **Manage one `FluidNode`.**
   - Created lazily in `onLoad()` (`ensureNode`) so it survives chunk
     loads.
   - Recreated when the painted fluid type changes
     (`setFluidType` → `rebuildNode`).
   - Destroyed in `setRemoved` and `onChunkUnloaded` to keep
     `UniNodespace` clean.
3. **Bridge vanilla neighbours each tick.** `tick()` iterates the six
   directions of the duct's connection state, and for each connected
   neighbour that is not itself a pipe or MK2 connector:
   - resolves an `IFluidHandler` (Forge) or `Storage<FluidVariant>`
     (Fabric) on the touching side;
   - lazily builds (and caches by `Direction`) a
     `ForgeFluidHandlerAdapter` that wraps the neighbour as an MK2
     `IFluidStandardTransceiverMK2`;
   - calls `adapter.trySubscribe(...)` and `adapter.tryProvide(...)` so
     the adapter participates in the network during the *same* tick.
4. **Sync to clients.** `getUpdatePacket()` and `getUpdateTag()` ship
   the fluid type so the multipart block model can pick the right
   overlay / tint colour. `load()` schedules a client-side mesh
   refresh via `DoorChunkInvalidationHelper.scheduleChunkInvalidation`.

The duct **does not expose `IFluidHandler` itself.** All transfer goes
through the MK2 net. This is intentional: a duct that pretended to be
an `IFluidHandler` would let neighbouring vanilla machines push into it
without the network ever seeing the fluid.

## 4. Node lifecycle

```
BE constructed
  └─ no node yet (fluidType may be loaded from NBT later)
BE.onLoad (server)
  └─ ensureNode → UniNodespace.createNode(level, node)
BE.tick (server)
  └─ ensureNode (in case the node was reaped)
  └─ adapter housekeeping for each side
setFluidType(newFluid)
  └─ rebuildNode:
     • destroyNode(old node)
     • if previous fluid != EMPTY → destroyNode(old NodeKey explicitly)
     • if newFluid != EMPTY     → createNode(new node)
BE.setRemoved / BE.onChunkUnloaded
  └─ UniNodespace.destroyNode(level, node)
```

`UniNodespace.NodeKey` is `(pos, provider)`. Because providers are
canonicalised per fluid, the same block position can host nodes for
several fluids — that is what makes painting a duct mid-network
"non-destructive" relative to other-typed networks.

## 5. The tick loop

The per-server-tick driver lives in `MainRegistry.onServerTick`:

```java
com.hbm_m.api.network.UniNodespace.updateNodespace(server);
```

`UniNodespace.updateNodespace` does, per dimension:

1. Walk every node. If it has no valid `net` or has been marked
   `recentlyChanged`, run `checkNodeConnection`:
   - For each of the node's six `NodeDirPos` connections, look up the
     neighbour node by `(pos, provider)` and validate the handshake
     with `checkConnection` (both sides must point at each other,
     directions must be opposite or both `null`).
   - Merge networks via `connectToNode` — the smaller net joins the
     larger one to keep `joinNetworks` cheap.
3. `updateNetworks()`:
   - reset each `NodeNet.fluidTracker`;
   - call `net.update()` (see below);
   - every 5 minutes (300 × 20 ticks) reap expired links and drop
     empty nets.

`FluidNet.update()` does the actual transfer:

1. **Setup providers.** Walk the providers map, drop entries whose
   `lastSeen` is older than `TIMEOUT = 3000 ms` (so adapters that stop
   ticking — e.g. their block entity unloaded — disappear within a few
   seconds). For each remaining provider, ask for
   `getFluidAvailable(fluid, pressure)` clipped by
   `getProviderSpeed(...)`, and bucket the entry by `pressure`.
2. **Setup receivers.** Same idea, but bucket by `[pressure][priority]`
   (3 levels each — `HIGH`, `NORMAL`, `LOW`).
3. **Transfer.** For each pressure level (only matching pressures can
   exchange fluid), distribute the available pool proportionally to
   demand, scanning priorities `HIGH → NORMAL → LOW`. Up to 100
   iterations re-distribute any leftover when a high-priority receiver
   maxes out — this mirrors the 1.7.10 behaviour and guarantees no
   single tick wastes fluid that could fit somewhere else.
4. **Infinite bypass.** If any node is simultaneously a provider and
   a receiver and declares itself
   `isInfiniteNetworkSource(fluid) == true` (or
   `isInfiniteNetworkSink`), the regular balancing is skipped and the
   whole network is filled / drained in one go. This is what makes the
   creative "infinite fluid" tank node work.
5. **Cleanup.** Reset per-tick scratch arrays.

The tracker on the right side of the duct overlay (`getFluidTracker()`
in `FluidDuctBlockEntity`) reads `FluidNet.fluidTracker` — i.e. the
total volume that flowed in the network during the previous tick.

## 6. Bridging to vanilla capabilities

Vanilla machines don't know about `IFluidUserMK2`. Bridging is the job
of `ForgeFluidHandlerAdapter` (despite the name it also handles Fabric,
gated by Stonecutter `//? if forge / fabric`):

- The adapter is owned by `FluidDuctBlockEntity.adapterCache` and keyed
  by `Direction`. Cached entries are dropped when the painted fluid
  changes (the adapter targets one `Fluid`).
- For multiblocks (`IMultiblockPart` with `PartRole.FLUID_CONNECTOR`)
  the adapter retargets the **controller** position with `side = null`,
  mirroring how the controller exposes its capability.
- `isLoaded()` returns false once the wrapped BE is removed, so the
  network reaps the entry within `TIMEOUT`.
- Pressure is fixed to `0` because `IFluidHandler` has no pressure
  concept. Machines that need non-zero pressure interaction must
  implement `IFluidConnectorMK2` (and the relevant provider / receiver
  interfaces) themselves and bypass the adapter — `FluidDuctBlockEntity`
  short-circuits in that case (`neighbor instanceof IFluidConnectorMK2`).

Net result: any vanilla / Forge / Fabric tank that exposes the standard
fluid capability becomes both a provider (drainable) and a receiver
(fillable) of the duct network, at pressure 0, with no extra effort
from the tank author.

## 7. Painting & propagation UX

`FluidDuctBlock` implements two ways to set the fluid of a duct from
the Fluid Identifier item:

| Interaction | Behaviour |
| --- | --- |
| Right-click on a duct | Calls `setFluidType` on that single duct — `rebuildNode` runs immediately. |
| Shift + right-click | `paintConnectedDuctNetwork` does a BFS over the connected ducts (same block type) up to `IDENTIFIER_NETWORK_LIMIT = 512` blocks, calling `setFluidTypeSilent` on each and finally syncing them in a batch. |

When the player breaks a duct with the identifier, the dropped item
inherits the duct's fluid type via `FluidDuctItem.setFluidType(stack,
ductBe.getFluidType())` so re-placement preserves the painting.

The "none" mod fluid (`ModFluids.NONE.getSource()`) is normalised to
`Fluids.EMPTY` in `normalizeDuctPaintFluid` — painting with it clears
the duct.

## 8. Configuration & toggles

There is no Cloth Config category dedicated to the fluid network — the
core simulation is always on. A few related options live in
`ModClothConfig` though:

- `enableDebugLogging` — turns on the `FluidDuctDBG` debug log
  (`[tick] pos=... fluidType=... connectedSides=... hasNode=...`) that
  `FluidDuctBlockEntity.tick()` prints every 2 seconds. Keep this off
  in production worlds.

## 9. Performance notes

- Building one node per duct is cheap, but every node is touched by
  `updateNodespace` once per server tick. Player-built networks bigger
  than a few thousand ducts will start eating tick budget; consider
  splitting via valves (which destroy and recreate the node on toggle).
- `ForgeFluidHandlerAdapter` instances are cached per side and live as
  long as the duct does. Avoid recreating them in hot paths if you
  add new neighbour-tracking code.
- The 100-iteration leftover loop inside `FluidNet.transferFluid` is
  O(N × 100) in the worst case but exits early once leftover is below
  1 mB. Networks with thousands of receivers should remain fine.

## 10. Adding new actors to the network

To make a new block entity participate without going through the Forge
adapter:

1. Implement `IFluidConnectorMK2` (and `IFluidProviderMK2` /
   `IFluidReceiverMK2` as needed) on the block entity.
2. Override `canConnect(Fluid, Direction)` to gate types and sides.
3. In `onLoad()` create a `FluidNode` (or implement `IFluidPipeMK2` and
   reuse the default 6-direction factory) and register it with
   `UniNodespace.createNode(...)`.
4. Destroy the node in `setRemoved()` and `onChunkUnloaded()` —
   forgetting this leaves orphan entries in `UniNodespace`.
5. Override `getProvidingPressureRange` / `getReceivingPressureRange`
   if your machine deals in pressure > 0.

To make a *vanilla-style* tank participate, do nothing — just expose
`IFluidHandler` (Forge) or register a `FluidStorage.SIDED` (Fabric).
The duct's `tick()` will pick it up.

## 11. Debugging checklist

- `[tick] pos=... fluidType=EMPTY (труба не покрашена идентификатором!)`
  — duct is not painted. Use the Fluid Identifier to set its fluid.
- `hasNode=false` after several ticks — `UniNodespace.createNode` was
  never called. Make sure the BE's `onLoad()` runs server-side
  (check the chunk is actually loaded server-side, not just
  visible client-side).
- Duct connects visually but transfers nothing — most often a
  pressure mismatch. Confirm provider's `getProvidingPressureRange`
  overlaps the receiver's `getReceivingPressureRange`. The Forge
  adapter is hard-coded to pressure 0.
- Adapter never reaps after a machine is broken — its
  `lastSeen` is updated every time the duct calls `tryProvide`/
  `trySubscribe`; the network drops it 3 s after the duct stops
  touching it. If you see a stale entry, check that
  `FluidDuctBlockEntity.adapterCache` is being cleared on type changes
  (`setFluidTypeSilent` and `load`).
- Run the parity matrix in
  [`fluid-pipe-parity-test-matrix.md`](../fluid-pipe-parity-test-matrix.md)
  before declaring an API-affecting change green.
