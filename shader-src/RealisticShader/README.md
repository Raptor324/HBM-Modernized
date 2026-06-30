# Realistic Shader – Minecraft 1.20.1 (V10)

Ein selbstgebautes Shaderpack für OptiFine oder Iris (1.20.1) mit Fokus auf:

- **Realistisches Wasser**: prozedurale Wellen-Normalmap (per finiter Differenz aus der echten
  Wellenformel berechnet, synchron mit der Vertex-Verformung), Fresnel-Reflexion, Sonnen-Glanzlicht,
  Tiefenfärbung
- **Bewegliches Gras & Blätter**: Wind-Animation über `block.properties` (IDs 10 = Gras/Pflanzen,
  11 = Blätter), inkl. passender Schatten-Animation
- **Schatten**: Shadow-Mapping mit echtem `SHADOW_QUALITY`-Regler (1x1 / 3x3 / 5x5 PCF), weiche
  Distanz-Abblendung statt harter Kante
- **Ambient Occlusion**: Screen-Space AO mit pro-Pixel rotierten Sample-Richtungen (kein
  Streifen-/Banding-Artefakt mehr wie in V9)
- **Bloom**: eigenständiger Bright-Pass + Blur-Composite-Pass, über `BLOOM_STRENGTH` regelbar
- **Composite-Pass**: ACES-Tonemapping, Entfernungsnebel (verstärkt bei Regen), goldene-Stunde-
  Farbstich, leichte Vignette, Gamma-Korrektur

## Was sich gegenüber V9 geändert hat (Bugfixes)

- `SHADOW_QUALITY`-Regler war im Shader-Menü vorhanden, hatte aber **keinerlei Effekt** auf den
  Code (Slider stand nur in `shaders.properties`, wurde nirgends gelesen). Jetzt steuert er
  wirklich die PCF-Kernelgröße der Schatten.
- `RAIN_STRENGTH`-Regler war ebenfalls wirkungslos. Jetzt skaliert er den Regen-Einfluss auf
  Schatten, Nebel und Farbgebung zusätzlich zum vanilla `rainStrength`.
- SSAO hatte sichtbares Streifen-Banding durch immer gleiche 8 Sample-Richtungen. Jetzt werden
  die Richtungen pro Pixel zufällig rotiert.
- Schatten-Pass (`shadow.fsh`) hatte einen anderen Alpha-Cutoff (0.3) als der sichtbare
  Geometrie-Pass (`gbuffers_terrain.fsh`, 0.1) – das erzeugte bei Blättern/Gras falsche
  Schattenkonturen. Beide nutzen jetzt denselben Cutoff (0.1).

## Installation

1. OptiFine oder Iris (+ Sodium) für Minecraft 1.20.1 installieren.
2. Den Ordner **„RealisticShader"** in `%appdata%/.minecraft/shaderpacks/` kopieren, oder die
   ZIP direkt dort ablegen (nicht entpacken nötig).
3. Im Spiel: Optionen → Video-Einstellungen → Shader-Pakete → "RealisticShader" auswählen.

## Einstellbare Optionen (im Shader-Menü)

- **shadowDistance** – Schattenreichweite
- **SUNPATHROTATE** – Sonnenwinkel
- **WAVING_STRENGTH** – Stärke der Gras-/Blätterbewegung
- **RAIN_STRENGTH** – Einfluss von Regen auf Licht/Nebel/Schatten
- **SHADOW_QUALITY** – Schattenqualität (Niedrig/Mittel/Hoch) – jetzt mit echter Wirkung
- **WATER_REFLECT** – Wasserreflexionen an/aus
- **BLOOM_STRENGTH** – Stärke des Bloom-Effekts (0 = aus)

## Hinweise zur Eigenständigkeit

Dieser Shader ist vollständig eigener Code. Konzeptionelle Ideen (z.B. Bright-Pass-Bloom,
rotierte SSAO-Samples, PCF-Schatten) sind allgemein bekannte Standardtechniken aus der
Echtzeit-Grafik und nicht von einem bestimmten Shaderpack kopiert – es wurde kein Code aus
anderen Packs übernommen.
