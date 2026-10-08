package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.PartTabMetaItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1-Port von {@code com.hbm.crafting.WeaponRecipes} (SEDNA-Teile, -Waffen, -Munition, -Mods, Geheimmunition,
 * Raketen/-teile, Minen) plus die SEDNA-abhaengigen Rezepte aus {@code ConsumableRecipes} (bomb_caller Atombombe).
 * Granatensystem (grenade_shell/fuze/filling/extra) portiert der Hauptagent separat.
 * OreDict-Abbildung: ANY_PLASTIC = Polymer/Bakelit, ANY_HARDPLASTIC = PC/PVC, ANY_RUBBER = (Bio-)Gummi,
 * ANY_RESISTANTALLOY = TC-/CD-Legierung, ANY_BISMOIDBRONZE = Bismut-/Arsenbronze, BIGMT = Saturnit,
 * DURA = Schnellarbeitsstahl, FERRO = Ferrouran; Waffenteile (ItemAutogen) = {@code part_<form>_<material>}.
 * circuit BASIC = integrated_circuit, ADVANCED = advanced_circuit, BISMOID = bismoid_circuit,
 * CAPACITOR_TANTALIUM = capacitor_tantalum, CHIP_BISMOID = bismoid_chip, CONTROLLER = controller.
 */
public final class WeaponRecipeGenerator {

    private WeaponRecipeGenerator() {}

    private static Ingredient tag(String ns, String path) {
        return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path)));
    }

    /** Waffenteil / Meta-Gegenstand aus {@link PartTabMetaItems}. */
    private static Item part(String id) {
        var sup = PartTabMetaItems.get(id);
        if (sup == null) throw new IllegalStateException("Teil fehlt: " + id);
        return sup.get();
    }

    private static Ingredient any(String... ids) {
        Item[] items = new Item[ids.length];
        for (int i = 0; i < ids.length; i++) items[i] = part(ids[i]);
        return Ingredient.of(items);
    }

    private static Item gun(String name) {
        return WeaponItems.gun(name);
    }

    private static Item ammo(EnumAmmo a) {
        return WeaponItems.ammo(a);
    }

    private static Item generic(EnumModGeneric e) {
        return WeaponItems.WEAPON_MOD_GENERIC.get(e).get();
    }

    private static Item special(EnumModSpecial e) {
        return WeaponItems.WEAPON_MOD_SPECIAL.get(e).get();
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "weapons");

        // ANY_SMOKELESS.dust() = ballistite, cordite; ANY_HIGHEXPLOSIVE.ingot() = ball_tnt, ball_tatb
        Ingredient anySmokeless = Ingredient.of(ModItems.BALLISTITE.get(), ModItems.CORDITE.get());
        Ingredient anyHighExplosive = Ingredient.of(ModItems.BALL_TNT.get(), ModItems.BALL_TATB.get());
        Ingredient anyRubber = Ingredient.of(m(ModMaterials.BIORUBBER, MaterialShape.INGOT), m(ModMaterials.RUBBER, MaterialShape.INGOT));
        Ingredient anyPlastic = Ingredient.of(m(ModMaterials.POLYMER, MaterialShape.INGOT), m(ModMaterials.BAKELITE, MaterialShape.INGOT));
        Ingredient anyHardPlastic = Ingredient.of(m(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT), m(ModMaterials.PVC, MaterialShape.INGOT));
        Ingredient keyPlanks = Ingredient.of(ItemTags.PLANKS);
        Ingredient keyPane = tag("forge", "glass_panes");
        Ingredient keyGreen = tag("forge", "dyes/green");
        Ingredient keyBlack = tag("forge", "dyes/black");
        Ingredient keyStick = tag("forge", "rods/wooden");
        Ingredient keyCobble = tag("forge", "cobblestone");

        // Materialien
        var steelIngot = m(ModMaterials.STEEL, MaterialShape.INGOT);
        var steelPlate = m(ModMaterials.STEEL, MaterialShape.PLATE);
        var steelCast = m(ModMaterials.STEEL, MaterialShape.PLATE_CAST);
        var steelBlock = m(ModMaterials.STEEL, MaterialShape.BLOCK);
        var gunmetalIngot = m(ModMaterials.GUNMETAL, MaterialShape.INGOT);
        var gunmetalPlate = m(ModMaterials.GUNMETAL, MaterialShape.PLATE);
        var wsteelIngot = m(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT);
        var wsteelPlate = m(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE);
        var wsteelCast = m(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE_CAST);
        var bigmtPlate = m(ModMaterials.SATURNITE, MaterialShape.PLATE);
        var bigmtCast = m(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST);
        var bigmtIngot = m(ModMaterials.SATURNITE, MaterialShape.INGOT);
        var duraIngot = m(ModMaterials.DURA_STEEL, MaterialShape.INGOT);
        var duraPlate = m(ModMaterials.DURA_STEEL, MaterialShape.PLATE);
        var duraCast = m(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST);
        var deshCast = m(ModMaterials.DESH, MaterialShape.PLATE_CAST);
        var ferroCast = m(ModMaterials.FERROURANIUM, MaterialShape.PLATE_CAST);
        var goldCast = m(ModMaterials.GOLD, MaterialShape.PLATE_CAST);
        var goldWireDense = m(ModMaterials.GOLD, MaterialShape.WIRE_DENSE);
        var rubberIngot = m(ModMaterials.RUBBER, MaterialShape.INGOT);
        var tiIngot = m(ModMaterials.TITANIUM, MaterialShape.INGOT);
        var star = m(ModMaterials.STARMETAL, MaterialShape.INGOT);
        var crystalRedstone = m(ModMaterials.REDSTONE, MaterialShape.CRYSTAL);
        Ingredient anyResistantCast = Ingredient.of(m(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST), m(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST));
        Ingredient anyResistantIngot = Ingredient.of(m(ModMaterials.TCALLOY, MaterialShape.INGOT), m(ModMaterials.CDALLOY, MaterialShape.INGOT));
        Ingredient anyBronzeCast = Ingredient.of(m(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST), m(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST));
        var basic = ModItems.INTEGRATED_CIRCUIT;
        var advanced = ModItems.ADVANCED_CIRCUIT;
        var bismoid = ModItems.BISMOID_CIRCUIT;

        // Waffenteile
        Item gmMech = part("part_mechanism_gunmetal"), wsMech = part("part_mechanism_weaponsteel"), bmMech = part("part_mechanism_saturnite");
        Item woodGrip = part("part_grip_wood"), woodStock = part("part_stock_wood");
        Ingredient plasticGrip = any("part_grip_polymer", "part_grip_bakelite"), plasticStock = any("part_stock_polymer", "part_stock_bakelite");
        Ingredient hardGrip = any("part_grip_pc", "part_grip_pvc"), hardStock = any("part_stock_pc", "part_stock_pvc");
        Ingredient resLightBarrel = any("part_barrel_light_tcalloy", "part_barrel_light_cdalloy");
        Ingredient resLightReceiver = any("part_receiver_light_tcalloy", "part_receiver_light_cdalloy");
        Ingredient resHeavyBarrel = any("part_barrel_heavy_tcalloy", "part_barrel_heavy_cdalloy");
        Ingredient resHeavyReceiver = any("part_receiver_heavy_tcalloy", "part_receiver_heavy_cdalloy");
        Ingredient bronzeLightBarrel = any("part_barrel_light_bbronze", "part_barrel_light_abronze");
        Ingredient bronzeLightReceiver = any("part_receiver_light_bbronze", "part_receiver_light_abronze");
        Ingredient bronzeHeavyReceiver = any("part_receiver_heavy_bbronze", "part_receiver_heavy_abronze");

        //Weapon mod table
        g.shaped(ModBlocks.MACHINE_WEAPON_TABLE.get(), 1, p("PPP", "TCT", "TST"), 'P', gunmetalPlate, 'T', steelIngot, 'C', Items.CRAFTING_TABLE, 'S', steelBlock);

        //SEDNA Parts
        g.shaped(part("part_stock_wood"), 1, p("WWW", "  W"), 'W', keyPlanks);
        g.shaped(part("part_grip_wood"), 1, p("W ", " W", " W"), 'W', keyPlanks);
        g.shaped(part("part_stock_polymer"), 1, p("WWW", "  W"), 'W', m(ModMaterials.POLYMER, MaterialShape.INGOT));
        g.shaped(part("part_grip_polymer"), 1, p("W ", " W", " W"), 'W', m(ModMaterials.POLYMER, MaterialShape.INGOT));
        g.shaped(part("part_stock_bakelite"), 1, p("WWW", "  W"), 'W', m(ModMaterials.BAKELITE, MaterialShape.INGOT));
        g.shaped(part("part_grip_bakelite"), 1, p("W ", " W", " W"), 'W', m(ModMaterials.BAKELITE, MaterialShape.INGOT));
        g.shaped(part("part_stock_pc"), 1, p("WWW", "  W"), 'W', m(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT));
        g.shaped(part("part_grip_pc"), 1, p("W ", " W", " W"), 'W', m(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT));
        g.shaped(part("part_stock_pvc"), 1, p("WWW", "  W"), 'W', m(ModMaterials.PVC, MaterialShape.INGOT));
        g.shaped(part("part_grip_pvc"), 1, p("W ", " W", " W"), 'W', m(ModMaterials.PVC, MaterialShape.INGOT));
        g.shaped(part("part_grip_rubber"), 1, p("W ", " W", " W"), 'W', rubberIngot);
        g.shaped(part("part_grip_ivory"), 1, p("W ", " W", " W"), 'W', Items.BONE);

        g.shaped(part("casing_shotshell"), 2, p("P", "C"), 'P', gunmetalPlate, 'C', part("casing_large"));
        g.shaped(part("casing_buckshot"), 2, p("P", "C"), 'P', anyPlastic, 'C', part("casing_large"));
        g.shaped(part("casing_buckshot_advanced"), 2, p("P", "C"), 'P', anyPlastic, 'C', part("casing_large_steel"));

        //SEDNA Guns
        g.shaped(gun("gun_pepperbox"), 1, p("IIW", "  C"), 'I', Items.IRON_INGOT, 'W', keyPlanks, 'C', Items.COPPER_INGOT);
        g.shaped(gun("gun_light_revolver"), 1, p("BRM", "  G"), 'B', part("part_barrel_light_steel"), 'R', part("part_receiver_light_steel"), 'M', gmMech, 'G', woodGrip);
        g.shaped(gun("gun_light_revolver_atlas"), 1, p(" M ", "MAM", " M "), 'M', wsMech, 'A', gun("gun_light_revolver"));
        g.shaped(gun("gun_henry"), 1, p("BRP", "BMS"), 'B', part("part_barrel_light_steel"), 'R', part("part_receiver_light_gunmetal"), 'M', gmMech, 'S', woodStock, 'P', gunmetalPlate);
        g.shaped(gun("gun_henry_lincoln"), 1, p(" M ", "PGP", " M "), 'M', wsMech, 'P', goldCast, 'G', gun("gun_henry"));
        g.shaped(gun("gun_greasegun"), 1, p("BRS", "SMG"), 'B', part("part_barrel_light_steel"), 'R', part("part_receiver_light_steel"), 'S', ModItems.BOLT_STEEL, 'M', gmMech, 'G', part("part_grip_steel"));
        g.shaped(gun("gun_maresleg"), 1, p("BRM", "BGS"), 'B', part("part_barrel_light_steel"), 'R', part("part_receiver_light_steel"), 'M', gmMech, 'G', ModItems.BOLT_STEEL, 'S', woodStock);
        g.shaped(gun("gun_maresleg_akimbo"), 1, p("SMS"), 'S', gun("gun_maresleg"), 'M', wsMech);
        g.shaped(gun("gun_flaregun"), 1, p("BRM", "  G"), 'B', part("part_barrel_heavy_steel"), 'R', part("part_receiver_light_steel"), 'M', gmMech, 'G', part("part_grip_steel"));
        g.shaped(gun("gun_am180"), 1, p("BRS", "GMG"), 'B', part("part_barrel_light_dura_steel"), 'R', part("part_receiver_light_dura_steel"), 'M', gmMech, 'G', woodGrip, 'S', woodStock);
        g.shaped(gun("gun_liberator"), 1, p("BB ", "BBM", "G G"), 'B', part("part_barrel_light_dura_steel"), 'M', gmMech, 'G', woodGrip);
        g.shaped(gun("gun_congolake"), 1, p("BM ", "BRS", "G  "), 'B', part("part_barrel_heavy_dura_steel"), 'M', gmMech, 'R', part("part_receiver_light_dura_steel"), 'S', woodStock, 'G', woodGrip);
        g.shaped(gun("gun_flamer"), 1, p(" MG", "BBR", " GM"), 'M', gmMech, 'G', part("part_grip_dura_steel"), 'B', part("part_barrel_heavy_dura_steel"), 'R', part("part_receiver_heavy_dura_steel"));
        g.shaped(gun("gun_flamer_topaz"), 1, p(" M ", "MFM", " M "), 'M', wsMech, 'F', gun("gun_flamer"));
        g.shaped(gun("gun_heavy_revolver"), 1, p("BRM", "  G"), 'B', part("part_barrel_light_desh"), 'R', part("part_receiver_light_desh"), 'M', gmMech, 'G', woodGrip);
        g.shaped(gun("gun_carbine"), 1, p("BRM", "G S"), 'B', part("part_barrel_light_desh"), 'R', part("part_receiver_light_desh"), 'M', gmMech, 'G', woodGrip, 'S', woodStock);
        g.shaped(gun("gun_uzi"), 1, p("BRS", " GM"), 'B', part("part_barrel_light_desh"), 'R', part("part_receiver_light_desh"), 'S', plasticStock, 'G', plasticGrip, 'M', gmMech);
        g.shaped(gun("gun_uzi_akimbo"), 1, p("UMU"), 'U', gun("gun_uzi"), 'M', wsMech);
        g.shaped(gun("gun_spas12"), 1, p("BRM", "BGS"), 'B', part("part_barrel_light_desh"), 'R', part("part_receiver_light_desh"), 'M', gmMech, 'G', plasticGrip, 'S', part("part_stock_desh"));
        g.shaped(gun("gun_panzerschreck"), 1, p("BBB", "PGM"), 'B', part("part_barrel_heavy_desh"), 'P', steelCast, 'G', part("part_grip_desh"), 'M', gmMech);
        g.shaped(gun("gun_star_f"), 1, p("BRM", "  G"), 'B', part("part_barrel_light_weaponsteel"), 'R', part("part_receiver_light_weaponsteel"), 'M', wsMech, 'G', plasticGrip);
        g.shaped(gun("gun_star_f_akimbo"), 1, p("UMU"), 'U', gun("gun_star_f"), 'M', bmMech);
        g.shaped(gun("gun_g3"), 1, p("BRM", "WGS"), 'B', part("part_barrel_light_weaponsteel"), 'R', part("part_receiver_light_weaponsteel"), 'M', wsMech, 'W', woodGrip, 'G', part("part_grip_rubber"), 'S', woodStock);
        g.shaped(gun("gun_g3_zebra"), 1, p(" M ", "MPM", " M "), 'M', bmMech, 'P', gun("gun_g3"));
        g.shaped(gun("gun_stinger"), 1, p("BBB", "PGM"), 'B', part("part_barrel_heavy_weaponsteel"), 'P', advanced, 'G', part("part_grip_weaponsteel"), 'M', wsMech);
        g.shaped(gun("gun_mk108"), 1, p(" GG", "BRM", " D "), 'G', plasticGrip, 'B', part("part_barrel_heavy_weaponsteel"), 'R', part("part_receiver_heavy_weaponsteel"), 'M', wsMech, 'D', part("shell_weaponsteel"));
        g.shaped(gun("gun_chemthrower"), 1, p("MHW", "PSS"), 'M', wsMech, 'H', part("pipe_rubber"), 'W', ModItems.WRENCH, 'P', part("part_barrel_heavy_weaponsteel"), 'S', part("shell_weaponsteel"));
        g.shaped(gun("gun_amat"), 1, p(" C ", "BRS", " MG"), 'G', woodGrip, 'B', part("part_barrel_heavy_ferrouranium"), 'R', part("part_receiver_heavy_ferrouranium"), 'M', wsMech, 'C', special(EnumModSpecial.SCOPE), 'S', woodStock);
        g.shaped(gun("gun_m2"), 1, p("  G", "BRM", "  G"), 'G', woodGrip, 'B', part("part_barrel_heavy_ferrouranium"), 'R', part("part_receiver_heavy_ferrouranium"), 'M', wsMech);
        g.shaped(gun("gun_autoshotgun"), 1, p("BRM", "G G"), 'B', part("part_barrel_heavy_ferrouranium"), 'R', part("part_receiver_heavy_ferrouranium"), 'M', wsMech, 'G', plasticGrip);
        g.shaped(gun("gun_autoshotgun_shredder"), 1, p(" M ", "MAM", " M "), 'M', bmMech, 'A', gun("gun_autoshotgun"));
        g.shaped(gun("gun_quadro"), 1, p("BCB", "BMB", "GG "), 'B', part("part_barrel_heavy_ferrouranium"), 'C', advanced, 'M', wsMech, 'G', plasticGrip);
        g.shaped(gun("gun_lag"), 1, p("BRM", "  G"), 'B', resLightBarrel, 'R', resLightReceiver, 'M', wsMech, 'G', plasticGrip);
        g.shaped(gun("gun_minigun"), 1, p("BMG", "BRE", "BGM"), 'B', resLightBarrel, 'M', wsMech, 'G', plasticGrip, 'R', resHeavyReceiver, 'E', ModItems.MOTOR_DESH);
        g.shaped(gun("gun_missile_launcher"), 1, p(" CM", "BBB", "G  "), 'C', advanced, 'M', wsMech, 'B', resHeavyBarrel, 'G', plasticGrip);
        g.shaped(gun("gun_tesla_cannon"), 1, p("CCC", "BRB", "MGE"), 'C', ModItems.COIL_COPPER, 'B', resHeavyBarrel, 'R', resHeavyReceiver, 'M', wsMech, 'G', plasticGrip, 'E', advanced);
        g.shaped(gun("gun_laser_pistol"), 1, p("CRM", "GG "), 'C', crystalRedstone, 'R', part("part_receiver_light_saturnite"), 'M', bmMech, 'G', hardGrip);
        g.shaped(gun("gun_laser_pistol_pew_pew"), 1, p(" M ", "MPM", " M "), 'M', bmMech, 'P', gun("gun_laser_pistol"));
        g.shaped(gun("gun_stg77"), 1, p(" D ", "BRS", "GGM"), 'D', special(EnumModSpecial.SCOPE), 'B', part("part_barrel_light_saturnite"), 'R', part("part_receiver_light_saturnite"), 'S', hardStock, 'G', hardGrip, 'M', bmMech);
        g.shaped(gun("gun_fatman"), 1, p("PPP", "BSR", "G M"), 'P', bigmtPlate, 'B', part("part_barrel_heavy_saturnite"), 'S', part("shell_saturnite"), 'R', part("part_receiver_heavy_saturnite"), 'G', hardGrip, 'M', bmMech);
        g.shaped(gun("gun_tau"), 1, p(" RD", "CTT", "GMS"), 'D', bismoid, 'C', ModItems.PIPE_COPPER, 'T', ModItems.COIL_COPPER_TORUS, 'G', hardGrip, 'R', part("part_receiver_light_saturnite"), 'M', bmMech, 'S', hardStock);
        g.shaped(gun("gun_lasrifle"), 1, p("DLC", "BRS", "MG "), 'D', crystalRedstone, 'L', special(EnumModSpecial.SCOPE), 'C', bismoid, 'B', bronzeLightBarrel, 'R', bronzeLightReceiver, 'S', hardStock, 'M', bmMech, 'G', hardGrip);
        g.shapeless(gun("gun_double_barrel_sacred_dragon"), 1, gun("gun_double_barrel"), ModItems.ITEM_SECRET_SELENIUM_STEEL);
        g.shaped(gun("gun_charge_thrower"), 1, p("MMM", "BBL", "GG "), 'M', gmMech, 'B', part("part_barrel_heavy_steel"), 'G', part("part_grip_steel"), 'L', Items.LEATHER);
        g.shaped(gun("gun_charge_thrower"), 1, p("MMM", "BBL", "GG "), 'M', gmMech, 'B', part("part_barrel_heavy_steel"), 'G', part("part_grip_steel"), 'L', anyRubber);
        g.shaped(gun("gun_drill"), 1, p(" GL", "IBP", " GL"), 'G', gunmetalIngot, 'L', anyRubber, 'I', tiIngot, 'B', steelBlock, 'P', ModItems.PISTON_SELENIUM);

        g.shaped(gun("gun_pa_melee"), 1, p(" C ", "MWM"), 'C', basic, 'M', ModItems.MOTOR, 'W', goldWireDense);
        g.shaped(gun("gun_pa_ranged"), 1, p("C", "W", "P"), 'C', basic, 'P', anyPlastic, 'W', goldWireDense);

        //SEDNA Ammo
        g.shaped(ammo(EnumAmmo.STONE), 6, p("C", "P", "G"), 'C', keyCobble, 'P', Items.PAPER, 'G', Items.GUNPOWDER);
        g.shaped(ammo(EnumAmmo.STONE_AP), 6, p("C", "P", "G"), 'C', Items.FLINT, 'P', Items.PAPER, 'G', Items.GUNPOWDER);
        g.shaped(ammo(EnumAmmo.STONE_SHOT), 6, p("C", "P", "G"), 'C', Items.GRAVEL, 'P', Items.PAPER, 'G', Items.GUNPOWDER);
        g.shaped(ammo(EnumAmmo.STONE_IRON), 6, p("C", "P", "G"), 'C', Items.IRON_INGOT, 'P', Items.PAPER, 'G', Items.GUNPOWDER);
        Item mortar = ammo(EnumAmmo.CT_MORTAR);
        g.shapeless(ammo(EnumAmmo.CT_MORTAR_CHARGE), 1, mortar, mortar, mortar, mortar, mortar, mortar, mortar, ModItems.DUCTTAPE, ModItems.DUCTTAPE);

        //SEDNA Mods
        var tape = ModItems.DUCTTAPE;
        g.shapeless(generic(EnumModGeneric.IRON_DAMAGE), 1, gunmetalIngot, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, tape);
        g.shapeless(generic(EnumModGeneric.IRON_DURA), 1, gunmetalIngot, Items.IRON_INGOT, tape);
        g.shapeless(generic(EnumModGeneric.STEEL_DAMAGE), 1, gmMech, steelCast, steelCast, steelCast, tape);
        g.shapeless(generic(EnumModGeneric.STEEL_DURA), 1, gunmetalPlate, steelCast, tape);
        g.shapeless(generic(EnumModGeneric.DURA_DAMAGE), 1, gmMech, duraCast, duraCast, duraCast, tape);
        g.shapeless(generic(EnumModGeneric.DURA_DURA), 1, gunmetalPlate, duraCast, tape);
        g.shapeless(generic(EnumModGeneric.DESH_DAMAGE), 1, gmMech, deshCast, deshCast, deshCast, tape);
        g.shapeless(generic(EnumModGeneric.DESH_DURA), 1, gunmetalPlate, deshCast, tape);
        g.shapeless(generic(EnumModGeneric.WSTEEL_DAMAGE), 1, wsMech, wsteelCast, wsteelCast, wsteelCast, tape);
        g.shapeless(generic(EnumModGeneric.WSTEEL_DURA), 1, wsteelPlate, wsteelCast, tape);
        g.shapeless(generic(EnumModGeneric.FERRO_DAMAGE), 1, wsMech, ferroCast, ferroCast, ferroCast, tape);
        g.shapeless(generic(EnumModGeneric.FERRO_DURA), 1, wsteelPlate, ferroCast, tape);
        g.shapeless(generic(EnumModGeneric.TCALLOY_DAMAGE), 1, wsMech, anyResistantCast, anyResistantCast, anyResistantCast, tape);
        g.shapeless(generic(EnumModGeneric.TCALLOY_DURA), 1, wsteelPlate, anyResistantCast, tape);
        g.shapeless(generic(EnumModGeneric.BIGMT_DAMAGE), 1, bmMech, bigmtCast, bigmtCast, bigmtCast, tape);
        g.shapeless(generic(EnumModGeneric.BIGMT_DURA), 1, bigmtPlate, bigmtCast, tape);
        g.shapeless(generic(EnumModGeneric.BRONZE_DAMAGE), 1, bmMech, anyBronzeCast, anyBronzeCast, anyBronzeCast, tape);
        g.shapeless(generic(EnumModGeneric.BRONZE_DURA), 1, bigmtPlate, anyBronzeCast, tape);
        g.shaped(special(EnumModSpecial.SILENCER), 1, p("P", "B", "P"), 'P', anyPlastic, 'B', part("part_barrel_light_steel"));
        g.shaped(special(EnumModSpecial.SCOPE), 1, p("SPS", "G G", "SPS"), 'P', anyPlastic, 'S', steelPlate, 'G', keyPane);
        g.shaped(special(EnumModSpecial.SAW), 1, p("BBS", "BHS"), 'B', ModItems.BOLT_STEEL, 'S', keyStick, 'H', duraPlate);
        g.shaped(special(EnumModSpecial.SPEEDLOADER), 1, p(" B ", "BSB", " B "), 'B', ModItems.BOLT_STEEL, 'S', wsteelPlate);
        g.shaped(special(EnumModSpecial.SLOWDOWN), 1, p(" I ", " M ", "I I"), 'I', wsteelIngot, 'M', wsMech);
        g.shaped(special(EnumModSpecial.SPEEDUP), 1, p("PIP", "WWW", "PIP"), 'P', wsteelPlate, 'I', gunmetalIngot, 'W', goldWireDense);
        g.shaped(special(EnumModSpecial.GREASEGUN), 1, p("BRM", "P G"), 'B', part("part_barrel_light_weaponsteel"), 'R', part("part_receiver_light_weaponsteel"), 'M', wsMech, 'P', duraPlate, 'G', plasticGrip);
        g.shaped(special(EnumModSpecial.CHOKE), 1, p("P", "B", "P"), 'P', wsteelPlate, 'B', part("part_barrel_light_dura_steel"));
        g.shaped(special(EnumModSpecial.FURNITURE_GREEN), 1, p("PDS", "  G"), 'P', anyPlastic, 'D', keyGreen, 'S', plasticStock, 'G', plasticGrip);
        g.shaped(special(EnumModSpecial.FURNITURE_BLACK), 1, p("PDS", "  G"), 'P', anyPlastic, 'D', keyBlack, 'S', plasticStock, 'G', plasticGrip);
        g.shaped(special(EnumModSpecial.SKIN_SATURNITE), 1, p("BRM", " P "), 'B', part("part_barrel_light_saturnite"), 'R', part("part_receiver_light_saturnite"), 'M', bmMech, 'P', bigmtPlate);
        g.shaped(special(EnumModSpecial.STACK_MAG), 1, p("P P", "P P", "PMP"), 'P', wsteelPlate, 'M', bmMech);
        g.shaped(special(EnumModSpecial.BAYONET), 1, p("  P", "BBB"), 'P', steelPlate, 'B', ModItems.BOLT_STEEL);
        g.shaped(special(EnumModSpecial.LAS_SHOTGUN), 1, p("PPP", "RCR", "PPP"), 'P', anyHardPlastic, 'R', crystalRedstone, 'C', advanced);
        g.shaped(special(EnumModSpecial.LAS_CAPACITOR), 1, p("CCC", "PIP"), 'C', ModItems.CAPACITOR_TANTALUM, 'P', anyHardPlastic, 'I', ModItems.BISMOID_CHIP);
        g.shaped(special(EnumModSpecial.LAS_AUTO), 1, p(" C ", "RFR", " C "), 'C', ModItems.BISMOID_CHIP, 'R', crystalRedstone, 'F', bronzeHeavyReceiver);
        g.shaped(special(EnumModSpecial.DRILL_HSS), 1, p(" IP", "IIM", " IP"), 'I', duraIngot, 'P', anyPlastic, 'M', gmMech);
        g.shaped(special(EnumModSpecial.DRILL_WEAPONSTEEL), 1, p(" IP", "IIM", " IP"), 'I', wsteelIngot, 'P', rubberIngot, 'M', gmMech);
        g.shaped(special(EnumModSpecial.DRILL_TCALLOY), 1, p(" IP", "IIM", " IP"), 'I', anyResistantIngot, 'P', rubberIngot, 'M', wsMech);
        g.shaped(special(EnumModSpecial.DRILL_SATURNITE), 1, p(" IP", "IIM", " IP"), 'I', bigmtIngot, 'P', anyHardPlastic, 'M', wsMech);
        g.shaped(special(EnumModSpecial.ENGINE_DIESEL), 1, p("DSD", "PPP", "DSD"), 'D', duraPlate, 'P', ModItems.PISTON_SELENIUM, 'S', ModItems.PIPE_STEEL);
        g.shaped(special(EnumModSpecial.ENGINE_AVIATION), 1, p("DSD", "PPP", "DSD"), 'D', duraCast, 'P', ModItems.PISTON_SELENIUM, 'S', gmMech);
        g.shaped(special(EnumModSpecial.ENGINE_ELECTRIC), 1, p("DSD", "PPP", "DSD"), 'D', anyPlastic, 'P', goldWireDense, 'S', ModItems.BATTERY_PACK_CAPACITOR_GOLD);
        g.shaped(special(EnumModSpecial.ENGINE_TURBO), 1, p("DSD", "PPP", "DSD"), 'D', anyBronzeCast, 'P', ModItems.PISTON_SELENIUM, 'S', wsMech);
        g.shaped(special(EnumModSpecial.MAGNET), 1, p("RGR", "GBG", "RGR"), 'R', rubberIngot, 'G', goldWireDense, 'B', m(ModMaterials.NIOBIUM, MaterialShape.BLOCK));
        g.shaped(special(EnumModSpecial.SIFTER), 1, p("IGI", "IGI"), 'I', duraIngot, 'G', ModBlocks.STEEL_GRATE.get());
        g.shaped(special(EnumModSpecial.CANISTERS), 1, p(" R ", "CCC", "SSS"), 'R', part("pipe_rubber"), 'C', ModItems.CANISTER_EMPTY, 'S', steelPlate);

        //secrets!
        g.shapeless(WeaponItems.ammo(EnumAmmoSecret.M44_EQUESTRIAN), 6, ammo(EnumAmmo.M44_JHP), ModItems.ITEM_SECRET_SELENIUM_STEEL);
        g.shapeless(WeaponItems.ammo(EnumAmmoSecret.G12_EQUESTRIAN), 6, ammo(EnumAmmo.G12), ModItems.ITEM_SECRET_SELENIUM_STEEL);
        g.shapeless(WeaponItems.ammo(EnumAmmoSecret.BMG50_EQUESTRIAN), 6, ammo(EnumAmmo.BMG50_FMJ), ModItems.ITEM_SECRET_SELENIUM_STEEL);

        //Missiles (nur die SEDNA-abhaengige Mikro-Rakete)
        g.shapeless(ModItems.MISSILE_MICRO, 1, ModItems.MISSILE_ASSEMBLY, ModItems.DUCTTAPE, ammo(EnumAmmo.NUKE_HIGH));

        // 1:1 WeaponRecipes "Missile fins/thrusters/fuselages/warheads/chips" (Baukastenraketen-Teile)
        java.util.function.Function<String, net.minecraft.world.item.Item> mp = id -> com.hbm_m.item.missile.MissilePartItems.get(id).get();
        var scaffold = ModBlocks.STEEL_SCAFFOLD.get();

        g.shaped(mp.apply("mp_stability_10_flat"), 1, p("PSP", "P P"), 'P', steelPlate, 'S', scaffold);
        g.shaped(mp.apply("mp_stability_10_cruise"), 1, p("ASA", " S ", "PSP"), 'A', m(ModMaterials.TITANIUM, MaterialShape.PLATE), 'P', steelPlate, 'S', scaffold);
        g.shaped(mp.apply("mp_stability_10_space"), 1, p("ASA", "PSP"), 'A', m(ModMaterials.ALUMINUM, MaterialShape.PLATE), 'P', m(ModMaterials.STEEL, MaterialShape.INGOT), 'S', scaffold);
        g.shaped(mp.apply("mp_stability_15_flat"), 1, p("ASA", "PSP"), 'A', m(ModMaterials.ALUMINUM, MaterialShape.PLATE), 'P', steelPlate, 'S', scaffold);
        g.shaped(mp.apply("mp_stability_15_thin"), 1, p("A A", "PSP", "PSP"), 'A', m(ModMaterials.ALUMINUM, MaterialShape.PLATE), 'P', steelPlate, 'S', scaffold);

        g.shaped(mp.apply("mp_thruster_15_balefire_large_rad"), 1, p("CCC", "CTC", "CCC"), 'C', m(ModMaterials.COPPER, MaterialShape.PLATE_CAST), 'T', mp.apply("mp_thruster_15_balefire_large"));

        for (String f : new String[] { "10_kerosene", "10_long_kerosene", "15_kerosene", "10_solid", "10_long_solid", "15_solid" }) {
            g.shaped(mp.apply("mp_fuselage_" + f + "_insulation"), 1, p("CCC", "CTC", "CCC"), 'C', anyRubber, 'T', mp.apply("mp_fuselage_" + f));
        }
        g.shaped(mp.apply("mp_fuselage_15_solid_desh"), 1, p("CCC", "CTC", "CCC"), 'C', m(ModMaterials.DESH, MaterialShape.INGOT), 'T', mp.apply("mp_fuselage_15_solid"));
        for (String f : new String[] { "10_kerosene", "10_long_kerosene", "15_kerosene" }) {
            g.shaped(mp.apply("mp_fuselage_" + f + "_metal"), 1, p("ICI", "CTC", "ICI"), 'C', steelPlate, 'I', m(ModMaterials.IRON, MaterialShape.PLATE), 'T', mp.apply("mp_fuselage_" + f));
        }

        g.shaped(mp.apply("mp_warhead_15_boxcar"), 1, p("SNS", "CBC", "SFS"), 'S', star, 'N', ModBlocks.DET_NUKE.get(),
                'C', advanced, 'B', ModBlocks.BOXCAR.get(),
                'F', com.hbm_m.recipe.FluidContainerIngredient.of(com.hbm_m.inventory.fluid.ModFluids.TRITIUM.getSource(), 16_000));

        Object[] chips = { ModItems.VACUUM_TUBE, ModItems.ANALOG_CIRCUIT, ModItems.INTEGRATED_CIRCUIT, ModItems.ADVANCED_CIRCUIT, ModItems.BISMOID_CIRCUIT };
        for (int i = 0; i < chips.length; i++) {
            g.shaped(mp.apply("mp_c_" + (i + 1)), 1, p("P", "C", "S"), 'P', anyRubber, 'C', chips[i], 'S', scaffold);
        }

        //Guns (Altsystem, braucht gun_lasrifle) und Feuerloescher
        g.shaped(ModItems.GUN_B92, 1, p("DDD", "SSC", "  R"), 'D', m(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE), 'S', star, 'C', bismoid, 'R', gun("gun_lasrifle"));
        g.shaped(gun("gun_fireext"), 1, p("HB", " T"), 'H', ModItems.PIPE_STEEL, 'B', ModItems.BOLT_STEEL, 'T', ModItems.TANK_STEEL);

        //Mines
        g.shaped(ModBlocks.MINE_AP.get(), 4, p("I", "C", "S"), 'I', m(ModMaterials.POLYMER, MaterialShape.PLATE), 'C', anySmokeless, 'S', steelIngot);
        g.shaped(ModBlocks.MINE_SHRAP.get(), 1, p("L", "M"), 'M', ModBlocks.MINE_AP.get(), 'L', ModItems.PELLET_BUCKSHOT);
        g.shaped(ModBlocks.MINE_HE.get(), 1, p(" C ", "PTP"), 'C', basic, 'P', steelPlate, 'T', anyHighExplosive);
        g.shaped(ModBlocks.MINE_FAT.get(), 1, p("CDN"), 'C', ModItems.ANALOG_CIRCUIT, 'D', ModItems.DUCTTAPE, 'N', ammo(EnumAmmo.NUKE_DEMO));

        // ConsumableRecipes: bomb_caller Meta 4 (Atombombe)
        g.shaped(ModItems.BOMB_CALLER_ATOMIC, 1, p("TRC"), 'T', ammo(EnumAmmo.NUKE_HIGH), 'R', ModItems.RANGEFINDER, 'C', ModItems.CONTROLLER);

        // Restport (WeaponRecipes 215/216): B92-Zelle und scharfe Starblaster-Zelle (volle Zelle per NBT, Peroxid-Tank)
        g.shaped(ModItems.GUN_B92_AMMO, 1, p("PSP", "ESE", "PSP"), 'P', steelPlate, 'S', star, 'E', ModItems.POWDER_SPARK_MIX);
        g.shapeless(ModItems.WEAPONIZED_STARBLASTER_CELL, 1,
                net.minecraftforge.common.crafting.StrictNBTIngredient.of(com.hbm_m.item.liquids.ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), com.hbm_m.inventory.fluid.ModFluids.PEROXIDE.getSource(), 1)),
                net.minecraftforge.common.crafting.StrictNBTIngredient.of(com.hbm_m.item.weapon.GunB92CellItem.getFullCell()),
                m(ModMaterials.COPPER, MaterialShape.WIRE));

        // AssemblyMachineRecipes "ammo" (ass.50bmgsm / ass.50bmgbypass): jetzt in AssemblyMachineRecipeGenerator
    }
}
