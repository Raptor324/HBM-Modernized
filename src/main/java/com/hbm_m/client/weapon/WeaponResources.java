package com.hbm_m.client.weapon;

import java.util.HashMap;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.render.anim.AnimationLoader;
import com.hbm_m.render.anim.BusAnimation;

import net.minecraft.resources.ResourceLocation;

/**
 * Die vom Waffensystem genutzten Eintraege des Original-{@code ResourceManager} (gleiche Feldnamen): OBJ-Modelle als
 * {@link SimpleObjModel} (Teile per {@code GunGL.renderPart(model, "Name")}), Texturen, Blender-Animationen
 * ({@code ResourceManager.x_anim.get("Name")} -> {@code WeaponResources.x_anim.get().get("Name")}).
 * Generiert aus {@code com.hbm.main.ResourceManager}.
 */
public final class WeaponResources {

    private WeaponResources() { }

    /**
     * Ensures all static weapon model ResourceLocations are registered in WeaponRenderRegistry.
     */
    public static void init() {
        // Triggers class loading and static initializers
    }

    public static ResourceLocation weaponModel(String path) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        com.hbm_m.client.render.weapon.WeaponRenderRegistry.registerModelLocation(loc);
        return loc;
    }

    private static ResourceLocation model(String path) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        if (path.startsWith("models/weapons/")) {
            com.hbm_m.client.render.weapon.WeaponRenderRegistry.registerModelLocation(loc);
        }
        return loc;
    }

    private static SimpleObjModel entityModel(String path) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        if (path.startsWith("models/projectiles/") || path.startsWith("models/weapons/")) {
            com.hbm_m.client.render.weapon.WeaponRenderRegistry.registerModelLocation(loc);
        }
        return new SimpleObjModel(loc);
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    private static Supplier<HashMap<String, BusAnimation>> anim(String path) {
        return Suppliers.memoize(() -> {
            HashMap<String, BusAnimation> m = AnimationLoader.load(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path));
            return m != null ? m : new HashMap<>();
        });
    }

    // 67 Weapon OBJ model ResourceLocations (Nucleus Milestone M1)
    public static final ResourceLocation aberrator_loc = weaponModel("models/weapons/aberrator.obj");
    public static final ResourceLocation am180_loc = weaponModel("models/weapons/am180.obj");
    public static final ResourceLocation amat_loc = weaponModel("models/weapons/amat.obj");
    public static final ResourceLocation bio_revolver_loc = weaponModel("models/weapons/bio_revolver.obj");
    public static final ResourceLocation bolter_loc = weaponModel("models/weapons/bolter.obj");
    public static final ResourceLocation boltgun_loc = weaponModel("models/weapons/boltgun.obj");
    public static final ResourceLocation building_loc = weaponModel("models/weapons/building.obj");
    public static final ResourceLocation c130_loc = weaponModel("models/weapons/c130.obj");
    public static final ResourceLocation carbine_loc = weaponModel("models/weapons/carbine.obj");
    public static final ResourceLocation chainsaw_loc = weaponModel("models/weapons/chainsaw.obj");
    public static final ResourceLocation charge_dynamite_loc = weaponModel("models/weapons/charge_dynamite.obj");
    public static final ResourceLocation charge_thrower_loc = weaponModel("models/weapons/charge_thrower.obj");
    public static final ResourceLocation chemthrower_loc = weaponModel("models/weapons/chemthrower.obj");
    public static final ResourceLocation chodeblaster_loc = weaponModel("models/weapons/chodeblaster.obj");
    public static final ResourceLocation coilgun_loc = weaponModel("models/weapons/coilgun.obj");
    public static final ResourceLocation congolake_loc = weaponModel("models/weapons/congolake.obj");
    public static final ResourceLocation crucible_loc = weaponModel("models/weapons/crucible.obj");
    public static final ResourceLocation detonator_laser_loc = weaponModel("models/weapons/detonator_laser.obj");
    public static final ResourceLocation drill_loc = weaponModel("models/weapons/drill.obj");
    public static final ResourceLocation fatman_loc = weaponModel("models/weapons/fatman.obj");
    public static final ResourceLocation fireext_loc = weaponModel("models/weapons/fireext.obj");
    public static final ResourceLocation flamethrower_loc = weaponModel("models/weapons/flamethrower.obj");
    public static final ResourceLocation flaregun_loc = weaponModel("models/weapons/flaregun.obj");
    public static final ResourceLocation folly_loc = weaponModel("models/weapons/folly.obj");
    public static final ResourceLocation g3_loc = weaponModel("models/weapons/g3.obj");
    public static final ResourceLocation gavel_loc = weaponModel("models/weapons/gavel.obj");
    public static final ResourceLocation greasegun_loc = weaponModel("models/weapons/greasegun.obj");
    public static final ResourceLocation grenades_loc = weaponModel("models/weapons/grenades.obj");
    public static final ResourceLocation grenade_aschrab_loc = weaponModel("models/weapons/grenade_aschrab.obj");
    public static final ResourceLocation grenade_frag_loc = weaponModel("models/weapons/grenade_frag.obj");
    public static final ResourceLocation hangman_loc = weaponModel("models/weapons/hangman.obj");
    public static final ResourceLocation henry_loc = weaponModel("models/weapons/henry.obj");
    public static final ResourceLocation lance_loc = weaponModel("models/weapons/lance.obj");
    public static final ResourceLocation laser_pistol_loc = weaponModel("models/weapons/laser_pistol.obj");
    public static final ResourceLocation lasrifle_loc = weaponModel("models/weapons/lasrifle.obj");
    public static final ResourceLocation lasrifle_mods_loc = weaponModel("models/weapons/lasrifle_mods.obj");
    public static final ResourceLocation launch_pad_erector_loc = weaponModel("models/weapons/launch_pad_erector.obj");
    public static final ResourceLocation launch_pad_silo_loc = weaponModel("models/weapons/launch_pad_silo.obj");
    public static final ResourceLocation liberator_loc = weaponModel("models/weapons/liberator.obj");
    public static final ResourceLocation lilmac_loc = weaponModel("models/weapons/lilmac.obj");
    public static final ResourceLocation m2_loc = weaponModel("models/weapons/m2_browning.obj");
    public static final ResourceLocation maresleg_loc = weaponModel("models/weapons/maresleg.obj");
    public static final ResourceLocation mas36_loc = weaponModel("models/weapons/mas36.obj");
    public static final ResourceLocation meteor_loc = weaponModel("models/weapons/meteor.obj");
    public static final ResourceLocation mike_hawk_loc = weaponModel("models/weapons/mike_hawk.obj");
    public static final ResourceLocation minigun_loc = weaponModel("models/weapons/minigun.obj");
    public static final ResourceLocation missile_launcher_loc = weaponModel("models/weapons/missile_launcher.obj");
    public static final ResourceLocation mk108_loc = weaponModel("models/weapons/mk108.obj");
    public static final ResourceLocation n_i_4_n_i_loc = weaponModel("models/weapons/n_i_4_n_i.obj");
    public static final ResourceLocation panzerschreck_loc = weaponModel("models/weapons/panzerschreck.obj");
    public static final ResourceLocation pepperbox_loc = weaponModel("models/weapons/pepperbox.obj");
    public static final ResourceLocation quadro_loc = weaponModel("models/weapons/quadro.obj");
    public static final ResourceLocation sacred_dragon_loc = weaponModel("models/weapons/sacred_dragon.obj");
    public static final ResourceLocation sexy_loc = weaponModel("models/weapons/sexy.obj");
    public static final ResourceLocation shredder_loc = weaponModel("models/weapons/shredder.obj");
    public static final ResourceLocation spas_12_loc = weaponModel("models/weapons/spas-12.obj");
    public static final ResourceLocation star_f_loc = weaponModel("models/weapons/star_f.obj");
    public static final ResourceLocation stg77_loc = weaponModel("models/weapons/stg77.obj");
    public static final ResourceLocation stinger_loc = weaponModel("models/weapons/stinger.obj");
    public static final ResourceLocation stopsign_loc = weaponModel("models/weapons/stopsign.obj");
    public static final ResourceLocation tau_loc = weaponModel("models/weapons/tau.obj");
    public static final ResourceLocation tesla_cannon_loc = weaponModel("models/weapons/tesla_cannon.obj");
    public static final ResourceLocation tom_flame_loc = weaponModel("models/weapons/tom_flame.obj");
    public static final ResourceLocation tom_main_loc = weaponModel("models/weapons/tom_main.obj");
    public static final ResourceLocation torpedo_loc = weaponModel("models/weapons/torpedo.obj");
    public static final ResourceLocation uzi_loc = weaponModel("models/weapons/uzi.obj");
    public static final ResourceLocation whiskey_loc = weaponModel("models/weapons/whiskey.obj");
    // Handheld melee / power armor models
    public static final ResourceLocation shimmer_sledge_loc = weaponModel("models/shimmer_sledge.obj");
    public static final ResourceLocation shimmer_axe_loc = weaponModel("models/shimmer_axe.obj");
    public static final ResourceLocation armor_ncr_loc = weaponModel("models/armor/ncrpa.obj");
    public static final ResourceLocation armor_remnant_loc = weaponModel("models/armor/remnant.obj");
    // Particle / Effect models (pre-baked into WeaponRenderRegistry to eliminate runtime OBJ parsing)
    public static final ResourceLocation casings = weaponModel("models/effect/casings.obj");
    public static final ResourceLocation casings_loc = casings;
    public static final ResourceLocation casings_model_loc = casings;

    public static final ResourceLocation aberrator = model("models/weapons/aberrator.obj");
    public static final ResourceLocation aberrator_tex = tex("textures/models/weapons/aberrator.png");
    public static final ResourceLocation am180 = model("models/weapons/am180.obj");
    public static final Supplier<HashMap<String, BusAnimation>> am180_anim = anim("models/weapons/animations/am180.json");
    public static final ResourceLocation am180_tex = tex("textures/models/weapons/am180.png");
    public static final ResourceLocation amat = model("models/weapons/amat.obj");
    public static final ResourceLocation amat_penance_tex = tex("textures/models/weapons/amat_penance.png");
    public static final ResourceLocation amat_subtlety_tex = tex("textures/models/weapons/amat_subtlety.png");
    public static final ResourceLocation amat_tex = tex("textures/models/weapons/amat.png");
    public static final ResourceLocation armor_ncr = model("models/armor/ncrpa.obj");
    public static final ResourceLocation bio_revolver = model("models/weapons/bio_revolver.obj");
    public static final ResourceLocation bio_revolver_atlas_tex = tex("textures/models/weapons/bio_revolver_atlas.png");
    public static final ResourceLocation bio_revolver_tex = tex("textures/models/weapons/bio_revolver.png");
    public static final ResourceLocation bolter = model("models/weapons/bolter.obj");
    public static final ResourceLocation bolter_tex = tex("textures/models/weapons/bolter.png");
    public static final SimpleObjModel boxcar = entityModel("models/boxcar.obj");
    public static final ResourceLocation boxcar_tex = tex("textures/models/boxcar.png");
    public static final SimpleObjModel building = entityModel("models/weapons/building.obj");
    public static final ResourceLocation building_tex = tex("textures/models/weapons/building.png");
    public static final ResourceLocation bullet_rifle_tex = tex("textures/models/projectiles/bullet_rifle.png");
    public static final ResourceLocation carbine = model("models/weapons/carbine.obj");
    public static final ResourceLocation carbine_bayonet_tex = tex("textures/models/weapons/carbine_bayonet.png");
    public static final ResourceLocation carbine_scope_tex = tex("textures/models/weapons/carbine_scope.png");
    public static final ResourceLocation carbine_tex = tex("textures/models/weapons/huntsman.png");
    public static final ResourceLocation casings_tex = tex("textures/particle/casings.png");
    public static final ResourceLocation charge_thrower = model("models/weapons/charge_thrower.obj");
    public static final ResourceLocation charge_thrower_hook_tex = tex("textures/models/weapons/charge_thrower_hook.png");
    public static final ResourceLocation charge_thrower_mortar_tex = tex("textures/models/weapons/charge_thrower_mortar.png");
    public static final ResourceLocation charge_thrower_rocket_tex = tex("textures/models/weapons/charge_thrower_rocket.png");
    public static final ResourceLocation charge_thrower_tex = tex("textures/models/weapons/charge_thrower.png");
    public static final ResourceLocation chemthrower = model("models/weapons/chemthrower.obj");
    public static final ResourceLocation chemthrower_tex = tex("textures/models/weapons/chemthrower.png");
    public static final ResourceLocation cluster_submunition_tex = tex("textures/models/weapons/fatman_submunition.png");
    public static final ResourceLocation coilgun = model("models/weapons/coilgun.obj");
    public static final ResourceLocation coilgun_tex = tex("textures/models/weapons/coilgun.png");
    public static final ResourceLocation congolake = model("models/weapons/congolake.obj");
    public static final Supplier<HashMap<String, BusAnimation>> congolake_anim = anim("models/weapons/animations/congolake.json");
    public static final ResourceLocation congolake_tex = tex("textures/models/weapons/congolake.png");
    public static final ResourceLocation dani_celestial_tex = tex("textures/models/weapons/dani_celestial.png");
    public static final ResourceLocation dani_lunar_tex = tex("textures/models/weapons/dani_lunar.png");
    public static final SimpleObjModel deb_blank = entityModel("models/projectiles/deb_blank.obj");
    public static final SimpleObjModel deb_element = entityModel("models/projectiles/deb_element.obj");
    public static final SimpleObjModel deb_fuel = entityModel("models/projectiles/deb_fuel.obj");
    public static final SimpleObjModel deb_graphite = entityModel("models/projectiles/deb_graphite.obj");
    public static final SimpleObjModel deb_lid = entityModel("models/projectiles/deb_lid.obj");
    public static final SimpleObjModel deb_rod = entityModel("models/projectiles/deb_rod.obj");
    public static final SimpleObjModel deb_zirnox_blank = entityModel("models/zirnox/deb_blank.obj");
    public static final SimpleObjModel deb_zirnox_concrete = entityModel("models/zirnox/deb_concrete.obj");
    public static final SimpleObjModel deb_zirnox_element = entityModel("models/zirnox/deb_element.obj");
    public static final SimpleObjModel deb_zirnox_exchanger = entityModel("models/zirnox/deb_exchanger.obj");
    public static final SimpleObjModel deb_zirnox_shrapnel = entityModel("models/zirnox/deb_shrapnel.obj");
    public static final ResourceLocation debug_gun_tex = tex("textures/models/weapons/debug_gun.png");
    public static final ResourceLocation double_barrel = model("models/weapons/sacred_dragon.obj");
    public static final ResourceLocation double_barrel_sacred_dragon_tex = tex("textures/models/weapons/double_barrel_sacred_dragon.png");
    public static final ResourceLocation double_barrel_tex = tex("textures/models/weapons/double_barrel.png");
    public static final ResourceLocation drill = model("models/weapons/drill.obj");
    public static final ResourceLocation drill_tex = tex("textures/models/weapons/drill.png");
    public static final SimpleObjModel duchessgambit = entityModel("models/duchessgambit.obj");
    public static final ResourceLocation duchessgambit_tex = tex("textures/models/duchessgambit.png");
    public static final ResourceLocation eott_tex = tex("textures/models/weapons/eott.png");
    public static final ResourceLocation fatman = model("models/weapons/fatman.obj");
    public static final ResourceLocation fatman_balefire_tex = tex("textures/models/weapons/fatman_balefire.png");
    public static final ResourceLocation fatman_mininuke_tex = tex("textures/models/weapons/fatman_mininuke.png");
    public static final ResourceLocation fatman_tex = tex("textures/models/weapons/fatman.png");
    public static final ResourceLocation flamethrower = model("models/weapons/flamethrower.obj");
    public static final Supplier<HashMap<String, BusAnimation>> flamethrower_anim = anim("models/weapons/animations/flamethrower.json");
    public static final ResourceLocation flamethrower_daybreaker_tex = tex("textures/models/weapons/flamethrower_daybreaker.png");
    public static final ResourceLocation flamethrower_tex = tex("textures/models/weapons/flamethrower.png");
    public static final ResourceLocation flamethrower_topaz_tex = tex("textures/models/weapons/flamethrower_topaz.png");
    public static final ResourceLocation flaregun = model("models/weapons/flaregun.obj");
    public static final ResourceLocation flaregun_tex = tex("textures/models/weapons/flaregun.png");
    public static final ResourceLocation flechette_tex = tex("textures/models/projectiles/flechette.png");
    public static final ResourceLocation folly = model("models/weapons/folly.obj");
    public static final ResourceLocation folly_tex = tex("textures/models/weapons/moonlight.png");
    public static final ResourceLocation g3 = model("models/weapons/g3.obj");
    public static final ResourceLocation g3_attachments = tex("textures/models/weapons/g3_attachments.png");
    public static final ResourceLocation g3_black_tex = tex("textures/models/weapons/g3_polymer_black.png");
    public static final ResourceLocation g3_green_tex = tex("textures/models/weapons/g3_polymer_green.png");
    public static final ResourceLocation g3_tex = tex("textures/models/weapons/g3.png");
    public static final ResourceLocation g3_zebra_tex = tex("textures/models/weapons/g3_zebra.png");
    public static final ResourceLocation greasegun = model("models/weapons/greasegun.obj");
    public static final ResourceLocation greasegun_clean_tex = tex("textures/models/weapons/greasegun_clean.png");
    public static final ResourceLocation greasegun_tex = tex("textures/models/weapons/greasegun.png");
    public static final ResourceLocation grenade_frag_tex = tex("textures/models/grenades/frag.png");
    public static final ResourceLocation grenade_tex = tex("textures/models/projectiles/grenade.png");
    public static final ResourceLocation hangman = model("models/weapons/hangman.obj");
    public static final ResourceLocation hangman_tex = tex("textures/models/weapons/hangman.png");
    public static final ResourceLocation heavy_revolver_protege_tex = tex("textures/models/weapons/protege.png");
    public static final ResourceLocation heavy_revolver_tex = tex("textures/models/weapons/heavy_revolver.png");
    public static final ResourceLocation henry = model("models/weapons/henry.obj");
    public static final ResourceLocation henry_lincoln_tex = tex("textures/models/weapons/henry_lincoln.png");
    public static final ResourceLocation henry_tex = tex("textures/models/weapons/henry.png");
    public static final ResourceLocation heretic_tex = tex("textures/models/weapons/sexy_heretic.png");
    public static final ResourceLocation himars_standard_tex = tex("textures/models/projectiles/himars_standard.png");
    public static final Supplier<HashMap<String, BusAnimation>> lag_anim = anim("models/weapons/animations/lag.json");
    public static final ResourceLocation laser_pistol = model("models/weapons/laser_pistol.obj");
    public static final ResourceLocation laser_pistol_morning_glory_tex = tex("textures/models/weapons/laser_pistol_morning_glory.png");
    public static final ResourceLocation laser_pistol_pew_pew_tex = tex("textures/models/weapons/laser_pistol_pew_pew.png");
    public static final ResourceLocation laser_pistol_tex = tex("textures/models/weapons/laser_pistol.png");
    public static final ResourceLocation lasrifle = model("models/weapons/lasrifle.obj");
    public static final ResourceLocation lasrifle_mods = model("models/weapons/lasrifle_mods.obj");
    public static final ResourceLocation lasrifle_mods_tex = tex("textures/models/weapons/lasrifle_mods.png");
    public static final ResourceLocation lasrifle_tex = tex("textures/models/weapons/lasrifle.png");
    public static final ResourceLocation liberator = model("models/weapons/liberator.obj");
    public static final ResourceLocation liberator_tex = tex("textures/models/weapons/liberator.png");
    public static final ResourceLocation lilmac = model("models/weapons/lilmac.obj");
    public static final ResourceLocation lilmac_scope_tex = tex("textures/models/weapons/lilmac_scope.png");
    public static final ResourceLocation lilmac_tex = tex("textures/models/weapons/lilmac.png");
    public static final ResourceLocation m2 = model("models/weapons/m2_browning.obj");
    public static final ResourceLocation m2_tex = tex("textures/models/weapons/m2_browning.png");
    public static final ResourceLocation maresleg = model("models/weapons/maresleg.obj");
    public static final ResourceLocation maresleg_broken_tex = tex("textures/models/weapons/maresleg_broken.png");
    public static final ResourceLocation maresleg_tex = tex("textures/models/weapons/maresleg.png");
    public static final ResourceLocation mas36 = model("models/weapons/mas36.obj");
    public static final ResourceLocation mas36_tex = tex("textures/models/weapons/mas36.png");
    public static final ResourceLocation meteor = model("models/weapons/meteor.obj");
    public static final ResourceLocation mike_hawk = model("models/weapons/mike_hawk.obj");
    public static final ResourceLocation mike_hawk_tex = tex("textures/models/weapons/lag.png");
    public static final ResourceLocation minigun = model("models/weapons/minigun.obj");
    public static final ResourceLocation minigun_dual_tex = tex("textures/models/weapons/minigun_dual.png");
    public static final ResourceLocation minigun_lacunae_tex = tex("textures/models/weapons/minigun_lacunae.png");
    public static final ResourceLocation minigun_tex = tex("textures/models/weapons/minigun.png");
    public static final ResourceLocation missile_launcher = model("models/weapons/missile_launcher.obj");
    public static final ResourceLocation missile_launcher_tex = tex("textures/models/weapons/missile_launcher.png");
    public static final ResourceLocation mk108 = model("models/weapons/mk108.obj");
    public static final ResourceLocation mk108_tex = tex("textures/models/weapons/mk108.png");
    public static final ResourceLocation n_i_4_n_i = model("models/weapons/n_i_4_n_i.obj");
    public static final ResourceLocation n_i_4_n_i_greyscale_tex = tex("textures/models/weapons/n_i_4_n_i_greyscale.png");
    public static final ResourceLocation n_i_4_n_i_tex = tex("textures/models/weapons/n_i_4_n_i.png");
    public static final ResourceLocation ncrpa_arm = tex("textures/armor/ncrpa_arm.png");
    public static final ResourceLocation panzerschreck = model("models/weapons/panzerschreck.obj");
    public static final ResourceLocation panzerschreck_tex = tex("textures/models/weapons/panzerschreck.png");
    public static final ResourceLocation pepperbox = model("models/weapons/pepperbox.obj");
    public static final ResourceLocation pepperbox_tex = tex("textures/models/weapons/pepperbox.png");
    public static final SimpleObjModel projectiles = entityModel("models/projectiles/projectiles.obj");
    public static final ResourceLocation quadro = model("models/weapons/quadro.obj");
    public static final ResourceLocation quadro_rocket_tex = tex("textures/models/weapons/quadro_rocket.png");
    public static final ResourceLocation quadro_tex = tex("textures/models/weapons/quadro.png");
    public static final ResourceLocation rocket_mirv_tex = tex("textures/models/projectiles/rocket_mirv.png");
    public static final ResourceLocation rocket_tex = tex("textures/models/projectiles/rocket.png");
    public static final SimpleObjModel sat_foeq_burning = entityModel("models/sat_foeq_burning.obj");
    public static final ResourceLocation sat_foeq_burning_tex = tex("textures/models/sat_foeq_burning.png");
    public static final SimpleObjModel sat_foeq_fire = entityModel("models/sat_foeq_fire.obj");
    public static final ResourceLocation sat_foeq_tex = tex("textures/models/sat_foeq.png");
    public static final SimpleObjModel sawmill = entityModel("models/machines/sawmill.obj");
    public static final ResourceLocation sawmill_tex = tex("textures/models/machines/sawmill.png");
    public static final ResourceLocation sexy = model("models/weapons/sexy.obj");
    public static final ResourceLocation sexy_tex = tex("textures/models/weapons/sexy_real_no_fake.png");
    public static final ResourceLocation shredder = model("models/weapons/shredder.obj");
    public static final ResourceLocation shredder_orig_tex = tex("textures/models/weapons/shredder_orig.png");
    public static final ResourceLocation shredder_tex = tex("textures/models/weapons/shredder.png");
    public static final ResourceLocation spas_12 = model("models/weapons/spas-12.obj");
    public static final Supplier<HashMap<String, BusAnimation>> spas_12_anim = anim("models/weapons/animations/spas12.json");
    public static final ResourceLocation spas_12_tex = tex("textures/models/weapons/spas-12.png");
    public static final SimpleObjModel sphere_uv = entityModel("models/sphere_uv.obj");
    public static final ResourceLocation star_f = model("models/weapons/star_f.obj");
    public static final ResourceLocation star_f_elite_tex = tex("textures/models/weapons/star_f_elite.png");
    public static final ResourceLocation star_f_tex = tex("textures/models/weapons/star_f.png");
    public static final ResourceLocation stg77 = model("models/weapons/stg77.obj");
    public static final Supplier<HashMap<String, BusAnimation>> stg77_anim = anim("models/weapons/animations/stg77.json");
    public static final ResourceLocation stg77_tex = tex("textures/models/weapons/stg77.png");
    public static final ResourceLocation stinger = model("models/weapons/stinger.obj");
    public static final ResourceLocation stinger_tex = tex("textures/models/weapons/stinger.png");
    public static final SimpleObjModel stirling = entityModel("models/machines/stirling.obj");
    public static final ResourceLocation stirling_steel_tex = tex("textures/models/machines/stirling_steel.png");
    public static final ResourceLocation stirling_tex = tex("textures/models/machines/stirling.png");
    public static final ResourceLocation tau = model("models/weapons/tau.obj");
    public static final ResourceLocation tau_tex = tex("textures/models/weapons/tau.png");
    public static final ResourceLocation tesla_cannon = model("models/weapons/tesla_cannon.obj");
    public static final ResourceLocation tesla_cannon_tex = tex("textures/models/weapons/tesla_cannon.png");
    public static final ResourceLocation tom_flame_tex = tex("textures/models/weapons/tom_flame.png");
    public static final SimpleObjModel torpedo = entityModel("models/weapons/torpedo.obj");
    public static final ResourceLocation torpedo_tex = tex("textures/models/weapons/torpedo.png");
    public static final SimpleObjModel turret_arty = entityModel("models/turrets/turret_arty.obj");
    public static final ResourceLocation turret_arty_tex = tex("textures/models/turrets/arty.png");
    public static final ResourceLocation turret_base_friendly_tex = tex("textures/models/turrets/base_friendly.png");
    public static final ResourceLocation turret_base_rusted = tex("textures/models/turrets/rusted/base.png");
    public static final ResourceLocation turret_base_tex = tex("textures/models/turrets/base.png");
    public static final ResourceLocation turret_carriage_ciws_rusted = tex("textures/models/turrets/rusted/carriage_ciws.png");
    public static final ResourceLocation turret_carriage_ciws_tex = tex("textures/models/turrets/carriage_ciws.png");
    public static final ResourceLocation turret_carriage_friendly_tex = tex("textures/models/turrets/carriage_friendly.png");
    public static final ResourceLocation turret_carriage_tex = tex("textures/models/turrets/carriage.png");
    public static final SimpleObjModel turret_chekhov = entityModel("models/turrets/turret_chekhov.obj");
    public static final ResourceLocation turret_chekhov_barrels_tex = tex("textures/models/turrets/chekhov_barrels.png");
    public static final ResourceLocation turret_chekhov_tex = tex("textures/models/turrets/chekhov.png");
    public static final ResourceLocation turret_connector_tex = tex("textures/models/turrets/connector.png");
    public static final SimpleObjModel turret_fritz = entityModel("models/turrets/turret_fritz.obj");
    public static final ResourceLocation turret_fritz_tex = tex("textures/models/turrets/fritz.png");
    public static final SimpleObjModel turret_himars = entityModel("models/turrets/turret_himars.obj");
    public static final ResourceLocation turret_himars_tex = tex("textures/models/turrets/himars.png");
    public static final SimpleObjModel turret_howard = entityModel("models/turrets/turret_howard.obj");
    public static final ResourceLocation turret_howard_barrels_rusted = tex("textures/models/turrets/rusted/howard_barrels.png");
    public static final ResourceLocation turret_howard_barrels_tex = tex("textures/models/turrets/howard_barrels.png");
    public static final SimpleObjModel turret_howard_damaged = entityModel("models/turrets/turret_howard_damaged.obj");
    public static final ResourceLocation turret_howard_rusted = tex("textures/models/turrets/rusted/howard.png");
    public static final ResourceLocation turret_howard_tex = tex("textures/models/turrets/howard.png");
    public static final SimpleObjModel turret_jeremy = entityModel("models/turrets/turret_jeremy.obj");
    public static final ResourceLocation turret_jeremy_tex = tex("textures/models/turrets/jeremy.png");
    public static final SimpleObjModel turret_maxwell = entityModel("models/turrets/turret_microwave.obj");
    public static final ResourceLocation turret_maxwell_tex = tex("textures/models/turrets/maxwell.png");
    public static final SimpleObjModel turret_richard = entityModel("models/turrets/turret_richard.obj");
    public static final ResourceLocation turret_richard_tex = tex("textures/models/turrets/richard.png");
    public static final SimpleObjModel turret_sentry = entityModel("models/turrets/turret_sentry.obj");
    public static final ResourceLocation turret_sentry_damaged_tex = tex("textures/models/turrets/sentry_damaged.png");
    public static final ResourceLocation turret_sentry_tex = tex("textures/models/turrets/sentry.png");
    public static final SimpleObjModel turret_tauon = entityModel("models/turrets/turret_tauon.obj");
    public static final ResourceLocation turret_tauon_tex = tex("textures/models/turrets/tauon.png");
    public static final ResourceLocation universal = tex("textures/models/thegadget3_.png");
    public static final ResourceLocation uzi = model("models/weapons/uzi.obj");
    public static final ResourceLocation uzi_saturnite_tex = tex("textures/models/weapons/uzi_saturnite.png");
    public static final ResourceLocation uzi_tex = tex("textures/models/weapons/uzi.png");
    public static final ResourceLocation whiskey = model("models/weapons/whiskey.obj");
    public static final ResourceLocation whiskey_tex = tex("textures/models/weapons/whiskey.png");
    public static final ResourceLocation wire_greyscale_tex = tex("textures/models/network/wire_greyscale.png");
    public static final ResourceLocation zirnox_destroyed_tex = tex("textures/models/zirnox_destroyed.png");
    public static final ResourceLocation zirnox_tex = tex("textures/models/zirnox.png");
    // ItemRenderFireExt (ausserhalb render/item/weapon/sedna)
    public static final ResourceLocation fireext = model("models/weapons/fireext.obj");
    public static final ResourceLocation fireext_tex = tex("textures/models/weapons/fireext_normal.png");
    public static final ResourceLocation fireext_foam_tex = tex("textures/models/weapons/fireext_foam.png");
    public static final ResourceLocation fireext_sand_tex = tex("textures/models/weapons/fireext_sand.png");
    // Baukastengranaten (ItemRenderGrenade / RenderGrenadeUniversal)
    public static final ResourceLocation grenades = model("models/weapons/grenades.obj");
    public static final ResourceLocation grenade_frag_body_tex = tex("textures/models/grenades/frag_body.png");
    public static final ResourceLocation grenade_frag_label_tex = tex("textures/models/grenades/frag_label.png");
    public static final ResourceLocation grenade_frag_fuze_tex = tex("textures/models/grenades/frag_fuze.png");
    public static final ResourceLocation grenade_stick_tex = tex("textures/models/grenades/stick.png");
    public static final ResourceLocation grenade_stick_body_tex = tex("textures/models/grenades/stick_body.png");
    public static final ResourceLocation grenade_stick_label_tex = tex("textures/models/grenades/stick_label.png");
    public static final ResourceLocation grenade_stick_fuze_tex = tex("textures/models/grenades/stick_fuze.png");
    public static final ResourceLocation grenade_tech_tex = tex("textures/models/grenades/tech.png");
    public static final ResourceLocation grenade_tech_body_tex = tex("textures/models/grenades/tech_body.png");
    public static final ResourceLocation grenade_tech_lights_tex = tex("textures/models/grenades/tech_lights.png");
    public static final ResourceLocation grenade_tech_fuze_tex = tex("textures/models/grenades/tech_fuze.png");
    public static final ResourceLocation grenade_nuka_tex = tex("textures/models/grenades/nuka.png");
    public static final ResourceLocation grenade_nuka_body_tex = tex("textures/models/grenades/nuka_body.png");
    public static final ResourceLocation grenade_nuka_label_tex = tex("textures/models/grenades/nuka_label.png");
    public static final ResourceLocation grenade_nuka_fuze_tex = tex("textures/models/grenades/nuka_fuze.png");

    // Aliases for melee & weapon renderers
    public static final ResourceLocation boltgun = boltgun_loc;
    public static final ResourceLocation chainsaw = chainsaw_loc;
    public static final ResourceLocation crucible = crucible_loc;
    public static final ResourceLocation gavel = gavel_loc;
    public static final ResourceLocation shimmer_sledge = shimmer_sledge_loc;
    public static final ResourceLocation shimmer_axe = shimmer_axe_loc;
    public static final ResourceLocation stopsign = stopsign_loc;
    public static final ResourceLocation armor_remnant = armor_remnant_loc;
}
