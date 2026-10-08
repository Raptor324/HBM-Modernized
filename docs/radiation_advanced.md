# Strahlungssystem „Advanced (NTM Next based)“

Wählbares Welt-Strahlungssystem neben Raptors klassischem Chunk-System. Code: `src/main/java/com/hbm_m/radiation/ntmnext/`, Daten: `src/main/resources/data/hbm_m/ntm_radiation/`.

## Umschalten

Im Config-GUI stellst du **World radiation system** (`radiationSystem`) auf `RAPTOR` (Standard) oder `ADVANCED`. Der Wert wird beim Serverstart übernommen, also die Welt danach neu laden.

Beide Systeme speichern getrennt:

- **Raptor:** speichert in der Chunk-Capability.
- **Advanced:** speichert in `<welt>/<dimension>/hbm_m/radiation_ntmnext/`.

Zurückschalten ist verlustfrei.

## Funktionsweise

| Stufe | Inhalt |
|---|---|
| 1 | Port des NTM-Next-Feldes (Java-Pfad). Jede Chunk-Sektion (16³) ist „uniform“ oder wird durch abschirmende Blöcke in getrennte Luft-Pockets geteilt. Die Dichte diffundiert über gemeinsame Flächen (exakter Zwei-Zellen-Austausch, Sweeps X/Y/Z im eigenen Thread-Pool, asynchron zum Server-Tick) und zerfällt mit einer Halbwertszeit. Nebel und Weltzerstörung bei hoher Dichte. |
| 2 | Getrennte Felder für **Gamma, Neutron und Beta**. Quellen bekommen einen Arten-Mix aus dem Block an der Emissionsposition (`source_mix/`). **Materialabschirmung** über Halbwertsschichten je Block und Art (`shielding/`, sonst Ableitung aus den Blockeigenschaften). **Nahfeld:** Abstandsquadratgesetz und Beer-Lambert-Dämpfung entlang des Strahls zu wiederholt emittierenden Blöcken. |
| 3 | **Kontamination** als eigene Ebene: Material je Sektion in den Gruppen SHORT, MEDIUM, LONG und EXOTIC, zerfällt exakt exponentiell und speist das Feld. Emissionen in Luft und einmalige Ereignisse (Explosion, Fallout, Gas, Leck) bleiben zum Teil als Kontamination liegen. Absorber und Dekon (`decrementRad`) bauen sie ab. Alpha nur als API (`alphaHazardAt`). **Spaltenmodell** für Wände: eine einzelne dichte Schicht dämpft gemäß Beer-Lambert. |
| 4 | **Wetter:** Frischer Fallout ist zunächst luftgetragen. Der **Wind** (deterministisch aus Seed und Zeit, mit Böen, nur in Dimensionen mit Himmel) verfrachtet ihn über das Gelände (Upwind-Verfahren, massenerhaltend). Der luftgetragene Anteil sinkt ab, **Regen** und Schnee waschen ihn schneller aus. Bei Regen fließt etwas Bodenkontamination in Senken. In **Gewässern** gleicht sie sich zwischen Nachbarn aus und wird im Ozean verdünnt. In ungeladene Chunks wandert nichts; das Material sammelt sich am Rand. |

Für Spieler und Items liefert `ChunkRadiationManager.getRadiation` die gewichtete RAD/s-Summe aller Arten plus Nahfeld in HBM-Größenordnung. Raptors Spielerseite ist unverändert.

## Konfiguration (`data/hbm_m/ntm_radiation/config.json`, per Datapack überschreibbar)

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `diffusivity`, `half_life_seconds` | 10, 120 | Ausbreitung und Zerfall des Feldes |
| `fog_rad`, `fog_chance`, `world_rad_effects` | 100, 20, true | Nebel bzw. Weltzerstörung |
| `rad_tick_rate` | 1 | Simulationsschritt alle n Ticks |
| `types.<art>.weight` | 1 / 1 / 1 | Gewicht in der RAD/s-Summe |
| `types.<art>.half_life_mult` | Gamma 1, Neutron 0,1, Beta 1 | Neutronen verschwinden schnell nach dem Abschalten |
| `types.<art>.diffusivity_mult`, `hvl_scale` | 1 | Feinabstimmung je Art |
| `default_mix` | γ0,6 / n0,1 / β0,3 | Mix für unbekannte Quellen |
| `effect_share` | 0,5 | Anteil der Schwellen je Art für Nebel/Zerstörung |
| `near_field.*` | Radius 8, gain 8, 32 Quellen, Cache 10 Ticks | Nahfeld |
| `wall_length_scale` | 16 | Wandwirkung im Feld |
| `contamination.enabled`, `one_off_fraction`, `decon_factor`, `tick_interval` | true, 0,5, 1, 20 | Kontamination |
| `contamination.fallout_split` | 70/25/5/0 % | Fallout-Aufteilung auf die Gruppen |
| `contamination.groups.<gruppe>` | siehe nächste Tabelle | Halbwertszeit, Emission, Arten-Mix |
| `weather.airborne_fraction`, `settle_rate` | 1,0, 1/120 s | Luftgetragener Anteil und Absinken |
| `weather.wind.*` | 1,5 ± 1 Blöcke/s, Drift 1200 s, Böen 0,5 | Wind, einzeln abschaltbar |
| `weather.rain.*` | Auswaschung 0,05/s, Schnee 0,02/s, Gewitter ×1,5, Abfluss 0,0005/s ab 2 Blöcken Gefälle | Regen, einzeln abschaltbar |
| `weather.water.*` | Tiefe ≥ 2, Austausch 0,01/s, Ozean-Halbwertszeit 2400 s | Gewässer, einzeln abschaltbar |

**Kontaminationsgruppen**

| Gruppe | Halbwertszeit | Emission | Mix |
|---|---|---|---|
| SHORT | 1 Spieltag | 0,003/s | γ0,4 / β0,6 |
| MEDIUM | 14 Spieltage | 0,003/s | γ0,5 / β0,5 |
| LONG | praktisch dauerhaft | 0,003/s | γ0,3 / β0,1 / α0,6 |
| EXOTIC | 100 Spieltage | 0,006/s | reines Gamma |

**Weitere Datapack-Ordner**

- `shielding/*.json`: `{"blocks":[...], "hvl":{"gamma":..,"neutron":..,"beta":..}, "priority":n}`
- `source_mix/*.json`: `{"blocks":[...], "mix":{...}, "contamination":{"short":..}, "priority":n}`
- `resistant/*.json`: vollständig abschirmende Blöcke
- `sources/*.json`: konstante Quellregeln wie im Original
- `<ns>:ntm_radiation/settings/<dim>.json`: Werte je Dimension, z. B. `ambient_rad`

Blockeinträge können `ns:id`, `#ns:tag` oder Muster mit `*` sein.

## Befehle (`/ntmrad`, OP)

| Befehl | Wirkung |
|---|---|
| `set <menge>` | Feld an der eigenen Position setzen (Mix des Blocks) |
| `clear` | Alle Daten der Dimension löschen |
| `info` | Feld und Nahfeld je Art, Summe, Kontamination je Gruppe (Boden/Luft), Zeit bis zur Halbierung, Alpha-Gefahr |
| `add <gamma\|neutron\|beta> <menge>` | Eintrag einer einzelnen Art |
| `contaminate <short\|medium\|long\|exotic> <menge>` | Bodenkontamination ablegen |
| `wind` | Aktuelle Windrichtung und -stärke |

## API

`com.hbm_m.radiation.ntmnext.NtmRadiationApi`:

- `fieldAt`, `nearFieldAt`, `totalAt`, `doseAt`
- `add`, `emit`, `set` (auch je Art)
- `contaminate`, `contaminateAirborne`, `contaminationAt`, `airborneAt`, `decontaminate`
- `alphaHazardAt`, `wind`, `halfValueLayer`, `sourceMix`

## Speicherformate

| Daten | Ort | Format |
|---|---|---|
| Feld Gamma | Hauptordner (kompatibel mit Stufe 1) | „NTX“ FMT 8 |
| Feld Neutron/Beta | Unterordner `neutron/` bzw. `beta/` | „NTX“ FMT 8 |
| Kontamination | Unterordner `contamination/` | Format 2 (Boden + Luft); Format 1 aus Stufe 3 wird gelesen |

## Lizenz

Die Kerndateien (Raster, Solver, Speicher, Emission, Diffusivität, Einstellungen, Weltwirkungen, Mixin) sind ein Port des Java-Pfades aus **NTM Next** (github.com/Warfactory-Official/ntm-next, Commit 3f9a261a).

- Autor: **movblock**; Anteile: Contributors to Hbm's Nuclear Tech Mod.
- Lizenz: **LGPL-3.0-only** (siehe `LICENSE.LESSER` und `THIRD_PARTY_NOTICES`). movblock hat die Nutzung des Java-Fallbacks bestätigt.
- Die native Bibliothek von NTM Next wurde weder verwendet noch nachgebaut.
- Die Erweiterungen der Stufen 2–4 sind eigener Port-Code.
