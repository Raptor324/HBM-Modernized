package com.hbm_m.api.space;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Umweltabfragen der HBM-Logik fuer Space-Addons (NTM Space: {@code CelestialBody}, {@code CBT_Atmosphere},
 * {@code AstronomyUtil}, {@code WorldProviderCelestial}).
 *
 * <p>Jede Methode hat einen Standard = <b>Erde</b>, mit dem sich HBM-Modernized ohne Addon exakt wie bisher verhaelt.
 * Ein Addon setzt genau eine Implementierung ueber {@link SpaceApi#setEnvironment(SpaceEnvironment)} und ueberschreibt
 * nur, was es braucht. Nur Vanilla-Typen in den Signaturen - auf 1.20.1-forge und 1.21.1-neoforge identisch.
 *
 * <p>Alle Methoden muessen auf Server <b>und</b> Client aufrufbar sein (Jetpacks/Fluegel laufen auf beiden Seiten,
 * Himmel nur auf dem Client) und duerfen {@code level == null} nicht erwarten.
 */
public interface SpaceEnvironment {

    /**
     * Normalschwerkraft in NTM-Einheiten (1:1 {@code AstronomyUtil.STANDARD_GRAVITY = 1.6F} = 0.08 Bloecke/Tick^2).
     * Schwellen im Original (z.B. {@code WingsMurk}: {@code > 1.5F}, {@code < 0.2F}) beziehen sich auf diese Einheit.
     */
    float STANDARD_GRAVITY = 1.6F;
    /** 1:1 {@code AstronomyUtil.PLAYER_GRAVITY_MODIFIER}: Umrechnung m/s^2 -> NTM-Einheit. */
    float PLAYER_GRAVITY_MODIFIER = STANDARD_GRAVITY / 9.81F;
    /** Druck der Erdatmosphaere in atm. */
    double STANDARD_PRESSURE = 1.0D;
    /** Raumtemperatur in Grad Celsius (= {@code FluidType.ROOM_TEMPERATURE}). */
    int STANDARD_TEMPERATURE = 20;

    /** Kennung der Erde (Standard-Himmelskoerper ohne Addon). */
    ResourceLocation EARTH = ResourceLocation.fromNamespaceAndPath("hbm_m", "earth");
    /** HBM-Wasser ({@code ModFluids.WATER}), Standard-Wassertabelle der Pumpen. */
    ResourceLocation HBM_WATER = ResourceLocation.fromNamespaceAndPath("hbm_m", "water");
    /** HBM-Luft ({@code ModFluids.AIR}); auf der Erde der einzige Atmosphaerenbestandteil mit {@link #STANDARD_PRESSURE}. */
    ResourceLocation HBM_AIR = ResourceLocation.fromNamespaceAndPath("hbm_m", "air");

    // ───────────────────────────── Schwerkraft / Orbit ─────────────────────────────

    /** Schwerkraft am Ort in NTM-Einheiten ({@link #STANDARD_GRAVITY} = Erde), Original {@code CelestialBody.getGravity}. */
    default float gravity(Level level, BlockPos pos) { return STANDARD_GRAVITY; }

    /**
     * Schwerkraft fuer ein Lebewesen (Original {@code CelestialBody.getGravity(EntityLivingBase)}: Wassermobs immer
     * Standard, im Orbit Stationsschwerkraft bzw. 0). Standard: {@link #gravity(Level, BlockPos)} am Standort.
     */
    default float gravity(LivingEntity entity) { return gravity(entity.level(), entity.blockPosition()); }

    /** true = Orbit-Dimension (kein Kippen von Maschinen, keine Wassertabelle, keine Atmosphaere). Original {@code CelestialBody.inOrbit}. */
    default boolean inOrbit(Level level) { return false; }

    // ───────────────────────────── Atmosphaere ─────────────────────────────

    /** Gesamtdruck der Atmosphaere am Ort in atm (Kuehltuerme, Kondensator, Ventilator, Ansaugstutzen). */
    default double pressure(Level level, BlockPos pos) { return STANDARD_PRESSURE; }

    /**
     * Partialdruck eines Gases (Fluid-Registry-ID) in atm, Original {@code CBT_Atmosphere.getFluidPressure} bzw.
     * {@code AtmosphereBlob}. Erde: {@link #HBM_AIR} = {@link #pressure(Level, BlockPos)}, alles andere 0.
     */
    default double gasPressure(Level level, BlockPos pos, ResourceLocation fluid) {
        return HBM_AIR.equals(fluid) ? pressure(level, pos) : 0.0D;
    }

    /**
     * Original {@code TileEntityMachineBase.breatheAir(world, x, y, z, amount)}: true = am Ort ist genug Sauerstoff
     * fuer Verbrennung (Luft >= 0.19 atm oder O2 >= 0.09 atm in Atmosphaere oder abgedichtetem Raum); verbraucht dabei
     * {@code amount} mB aus einem abgedichteten Raum. {@code amount = 0} prueft nur. Erde: immer true.
     */
    default boolean consumeOxygen(Level level, BlockPos pos, int amount) { return true; }

    /** Gas in die Umgebung abgeben (Original {@code CelestialBody.emitGas}: Fackeln, Ventilatoren, EntityChemical). Erde: nichts. */
    default void emitGas(Level level, BlockPos pos, ResourceLocation fluid, int mb) { }

    /**
     * Druck der <b>Koerper</b>-Atmosphaere in atm, ohne abgedichtete Raeume (Original {@code TileEntityCondenser} /
     * {@code MachineCondenser}/{@code MachineTower*}: {@code CelestialBody.inOrbit(world) || CBT_Atmosphere == null}
     * -> 0, sonst {@code atmosphere.getPressure()}). Kondensator und Kuehltuerme verdampfen unter 0.01 atm.
     * Erde: {@link #STANDARD_PRESSURE}.
     */
    default double bodyPressure(Level level) { return STANDARD_PRESSURE; }

    /**
     * Original {@code TileEntityMachineIntake.canCompress}: false bei Druck am Ort <= 0.01 atm; {@code mb = 0} prueft
     * nur (Tank voll). Sonst ausserhalb abgedichteter Raeume {@code mb} des Hauptgases der Koerper-Atmosphaere
     * entnehmen ({@code FT_Gaseous.capture}) -> true; in einem Raum {@code mb} aus einem Raum ohne Druckluft mit
     * >= 0.1 atm verbrauchen -> true, sonst false. Erde: immer true.
     */
    default boolean drawAtmosphere(Level level, BlockPos pos, int mb) { return true; }

    // ───────────────────────────── Lebewesen / Pflanzen ─────────────────────────────

    /**
     * Server-Tick je Lebewesen an der Stelle des Originals ({@code EntityEffectHandler.onUpdate}, nach Zeitbombe,
     * 528-Netherbrand und Kraterbiom-Strahlung): {@code getAtmosphereCached}, {@code handleOxy} (Ersticken),
     * {@code handleCorrosion} (aetzende Atmosphaere). Erde: nichts.
     */
    default void onLivingTick(LivingEntity entity) { }

    /**
     * Space-API (W-R4b-2): Lebewesen steht in einer Dekontaminationsdusche ({@code DeconBlockEntity}, jeden Server-Tick,
     * nach HBMs eigener Reinigung). Original NTM Space {@code TileEntityDecon.deconNeutron}: Koerper-Neutronenaktivierung
     * {@code * 0.899916} (mit Strahlung {@code neut / 20}) und {@code HazardTypeNeutron.decay} der Inventar-Gegenstaende.
     */
    default void onDecontaminate(LivingEntity entity) { }

    /**
     * Umgebungsstrahlung in RAD/s am Ort eines Lebewesens (Original {@code EntityEffectHandler.handleRadiationFX}:
     * in Koerper-/Orbit-Dimensionen bei Himmelslicht >= 14 {@code (sunPower - Druck * 4) * celestialRadMultiplier}).
     * HBM nimmt den groesseren Wert aus Chunk-/Nether-Strahlung und diesem. Erde: 0.
     */
    default float ambientRadiation(Level level, BlockPos pos) { return 0.0F; }

    /**
     * Original {@code EntityChemical} (Saatbrei): Erde wird nur zu Gras, wenn die Koerper-Atmosphaere Luft,
     * Duna-Luft, CO2 oder O2 enthaelt. Erde: true.
     */
    default boolean supportsPlantGrowth(Level level, BlockPos pos) { return true; }

    /**
     * Nach dem Gras-Wachstum durch Saatbrei (Original {@code ChunkAtmosphereManager.addGrowth(world, grass, x, y, z,
     * 4, 6)} und Umwandlung CO2/Duna-Luft -> Luft um {@code GRASS_GROWTH_CONVERSION * 24} mB). Erde: nichts.
     */
    default void onPlantGrowth(Level level, BlockPos pos) { }

    /**
     * Ein Chemiewerfer-Strahl ({@code EntityChemical}) trifft einen Block (Original: {@code FT_Terraformer} wandelt
     * {@code getTargetBlock()} in Erde und meldet Wachstum). Wird nach allen HBM-Effekten aufgerufen. Erde: nichts.
     */
    default void onChemicalImpact(Level level, BlockPos pos, net.minecraft.world.level.material.Fluid fluid) { }

    /** Umgebungstemperatur in Grad Celsius (Original {@code CBT_Temperature}). Erde: {@link #STANDARD_TEMPERATURE}. */
    default int temperature(Level level, BlockPos pos) { return STANDARD_TEMPERATURE; }

    // ───────────────────────────── Sonne / Himmel / Koerper ─────────────────────────────

    /**
     * Relative Sonnenleistung (Original {@code WorldProviderCelestial.getSunPower(provider, x, z)}: Abstand zur Sonne,
     * Finsternis, im Orbit stationsabhaengig). Erde: 1.0.
     */
    default float solarIntensity(Level level, BlockPos pos) { return 1.0F; }

    /** Kennung des Himmelskoerpers der Dimension (Bedrock-Erze, Satelliten, Ressourcen). Erde: {@link #EARTH}. */
    default ResourceLocation body(Level level) { return EARTH; }

    /**
     * Himmelskoerper, ueber dem eine Position liegt (Original {@code CelestialBody.getTarget}: im Orbit der umkreiste
     * Koerper der Station am Ort). Standard: {@link #body(Level)}.
     */
    default ResourceLocation targetBody(Level level, BlockPos pos) { return body(level); }

    /**
     * true = das Addon zeichnet den Himmel dieser Dimension selbst; HBM-Skybox-Zusaetze ({@code RenderNTMSkybox}) und
     * der Impact-Himmel (Staub/Feuer-Mixins) bleiben dann aus. Nur clientseitig gefragt. Erde: false.
     */
    default boolean overridesSky(Level level) { return false; }

    /**
     * Fluessigkeit der Wassertabelle (Fluid-Registry-ID) fuer Pumpen und das Endlosfass (Original {@code CBT_Water});
     * {@code null} = keine Tabelle (Pumpe arbeitet nicht). Erde: {@link #HBM_WATER}.
     */
    @Nullable
    default ResourceLocation waterTableFluid(Level level) { return HBM_WATER; }

    /**
     * W-R9A-5: Fang der Fisch-Granate auf diesem Koerper (Original {@code ModEventHandler.updateFish} +
     * {@code WorldProviderCelestial.getFish}; die Granate zieht wie im Original immer aus der Fisch-Kategorie).
     * {@code null} = Vanilla-Beutetabelle {@code gameplay/fishing/fish}. Erde: null. Nur Server.
     */
    @Nullable
    default net.minecraft.world.item.ItemStack fishingOverride(Level level, net.minecraft.util.RandomSource random) { return null; }
}
