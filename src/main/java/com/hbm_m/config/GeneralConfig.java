package com.hbm_m.config;

/**
 * Общие флаги. Порт {@link com.hbm.config.GeneralConfig} (1.7.10).
 *
 * <p>Restport: die Felder sind ueber {@code ConfigSchema} (statische Bindung) in server.json einstellbar,
 * Standardwerte wie im Original-{@code loadFromConfig}. Die Nachbehandlung des Originals (528 schaltet LBSM ab,
 * ohne 528 sind die X528-Unterschalter aus) steckt in den Zugriffsmethoden unten.
 */
public final class GeneralConfig {

    /** Original enable528Mode. Wird teils in static final gelesen - Neustart noetig. */
    public static boolean enable528 = false;

    // R9 Weltgenerierung: wirksame Vorgaben des Originals (loadFromConfig)
    public static boolean enableDebugMode = false;
    public static boolean enablePlutoniumOre = false;
    /** 0 = aus, 1 = an, 2 = Weltschalter "Strukturen generieren" beachten (Original 1.03_enableDungeonSpawn true|false|flag). */
    public static int enableDungeons = 2;
    public static boolean enableMDOres = true;
    public static boolean enableMines = true;
    public static boolean enableRad = true;
    public static boolean enableVaults = true;

    /** Original 1.22_enableCrosshairs (im Original nirgends gelesen). */
    public static boolean enableCrosshairs = true;
    /** Original 1.31_enableSkyboxes: {@code RenderNTMSkybox} + {@code LevelRendererImpactMixin}. */
    public static boolean enableSkyboxes = true;
    /** Original 1.32_enableImpactWorldProvider: schaltet die WorldProviderNTM-Entsprechung (Impact-Mixins Licht/Himmel/Sterne/Wolken/Nebel/Morgenrot). */
    public static boolean enableImpactWorldProvider = true;
    /** Original 1.37_enableGuideBook (Lesestelle im Original auskommentiert). */
    public static boolean enableGuideBook = true;
    /** Original 1.99_enableExpensiveMode: Rezeptfassungen ueber {@code recipe.condition.ConfigRecipeFlags} ("expensive"), HUD-Plakette. */
    public static boolean enableExpensiveMode = false;

    // ── 528 ──
    /** Original X528_forceReasimBoilers. */
    public static boolean enable528ReasimBoilers = true;
    /** Original X528_enableBosniaSimulator. */
    public static boolean enable528BosniaSimulator = true;
    /** Original X528_enable528PressurizedRecipes: Druckfassungen der Chemiewerk-Rezepte ("528_pressurized"). */
    public static boolean enable528PressurizedRecipes = true;

    // ── LBSM ── Rezeptvarianten (Ruestung/Werkzeug/Chemie/Zentrifuge/Amboss/Crafting/Medizin) ueber ConfigRecipeFlags.
    // TODO(port): FullSchrab (Transmutator/ItemSchraranium) hat im Port keine Lesestelle; wirksam ausserdem: ShorterDecay, schrabRate, SafeCrates.
    /** Original enableLessBullshitMode. */
    public static boolean enableLBSM = false;
    public static boolean enableLBSMFullSchrab = true;
    public static boolean enableLBSMShorterDecay = true;
    public static boolean enableLBSMSimpleArmorRecipes = true;
    public static boolean enableLBSMSimpleToolRecipes = true;
    public static boolean enableLBSMSimpleAlloy = true;
    public static boolean enableLBSMSimpleChemsitry = true;
    public static boolean enableLBSMSimpleCentrifuge = true;
    public static boolean enableLBSMUnlockAnvil = true;
    public static boolean enableLBSMSimpleCrafting = true;
    public static boolean enableLBSMSimpleMedicineRecipes = true;
    public static boolean enableLBSMSafeCrates = true;
    /** Original LBSM_schrabOreRate. */
    public static int schrabRate = 20;

    /** Original: {@code if(enable528) enableLBSM = false}. */
    public static boolean lbsm() {
        return enableLBSM && !enable528;
    }

    /** Original: nur im 528-Modus wirksam (sonst in loadFromConfig auf false gesetzt). */
    public static boolean enable528BosniaSimulator() {
        return enable528 && enable528BosniaSimulator;
    }

    /** Original enable528ReasimBoilers nach loadFromConfig. */
    public static boolean reasimBoilers528() {
        return enable528 && enable528ReasimBoilers;
    }

    /** Original enable528PressurizedRecipes nach loadFromConfig. */
    public static boolean pressurizedRecipes528() {
        return enable528 && enable528PressurizedRecipes;
    }

    /** Original {@code true528()}; ExplosiveEnergistics (AE2) entfaellt und zaehlt als an. */
    public static boolean true528() {
        ModClothConfig c = ModClothConfig.get();
        return enable528 && enable528ReasimBoilers && !c.enable528ColtanSpawn && enable528BosniaSimulator
                && c.enable528NetherBurn && enable528PressurizedRecipes
                && c.enable528MachineGravity && c.coltanRate <= 2;
    }

    /** Original {@code trueExp()}: JSON-Rezeptueberschreibungen ({@code PrecAssRecipes.modified}) gibt es im Port nicht. */
    public static boolean trueExp() {
        return enableExpensiveMode;
    }

    private GeneralConfig() {
    }
}
