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
- [x] GUIRBMKBoiler — ✅ neu gebaut (2026-08-09): echte Textur `gui_rbmk_boiler.png` genutzt, Wasser-/Dampf-Füllstand + Steam-Grade-Anzeige 1:1 restauriert, Spieler-Inventar-Slot-Verschiebung (~20px) behoben.
- [x] GUIRBMKConsole → GUIMachineRbmkConsole — ✅ neu gebaut: Textur war schon vorhanden, aber mit **falschen UV-Koordinaten** (Boiler/Moderator/Absorber/Reflector/Cooler/Heater/Outgasser/Storage zeigten alle die falsche Sprite) — korrigiert; Heater-Overlay ergänzt. Mini-Screen-Icons (18px-Reihe) bleiben architekturbedingt limitiert (anderes Datenmodell `ColumnType` statt Original-`ScreenType`), zeigen jetzt zumindest gültige statt zufällige Icons.
- [x] GUIRBMKControl — ✅ neu gebaut: echte Textur `gui_rbmk_control.png`, Rod-Level-Balken + Farbgruppen-Marker 1:1 restauriert, Klick-Regionen statt Original-Drag (dokumentierte Vereinfachung). Power-Icon nicht restaurierbar (BlockEntity hat kein power-Feld).
- [x] GUIRBMKOutgasser — ✅ neu gebaut: echte Textur, Slot auf Original-Position verschoben. Balken zeigen mangels echter Progress-/Fluid-Daten stattdessen Xenon-Vergiftung% bzw. Heizwert (dokumentierter Kompromiss).
- [x] GUIRBMKRod — ✅ neu gebaut: echte Textur, Depletion-/Xenon-Balken mit echten Live-Werten. **Bug behoben:** Spieler-Inventar fehlte komplett im Menu (war nie erreichbar) — jetzt ergänzt inkl. Hitze-Sicherheitssperre beim Entnehmen zu heißer Stäbe.
- [x] GUIRBMKStorage — ✅ neu gebaut: Slot-Grid von 2×6 auf Original-3×4 umgestellt (Textur ist fest für 3×4 gezeichnet), damit behoben: Icons waren vorher nicht an den echten Slot-Positionen ausgerichtet.

Build-Status RBMK-Rebuild: `./gradlew compileJava` → `BUILD SUCCESSFUL`.
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

### PWR-Familie — NICHT ANFASSEN, aktive Arbeit des Entwicklers

Recherche (2026-08-09): Original `ContainerPWR`/`GUIPWR` gehört zu
`TileEntityPWRController`, einem echten Multiblock (flood-fill über
Casing/Rods/Reflektoren/Heatex/Kanäle/Ports). Exakte Original-Werte:
`xSize=176,ySize=188`, Textur `gui_pwr.png`, Slots bei (53,5)/(89,32)/(8,59),
Fortschrittsbalken/Rod-Balken/Overheat-Icon/2 Fluid-Tanks — alles dokumentiert
im Original.

Der modernisierte `PWRControllerMenu`/`GUIMachinePWRController` ist
**bewusst kein 1:1-Port**: Single-Block statt Multiblock,
`imageWidth=226/imageHeight=230` statt 176/188, andere Slot-Positionen,
kein Coolant-Tank-Slot, Balken als Fill-Rechtecke statt Textur-Gauges,
+/-5%-Buttons statt Original-Drag-Regler. Die Code-Kommentare sagen das
explizit ("keine pixelgenaue Reproduktion der Original-GUIPWR-Overlay-
Koordinaten"). `GUIPWRPrinter` hat im Original gar kein Pendant (reines
neues Feature, ebenfalls dokumentiert als bewusst nicht 1:1).

**→ Diese Gruppe wird nicht automatisiert angefasst, da aktuell aktive,
bewusste Entwicklungsarbeit (siehe git log "PWR part 3?", "PWR Blocks and
Cargo Elevator..."). Bei Bedarf bitte explizit anfragen.**

### Reaktor / Fusion / Teilchenbeschleuniger

Recherche (2026-08-09): alle 18 Original-Namen haben echte Container+GUI
mit echten Slots (2-30 Slots je nach Machine).

**Bereits fertig (falsch als fehlend gelistet):**
- [x] GUIReactorZirnox → `MachineZirnoxBlock`/`MachineZirnoxMenu`/`GUIMachineZirnox` ✅
- [x] GUIWatz → `MachineWatzPowerplantBlock`/`...Menu`/`GUIMachineWatzPowerplant` ✅

**Block+BlockEntity existiert bereits → GUI+Menu gebaut (2026-08-09):**
- [x] GUICoreEmitter — ✅ gebaut (`MachineCoreEmitterMenu`+`GUIMachineCoreEmitter`, echte Textur `gui_emitter.png`). `createMenu()` gab vorher `null` zurück (Bug behoben). Original-Watt-Textbox/On-Off-Toggle entfallen (BE hat keine `watts`/`isOn`-Felder, feuert immer bei Energie+Kühlmittel).
- [x] GUICoreInjector — ✅ gebaut (`MachineCoreInjectorMenu`+`GUIMachineCoreInjector`, echte Textur `gui_injector.png`, Deuterium-/Tritium-Tanks). `createMenu()` gab vorher `null` zurück (Bug behoben). Original-Crafting-Slotpaare entfallen (BE hat keine Rezeptlogik, `isItemValidForSlot` immer `false`).
- [x] GUICoreReceiver — ✅ gebaut (`MachineCoreReceiverMenu`+`GUIMachineCoreReceiver`, echte Textur `gui_receiver.png`). `createMenu()` gab vorher `null` zurück (Bug behoben). SPK/HE-Doppelwirtschaft aus Original zu einem Energie-Pool zusammengefasst (BE-Design), Cryogel-Tank als Fill-Rechteck (keine passende Textur-UV bekannt).

**Braucht erst Block+BlockEntity-Arbeit (13):** GUICore, GUICoreStabilizer,
GUIFusionBreeder, GUIFusionKlystron, GUIFusionTorus, GUIICF, GUIICFPress,
GUIPADetector, GUIPADipole, GUIPAQuadrupole, GUIPARFC, GUIPASource,
GUIReactorControl (klassischer Fission-Reaktor — nicht verwechseln mit
RBMK/PWR, komplett eigenes System im Original).

### Nukes

Recherche (2026-08-09): alle 11 Original-Namen haben echte Container+GUI.

**Bereits fertig (falsch als fehlend gelistet):**
- [x] GUINukeBoy → `NukeFatManBlockEntity`/`NukeFatManMenu`/`GUINukeFatMan` ✅ ("Fat Man" ist nur der Anzeigename)
- [x] (Bonus, war schon in Teil A) NukePrototype → `NukePrototypeMenu`/`GUINukePrototype` ✅

**Braucht komplette Neuentwicklung (kein Block/Item/Entity vorhanden):**
- [ ] GUINukeCustom
- [ ] GUINukeFleija — Explosions-/Wolkeneffekt existiert bereits (`ExplosionFleija`, `EntityCloudFleija`, `FleijaSphereMesh`), aber kein platzierbares Bomben-Item/-Block + kein GUI
- [ ] GUINukeFstbmb
- [ ] GUINukeGadget
- [ ] GUINukeMan
- [ ] GUINukeMike
- [ ] GUINukeN2
- [ ] GUINukeSolinium
- [ ] GUINukeTsar
- [ ] GUIBombMulti

### Türme/Turrets — GRUPPE FERTIG ✅

Recherche (2026-08-09): Original nutzt für **alle** 12 Turret-Varianten
nur einen einzigen echten Container (`ContainerTurretBase`); die
`GUITurretX`-Klassen unterscheiden sich nur in Hintergrundtextur. Der
modernisierte Code spiegelt das exakt: ein gemeinsames `GUITurret`/
`TurretMenu`, angetrieben von der `TurretStats`-Enum mit allen 11
konkreten Varianten (SENTRY, CHEKHOV, FRIENDLY, JEREMY, TAUON, RICHARD,
HOWARD, MAXWELL, FRITZ, ARTY, HIMARS), je mit eigenem Block+BlockEntity.

- [x] GUITurretArty / Base / Chekhov / Friendly / Fritz / HIMARS / Howard / Jeremy / Maxwell / Richard / Sentry / Tauon — ✅ alle fertig über gemeinsames `GUITurret`/`TurretMenu`, deckt sich mit Original-Architektur
- [ ] GUIWeaponTable — **fehlt komplett**, ist inhaltlich kein Turret (eigenständiger Munitions-/Waffen-Modifikations-Tisch mit 3D-Vorschau), kein Block/Container/GUI im modernisierten Code vorhanden

Nebenfund (nicht Teil dieses Auftrags): alle 11 Turret-Items sind in
`CreativeModeTabEventHandler.java` (`populateWeaponsTab`, Zeilen ~309-320)
manuell auskommentiert — bewusster eigener Commit ("Creative tab"), daher
nicht automatisch angefasst.

### Pneumatik / Transportrohre — komplett unbearbeitet

Recherche (2026-08-09): alle 7 Original-Namen haben echte Container+GUI,
aber **kein Pneumo-Rohr-System existiert im modernisierten Code** (nur
funktional andere `ConveyorBlock*`-Familie). Braucht das komplette
Subsystem neu:
- [ ] GUIPneumoStorageAccess
- [ ] GUIPneumoStorageClutter
- [ ] GUIPneumoStorageExporter
- [ ] GUIPneumoStorageImporter
- [ ] GUIPneumoStorageMono
- [ ] GUIPneumoTube
- [ ] GUICartDestroyer

### Lagerung / Kisten

Recherche (2026-08-09):

**Bereits fertig (falsch als fehlend gelistet):**
- [x] GUICrateDesh / Iron / Steel / Tungsten → `*Crate*Menu`/`GUI*Crate` ✅
- [x] GUIBarrel → aufgeteilt in `GUIBarrelIron`/`GUIBarrelSteel` + `BarrelIronBlockEntity`/`BarrelSteelBlockEntity` ✅ (Original hatte nur einen gemeinsamen Container)

**Komplett fehlend (kein modernisiertes Pendant, entgegen ursprünglicher Annahme):**
- [ ] GUISafe
- [ ] GUILeadBox
- [ ] GUIFileCabinet
- [ ] GUIAmmoBag
- [ ] GUICasingBag
- [ ] GUIPlasticBag
- [ ] GUIToolBox
- [ ] GUIBatteryREDD

### Raumfahrt

Recherche (2026-08-09):
- [ ] GUISatDock — kein Block/BlockEntity vorhanden (nicht zu verwechseln mit `MachineSatLinkerBlock`, das ist ein anderes Gerät: Satelliten-Linking statt Chip-Lagerung)
- [ ] GUISoyuzCapsule — Flug-Entity (`SoyuzCapsuleEntity`+Renderer) existiert bereits, aber kein GUI/Menu für die Innenraum-Lagerung (nicht zu verwechseln mit `GUISoyuzLauncher`, das ist bereits fertiger Teil von Teil A)

### RBMK-Zusatzteile

Recherche (2026-08-09): alle 3 Original-Namen haben echte Container+GUI
(teils nur Spieler-Inventar, keine Machine-Slots). Block+BlockEntity
existierten im modernisierten Code bereits für alle drei → **gebaut
(2026-08-09)**:
- [x] GUIRBMKAutoloader — ✅ gebaut, echte Textur `gui_autoloader.png`. `createMenu()` gab vorher `null` zurück (Bug behoben). Original hatte 18 Slots (3×3 Input + 3×3 Output) + editierbaren Cycle-Schwellwert; modernisierte BE nutzt stattdessen 1 gemeinsamen 9-Slot-Puffer mit festen Schwellwerten — 1:1 nur im Rahmen dieser vereinfachten Architektur.
- [x] GUIRBMKControlAuto — ✅ gebaut, echte Textur `gui_rbmk_control_auto.png`, Textfelder für Level-/Heat-Kurve + Funktions-Auswahl-Buttons + Rod-Level-Balken. Power-Icon nicht restaurierbar (kein power-Feld, wie beim manuellen RBMKControl). **Hinweis:** der zugehörige Build-Agent ist mittendrin hängengeblieben (Menu+BlockEntity+Registrierung fertig, aber GUI-Klasse fehlte) — von mir manuell fertiggestellt.
- [x] GUIRBMKHeater — ✅ gebaut, echte Textur `gui_rbmk_heater.png`, Wasser-/Dampf-Tanks über bestehende `FluidTank.renderTank`. Original-Fluid-ID-Slot entfällt (BE hat keine Item-Slots, nur die beiden Tanks).

Build-Status aller 6 "direkt baubar"-Machines (CoreEmitter/Injector/Receiver
+ RBMKAutoloader/ControlAuto/Heater): `./gradlew compileJava` → `BUILD SUCCESSFUL`.

### Sonstige Blöcke

Recherche (2026-08-09):
- [x] GUICounterTorch — ✅ bereits fertig als `GUIRadioTorchCounter`/`RadioTorchCounterMenu` (Datei dokumentiert sich selbst als "Port of GUICounterTorch")
- [x] GUIDiode — kein echtes GUI nötig (Original ist reiner `GuiScreen` ohne Container, kein Inventar) — out of scope
- [ ] GUIForceField — Original hat 3 echte Slots, kein modernisiertes Pendant (kein Block/BE/GUI/Menu) — braucht Grundarbeit
- [ ] GUILemegeton — Original hat echte Crafting-Slots, kein modernisiertes Pendant — braucht Grundarbeit

### Reine Anzeige-/HUD-Screens (niedrige Priorität — keine echten Container/Slots)

Recherche (2026-08-09):

**Bereits fertig:**
- [x] GUIScreenDesignator → `DesignatorScreen.java` ✅
- [x] GUIScreenRadioAUTOCAL → `GUIRadioAutocal.java` ✅
- [x] GUIScreenRadioTelex → `GUIRadioTelex.java` ✅
- [x] GUIScreenRadioTorch → `GUIRadioTorchSimple.java` ✅ (vermutlich)
- [x] GUIScreenRadioTorchController → `GUIRadioTorchController.java` ✅
- [x] GUIScreenRadioTorchLogic → `GUIRadioTorchLogic.java` ✅
- [x] GUIScreenRadioTorchReader → `GUIRadioTorchReader.java` ✅

**Fehlend, echte reine Anzeige-Screens ohne Container (niedrige Priorität):**
GUIScreenBobble, GUIScreenBobmazon, GUIScreenClayTablet, GUIScreenFluid,
GUIScreenGuide, GUIScreenHolotape, GUIScreenPager, GUIScreenPreview,
GUIScreenRBMKDisplay, GUIScreenRBMKGauge, GUIScreenRBMKGraph,
GUIScreenRBMKIndicator, GUIScreenRBMKKeyPad, GUIScreenRBMKLever,
GUIScreenRBMKTerminal, GUIScreenSatCoord, GUIScreenSlicePrinter,
GUIScreenSnowglobe, GUIScreenToolAbility, GUIScreenWikiRender,
GUIBookLore, GUICalculator

**Achtung — fälschlich als "reine Anzeige" eingestuft, haben tatsächlich
einen echten Container/Inventar (höhere Priorität als der Rest dieser
Gruppe, separat bewerten):**
- [ ] GUIBook — extends `GuiContainer`, hat echten Container
- [ ] GuiFileList — ist kein eigenständiger Screen, sondern eine Scroll-Listen-Widget-Komponente innerhalb eines anderen Screens
- [ ] GuiInfoContainer — abstrakte Klasse mit echtem Container + NEI-Handler-Interface

**Kein Screen, keine Aktion nötig:**
- GUIElements — ist gar kein Screen, nur eine statische Helper-/Widget-Zeichenklasse

## Zusammenfassung Teil B (Stand 2026-08-09)

- **Bereits fertig, aber falsch als fehlend gelistet:** ~20 Einträge (ArcFurnaceLarge, AssemblyMachine, Diesel, Excavator, GasFlare, ReactorBreeding, Electrolyser x2, FurnaceCombo, RtgFurnace, ReactorZirnox, Watz, NukeBoy, alle 11 Turret-Varianten, 4 Crates, Barrel, CounterTorch, 7 Radio-Screens)
- **Neu gebaut (2026-08-09):** 4 (PrecAss, Turbofan, Heatex, Oilburner)
- **Direkt baubar (Block+BE existiert bereits):** 6 (CoreEmitter, CoreInjector, CoreReceiver, RBMKAutoloader, RBMKControlAuto, RBMKHeater)
- **Braucht erst Block/BlockEntity-Grundarbeit:** ~30 (u.a. AssemblyFactory, CompactLauncher, PlasmaForge, MachineCustom, LaunchTable, DiFurnace, Core, CoreStabilizer, Fusion*, ICF*, PA*, ReactorControl, 9 Nukes, WeaponTable, Safe/LeadBox/FileCabinet/etc., SatDock, ForceField, Lemegeton)
- **Komplettes Subsystem fehlt:** Pneumatik/Transportrohre (7 Einträge, braucht eigene Architektur)
- **Bewusst nicht angefasst:** PWR-Familie (aktive Entwicklerarbeit)
- **Niedrige Priorität, meist unbearbeitet:** ~24 reine HUD-Screens

## Nächster Schritt

Empfehlung: als nächstes die 9 "direkt baubar"-Einträge (CoreEmitter/
CoreInjector/CoreReceiver + die 3 RBMK-Zusatzteile) angehen, da dafür wie
bei PrecAss/Turbofan/Heatex/Oilburner kein Vorlauf nötig ist. Danach
Priorität mit dem Auftraggeber klären: Nukes (Content-Impact hoch, aber
Grundarbeit nötig) vs. Pneumatik-Subsystem vs. Lagerung-Kleinkram vs.
HUD-Screens (niedrigste Priorität).
