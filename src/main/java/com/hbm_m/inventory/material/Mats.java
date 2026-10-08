package com.hbm_m.inventory.material;

import static com.hbm_m.inventory.material.MaterialShapes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.inventory.material.Mats}: alle Giesserei-Materialien des Originals mit ihren IDs (Ordnungszahl
 * *100 + Massenzahl, Legierungen ab 30), Farben, Formen und Umwandlungen. Wo das Original das Ore Dictionary fragt
 * ({@code <praefix><Material>}), loest der Port ueber die Port-Item-IDs, die Vanilla-Items und Forge-Tags auf.
 */
public class Mats {

    public static List<NTMMaterial> orderedList = new ArrayList<>();
    public static Map<String, MaterialShapes> prefixByName = MaterialShapes.prefixByName;
    public static HashMap<Integer, NTMMaterial> matById = new HashMap<>();
    public static HashMap<String, NTMMaterial> matByName = new HashMap<>();
    /** Original {@code materialEntries}: feste Zuordnungen je Item. */
    public static Map<Item, List<MaterialStack>> materialEntries = new LinkedHashMap<>();
    /** Original {@code materialOreEntries}: je Erz-Schluessel (Forge-Tag, z. B. {@code forge:ores/iron}). */
    public static Map<String, List<MaterialStack>> materialOreEntries = new LinkedHashMap<>();
    /** Port: Erzbloecke ohne passenden Forge-Tag -> Erz-Schluessel. */
    public static Map<String, String> oreItemKeys = new HashMap<>();

    public static final int _VS = 0;
    public static final int _AS = 30;
    public static final int _ES = 20_000;

    //Vanilla and vanilla-like
    public static final NTMMaterial MAT_WOOD         = makeNonSmeltable(_VS + 3,   n("Wood"),               0x896727, 0x281E0B, 0x896727).setAutogen(STOCK, GRIP).n().port("wood");
    public static final NTMMaterial MAT_IVORY        = makeNonSmeltable(_VS + 4,   n("Bone"),               0xFFFEEE, 0x797870, 0xEDEBCA).setAutogen(GRIP).n().port("ivory");
    public static final NTMMaterial MAT_STONE        = makeSmeltable(_VS + 0,      n("Stone"),              0x7F7F7F, 0x353535, 0x4D2F23).n();
    public static final NTMMaterial MAT_CARBON       = makeAdditive(699,           n("Carbon"),             0x363636, 0x030303, 0x404040).setAutogen(WIRE, BLOCK).n().port("carbon");
    public static final NTMMaterial MAT_COAL         = makeNonSmeltable(600,       n("Coal"),               0x363636, 0x030303, 0x404040).setConversion(MAT_CARBON, 2, 1).setAutogen(FRAGMENT).n().port("coal");
    public static final NTMMaterial MAT_LIGNITE      = makeNonSmeltable(601,       n("Lignite"),            0x542D0F, 0x261508, 0x472913).setConversion(MAT_CARBON, 3, 1).setAutogen(FRAGMENT).n().port("lignite");
    public static final NTMMaterial MAT_COALCOKE     = make(610,                   n("CoalCoke")).setConversion(MAT_CARBON, 4, 3).n();
    public static final NTMMaterial MAT_PETCOKE      = make(611,                   n("PetCoke")).setConversion(MAT_CARBON, 4, 3).n();
    public static final NTMMaterial MAT_LIGCOKE      = make(612,                   n("LigniteCoke")).setConversion(MAT_CARBON, 4, 3).n();
    public static final NTMMaterial MAT_GRAPHITE     = make(620,                   n("Graphite")).setConversion(MAT_CARBON, 1, 1).n().port("graphite");
    public static final NTMMaterial MAT_DIAMOND      = makeNonSmeltable(1430,      n("Diamond"),            0xFFFFFF, 0x1B7B6B, 0x8CF4E2).setConversion(MAT_CARBON, 1, 1).setAutogen(FRAGMENT).n().port("diamond");
    public static final NTMMaterial MAT_IRON         = makeSmeltable(2600,         n("Iron"),               0xFFFFFF, 0x353535, 0xFFA259).setAutogen(FRAGMENT, DUST, PIPE, CASTPLATE, WELDEDPLATE, BLOCK).m().port("iron");
    public static final NTMMaterial MAT_GOLD         = makeSmeltable(7900,         n("Gold"),               0xFFFF8B, 0xC26E00, 0xE8D754).setAutogen(FRAGMENT, WIRE, NUGGET, DUST, DENSEWIRE, CASTPLATE, BLOCK).m().port("gold");
    public static final NTMMaterial MAT_REDSTONE     = makeSmeltable(_VS + 1,      n("Redstone"),           0xE3260C, 0x700E06, 0xFF1000).setAutogen(FRAGMENT, INGOT).n().port("redstone");
    public static final NTMMaterial MAT_OBSIDIAN     = makeSmeltable(_VS + 2,      n("Obsidian"),           0x3D234D).n();
    public static final NTMMaterial MAT_HEMATITE     = makeAdditive(2601,          n("Hematite"),           0xDFB7AE, 0x5F372E, 0x6E463D).m().port("hematite");
    public static final NTMMaterial MAT_WROUGHTIRON  = makeSmeltable(2602,         n("WroughtIron"),        0xFAAB89).m().port("wrought_iron");
    public static final NTMMaterial MAT_PIGIRON      = makeSmeltable(2603,         n("PigIron"),            0xFF8B59).m().port("pig_iron");
    public static final NTMMaterial MAT_METEORICIRON = makeSmeltable(2604,         n("MeteoricIron"),       0x715347).m().port("meteorite");
    public static final NTMMaterial MAT_MALACHITE    = makeAdditive(2901,          n("Malachite"),          0xA2F0C8, 0x227048, 0x61AF87).m().port("malachite");
    public static final NTMMaterial MAT_BAUXITE      = makeNonSmeltable(2902,      n("Bauxite"),            0xF4BA30, 0xAA320A, 0xE2560F).setAutogen(FRAGMENT).n().port("bauxite");
    public static final NTMMaterial MAT_CRYOLITE     = makeNonSmeltable(2903,      n("Cryolite"),           0xCBC2A4, 0x8B711F, 0x8B701A).setAutogen(FRAGMENT).n().port("cryolite");

    //Radioactive
    public static final NTMMaterial MAT_URANIUM      = makeSmeltable(9200,  n("Uranium"),                   0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK).m().port("uranium");
    public static final NTMMaterial MAT_U233         = makeSmeltable(9233,  n("Uranium233", "U233"),        0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(NUGGET, BILLET, DUST, BLOCK).m().port("u233");
    public static final NTMMaterial MAT_U235         = makeSmeltable(9235,  n("Uranium235", "U235"),        0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(NUGGET, BILLET, DUST, BLOCK).m().port("u235");
    public static final NTMMaterial MAT_U238         = makeSmeltable(9238,  n("Uranium238", "U238"),        0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK).m().port("u238");
    public static final NTMMaterial MAT_THORIUM      = makeSmeltable(9032,  n("Thorium232", "Th232", "Thorium"), 0xBF825F, 0x1C0000, 0xBF825F).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK).m().port("th232", "thorium");
    public static final NTMMaterial MAT_PLUTONIUM    = makeSmeltable(9400,  n("Plutonium"),                 0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, DUST, BLOCK).m().port("plutonium");
    public static final NTMMaterial MAT_RGP          = makeSmeltable(9401,  n("PlutoniumRG"),               0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK).m().port("pu_mix", "plutoniumrg");
    public static final NTMMaterial MAT_PU238        = makeSmeltable(9438,  n("Plutonium238", "Pu238"),     0xFFBC59, 0xFF8E2B, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK).m().port("pu238");
    public static final NTMMaterial MAT_PU239        = makeSmeltable(9439,  n("Plutonium239", "Pu239"),     0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK).m().port("pu239");
    public static final NTMMaterial MAT_PU240        = makeSmeltable(9440,  n("Plutonium240", "Pu240"),     0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK).m().port("pu240");
    public static final NTMMaterial MAT_PU241        = makeSmeltable(9441,  n("Plutonium241", "Pu241"),     0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK).m().port("pu241");
    public static final NTMMaterial MAT_RGA          = makeSmeltable(9501,  n("AmericiumRG"),               0xCEB3B9, 0x3A1C21, 0x93767B).setAutogen(NUGGET, BILLET, BLOCK).m().port("am_mix", "americiumrg");
    public static final NTMMaterial MAT_AM241        = makeSmeltable(9541,  n("Americium241", "Am241"),     0xCEB3B9, 0x3A1C21, 0x93767B).setAutogen(NUGGET, BILLET, BLOCK).m().port("am241");
    public static final NTMMaterial MAT_AM242        = makeSmeltable(9542,  n("Americium242", "Am242"),     0xCEB3B9, 0x3A1C21, 0x93767B).setAutogen(NUGGET, BILLET, BLOCK).m().port("am242");
    public static final NTMMaterial MAT_NEPTUNIUM    = makeSmeltable(9337,  n("Neptunium237", "Np237", "Neptunium"), 0xA6B2A6, 0x030F03, 0x647064).setAutogen(NUGGET, BILLET, DUST, BLOCK).m().port("neptunium");
    public static final NTMMaterial MAT_POLONIUM     = makeSmeltable(8410,  n("Polonium210", "Po210", "Polonium"), 0x968779, 0x3D1509, 0x715E4A).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK).m().port("polonium", "po210");
    public static final NTMMaterial MAT_TECHNETIUM   = makeSmeltable(4399,  n("Technetium99", "Tc99"),      0xFAFFFF, 0x576C6C, 0xCADFDF).setAutogen(FRAGMENT, NUGGET, BILLET, BLOCK).m().port("technetium", "tc99");
    public static final NTMMaterial MAT_RADIUM       = makeSmeltable(8826,  n("Radium226", "Ra226"),        0xFCFCFC, 0xADBFBA, 0xE9FAF6).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK).m().port("ra226");
    public static final NTMMaterial MAT_ACTINIUM     = makeSmeltable(8927,  n("Actinium227", "Ac227"),      0xECE0E0, 0x221616, 0x958989).setAutogen(NUGGET, BILLET).m().port("actinium");
    public static final NTMMaterial MAT_CO60         = makeSmeltable(2760,  n("Cobalt60", "Co60"),          0xC2D1EE, 0x353554, 0x8F72AE).setAutogen(NUGGET, BILLET, DUST).m().port("co60");
    public static final NTMMaterial MAT_AU198        = makeSmeltable(7998,  n("Gold198", "Au198"),          0xFFFF8B, 0xC26E00, 0xE8D754).setAutogen(NUGGET, BILLET, DUST).m().port("au198");
    public static final NTMMaterial MAT_PB209        = makeSmeltable(8209,  n("Lead209", "Pb209"),          0xB38A94, 0x12020E, 0x7B535D).setAutogen(NUGGET, BILLET, DUST).m().port("pb209");
    public static final NTMMaterial MAT_SCHRABIDIUM  = makeSmeltable(12626, n("Schrabidium"),               0x32FFFF, 0x005C5C, 0x32FFFF).setAutogen(NUGGET, WIRE, BILLET, DUST, DENSEWIRE, PLATE, CASTPLATE, BLOCK).m().port("schrabidium");
    public static final NTMMaterial MAT_SOLINIUM     = makeSmeltable(12627, n("Solinium"),                  0xA2E6E0, 0x00433D, 0x72B6B0).setAutogen(NUGGET, BILLET, BLOCK).m().port("solinium");
    public static final NTMMaterial MAT_SCHRABIDATE  = makeSmeltable(12600, n("Schrabidate"),               0x77C0D7, 0x39005E, 0x6589B4).setAutogen(DUST, DENSEWIRE, CASTPLATE, BLOCK).m().port("schrabidate");
    public static final NTMMaterial MAT_SCHRARANIUM  = makeSmeltable(12601, n("Schraranium"),               0x2B3227, 0x2B3227, 0x24AFAC).setAutogen(BLOCK).m().port("schraranium");
    public static final NTMMaterial MAT_GHIORSIUM    = makeSmeltable(12836, n("Ghiorsium336", "Gh336"),     0xF4EFE1, 0x2A3306, 0xC6C6A1).setAutogen(NUGGET, BILLET, BLOCK).m().port("gh336", "ghiorsium336");

    //Base metals
    public static final NTMMaterial MAT_TITANIUM      = makeSmeltable(2200, n("Titanium"),      0xF7F3F2, 0x4F4C4B, 0xA99E79).setAutogen(FRAGMENT, DUST, PLATE, DENSEWIRE, CASTPLATE, WELDEDPLATE, SHELL, BLOCK).m().port("titanium");
    public static final NTMMaterial MAT_COPPER        = makeSmeltable(2900, n("Copper"),        0xFDCA88, 0x601E0D, 0xC18336).setAutogen(FRAGMENT, WIRE, DUST, PLATE, DENSEWIRE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK).m().port("copper");
    public static final NTMMaterial MAT_TUNGSTEN      = makeSmeltable(7400, n("Tungsten"),      0x868686, 0x000000, 0x977474).setAutogen(FRAGMENT, WIRE, BOLT, DUST, DENSEWIRE, CASTPLATE, WELDEDPLATE, BLOCK).m().port("tungsten");
    public static final NTMMaterial MAT_ALUMINIUM     = makeSmeltable(1300, n("Aluminum"),      0xFFFFFF, 0x344550, 0xD0B8EB).setAutogen(FRAGMENT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK).m().port("aluminum", "aluminium");
    public static final NTMMaterial MAT_LEAD          = makeSmeltable(8200, n("Lead"),          0xA6A6B2, 0x03030F, 0x646470).setAutogen(FRAGMENT, NUGGET, WIRE, BOLT, DUST, PLATE, CASTPLATE, PIPE, BLOCK).m().port("lead");
    public static final NTMMaterial MAT_BISMUTH       = makeSmeltable(8300, n("Bismuth"),       0xB200FF, 0xB200FF, 0xB200FF).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK).m().port("bismuth");
    public static final NTMMaterial MAT_ARSENIC       = makeSmeltable(3300, n("Arsenic"),       0x6CBABA, 0x242525, 0x558080).setAutogen(NUGGET).m().port("arsenic");
    public static final NTMMaterial MAT_TANTALIUM     = makeSmeltable(7300, n("Tantalum"),      0xFFFFFF, 0x1D1D36, 0xA89B74).setAutogen(FRAGMENT, NUGGET, DUST, BLOCK).m().port("tantalium", "tantalum");
    public static final NTMMaterial MAT_NEODYMIUM     = makeSmeltable(6000, n("Neodymium"),     0xE6E6B6, 0x1C1C00, 0x8F8F5F).setAutogen(FRAGMENT, NUGGET, DUSTTINY, INGOT, DUST, DENSEWIRE, BLOCK).m().port("neodymium");
    public static final NTMMaterial MAT_NIOBIUM       = makeSmeltable(4100, n("Niobium"),       0xB76EC9, 0x2F2D42, 0xD576B1).setAutogen(FRAGMENT, NUGGET, DUSTTINY, DUST, DENSEWIRE, BLOCK).m().port("niobium");
    public static final NTMMaterial MAT_BERYLLIUM     = makeSmeltable(400,  n("Beryllium"),     0xB2B2A6, 0x0F0F03, 0xAE9572).setAutogen(FRAGMENT, NUGGET, DUST, BLOCK).m().port("beryllium");
    public static final NTMMaterial MAT_EMERALD       = makeNonSmeltable(401, n("Emerald"),     0xBAFFD4, 0x003900, 0x17DD62).setConversion(MAT_BERYLLIUM, 4, 3).setAutogen(FRAGMENT, DUST, GEM, BLOCK).n().port("emerald");
    public static final NTMMaterial MAT_COBALT        = makeSmeltable(2700, n("Cobalt"),        0xC2D1EE, 0x353554, 0x8F72AE).setAutogen(FRAGMENT, NUGGET, DUSTTINY, BILLET, DUST, BLOCK).m().port("cobalt");
    public static final NTMMaterial MAT_BORON         = makeSmeltable(500,  n("Boron"),         0xBDC8D2, 0x29343E, 0xAD72AE).setAutogen(FRAGMENT, DUSTTINY, DUST, BLOCK).m().port("boron");
    public static final NTMMaterial MAT_BORAX         = makeSmeltable(501,  n("Borax"),         0xFFFFFF, 0x946E23, 0xFFECC6).setAutogen(FRAGMENT, INGOT, DUST).n().port("borax");
    public static final NTMMaterial MAT_LANTHANIUM    = makeSmeltable(5700, n("Lanthanum"),     0xC8E0E0, 0x3B5353, 0xA1B9B9).setAutogen(FRAGMENT, BLOCK).m().port("lanthanium", "lanthanum");
    public static final NTMMaterial MAT_ZIRCONIUM     = makeSmeltable(4000, n("Zirconium"),     0xE3DCBE, 0x3E3719, 0xADA688).setAutogen(FRAGMENT, NUGGET, WIRE, DUSTTINY, BILLET, DUST, CASTPLATE, WELDEDPLATE, BLOCK).m().port("zirconium");
    public static final NTMMaterial MAT_SODIUM        = makeSmeltable(1100, n("Sodium"),        0xD3BF9E, 0x3A5A6B, 0x7E9493).setAutogen(FRAGMENT, INGOT, DUST).m().port("sodium");
    public static final NTMMaterial MAT_SODALITE      = makeNonSmeltable(1101, n("Sodalite"),   0xDCE5F6, 0x4927B4, 0x96A7E6).setAutogen(FRAGMENT, GEM).n().port("sodalite");
    public static final NTMMaterial MAT_STRONTIUM     = makeSmeltable(3800, n("Strontium"),     0xF1E8BA, 0x271E00, 0xCAC193).setAutogen(FRAGMENT, INGOT, DUST).m().port("strontium");
    public static final NTMMaterial MAT_CALCIUM       = makeSmeltable(2000, n("Calcium"),       0xCFCFA6, 0x747F6E, 0xB7B784).setAutogen(DUST).m().port("calcium");
    public static final NTMMaterial MAT_LITHIUM       = makeSmeltable(300,  n("Lithium"),       0xFFFFFF, 0x818181, 0xD6D6D6).setAutogen(FRAGMENT, DUST, BLOCK).m().port("lithium");
    public static final NTMMaterial MAT_SULFUR        = makeNonSmeltable(1600, n("Sulfur"),     0xFCEE80, 0xBDA022, 0xF1DF68).setAutogen(FRAGMENT, DUST, BLOCK).n().port("sulfur");
    public static final NTMMaterial MAT_KNO           = makeNonSmeltable(700,  n("Saltpeter"),  0xD4D4D4, 0x969696, 0xC9C9C9).setAutogen(FRAGMENT, DUST, BLOCK).n().port("kno", "niter");
    public static final NTMMaterial MAT_FLUORITE      = makeNonSmeltable(900,  n("Fluorite"),   0xFFFFFF, 0xB0A192, 0xE1DBD4).setAutogen(FRAGMENT, DUST, BLOCK).n().port("fluorite");
    public static final NTMMaterial MAT_PHOSPHORUS    = makeNonSmeltable(1500, n("RedPhosphorus"), 0xCB0213, 0x600006, 0xBA0615).setAutogen(FRAGMENT, DUST, BLOCK).n().port("phosphorus", "red_phosphorus");
    public static final NTMMaterial MAT_CHLOROCALCITE = makeNonSmeltable(1701, n("Chlorocalcite"), 0xF7E761, 0x475B46, 0xB8B963).setAutogen(FRAGMENT, DUST).n().port("chlorocalcite");
    public static final NTMMaterial MAT_MOLYSITE      = makeNonSmeltable(1702, n("Molysite"),   0xF9E97B, 0x216E00, 0xD0D264).setAutogen(FRAGMENT, DUST).n().port("molysite");
    public static final NTMMaterial MAT_CINNABAR      = makeNonSmeltable(8001, n("Cinnabar"),   0xD87070, 0x993030, 0xBF4E4E).setAutogen(FRAGMENT, GEM).n().port("cinnabar", "cinnebar");
    public static final NTMMaterial MAT_CADMIUM       = makeSmeltable(4800, n("Cadmium"),       0xFFFADE, 0x350000, 0xA85600).setAutogen(DUST).m().port("cadmium");
    public static final NTMMaterial MAT_SILICON       = makeSmeltable(1400, n("Silicon"),       0xD1D7DF, 0x1A1A3D, 0x878B9E).setAutogen(FRAGMENT, NUGGET, BILLET).m().port("silicon");
    public static final NTMMaterial MAT_ASBESTOS      = makeSmeltable(1401, n("Asbestos"),      0xD8D9CF, 0x616258, 0xB0B3A8).setAutogen(FRAGMENT, BLOCK).n().port("asbestos");
    public static final NTMMaterial MAT_OSMIRIDIUM    = makeSmeltable(7699, n("Osmiridium"),    0xDBE3EF, 0x7891BE, 0xACBDD9).setAutogen(NUGGET, CASTPLATE, WELDEDPLATE).m().port("osmiridium");

    //Alloys
    public static final NTMMaterial MAT_STEEL       = makeSmeltable(_AS + 0,  n("Steel"),              0xAFAFAF, 0x0F0F0F, 0x4A4A4A).setAutogen(DUSTTINY, BOLT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, GRIP).m().port("steel");
    public static final NTMMaterial MAT_MINGRADE    = makeSmeltable(_AS + 1,  n("Mingrade"),           0xFFBA7D, 0xAF1700, 0xE44C0F).setAutogen(WIRE, DUST, DENSEWIRE, BLOCK).m().port("red_copper", "mingrade");
    public static final NTMMaterial MAT_DURA        = makeSmeltable(_AS + 3,  n("DuraSteel"),          0x82A59C, 0x06281E, 0x42665C).setAutogen(BOLT, DUST, PLATE, CASTPLATE, PIPE, BLOCK, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, GRIP).m().port("dura_steel", "highspeed_steel");
    public static final NTMMaterial MAT_DESH        = makeSmeltable(_AS + 12, n("WorkersAlloy"),       0xFF6D6D, 0x720000, 0xF22929).setAutogen(DUST, CASTPLATE, BLOCK, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, STOCK, GRIP).m().port("desh");
    public static final NTMMaterial MAT_STAR        = makeSmeltable(_AS + 5,  n("Starmetal"),          0xCCCCEA, 0x11111A, 0xA5A5D3).setAutogen(DUST, DENSEWIRE, CASTPLATE, BLOCK).m().port("starmetal", "star_metal");
    public static final NTMMaterial MAT_FERRO       = makeSmeltable(_AS + 7,  n("Ferrouranium"),       0xB7B7C9, 0x101022, 0x6B6B8B).setAutogen(CASTPLATE, HEAVYBARREL, HEAVYRECEIVER).m().port("ferrouranium");
    public static final NTMMaterial MAT_TCALLOY     = makeSmeltable(_AS + 6,  n("TcAlloy"),            0xD4D6D6, 0x323D3D, 0x9CA6A6).setAutogen(DUST, CASTPLATE, WELDEDPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER).m().port("tcalloy");
    public static final NTMMaterial MAT_CDALLOY     = makeSmeltable(_AS + 13, n("CdAlloy"),            0xF7DF8F, 0x604308, 0xFBD368).setAutogen(CASTPLATE, WELDEDPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER).m().port("cdalloy");
    public static final NTMMaterial MAT_BBRONZE     = makeSmeltable(_AS + 16, n("BismuthBronze"),      0xE19A69, 0x485353, 0x987D65).setAutogen(CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER).m().port("bismuth_bronze", "bbronze");
    public static final NTMMaterial MAT_ABRONZE     = makeSmeltable(_AS + 17, n("ArsenicBronze"),      0xDB9462, 0x203331, 0x77644D).setAutogen(CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER).m().port("arsenic_bronze", "abronze");
    public static final NTMMaterial MAT_BSCCO       = makeSmeltable(_AS + 18, n("BSCCO"),              0x767BF1, 0x000000, 0x5E62C0).setAutogen(DENSEWIRE).m().port("bscco");
    public static final NTMMaterial MAT_MAGTUNG     = makeSmeltable(_AS + 8,  n("MagnetizedTungsten"), 0x22A2A2, 0x0F0F0F, 0x22A2A2).setAutogen(WIRE, DUST, DENSEWIRE, BLOCK).m().port("magnetized_tungsten");
    public static final NTMMaterial MAT_CMB         = makeSmeltable(_AS + 9,  n("CMBSteel"),           0x6F6FB4, 0x000011, 0x6F6FB4).setAutogen(DUST, PLATE, CASTPLATE, WELDEDPLATE, BLOCK).m().port("combine_steel", "cmb");
    public static final NTMMaterial MAT_DNT         = makeSmeltable(_AS + 15, n("Dineutronium"),       0x7582B9, 0x16000E, 0x455289).setAutogen(DUST, DENSEWIRE, BLOCK).m().port("dineutronium", "dnt");
    public static final NTMMaterial MAT_FLUX        = makeAdditive(_AS + 10,  n("Flux"),               0xF1E0BB, 0x6F6256, 0xDECCAD).setAutogen(DUST).n().port("flux");
    public static final NTMMaterial MAT_SLAG        = makeSmeltable(_AS + 11, n("Slag"),               0x554940, 0x34281F, 0x6C6562).setAutogen(INGOT, BLOCK).n().port("slag");
    public static final NTMMaterial MAT_MUD         = makeSmeltable(_AS + 14, n("WatzMud"),            0xBCB5A9, 0x481213, 0x96783B).n().port("mud");
    public static final NTMMaterial MAT_GUNMETAL    = makeSmeltable(_AS + 19, n("GunMetal"),           0xFFEF3F, 0xAD3600, 0xF9C62C).setAutogen(LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP).n().port("gunmetal");
    public static final NTMMaterial MAT_WEAPONSTEEL = makeSmeltable(_AS + 20, n("WeaponSteel"),        0xA0A0A0, 0x000000, 0x808080).setAutogen(CASTPLATE, SHELL, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP).n().port("weaponsteel");
    public static final NTMMaterial MAT_SATURN      = makeSmeltable(_AS + 4,  n("Saturnite"),          0x3AC4DA, 0x09282C, 0x30A4B7).setAutogen(PLATE, CASTPLATE, SHELL, BLOCK, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP).m().port("saturnite");

    //Extension
    public static final NTMMaterial MAT_RAREEARTH   = makeNonSmeltable(_ES + 0, n("RareEarth"),    0xC1BDBD, 0x384646, 0x7B7F7F).setAutogen(FRAGMENT).n().port("rare_earth", "rare");
    public static final NTMMaterial MAT_POLYMER     = makeNonSmeltable(_ES + 1, n("Polymer"),      0x363636, 0x040404, 0x272727).setAutogen(STOCK, GRIP).n().port("polymer");
    public static final NTMMaterial MAT_BAKELITE    = makeNonSmeltable(_ES + 2, n("Bakelite"),     0xF28086, 0x2B0608, 0xC93940).setAutogen(STOCK, GRIP).n().port("bakelite");
    public static final NTMMaterial MAT_RUBBER      = makeNonSmeltable(_ES + 3, n("Rubber"),       0x817F75, 0x0F0D03, 0x4B4A3F).setAutogen(PIPE, GRIP).n().port("rubber");
    public static final NTMMaterial MAT_HARDPLASTIC = makeNonSmeltable(_ES + 4, n("Polycarbonate"), 0xEDE7C4, 0x908A67, 0xE1DBB8).setAutogen(STOCK, GRIP).n().port("pc");
    public static final NTMMaterial MAT_PVC         = makeNonSmeltable(_ES + 5, n("PVC"),          0xFCFCFC, 0x9F9F9F, 0xF0F0F0).setAutogen(STOCK, GRIP).n().port("pvc");

    private static String[] n(String... names) { return names; }

    public static NTMMaterial makeSmeltable(int id, String[] dict, int color) { return makeSmeltable(id, dict, color, color, color); }

    public static NTMMaterial make(int id, String[] dict) {
        return new NTMMaterial(id, dict);
    }

    public static NTMMaterial makeSmeltable(int id, String[] dict, int solidColorLight, int solidColorDark, int moltenColor) {
        return new NTMMaterial(id, dict).smeltable(SmeltingBehavior.SMELTABLE).setSolidColor(solidColorLight, solidColorDark).setMoltenColor(moltenColor);
    }

    public static NTMMaterial makeAdditive(int id, String[] dict, int solidColorLight, int solidColorDark, int moltenColor) {
        return new NTMMaterial(id, dict).smeltable(SmeltingBehavior.ADDITIVE).setSolidColor(solidColorLight, solidColorDark).setMoltenColor(moltenColor);
    }

    public static NTMMaterial makeNonSmeltable(int id, String[] dict, int solidColorLight, int solidColorDark, int moltenColor) {
        return new NTMMaterial(id, dict).smeltable(SmeltingBehavior.NOT_SMELTABLE).setSolidColor(solidColorLight, solidColorDark).setMoltenColor(moltenColor);
    }

    // ═══════════════════════════ Item <-> Form (Ersatz fuer das Ore Dictionary) ═══════════════════════════

    /** Material und Form eines Formen-Items (Original: {@code <praefix><Name>} im Ore Dictionary). */
    public record MatShape(NTMMaterial material, MaterialShapes shape) { }

    private static Map<Item, MatShape> itemShapes;
    private static Map<NTMMaterial, Map<MaterialShapes, Item>> shapeItems;
    private static Map<String, NTMMaterial> matByTagName;

    private static synchronized void buildItemTables() {
        if (itemShapes != null) return;

        Map<Item, MatShape> rev = new IdentityHashMap<>();
        Map<NTMMaterial, Map<MaterialShapes, Item>> fwd = new IdentityHashMap<>();
        Map<String, NTMMaterial> tagNames = new HashMap<>();

        // Vanilla zuerst, wie die Ore-Dictionary-Eintraege des Originals
        vanilla(rev, fwd, MAT_IRON, INGOT, Items.IRON_INGOT);
        vanilla(rev, fwd, MAT_IRON, NUGGET, Items.IRON_NUGGET);
        vanilla(rev, fwd, MAT_IRON, BLOCK, Items.IRON_BLOCK);
        vanilla(rev, fwd, MAT_GOLD, INGOT, Items.GOLD_INGOT);
        vanilla(rev, fwd, MAT_GOLD, NUGGET, Items.GOLD_NUGGET);
        vanilla(rev, fwd, MAT_GOLD, BLOCK, Items.GOLD_BLOCK);
        vanilla(rev, fwd, MAT_COPPER, INGOT, Items.COPPER_INGOT);
        vanilla(rev, fwd, MAT_COPPER, BLOCK, Items.COPPER_BLOCK);
        vanilla(rev, fwd, MAT_REDSTONE, DUST, Items.REDSTONE);
        vanilla(rev, fwd, MAT_REDSTONE, BLOCK, Items.REDSTONE_BLOCK);
        vanilla(rev, fwd, MAT_COAL, GEM, Items.COAL);
        vanilla(rev, fwd, MAT_COAL, BLOCK, Items.COAL_BLOCK);
        vanilla(rev, fwd, MAT_DIAMOND, GEM, Items.DIAMOND);
        vanilla(rev, fwd, MAT_DIAMOND, BLOCK, Items.DIAMOND_BLOCK);
        vanilla(rev, fwd, MAT_EMERALD, GEM, Items.EMERALD);
        vanilla(rev, fwd, MAT_EMERALD, BLOCK, Items.EMERALD_BLOCK);

        // Port-Items mit eigenem Namen (Original: gemLignite, gemCoalCoke ...)
        special(rev, fwd, MAT_LIGNITE, GEM, "lignite");
        special(rev, fwd, MAT_COALCOKE, GEM, "coal_coke");
        special(rev, fwd, MAT_PETCOKE, GEM, "coke_petroleum");
        special(rev, fwd, MAT_LIGCOKE, GEM, "lignite_coke");
        special(rev, fwd, MAT_CINNABAR, GEM, "cinnebar");
        special(rev, fwd, MAT_BORAX, DUST, "borax");
        special(rev, fwd, MAT_SLAG, INGOT, "ingot_slag");
        special(rev, fwd, MAT_SLAG, BLOCK, "block_slag");
        special(rev, fwd, MAT_FLUX, DUST, "flux_powder");

        for (NTMMaterial mat : orderedList) {
            for (String name : mat.names) tagNames.put(snake(name), mat);
            for (String alias : mat.portIds) {
                tagNames.putIfAbsent(alias, mat);
                for (MaterialShapes shape : allShapes) {
                    for (String pattern : shape.portPatterns) {
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, String.format(pattern, alias)));
                        if (item == null || item == Items.AIR) continue;
                        rev.putIfAbsent(item, new MatShape(mat, shape));
                        fwd.computeIfAbsent(mat, k -> new IdentityHashMap<>()).putIfAbsent(shape, item);
                    }
                }
            }
        }

        shapeItems = fwd;
        matByTagName = tagNames;
        itemShapes = rev;
    }

    private static void vanilla(Map<Item, MatShape> rev, Map<NTMMaterial, Map<MaterialShapes, Item>> fwd, NTMMaterial mat, MaterialShapes shape, Item item) {
        rev.put(item, new MatShape(mat, shape));
        fwd.computeIfAbsent(mat, k -> new IdentityHashMap<>()).putIfAbsent(shape, item);
    }

    private static void special(Map<Item, MatShape> rev, Map<NTMMaterial, Map<MaterialShapes, Item>> fwd, NTMMaterial mat, MaterialShapes shape, String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
        if (item != null && item != Items.AIR) vanilla(rev, fwd, mat, shape, item);
    }

    /** "WorkersAlloy" -> "workers_alloy" (Forge-Tag-Schreibweise). */
    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    /** Form und Material eines Items, oder {@code null}. Port-Items und Vanilla zuerst, dann Forge-Tags fremder Mods. */
    @Nullable
    public static MatShape getShape(ItemStack stack) {
        if (stack.isEmpty()) return null;
        buildItemTables();
        MatShape direct = itemShapes.get(stack.getItem());
        if (direct != null) return direct;

        for (TagKey<Item> tag : (Iterable<TagKey<Item>>) stack.getTags()::iterator) {
            ResourceLocation loc = tag.location();
            if (!loc.getNamespace().equals("forge")) continue;
            String path = loc.getPath();
            int slash = path.indexOf('/');
            if (slash < 0) continue;
            String folder = path.substring(0, slash);
            NTMMaterial mat = matByTagName.get(path.substring(slash + 1));
            if (mat == null) continue;
            for (MaterialShapes shape : allShapes) {
                for (String f : shape.tagFolders) {
                    if (f.equals(folder)) return new MatShape(mat, shape);
                }
            }
        }
        return null;
    }

    /** Original {@code MoldShape.getOutput}: das Item einer Form, NTM-Items bevorzugt. */
    @Nullable
    public static Item getItemForShape(NTMMaterial mat, MaterialShapes shape) {
        buildItemTables();
        Map<MaterialShapes, Item> map = shapeItems.get(mat);
        return map != null ? map.get(shape) : null;
    }

    // ═══════════════════════════ Materialien aus Items ═══════════════════════════

    /** will not respect stacksizes - all stacks will be treated as a singular */
    public static List<MaterialStack> getMaterialsFromItem(ItemStack stack) {
        List<MaterialStack> list = new ArrayList<>();
        if (stack.isEmpty()) return list;
        MatDistribution.ensureLoaded();

        boolean found = false;

        // Erz-Eintraege (Original: Ore-Dictionary-Name in materialOreEntries)
        String oreKey = oreItemKeys.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (oreKey == null) {
            for (TagKey<Item> tag : (Iterable<TagKey<Item>>) stack.getTags()::iterator) {
                String key = tag.location().toString();
                if (materialOreEntries.containsKey(key)) { oreKey = key; break; }
            }
        }
        if (oreKey != null) {
            list.addAll(materialOreEntries.get(oreKey));
            found = true;
        }

        if (!found) {
            MatShape shape = getShape(stack);
            if (shape != null) {
                NTMMaterial material = shape.material();
                if (material.smeltsInto.smeltable == SmeltingBehavior.SMELTABLE || material.smeltsInto.smeltable == SmeltingBehavior.ADDITIVE) {
                    list.add(new MaterialStack(material, shape.shape().q(1)));
                }
            }
        }

        List<MaterialStack> entries = materialEntries.get(stack.getItem());
        if (entries != null) {
            entries.forEach(x -> { if (x != null) list.add(x); });
        }

        MaterialStack scrap = com.hbm_m.item.material.ItemScraps.getMats(stack);
        if (scrap != null) list.add(scrap);

        return list;
    }

    public static List<MaterialStack> getSmeltingMaterialsFromItem(ItemStack stack) {
        List<MaterialStack> baseMats = getMaterialsFromItem(stack);
        List<MaterialStack> smelting = new ArrayList<>();
        baseMats.forEach(x -> smelting.add(new MaterialStack(x.material.smeltsInto, (int) (x.amount * x.material.convOut / x.material.convIn))));
        return smelting;
    }

    public static class MaterialStack {
        public final NTMMaterial material;
        public int amount;

        public MaterialStack(NTMMaterial material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public MaterialStack copy() {
            return new MaterialStack(material, amount);
        }
    }

    public static String formatAmount(int amount, boolean showInMb) {

        if (showInMb) return (amount * 2) + "mB";

        StringBuilder format = new StringBuilder();

        int blocks = amount / BLOCK.q(1);
        amount -= BLOCK.q(blocks);
        int ingots = amount / INGOT.q(1);
        amount -= INGOT.q(ingots);
        int nuggets = amount / NUGGET.q(1);
        amount -= NUGGET.q(nuggets);
        int quanta = amount;

        if (blocks > 0) format.append(Component.translatable(blocks == 1 ? "matshape.block" : "matshape.blocks", blocks).getString()).append(" ");
        if (ingots > 0) format.append(Component.translatable(ingots == 1 ? "matshape.ingot" : "matshape.ingots", ingots).getString()).append(" ");
        if (nuggets > 0) format.append(Component.translatable(nuggets == 1 ? "matshape.nugget" : "matshape.nuggets", nuggets).getString()).append(" ");
        if (quanta > 0) format.append(Component.translatable(quanta == 1 ? "matshape.quantum" : "matshape.quanta", quanta).getString()).append(" ");

        return format.toString().trim();
    }

    /** Hilfsfunktion fuer Rezepttabellen: alle Erz-Schluessel und festen Eintraege. */
    public static Iterable<Entry<String, List<MaterialStack>>> oreEntries() {
        MatDistribution.ensureLoaded();
        return materialOreEntries.entrySet();
    }
}
