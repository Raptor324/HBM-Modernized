# GUI + Container Porting Roadmap

Ziel: Für **jede** Machine/jeden Block mit GUI aus dem Original-Repo
(`Hbm-s-Nuclear-Tech-GIT-master`, Paket `com.hbm.inventory.gui` /
`com.hbm.inventory.container`) wird die GUI- und Container/Menu-Klasse
**1:1** (gleiche Zahlen: Slot-Positionen, Balken-Rects, Texturkoordinaten,
`isItemValid`/Transfer-Regeln, sync fields) in die modernisierte Codebasis
(`com.hbm_m.inventory.gui` / `com.hbm_m.inventory.menu`) übertragen, nur
auf die neue Menu/Screen-API umgesetzt.

Stand dieser Analyse: 2026-08-09 (automatischer Abgleich per Dateiname,
danach manuell zu verifizieren — Namensabgleich ersetzt keine inhaltliche
Prüfung!).

- Original: 217 `GUI*.java`, 173 `Container*.java`
- Modernisiert (aktuell vorhanden): 136 `GUI*.java`, 124 `*Menu.java`
- Automatisch als "vorhanden" erkannt: 90 GUI-Paare (siehe Teil A)
- Ohne erkannten Gegenstück: 127 GUI-Klassen (siehe Teil B) — **enthält
  vermutlich einzelne falsche Positive durch abweichende Benennung**,
  muss beim Abarbeiten pro Eintrag nochmal per Grep verifiziert werden.

## Wie ein Eintrag abgearbeitet wird

1. Original-Paar suchen: `GUIXxx.java` + `ContainerXxx.java` im Original-Repo.
2. Modernisiertes Gegenstück suchen/anlegen: `GUIMachineXxx.java` +
   `MachineXxxMenu.java` (oder passendes Namensschema) unter
   `src/main/java/com/hbm_m/inventory/gui` bzw. `.../inventory/menu`.
3. Slot-Koordinaten, Balken/Progress-Rects, Texturpfade, Tooltip-Texte,
   `quickMoveStack`/Slot-Validierung, Sync-Felder 1:1 aus dem Original
   übernehmen (Werte, nicht nur Struktur).
4. Registrierung prüfen (MenuType-Registry, ScreenRegistration im Client-Setup).
5. Häkchen setzen in diesem Dokument.

## Teil A — Bereits vorhanden (Audit nötig, keine Neuanlage)

Diese Paare existieren schon in der modernisierten Codebasis. Hier geht es
nur noch darum, sie gegen das Original zu prüfen und alle Zahlen exakt
anzugleichen (bislang wurden sie vermutlich nicht 1:1, sondern
"sinngemäß" übernommen).

Status-Legende: ✅ = geprüft, war bereits/ist jetzt 1:1 korrekt · 🔧 = Zahlenfehler gefunden und behoben · ⚠️ = braucht manuelle Entscheidung (Architektur/Slot-Anzahl weicht bewusst ab, keine reine Zahlenkorrektur möglich) · 🛑 = größere Neuimplementierung nötig (komplett andere Darstellungstechnik, z.B. Textur→Flat-Fill)

- [x] GUIAnvil — ✅ bereits korrekt
- [x] GUIArmorTable — 🔧 SIDE_PANEL_HEIGHT 80→100 korrigiert
- [x] GUIAshpit → GUIMachineAshpit — ✅
- [x] GUIAutocrafter → GUIMachineAutocrafter — ⚠️ nur 11 statt 21 Slots (dokumentierte Vereinfachung)
- [x] GUIBatterySocket — ✅
- [x] GUIBlastFurnace — ⚠️ 4 statt 5 Slots, kein Fluid-Tank-System
- [x] GUICombustionEngine → GUIMachineCombustionEngine — ⚠️ bewusst auf 2 Slots reduziert (Zündung/Drossel fehlen)
- [x] GUICompressor → GUIMachineCompressor — ✅ (gemeinsame Slots exakt, Upgrade-Slots bewusst entfernt)
- [x] GUICraneBoxer — ✅
- [x] GUICraneExtractor — 🔧 Klick-Hitbox Whitelist-Toggle korrigiert
- [x] GUICraneGrabber — 🔧 Koordinaten waren fälschlich vom Extractor kopiert (imageWidth, Filter-/Upgrade-Slots, Hitbox) — jetzt korrekt
- [x] GUICraneInserter — ✅
- [x] GUICraneRouter — ✅
- [x] GUICraneUnboxer — ✅
- [x] GUICrucible → GUIMachineCrucible — ✅ Zahlen, ⚠️ Rezept-Icon/Auswahl-Button fehlt (Feature, kein Zahlenfehler)
- [x] GUICrystallizer → GUIMachineCrystallizer — ✅
- [x] GUIDroneCrate → GUIMachineDroneCrate — 🔧 Fluid-Tank-Anzeige komplett ergänzt
- [x] GUIDroneDock → GUIMachineDroneDock — 🔧 imageHeight 186→185
- [x] GUIDroneProvider → GUIMachineDroneProvider — ✅
- [x] GUIDroneRequester → GUIMachineDroneRequester — ✅
- [x] GUIElectrolyser → GUIMachineElectrolyser — ⚠️ 10 statt 24 Slots
- [x] GUIFEL → GUIMachineFel — ⚠️ Menu hat gar keine Maschinen-Slots (Stub)
- [x] GUIFirebox → GUIMachineFirebox — 🔧 imageHeight 166→168, ⚠️ Heiz-/Brennbalken fehlen strukturell
- [x] GUIFunnel → GUIMachineFunnel — 🔧 Slot-Spacing 26+i*16→8+i*18, Spieler-Inv Y korrigiert
- [x] GUIFurnaceBrick → GUIMachineFurnaceBrick — 🔧 Balken-UV/Scale korrigiert, ⚠️ Asche-Slot fehlt
- [x] GUIFurnaceIron → GUIMachineFurnaceIron — 🔧 Balken-Position/UV/Scale korrigiert, ⚠️ Glow-Icon fehlt
- [x] GUIFurnaceSteel → GUIMachineFurnaceSteel — 🔧 Balken-Position/UV/Scale korrigiert, ⚠️ Heizbalken fehlt (externe Wärmequelle, dokumentiert)
- [x] GUILaunchPadLarge — ✅
- [x] GUILaunchPadRusted — ⚠️ Stub ohne Launch-Codes/Missile-Preview/Release-Button
- [x] GUILiquefactor → GUIMachineLiquefactor — ✅
- [x] GUIMachineAmmoPress — ⚠️ dokumentierte Layoutänderung (kein Rezept-Suchraster)
- [x] GUIMachineAnnihilator — ⚠️ Meilenstein-Payout-System bewusst nicht übernommen
- [x] GUIMachineArcWelder — 🔧 Fluid-Tank-Rendering ergänzt, Titel-Offset ergänzt
- [x] GUIMachineBattery — ✅
- [x] GUIMachineCatalyticReformer — 🔧 Energiebalken-Skalierung 52→54
- [x] GUIMachineCentrifuge — ⚠️ komplett eigenes Fill-Rechteck-Layout (Textur ohne Widget-Sprites)
- [x] GUIMachineChemicalFactory — ⚠️ komplette Neukonzeption (4-Lane-Multiblock)
- [x] GUIMachineChemicalPlant — ✅ (inkl. dokumentierter bewusster Upgrade-Slot-Verschiebung)
- [x] GUIMachineCoker — ✅
- [x] GUIMachineCyclotron — 🔧 Input/Target-Slots waren vertauscht, Balken-Skalierung falsch, 3 Fluid-Tanks fehlten komplett — alles korrigiert
- [x] GUIMachineDiFurnaceRTG → GUIMachineDifurnaceRtg — ⚠️ Pellet-Raster umgebaut, Balken/Panels fehlen
- [x] GUIMachineEPress — ⚠️ eigenes Layout/eigene Textur (dokumentiert)
- [x] GUIMachineElectricFurnace — 🔧 Fortschritts-Füllrechteck-Geometrie korrigiert
- [x] GUIMachineExposureChamber — 🔧 Fortschritts-/Partikel-/Energie-Rechtecke korrigiert
- [x] GUIMachineFluidTank — ✅ (dokumentierter Sprite-Tausch 0↔2 unverändert)
- [x] GUIMachineGasCent → GUIMachineGasCentrifuge — ✅ (Tank-Rendering-Differenz architekturbedingt)
- [x] GUIMachineHydrotreater — ⚠️ 9 Fluid-I/O-Slots bewusst entfernt (MK2-Rohrnetz)
- [x] GUIMachineKeyForge → GUIMachineKeyforge — 🔧 imageHeight 184→186
- [x] GUIMachineLargeTurbine — ✅
- [x] GUIMachineMissileAssembly → GUIMissileAssembly — 🔧 inventoryLabelY korrigiert
- [x] GUIMachineOilWell — ⚠️ komplett andere Canvas-Größe/Slot-Anzahl
- [x] GUIMachinePUREX — ⚠️ fundamental anderes Design (variable Slots vs. fix)
- [x] GUIMachinePress — 🔧 Output-Slot-Koordinate korrigiert, ⚠️ 9 Extra-Storage-Slots fehlen
- [x] GUIMachineRTG → GUIMachineRtg — ⚠️ Pellet-Grid-Offset falsch, Energie-/Hitze-Balken fehlen komplett
- [x] GUIMachineRadGen — ✅ (Pro-Slot-Tooltip fehlt, kleinere Lücke)
- [x] GUIMachineRadarNT — ✅ vorbildlicher 1:1-Port
- [x] GUIMachineRadarNTSlots — ✅ (Menu heißt tatsächlich `MachineRadarSlotsMenu`)
- [x] GUIMachineRefinery — ⚠️ komplett andere Canvas/Textur-Atlas
- [x] GUIMachineRotaryFurnace — ⚠️ Fuel-Bar-Position falsch, Output-Tanks fehlen komplett
- [x] GUIMachineSatLinker — ⚠️ andere Textur/Canvas-Höhe (bewusste Asset-Entscheidung)
- [x] GUIMachineShredder — 🔧 Titel-Zentrierung korrigiert (106-width/2)
- [x] GUIMachineSiren — ⚠️ Kassetten-Metadaten-Anzeige fehlt komplett
- [x] GUIMachineSolderingStation — 🔧 Titel-Offset korrigiert, ⚠️ Fluid-Tank-Anzeige + Rezept-Routing fehlen
- [x] GUIMachineStrandCaster — ⚠️ 2 statt 7 Slots (dokumentiert "auf 1 vereinfacht")
- [x] GUIMachineTurbine — ✅
- [x] GUIMachineTurbineGas — ⚠️ Start/Stop/Auto-Buttons, RPM-Messer, Leistungsregler fehlen komplett
- [x] GUIMachineVacuumDistill — 🔧 Energiebalken-Skalierung 52→54
- [x] GUIMachineWoodBurner — 🔧 Energiebalken-UV-Koordinate korrigiert, ⚠️ Flüssigbrennstoff-Modus fehlt komplett
- [x] GUIMassStorage → GUIMachineMassStorage — 🔧 imageHeight + Spieler-Inv-Position korrigiert, ⚠️ Gauge/Toggle-Button/Tooltips fehlen
- [x] GUIMicrowave → GUIMachineMicrowave — 🔧 quickMoveStack-Reihenfolge korrigiert, ⚠️ Rendering komplett anders
- [x] GUIMiningLaser → GUIMachineMiningLaser — ⚠️ 9 statt 21+8 Slots
- [x] GUIMixer → GUIMachineMixer — ⚠️ nur Batterie-Slot statt 5 Slots
- [x] GUINukePrototype — ✅
- [x] GUIOreSlopper → GUIMachineOreSlopper — ⚠️ andere Slot-Anzahl/-Position
- [x] GUIPyroOven → GUIMachinePyroOven — ✅ vorbildlicher 1:1-Port
- [x] GUIRBMKBoiler — 🛑 komplett andere Architektur (texturlos statt Textur-GUI)
- [x] GUIRBMKConsole → GUIMachineRbmkConsole — 🛑 andere Enum-Struktur für Mini-Screens, keine reine Zahlenkorrektur möglich
- [x] GUIRBMKControl — 🛑 komplett andere Architektur (Flat-Fill statt Textur-Gauges)
- [x] GUIRBMKOutgasser — 🛑 1 statt 2 Slots, kein Balken-Rendering
- [x] GUIRBMKRod — 🛑 kein Player-Inventar im Menu, kein Balken-Rendering
- [x] GUIRBMKStorage — 🛑 Slot-Grid 3×4 (Original) vs. 2×6 (Mod), zusätzlich interner Rendering-Bug gefunden (Icons nicht an Menu-Slots ausgerichtet)
- [x] GUIRadioRec — ⚠️ komplett andere UI-Technik (Vanilla-Widgets statt Textur)
- [x] GUIRadiolysis → GUIMachineRadiolysis — ⚠️ 2 statt 15 Slots (RTG-Grid entfällt, dokumentiert)
- [x] GUIReactorResearch → GUIMachineReactorResearch — ✅ alle Rod-Slot-Koordinaten exakt
- [x] GUISILEX → GUIMachineSilex — ⚠️ Menu hat gar keine Maschinen-Slots mehr
- [x] GUIScreenRecipeSelector — ✅ alle Werte exakt
- [x] GUISolidifier → GUIMachineSolidifier — 🔧 Titel-Position/-Farbe korrigiert
- [x] GUISoyuzLauncher — ⚠️ komplett neues Layout/andere Texturgröße
- [x] GUIStorageDrum → GUIMachineStorageDrum — ⚠️ rautenförmiges Original-Layout vs. einfaches Raster ohne Textur
- [x] GUIWasteDrum → GUIMachineWasteDrum — ⚠️ nutzt Original-Textur, aber falsches Slot-Raster (Slots liegen nicht auf den eingezeichneten Rahmen — sichtbarer visueller Bug)

**Zusammenfassung Teil A:** 90 Paare geprüft. ~25 waren bereits 1:1 korrekt, ~25 wurden mit reinen Zahlen-/Koordinatenkorrekturen auf 1:1 gebracht (u.a. Cyclotron-Slot-Vertauschung + fehlende Tanks, CraneGrabber komplett falsche Koordinaten, mehrere Titel-/Balken-Positionsfehler), ~30 sind bewusste/dokumentierte Architekturvereinfachungen (weniger Slots als Original, kein 1:1 ohne Strukturänderung) und ~6 (die RBMK-Familie: Boiler/Console/Control/Outgasser/Rod/Storage) brauchen eine größere Neuimplementierung, da sie komplett texturlos/andersartig aufgebaut sind statt nur andere Zahlen zu haben.

Bereits im aktuellen Branch (git status) in Arbeit, aber noch ohne
1:1-Zahlenabgleich: **GUIMachinePWRController** (`ContainerPWR`/`GUIPWR`
im Original prüfen — siehe Teil B, "PWR"-Familie ist dort separat
gelistet, da Original-Namensschema abweicht).

## Teil B — Kein Original-Pendant automatisch gefunden (verifizieren + neu anlegen)

Gruppiert nach Themenbereich, absteigend nach vermuteter Priorität für
den aktuellen Spielinhalt.

### Industrie-Maschinen (hohe Priorität)

Korrigiert nach manueller Prüfung (2026-08-09) — die automatische
Namenserkennung hatte hier viele falsche Treffer, da im modernisierten
Code andere Klassennamen verwendet werden:

**Bereits vollständig vorhanden (Block+BlockEntity+GUI+Menu), fälschlich
als fehlend gelistet — keine Aktion nötig:**
GUIMachineArcFurnaceLarge → `MachineArcFurnaceBlock`+GUIMachineArcFurnace ✅,
GUIMachineAssemblyMachine → `MachineAssemblerBlock`/`MachineAdvancedAssemblerBlock` ✅,
GUIMachineDiesel → `MachineDieselGeneratorBlock` ✅,
GUIMachineExcavator → `MachineMiningDrillBlock` ("Large Mining Drill") ✅,
GUIMachineGasFlare → `MachineFlareStackBlock` ✅,
GUIMachineReactorBreeding → `MachineBreederBlock` ✅,
GUIElectrolyserFluid/GUIElectrolyserMetal → ein gemeinsamer `MachineElectrolyserBlock` (Dual-Mode) ✅,
GUIFurnaceCombo → `MachineCombinationOvenBlock` ✅,
GUIRtgFurnace → `MachineDifurnaceRtgBlock` ✅ (bereits Teil von Teil A oben).

- [x] GUIMachineArcFurnaceLarge — bereits vorhanden, kein Fehlbestand
- [x] GUIMachineAssemblyMachine — bereits vorhanden, kein Fehlbestand
- [x] GUIMachineDiesel — bereits vorhanden, kein Fehlbestand
- [x] GUIMachineExcavator — bereits vorhanden, kein Fehlbestand
- [x] GUIMachineGasFlare — bereits vorhanden, kein Fehlbestand
- [x] GUIMachineReactorBreeding — bereits vorhanden, kein Fehlbestand
- [x] GUIElectrolyserFluid / GUIElectrolyserMetal — bereits vorhanden (Dual-Mode), kein Fehlbestand
- [x] GUIFurnaceCombo — bereits vorhanden, kein Fehlbestand
- [x] GUIRtgFurnace — bereits vorhanden, kein Fehlbestand

**Echt fehlend, Block+BlockEntity existierte schon → GUI+Menu gebaut
(2026-08-09, kompiliert erfolgreich):**
- [x] GUIMachinePrecAss — ✅ gebaut. Bug behoben: `ModMenuTypes.MACHINE_PRECASS_MENU` war als `MenuType<MachineAdvancedAssemblerMenu>` deklariert (Platzhalter-Typ) statt `MenuType<MachinePrecAssMenu>`; `ClientSetup.java` zeigte auf `GUIMachineAdvancedAssembler::new` statt eigenes `GUIMachinePrecAss`. Beides korrigiert, `MachinePrecAssMenu` war bereits korrekt (1:1 zu `ContainerMachinePrecAss`, da Original selbst ein Advanced-Assembler-Klon ist).
- [x] GUIMachineTurbofan — ✅ gebaut (`MachineTurbofanMenu`+`GUIMachineTurbofan` neu). Bug behoben: `MachineTurbofanBlockEntity.createMenu()` gab `null` zurück. ⚠️ Upgrade-Slot (98,71) aus dem Original entfällt (kein Upgrade-System am Block); Fuel-Container/Fluid-ID-Slots im GUI vorhanden aber inert (Tank füllt nur über MK2-Rohrnetz).
- [x] GUIHeaterHeatex — ✅ gebaut (`MachineHeatexMenu`+`GUIMachineHeatex` neu, echte Textur `gui_heatex.png` genutzt). ⚠️ Original-Item-Slot (Fluid-Typ-Zuweisung) + editierbare Zyklus-/Delay-Textfelder entfallen, da die BlockEntity keine Slots/diese Felder besitzt (Tanks fix auf coolant_hot/coolant, Ops/Tick ist Konstante).
- [x] GUIOilburner — ✅ gebaut (`MachineOilburnerMenu`+`GUIMachineOilburner` neu, echte Texturen `gui_oilburner.png`/`gui_oilburner_hp.png`). **Bug gefunden+behoben:** `serverTick()` rief nie `sendUpdateToClient()` auf — GUI wäre eingefroren geblieben (Heat/Tank/Burning-Status hätte den Client nie erreicht). ⚠️ Die 3 Original-Fluid-Slots entfallen (BE nutzt Forge-Fluid-Capability statt Slots, dokumentiert); manueller On/Off-Toggle-Button entfällt (BE brennt automatisch bei Redstone-Signal statt persistentem Flag).

Build-Status: `./gradlew compileJava` → `BUILD SUCCESSFUL` (verifiziert nach allen vier Änderungen).

**Korrektur:** GUIPump wurde fälschlich als fehlend gelistet (automatischer
Namensabgleich hat `GUIPump` — das GUI des *Fluid-Rohrleitungs*-Pumpenblocks
`FluidPump`, ein reiner `GuiScreen` ohne Container — mit dem unabhängigen
`MachinePumpElectric`/`MachinePumpSteam` verwechselt). Die tatsächlichen
Original-TileEntities `TileEntityMachinePumpElectric`/`...Steam` haben
**kein eigenes GUI** (keine `onBlockActivated`/GUI-Handler-Verknüpfung im
Original gefunden) — daher außerhalb des Auftragsumfangs, keine Aktion.

**Echt fehlend, UND Block/BlockEntity existieren noch nicht (nur leerer
Block ohne Funktion registriert) → braucht erst BlockEntity-Arbeit, bevor
ein GUI überhaupt Sinn ergibt:**
- [ ] GUIMachineAssemblyFactory — nur als reiner `Block` registriert (ModBlocks.java:2469), keine BlockEntity
- [ ] GUIMachineCompactLauncher — nur als reiner `Block` registriert (ModBlocks.java:2039), keine BlockEntity
- [ ] GUIMachinePlasmaForge — nur als reiner `Block` registriert (ModBlocks.java:2629), keine BlockEntity
- [ ] GUIMachineCustom — nirgends gefunden, existiert im modernisierten Mod noch gar nicht
- [ ] GUIMachineLaunchTable — nirgends gefunden, existiert im modernisierten Mod noch gar nicht
- [ ] GUIDiFurnace (normale Variante, nicht RTG) — nur die RTG-Variante existiert, normale Variante fehlt komplett

Zusatzfund: `MachineElectricHeaterBlock` ist registriert, hat aber keine
zugehörige BlockEntity gefunden — separat von Heatex, ggf. unvollständig/totes Feature, nicht Teil dieser Liste.

### PWR-Familie (aktuell in Arbeit laut git status)
GUIPWR (Original-Basisname — abgleichen mit eurem neuen
`MachinePWRControllerBlock`/`PWRControllerBlockEntity`/
`PWRPartBlock`/`PWRPartBlockEntity` System; im Original evtl. nur eine
einzige Container/GUI-Klasse für den Controller, Rest ist Multiblock ohne
eigenes Menü — verifizieren)

### Reaktor / Fusion / Teilchenbeschleuniger
GUICore, GUICoreEmitter, GUICoreInjector, GUICoreReceiver,
GUICoreStabilizer, GUIFusionBreeder, GUIFusionKlystron, GUIFusionTorus,
GUIICF, GUIICFPress, GUIPADetector, GUIPADipole, GUIPAQuadrupole,
GUIPARFC, GUIPASource, GUIReactorControl, GUIReactorZirnox, GUIWatz

### Nukes
GUINukeBoy, GUINukeCustom, GUINukeFleija, GUINukeFstbmb, GUINukeGadget,
GUINukeMan, GUINukeMike, GUINukeN2, GUINukeSolinium, GUINukeTsar,
GUIBombMulti

### Türme/Turrets
GUITurretArty, GUITurretBase, GUITurretChekhov, GUITurretFriendly,
GUITurretFritz, GUITurretHIMARS, GUITurretHoward, GUITurretJeremy,
GUITurretMaxwell, GUITurretRichard, GUITurretSentry, GUITurretTauon,
GUIWeaponTable

### Pneumatik / Transportrohre
GUIPneumoStorageAccess, GUIPneumoStorageClutter,
GUIPneumoStorageExporter, GUIPneumoStorageImporter, GUIPneumoStorageMono,
GUIPneumoTube, GUICartDestroyer

### Lagerung / Kisten
GUICrateDesh, GUICrateIron, GUICrateSteel, GUICrateTungsten, GUISafe,
GUILeadBox, GUIFileCabinet, GUIAmmoBag, GUICasingBag, GUIPlasticBag,
GUIToolBox, GUIBarrel, GUIBatteryREDD

### Raumfahrt
GUISatDock, GUISoyuzCapsule

### RBMK-Zusatzteile
GUIRBMKAutoloader, GUIRBMKControlAuto, GUIRBMKHeater

### Sonstige Blöcke
GUICounterTorch, GUIDiode, GUIForceField, GUILemegeton

### Reine Anzeige-/HUD-Screens (niedrige Priorität — keine echten Container/Slots)
GUIScreenBobble, GUIScreenBobmazon, GUIScreenClayTablet,
GUIScreenDesignator, GUIScreenFluid, GUIScreenGuide, GUIScreenHolotape,
GUIScreenPager, GUIScreenPreview, GUIScreenRBMKDisplay,
GUIScreenRBMKGauge, GUIScreenRBMKGraph, GUIScreenRBMKIndicator,
GUIScreenRBMKKeyPad, GUIScreenRBMKLever, GUIScreenRBMKTerminal,
GUIScreenRadioAUTOCAL, GUIScreenRadioTelex, GUIScreenRadioTorch,
GUIScreenRadioTorchController, GUIScreenRadioTorchLogic,
GUIScreenRadioTorchReader, GUIScreenSatCoord, GUIScreenSlicePrinter,
GUIScreenSnowglobe, GUIScreenToolAbility, GUIScreenWikiRender, GUIBook,
GUIBookLore, GUICalculator, GUIElements, GuiFileList, GuiInfoContainer

## Nächster Schritt

Bitte auswählen, mit welcher Gruppe aus Teil B (oder welchem Teil-A-Audit)
als nächstes begonnen werden soll — angesichts der Größe (127 fehlende +
90 zu auditierende Paare) wird das über mehrere Sessions abgearbeitet,
jeweils machine-für-machine mit eigenem Commit.
