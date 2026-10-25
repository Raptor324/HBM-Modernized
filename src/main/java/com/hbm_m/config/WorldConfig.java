package com.hbm_m.config;

/**
 * 1:1 {@code com.hbm.config.WorldConfig} - wirksame Standardwerte des Originals (Vorgaben aus {@code loadFromConfig},
 * z.B. Uran 7, Asbest 2, Meteorit 200). Restport: statisch an {@code ConfigSchema} gebunden (server.json),
 * Kategorien "ores" (2.xx) und "dungeons" (4.xx). capsuleStructure liest der Port aus {@link ModClothConfig}.
 */
public final class WorldConfig {

    private WorldConfig() { }

    // 2.Bxx Bedrock-Erz-Gewichte. Im Original liest nur BedrockOre (Nether: Glowstone/Phosphor/Quarz) sie,
    // die Overworld-Gewichte stammen aus dem alten Bedrock-Erz-System und sind dort tot.
    public static int bedrockIronSpawn = 100;
    public static int bedrockCopperSpawn = 200;
    public static int bedrockBoraxSpawn = 50;
    public static int bedrockChlorocalciteSpawn = 35;
    public static int bedrockAsbestosSpawn = 50;
    public static int bedrockNiobiumSpawn = 50;
    public static int bedrockNeodymiumSpawn = 50;
    public static int bedrockTitaniumSpawn = 100;
    public static int bedrockTungstenSpawn = 100;
    public static int bedrockGoldSpawn = 50;
    public static int bedrockUraniumSpawn = 35;
    public static int bedrockThoriumSpawn = 50;
    public static int bedrockCoalSpawn = 200;
    public static int bedrockNiterSpawn = 50;
    public static int bedrockFluoriteSpawn = 50;
    public static int bedrockRedstoneSpawn = 50;
    public static int bedrockRareEarthSpawn = 50;
    public static int bedrockBauxiteSpawn = 100;
    public static int bedrockEmeraldSpawn = 50;
    public static int bedrockGlowstoneSpawn = 100;
    public static int bedrockPhosphorusSpawn = 50;
    public static int bedrockQuartzSpawn = 100;

    public static boolean overworldOre = true;
    public static boolean netherOre = true;
    public static boolean endOre = true;

    public static int uraniumSpawn = 7;
    public static int thoriumSpawn = 7;
    public static int titaniumSpawn = 8;
    public static int sulfurSpawn = 5;
    public static int aluminiumSpawn = 7;
    public static int copperSpawn = 12;
    public static int fluoriteSpawn = 6;
    public static int niterSpawn = 6;
    public static int tungstenSpawn = 10;
    public static int leadSpawn = 6;
    public static int berylliumSpawn = 6;
    public static int ligniteSpawn = 2;
    public static int asbestosSpawn = 2;
    public static int rareSpawn = 6;
    public static int lithiumSpawn = 6;
    public static int cinnebarSpawn = 1;
    public static int gassshaleSpawn = 5;
    public static int gasbubbleSpawn = 12;
    public static int explosivebubbleSpawn = 0;
    public static int cobaltSpawn = 2;
    public static int oilSpawn = 100;
    public static int bedrockOilSpawn = 200;
    public static int meteoriteSpawn = 200;

    public static int ironClusterSpawn = 4;
    public static int titaniumClusterSpawn = 2;
    public static int aluminiumClusterSpawn = 3;
    public static int copperClusterSpawn = 4;
    public static int alexandriteSpawn = 100;

    public static int limestoneSpawn = 1;

    public static int netherUraniumuSpawn = 8;
    public static int netherTungstenSpawn = 10;
    public static int netherSulfurSpawn = 26;
    public static int netherPhosphorusSpawn = 24;
    public static int netherCoalSpawn = 8;
    public static int netherPlutoniumSpawn = 8;
    public static int netherCobaltSpawn = 2;

    public static int endTikiteSpawn = 8;

    public static boolean enableHematite = true;
    public static boolean enableMalachite = true;
    public static boolean enableBauxite = true;

    public static boolean enableSulfurCave = true;
    public static boolean enableAsbestosCave = true;

    public static int antennaStructure = 250;
    public static int atomStructure = 500;
    public static int dungeonStructure = 64;
    public static int satelliteStructure = 500;
    public static int dudStructure = 500;
    public static int spaceshipStructure = 1000;
    public static int barrelStructure = 5000;
    public static int geyserChlorine = 3000;
    public static int geyserVapor = 250;
    public static int capsuleStructure = 100;
    public static int arcticStructure = 500;
    public static int jungleStructure = 2000;
    public static int pyramidStructure = 4000;

    public static int broadcaster = 5000;
    public static int minefreq = 64;
    public static int radfreq = 5000;
    public static int vaultfreq = 2500;
}
