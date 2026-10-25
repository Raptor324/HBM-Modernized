package com.hbm_m.client.weapon;

import static com.hbm_m.item.weapon.sedna.factory.GunFactory.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory10ga.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory12ga.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory22lr.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory357.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory35800.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory40mm.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory44.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory45.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory50.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory556mm.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory75Bolt.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory762mm.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactory9mm.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryAccelerator.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryBlackPowder.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryCatapult.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryEnergy.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryFolly.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryTool.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryTurret.*;
import static com.hbm_m.item.weapon.sedna.factory.XFactoryRocket.*;

import java.util.function.BiConsumer;

import com.hbm_m.client.weapon.render.*;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;

/**
 * 1:1 {@code GunFactoryClient}: Item-Renderer der Waffen, Projektil-Renderer der BulletConfigs und HUD-Komponenten.
 * Laeuft clientseitig nach {@code GunFactory.init()} (FMLClientSetupEvent, enqueueWork).
 */
public class GunFactoryClient {

    public static void init() {
        //GUNS
        GunItemRenderer.register(WeaponItems.gun("gun_debug"), new ItemRenderDebug());
        GunItemRenderer.register(WeaponItems.gun("gun_pepperbox"), new ItemRenderPepperbox());
        GunItemRenderer.register(WeaponItems.gun("gun_light_revolver"), new ItemRenderAtlas(WeaponResources.bio_revolver_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_light_revolver_atlas"), new ItemRenderAtlas(WeaponResources.bio_revolver_atlas_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_light_revolver_dani"), new ItemRenderDANI());
        GunItemRenderer.register(WeaponItems.gun("gun_henry"), new ItemRenderHenry(WeaponResources.henry_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_henry_lincoln"), new ItemRenderHenry(WeaponResources.henry_lincoln_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_greasegun"), new ItemRenderGreasegun());
        GunItemRenderer.register(WeaponItems.gun("gun_maresleg"), new ItemRenderMaresleg(WeaponResources.maresleg_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_maresleg_akimbo"), new ItemRenderMareslegAkimbo());
        GunItemRenderer.register(WeaponItems.gun("gun_maresleg_broken"), new ItemRenderMaresleg(WeaponResources.maresleg_broken_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_flaregun"), new ItemRenderFlaregun());
        GunItemRenderer.register(WeaponItems.gun("gun_heavy_revolver"), new ItemRenderHeavyRevolver(WeaponResources.heavy_revolver_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_heavy_revolver_lilmac"), new ItemRenderHeavyRevolver(WeaponResources.lilmac_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_heavy_revolver_protege"), new ItemRenderHeavyRevolver(WeaponResources.heavy_revolver_protege_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_carbine"), new ItemRenderCarbine());
        GunItemRenderer.register(WeaponItems.gun("gun_am180"), new ItemRenderAm180());
        GunItemRenderer.register(WeaponItems.gun("gun_liberator"), new ItemRenderLiberator());
        GunItemRenderer.register(WeaponItems.gun("gun_congolake"), new ItemRenderCongoLake());
        GunItemRenderer.register(WeaponItems.gun("gun_flamer"), new ItemRenderFlamer(WeaponResources.flamethrower_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_flamer_topaz"), new ItemRenderFlamer(WeaponResources.flamethrower_topaz_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_flamer_daybreaker"), new ItemRenderFlamer(WeaponResources.flamethrower_daybreaker_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_lag"), new ItemRenderLAG());
        GunItemRenderer.register(WeaponItems.gun("gun_uzi"), new ItemRenderUzi());
        GunItemRenderer.register(WeaponItems.gun("gun_uzi_akimbo"), new ItemRenderUziAkimbo());
        GunItemRenderer.register(WeaponItems.gun("gun_spas12"), new ItemRenderSPAS12());
        GunItemRenderer.register(WeaponItems.gun("gun_panzerschreck"), new ItemRenderPanzerschreck());
        GunItemRenderer.register(WeaponItems.gun("gun_star_f"), new ItemRenderStarF());
        GunItemRenderer.register(WeaponItems.gun("gun_star_f_akimbo"), new ItemRenderStarFAkimbo());
        GunItemRenderer.register(WeaponItems.gun("gun_g3"), new ItemRenderG3(WeaponResources.g3_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_g3_zebra"), new ItemRenderG3(WeaponResources.g3_zebra_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_stinger"), new ItemRenderStinger());
        GunItemRenderer.register(WeaponItems.gun("gun_mk108"), new ItemRenderMK108());
        GunItemRenderer.register(WeaponItems.gun("gun_chemthrower"), new ItemRenderChemthrower());
        GunItemRenderer.register(WeaponItems.gun("gun_amat"), new ItemRenderAmat(WeaponResources.amat_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_amat_subtlety"), new ItemRenderAmat(WeaponResources.amat_subtlety_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_amat_penance"), new ItemRenderAmat(WeaponResources.amat_penance_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_m2"), new ItemRenderM2());
        GunItemRenderer.register(WeaponItems.gun("gun_autoshotgun"), new ItemRenderShredder(WeaponResources.shredder_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_autoshotgun_shredder"), new ItemRenderShredder(WeaponResources.shredder_orig_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_autoshotgun_sexy"), new ItemRenderSexy(WeaponResources.sexy_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_autoshotgun_heretic"), new ItemRenderSexy(WeaponResources.heretic_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_quadro"), new ItemRenderQuadro());
        GunItemRenderer.register(WeaponItems.gun("gun_minigun"), new ItemRenderMinigun(WeaponResources.minigun_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_minigun_lacunae"), new ItemRenderMinigun(WeaponResources.minigun_lacunae_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_minigun_dual"), new ItemRenderMinigunDual());
        GunItemRenderer.register(WeaponItems.gun("gun_missile_launcher"), new ItemRenderMissileLauncher());
        GunItemRenderer.register(WeaponItems.gun("gun_tesla_cannon"), new ItemRenderTeslaCannon());
        GunItemRenderer.register(WeaponItems.gun("gun_laser_pistol"), new ItemRenderLaserPistol(WeaponResources.laser_pistol_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_laser_pistol_pew_pew"), new ItemRenderLaserPistol(WeaponResources.laser_pistol_pew_pew_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_laser_pistol_morning_glory"), new ItemRenderLaserPistol(WeaponResources.laser_pistol_morning_glory_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_stg77"), new ItemRenderSTG77());
        GunItemRenderer.register(WeaponItems.gun("gun_tau"), new ItemRenderTau());
        GunItemRenderer.register(WeaponItems.gun("gun_fatman"), new ItemRenderFatMan());
        GunItemRenderer.register(WeaponItems.gun("gun_lasrifle"), new ItemRenderLasrifle());
        GunItemRenderer.register(WeaponItems.gun("gun_coilgun"), new ItemRenderCoilgun());
        GunItemRenderer.register(WeaponItems.gun("gun_hangman"), new ItemRenderHangman());
        GunItemRenderer.register(WeaponItems.gun("gun_mas36"), new ItemRenderMAS36());
        GunItemRenderer.register(WeaponItems.gun("gun_bolter"), new ItemRenderBolter());
        GunItemRenderer.register(WeaponItems.gun("gun_folly"), new ItemRenderFolly());
        GunItemRenderer.register(WeaponItems.gun("gun_aberrator"), new ItemRenderAberrator());
        GunItemRenderer.register(WeaponItems.gun("gun_aberrator_eott"), new ItemRenderEOTT());
        GunItemRenderer.register(WeaponItems.gun("gun_double_barrel"), new ItemRenderDoubleBarrel(WeaponResources.double_barrel_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_double_barrel_sacred_dragon"), new ItemRenderDoubleBarrel(WeaponResources.double_barrel_sacred_dragon_tex));
        GunItemRenderer.register(WeaponItems.gun("gun_charge_thrower"), new ItemRenderChargeThrower());
        GunItemRenderer.register(WeaponItems.gun("gun_drill"), new ItemRenderDrill());
        GunItemRenderer.register(WeaponItems.gun("gun_n_i_4_n_i"), new ItemRenderNI4NI());
        GunItemRenderer.register(WeaponItems.gun("gun_pa_melee"), new ItemRenderPAMelee());
        
        // Original ClientProxy: Feuerloescher und Baukastengranate (eigene IItemRenderer ausserhalb von GunFactoryClient)
        GunItemRenderer.register(WeaponItems.gun("gun_fireext"), new ItemRenderFireExt());
        GunItemRenderer.register(com.hbm_m.item.weapon.grenade.GrenadeItems.GRENADE_UNIVERSAL.get(), new ItemRenderGrenade());
        GunItemRenderer.register(com.hbm_m.item.ModItems.CRUCIBLE_SWORD.get(), new com.hbm_m.client.weapon.render.ItemRenderCrucible());
        // Original ClientProxy: ItemRenderChainsaw, ItemRenderBoltgun
        GunItemRenderer.register(com.hbm_m.item.ModItems.CHAINSAW.get(), new com.hbm_m.client.weapon.render.ItemRenderChainsaw());
        GunItemRenderer.register(com.hbm_m.item.ModItems.BOLTGUN.get(), new com.hbm_m.client.weapon.render.ItemRenderBoltgun());
        // Original ClientProxy: ItemRenderShim, ItemRenderGavel, ItemRenderRedstoneSword (Inventar = flaches Symbol)
        com.hbm_m.client.weapon.render.ItemRenderShim shim = new com.hbm_m.client.weapon.render.ItemRenderShim();
        GunItemRenderer.register(com.hbm_m.item.ModItems.SHIMMER_SLEDGE.get(), shim);
        GunItemRenderer.register(com.hbm_m.item.ModItems.SHIMMER_AXE.get(), shim);
        GunItemRenderer.register(com.hbm_m.item.ModItems.STOPSIGN.get(), shim);
        GunItemRenderer.register(com.hbm_m.item.ModItems.SOPSIGN.get(), shim);
        GunItemRenderer.register(com.hbm_m.item.ModItems.CHERNOBYLSIGN.get(), shim);
        com.hbm_m.client.weapon.render.ItemRenderGavel gavel = new com.hbm_m.client.weapon.render.ItemRenderGavel();
        GunItemRenderer.register(com.hbm_m.item.ModItems.WOOD_GAVEL.get(), gavel);
        GunItemRenderer.register(com.hbm_m.item.ModItems.LEAD_GAVEL.get(), gavel);
        GunItemRenderer.register(com.hbm_m.item.ModItems.DIAMOND_GAVEL.get(), gavel);
        GunItemRenderer.register(com.hbm_m.item.ModItems.MESE_GAVEL.get(), gavel);
        GunItemRenderer.register(com.hbm_m.item.ModItems.REDSTONE_SWORD.get(), new com.hbm_m.client.weapon.render.ItemRenderRedstoneSword());

        //PROJECTILES
        ammo_debug.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        ammo_debug_shot.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        
        stone.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        flint.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        iron.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        shot.setRenderer(LegoClient.RENDER_STANDARD_BULLET);

        m357_bp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m357_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m357_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m357_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m357_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        m357_express.setRenderer(LegoClient.RENDER_EXPRESS_BULLET);

        m44_bp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m44_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m44_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m44_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        m44_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        m44_express.setRenderer(LegoClient.RENDER_EXPRESS_BULLET);
        m44_equestrian_pip.setRenderer(LegoClient.RENDER_LEGENDARY_BULLET);
        m44_equestrian_mn7.setRenderer(LegoClient.RENDER_LEGENDARY_BULLET);

        p22_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p22_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p22_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p22_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        
        p9_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p9_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p9_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p9_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        
        p45_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p45_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p45_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        p45_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        p45_du.setRenderer(LegoClient.RENDER_DU_BULLET);

        r556_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        r556_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        r556_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        r556_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        
        setRendererBulk(LegoClient.RENDER_AP_BULLET, r556_inc_sp, r556_inc_fmj, r556_inc_jhp, r556_inc_ap);
        
        r762_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        r762_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        r762_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        r762_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        r762_du.setRenderer(LegoClient.RENDER_DU_BULLET);
        r762_he.setRenderer(LegoClient.RENDER_HE_BULLET);
        
        bmg50_sp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        bmg50_fmj.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        bmg50_jhp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        bmg50_ap.setRenderer(LegoClient.RENDER_AP_BULLET);
        bmg50_du.setRenderer(LegoClient.RENDER_DU_BULLET);
        bmg50_he.setRenderer(LegoClient.RENDER_HE_BULLET);
        bmg50_sm.setRenderer(LegoClient.RENDER_SM_BULLET);
        bmg50_black.setRenderer(LegoClient.RENDER_BLACK_BULLET);

        b75.setRenderer(LegoClient.RENDER_AP_BULLET);
        b75_inc.setRenderer(LegoClient.RENDER_AP_BULLET);
        b75_exp.setRenderer(LegoClient.RENDER_EXPRESS_BULLET);
        
        g12_bp.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_bp_magnum.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_bp_slug.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_slug.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_flechette.setRenderer(LegoClient.RENDER_FLECHETTE_BULLET);
        g12_magnum.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_explosive.setRenderer(LegoClient.RENDER_EXPRESS_BULLET);
        g12_phosphorus.setRenderer(LegoClient.RENDER_AP_BULLET);
        g12_equestrian_bj.setRenderer(LegoClient.RENDER_LEGENDARY_BULLET);
        g12_equestrian_tkr.setRenderer(LegoClient.RENDER_LEGENDARY_BULLET);

        g12_sub.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_sub_slug.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_sub_flechette.setRenderer(LegoClient.RENDER_FLECHETTE_BULLET);
        g12_sub_magnum.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g12_sub_explosive.setRenderer(LegoClient.RENDER_EXPRESS_BULLET);
        g12_sub_phosphorus.setRenderer(LegoClient.RENDER_AP_BULLET);
        
        setRendererBulkBeam(LegoClient.RENDER_LASER_CYAN, g12_shredder, g12_shredder_slug, g12_shredder_flechette, g12_shredder_magnum, g12_shredder_explosive, g12_shredder_phosphorus);

        g10.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g10_shrapnel.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g10_du.setRenderer(LegoClient.RENDER_DU_BULLET);
        g10_slug.setRenderer(LegoClient.RENDER_STANDARD_BULLET);
        g10_explosive.setRenderer(LegoClient.RENDER_HE_BULLET);

        g26_flare.setRenderer(LegoClient.RENDER_FLARE);
        g26_flare_supply.setRenderer(LegoClient.RENDER_FLARE_SUPPLY);
        g26_flare_weapon.setRenderer(LegoClient.RENDER_FLARE_WEAPON);
        
        setRendererBulk(LegoClient.RENDER_GRENADE, g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus);
        
        setRendererBulk(LegoClient.RENDER_RPZB, rocket_rpzb);
        setRendererBulk(LegoClient.RENDER_QD, rocket_qd);
        setRendererBulk(LegoClient.RENDER_ML, rocket_ml);
        setRendererBulk(LegoClient.RENDER_RPZB, rocket_ncrpa_steer);
        setRendererBulk(LegoClient.RENDER_RPZB, rocket_ncrpa);
        
        setRendererBulk(LegoClient.RENDER_NUKE, nuke_standard, nuke_demo, nuke_high);
        nuke_tots.setRenderer(LegoClient.RENDER_GRENADE);
        nuke_hive.setRenderer(LegoClient.RENDER_HIVE);
        nuke_balefire.setRenderer(LegoClient.RENDER_NUKE_BALEFIRE);
        cluster_submunition.setRenderer(LegoClient.RENDER_BOMB);

        setRendererBulkBeam(LegoClient.RENDER_LIGHTNING, energy_tesla, energy_tesla_overcharge, energy_tesla_ir);
        setRendererBulkBeam(LegoClient.RENDER_LIGHTNING_SUB, energy_tesla_ir_sub, com.hbm_m.blockentity.machines.BatterySocketBlockEntity.discharge);
        setRendererBulkBeam(LegoClient.RENDER_TAU, tau_uranium);
        setRendererBulkBeam(LegoClient.RENDER_TAU_CHARGE, tau_uranium_charge);
        setRendererBulkBeam(LegoClient.RENDER_LASER_RED, energy_las, energy_las_overcharge, energy_las_ir);
        setRendererBulkBeam(LegoClient.RENDER_LASER_PURPLE, energy_lacunae, energy_lacunae_overcharge, energy_lacunae_ir);
        setRendererBulkBeam(LegoClient.RENDER_LASER_EMERALD, energy_emerald, energy_emerald_overcharge, energy_emerald_ir);
        
        setRendererBulk(LegoClient.RENDER_AP_BULLET, coil_tungsten, coil_ferrouranium);
        
        folly_sm.setRendererBeam(LegoClient.RENDER_FOLLY);
        folly_nuke.setRenderer(LegoClient.RENDER_BIG_NUKE);

        p35800.setRendererBeam(LegoClient.RENDER_CRACKLE);
        p35800_bl.setRendererBeam(LegoClient.RENDER_BLACK_LIGHTNING);
        
        ni4ni_arc.setRendererBeam(LegoClient.RENDER_NI4NI_BOLT);

        ct_hook.setRenderer(LegoClient.RENDER_CT_HOOK);
        ct_mortar.setRenderer(LegoClient.RENDER_CT_MORTAR);
        ct_mortar_charge.setRenderer(LegoClient.RENDER_CT_MORTAR_CHARGE);
        
        setRendererBulk(LegoClient.RENDER_GRENADE, shell_normal, shell_explosive, shell_ap, shell_du, shell_w9); //TODO: change the sabots

        setRendererBulk(LegoClient.RENDER_FRAGMENTATION, com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.fragmentation, com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.pellets, com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.pellets_heavy);
        com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.laser.setRendererBeam(LegoClient.RENDER_LASER_RED);
        
        com.hbm_m.blockentity.machines.pile.PileCoreBlockEntity.pile_debris.setRenderer(LegoClient.RENDER_GRAPHITE);
        
        //HUDS
        ((ItemGunBaseNT) WeaponItems.gun("gun_debug")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO, LegoClient.HUD_COMPONENT_AMMO_SECOND);
        
        ((ItemGunBaseNT) WeaponItems.gun("gun_pepperbox")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_light_revolver")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_light_revolver_atlas")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_henry")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_henry_lincoln")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_greasegun")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_maresleg")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_maresleg_broken")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_flaregun")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_heavy_revolver")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_heavy_revolver_lilmac")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_heavy_revolver_protege")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_carbine")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_am180")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_liberator")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_congolake")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_flamer")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        ((ItemGunBaseNT) WeaponItems.gun("gun_flamer_topaz")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        ((ItemGunBaseNT) WeaponItems.gun("gun_flamer_daybreaker")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        ((ItemGunBaseNT) WeaponItems.gun("gun_uzi")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_spas12")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_panzerschreck")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_star_f")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_g3")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_g3_zebra")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_stinger")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_mk108")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_chemthrower")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_amat")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_amat_subtlety")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_amat_penance")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_m2")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_autoshotgun")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_autoshotgun_shredder")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_autoshotgun_sexy")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_autoshotgun_heretic")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_quadro")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_lag")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_minigun")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_minigun_lacunae")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_missile_launcher")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_tesla_cannon")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_laser_pistol")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_laser_pistol_pew_pew")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_laser_pistol_morning_glory")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_stg77")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_tau")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_fatman")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_lasrifle")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_coilgun")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_hangman")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_mas36")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_bolter")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_folly")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_aberrator")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_double_barrel")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_double_barrel_sacred_dragon")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_fireext")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_charge_thrower")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        
        ((ItemGunBaseNT) WeaponItems.gun("gun_light_revolver_dani")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY_MIRROR, LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ((ItemGunBaseNT) WeaponItems.gun("gun_light_revolver_dani")).getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_maresleg_akimbo")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY_MIRROR, LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ((ItemGunBaseNT) WeaponItems.gun("gun_maresleg_akimbo")).getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_uzi_akimbo")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY_MIRROR, LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ((ItemGunBaseNT) WeaponItems.gun("gun_uzi_akimbo")).getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_star_f_akimbo")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY_MIRROR, LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ((ItemGunBaseNT) WeaponItems.gun("gun_star_f_akimbo")).getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_minigun_dual")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_DURABILITY_MIRROR, LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ((ItemGunBaseNT) WeaponItems.gun("gun_minigun_dual")).getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ((ItemGunBaseNT) WeaponItems.gun("gun_aberrator_eott")).getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ((ItemGunBaseNT) WeaponItems.gun("gun_aberrator_eott")).getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_AMMO);
    }
    
    public static void setRendererBulk(BiConsumer<EntityBulletBaseMK4, Float> renderer, BulletConfig... configs) { for(BulletConfig config : configs) config.setRenderer(renderer); }
    public static void setRendererBulkBeam(BiConsumer<EntityBulletBeamBase, Float> renderer, BulletConfig... configs) { for(BulletConfig config : configs) config.setRendererBeam(renderer); }
}
