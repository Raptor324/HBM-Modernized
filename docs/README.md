# HBM Modernized — Documentation

This folder collects the engineering documentation for **HBM's Nuclear
Tech Modernized**. The user-facing description of the mod lives in the
[top-level README](../README.md) — what you'll find here is everything a
contributor or maintainer needs to understand the code.

## Contributor entry points

- [`development.md`](development.md) — JDKs, Stonecutter, dev runs,
  datagen, access transformers / wideners, helper scripts, common
  pitfalls. **Start here if you've just cloned the repo.**
- [`fluid-pipe-parity-test-matrix.md`](fluid-pipe-parity-test-matrix.md)
  — manual QA checklist for the fluid network. Run before merging
  anything that touches `FluidNet`, `FluidDuctBlock(Entity)`, or any
  `IFluidConnectorMK2` implementor.

## Subsystem references

Detailed walkthroughs of the most architecturally heavy parts of the
mod:

- [`systems/rendering_system_new.md`](systems/rendering_system_new.md)
  — OBJ rendering pipeline, instanced rendering, Iris/Oculus
  compatibility paths. The longest doc in the repo; index at the top.
- [`systems/radiation-system.md`](systems/radiation-system.md) — chunk
  radiation simulation, hazard registry, player accumulation, commands,
  full config reference.
- [`systems/fluid-pipe-system.md`](systems/fluid-pipe-system.md) —
  MK2 fluid network model, duct block entity, Forge / Fabric capability
  bridging, painting UX.

## Conventions

- Long-form docs are written in **English** (this is a multi-language
  team — English keeps the contribution funnel wide). Inline comments
  in the source remain a mix of English and Russian.
- Each subsystem doc starts with a "components at a glance" diagram and
  ends with a troubleshooting / common-pitfalls section.
- Cross-link between docs liberally; do not duplicate content.
- Images go in [`images/`](images). Prefer existing screenshots over
  taking new ones if they already illustrate the feature you're
  describing.
