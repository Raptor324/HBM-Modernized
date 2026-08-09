# Machinen-Tab Review-Checkliste

Alle Items/Blöcke, die bisher im Machinen-Tab (`ntm_machines_tab`) waren,
wurden am 2026-08-09 komplett in den Dev-Tab (`ntm_dev_tab`) verschoben
(siehe `src/main/java/com/hbm_m/main/CreativeModeTabEventHandler.java`,
`populateMachinesTab`/`populateDevItemsTab`). Der Machinen-Tab ist jetzt
leer und wird nur noch mit dem gefüllt, was du hier als "fertig"
abgehakt hast.

**Update 2026-08-09:** 53 abgehakte Items wurden bereits in den Machinen-Tab
zurückverschoben (markiert mit ✅). Alles ohne dieses Symbol ist noch offen
oder wartet auf deine Prüfung.

## Wie du das benutzt

- Häkchen setzen (`- [ ]` → `- [x]`) bei allem, was du geprüft hast und
  für **fertig genug für den Machinen-Tab** hältst.
- Optional dahinter einen Kommentar schreiben, z.B.
  `- [x] TURBOFAN — GUI ok, Textur fehlt noch` — das hilft mir beim
  Einsortieren.
- Wenn ein Item eigentlich kaputt/Platzhalter ist und raus soll, schreib
  einfach `-> LÖSCHEN` oder `-> raus` dahinter statt es abzuhaken.
- Ich lese diese Datei regelmäßig und trage alles Abgehakte per Code
  wieder in `populateMachinesTab` ein (und entferne es aus
  `populateDevItemsTab`). Du musst dafür nichts committen, ich lese
  direkt den Dateistand.
- Neue, frisch von mir fertiggestellte Machines trage ich hier oben in
  der Liste mit `🆕` ein, damit du weißt, dass die zuerst getestet werden
  sollten.

## Frisch fertiggestellt (2026-08-09), zuerst testen

- [ ] MACHINE_PRECASS 🆕 — Precision Assembler, GUI+Menu neu gebaut (Bug behoben: zeigte vorher Advanced-Assembler-Platzhalter-GUI)
- [ ] TURBOFAN 🆕 — GUI+Menu neu gebaut, Upgrade-Slot des Originals entfällt (kein Upgrade-System am Block)
- [ ] HEATEX 🆕 — GUI+Menu neu gebaut, Fluid-Typ-Zuweisung + Zyklus-Textfelder aus Original entfallen (BlockEntity unterstützt das nicht)
- [ ] OILBURNER 🆕 / OILBURNER_HP 🆕 — GUI+Menu neu gebaut, Bug behoben (GUI wäre eingefroren, da Server nie an Client synced hat)
- [ ] CORE_EMITTER 🆕 — GUI+Menu neu gebaut (Fission-Core-System), Watt-Textbox/On-Off-Toggle des Originals entfallen
- [ ] CORE_INJECTOR 🆕 — GUI+Menu neu gebaut, zeigt Deuterium-/Tritium-Tanks
- [ ] CORE_RECEIVER 🆕 — GUI+Menu neu gebaut, Cryogel-Tank als Platzhalter-Füllrechteck (keine passende Textur gefunden)

RBMK-Teile (Autoloader/ControlAuto/Heater ebenfalls heute neu gebaut) sind
absichtlich NICHT hier gelistet, sondern zusammen mit dem restlichen
RBMK-System in den Dev-Tab verschoben, da du meintest RBMK generell noch
nicht kontrolliert zu haben.

## Lagerung (Kisten/Fässer)

- [x] CRATE_IRON ✅ (im Machinen-Tab)
- [x] CRATE_STEEL ✅ (im Machinen-Tab)
- [x] CRATE_TUNGSTEN ✅ (im Machinen-Tab)
- [x] CRATE_DESH ✅ (im Machinen-Tab)
- [x] CRATE_TEMPLATE ✅ (im Machinen-Tab)
- [ ] BARREL_CORRODED
- [x] BARREL_IRON ✅ (im Machinen-Tab)
- [x] BARREL_STEEL ✅ (im Machinen-Tab)
- [ ] BARREL_TCALLOY
- [ ] BARREL_PLASTIC

## Ambosse

- [x] ANVIL_IRON ✅ (im Machinen-Tab)
- [x] ANVIL_LEAD ✅ (im Machinen-Tab)
- [x] ANVIL_STEEL ✅ (im Machinen-Tab)
- [x] ANVIL_DESH ✅ (im Machinen-Tab)
- [x] ANVIL_FERROURANIUM ✅ (im Machinen-Tab)
- [x] ANVIL_SATURNITE ✅ (im Machinen-Tab)
- [x] ANVIL_BISMUTH_BRONZE ✅ (im Machinen-Tab)
- [x] ANVIL_ARSENIC_BRONZE ✅ (im Machinen-Tab)
- [x] ANVIL_SCHRABIDATE ✅ (im Machinen-Tab)
- [x] ANVIL_DNT ✅ (im Machinen-Tab)
- [x] ANVIL_OSMIRIDIUM ✅ (im Machinen-Tab)
- [x] ANVIL_MURKY ✅ (im Machinen-Tab)

## Maschinen (Teil 1)

- [x] PRESS ✅ (im Machinen-Tab)
- [x] BLAST_FURNACE ✅ (im Machinen-Tab)
- [x] BLAST_FURNACE_EXTENSION ✅ (im Machinen-Tab)
- [x] HEATING_OVEN ✅ (im Machinen-Tab)
- [x] STEAM_CONDENSER ✅ (im Machinen-Tab)
- [x] SHREDDER ✅ (im Machinen-Tab)
- [x] WOOD_BURNER ✅ (im Machinen-Tab)
- [x] MACHINE_SIREN ✅ (im Machinen-Tab)
- [x] CHEMICAL_PLANT ✅ (im Machinen-Tab)
- [x] CRUCIBLE ✅ (im Machinen-Tab)
- [x] FOUNDRY_BASIN ✅ (im Machinen-Tab)
- [x] FOUNDRY_CHANNEL ✅ (im Machinen-Tab)
- [x] FOUNDRY_OUTLET ✅ (im Machinen-Tab)
- [ ] GAS_CENTRIFUGE
- [x] CENTRIFUGE ✅ (im Machinen-Tab)
- [x] CRYSTALLIZER ✅ (im Machinen-Tab)
- [ ] BREEDER
- [ ] LARGE_PYLON
- [x] MACHINE_ASSEMBLER ✅ (im Machinen-Tab)
- [ ] ADVANCED_ASSEMBLY_MACHINE
- [ ] MACHINE_DIFURNACE_RTG
- [ ] MACHINE_TELEPORTER
- [ ] TELEANCHOR
- [x] MACHINE_RADAR ✅ (im Machinen-Tab)
- [ ] MACHINE_FAN
- [ ] MACHINE_DRAIN
- [ ] MACHINE_TRANSFORMER
- [ ] MACHINE_RTG
- [ ] MACHINE_WASTE_DRUM
- [ ] MACHINE_COMPRESSOR_COMPACT
- [ ] HYDRAULIC_FRACKINING_TOWER
- [ ] COOLING_TOWER
- [ ] TOWER_SMALL
- [ ] CYCLOTRON

## Platten (Guss/Geschweißt) — Crafting-Items, keine Blöcke

- [ ] PLATE_CAST_IRON
- [ ] PLATE_CAST_STEEL
- [ ] PLATE_CAST_COPPER
- [ ] PLATE_CAST_GOLD
- [ ] PLATE_CAST_TITANIUM
- [ ] PLATE_CAST_ALUMINIUM
- [ ] PLATE_CAST_TUNGSTEN
- [ ] PLATE_CAST_ZIRCONIUM
- [ ] PLATE_CAST_OSMIRIDIUM
- [ ] PLATE_CAST_ALLOY
- [ ] PLATE_CAST_DURA_STEEL
- [ ] PLATE_CAST_DESH
- [ ] PLATE_CAST_STAR_METAL
- [ ] PLATE_CAST_TCALLOY
- [ ] PLATE_CAST_CDALLOY
- [ ] PLATE_CAST_CMB
- [ ] PLATE_CAST_SCHRABIDIUM
- [ ] PLATE_CAST_BBRONZE
- [ ] PLATE_CAST_ABRONZE
- [ ] PLATE_CAST_SATURNITE
- [ ] PLATE_WELDED_IRON
- [ ] PLATE_WELDED_STEEL
- [ ] PLATE_WELDED_COPPER
- [ ] PLATE_WELDED_TITANIUM
- [ ] PLATE_WELDED_ALUMINIUM
- [ ] PLATE_WELDED_TUNGSTEN
- [ ] PLATE_WELDED_ZIRCONIUM
- [ ] PLATE_WELDED_OSMIRIDIUM
- [ ] PLATE_WELDED_TCALLOY
- [ ] PLATE_WELDED_CDALLOY
- [ ] PLATE_WELDED_CMB

## Gussformen (Molds) — Crafting-Items

- [ ] MOLD_BARREL_HEAVY
- [ ] MOLD_BARREL_LIGHT
- [ ] MOLD_BASE
- [ ] MOLD_BILLET
- [ ] MOLD_BLADE
- [ ] MOLD_BLADES
- [ ] MOLD_BLOCK
- [ ] MOLD_C357
- [ ] MOLD_CBUCKSHOT
- [ ] MOLD_GEM
- [ ] MOLD_GRIP
- [ ] MOLD_HULL_BIG
- [ ] MOLD_HULL_SMALL
- [ ] MOLD_INGOT
- [ ] MOLD_INGOTS
- [ ] MOLD_MECHANISM
- [ ] MOLD_MOGUS
- [ ] MOLD_NUGGET
- [ ] MOLD_PIPE
- [ ] MOLD_PIPES
- [ ] MOLD_PLATE
- [ ] MOLD_PLATE_CAST
- [ ] MOLD_PLATES
- [ ] MOLD_PLATES_CAST
- [ ] MOLD_RECEIVER_HEAVY
- [ ] MOLD_RECEIVER_LIGHT
- [ ] MOLD_SHELL
- [ ] MOLD_STAMP
- [ ] MOLD_STEEL_BASE
- [ ] MOLD_STOCK
- [ ] MOLD_WIRE
- [ ] MOLD_WIRE_DENSE
- [ ] MOLD_WIRES_DENSE

## Reaktor-Teile (Crafting-Items)

- [ ] PART_LITHIUM
- [ ] PART_BERYLLIUM
- [ ] PART_CARBON
- [ ] PART_COPPER
- [ ] PART_PLUTONIUM

## Maschinen (Teil 2)

- [ ] ZIRNOX
- [x] ARC_WELDER ✅ (im Machinen-Tab)
- [x] SOLDERING_STATION ✅ (im Machinen-Tab)
- [ ] MIXER *(war schon vorher auskommentiert im Original-Machinen-Tab — evtl. bewusst kaputt/WIP, bitte extra prüfen)*
- [x] DERRICK ✅ (im Machinen-Tab)
- [ ] MACHINE_WELL
- [ ] RBMK_CONSOLE *(Teil A: GUI wurde als "braucht Neuimplementierung" markiert, siehe gui-container-porting-roadmap.md)*
- [ ] FLARE_STACK
- [ ] PUMPJACK
- [ ] PUMP_STEAM
- [ ] PUMP_ELECTRIC
- [x] RADAR ✅ (im Machinen-Tab)
- [x] LARGE_RADAR ✅ (im Machinen-Tab)
- [ ] CRACKING_TOWER
- [x] FRACTION_TOWER ✅ (im Machinen-Tab)
- [x] MINING_DRILL ✅ (im Machinen-Tab)
- [ ] FEL
- [ ] SILEX
- [x] FLUID_TANK ✅ (im Machinen-Tab)
- [x] MACHINE_BATTERY_SOCKET ✅ (im Machinen-Tab)
- [ ] INDUSTRIAL_BOILER
- [ ] SOLAR_BOILER
- [ ] SOLAR_MIRRORS
- [ ] WATZ_POWERPLANT
- [ ] HYDROTREATER
- [ ] CATALYTIC_REFORMER
- [ ] DEUTERIUM_TOWER
- [ ] CHEMICAL_FACTORY
- [ ] STEAM_TURBINE
- [ ] LIQUEFACTOR
- [ ] CORE_EMITTER *(siehe "Frisch fertiggestellt" oben, dort abhaken)*
- [ ] CORE_INJECTOR *(siehe "Frisch fertiggestellt" oben, dort abhaken)*
- [ ] CORE_RECEIVER *(siehe "Frisch fertiggestellt" oben, dort abhaken)*
- [ ] VACUUM_DISTILL
- [ ] INDUSTRIAL_TURBINE
- [ ] TURBINE
- [ ] SUBSTATION
- [x] REFINERY ✅ (im Machinen-Tab)
- [x] MACHINE_BATTERY ✅ (im Machinen-Tab)
- [x] MACHINE_BATTERY_LITHIUM ✅ (im Machinen-Tab)
- [x] MACHINE_BATTERY_SCHRABIDIUM ✅ (im Machinen-Tab)
- [x] MACHINE_BATTERY_DINEUTRONIUM ✅ (im Machinen-Tab)
- [ ] CONVERTER_BLOCK
- [x] SWITCH ✅ (im Machinen-Tab)
- [ ] WIRE_COATED
- [x] GEIGER_COUNTER_BLOCK ✅ (im Machinen-Tab)
- [ ] DECON
- [ ] EMP
- [x] RAD_ABSORBER *(alle Tier-Varianten)* ✅ (im Machinen-Tab)

## Sonstiges im Dev-Tab, das evtl. auch schon fertig ist (nicht Teil des ursprünglichen Machinen-Tabs, aber ggf. sichten)

Der Dev-Tab enthält daneben noch einen riesigen, schon vorher existierenden
Block "WIP Machines (3D OBJ models)" sowie einen Bereich
"DEV: importierte fehlende Bloecke aus dem Original-HBM" — die waren
nie im Machinen-Tab und sind hier absichtlich **nicht** aufgeführt, da
sie laut Code-Kommentar explizit als "noch nicht fertig sortiert"
markiert sind. Wenn du davon auch etwas als fertig findest, schreib mir
einfach den `ModBlocks`/`ModItems`-Konstantennamen in einer neuen
Zeile unten in diese Datei — ich kümmere mich dann darum.

## Manuell ergänzt von dir

<!-- Hier kannst du frei Namen/Notizen ergänzen, die oben nicht gelistet sind -->
alle Cassette items