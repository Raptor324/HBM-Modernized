package com.hbm_m.item.weapon.sedna.mods;

import static com.hbm_m.item.weapon.sedna.mods.XWeaponModManager.*;

import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModCaliber;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModTest;
import com.hbm_m.item.weapon.sedna.factory.XFactory22lr;
import com.hbm_m.item.weapon.sedna.factory.XFactory357;
import com.hbm_m.item.weapon.sedna.factory.XFactory44;
import com.hbm_m.item.weapon.sedna.factory.XFactory45;
import com.hbm_m.item.weapon.sedna.factory.XFactory50;
import com.hbm_m.item.weapon.sedna.factory.XFactory556mm;
import com.hbm_m.item.weapon.sedna.factory.XFactory762mm;
import com.hbm_m.item.weapon.sedna.factory.XFactory9mm;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager.WeaponModDefinition;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Inhalt von {@code XWeaponModManager.init()} (1.7.10): ordnet die Mod-Gegenstaende den Waffen zu. Wird erst nach der
 * Registrierung aller Waffen aufgerufen (gemeinsame Initialisierung). Reihenfolge der IDs nicht aendern!
 */
public final class XWeaponModInit {

    private XWeaponModInit() { }

    /** Original {@code ToolMaterial.EMERALD.ordinal()} (WOOD, STONE, IRON, EMERALD, GOLD) - Abbaustufe Diamant */
    private static final int EMERALD = 3;

    /** Original {@code ModItems.gun_x} */
    private static Item gun(String name) { return WeaponItems.gun(name); }

    /** Assigns the IWeaponMod instances to items */
    public static void init() {
        /* ORDER MATTERS! */
        /* CTOR contains registering to the idToMod, avoid reordering to prevent ID shifting! */
        /// TEST ///
        IWeaponMod TEST_FIRERATE = new WeaponModTestFirerate(0, "FIRERATE");
        IWeaponMod TEST_DAMAGE = new WeaponModTestDamage(1, "DAMAGE");
        IWeaponMod TEST_MULTI = new WeaponModTestMulti(2, "MULTI");
        
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.FIRERATE).get())).addDefault(TEST_FIRERATE);
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.DAMAGE).get())).addDefault(TEST_DAMAGE);
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.MULTI).get())).addDefault(TEST_MULTI);

        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_2_5).get())).addDefault(new WeaponModOverride(3, 2.5F, "OVERRIDE"));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_5).get())).addDefault(new WeaponModOverride(4, 5F, "OVERRIDE"));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_7_5).get())).addDefault(new WeaponModOverride(5, 7.5F, "OVERRIDE"));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_10).get())).addDefault(new WeaponModOverride(6, 10F, "OVERRIDE"));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_12_5).get())).addDefault(new WeaponModOverride(7, 12_5F, "OVERRIDE"));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_15).get())).addDefault(new WeaponModOverride(8, 15F, "OVERRIDE"));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_TEST.get(EnumModTest.OVERRIDE_20).get())).addDefault(new WeaponModOverride(9, 20F, "OVERRIDE"));

        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_GENERIC.get(EnumModGeneric.IRON_DAMAGE).get())).addMod(gun("gun_pepperbox"), new WeaponModGenericDamage(100));
        new WeaponModDefinition(new ItemStack(WeaponItems.WEAPON_MOD_GENERIC.get(EnumModGeneric.IRON_DURA).get())).addMod(gun("gun_pepperbox"), new WeaponModGenericDurability(101));

        Item[] steelGuns = new Item[] {
        		gun("gun_light_revolver"), gun("gun_light_revolver_atlas"),
        		gun("gun_henry"), gun("gun_henry_lincoln"),
        		gun("gun_greasegun"),
        		gun("gun_maresleg"), gun("gun_maresleg_akimbo"),
        		gun("gun_flaregun") };
        Item[] duraGuns = new Item[] {
        		gun("gun_am180"),
        		gun("gun_liberator"),
        		gun("gun_congolake"),
        		gun("gun_flamer"),
        		gun("gun_flamer_topaz") };
        Item[] deshGuns = new Item[] {
        		gun("gun_heavy_revolver"),
        		gun("gun_carbine"),
        		gun("gun_uzi"), gun("gun_uzi_akimbo"),
        		gun("gun_spas12"),
        		gun("gun_panzerschreck") };
        Item[] wsteelGuns = new Item[] {
        		gun("gun_star_f"), gun("gun_star_f_akimbo"),
        		gun("gun_g3"), gun("gun_g3_zebra"),
        		gun("gun_mk108"),
        		gun("gun_chemthrower") };
        Item[] ferroGuns = new Item[] {
        		gun("gun_amat"),
        		gun("gun_m2"),
        		gun("gun_autoshotgun"), gun("gun_autoshotgun_shredder"),
        		gun("gun_quadro") };
        Item[] tcalloyGuns = new Item[] {
        		gun("gun_lag"),
        		gun("gun_minigun"),
        		gun("gun_missile_launcher"),
        		gun("gun_tesla_cannon") };
        Item[] bigmtGuns = new Item[] {
        		gun("gun_laser_pistol"), gun("gun_laser_pistol_pew_pew"),
        		gun("gun_stg77"),
        		gun("gun_fatman"),
        		gun("gun_tau") };
        Item[] bronzeGuns = new Item[] {
        		gun("gun_lasrifle") };

        new WeaponModDefinition(EnumModGeneric.STEEL_DAMAGE).addMod(steelGuns, new WeaponModGenericDamage(102));
        new WeaponModDefinition(EnumModGeneric.STEEL_DURA).addMod(steelGuns, new WeaponModGenericDurability(103));
        new WeaponModDefinition(EnumModGeneric.DURA_DAMAGE).addMod(duraGuns, new WeaponModGenericDamage(104));
        new WeaponModDefinition(EnumModGeneric.DURA_DURA).addMod(duraGuns, new WeaponModGenericDurability(105));
        new WeaponModDefinition(EnumModGeneric.DESH_DAMAGE).addMod(deshGuns, new WeaponModGenericDamage(106));
        new WeaponModDefinition(EnumModGeneric.DESH_DURA).addMod(deshGuns, new WeaponModGenericDurability(107));
        new WeaponModDefinition(EnumModGeneric.WSTEEL_DAMAGE).addMod(wsteelGuns, new WeaponModGenericDamage(108));
        new WeaponModDefinition(EnumModGeneric.WSTEEL_DURA).addMod(wsteelGuns, new WeaponModGenericDurability(109));
        new WeaponModDefinition(EnumModGeneric.FERRO_DAMAGE).addMod(ferroGuns, new WeaponModGenericDamage(110));
        new WeaponModDefinition(EnumModGeneric.FERRO_DURA).addMod(ferroGuns, new WeaponModGenericDurability(111));
        new WeaponModDefinition(EnumModGeneric.TCALLOY_DAMAGE).addMod(tcalloyGuns, new WeaponModGenericDamage(112));
        new WeaponModDefinition(EnumModGeneric.TCALLOY_DURA).addMod(tcalloyGuns, new WeaponModGenericDurability(113));
        new WeaponModDefinition(EnumModGeneric.BIGMT_DAMAGE).addMod(bigmtGuns, new WeaponModGenericDamage(114));
        new WeaponModDefinition(EnumModGeneric.BIGMT_DURA).addMod(bigmtGuns, new WeaponModGenericDurability(115));
        new WeaponModDefinition(EnumModGeneric.BRONZE_DAMAGE).addMod(bronzeGuns, new WeaponModGenericDamage(116));
        new WeaponModDefinition(EnumModGeneric.BRONZE_DURA).addMod(bronzeGuns, new WeaponModGenericDurability(117));

        new WeaponModDefinition(EnumModSpecial.SPEEDLOADER).addMod(gun("gun_liberator"), new WeaponModLiberatorSpeedloader(200));
        new WeaponModDefinition(EnumModSpecial.SILENCER).addMod(new Item[] {gun("gun_am180"), gun("gun_uzi"), gun("gun_uzi_akimbo"), gun("gun_star_f"), gun("gun_star_f_akimbo"), gun("gun_g3"), gun("gun_amat")}, new WeaponModSilencer(ID_SILENCER));
        new WeaponModDefinition(EnumModSpecial.SCOPE).addMod(new Item[] {gun("gun_heavy_revolver"), gun("gun_carbine"), gun("gun_g3"), gun("gun_mas36"), gun("gun_charge_thrower")}, new WeaponModScope(ID_SCOPE));
        new WeaponModDefinition(EnumModSpecial.SAW)
        	.addMod(new Item[] {gun("gun_maresleg"), gun("gun_double_barrel")}, new WeaponModSawedOff(ID_SAWED_OFF))
        	.addMod(gun("gun_panzerschreck"), new WeaponModPanzerschreckSawedOff(ID_NO_SHIELD))
        	.addMod(new Item[] {gun("gun_g3"), gun("gun_g3_zebra")}, new WeapnModG3SawedOff(ID_NO_STOCK));
        new WeaponModDefinition(EnumModSpecial.GREASEGUN).addMod(gun("gun_greasegun"), new WeaponModGreasegun(ID_GREASEGUN_CLEAN));
        new WeaponModDefinition(EnumModSpecial.SLOWDOWN).addMod(new Item[] {gun("gun_minigun"), gun("gun_minigun_dual")}, new WeaponModSlowdown(207));
        new WeaponModDefinition(EnumModSpecial.SPEEDUP)
        	.addMod(new Item[] {gun("gun_minigun"), gun("gun_minigun_dual")}, new WeaponModMinigunSpeedup(ID_MINIGUN_SPEED))
        	.addMod(new Item[] {gun("gun_autoshotgun"), gun("gun_autoshotgun_shredder"), gun("gun_mk108")}, new WeaponModShredderSpeedup(209));
        new WeaponModDefinition(EnumModSpecial.CHOKE).addMod(new Item[] {gun("gun_pepperbox"), gun("gun_maresleg"), gun("gun_double_barrel"), gun("gun_liberator"), gun("gun_spas12"), gun("gun_autoshotgun_sexy"), gun("gun_autoshotgun_heretic")}, new WeaponModChoke(210));
        new WeaponModDefinition(EnumModSpecial.FURNITURE_GREEN).addMod(gun("gun_g3"), new WeaponModPolymerFurniture(ID_FURNITURE_GREEN));
        new WeaponModDefinition(EnumModSpecial.FURNITURE_BLACK).addMod(gun("gun_g3"), new WeaponModPolymerFurniture(ID_FURNITURE_BLACK));
        new WeaponModDefinition(EnumModSpecial.BAYONET)
        .addMod(gun("gun_mas36"), new WeaponModMASBayonet(ID_MAS_BAYONET))
        .addMod(gun("gun_carbine"), new WeaponModCarbineBayonet(ID_CARBINE_BAYONET));
        new WeaponModDefinition(EnumModSpecial.STACK_MAG).addMod(new Item[] {gun("gun_greasegun"), gun("gun_uzi"), gun("gun_uzi_akimbo"), gun("gun_aberrator"), gun("gun_aberrator_eott")}, new WeaponModStackMag(214));
        new WeaponModDefinition(EnumModSpecial.SKIN_SATURNITE).addMod(new Item[] {gun("gun_uzi"), gun("gun_uzi_akimbo")}, new WeaponModUziSaturnite(ID_UZI_SATURN));
        new WeaponModDefinition(EnumModSpecial.LAS_SHOTGUN).addMod(new Item[] {gun("gun_lasrifle")}, new WeaponModLasShotgun(ID_LAS_SHOTGUN));
        new WeaponModDefinition(EnumModSpecial.LAS_CAPACITOR).addMod(new Item[] {gun("gun_lasrifle")}, new WeaponModLasCapacitor(ID_LAS_CAPACITOR));
        new WeaponModDefinition(EnumModSpecial.LAS_AUTO).addMod(new Item[] {gun("gun_lasrifle")}, new WeaponModLasAuto(ID_LAS_AUTO));
        new WeaponModDefinition(EnumModSpecial.NICKEL).addMod(new Item[] {gun("gun_n_i_4_n_i")}, new WeaponModNickel(ID_NI4NI_NICKEL, "COIN1"));
        new WeaponModDefinition(EnumModSpecial.DOUBLOONS).addMod(new Item[] {gun("gun_n_i_4_n_i")}, new WeaponModNickel(ID_NI4NI_DOUBLOONS, "COIN2"));
        
        new WeaponModDefinition(EnumModSpecial.DRILL_HSS).addMod(new Item[] {gun("gun_drill")}, new WeaponModDrill(ID_DRILL_HSS).damage(1.25F).dt(3F).pierce(0.15F).harvest(EMERALD));
        new WeaponModDefinition(EnumModSpecial.DRILL_WEAPONSTEEL).addMod(new Item[] {gun("gun_drill")}, new WeaponModDrill(ID_DRILL_WSTEEL).damage(1.5F).dt(5F).pierce(0.2F).aoe(2).harvest(EMERALD));
        new WeaponModDefinition(EnumModSpecial.DRILL_TCALLOY).addMod(new Item[] {gun("gun_drill")}, new WeaponModDrill(ID_DRILL_TCALLOY).damage(2F).dt(7.5F).pierce(0.2F).reach(2).aoe(3).harvest(EMERALD + 1));
        new WeaponModDefinition(EnumModSpecial.DRILL_SATURNITE).addMod(new Item[] {gun("gun_drill")}, new WeaponModDrill(ID_DRILL_SATURN).damage(3F).dt(10F).pierce(0.25F).reach(2).aoe(3).harvest(EMERALD + 2));
        new WeaponModDefinition(EnumModSpecial.ENGINE_DIESEL).addMod(new Item[] {gun("gun_drill")}, new WeaponModEngine(ID_ENGINE_DIESEL).mag(WeaponModEngine.ENGINE_DIESEL).delay(15));
        new WeaponModDefinition(EnumModSpecial.ENGINE_AVIATION).addMod(new Item[] {gun("gun_drill")}, new WeaponModEngine(ID_ENGINE_AVIATION).mag(WeaponModEngine.ENGINE_AVIATION).delay(10));
        new WeaponModDefinition(EnumModSpecial.ENGINE_ELECTRIC).addMod(new Item[] {gun("gun_drill")}, new WeaponModEngine(ID_ENGINE_ELECTRIC).mag(WeaponModEngine.ENGINE_ELECTRIC).delay(15));
        new WeaponModDefinition(EnumModSpecial.ENGINE_TURBO).addMod(new Item[] {gun("gun_drill")}, new WeaponModEngine(ID_ENGINE_TURBO).mag(WeaponModEngine.ENGINE_TURBO).delay(5));
        new WeaponModDefinition(EnumModSpecial.MAGNET).addMod(new Item[] {gun("gun_drill")}, new WeaponModDrillFortune(230, "MAGNET", 2));
        new WeaponModDefinition(EnumModSpecial.SIFTER).addMod(new Item[] {gun("gun_drill")}, new WeaponModDrillFortune(231, "SIFTER", 1));
        new WeaponModDefinition(EnumModSpecial.CANISTERS).addMod(new Item[] {gun("gun_drill")}, new WeaponModCanisters(232));

        BulletConfig[] p9 = new BulletConfig[] {XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap};
        BulletConfig[] p45 = new BulletConfig[] {XFactory45.p45_sp, XFactory45.p45_fmj, XFactory45.p45_jhp, XFactory45.p45_ap, XFactory45.p45_du};
        BulletConfig[] p22 = new BulletConfig[] {XFactory22lr.p22_sp, XFactory22lr.p22_fmj, XFactory22lr.p22_jhp, XFactory22lr.p22_ap};
        BulletConfig[] m357 = new BulletConfig[] {XFactory357.m357_sp, XFactory357.m357_fmj, XFactory357.m357_jhp, XFactory357.m357_ap, XFactory357.m357_express};
        BulletConfig[] m44 = new BulletConfig[] {XFactory44.m44_sp, XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express};
        BulletConfig[] r556 = new BulletConfig[] {XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp, XFactory556mm.r556_ap};
        BulletConfig[] r762 = new BulletConfig[] {XFactory762mm.r762_sp, XFactory762mm.r762_fmj, XFactory762mm.r762_jhp, XFactory762mm.r762_ap, XFactory762mm.r762_du, XFactory762mm.r762_he};
        BulletConfig[] bmg50 = new BulletConfig[] {XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_he};
        new WeaponModDefinition(EnumModCaliber.P9)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(300, 28, 10F, p9))
        	.addMod(gun("gun_star_f"), new WeaponModCaliber(301, 12, 15F, p9))
        	.addMod(gun("gun_star_f_akimbo"), new WeaponModCaliber(302, 12, 15F, p9));
        new WeaponModDefinition(EnumModCaliber.P45)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(310, 28, 10F, p45))
        	.addMod(gun("gun_greasegun"), new WeaponModCaliber(311, 24, 3F, p45))
        	.addMod(gun("gun_uzi"), new WeaponModCaliber(312, 24, 3F, p45))
        	.addMod(gun("gun_uzi_akimbo"), new WeaponModCaliber(313, 24, 3F, p45))
        	.addMod(gun("gun_lag"), new WeaponModCaliber(314, 15, 25F, p45));
        new WeaponModDefinition(EnumModCaliber.P22)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(320, 28, 10F, p22))
        	.addMod(gun("gun_uzi"), new WeaponModCaliber(321, 40, 3F, p22))
        	.addMod(gun("gun_uzi_akimbo"), new WeaponModCaliber(322, 40, 3F, p22));
        new WeaponModDefinition(EnumModCaliber.M357)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(330, 20, 10F, m357))
        	.addMod(gun("gun_lag"), new WeaponModCaliber(331, 15, 25F, m357));
        new WeaponModDefinition(EnumModCaliber.M44)
        	.addMod(gun("gun_lag"), new WeaponModCaliber(340, 13, 25F, m44));
        new WeaponModDefinition(EnumModCaliber.R556)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(350, 10, 10F, r556))
        	.addMod(gun("gun_carbine"), new WeaponModCaliber(351, 20, 15F, r556))
        	.addMod(new Item[] {gun("gun_minigun"), gun("gun_minigun_dual")}, new WeaponModCaliber(352, 0, 6F, r556));
        new WeaponModDefinition(EnumModCaliber.R762)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(360, 8, 10F, r762))
        	.addMod(gun("gun_g3"), new WeaponModCaliber(361, 24, 5F, r762));
        new WeaponModDefinition(EnumModCaliber.BMG50)
        	.addMod(gun("gun_henry"), new WeaponModCaliber(370, 5, 10F, bmg50))
        	.addMod(new Item[] {gun("gun_minigun"), gun("gun_minigun_dual")}, new WeaponModCaliber(371, 0, 6F, bmg50));
    }
}
