# HBM-Modernized × TACZ — MVP Architecture & Tuning Guide

Deliverable mandated by `docs/tacz-integration/master-prompt-mvp.md` PHASE 6 step 3 (final
checklist bullet): *"Write a concise `docs/tacz-integration/mvp-architecture-and-tuning-guide.md`
explaining how to add new guns and how to tune `nmesh_bindings/*.json` live in-game with
`/tacz reload`."* This document covers that, plus the full architecture map, the
`ExternalGunRenderBridge` SPI contract, and the aggregated caveats of Phases 1–5.

Repositories this guide references:

| Repo | Path | Role |
|---|---|---|
| HBM-Modernized | `C:/Projects/HBM-Modernized-agents` (branch `machines`) | Hosts the `ExternalGunRenderBridge` SPI and its 12 wiring sites |
| HBM-TACZ-Addon | `C:/Projects/HBM-TACZ-Addon` | The companion mod (`hbm_tacz`) — gun pack, nmesh engine, hybrid bridge |
| TACZ | `C:/Projects/TACZ` (branch `1.20.1`, `1.2.0-hotfix`) | Reference sources + default gun pack (donor animations/skeletons/sounds) |

**Verification status of this document.** Everything cited below with a `file:line` reference
was read in this session. Checks actually run here: the two shipped `.nmesh` binaries were
parsed (`python -I C:/Users/user/tacz-phase5-work/parse_nmesh.py`, byte-exact consumption,
13/8 sub-meshes confirmed); both addon `.geo.json` skeletons were parsed (116/97 bones, 0
cubes, alias pivots dumped); donor assets (`hk_g3`/`ak47` animations, state machines,
`ads_presets/renew.json`) were confirmed present in TACZ's default pack; every
`ExternalGunRenderBridge` call site was located by grep. **Not run here** (status per the
phase notes, see §5): any Gradle build of either repo, any `runClient` smoke test, any
in-game `/tacz reload` — visual transforms remain tuned to placeholder values.

---

## 1. Architecture overview

### 1.1 The two runtime modes

The addon (`hbm_tacz`) declares **TACZ as a required dependency and `hbm_m` as an optional
one** (`C:/Projects/HBM-TACZ-Addon/src/main/resources/META-INF/mods.toml:18-33`: `tacz`
`mandatory=true`, `hbm_m` `mandatory=false`, both `ordering="AFTER"`). One jar supports both
modes; the difference is decided at runtime by two independent guards:

- **Class-load guard (Java code):** every class that imports `com.hbm_m.*` lives under
  `com.hbm_tacz.compat.hbm.*` and is reached only through the `ModList.get().isLoaded("hbm_m")`
  check in the `HbmTacz` constructor (`src/main/java/com/hbm_tacz/HbmTacz.java:35-39`) →
  `HbmCompatBootstrap.init()` (`HbmCompatBootstrap.java:32-42`), which additionally dist-gates
  the client wiring (`FMLEnvironment.dist.isClient()` → `HbmCompatClient.init()`).
- **Mixin guard:** the `hbmcompat` mixins are rejected by
  `com.hbm_tacz.mixin.HbmTaczMixinPlugin.shouldApplyMixin` unless
  `LoadingModList.get().getModFileById("hbm_m") != null`
  (`src/main/java/com/hbm_tacz/mixin/HbmTaczMixinPlugin.java:38-40`) — exactly the check the
  master spec §2.2 mandates. The Phase 3 client mixins into TACZ classes live in a separate,
  plugin-less config (`hbm_tacz.json`).

| | **Mode A — Standalone TACZ** (`tacz` + `hbm_tacz`, no `hbm_m`) | **Mode B — Hybrid SEDNA bridge** (`tacz` + `hbm_tacz` + `hbm_m`) |
|---|---|---|
| Gun items | Standard `tacz:modern_kinetic_gun` items with `GunId: hbm_m:gun_*`; TACZ owns firing (`EntityKineticBullet`), ammo, attachments, creative tabs | The real HBM `ItemGunBaseNT` items; SEDNA owns 100 % of server logic (`Lego.doStandardFire`, `EntityBulletBaseMK4`, `BulletConfig`, jams, weapon mods, mob AI) |
| Rendering | TACZ pipeline + injected `NMeshBedrockCube`s (the only addon machinery active) | Same pipeline, but the gun reaches it through `ExternalGunRenderBridge` + `MixinItemGunBaseNT` |
| Input | TACZ's own keybind handlers | All six TACZ input mixins HEAD-cancel while an `ItemGunBaseNT` is in the main hand (`HbmTaczCompatGates.isHbmGunMainHand`, `HbmTaczCompatGates.java:31-38`) |
| Creative tabs | `hbm_m:*` pack guns appear normally | `MixinAbstractGunItem` (`mixin/hbmcompat/MixinAbstractGunItem.java:27-32`) strips the `hbm_m` namespace entries so no duplicates appear next to the real SEDNA items |
| Recoil | TACZ camera recoil from the pack `recoil` splines | Same splines, applied via the client `GunFireEvent` posted by the animation bridge; HBM's own camera kick is suppressed (`GunClientHooks.java:158-165`) |
| Ammo | TACZ `index/ammo` items | SEDNA magazines (`magcount_0` NBT) are the source of truth; TACZ HUD reads them through `MixinItemGunBaseNT.getCurrentAmmoCount` |

### 1.2 Class map

#### HBM-Modernized side (SPI + wiring)

SPI — `src/main/java/com/hbm_m/client/render/weapon/bridge/ExternalGunRenderBridge.java`
(full contract in §4): static holder `get()`/`register()` (`:31-38`), six abstract methods
(`:45-82`), private null-object `None` (`:85-119`). Zero TACZ imports; vanilla + HBM types
only, so it compiles on both Stonecutter targets.

Wiring sites — every consult is `ExternalGunRenderBridge.get().…` so a late `register()`
takes effect immediately:

| # | Site | Location | Behavior when bridge claims the stack |
|---|---|---|---|
| 1 | `IClientItemExtensions.getCustomRenderer()` | `src/main/java/com/hbm_m/item/weapon/sedna/ItemGunBaseNT.java:396-400` (Forge branch; NeoForge twin `:436-440`) | Returns the external BEWLR (`resolveCustomRenderer`); fallback is HBM's `GunItemRenderer.INSTANCE` |
| 2 | `IClientItemExtensions.getArmPose()` | `ItemGunBaseNT.java:404-417` (check at `:406-408`; twin `:444-447`) | `ArmPose.EMPTY` — TACZ's `playerAnimator` owns 3rd-person arms (checked **before** the akimbo ITEM and default BOW_AND_ARROW poses) |
| 3 | `GunClientEventsForge.onRenderHand` | `src/main/java/com/hbm_m/client/weapon/GunClientEventsForge.java:25-35` (early return at `:29`; twin `:101-112`) | HBM draws nothing and does **not** cancel `RenderHandEvent` — SBM's `FirstPersonRenderHandler` (gun + hands) takes the event |
| 4 | `GunClientEventsForge.onOverlayPre` (CROSSHAIR) | `GunClientEventsForge.java:37-49` (twin `:114-126`) | HBM skips its crosshair render **and** skips canceling the vanilla CROSSHAIR overlay; TACZ owns ADS crosshair suppression. The HOTBAR branch still calls `renderHotbarExtras` (ammo/durability HUD stays) |
| 5 | `GunClientEventsForge.onFov` | `GunClientEventsForge.java:79-85` (twin `:157-163`) | HBM skips `modifyFov`; TACZ's `CameraSetupEvent` owns ADS FOV |
| 6 | `GunClientHooks.handleAnimationPacket` | `src/main/java/com/hbm_m/client/weapon/GunClientHooks.java:52-67` (forward at `:67`) | `onGunAnimation(...)` — the `HbmAnimationPacket` → TACZ state-machine bridge (§1.3, flow B) |
| 7 | `GunClientHooks.onRenderFrame` | `GunClientHooks.java:148-165` (guard at `:158-165`) | `shouldSuppressNativeRecoil` → HBM's camera kick is skipped but the recoil statics are still multiplied by `decayStep`, because `setupRecoil` accumulates (`+=`) — skipping decay entirely would grow them unbounded |
| 8 | `GunClientHooks.renderHand` | `GunClientHooks.java:220-235` (check at `:226`) | Returns `false` so the vanilla hand is **not** canceled — the external first-person handler must take the event |
| 9 | `GunClientHooks.renderHotbarExtras` (scope overlay) | `GunClientHooks.java:250-261` (gate at `:260`) | `GunHud.renderScope` is skipped; HUD components (ammo counters) still draw |
| 10 | `GunItemRenderer.renderByItem` | `src/main/java/com/hbm_m/client/weapon/GunItemRenderer.java:47-50` | `renderByItem(...)` returns `true` → HBM's own BEWLR path exits immediately (3rd person, GUI, ground, item frames) |
| 11 | `WeaponRenderRegistry.bindBakedModels` | `src/main/java/com/hbm_m/client/render/weapon/WeaponRenderRegistry.java:266-281` (skip at `:273`) | `shouldSkipNativeVboBake` → the native `WeaponPartVboRenderer` VBO pre-bake for that model is skipped (VRAM saving; the bridge must never skip casings/grenades) |
| 12 | HBM's own `MixinItemInHandLayer` | `src/main/java/com/hbm_m/mixin/client/MixinItemInHandLayer.java:41` | HBM's akimbo off-hand injection is skipped |

Naming deviation from the master spec, kept deliberately: §1.2 of the spec calls the hooks
`tryRenderByItem(...)` / `notifyGunAnimation(...)`, but §1.1 — which fixes "the six listed
methods" — names them `renderByItem(...)` / `onGunAnimation(...)`. The implementation uses
the §1.1 names (verified at every call site above); semantics match §1.2's intent. The
`onRenderFrame` wiring (#7) is a deliberate addition beyond the §1.2 list — without it,
`shouldSuppressNativeRecoil` would have no consumer.

#### Addon side (`C:/Projects/HBM-TACZ-Addon/src/main/java/com/hbm_tacz/...`)

| Class | Role |
|---|---|
| `HbmTacz.java` | `@Mod("hbm_tacz")`. Logs the active mode, runs the `hbm_m` guard → `HbmCompatBootstrap.init()` (`:35-39`), registers the embedded gun pack for first-launch export: `ResourceManager.registerExportResource(HbmTacz.class, "/gunpack")` (`:50`; the leading `/` is required because `Class.getResource` resolves path-less names against the class's own package) |
| `HbmCompatBootstrap.java` | Re-checks `hbm_m`, dist-gates the client bootstrap (`:32-42`) |
| `compat/hbm/HbmCompatClient.java` | **The registration point of the whole bridge**: `ExternalGunRenderBridge.register(new HbmGunRenderBridge())` + `MinecraftForge.EVENT_BUS.register(new HbmAimSyncListener())` (`:30-38`). Runs during mod construction, before any render |
| `compat/hbm/HbmGunRenderBridge.java` | The `ExternalGunRenderBridge` implementation — every SPI method mapped to TACZ API calls (`:38-192`, detailed in §4) |
| `compat/hbm/HbmTaczGunItemRenderer.java` | Single BEWLR instance `RENDERER` (`:34`), extends TACZ's `GunItemRendererWrapper` (which extends SBM's `AnimateGeoItemRenderer`, implementing `IFPGeoItemRenderer` — so returning it from `getCustomRenderer()` hooks SBM first-person rendering, draw/put-away and TACZ's 3P state-machine init). Falls back to HBM's `GunItemRenderer.INSTANCE` per stack when `TimelessAPI.getGunDisplay(stack).isEmpty()` (`:47-58`); also provides `HbmGunAnimationStateContext` via `initContext` (`:40-44`) |
| `compat/hbm/HbmGunAnimationStateContext.java` | Lua state-machine context for bridged guns: `getReloadStateType()` maps SEDNA `state_0 == RELOADING` + `magprev_0` → `EMPTY_RELOAD_FEEDING` / `TACTICAL_RELOAD_FEEDING` (`:52-66`); `getShootCoolDown()` maps the SEDNA cooldown timer (`timer_0` ticks × 50 ms) (`:69-78`) |
| `compat/hbm/HbmAimSyncListener.java` | Client-tick-END listener mirroring HBM's `aiming` NBT flag into `IClientPlayerGunOperator.fromLocalPlayer(player).aim(...)` on state edges, clearing TACZ aim on gun switch / empty hand (`:39-78`). Registered manually (not `@Mod.EventBusSubscriber`) so the class is never loaded in standalone mode |
| `compat/hbm/HbmTaczCompatGates.java` | `isHbmGunMainHand()` — the one shared gate for all six input-suppression mixins (`:31-38`) |
| `mixin/HbmTaczMixinPlugin.java` | `IMixinConfigPlugin` gating `hbmcompat` on `hbm_m` presence (`:38-40`) |
| `mixin/hbmcompat/MixinItemGunBaseNT.java` | `@Mixin(ItemGunBaseNT.class, remap=false)` adding `implements GunItemDataAccessor` — the SEDNA→TACZ state mapping (`:68-69`); details in §1.3 flow C |
| `mixin/hbmcompat/MixinAbstractGunItem.java` | Filters `hbm_m:*` stacks out of TACZ creative tabs in hybrid mode (`:27-32`) |
| `mixin/hbmcompat/Mixin{Shoot,Reload,Aim,FireSelect,Unload,Melee}Key.java` | HEAD-cancel TACZ's own input handlers (`ShootKey.autoShoot`, `ReloadKey.onReloadPress`+auto-reload, `AimKey.onAimInput`, `FireSelectKey.onFireSelectPress`, `UnloadKey.onUnloadPress`, `MeleeKey.onMeleePress`) while a SEDNA gun is held — HBM owns the whole input chain (`HbmKeybinds → KeybindPacket → ItemGunBaseNT.handleKeybind → Lego`); example: `MixinShootKey.java:26-31`. Registered in the `client` section of `hbm_tacz_compat.json` (their `@OnlyIn(CLIENT)` targets don't exist on dedicated servers) |
| `mixin/client/MixinGunDisplayInstance.java` | Injects `NMeshGunModelInjector.inject(displayId, model)` at the TAIL of `checkTextureAndModel` and `checkLod` — the only two `new BedrockGunModel` sites in TACZ 1.2.0 (`:46-58`) |
| `mixin/client/MixinExternalGunItemVboRenderer.java` | `tryRender` HEAD-cancel returning `StaticGunRenderResult.UNSUPPORTED` for nmesh-backed guns (`:30-39`) — TACZ's static VBO fast path would draw an empty mesh (the pack skeletons are cube-less), this forces the animated `BedrockGunModel` path for THIRD_PERSON/GROUND/FIXED |
| `render/nmesh/NMeshData.java` | Standalone `.nmesh` v3 binary parser (magic `0x4E4D5348`), zero `com.hbm_m` dependencies |
| `render/nmesh/NMeshAssetManager.java` | Client reload listener caching every `assets/<ns>/models/weapons/*.nmesh` (`:32-72`) |
| `render/nmesh/NMeshBinding.java` | POJO + validator for one binding file — the field reference in §3 is this class (`:70-217`) |
| `render/nmesh/NMeshBindingManager.java` | Client reload listener for `assets/<ns>/nmesh_bindings/<gun_id>.json`, extends vanilla `SimpleJsonResourceReloadListener` (`:32-71`) |
| `render/nmesh/NMeshGunModelInjector.java` | Resolves the binding for a display, bakes `NMeshBedrockCube`s into the bound bones (`:70-127`), computes the rest-pose inverse (`:138-161`), exposes `isNmeshBackedGun(stack)` for the VBO mixin (`:97-104`) |
| `render/nmesh/NMeshBedrockCube.java` | The injected cube: per-vertex `M_rest⁻¹ · (x, 24−y, z) · base_transform · entry_transform · p_nmesh`, UV `v' = 1−v` when `flip_v`, triangle→degenerate-quad emission and winding-flip compensation (`:38-45`, `:104`, `:140`, `:200` — every triangle is emitted as the 4-vertex quad `(v0, v1, v2, v2)` because TACZ's `RenderType.entityCutout` buffers use `VertexFormat.Mode.QUADS`) |
| `client/ClientSetup.java` | Registers both reload listeners via `RegisterClientReloadListenersEvent` (mod bus, client dist) (`:34-38`) |

Resources: `src/main/resources/hbm_tacz.json` (client mixins `client.MixinGunDisplayInstance`,
`client.MixinExternalGunItemVboRenderer`, `:9-11`), `src/main/resources/hbm_tacz_compat.json`
(`mixins`: `MixinItemGunBaseNT`, `MixinAbstractGunItem`; `client`: the six input mixins;
shared `refmap: hbm_tacz.refmap.json`), `src/main/resources/pack.mcmeta`,
`src/main/resources/gunpack/**` (33-file gun pack, §2), and
`src/main/resources/assets/hbm_m/{models/weapons/*.nmesh, nmesh_bindings/*.json}`
(`g3.nmesh` 122 242 B, `stg77.nmesh` 91 751 B — sizes confirmed by the binary parse above).

#### TACZ touchpoints (what of TACZ the addon actually touches)

| TACZ element | Touched by |
|---|---|
| `com.tacz.guns.client.resource.GunDisplayInstance#checkTextureAndModel` / `#checkLod` | `MixinGunDisplayInstance` — the single choke point where every `BedrockGunModel` is built |
| `com.tacz.guns.client.renderer.item.ExternalGunItemVboRenderer#tryRender` | `MixinExternalGunItemVboRenderer` |
| `com.tacz.guns.client.renderer.item.GunItemRendererWrapper` | extended by `HbmTaczGunItemRenderer` |
| `com.tacz.guns.api.item.IGun` / `GunItemDataAccessor` | implemented on `ItemGunBaseNT` via `MixinItemGunBaseNT` |
| `com.tacz.guns.api.item.gun.AbstractGunItem#fillItemCategory` | `MixinAbstractGunItem` |
| `com.tacz.guns.client.input.{Shoot,Reload,Aim,FireSelect,Unload,Melee}Key` | six input-suppression mixins |
| `com.tacz.guns.api.TimelessAPI` (`getGunDisplay`, `getCommonGunIndex`, `getAllCommonGunIndex`) | `HbmGunRenderBridge`, `MixinItemGunBaseNT`, `NMeshGunModelInjector` — plain API calls |
| `com.tacz.guns.api.resource.ResourceManager.registerExportResource` | `HbmTacz.java:50` (gun-pack export) |
| `com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator` | `HbmAimSyncListener` |
| TACZ default gun pack (read-only donors) | displays reference `tacz:hk_g3` / `tacz:ak47` animations + state machines, `tacz:renew` ads preset, `tacz:hk_g3/*` + `tacz:ak47/*` sounds, `tacz:rifle_default.player_animation` (stg77 display, `player_animator_3rd`) — all confirmed present under `C:/Projects/TACZ/src/main/resources/assets/tacz/custom/tacz_default_gun/assets/tacz/` in this session |

### 1.3 Runtime flows

**A. Render frame (hybrid mode).**
`IClientItemExtensions.getCustomRenderer` (wiring #1) → `HbmTaczGunItemRenderer.RENDERER`
→ `GunItemRendererWrapper.renderByItem` → for THIRD_PERSON/GROUND/FIXED TACZ first tries the
static VBO fast path (`ExternalGunItemVboRenderer.tryRender`), which the mixin forces to
`UNSUPPORTED` for nmesh-backed guns → animated `BedrockGunModel` path → `BedrockPart.render`
walks the bone hierarchy → `NMeshBedrockCube.compile` transforms the bound `.nmesh`
sub-meshes by the bone's `PoseStack.Pose` and emits degenerate quads into TACZ's
`VertexConsumer`. First person is drawn by SBM's `FirstPersonRenderHandler`, which intercepts
`RenderHandEvent` because HBM neither renders nor cancels the event (wiring #3/#8).

**B. Shot (hybrid mode).** Server: SEDNA fires (`Lego.doStandardFire`), plays its own fire
sound, spawns `EntityBulletBaseMK4`, sends `HbmAnimationPacket(CYCLE)` → client
`GunClientHooks.handleAnimationPacket` (`:67`) → `HbmGunRenderBridge.onGunAnimation`
(`HbmGunRenderBridge.java:139-160`): posts client `GunShootEvent` (a cancel suppresses the
per-shot effects), then `GunFireEvent(player, stack, LogicalSide.CLIENT)` — which triggers
TACZ's full shot-effect set: `CameraSetupEvent.initialCameraRecoil` spline camera recoil,
`FirstPersonRenderGunEvent.onGunFire` FP sway + muzzle flash + Snowstorm particles,
3P muzzle particles, `playerAnimator` fire pose — then
`stateMachine.trigger(INPUT_SHOOT)`. TACZ's shoot sound is deliberately **not** played
(`SoundPlayManager.playShootSound` skipped, `HbmGunRenderBridge.java:148-150`): SEDNA already
played the sound server-side, a TACZ sound would double it. `CYCLE_DRY` triggers
`INPUT_SHOOT` only (no effects); `RELOAD` → `INPUT_RELOAD`; `INSPECT`/`JAMMED` →
`INPUT_INSPECT`; `RELOAD_CYCLE`/`RELOAD_END`/`ALT_CYCLE`/`SPINUP`/`SPINDOWN`/`EQUIP` are
ignored (reasons in `HbmGunRenderBridge.java:182-189`).

**C. Gun state queries.** `MixinItemGunBaseNT` maps SEDNA state into the `IGun` contract:
`getGunId` → TACZ `GunId` NBT or `hbm_m:<registry path>` (`:104-119`); `getGunDisplayId` →
NBT or `DefaultAssets.DEFAULT_GUN_DISPLAY_ID` (`:122-134`); `getCurrentAmmoCount` →
receiver-0 magazine `getAmount(stack, null)` (`:168-176`); `hasBulletInBarrel` →
`ammoCount > 0` (`:192-195`); `getFireMode` → `refireOnHold && mode_0 == 0 ? AUTO : SEMI`
(`:141-148`); `getRPM` → `1200 / delayAfterFire` (`:202-206`); attachment NBT methods and
`getAimingZoom` etc. are inherited `GunItemDataAccessor` defaults (same pattern as TACZ's
own `ModernKineticGunItem`); `dropAllAmmo` refunds the loaded SEDNA ammo item
(`:280-303`).

**D. ADS (hybrid mode).** Middle mouse → HBM toggles its `aiming` NBT (`Lego.LAMBDA_TOGGLE_AIM`)
→ `HbmAimSyncListener.onClientTick` mirrors it into
`IClientPlayerGunOperator.fromLocalPlayer(player).aim(...)` on state edges
(`HbmAimSyncListener.java:65-69`). Because the bridged gun is an `IGun`, TACZ's own
`LocalPlayerAim.tickAimingProgress` animates `clientAimingProgress` with the pack's aim
time, and `FirstPersonRenderGunEvent.applyFirstPersonGunTransform`, the
`ads_preset` `idle_view`→`iron_view` interpolation and `CameraSetupEvent` FOV zoom all read
that progress — TACZ's ADS pipeline runs natively on HBM guns. Side effect (intentional):
TACZ's server operator blocks sprinting while aiming.

### 1.4 Build & resource plumbing

- **Stonecutter layout** (same shape as HBM-Modernized): `settings.gradle.kts` declares
  projects `:1.20.1-forge` (active MVP target) and `:1.21.1-neoforge` (stub — the main
  source set is emptied in `build.neoforge.gradle.kts` so the shared TACZ-only sources are
  never compiled there; `:1.21.1-neoforge:compileJava` → NO-SOURCE).
- **Dependencies** (`build.forge.gradle.kts:76-88`): TACZ `-all` fat jar via
  `modCompileOnly` + `modRuntimeOnly`; `simplebedrockmodel` `modCompileOnly` only (TACZ
  bundles SBM at runtime — a second copy would register a duplicate mod); `hbm_m`
  `modCompileOnly` only (optional dep). All three jars live in the git-ignored `libs/`
  (provenance in `libs/README.md`); they are SRG-reobfuscated and MDG remaps them to Mojmap.
- **Mixin plumbing:** both configs are registered twice — `mods.toml:14`
  (`mixins=["hbm_tacz.json", "hbm_tacz_compat.json"]`, expanded by `processResources`) and
  the jar manifest `MixinConfigs` attribute; one shared refmap
  (`hbm_tacz.refmap.json`) declared by both configs and wired to the Mixin annotation
  processor in `build.forge.gradle.kts`. While the refmap would be empty, the
  `ensureMixinRefmap` task (`build.forge.gradle.kts`, writes `{}` refmap + sidecar) keeps
  `reobfJar` green; once real `@Mixin` classes exist the AP output replaces the placeholders
  automatically.
- **Gun-pack export:** TACZ copies registered jar folders to `<gamedir>/tacz/` on the **first
  load only** (`C:/Projects/TACZ/src/main/java/com/tacz/guns/resource/GunPackLoader.java:80-84`
  — the `firstLoad` block calling `GetJarResources.copyModDirectory` per
  `ResourceManager.ExtraEntry`; silently skipped while the folder doesn't exist in the jar).
  So `src/main/resources/gunpack/` lands at `<gamedir>/tacz/gunpack/`, hot-editable there
  with `/tacz reload`. The exported copy is **not** re-exported on later launches — after
  editing pack files in the repo you must delete `<gamedir>/tacz/gunpack` (or copy the
  folder manually) and restart, or iterate directly in the exported copy.

---

## 2. How to add a new gun

The two MVP showcase guns (`hbm_m:gun_g3` ← donor `tacz:hk_g3`, `hbm_m:gun_stg77` ← donor
`tacz:ak47`) are the worked examples; every file below exists for them and can be cloned.

### 2.1 ID conventions (fixed — do not improvise)

| Thing | Convention | Example |
|---|---|---|
| Gun id | `hbm_m:gun_<name>` — the file name of `gunpack/data/hbm_m/index/guns/<id>.json` | `hbm_m:gun_g3` |
| Display id | `hbm_m:gun_<name>_display` | `hbm_m:gun_g3_display` |
| Gun-data id | `hbm_m:gun_<name>_data` | `hbm_m:gun_g3_data` |
| Geo model id | `hbm_m:gun/gun_<name>_geo` (under `gunpack/assets/hbm_m/geo_models/gun/`) | `hbm_m:gun/gun_g3_geo` |
| Textures | `hbm_m:gun/uv/gun_<name>` (UV) and `hbm_m:gun/slot/gun_<name>` (inventory icon) under `gunpack/assets/hbm_m/textures/gun/` | `hbm_m:gun/uv/gun_g3` |
| Ammo id | `hbm_m:<caliber>_<variant>` named after the HBM `GunFactory.EnumAmmo` entry | `hbm_m:r556_sp` |
| Ammo display id | `hbm_m:<caliber>_display`, shared by all variants of a caliber | `hbm_m:r556_display` |
| nmesh asset | `assets/hbm_m/models/weapons/<name>.nmesh`, referenced as `hbm_m:models/weapons/<name>.nmesh` | `hbm_m:models/weapons/g3.nmesh` |
| Binding file | `assets/hbm_m/nmesh_bindings/<gun_id>.json` (file name = gun id) | `.../nmesh_bindings/gun_g3.json` → `hbm_m:gun_g3` |
| **Hybrid-mode link** | The SEDNA item's **registry name must equal `<name>` of the gun id** — `MixinItemGunBaseNT.getGunId` derives `hbm_m:gun_g3` from the registry name (`MixinItemGunBaseNT.java:113-118`) | item `hbm_m:gun_g3` ↔ pack id `hbm_m:gun_g3` |

The hybrid link is the one convention that silently breaks things if ignored: with a
mismatched registry name, the pack display never resolves for the SEDNA item, every bridge
method returns `false`, and the gun falls back to HBM's native rendering — no crash, just no
TACZ.

### 2.2 Step-by-step

**Step 0 — HBM-side prerequisites.** The weapon must exist as a SEDNA item whose registry
name matches §2.1 (`GunFactory.reg("gun_g3", new ItemGunBaseNT(...))`,
`src/main/java/com/hbm_m/item/weapon/sedna/factory/XFactory556mm.java:72`; listed in
`WeaponItems.java:84`), and its OBJ must be compiled to `.nmesh` (HBM's generated
assets, e.g. `versions/1.20.1-forge/build/generated/nmesh/assets/hbm_m/models/weapons/g3.nmesh`).

**Step 1 — copy the mesh + texture into the addon.**
- `src/main/resources/assets/hbm_m/models/weapons/<name>.nmesh` (binary, verbatim).
- The UV texture the `.nmesh` UVs were authored for →
  `gunpack/assets/hbm_m/textures/gun/uv/gun_<name>.png`. **Use HBM's own texture, not the
  donor's** — the `.nmesh` UVs are authored for HBM's texture page; the donor UV renders
  scrambled. The donor only supplies the slot icon
  (`textures/gun/slot/gun_<name>.png`, e.g. the 64×64 donor icons used for the MVP).

**Step 2 — inspect the mesh.** Sub-mesh names are the OBJ object names. Verify against the
actual binary rather than memory (`python -I C:/Users/user/tacz-phase5-work/parse_nmesh.py
<path>.nmesh` prints obj/group/AABB per sub-mesh). Measured this session:
- `g3.nmesh` (13): `Rifle, Bullet, Guide_And_Bolt, Handle, Plug, Mag_Paddle, Magazine,
  Stock, Flash_Hider, Scope, Silencer, Selector, Trigger`
- `stg77.nmesh` (8): `Breech, Barrel, Lever, Safety, Handle, Bullets, Magazine, Gun`

**Step 3 — create the cube-less geo skeleton**
`gunpack/assets/hbm_m/geo_models/gun/gun_<name>_geo.json`. Take the donor's
`.../geo_models/gun/<donor>_geo.json` from the TACZ default pack and:
- keep its **exact bone hierarchy** (it carries the animations you are about to reuse);
- **strip every `cubes` array** (the MVP files contain 0 cubes over 116/97 bones — verified
  by the geo parse in this session);
- keep TACZ's functional bones: `root`, `gun_body`, `magazine`, `bolt`, `lefthand_pos`,
  `righthand_pos`, `iron_view`, `idle_view`, `muzzle_flash`, `muzzle_pos`, `shell`,
  `thirdperson_hand`, `ground`, `fixed`, `<attachment>_pos`/`<attachment>_default`,
  refit views;
- add **alias bones** for HBM part groups that have no donor counterpart (pivot = parent
  pivot → identity transform, so they inherit all donor animation). MVP aliases:
  `gun_g3_geo`: `gun_body`←`G3_default_body` [0, 8, 13.86], `charging_handle`←`handle`
  [0.37461, 11.15625, −2.01474], `safety`←`selector` [0, 8, 14.42188];
  `gun_stg77_geo`: `gun_body`←`AKM` [0, 0, 0], `charging_handle`←`bolt`
  [−0.48863, 17.54799, −0.0625], `safety`←`safety3` [−0.87501, 8.22318, 6.88238].
  Parent aliases on bones the donor animations actually move (for `hk_g3`: `shoot`
  animates root/bolt/bullet_in_barrel, `reload_empty` animates `G3_bolt`/`handle`; for
  `ak47`: `shoot` animates root/bolt, `static_idle` animates `safety3`).

**Step 4 — display JSON** `gunpack/assets/hbm_m/display/guns/gun_<name>_display.json`.
Clone `gun_g3_display.json`: `"model"` → your geo id, `"texture"`/`"slot"` → §2.1 texture
ids, then reuse donor cross-pack references: `"animation": "tacz:hk_g3"` (or `tacz:ak47`),
`"state_machine": "tacz:hk_g3_state_machine"` (or `ak47_state_machine`),
`"use_default_animation": "rifle"`, `"ads_preset": "tacz:renew"`, `"muzzle_flash"` with
`tacz:flash/common_muzzle_flash`, sound map over donor ids (`tacz:hk_g3/*`, plus the
generic `tacz:dry_fire`, `tacz:fire_select`, `tacz:head_hit`, `tacz:flesh_hit`, `tacz:kill`,
`tacz:melee_stock/*`). No `hud` field (donor HUDs are sight pictures of donor geometry —
wrong for nmesh guns; the field is optional). Display `transform.scale` entries
(`thirdperson`/`ground`/`fixed`) are TACZ view-space scales, independent of the binding's
`base_transform`.

**Step 5 — index + data + tags.**
- `gunpack/data/hbm_m/index/guns/<gun_id>.json`: `name` (lang key), `display`, `data`,
  `type` (rifle/…), `item_type: "modern_kinetic"`, `sort`.
- `gunpack/data/hbm_m/data/guns/<gun_id>_data.json`: stats. Derive them from the HBM factory
  (both MVP guns live in `XFactory556mm`, not `XFactory762mm` — grep over
  `src/main/java/com/hbm_m/item/weapon/sedna/factory/` finds `gun_g3`/`gun_stg77` only
  there): damage = `Receiver.dmg × BulletConfig.damageMult`; `rpm = 1200 / delayAfterFire`;
  magazine = the `MagazineFullReload` amount; draw time = `draw` ticks ÷ 20; aim time =
  `aimSpeed`-derived; recoil ≈ the HBM recoil-lambda σ as a uniform spline; hipfire spread =
  HBM `Receiver` hipfire radians → degrees. TACZ has no data/ammo JSON — ammo is expressed
  as `index/ammo` entries (step 6) plus the `ammo`/`ammo_amount` fields here. Keep
  `allow_attachment_types` in sync with `tacz_tags/attachments/allow_attachments/<gun_id>.json`
  (MVP mapping of HBM weapon mods: `ID_SILENCER→muzzle`, `ID_SCOPE→scope`,
  `ID_NO_STOCK/FURNITURE→stock`, `ID_STACK_MAG→extended_mag`).
- Note: TACZ's pack loader parses leniently
  (`LazyJsonDataManager.java:148` — `GsonHelper.fromJson(gson, reader, JsonElement.class, true)`),
  so **comments are fine in gunpack JSONs** — the MVP data files use them to cite the HBM
  source lines (see `gun_g3_data.json` header).

**Step 6 — ammo.** `gunpack/data/hbm_m/index/ammo/<id>.json` per variant
(`name`, `display` → shared `hbm_m:r556_display`, `stack_size`, `sort`) and one
`gunpack/assets/hbm_m/display/ammo/<caliber>_display.json` (model/texture/slot/shell — the
MVP copies the donor 5.56×45 cartridge assets). Damage for each variant goes into the gun
data `bullet` block of the guns that default to it.

**Step 7 — lang.** `gunpack/assets/hbm_m/lang/{en_us,ru_ru}.json` — pack name/desc keys plus
`hbm_m.gun.<gun_id>.name` and `hbm_m.ammo.<ammo_id>.name` (see the MVP `en_us.json`).

**Step 8 — binding** `src/main/resources/assets/hbm_m/nmesh_bindings/<gun_id>.json` (§3 for
the schema; note these files are **outside** the gunpack and parsed strictly — no comments).
Bind every rendered sub-mesh to an alias or functional bone. Do **not** bind to
`<type>_pos` bones: TACZ installs a functional renderer there (`attachmentAdapterNodeRender`,
`BedrockGunModel.java:162`, wired for `MOUNT`/`CARRY`/`SIGHT` nodes at `:93-99`), and a
functional renderer suppresses the bone's cubes **and children**
(`C:/Projects/TACZ/src/main/java/com/tacz/guns/client/model/FunctionalBedrockPart.java:68-90`
— checked here: the non-null `functionalRenderer` branch renders only the functional
renderer); bind to weapon-body bones or the alias bones instead — the hide-style renderers
on those (`extendedMagHiddenRender`/`scopeHiddenRender`/`ammoHiddenRender`/
`handguardDefault|TacticalRender`, implementations at `BedrockGunModel.java:208-224, 423-440`,
installed at `:87-113`) return `null`, so injected cubes ride along.

**Step 9 — build & deploy.** Compile the addon
(`cmd /c C:\Projects\HBM-TACZ-Addon\gradlew.bat -p C:/Projects/HBM-TACZ-Addon
:1.20.1-forge:compileJava` — the Phase 2 gate command). On first launch TACZ exports
`gunpack/` → `<gamedir>/tacz/gunpack/` (§1.4). Deleting the exported folder re-exports on
the next launch.

**Step 10 — verify in game.** Mode A: creative tab → hold the gun → it should render the
nmesh over the animated donor skeleton. Mode B: `/give @s hbm_m:gun_<name>` → same
rendering, SEDNA logic. Then tune (§3.4).

### 2.3 MVP per-gun notes (what was deliberately left unbound / static)

- **g3**: `Scope` and `Silencer` are HBM weapon-mod parts (`ItemRenderG3` renders them only
  with matching mods) — deliberately unbound, TACZ attachments replace them. `Bullet` (HBM
  shows it only when the mag is empty) and `Plug` (moves only in HBM dry/reload anims) are
  bound **statically** to `gun_body` — the binding schema has no visibility rules yet, so
  they will be visible always.
- **stg77**: `Bullets` (HBM never renders it; it sits inside the magwell) is bound statically
  to `gun_body`; move it to the `magazine` bone in `gun_stg77.json` if it should drop with
  the mag during reload animations.
- `base_transform` in both MVP bindings is deliberately the muzzle-direction fix only
  (`rotate [0, 180, 0]`, `scale [1,1,1]`); see §3.4 for the tuning plan.

---

## 3. `nmesh_bindings` field reference

### 3.1 Schema

Authoritative implementation: `C:/Projects/HBM-TACZ-Addon/src/main/java/com/hbm_tacz/render/nmesh/NMeshBinding.java`
(fields `:74-90`, `Transform` `:93-123`, `SubMeshBinding` `:126-153`, validation `:178-205`).
Live examples: `C:/Projects/HBM-TACZ-Addon/src/main/resources/assets/hbm_m/nmesh_bindings/gun_g3.json`
and `gun_stg77.json`.

```json
{
  "nmesh": "hbm_m:models/weapons/g3.nmesh",
  "flip_v": true,
  "auto_cancel_bone_pivot": true,
  "base_transform": {
    "translate": [0.0, 0.0, 0.0],
    "rotate": [0.0, 180.0, 0.0],
    "scale": [1.0, 1.0, 1.0]
  },
  "bones": {
    "gun_body": [
      { "submesh": "Rifle",   "translate": [0, 0, 0], "rotate": [0, 0, 0], "scale": [1, 1, 1] },
      { "submesh": "Magazine" }
    ],
    "magazine":       [ { "submesh": "Magazine" } ],
    "bolt":           [ { "submesh": "Guide_And_Bolt" } ],
    "charging_handle":[ { "submesh": "Handle" } ],
    "safety":         [ { "submesh": "Selector" } ]
  }
}
```

**Strict JSON — no comments in these files.** `NMeshBindingManager` extends vanilla
`SimpleJsonResourceReloadListener` (`NMeshBindingManager.java:32`), whose Gson path is
non-lenient: a `//` or `/* */` comment makes the file fail to parse, it never reaches
`apply()`, and the symptom is the log line
`[hbm_tacz/nmesh] Loaded 0 nmesh binding(s)` (`NMeshBindingManager.java:59`) with guns
rendering as bare skeletons. (Gunpack JSONs are different — TACZ's loader is lenient, §2.2
step 5.) Rule of thumb: **comments allowed under `gunpack/`, forbidden under
`assets/hbm_m/nmesh_bindings/`.**

### 3.2 Field table

| Field | Type | Default | Semantics |
|---|---|---|---|
| `nmesh` | string (resource location) | required | The mesh to bind, e.g. `hbm_m:models/weapons/g3.nmesh`. Must exist under `assets/<ns>/models/weapons/` (parsed by `NMeshAssetManager`, which scans **all** namespaces for `*.nmesh` — `NMeshAssetManager.java:51`). Missing mesh → warn-once `Binding … references missing .nmesh asset …` (`NMeshGunModelInjector.java:80`) |
| `flip_v` | bool | **`true`** | `.nmesh` stores raw OBJ V (bottom-up); MC textures sample V=0 at the top. `true` emits `v' = 1 − v` (`NMeshBedrockCube.java:140`). Every HBM weapon texture wants `true` |
| `auto_cancel_bone_pivot` | bool | **`true`** | Rest-pose compensation, deep dive in §3.3b |
| `base_transform` | object `{translate, rotate, scale}`, each `[x, y, z]` floats | identity | Whole-model alignment from nmesh space into geo space, deep dive in §3.3a. Arrays must have exactly 3 numbers (`NMeshBinding.java:111-122`) |
| `bones` | map *bone name* → list of entries | `{}` | Which sub-meshes attach to which geo bone. Bone must exist in the display skeleton (`model.getNode`, warn-once otherwise, `NMeshGunModelInjector.java:85-91`); sub-mesh name must match an OBJ object/group name in the mesh (warn-once listing available names, `:113-117`). A name matches all sub-meshes with that name (fan-out) |

Per-entry (`SubMeshBinding`, `:126-153`): `submesh` (required string) plus optional
`translate` / `rotate` / `scale` triples (default `[0,0,0]` / `[0,0,0]` / `[1,1,1]`) —
part-level corrections applied **inside** the bone's frame, after rest-pose cancellation and
before `base_transform` (order below).

### 3.3 Coordinate semantics and the two big knobs

**Spaces.** *nmesh space* = HBM weapon OBJ space: y-up, muzzle along `+z`, ~pixel units
(measured: g3 `Flash_Hider` z ∈ [11.25, 12.5], stock to z = −12.5 → muzzle `+z`;
stg77 `Barrel` z max = 8.0). *Geo space* = the Bedrock 1.12 geometry space of the display
skeleton (y-up, pivots in [0, 32]); TACZ gun models face **−z** (donor `muzzle_flash` pivot
z = −15.925 for `hk_g3`, −21.325 for `ak47` — both read from the geo files this session).

**(a) `base_transform`.** The global nmesh→geo alignment. Per vertex:

```
p_geo = base_T · base_R · base_S · entry_T · entry_R · entry_S · p_nmesh
```

— per-sub-mesh transform **first**, then `base_transform` (`NMeshBinding.java:50-53`).
Rotations are degrees in **Z → Y → X order** (TACZ's `BedrockPart`/cube-bake convention).

Typical content: **`rotate [0, 180, 0]`** — required because HBM muzzle is `+z` and TACZ is
`−z` (this is what both MVP bindings ship). Two consequences to know while tuning:
- 180° y-rotation **mirrors X**: asymmetric parts (charging handle, selector) land on the
  mirrored side. HBM and the donors agree on `+x = left` (stg77 `Handle` x ∈ [0.34, 1.06] =
  AUG's left charging handle; donor `hk_g3` handle pivot x = +0.37461 = G3's left charging
  handle), so the sides swap — if a handle ends up on the wrong side, that is this mirror,
  not a binding bug.
- **Scale** should match donor anchor distances. Measured raw material: HBM g3 spans
  z ≈ −12.5…+12.5 (muzzle tip +12.5), stg77 z ≈ −8…+8; donor muzzle anchors sit at
  z = −15.925 (`hk_g3`) / −21.325 (`ak47`). Phase 5's estimate for matching donor
  hand/ADS anchors is **scale ≈ 1.4 (g3) / 2.2 (stg77)** — but these are *estimates*, never
  confirmed in game; the shipped bindings keep `1.0` until §3.4 tuning. A side effect of
  `rotY180` at scale ≠ 1: mirroring + scaling compose fine, but any per-bone `translate`
  tuned at scale 1 must be re-derived after changing scale.

The engine also **compensates triangle winding**: the geo→render mapping `(x, 24−y, z)` is a
reflection (det = −1) which reverses CCW winding, so the bake flips every triangle — unless
your configured entry+base transform is itself mirroring (determinant < 0), in which case the
flip is skipped (`NMeshBedrockCube.java:104`). Practical upshot: don't add mirror scales
(`scale [-1, 1, 1]`) casually; they invert the culling behavior and swap the side-mirror
correction.

**(b) `auto_cancel_bone_pivot`.** In the `.nmesh`, every sub-mesh is exported in
**whole-weapon model space** (magazine vertices are already at the magwell, not centered at
origin). A `BedrockPart` bone, however, renders its cubes in its **local** frame: the bone
chain's rest transforms (pivot offsets + initial rotations) would double-apply. With
`auto_cancel_bone_pivot: true` (default) the injector composes
`BedrockPart.translateAndRotateAndScale` over root→bone using **rest values only** and
inverts it (`NMeshGunModelInjector.computeRestPoseInverse`, `:138-161`); the cube bake then
applies `M_rest⁻¹ · (x, 24−y, z) · base_transform · entry · p_nmesh` per vertex — the
`(x, 24−y, z)` step being the exact inverse of TACZ's Bedrock y-flip conversion. Net effect:
at rest pose the sub-meshes assemble seamlessly into the exact whole gun in geo coordinates,
while animation deltas (which TACZ writes into `offsetX/Y/Z` + `additionalQuaternion`, both
identity at rest) rotate them around the bone's **true pivot** — i.e. the magazine swings on
the magwell, the bolt on its pin, exactly like donor cubes.

Leave it `true` unless you author coordinates directly in the bone's local frame. With
`false`, the bake trusts you: coordinates are taken as already bone-local, and any rest-pose
pivot/rotation of the bone chain is your problem.

**Why cubes, not a functional renderer.** The injector only appends to `part.cubes`
(`NMeshGunModelInjector.java:120`) and never sets `functionalRenderer` — a non-null
functional renderer suppresses the bone's cubes **and all children**
(TACZ `FunctionalBedrockPart.java:68-90`, checked here). Binding through
`cubes` is what keeps `magazine`, `bolt`, `muzzle_flash`, hand bones etc. rendering.

**Binding resolution order** (`NMeshGunModelInjector.resolveBinding`, `:177-187`): the
injector knows the **display id**, so it walks `TimelessAPI.getAllCommonGunIndex()`, collects
(sorted, deterministically) every gun id whose index POJO points at that display, and takes
the first that has a binding; a binding filed under the **display id** is accepted as a
fallback (shared displays). In practice: file as `<gun_id>.json`.

### 3.4 Live-tuning workflow (`/tacz reload`)

Both managers are vanilla client reload listeners (`ClientSetup.java:34-38`), so they re-run
on `F3+T` **and** on `/tacz reload` — TACZ's reload command runs a full
`Minecraft#reloadResourcePacks()` (`ClientAssetsManager.reloadAllPack`,
`C:/Projects/TACZ/src/main/java/com/tacz/guns/client/resource/ClientAssetsManager.java:207-216`)
and then rebuilds every `GunDisplayInstance`/`BedrockGunModel`, which re-fires
`MixinGunDisplayInstance` → injection. Bindings, meshes and models hot-swap together.

1. Edit `src/main/resources/assets/hbm_m/nmesh_bindings/<gun_id>.json` (dev workspace: no
   re-deploy needed — the file is read from the mod resources each reload; in a production
   install it lives in the mod jar, so iterate via a resource pack override or a rebuilt jar).
2. Run `/tacz reload` (or `F3+T`).
3. Read the log — the messages you will actually use:
   - `[hbm_tacz/nmesh] Loaded N nmesh binding(s)` — N must equal your binding file count; `0`
     ⇒ strict-JSON parse failure (comments!) or wrong directory
     (`NMeshBindingManager.java:59`).
   - `[hbm_tacz/nmesh] Loaded N .nmesh model(s) (M failed)` — corrupt/missing binary
     (`NMeshAssetManager.java:61-62`).
   - `[hbm_tacz/nmesh] Binding … references bone "X" that the gun model does not contain; skipped`
     — bone-name typo (`NMeshGunModelInjector.java:87-89`).
   - `[hbm_tacz/nmesh] Binding … wants sub-mesh "Y" but … has no matching sub-mesh (available: …)`
     — sub-mesh typo; the available names are printed (`:113-117`).
   - Each warning fires **once per key** (`warnOnce`, `:189-193`), so a stale warning can
     persist in the log after you fixed it — check the newest reload block.
4. Knob order (fix the cheapest global error first):
   1. `base_transform.rotate` — muzzle direction (start `[0, 180, 0]`), X-mirror check.
   2. `base_transform.translate` — align receiver origins.
   3. `base_transform.scale` — match donor size / hand & ADS anchors.
   4. per-entry `translate/rotate/scale` — part-level fixes only (e.g. seat the magazine).
5. **If an edit doesn't appear after one `/tacz reload`:** TACZ builds gun models lazily
   (`ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD`, consulted at
   `C:/Projects/TACZ/src/main/java/com/tacz/guns/client/resource/GunDisplayInstance.java:137,157,178`).
   A model lazily built during the reload window can bake one-reload-stale binding values —
   run `/tacz reload` once more or re-equip the gun. Setting
   `EnableLazyClientAssetLoad` to `false` in TACZ's client config makes reloads fully
   synchronous at the cost of startup time.

**What `/tacz reload` does *not* cover:** files under `gunpack/` are read from the exported
`<gamedir>/tacz/gunpack` copy — after editing pack JSONs in the repo, re-export (§1.4) or
edit the exported copy in place (that one *is* hot-reloaded by `/tacz reload`).

---

## 4. `ExternalGunRenderBridge` — the SPI contract for future renderer integrations

Source of truth: `C:/Projects/HBM-Modernized-agents/src/main/java/com/hbm_m/client/render/weapon/bridge/ExternalGunRenderBridge.java`
(javadoc in German, signatures quoted below verbatim).

### 4.1 Shape and guarantees

- **Public abstract class, not an interface.** The holder field is `private static` and the
  holder is initialized with a nested private no-op (`None`, `:85-119`):
  ```java
  public static ExternalGunRenderBridge get() { ... }                       // :31 — never null
  public static void register(ExternalGunRenderBridge bridge) { ... }       // :36 — register(null) resets to the no-op
  ```
  This makes "no bridge registered → bit-identical pre-bridge behavior" structural: all 12
  wiring sites call `ExternalGunRenderBridge.get().…` directly with **no null checks**, and
  the no-op's answers (`isHandled=false`, `resolveCustomRenderer=fallback`,
  `renderByItem=false`, `onGunAnimation=no-op`, `shouldSuppressNativeRecoil=false`,
  `shouldSkipNativeVboBake=false`) are exactly the pre-bridge behavior. A future integration
  can also call `register(null)` to unclaim everything.
- **Loader-agnostic.** Only vanilla types (`PoseStack`, `BlockEntityWithoutLevelRenderer`,
  `MultiBufferSource`, `ResourceLocation`, `ItemDisplayContext`, `ItemStack`) plus
  `com.hbm_m.render.anim.AnimationEnums.GunAnimation`. No Forge/NeoForge/TACZ imports — the
  class compiles on both Stonecutter targets, and the wiring in
  `ItemGunBaseNT`/`GunClientEventsForge` is duplicated in the `//? if forge {` / `//?} elif
  neoforge` twin blocks (the shared import sits above the gate so both branches see it).
- **Late registration works.** The holder is read dynamically per call, so registering
  during (or after) mod construction immediately affects every site.

### 4.2 The six methods

| Signature (verbatim) | Fired from | Contract for implementers |
|---|---|---|
| `public abstract boolean isHandled(ItemStack stack)` | `ItemGunBaseNT.java:406` (arm pose), `GunClientEventsForge.java:29,40,83`, `GunClientHooks.java:160,226,260`, `MixinItemInHandLayer.java:41` | `true` ⇔ "I own rendering/HUD/FOV for this stack". Must be **cheap** (called per frame, several times), must accept any stack (non-guns, empty, null-safe) and return `false` for everything you don't fully support. The addon's implementation: `ItemGunBaseNT && TimelessAPI.getGunDisplay(stack).isPresent()` (`HbmGunRenderBridge.java:50-57`) |
| `public abstract BlockEntityWithoutLevelRenderer resolveCustomRenderer(BlockEntityWithoutLevelRenderer fallback)` | `ItemGunBaseNT.java:396-400` | Return your BEWLR (implement SBM `IFPGeoItemRenderer` via `GunItemRendererWrapper` to get SBM first-person + draw animations for free), or `fallback` when you don't want the item at all. Returning the wrapper **unconditionally** (as the addon does, `HbmGunRenderBridge.java:60-67`) additionally makes TACZ's 3P `TickAnimationEvent` and SBM's `FirstPersonRenderHandler` treat the gun like a native TACZ gun as soon as a display resolves; the per-stack fallback then lives inside the wrapper's `renderByItem` (`HbmTaczGunItemRenderer.java:47-58`) |
| `public abstract boolean renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay)` | `GunItemRenderer.java:50` | Return `true` only if you actually drew the stack in this context (HBM exits immediately); `false` falls through to HBM's native draw. This is the 3rd-person/GUI/ground/item-frame hook |
| `public abstract void onGunAnimation(ItemStack stack, GunAnimation type, int receiverIndex, int gunIndex)` | `GunClientHooks.java:67` (main-hand only) | Translate HBM's `HbmAnimationPacket` enum (`CYCLE`, `CYCLE_DRY`, `RELOAD`, `RELOAD_CYCLE`, `RELOAD_END`, `JAMMED`, `INSPECT`, `EQUIP`, plus internal `ALT_CYCLE`/`SPINUP`/`SPINDOWN`) into your animation pipeline. See `HbmGunRenderBridge.onGunAnimation` (`:127-191`) for the reference mapping, including which events must **not** be re-triggered |
| `public abstract boolean shouldSuppressNativeRecoil(ItemStack stack)` | `GunClientHooks.java:158-165` | `true` = HBM skips applying its `recoilVertical/recoilHorizontal` camera kick this frame (your renderer owns the camera). HBM still decays the statics, so they don't accumulate |
| `public abstract boolean shouldSkipNativeVboBake(ResourceLocation modelLocation)` | `WeaponRenderRegistry.java:273` | `true` = HBM skips pre-compiling its native `WeaponPartVboRenderer` VBOs for that model (VRAM). **Hard rule**: never return `true` for `casings`, grenades, projectiles or any non-weapon model — the addon gates on `models/weapons/<base>.*` **and** the existence of a matching pack gun index (`hbm_m:gun_<base>`) so casings/grenades can never match (`HbmGunRenderBridge.java:99-120`) |

### 4.3 Implementer checklist (for a future renderer integration)

1. Register once, client-dist only, from your own compat bootstrap — never from a
   `@Mod.EventBusSubscriber` that would load `com.hbm_m`-referencing classes unconditionally
   (see `HbmAimSyncListener.java:29-31` for the reasoning).
2. Keep `isHandled` stateless and allocation-free; derive everything from the stack.
3. Make every "claim" condition go through the same predicate (`isHandled`), so renderer,
   HUD, FOV, recoil and bake-skip claims can never disagree per stack.
4. Respect the GUI: HBM's hotbar HUD (ammo/durability) stays; if you replace it, do it in
   your renderer, not by widening the SPI.
5. In a Stonecutter multiloader host like HBM, wire your call sites in **both** `//? if
   forge {` and `//?} elif neoforge` twin blocks and keep the import above the gate (the
   existing sites are the template).
6. Any class importing `com.hbm_m.*` on the addon/integrator side must live behind the mod
   presence guard (`ModList.get().isLoaded("hbm_m")`) — mirror `com.hbm_tacz.compat.hbm.*`.

---

## 5. Known caveats and unverified items

Aggregated from the phase notes and cross-checked against the code where possible. Items
marked **(checked here)** were re-verified in this session; the rest are carried from the
phase notes.

### 5.1 Build & verification status

- **No Gradle build has ever covered the Phase 1–5 Java changes.** Phase 1's task forbade
  Gradle (checks were javac-based: the SPI compiles clean against stubs; the six wired HBM
  files were parse-checked and type-checked against the compiled bridge; a simulated
  stonecutter neoforge transform was parse-checked). Phases 3–4 similarly ran javac-only
  type-checks (Phase 3: 9 files; Phase 4: 16 addon files + the 2 SPI sources, exit 0, with
  the Mixin AP active). The only full-classpath gates ever executed are the Phase 2 scaffold
  gates (`:1.20.1-forge:compileJava` → BUILD SUCCESSFUL, 4 runs, plus `:1.20.1-forge:build`)
  — which at that time compiled only the 3 scaffold classes. **Phase 6 must run**
  `:1.20.1-forge:compileJava` and `:1.21.1-neoforge:compileJava` on both repos, then a
  `runClient` smoke test. (Checked here: the HBM working tree is dirty with 384+ unrelated
  pre-existing changes; do not revert or commit them.)
- **The empty-config refmap workaround is no longer a no-op-verified path.** With real
  mixins now present in both configs, the Mixin AP should write the real
  `hbm_tacz.refmap.json` and `ensureMixinRefmap` should do nothing — but that handoff has
  only been reasoned about, never observed in a build (Phase 2 verified only the zero-mixin
  placeholder case).
- **Dev-run risk:** `modRuntimeOnly` uses the TACZ `-all` fat jar; in MDG dev runs Forge must
  resolve the nested jarJar entries (SBM 2.6.3 is obfuscated). If `runClient` hits
  nested-jar/remap trouble, switch the runtime dep to the plain reobf jar plus explicit
  runtime deps for luaj/commons-math3/bcel (Phase 2 note).
- **Compile-env note (Windows):** the JetBrains Runtime javacs on this machine crash with a
  `TransTypes` ClassCastException when compiling this file set without
  `net.minecraftforge.api.distmarker.Dist` on the classpath; Temurin javac 25 reports the
  real error, and the real MDG classpath contains `Dist` (via `mergetool-1.1.5`), so Gradle
  builds are unaffected. The addon's `build/moddev/artifacts` jars were once regenerated
  vanilla-only by an interrupted run; MDG regenerates them on the next build.

### 5.2 Runtime behavior — designed but never executed

- **No `runClient` has ever run with the bridge registered.** Everything in §1.3 flows
  A–D is implementation-verified (code read) but not behavior-verified. Priority checks for
  the Phase 6 smoke test: first-person interplay between HBM's
  `GunClientEventsForge.onRenderHand` (early-return) and SBM's `FirstPersonRenderHandler`
  for *un-packaged* guns (`HbmTaczGunItemRenderer.renderByItem` no-ops on empty display,
  HBM's hook returns `false` — same shape TACZ uses, but unconfirmed); ADS sync; reload
  animation trigger; 3rd-person model (VBO mixin) and muzzle position.
- **Visual transforms are placeholders.** Both MVP bindings ship
  `base_transform = rotate [0,180,0], scale [1,1,1]` with identity per-bone transforms.
  Phase 5's estimates (`scale ≈ 1.4` g3, `≈ 2.2` stg77) and the whole muzzle-flash /
  `iron_view` alignment are **unverified in game** — that tuning is exactly what §3.4 is
  for. Scale-hint raw numbers checked here: HBM g3 z-extent ≈ −12.5…+12.5, stg77 ≈ −8…+8;
  donor muzzle anchors z = −15.925 (hk_g3) / −21.325 (ak47).
- **Reload animation vs. server state can desync visually.** The pack's
  `reload.feed`/`cooldown` follow the reused donor animation lengths (g3: empty 2.7/3.15,
  tactical 1.55/2.05; stg77: 2.25/2.6, 1.55/2.0) while HBM's server reload is
  50 ticks / 46 ticks — commented in `gun_g3_data.json`/`gun_stg77_data.json`. In hybrid
  mode the SEDNA server timing is authoritative; only the played clip follows the pack.
- **Reload/inspect sounds.** All four per-gun display sound ids
  (`reload_empty`, `reload_tactical`, `inspect`, `inspect_empty`) have no direct ogg in the
  donor pack either — actual sounds come from the donor animations' `sound_effects`
  keyframes; TACZ falls back to sound-event-then-silence for the map entries. Author
  accordingly: the donor animations' sound keyframes can overlap HBM's server-side reload
  orchestras in hybrid mode.
- **Shoot sounds** are intentionally never played by the bridge (SEDNA plays server-side);
  the `shoot`/`silence` display entries matter only in standalone mode.

### 5.3 Functional gaps (documented, not hidden)

- **Belt-fed guns report 1 round.** `getCurrentAmmoCount` calls
  `MagazineBelt.getAmount(stack, null)`, whose null-inventory path returns the
  `EntityAIFireGun` fallback of 1 — the Lua ammo display for e.g. the minigun is wrong until
  a per-gun pack entry or an inventory-aware read is added (noted in
  `MixinItemGunBaseNT.java:232-237`).
- **TACZ refit attachments install only when the pack gun data defines
  `allow_attachments`** (`SlotAdapterHelper` + `allowAttachmentType`); the MVP maps HBM
  weapon-mod slots to `scope/muzzle/stock/extended_mag` and ships matching
  `tacz_tags/attachments/allow_attachments/<gun_id>.json`.
- **Static parts:** g3 `Bullet`/`Plug` and stg77 `Bullets` render always (no visibility
  rules in the binding schema, §2.3). g3 `Scope`/`Silencer` unbound by design.
- **StG77 `text_show` dropped:** the donor ak47's text anchor bone
  (`ammo_count_text_show_pos`) doesn't exist in the donor geo (grep = 0), so the block and
  its lang key were removed rather than shipped unverified.
- **No HBM-equivalent stats** for `weight`, `put_away_time`, `melee`, `sprint_time`
  (donor-derived values); no `bullet life/friction/knockback` calibration (TACZ POJO
  defaults apply; `knockbackMult 0.1` noted); aim inaccuracy 0.15 (TACZ convention) instead
  of HBM's exact 0, because HBM's 0 assumes its wear system.
- **Ammo display id collision risk:** `hbm_m:r556_display` is shared by four index entries;
  per-variant displays would need distinct ids if visuals must differ.

### 5.4 Documented deviations from the master spec

- **Method names** `renderByItem`/`onGunAnimation` (spec §1.1) instead of
  `tryRenderByItem`/`notifyGunAnimation` (spec §1.2) — §4 of this guide.
- **`onRenderFrame` wiring** (recoil suppression consumer) is an addition beyond the §1.2
  six-site list; without it the sixth SPI method would be dead.
- **`flip_v` defaults to `true`** and **`auto_cancel_bone_pivot` defaults to `true`** (the
  spec example sets the former explicitly and only names the latter); transform order,
  ZYX rotation order and the bake-time winding flip are Phase 3 definitions of what the
  spec left open (all documented in `NMeshBinding.java` javadoc).
- **Parser hardening:** index count must be a multiple of 3 (the quad emitter walks
  triples) — a constraint the original HBM reader never enforced.
- **Idempotence:** injection is guarded by `WeakHashMap<BedrockGunModel,Boolean>` and
  warn-once logging (`NMeshGunModelInjector.java:63-64`).
- **Gun-pack path:** the master spec's Phase 2 sketch registered
  `/assets/hbm_m/custom/hbm_gunpack`; the shipped code registers `/gunpack`
  (`HbmTacz.java:50`) matching the Phase 5 layout `src/main/resources/gunpack/` (exported
  folder name = last path segment, `<gamedir>/tacz/gunpack`).
- **Input suppression beyond the spec:** the six TACZ input mixins are not in the master
  spec's task list but are required by the Mode-B pillar "HBM retains 100 % of input/logic"
  — every TACZ input handler keys on `instanceof IGun`, which `MixinItemGunBaseNT` makes
  true for `ItemGunBaseNT`; without them TACZ would double-fire on LMB, double-reload on R,
  aim on RMB (HBM's fire-mode toggle) and fight the fire-mode selector.
- **Deliberately ungated TACZ keys** (harmless or bonus in hybrid mode): `InspectKey`
  (bound to **H** — TACZ `InspectKey.java:18-22`, verified in the TACZ source this session;
  HBM's inspect is R on a non-reloadable gun and reaches TACZ through the bridge as
  `INPUT_INSPECT`, so the two paths are on different keys and coexist — a bonus, but worth
  confirming in the smoke test), `CrawlKey`, `ZoomKey` (no-op without TACZ scope
  attachments), `InteractKey` (O-key ammo crates), `RefitKey` (TACZ refit UI; attachments
  install as NBT through the `GunItemDataAccessor` defaults).

### 5.5 Isolation invariants to keep (checked here)

- `com.hbm_m` imports exist **only** in `com.hbm_tacz.compat.hbm.*` and
  `com.hbm_tacz.mixin.hbmcompat.MixinItemGunBaseNT` (the remaining grep hits in
  `HbmTacz`/`HbmCompatBootstrap`/`HbmTaczMixinPlugin` are javadoc text). Phase 3's
  `render.nmesh` + `client` mixins import TACZ classes only.
- The six TACZ-class client mixins live in `hbm_tacz.json` (no plugin) and never touch
  `com.hbm_m`; the eight `hbmcompat` mixins live behind `HbmTaczMixinPlugin`.
- `MixinItemGunBaseNT` references only common (non-client) classes, so it transforms safely
  on dedicated servers where the extra `IGun` implementation is inert; the input mixins
  target `@OnlyIn(CLIENT)` TACZ classes and are listed in the config's `client` section.

---

## 6. Phase 7 — pivot relocation, bone overrides, animation multipliers, the F8 Live Tuner, hybrid dedup

Phase 7 fixed the two remaining MVP problems — *moving parts detached at rest and drifting in
arcs* (spec `C:/Projects/HBM-TACZ-Addon/1.md` §0) and *blind JSON-coordinate tuning* — and closed
the hybrid-mode creative-tab item duplication (spec `C:/Projects/HBM-TACZ-Addon/2.md`). Four
deliverables:

1. **Part A (engine)** — true sub-mesh pivot recentering (`auto_pivot_to_submesh` +
   `pivot_offset` + child compensation), `bone_overrides` for arbitrary bones, per-bone
   animation multipliers (`anim_translate_scale` / `anim_rotate_scale`), all rest offsets back to
   `[0,0,0]`, and moving parts rebound to the *actually animated* donor bones (§6.1).
2. **Part B (tuner)** — the in-game Live Tuner, `F8` / `/hbm_tacz tune` (§6.2).
3. **STEP 5 (dedup)** — eradication of synthetic `tacz:* / id "hbm_m:*"` duplicates in hybrid
   mode (§6.4).
4. **This section** — spec checklist item 4.

**Verification status of §6.** Every `file:line` below was re-read in the session that wrote this
section (2026-11-18): all 43 addon sources under `C:/Projects/HBM-TACZ-Addon/src/main/java`
(counted by `find`), the two shipped bindings, both mixin configs, the tuner lang file, the
addon `gunpack/assets/hbm_m/` directory listing, and the cited TACZ 1.2.0 / HBM sources
(`BedrockModel.java`, `ObjectAnimationRunner.java`, `AnimationController.java`,
`AnimationStateContext.java`, `ObjectAnimation.java`, `AbstractGunItem` / `ModernKineticGunItem` /
`AmmoItem` / `AttachmentItem` / `AmmoBoxItem` / `init/ModItems` / `init/ModCreativeTabs` /
`compat/jei/GunModPlugin`, HBM `ItemGunBaseNT.setIsAiming`). Checks **run by the implementing
agents but not re-run here** (per the phase notes): a full-classpath javac type-check of all 43
addon sources (0 errors, JDK 17 Temurin), a functional binding round-trip test driven by the real
`gun_g3.json`/`gun_stg77.json` (parse → Gson tree → simulated edits → parse →
`resolveBoneConfig` → double round-trip, all passed), and `python verify_relocation.py` (a numpy
re-implementation of the relocation math driven by the real geo skeletons + nmesh AABBs + the
rewritten bindings: rest world positions invariant, relocated pivots land exactly on `P_outer`,
child compensation exact, baked sub-mesh centers rotate around their own origin — ALL CHECKS
PASSED). **Still never run:** any Gradle `compileJava`/`build` of the Phase-7 code (task rules
forbid it; the workflow compiles and gates — spec checklist item 1 is delegated) and any
`runClient`/in-game test; every UI/rendering behavior in §6.2 is implementation-verified only
(§6.5).

### 6.1 Pivot relocation (`auto_pivot_to_submesh`, `pivot_offset`) and why rest offsets are `[0, 0, 0]` again

#### 6.1.1 Schema additions (fully backward compatible)

`NMeshBinding.java` gained three optional maps (fields `:128-136`, POJOs `:171-208`, validation
`:415-431`), and every knob is additionally accepted **per-entry**, directly inside
`bones.<bone>[]` entries (`:225-237`) — the spec's "support both cleanly". Resolution order is
**entry value > `bone_configs` value > default** (`resolveBoneConfig`, `:324-374`; the first
non-null entry wins).

| Field | Applies to | Semantics |
|---|---|---|
| `bone_overrides.<bone>.{translate,rotate}` | any bone of the skeleton | rest-pose edit, authored in **geo space (y-up)**; shifts the bone rigidly with its whole subtree |
| `bone_configs.<bone>.auto_pivot_to_submesh` | bound bone | relocate the bone's rest pivot to the bound sub-meshes' union AABB center (§6.1.2) |
| `bone_configs.<bone>.pivot_offset` | bound bone | geo-space offset added to the computed pivot — fine-tunes the hinge **without** moving the mesh at rest |
| `bone_configs.<bone>.anim_translate_scale` / `anim_rotate_scale` | bound bone | per-axis multipliers on the bone's animation deltas (§6.3) |

Default for `auto_pivot_to_submesh`: **true for any moving bone that is not the main gun body**.
The injector's heuristic (`markBodyBones`, `NMeshGunModelInjector.java:285-298`): a bound bone
with another bound bone below it in the hierarchy is the body → default `false`. Both shipped
bindings also set it explicitly (`false` on `G3_default_body`/`aug_default`, `true` on the moving
parts), so for the MVP guns the heuristic never actually decides anything.

#### 6.1.2 The relocation algorithm (`NMeshGunModelInjector.relocatePivots`, `:306-361`)

For each bound bone whose resolved flag is true (processed **top-down by chain depth**, `:314`,
so a relocated parent's fresh rest matrix feeds the child's relocation and compensations compose
when both parent and child are bound):

1. Union AABB center of the bound sub-meshes in nmesh space `c_nmesh` (`:320`; union accumulated
   per entry in `collectBoundBones`, `:261-268`).
2. Into geo space through `base_transform * entry` — averaged over the entries (exact when they
   share transforms, the normal case), `:324-330` — then add `pivot_offset` → `P_geo` (`:331`).
3. Into TACZ outer-chain pixel coordinates (+Y down): `P_outer = (gx, 24 − gy, gz)` (`:334-336`)
   — the exact inverse of TACZ's `BedrockModel.convertPivot` and the same convention the cube
   bake uses.
4. Desired local translation inside the parent: `(newX, newY, newZ) = M_parent⁻¹ · P_outer`,
   where `M_parent` composes `BedrockPart.translateAndRotateAndScale` over root→parent at
   **rest pose only** (`computeRestPoseMatrix`, `:399-422`; animation deltas live in
   `offsetX/Y/Z` and `additionalQuaternion`, both identity at rest, so the rest composition is
   stable across animation).
5. Move the bone and record `Δ = new − old` local translation (`:345-348`).
6. **Child compensation:** every direct child gets `− R_B_rest⁻¹ · Δ` applied to its local
   position, `R_B_rest = Rz·Ry·Rx` of the bone's rest rotations (`buildRestRotation`, `:425-427`;
   JOML `rotationZYX` composes in the same Z·Y·X order TACZ's `translateAndRotateAndScale` uses)
   (`:351-359`) — so all descendants keep their exact world rest positions.
7. `restPoseInverse` is recomputed **after** relocation and overrides, then the cubes bake
   (`bake`, `:363-387`) — so `NMeshBedrockCube`'s
   `M_rest⁻¹ · (x, 24−y, z) · base_transform · entry · p_nmesh` still lands every vertex at its
   monolithic geo-space position.

Consequence (proven numerically by the relocation verifier): because `M_parent⁻¹ · P_outer` is
exactly the bone's new local translation, the baked sub-mesh AABB center sits **on the bone's
local origin** — an `additionalQuaternion` rotation therefore spins the part around its own
center (+ `pivot_offset`) instead of orbiting a foreign donor pivot. That orbit was the "drift"
root cause: donor skeletons carry pivots far from the HBM part's hinge (spec 1.md §0 example:
`bolt` pivot `[-2.19, 16.29, 42.375]` in the old g3 geo tree).

#### 6.1.3 Why the shipped bindings are `[0, 0, 0]` now

With `auto_cancel_bone_pivot: true` the bake already assembles the whole `.nmesh` in geo space
(§3.3b). Any non-zero per-entry `translate` therefore physically rips the part out of its socket
at rest — exactly what the MVP shipped (`Magazine [-0.174, -1.662, 0.904]`, `Handle
[-0.785, -1.078, 1.647]`, spec 1.md §0 diagnosis). Phase 7 zeroes every entry and moves the
*hinge* instead of the *part*. Current shipped bindings
(`C:/Projects/HBM-TACZ-Addon/src/main/resources/assets/hbm_m/nmesh_bindings/gun_g3.json` /
`gun_stg77.json`, both read in full this session — every `translate`/`rotate` is `[0,0,0]`,
every `scale` `[1,1,1]`):

| Gun | Bone binding (old → new, reason) | `auto_pivot_to_submesh` |
|---|---|---|
| g3 | `gun_body` → **`G3_default_body`** — the real animated body bone (inspect rot+pos); the old dummy `gun_body` is never animated | `false` |
| g3 | `magazine` → **`magazine`** (kept; the bone itself has no `hk_g3` keyframes but animated ancestors `mag_and_bullet`/`mag_and_l` propagate to it) | `true` |
| g3 | `bolt` → **`bolt`** (kept; deepest animated bone of the bolt chain — catches `G3_bolt` position keyframes as ancestor motion plus its own `shoot` keyframes) | `true` |
| g3 | `charging_handle` (dummy) → **`handle`** — animated: rotation in `reload_empty`/`inspect`/`inspect_empty`, parent `pull` adds position | `true` |
| g3 | `safety` (dummy) → **`selector`** — animated in `static_semi`/`static_auto`/`switch_semi`/`switch_auto` | `true` |
| stg77 | `aug_default` (kept, body) | `false` |
| stg77 | `magazine` (kept; own `inspect_empty` keyframes + `mag_and_lefthand` ancestor) | `true` |
| stg77 | `aug_bolt` (kept; `static_bolt_caught`/`shoot`/`inspect`/`inspect_empty`) | `true` |
| stg77 | `aug_charge` (kept; `reload_empty` rot 55/pos 60, `inspect*`) — binds `Handle` + `Lever` | `true` |
| stg77 | `safety` (kept **per the ask**; the aug donor animation has **no keyframes** on `safety`, so its relocation is a harmless no-op at runtime) | `true` |

The body entries now also carry the statically bound sub-meshes (`Bullet`, `Plug`, `Bullets`, …)
with zero offsets — flush *because* relocation moves pivots, never vertices. The bindings also
ship **tuned** `base_transform` values now (g3: translate `[-0.311, 11.75, 9.3125]`, uniform
scale `1.685`; stg77: translate `[0.155, 7.001, 1.43]`, uniform scale `2.3033`) — the §2.3 and
§5.2 statements about placeholder `base_transform` and the scale-1.4/2.2 estimates are
**superseded**; §3.4's `/tacz reload` workflow remains the non-tuner fallback path.

#### 6.1.4 Drift-free live re-injection

`cleanup` (`NMeshGunModelInjector.java:172-187`) walks every bone reachable from
`model.getShouldRender()`, captures the pristine loader-written rest values the first time it
sees a bone (`MixinBedrockPart.hbmTacz$captureOriginalRestStateIfNeeded`,
`MixinBedrockPart.java:99-110` — capture strictly precedes the first mutation; the only runtime
writers of `BedrockPart` rest fields in TACZ 1.2.0 are the model loaders), then restores them,
resets the multipliers to 1.0 and strips injected `NMeshBedrockCube`s.
`reinjectLive(displayId, model)` (`:148-150`) is therefore idempotent and frame-cheap — the
tuner calls it on every slider tick without accumulating offsets. The old `INJECTED`-map
double-inject guard was replaced by always-clean-then-apply; a weak `TOUCHED` set (`:106`) only
skips the cleanup walk for models that never had a binding.

#### 6.1.5 `bone_overrides` vs pivot relocation — know which one you need

- `bone_overrides` **shifts a bone and everything under it** (`applyBoneOverrides`,
  `:190-210`): `part.x += tx, part.y -= ty, part.z += tz` (the negated Y converts geo-space
  y-up into the outer chain's +Y-down pixel coordinates), degrees added to `xRot/yRot/zRot`.
  Use for anchor bones with no sub-mesh: `iron_view`, `idle_view`, `lefthand_pos`,
  `righthand_pos`, `muzzle_flash`, `shell`.
- Pivot relocation **moves only the hinge** — at rest nothing shifts (child compensation plus the
  post-relocation bake keep the sub-mesh world position identical). Use for animated part bones.

### 6.2 The F8 in-game Live Tuner

Sources: `com/hbm_tacz/client/tuner/` (10 classes). The panel replaces the Blockbench round-trip:
edit → same-frame render → save back to source.

#### 6.2.1 Activation

| Input | Wiring |
|---|---|
| `F8` keybind `key.hbm_tacz.open_tuner`, category `key.categories.hbm_tacz`, `KeyConflictContext.IN_GAME` | `TunerClientSetup.ModBus.onRegisterKeyMappings` (`TunerClientSetup.java:59-68`); labels in the new `src/main/resources/assets/hbm_tacz/lang/{en_us,ru_ru}.json` (read this session) |
| `/hbm_tacz tune` client command | `TunerClientSetup.onRegisterClientCommands` (`:140-160`; `RegisterClientCommandsEvent` is a GAME-bus event on Forge 1.20.1) |
| Open gate | `TunerState.openFor` (`TunerState.java:89-114`): main-hand stack must pass `NMeshGunModelInjector.isNmeshBackedGun` (keys on the gun-id binding — works in **both** runtime modes, §6.5) and the display model must be loaded; failure → action-bar message |

Close: `Esc`, `[Close]`, or the defensive net when the screen vanishes (`TunerClientSetup.java:91-95`).
`TunerState.close` (`TunerState.java:168-194`) releases the ADS lock, hides gizmos, restores the
scrubber's hijacked tracks and drops the live binding override.

#### 6.2.2 Screen behavior

`NMeshInGameTunerScreen`: `isPauseScreen() == false` (`:91-93`) — game and animations keep
running; no vanilla dark tint (`renderBackground` no-op, `:96-99`); a semi-transparent 210 px
panel on the left edge (`:52-53`, `:437-439`), scissor-clipped and **scrollable** (hand-rolled
widgets in `TunerWidgets` — vanilla `AbstractWidget` can't express wheel-over-slider plus scroll;
the 4 tabs carry ~40 controls); a 1 px cyan crosshair hairline at screen center for `iron_view`
alignment (`:492-498`). Wheel over a slider steps the value ±0.01 (Shift = coarse ±0.1), wheel
elsewhere scrolls the panel (`:582-595`; `TunerWidgets.Slider.mouseScrolled`,
`TunerWidgets.java:99-105`).

#### 6.2.3 The four tabs (spec STEP 4.3)

| Tab | Controls |
|---|---|
| **Base** (*Base & Sights*, `NMeshInGameTunerScreen.buildBaseTab`, `:129-156`) | `base_transform` translate/rotate rows + uniform scale; `[ADS Lock: ON/OFF]` button right above `iron_view`; `iron_view` / `idle_view` override groups (T+R); `muzzle_flash` / `shell` groups (translate-only); `[Test Shot / Flash]` — triggers `INPUT_SHOOT` on the donor state machine and posts a client `GunFireEvent` (muzzle flash + FP sway + camera recoil; `:411-428`) |
| **Hands** (`:172-176`) | `righthand_pos` / `lefthand_pos` override groups (T+R) |
| **Bones** (*Bones & Pivots*, `:178-264`) | `[Show Pivots]` gizmo toggle; bound-bone cycler `< [bone (submeshes)] >`; `[Auto Pivot to SubMesh]` toggle; `pivot_offset` X/Y/Z; `Anim T scale` / `Anim R scale` sliders (0.0–3.0); per-entry rest-offset rows + `[Zero Rest Offset]`; `[Reset Bone Config]`; sub-mesh → bone **rebind** cyclers + `[Bind]` / `[Unbind]` |
| **Time** (*Timeline & Keyframes*, `:266-330`) | donor clip cycler with length readout; `[Run Clip & Freeze]`; `[Play]`/`[Pause]`; timeline slider `0.00s … clipLength`; pose-keyframe recorder (bone cycler, KF Pos/KF Rot sliders, `[+ Record Keyframe at t]`, `[Delete Keyframe]`, `[Export Custom .animation.json]`) |

Every binding edit flows: mutate the in-memory Gson tree → `NMeshBinding.parse` (validation for
free) → `NMeshBindingManager.putTunerOverride` (`NMeshBindingManager.java:152-158` — the live-map
entry `reinjectLive` reads; survives only until the next resource reload) →
`NMeshGunModelInjector.reinjectLive` (`TunerBindingEditor.apply`,
`TunerBindingEditor.java:113-128`; invalid edits are rejected with a status message, never
crash). `[Reset]` buttons restore whole subtrees from the session-start copy
(`TunerBindingEditor.java:49-51`, `:163-174`).

#### 6.2.4 ADS lock (spec 4.2.2)

`[ADS Lock: ON]` is re-asserted **every client tick** while the tuner is open
(`TunerClientSetup.applyAdsLock`, `:102-115`): `IClientPlayerGunOperator.fromLocalPlayer(player).aim(true)`
(both modes) plus `TunerHybridBridge.setIsAiming(stack, true)` — in hybrid mode that consumer
writes HBM SEDNA's `aiming` NBT via `ItemGunBaseNT.setIsAiming` (`HbmTunerCompat.java:22-26`;
HBM method verified at
`C:/Projects/HBM-Modernized-agents/src/main/java/com/hbm_m/item/weapon/sedna/ItemGunBaseNT.java:476`),
because `HbmAimSyncListener` mirrors the NBT flag into TACZ aim per tick — flipping only TACZ's
mirror would lose the lock to HBM's own tick. The consumer is installed exclusively from
`HbmCompatClient.init()` (`HbmCompatClient.java:36`) behind the `hbm_m` guard and ships `null`
in standalone mode (`TunerHybridBridge.java:24-33`) — the tuner classes never import
`com.hbm_m.*`, preserving the §5.5 isolation invariant. On close the lock releases aim on both
paths (`TunerState.close`, `:170-181`) so the player is not stuck ADS.

#### 6.2.5 Pivot gizmos (spec 4.2.3)

`[Show Pivots: ON]` + the Tab-3 bone cycler select the bone. `TunerGizmoRenderer` (implements
TACZ `IFunctionalRenderer`) draws an RGB 3-axis cross at the selected bone's local origin and a
yellow cross at `muzzle_flash` through a dedicated no-depth-test `RenderType.lines()` clone
(`TunerGizmoRenderer.java:70-91`, `:107-133`). Hook — **no new mixin, no new config entries**
(`hbm_tacz.json` is untouched): `RenderHandEvent` (HIGH priority, fires before SBM's
first-person draw) calls the public `BedrockModel.delegateRender(renderer)` once per frame
(`TunerClientSetup.onRenderHand`, `:123-137`); the delegate list is drained at the end of
TACZ's render pass — registration verified at
`C:/Projects/TACZ/src/main/java/com/tacz/guns/client/model/bedrock/BedrockModel.java:70-72`,
drain at `:399-402` (also reached by the AR-accelerated path). The bone frame is replicated by
replaying `translateAndRotateAndScale` over root→bone (`:142-151`), so rest pose, animation
offsets, the additional quaternion and §6.3 multipliers all participate — the cross sits exactly
on the relocation target. Gizmos draw in **first person only** (spec scope).

#### 6.2.6 Timeline scrubber

Real TACZ 1.2.0 runner API (all verified in TACZ source this session): tracks are
engine-assigned ints with no public enumeration, so the scrubber probes
`controller.getAnimation(0..63)` (null beyond size — `AnimationController.java:46`); clips start
via `controller.runAnimation(track, name, playType, 0)` (`:79`); clip length =
`runner.getAnimation().getMaxEndTimeS()` (`ObjectAnimation.java:124`); `pause()` sets
`running=false, pausing=true` (`ObjectAnimationRunner.java:78-81`); the controller keeps
updating paused runners while `updateProgress` no longer advances `progressNs` — so
`pause() + setProgressNs((long)(t * 1e9))` renders frozen at `t` (the identical conversion
TACZ's own public helper `AnimationStateContext.setAnimationProgress(track, progressS, …)` uses,
`AnimationStateContext.java:217`).

Implementation (`TunerScrubber.java`):

- **Freeze = all live runners** (`:208-221`): tracks are unenumerable and the donor machines own
  several (main/static/bolt-caught/blending) — freezing one would leave the others animating.
  Each hijack records `(track, clip, playType)`; on close every track is restored to its
  pre-freeze clip (or emptied) (`:106-119`).
- Clips run with **`PLAY_ONCE_HOLD`** instead of the donors' `PLAY_ONCE_STOP` (`:179-181`): a
  STOPped runner is dropped from the controller's update set and could not be re-pinned.
- `reassertFrame` (`:240-271`) pins every runner to `min(scrubT, its own clip length)` — the
  per-runner clamp below `maxEnd` also keeps the `PLAY_ONCE_HOLD` end-hijack (`hold()` sets
  `progressNs = maxEnd + 1`, verified in TACZ `ObjectAnimationRunner.java`) from taking over.
  It runs from the client-tick handler **and** the `RenderHandEvent` hook — at least once per
  frame, so the state machine can un-freeze a runner for at most one frame; if the machine
  replaced a runner, the same clip is re-run and re-frozen (`:252-259`).
- Clip list = 30 candidate names (union of the `hk_g3`/`aug` donor animations and the default
  state machine — draw/put_away/shoot/bolt/reload_*/inspect_*/static_*/switch_*/run*/melee_*,
  `:59-66`) filtered by the public `AnimationController.containPrototype` (`:41`). TACZ has no
  enumerate-prototypes API, so clips outside the list are invisible to the cycler.

#### 6.2.7 Pose keyframe recorder + export

Recorder rows set an absolute pose (position in geo units, rotation in degrees — the binding's
spaces); `[+ Record Keyframe at t]` stores/overwrites it at the scrubbed timestamp
(`TunerAnimationExport.record`, `TunerAnimationExport.java:58-63`); `[Delete Keyframe]` removes
the nearest keyframe within 0.15 s (`:66-87`). `[Export Custom .animation.json]` writes Bedrock
**1.8.0** with `{"post": [x,y,z], "lerp_mode": "catmullrom"}` keyframes (`:127-158`) — the exact
shape TACZ's `AnimationKeyframesSerializer` parses (`lerp_mode`/`pre`/`post`). **Authoring-only:**
recorded poses do *not* live-preview on the gun (playback would require registering an animation
prototype on the donor Lua state machine — out of MVP scope); wiring the exported file into a
display's `animation` field is manual, or a later phase.

#### 6.2.8 `[Save to JSON]` paths

`TunerSaver.save()` (`TunerSaver.java:45-65`) pretty-prints the working tree
(`GsonBuilder().setPrettyPrinting()`) and tries, in order:

1. **Dev source tree** `C:/Projects/HBM-TACZ-Addon/src/main/resources/assets/hbm_m/nmesh_bindings/<gun_path>.json`,
   when that directory exists (it does — the shipped bindings live there, checked here); plus a
   discovery pass walking up from the running mod file looking for
   `src/main/resources/assets/hbm_m/nmesh_bindings` (`:91-115`), so a moved checkout keeps
   working.
2. **Always-writable fallback** `<gamedir>/tacz/hbm_nmesh_bindings/<path>.json` — `hbm_m`
   bindings as `<path>.json`, foreign namespaces as `<ns>#<path>.json` (`:` is illegal in Windows
   file names) (`:54-56`, `:68-84`). The engine re-reads this folder on **every** resource reload
   and its files win over jar resources (`NMeshBindingManager.loadExternalOverrides`,
   `NMeshBindingManager.java:98-133`; `externalOverrideDir` `:60-62`) — this keeps tuning working
   in a packaged build.

The status line shows the exact path written. `[Export Custom .animation.json]` follows the same
existence rule (`TunerSaver.animationExportTarget`, `:129-137`): the source-tree
`src/main/resources/gunpack/assets/hbm_m/animations/` **when present** — it is not (checked
here: `gunpack/assets/hbm_m/` contains only `display`, `geo_models`, `lang`, `textures`), so
exports currently land in the exported gunpack copy
`<gamedir>/tacz/gunpack/assets/hbm_m/animations/`, which `/tacz reload` hot-reads (§3.4).

#### 6.2.9 Recommended tuner workflow

1. Hold the gun, `F8`. Tab **Base**: fix `base_transform` (muzzle direction `[0,180,0]`, then
   translate, then uniform scale) until the monolithic gun sits right; `[Test Shot / Flash]` to
   aim `muzzle_flash`.
2. Tab **Base**: `[ADS Lock: ON]`, align `iron_view` to the center crosshair (two sliders);
   check `idle_view`.
3. Tab **Hands** + Tab **Time**: freeze `reload_empty` at the left-hand mag-grab frame, align
   `lefthand_pos` onto the HBM magazine; same for `righthand_pos`.
4. Tab **Bones**: per moving bone confirm `[Auto Pivot to SubMesh]`, nudge `pivot_offset` while
   watching the gizmo, and trim `Anim T/R scale` (§6.3) while scrubbing `shoot`/`bolt`.
5. `[Save to JSON]` — the status line shows the file; `/tacz reload` (or restart) loads from
   disk. The live override is dropped on tuner close, so what renders after reload is exactly
   what was saved.

### 6.3 `anim_translate_scale` / `anim_rotate_scale` — fitting donor travel to HBM geometry

Donor animations encode donor geometry: a bolt slide of 4.5 geo-units may exceed HBM's 3-unit
receiver port; donor hand arcs may not match HBM's magazine. Instead of forking the donor
`.animation.json`, scale the deltas per bone:

- **Where configured:** `bone_configs.<bone>.anim_translate_scale` / `anim_rotate_scale` (or the
  per-entry equivalents); resolution entry > config > default `[1,1,1]`
  (`NMeshBinding.resolveBoneConfig`, `:324-374`).
- **How applied:** `MixinBedrockPart` injects at HEAD of `translateAndRotateAndScale` and cancels
  **only when any multiplier ≠ 1** (`MixinBedrockPart.java:194-224`), replaying TACZ's exact
  sequence (`BedrockPart.java:90-104`) with the animation translation deltas (`offsetX/Y/Z`,
  block units) multiplied per axis; rest pose values (`x/y/z`, `xRot/yRot/zRot`, scales) are
  never scaled, so the monolithic rest assembly is bit-identical. Identity multipliers take the
  fast path — TACZ's original method runs unchanged (this is why body bones ship `1.0`: TACZ's
  sway writes to `rootNode.additionalQuaternion` stay on the untouched path).
- **Rotation scaling** (`:226-262`): uniform scales slerp from identity (angle scaling along the
  same axis; JOML's slerp flips on negative dot, so the shortest path is always taken);
  non-uniform scales decompose with TACZ's `MathUtil.toEulerAngles` (x=pitch, y=yaw, z=roll,
  radians — the exact convention `ModelRotateListener` built the quaternion with), multiply each
  angle by its multiplier and rebuild via `MathUtil.toQuaternion`. (The spec said "scale the
  Euler/axis-angle components … **or** slerp from identity"; one alpha cannot express
  non-uniform, so both branches are used as described — a documented deviation.)
- **Recipe:** freeze the animation at full travel (scrubber + gizmo), measure the needed ratio,
  set the multiplier on the moving bone only. Example: donor bolt travel 4.5 vs HBM port 3.0 →
  `anim_translate_scale ≈ [1, 1, 0.67]` (z = the travel axis in geo space after
  `base_transform.rotate [0,180,0]`). Scales are **per bone, not per clip** — every clip that
  moves that bone is scaled — so keep values near 1 and fix residual per-clip mismatches with
  `pivot_offset` / `bone_overrides` instead.
- The tuner exposes both triples on Tab **Bones** (0.0–3.0 sliders; `TunerBindingEditor.setConfig`
  syncs per-entry copies so saved files stay consistent, `TunerBindingEditor.java:282-295`).

### 6.4 Hybrid-mode dedup — what is filtered, when, and why real `hbm_m` items stay visible

**What is filtered.** Exactly one shape of stack (`HbmDuplicateFilter.isSyntheticTaczDuplicate`,
`HbmDuplicateFilter.java:67-101`): the **item's registry key namespace is `tacz`** — i.e. it
really is one of TACZ's container items `tacz:modern_kinetic_gun` / `tacz:ammo` /
`tacz:attachment` (registry names verified at
`C:/Projects/TACZ/src/main/java/com/tacz/guns/init/ModItems.java:27,31,32`) — **and** the inner
NBT id (`GunId`/`AmmoId`/`AttachmentId`) has namespace `hbm_m` (the ids of the embedded `hbm_m`
gun pack). Anything else is kept: real `hbm_m:gun_*` SEDNA items, real `tacz:`-pack guns, other
mods' items.

**Why the registry check is mandatory.** `MixinItemGunBaseNT` makes `ItemGunBaseNT` implement
`GunItemDataAccessor` (→ `IGun`) with `getGunId` returning `hbm_m:<registry path>`
(`MixinItemGunBaseNT.java:68-69, 104-118`). The old predicate (`IGun` + inner-id namespace only)
therefore also matched *genuine HBM guns* and would have stripped them from every global tab
event. Requiring the `tacz` registry namespace first means a real `hbm_m` item can never match.

**When — two layers, both active only when `hbm_m` is loaded:**

1. **Source level.** `hbm_tacz_compat.json` `mixins` = `MixinItemGunBaseNT`,
   `MixinAbstractGunItem`, `MixinAmmoItem`, `MixinAttachmentItem` (config read this session),
   the whole config gated by `HbmTaczMixinPlugin.shouldApplyMixin`
   (`HbmTaczMixinPlugin.java:39`). Each dedup mixin injects at `@At("RETURN")` of
   `fillItemCategory` and `removeIf(predicate)` (`MixinAbstractGunItem.java:35-39`,
   `MixinAmmoItem.java:29-32`, `MixinAttachmentItem.java:30-35`) on TACZ's three **static** tab
   builders — `AbstractGunItem.fillItemCategory(GunTabType)` (`AbstractGunItem.java:352`),
   `AmmoItem.fillItemCategory()` (`AmmoItem.java:66-73`),
   `AttachmentItem.fillItemCategory(AttachmentType)` (`AttachmentItem.java:56`) — consumed by
   **15** TACZ creative tabs (`ModCreativeTabs.java:46` ammo, `:51-76` six attachment tabs,
   `:81-116` eight gun tabs; all lines verified this session).
   **`ModernKineticGunItem` has no `fillItemCategory` override** (full-file read + tree grep
   this session: exactly 6 `fillItemCategory` declarations in TACZ, all static) — a subclass
   mixin would hard-fail under `require=1`; the single base-class injection covers every gun
   stack TACZ can produce. The handlers are static because the targets are static.
2. **Event level.** `HbmCompatBootstrap.registerCreativeTabDedupFilter`
   (`HbmCompatBootstrap.java:71-76`, invoked from `init()` behind the `hbm_m` re-check `:50-55`)
   subscribes `HbmCreativeTabFilter::onBuildCreativeTabContents` to the **mod bus** at
   `EventPriority.LOWEST`; the handler iterates the mutable
   `BuildCreativeModeTabContentsEvent` entries and `Iterator.remove()`s matches
   (`HbmCreativeTabFilter.java:45-55`). Forge runs each tab's own `displayItems` generator
   *into* the event's entry map and replays the survivors into the real tab
   (`ForgeHooks.onCreativeModeTabBuildContents`), so removals propagate into the final contents.

**Coverage of Creative SEARCH and JEI (no extra code needed).** Vanilla's Creative SEARCH tab
rebuilds from the category tabs filtered above — Forge 1.20.1 has no `getSearchEntries()` (the
method does not exist on this version). JEI enumerates creative-tab stacks for NBT-variant
ingredients, and TACZ registers subtype interpreters keyed on the inner ids
(`compat/jei/GunModPlugin.java:153-160`, `GunModSubtype`), so each `hbm_m`-NBT stack would be a
distinct JEI ingredient — and those stacks are removed before JEI ever sees them.
`AmmoBoxItem.fillItemCategory` (`AmmoBoxItem.java:245-259`, read this session) adds only the 5
generic ammo-box levels with no ammo-id enumeration, so it is not a duplicate source.

**Standalone mode.** `HbmTaczMixinPlugin` rejects the whole compat config and the event listener
is never registered (`init()` returns early) — every pack gun/ammo stays visible untouched.

### 6.5 Phase-7 known caveats (from the phase notes; unverified in game)

Carried from the tuner / engine / dedup phase notes. Items marked *(checked here)* were
re-verified by reading code this session — none of the behaviors below has been executed in a
game client:

- **Nothing Phase-7 has run in a game.** No Gradle build (forbidden for the implementing agents;
  the workflow's `compileJava` gate is still the first real compile — spec checklist item 1 is
  delegated), no `runClient`. The agents' javac type-check (43 files, 0 errors), binding
  round-trip test and relocation verifier all passed, but they are not the Gradle compile the
  spec checklist names. Panel layout, gizmo placement, scrubbing feel, ADS-lock interplay, dedup
  visuals — all implementation-verified only.
- **Gizmos are first-person only** (§6.2.5) — the hook rides the delegate pass of the held gun's
  model; 3P/ground/fixed render no gizmos. *(by design, spec scope)*
- **Scrubber freezes all live runners** and restores recorded `(clip, playType)` pairs on close;
  a clip the state machine starts *while frozen* is caught on the next reassert frame and
  re-frozen — expect at most a one-frame flicker, not a desync. Clips outside the 30-name
  candidate list cannot be cycled to (no enumerate API in TACZ).
- **The recorder is authoring-only** — no live preview of recorded keyframes on the gun;
  exporting does not wire the file into any display.
- **`isNmeshBackedGun` keys on the gun-id binding only** (`NMeshGunModelInjector.java:153-160`,
  *checked here*): a binding filed under a display-id fallback alone would not pass the F8 open
  gate nor disable TACZ's static VBO fast path (pre-existing asymmetry with `resolveBinding`,
  which *does* accept display-id bindings, `:487-497`). The shipped bindings are gun-id-keyed
  and unaffected.
- **StG77 `safety` relocation is a runtime no-op** — the aug donor animation has no keyframes on
  that bone (phase-note verification against `aug.animation.json`); harmless, but it also means
  that particular relocation cannot be visually confirmed in game. The g3 analogues
  (`handle`/`selector`) *are* animated.
- **Left hand ↔ HBM magazine alignment is not pre-solved**: no `bone_overrides` ship for
  `lefthand`/`mag_and_l` (g3) or `mag_and_lefthand` (stg77) — the correct offset depends on
  in-game visuals that cannot be verified without running the game; use Tab **Hands** + the
  scrubber (engine deviation note).
- **Wheel steps are spec-literal ±0.01 / ±0.1 on every slider**, bypassing drag quantization
  (0.01 for unit rows, 0.5 for degree rows); ±0.01 per tick on a degree slider is very fine —
  the drag path remains the fast adjuster.
- **`putTunerOverride` survives only until the next resource reload** (`NMeshBindingManager.apply`
  clears and re-reads everything, `:77-91`, *checked here*): a `/tacz reload` while the tuner is
  open re-reads disk and drops unsaved edits — persistence is always `[Save to JSON]`.
- **Animation export target** — the source-tree `gunpack/assets/hbm_m/animations/` folder does
  not exist (*checked here*), so exports land in `<gamedir>/tacz/gunpack/...`; that folder is
  re-exported/overwritten per §1.4, so move kept exports into the repo.
- **`MixinBedrockPart` rotation scaling** — uniform → slerp-from-identity, non-uniform → Euler
  component scaling (the spec's "or" realized as two branches, §6.3).
- **Panel density** — ~40 controls across 4 tabs in a 210 px panel: it scrolls; wheel over a
  slider takes precedence over panel scrolling.
- **Dedup nuance** — `MixinAttachmentItem` is defensive (the current pack ships no attachment
  indexes: `gunpack/data/hbm_m/index/` has only `guns/` and `ammo/`, per phase notes) but keeps
  any future pack attachment from re-leaking duplicates.
