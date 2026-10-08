package com.hbm_m.entity;

import com.hbm_m.entity.effect.EntityCloudFleija;
import com.hbm_m.entity.effect.EntityFalloutRain;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.entity.grenades.*;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.entity.mob.EntityCreeperGold;
import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.entity.mob.EntityCreeperPhosgene;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.entity.mob.EntityCreeperVolatile;
import com.hbm_m.entity.mob.NoloEntity;
import com.hbm_m.entity.missile.MissileABMEntity;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.entity.missile.MissileTestEntity;
import com.hbm_m.entity.missile.MissileShuttleEntity;
import com.hbm_m.entity.missile.MissileStealthEntity;
import com.hbm_m.entity.missile.MissileTier0;
import com.hbm_m.entity.missile.MissileTier1;
import com.hbm_m.entity.missile.MissileTier2;
import com.hbm_m.entity.missile.Missile9M723Entity;
import com.hbm_m.entity.missile.MissileTier3;
import com.hbm_m.entity.missile.MissileTier4;
import com.hbm_m.entity.projectile.ZirnoxDebrisEntity;
import com.hbm_m.entity.projectile.TurretBulletEntity;
import com.hbm_m.entity.projectile.TurretRocketEntity;
import com.hbm_m.main.MainRegistry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(MainRegistry.MOD_ID, Registries.ENTITY_TYPE);



    /** 1:1 with the original's "entity_rbmk_debris" - meltdown wreckage, see RBMKDebrisEntity. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.rbmk.RBMKDebrisEntity>> RBMK_DEBRIS =
        ENTITY_TYPES.register("rbmk_debris",
            () -> EntityType.Builder.<com.hbm_m.entity.rbmk.RBMKDebrisEntity>of(
                        com.hbm_m.entity.rbmk.RBMKDebrisEntity::new, MobCategory.MISC)
                .sized(1f, 1f)
                .clientTrackingRange(63)
                .updateInterval(1).build("rbmk_debris"));

    public static final RegistrySupplier<EntityType<TurretBulletEntity>> TURRET_BULLET =
        ENTITY_TYPES.register("turret_bullet",
            () -> EntityType.Builder.<TurretBulletEntity>of(TurretBulletEntity::new, MobCategory.MISC)
                .sized(0.25f, 0.25f)
                .build("turret_bullet"));

    public static final RegistrySupplier<EntityType<TurretRocketEntity>> TURRET_ROCKET =
        ENTITY_TYPES.register("turret_rocket",
            () -> EntityType.Builder.<TurretRocketEntity>of(TurretRocketEntity::new, MobCategory.MISC)
                .sized(0.4f, 0.4f)
                .build("turret_rocket"));

    public static final RegistrySupplier<EntityType<GrenadeProjectileEntity>> GRENADE_PROJECTILE =
        ENTITY_TYPES.register("grenade_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenade_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeProjectileEntity>> GRENADEHE_PROJECTILE =
        ENTITY_TYPES.register("grenadehe_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadehe_projectile"));

    public static final RegistrySupplier<EntityType<AirstrikeEntity>> AIRSTRIKE_ENTITY =
            ENTITY_TYPES.register("airstrike",
                    () -> EntityType.Builder.<AirstrikeEntity>of(AirstrikeEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.0F)
                            .build("airstrike")
            );
    public static final RegistrySupplier<EntityType<AirstrikeNukeEntity>> AIRSTRIKE_NUKE_ENTITY =
            ENTITY_TYPES.register("airstrikenuke",
                    () -> EntityType.Builder.<AirstrikeNukeEntity>of(AirstrikeNukeEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.0F)
                            .build("airstrikenuke"));

    public static final RegistrySupplier<EntityType<AirstrikeAgentEntity>> AIRSTRIKE_AGENT_ENTITY =
            ENTITY_TYPES.register("airstrikeagent",
                    () -> EntityType.Builder.<AirstrikeAgentEntity>of(AirstrikeAgentEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.0F)
                            .build("airstrikeagent"));

    public static final RegistrySupplier<EntityType<AirBombProjectileEntity>> AIRBOMB_PROJECTILE =
            ENTITY_TYPES.register("airbomb_projectile",
                    () -> EntityType.Builder.<AirBombProjectileEntity>of(AirBombProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("airbomb_projectile"));
    public static final RegistrySupplier<EntityType<AirNukeBombProjectileEntity>> AIRNUKEBOMB_PROJECTILE =
            ENTITY_TYPES.register("airnukebomb_projectile",
                    () -> EntityType.Builder.<AirNukeBombProjectileEntity>of(AirNukeBombProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("airnukebomb_projectile"));
    public static final RegistrySupplier<EntityType<GrenadeProjectileEntity>> GRENADEFIRE_PROJECTILE =
        ENTITY_TYPES.register("grenadefire_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadefire_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeProjectileEntity>> GRENADESMART_PROJECTILE =
        ENTITY_TYPES.register("grenadesmart_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadesmart_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeProjectileEntity>> GRENADESLIME_PROJECTILE =
        ENTITY_TYPES.register("grenadeslime_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadeslime_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("grenade_if_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_FIRE_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_fire_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenade_if_fire_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_SLIME_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_slime_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("grenade_if_slime_projectile"));

    public static final RegistrySupplier<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_HE_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_he_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenade_if_he_projectile"));


    public static final RegistrySupplier<EntityType<GrenadeNucProjectileEntity>> GRENADE_NUC_PROJECTILE =
            ENTITY_TYPES.register("grenade_nuc_projectile",
                    () -> EntityType.Builder.<GrenadeNucProjectileEntity>of(GrenadeNucProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("grenade_nuc_projectile"));

    // ПРОТОТИП БАЛЛИСТИЧЕСКОЙ РАКЕТЫ (TIER 0)
    public static final RegistrySupplier<EntityType<MissileTestEntity>> MISSILE_TEST =
            ENTITY_TYPES.register("missile_test",
                    () -> EntityType.Builder.<MissileTestEntity>of(MissileTestEntity::new, MobCategory.MISC)
                            .sized(1.5F, 1.5F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("missile_test"));

    public static final RegistrySupplier<EntityType<MissileABMEntity>> MISSILE_ABM =
            ENTITY_TYPES.register("missile_abm",
                    () -> EntityType.Builder.<MissileABMEntity>of(MissileABMEntity::new, MobCategory.MISC)
                            .sized(1.5F, 1.5F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("missile_abm"));

    // Soyuz Launcher: flight entity + cargo-mode descent capsule (see SoyuzLauncherBlockEntity)
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.missile.SoyuzEntity>> SOYUZ =
            ENTITY_TYPES.register("soyuz",
                    () -> EntityType.Builder.<com.hbm_m.entity.missile.SoyuzEntity>of(com.hbm_m.entity.missile.SoyuzEntity::new, MobCategory.MISC)
                            .sized(5.0F, 50.0F)
                            .clientTrackingRange(512)
                            .updateInterval(1)
                            .build("soyuz"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.missile.SoyuzCapsuleEntity>> SOYUZ_CAPSULE =
            ENTITY_TYPES.register("soyuz_capsule",
                    () -> EntityType.Builder.<com.hbm_m.entity.missile.SoyuzCapsuleEntity>of(com.hbm_m.entity.missile.SoyuzCapsuleEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("soyuz_capsule"));

    /** Original {@code entity_artillery_shell} / {@code entity_himars} (EntityMappings, Tracking 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityArtilleryShell>> ARTILLERY_SHELL =
            ENTITY_TYPES.register("entity_artillery_shell",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityArtilleryShell>of(com.hbm_m.entity.projectile.EntityArtilleryShell::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(1).build("entity_artillery_shell"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityArtilleryRocket>> ARTILLERY_ROCKET =
            ENTITY_TYPES.register("entity_himars",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityArtilleryRocket>of(com.hbm_m.entity.projectile.EntityArtilleryRocket::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1).build("entity_himars"));

    // Tier 0
    public static final RegistrySupplier<EntityType<MissileTier0.MissileMicro>> MISSILE_MICRO =
            ENTITY_TYPES.register("missile_micro",
                    () -> missileBuilder(MissileTier0.MissileMicro::new).build("missile_micro"));

    public static final RegistrySupplier<EntityType<MissileTier0.MissileSchrabidium>> MISSILE_SCHRABIDIUM =
            ENTITY_TYPES.register("missile_schrabidium",
                    () -> missileBuilder(MissileTier0.MissileSchrabidium::new).build("missile_schrabidium"));

    public static final RegistrySupplier<EntityType<MissileTier0.MissileBHole>> MISSILE_BHOLE =
            ENTITY_TYPES.register("missile_bhole",
                    () -> missileBuilder(MissileTier0.MissileBHole::new).build("missile_bhole"));

    public static final RegistrySupplier<EntityType<MissileTier0.MissileTaint>> MISSILE_TAINT =
            ENTITY_TYPES.register("missile_taint",
                    () -> missileBuilder(MissileTier0.MissileTaint::new).build("missile_taint"));

    public static final RegistrySupplier<EntityType<MissileTier0.MissileEmp>> MISSILE_EMP =
            ENTITY_TYPES.register("missile_emp",
                    () -> missileBuilder(MissileTier0.MissileEmp::new).build("missile_emp"));

    /** 1:1 entity_custom_missile: Baukasten-Rakete. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.missile.EntityMissileCustom>> MISSILE_CUSTOM =
            ENTITY_TYPES.register("entity_custom_missile",
                    () -> missileBuilder(com.hbm_m.entity.missile.EntityMissileCustom::new).build("entity_custom_missile"));

    // Tier 1
    public static final RegistrySupplier<EntityType<MissileTier1.MissileGeneric>> MISSILE_GENERIC =
            ENTITY_TYPES.register("missile_generic",
                    () -> missileBuilder(MissileTier1.MissileGeneric::new).build("missile_generic"));

    public static final RegistrySupplier<EntityType<MissileTier1.MissileIncendiary>> MISSILE_INCENDIARY =
            ENTITY_TYPES.register("missile_incendiary",
                    () -> missileBuilder(MissileTier1.MissileIncendiary::new).build("missile_incendiary"));

    public static final RegistrySupplier<EntityType<MissileTier1.MissileCluster>> MISSILE_CLUSTER =
            ENTITY_TYPES.register("missile_cluster",
                    () -> missileBuilder(MissileTier1.MissileCluster::new).build("missile_cluster"));

    public static final RegistrySupplier<EntityType<MissileTier1.MissileBuster>> MISSILE_BUSTER =
            ENTITY_TYPES.register("missile_buster",
                    () -> missileBuilder(MissileTier1.MissileBuster::new).build("missile_buster"));

    public static final RegistrySupplier<EntityType<MissileTier1.MissileDecoy>> MISSILE_DECOY =
            ENTITY_TYPES.register("missile_decoy",
                    () -> missileBuilder(MissileTier1.MissileDecoy::new).build("missile_decoy"));

    public static final RegistrySupplier<EntityType<MissileStealthEntity>> MISSILE_STEALTH =
            ENTITY_TYPES.register("missile_stealth",
                    () -> missileBuilder(MissileStealthEntity::new).build("missile_stealth"));

    // Tier 2
    public static final RegistrySupplier<EntityType<MissileTier2.MissileStrong>> MISSILE_STRONG =
            ENTITY_TYPES.register("missile_strong",
                    () -> missileBuilder(MissileTier2.MissileStrong::new).build("missile_strong"));

    public static final RegistrySupplier<EntityType<MissileTier2.MissileIncendiaryStrong>> MISSILE_INCENDIARY_STRONG =
            ENTITY_TYPES.register("missile_incendiary_strong",
                    () -> missileBuilder(MissileTier2.MissileIncendiaryStrong::new).build("missile_incendiary_strong"));

    public static final RegistrySupplier<EntityType<MissileTier2.MissileClusterStrong>> MISSILE_CLUSTER_STRONG =
            ENTITY_TYPES.register("missile_cluster_strong",
                    () -> missileBuilder(MissileTier2.MissileClusterStrong::new).build("missile_cluster_strong"));

    public static final RegistrySupplier<EntityType<MissileTier2.MissileBusterStrong>> MISSILE_BUSTER_STRONG =
            ENTITY_TYPES.register("missile_buster_strong",
                    () -> missileBuilder(MissileTier2.MissileBusterStrong::new).build("missile_buster_strong"));

    public static final RegistrySupplier<EntityType<MissileTier2.MissileEmpStrong>> MISSILE_EMP_STRONG =
            ENTITY_TYPES.register("missile_emp_strong",
                    () -> missileBuilder(MissileTier2.MissileEmpStrong::new).build("missile_emp_strong"));

    // Tier 3
    public static final RegistrySupplier<EntityType<MissileTier3.MissileBurst>> MISSILE_BURST =
            ENTITY_TYPES.register("missile_burst",
                    () -> missileBuilder(MissileTier3.MissileBurst::new).build("missile_burst"));

    public static final RegistrySupplier<EntityType<MissileTier3.MissileInferno>> MISSILE_INFERNO =
            ENTITY_TYPES.register("missile_inferno",
                    () -> missileBuilder(MissileTier3.MissileInferno::new).build("missile_inferno"));

    public static final RegistrySupplier<EntityType<MissileTier3.MissileRain>> MISSILE_RAIN =
            ENTITY_TYPES.register("missile_rain",
                    () -> missileBuilder(MissileTier3.MissileRain::new).build("missile_rain"));

    public static final RegistrySupplier<EntityType<MissileTier3.MissileDrill>> MISSILE_DRILL =
            ENTITY_TYPES.register("missile_drill",
                    () -> missileBuilder(MissileTier3.MissileDrill::new).build("missile_drill"));

    // Iskander-M 9M723 - quasiballistisch, gedrueckte Bahn, Mach-6-Reiseprofil
    public static final RegistrySupplier<EntityType<Missile9M723Entity.HighExplosive>> MISSILE_9M723 =
            ENTITY_TYPES.register("missile_9m723",
                    () -> missileBuilder(Missile9M723Entity.HighExplosive::new).build("missile_9m723"));

    public static final RegistrySupplier<EntityType<Missile9M723Entity.Buster>> MISSILE_9M723_BUSTER =
            ENTITY_TYPES.register("missile_9m723_buster",
                    () -> missileBuilder(Missile9M723Entity.Buster::new).build("missile_9m723_buster"));

    /** RT-2PM2 Topol-M - ICBM mit einem Gefechtskopf. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.missile.MissileTopolEntity>> MISSILE_TOPOL =
            ENTITY_TYPES.register("missile_topol",
                    () -> missileBuilder(com.hbm_m.entity.missile.MissileTopolEntity::new).build("missile_topol"));

    public static final RegistrySupplier<EntityType<MissileShuttleEntity>> MISSILE_SHUTTLE =
            ENTITY_TYPES.register("missile_shuttle",
                    () -> missileBuilder(MissileShuttleEntity::new).build("missile_shuttle"));

    // Tier 4
    public static final RegistrySupplier<EntityType<MissileTier4.MissileNuclear>> MISSILE_NUCLEAR =
            ENTITY_TYPES.register("missile_nuclear",
                    () -> missileBuilder(MissileTier4.MissileNuclear::new).build("missile_nuclear"));

    public static final RegistrySupplier<EntityType<MissileTier4.MissileNuclearCluster>> MISSILE_NUCLEAR_CLUSTER =
            ENTITY_TYPES.register("missile_nuclear_cluster",
                    () -> missileBuilder(MissileTier4.MissileNuclearCluster::new).build("missile_nuclear_cluster"));

    public static final RegistrySupplier<EntityType<MissileTier4.MissileVolcano>> MISSILE_VOLCANO =
            ENTITY_TYPES.register("missile_volcano",
                    () -> missileBuilder(MissileTier4.MissileVolcano::new).build("missile_volcano"));

    public static final RegistrySupplier<EntityType<MissileTier4.MissileDoomsday>> MISSILE_DOOMSDAY =
            ENTITY_TYPES.register("missile_doomsday",
                    () -> missileBuilder(MissileTier4.MissileDoomsday::new).build("missile_doomsday"));

    public static final RegistrySupplier<EntityType<MissileTier4.MissileDoomsdayRusted>> MISSILE_DOOMSDAY_RUSTED =
            ENTITY_TYPES.register("missile_doomsday_rusted",
                    () -> missileBuilder(MissileTier4.MissileDoomsdayRusted::new).build("missile_doomsday_rusted"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.ClusterRocketEntity>> CLUSTER_ROCKET =
            ENTITY_TYPES.register("cluster_rocket",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.ClusterRocketEntity>of(
                                    com.hbm_m.entity.projectile.ClusterRocketEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("cluster_rocket"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EmpPulseEntity>> EMP_PULSE =
            ENTITY_TYPES.register("emp_pulse",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EmpPulseEntity>of(
                                    com.hbm_m.entity.logic.EmpPulseEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("emp_pulse"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.BlackHoleEntity>> BLACK_HOLE =
            ENTITY_TYPES.register("black_hole",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.BlackHoleEntity>of(
                                    com.hbm_m.entity.effect.BlackHoleEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .fireImmune().build("black_hole"));

    /** Original {@code entity_grenade_universal} (EntityMappings, Tracking 250). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.grenade.EntityGrenadeUniversal>> GRENADE_UNIVERSAL =
            ENTITY_TYPES.register("entity_grenade_universal",
                    () -> EntityType.Builder.<com.hbm_m.entity.grenade.EntityGrenadeUniversal>of(com.hbm_m.entity.grenade.EntityGrenadeUniversal::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_grenade_universal"));

    /** Original {@code entity_fire_lingering} (EntityMappings, Tracking 250). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityFireLingering>> FIRE_LINGERING =
            ENTITY_TYPES.register("entity_fire_lingering",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityFireLingering>of(com.hbm_m.entity.effect.EntityFireLingering::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(16).updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_fire_lingering"));

    /** SEDNA: Original entity_bullet_mk4 / entity_bullet_mk4_cl / entity_beam_mk4 / entity_coin (EntityMappings). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBulletBaseMK4>> BULLET_MK4 =
            ENTITY_TYPES.register("entity_bullet_mk4",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBulletBaseMK4>of(com.hbm_m.entity.projectile.EntityBulletBaseMK4::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).fireImmune()
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_bullet_mk4"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBulletBaseMK4CL>> BULLET_MK4_CL =
            ENTITY_TYPES.register("entity_bullet_mk4_cl",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBulletBaseMK4CL>of(com.hbm_m.entity.projectile.EntityBulletBaseMK4CL::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).fireImmune()
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_bullet_mk4_cl"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBulletBeamBase>> BULLET_BEAM =
            ENTITY_TYPES.register("entity_beam_mk4",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBulletBeamBase>of(com.hbm_m.entity.projectile.EntityBulletBeamBase::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).fireImmune()
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_beam_mk4"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityCoin>> COIN =
            ENTITY_TYPES.register("entity_coin",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityCoin>of(com.hbm_m.entity.projectile.EntityCoin::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(63).updateInterval(1).build("entity_coin"));

    /** Original {@code entity_shrapnel} (EntityMappings, Tracking 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityShrapnel>> SHRAPNEL =
            ENTITY_TYPES.register("entity_shrapnel",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityShrapnel>of(
                                    com.hbm_m.entity.projectile.EntityShrapnel::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .fireImmune()
                            .build("entity_shrapnel"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.RubbleEntity>> RUBBLE =
            ENTITY_TYPES.register("rubble",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.RubbleEntity>of(
                                    com.hbm_m.entity.projectile.RubbleEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("rubble"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.VortexEntity>> VORTEX =
            ENTITY_TYPES.register("vortex",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.VortexEntity>of(
                                    com.hbm_m.entity.effect.VortexEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .fireImmune().build("vortex"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.RagingVortexEntity>> RAGING_VORTEX =
            ENTITY_TYPES.register("raging_vortex",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.RagingVortexEntity>of(
                                    com.hbm_m.entity.effect.RagingVortexEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .fireImmune().build("raging_vortex"));

    /** Original EntityGrenadeBouncyGeneric (Dynamit u. a.). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.grenades.EntityGrenadeBouncyGeneric>> GRENADE_BOUNCY_GENERIC =
            ENTITY_TYPES.register("grenade_bouncy_generic",
                    () -> EntityType.Builder.<com.hbm_m.entity.grenades.EntityGrenadeBouncyGeneric>of(
                                    com.hbm_m.entity.grenades.EntityGrenadeBouncyGeneric::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1)
                            .build("grenade_bouncy_generic"));
    /** Original EntityDisperserCanister ("entity_disperser"), geworfener Dispersionskanister / Glyphiden-Druese. */
    /** R6c: 1:1 EntityFireworks. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityFireworks>> FIREWORKS =
            ENTITY_TYPES.register("entity_firework_ball",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityFireworks>of(
                                    com.hbm_m.entity.effect.EntityFireworks::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_firework_ball"));

    /** R6c: 1:1 EntityEMPBlast ("entity_emp_blast"). */
    /** R6d: 1:1 EntityModFX-Gaswolke ({@code entity_chlorine_fx}). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityModFX.Chlorine>> CHLORINE_FX =
            ENTITY_TYPES.register("entity_chlorine_fx",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityModFX.Chlorine>of(
                                    com.hbm_m.entity.effect.EntityModFX.Chlorine::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_chlorine_fx"));
    /** R6d: 1:1 EntityModFX-Gaswolke ({@code entity_pink_cloud_fx}). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityModFX.PinkCloud>> PINK_CLOUD_FX =
            ENTITY_TYPES.register("entity_pink_cloud_fx",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityModFX.PinkCloud>of(
                                    com.hbm_m.entity.effect.EntityModFX.PinkCloud::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_pink_cloud_fx"));
    /** R6d: 1:1 EntityModFX-Gaswolke ({@code entity_cloud_fx}). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityModFX.Cloud>> CLOUD_FX =
            ENTITY_TYPES.register("entity_cloud_fx",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityModFX.Cloud>of(
                                    com.hbm_m.entity.effect.EntityModFX.Cloud::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_cloud_fx"));
    /** R6d: 1:1 EntityModFX-Gaswolke ({@code entity_agent_orange}). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityModFX.Orange>> ORANGE_FX =
            ENTITY_TYPES.register("entity_agent_orange",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityModFX.Orange>of(
                                    com.hbm_m.entity.effect.EntityModFX.Orange::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_agent_orange"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityEMPBlast>> EMP_BLAST =
            ENTITY_TYPES.register("entity_emp_blast",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityEMPBlast>of(
                                    com.hbm_m.entity.effect.EntityEMPBlast::new, MobCategory.MISC)
                            .fireImmune().sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_emp_blast"));

    /** R6c: 1:1 EntityTNTPrimedBase ("entity_ntm_tnt_primed"). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.item.EntityTNTPrimedBase>> TNT_PRIMED_BASE =
            ENTITY_TYPES.register("entity_ntm_tnt_primed",
                    () -> EntityType.Builder.<com.hbm_m.entity.item.EntityTNTPrimedBase>of(
                                    com.hbm_m.entity.item.EntityTNTPrimedBase::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_ntm_tnt_primed"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.grenades.EntityDisperserCanister>> DISPERSER_CANISTER =
            ENTITY_TYPES.register("entity_disperser",
                    () -> EntityType.Builder.<com.hbm_m.entity.grenades.EntityDisperserCanister>of(
                                    com.hbm_m.entity.grenades.EntityDisperserCanister::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1)
                            .build("entity_disperser"));
    /** Original EntityBoatRubber. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.item.EntityBoatRubber>> BOAT_RUBBER =
            ENTITY_TYPES.register("boat_rubber",
                    () -> EntityType.Builder.<com.hbm_m.entity.item.EntityBoatRubber>of(
                                    com.hbm_m.entity.item.EntityBoatRubber::new, MobCategory.MISC)
                            .sized(1.5F, 0.6F).clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("boat_rubber"));
    /** Original EntityMeteor (entity_meteor, Reichweite 250). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityMeteor>> METEOR =
            ENTITY_TYPES.register("entity_meteor",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityMeteor>of(
                                    com.hbm_m.entity.projectile.EntityMeteor::new, MobCategory.MISC)
                            .sized(4.0F, 4.0F).clientTrackingRange(16).updateInterval(1).fireImmune()
                            .build("entity_meteor"));
    /** Original EntityBoxcar (entity_boxcar, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBoxcar>> BOXCAR =
            ENTITY_TYPES.register("entity_boxcar",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBoxcar>of(
                                    com.hbm_m.entity.projectile.EntityBoxcar::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1).fireImmune()
                            .build("entity_boxcar"));
    /** Original EntityDeathBlast (Orbitaler Todesstrahl). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntityDeathBlast>> DEATH_BLAST =
            ENTITY_TYPES.register("entity_laser_blast",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntityDeathBlast>of(
                                    com.hbm_m.entity.logic.EntityDeathBlast::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_laser_blast"));
    /** Original EntityOrbitalLaser. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntityOrbitalLaser>> ORBITAL_LASER =
            ENTITY_TYPES.register("entity_orbital_laser",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntityOrbitalLaser>of(
                                    com.hbm_m.entity.logic.EntityOrbitalLaser::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_orbital_laser"));
    /** Original EntityBullet (entity_bullet, Reichweite 250): altes Geschoss fuer Hubschrauber und Cyberkrabbe. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBullet>> BULLET =
            ENTITY_TYPES.register("entity_bullet",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBullet>of(
                                    com.hbm_m.entity.projectile.EntityBullet::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1)
                            .build("entity_bullet"));
    /** Original EntityChopperMine (entity_chopper_mine): Kasten 12x12. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityChopperMine>> CHOPPER_MINE =
            ENTITY_TYPES.register("entity_chopper_mine",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityChopperMine>of(
                                    com.hbm_m.entity.projectile.EntityChopperMine::new, MobCategory.MISC)
                            .sized(12F, 12F).fireImmune().clientTrackingRange(63).updateInterval(1)
                            .build("entity_chopper_mine"));
    /** Original EntityHunterChopper (entity_mob_hunter_chopper): 8.25x3. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityHunterChopper>> HUNTER_CHOPPER =
            ENTITY_TYPES.register("entity_mob_hunter_chopper",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityHunterChopper>of(
                                    com.hbm_m.entity.mob.EntityHunterChopper::new, MobCategory.MONSTER)
                            .sized(8.25F, 3.0F).fireImmune().clientTrackingRange(16).updateInterval(3)
                            .build("entity_mob_hunter_chopper"));
    /** Original EntityCyberCrab/TeslaCrab/TaintCrab. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityCyberCrab>> CYBER_CRAB =
            ENTITY_TYPES.register("entity_cyber_crab",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityCyberCrab>of(
                                    com.hbm_m.entity.mob.EntityCyberCrab::new, MobCategory.MONSTER)
                            .sized(0.75F, 0.35F).clientTrackingRange(10)
                            .build("entity_cyber_crab"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityTeslaCrab>> TESLA_CRAB =
            ENTITY_TYPES.register("entity_tesla_crab",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityTeslaCrab>of(
                                    com.hbm_m.entity.mob.EntityTeslaCrab::new, MobCategory.MONSTER)
                            .sized(0.75F, 1.25F).clientTrackingRange(10)
                            .build("entity_tesla_crab"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityTaintCrab>> TAINT_CRAB =
            ENTITY_TYPES.register("entity_taint_crab",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityTaintCrab>of(
                                    com.hbm_m.entity.mob.EntityTaintCrab::new, MobCategory.MONSTER)
                            .sized(1.25F, 1.25F).clientTrackingRange(10)
                            .build("entity_taint_crab"));
    /** Original EntityQuackos/Pigeon/PlasticBag/Dummy/Ghost/BlockSpider. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityQuackos>> QUACKOS =
            ENTITY_TYPES.register("entity_elder_one",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityQuackos>of(
                                    com.hbm_m.entity.mob.EntityQuackos::new, MobCategory.CREATURE)
                            .sized(7.5F, 17.5F).clientTrackingRange(10)
                            .build("entity_elder_one"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityPigeon>> PIGEON =
            ENTITY_TYPES.register("entity_pigeon",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityPigeon>of(
                                    com.hbm_m.entity.mob.EntityPigeon::new, MobCategory.CREATURE)
                            .sized(0.5F, 1.0F).clientTrackingRange(10)
                            .build("entity_pigeon"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityPlasticBag>> PLASTIC_BAG =
            ENTITY_TYPES.register("entity_plastic_bag",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityPlasticBag>of(
                                    com.hbm_m.entity.mob.EntityPlasticBag::new, MobCategory.WATER_CREATURE)
                            .sized(0.45F, 0.45F).clientTrackingRange(10)
                            .build("entity_plastic_bag"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityDummy>> TEST_DUMMY =
            ENTITY_TYPES.register("entity_ntm_test_dummy",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityDummy>of(
                                    com.hbm_m.entity.mob.EntityDummy::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F).clientTrackingRange(10)
                            .build("entity_ntm_test_dummy"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityGhost>> GHOST =
            ENTITY_TYPES.register("entity_ntm_ghost",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityGhost>of(
                                    com.hbm_m.entity.mob.EntityGhost::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F).clientTrackingRange(64)
                            .updateInterval(1).build("entity_ntm_ghost"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityBlockSpider>> BLOCK_SPIDER =
            ENTITY_TYPES.register("entity_taintcrawler",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityBlockSpider>of(
                                    com.hbm_m.entity.mob.EntityBlockSpider::new, MobCategory.MONSTER)
                            .sized(0.95F, 1.25F).clientTrackingRange(64)
                            .updateInterval(1).build("entity_taintcrawler"));
    /** Original EntityFBI/EntityFBIDrone (FBI-Razzia). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityFBI>> FBI =
            ENTITY_TYPES.register("entity_ntm_fbi",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityFBI>of(
                                    com.hbm_m.entity.mob.EntityFBI::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(10)
                            .build("entity_ntm_fbi"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityFBIDrone>> FBI_DRONE =
            ENTITY_TYPES.register("entity_ntm_fbi_drone",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityFBIDrone>of(
                                    com.hbm_m.entity.mob.EntityFBIDrone::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).clientTrackingRange(16)
                            .build("entity_ntm_fbi_drone"));
    /** Original EntityDuck (entity_fucc_a_ducc). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityDuck>> DUCK =
            ENTITY_TYPES.register("entity_fucc_a_ducc",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityDuck>of(
                                    com.hbm_m.entity.mob.EntityDuck::new, MobCategory.CREATURE)
                            .sized(0.4F, 0.7F).clientTrackingRange(10)
                            .build("entity_fucc_a_ducc"));
    /** Original NTM-Loren (entity_ntm_cart_*). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.cart.EntityMinecartCrate>> CART_CRATE =
            ENTITY_TYPES.register("entity_ntm_cart_crate",
                    () -> EntityType.Builder.<com.hbm_m.entity.cart.EntityMinecartCrate>of(
                                    com.hbm_m.entity.cart.EntityMinecartCrate::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F).clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cart_crate"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.cart.EntityMinecartDestroyer>> CART_DESTROYER =
            ENTITY_TYPES.register("entity_ntm_cart_destroyer",
                    () -> EntityType.Builder.<com.hbm_m.entity.cart.EntityMinecartDestroyer>of(
                                    com.hbm_m.entity.cart.EntityMinecartDestroyer::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F).clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cart_destroyer"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.cart.EntityMinecartOre>> CART_ORE =
            ENTITY_TYPES.register("entity_ntm_cart_ore",
                    () -> EntityType.Builder.<com.hbm_m.entity.cart.EntityMinecartOre>of(
                                    com.hbm_m.entity.cart.EntityMinecartOre::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F).clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cart_ore"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.cart.EntityMinecartPowder>> CART_POWDER =
            ENTITY_TYPES.register("entity_ntm_cart_powder",
                    () -> EntityType.Builder.<com.hbm_m.entity.cart.EntityMinecartPowder>of(
                                    com.hbm_m.entity.cart.EntityMinecartPowder::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F).clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cart_powder"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.cart.EntityMinecartSemtex>> CART_SEMTEX =
            ENTITY_TYPES.register("entity_ntm_cart_semtex",
                    () -> EntityType.Builder.<com.hbm_m.entity.cart.EntityMinecartSemtex>of(
                                    com.hbm_m.entity.cart.EntityMinecartSemtex::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F).clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cart_semtex"));
    /** Original EntityMinecartTest (entity_minecart_test, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.cart.EntityMinecartTest>> MINECART_TEST =
            ENTITY_TYPES.register("entity_minecart_test",
                    () -> EntityType.Builder.<com.hbm_m.entity.cart.EntityMinecartTest>of(
                                    com.hbm_m.entity.cart.EntityMinecartTest::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F).clientTrackingRange(64)
                            .updateInterval(1).build("entity_minecart_test"));
    /** Zugsystem (Original entity_ntm_seat_dummy/bounding_dummy/cargo_tram/cargo_tram_trailer, Reichweite 250). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.train.SeatDummyEntity>> TRAIN_SEAT_DUMMY =
            ENTITY_TYPES.register("entity_ntm_seat_dummy",
                    () -> EntityType.Builder.<com.hbm_m.entity.train.SeatDummyEntity>of(
                                    com.hbm_m.entity.train.SeatDummyEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.1F).clientTrackingRange(16).updateInterval(1).noSummon()
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_seat_dummy"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.train.BoundingBoxDummyEntity>> TRAIN_BOUNDING_DUMMY =
            ENTITY_TYPES.register("entity_ntm_bounding_dummy",
                    () -> EntityType.Builder.<com.hbm_m.entity.train.BoundingBoxDummyEntity>of(
                                    com.hbm_m.entity.train.BoundingBoxDummyEntity::new, MobCategory.MISC)
                            .sized(1F, 1F).clientTrackingRange(16).updateInterval(1).noSummon()
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_bounding_dummy"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.train.TrainCargoTram>> TRAIN_CARGO_TRAM =
            ENTITY_TYPES.register("entity_ntm_cargo_tram",
                    () -> EntityType.Builder.<com.hbm_m.entity.train.TrainCargoTram>of(
                                    com.hbm_m.entity.train.TrainCargoTram::new, MobCategory.MISC)
                            .sized(5F, 2F).clientTrackingRange(16).updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cargo_tram"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.train.TrainCargoTramTrailer>> TRAIN_CARGO_TRAM_TRAILER =
            ENTITY_TYPES.register("entity_ntm_cargo_tram_trailer",
                    () -> EntityType.Builder.<com.hbm_m.entity.train.TrainCargoTramTrailer>of(
                                    com.hbm_m.entity.train.TrainCargoTramTrailer::new, MobCategory.MISC)
                            .sized(5F, 2F).clientTrackingRange(16).updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_ntm_cargo_tram_trailer"));
    /** Original EntityCloudTom (entity_moonstone_blast). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityCloudTom>> CLOUD_TOM =
            ENTITY_TYPES.register("entity_moonstone_blast",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityCloudTom>of(
                                    com.hbm_m.entity.effect.EntityCloudTom::new, MobCategory.MISC)
                            .sized(20F, 40F).fireImmune().clientTrackingRange(512).updateInterval(1)
                            .build("entity_moonstone_blast"));
    /** Original EntityBurningFOEQ (entity_burning_foeq). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBurningFOEQ>> BURNING_FOEQ =
            ENTITY_TYPES.register("entity_burning_foeq",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBurningFOEQ>of(
                                    com.hbm_m.entity.projectile.EntityBurningFOEQ::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_burning_foeq"));
    /** Original EntityFallingNuke (entity_falling_bomb). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityFallingNuke>> FALLING_NUKE =
            ENTITY_TYPES.register("entity_falling_bomb",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityFallingNuke>of(
                                    com.hbm_m.entity.projectile.EntityFallingNuke::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_falling_bomb"));
    /** Original EntityCloudFleijaRainbow (entity_cloud_rainbow). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityCloudFleijaRainbow>> CLOUD_FLEIJA_RAINBOW =
            ENTITY_TYPES.register("entity_cloud_rainbow",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityCloudFleijaRainbow>of(
                                    com.hbm_m.entity.effect.EntityCloudFleijaRainbow::new, MobCategory.MISC)
                            .sized(20F, 40F).fireImmune().clientTrackingRange(512).updateInterval(1)
                            .build("entity_cloud_rainbow"));
    /** Original EntityBulletBaseNT (entity_bullet_mk3, Reichweite 250): Altsystem der NPC-Geschosse. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBulletBaseNT>> BULLET_BASE_NT =
            ENTITY_TYPES.register("entity_bullet_mk3",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBulletBaseNT>of(
                                    com.hbm_m.entity.projectile.EntityBulletBaseNT::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_bullet_mk3"));
    /** Original EntityB92Beam (entity_beam_bomb, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityB92Beam>> B92_BEAM =
            ENTITY_TYPES.register("entity_beam_bomb",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityB92Beam>of(
                                    com.hbm_m.entity.projectile.EntityB92Beam::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_beam_bomb"));
    /** Original EntityBobmazon (entity_bobmazon_delivery, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.missile.EntityBobmazon>> BOBMAZON =
            ENTITY_TYPES.register("entity_bobmazon_delivery",
                    () -> EntityType.Builder.<com.hbm_m.entity.missile.EntityBobmazon>of(
                                    com.hbm_m.entity.missile.EntityBobmazon::new, MobCategory.MISC)
                            .sized(1.0F, 3.0F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_bobmazon_delivery"));
    /** Original EntityMinerRocket (entity_miner_lander). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.missile.EntityMinerRocket>> MINER_ROCKET =
            ENTITY_TYPES.register("entity_miner_lander",
                    () -> EntityType.Builder.<com.hbm_m.entity.missile.EntityMinerRocket>of(
                                    com.hbm_m.entity.missile.EntityMinerRocket::new, MobCategory.MISC)
                            .sized(1.0F, 3.0F).clientTrackingRange(63).updateInterval(1)
                            .build("entity_miner_lander"));
    /** Original EntityItemBuoyant. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.item.EntityItemBuoyant>> ITEM_BUOYANT =
            ENTITY_TYPES.register("item_buoyant",
                    () -> EntityType.Builder.<com.hbm_m.entity.item.EntityItemBuoyant>of(
                                    com.hbm_m.entity.item.EntityItemBuoyant::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(7).updateInterval(1)
                            .build("item_buoyant"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.QuasarEntity>> DIGAMMA_QUASAR =
            ENTITY_TYPES.register("digamma_quasar",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.QuasarEntity>of(
                                    com.hbm_m.entity.effect.QuasarEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .fireImmune().build("digamma_quasar"));

    /** The digamma lance dropped on a reactor that melted down carrying digamma fuel. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.SpearEntity>> DIGAMMA_SPEAR =
            ENTITY_TYPES.register("digamma_spear",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.SpearEntity>of(
                                    com.hbm_m.entity.effect.SpearEntity::new, MobCategory.MISC)
                            .sized(2.0F, 10.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .fireImmune()
                            .build("digamma_spear"));

    /** Tracking range in chunks; server multiplies by 16 for blocks (see ChunkMap.TrackedEntity). */
    private static final int MISSILE_TRACKING_CHUNKS = 512;

    private static <T extends MissileBaseEntity> EntityType.Builder<T> missileBuilder(EntityType.EntityFactory<T> factory) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(1.5F, 1.5F)
                .clientTrackingRange(MISSILE_TRACKING_CHUNKS)
                .updateInterval(1);
    }

    // Длительный Fleija-взрыв MK3 (шрабидиевая ракета, анти-шрабидиевые ячейки)
    public static final RegistrySupplier<EntityType<EntityNukeExplosionMK3>> NUKE_MK3 =
            ENTITY_TYPES.register("nuke_mk3",
                    () -> EntityType.Builder.<EntityNukeExplosionMK3>of(EntityNukeExplosionMK3::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("nuke_mk3"));

    // Длительный взрыв солиния («синяя стирка»)
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntitySoliniumExplosion>> SOLINIUM_EXPLOSION =
            ENTITY_TYPES.register("solinium_explosion",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntitySoliniumExplosion>of(
                                    com.hbm_m.entity.logic.EntitySoliniumExplosion::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("solinium_explosion"));

    // Длительный взрыв бейлфайра
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntityBalefireExplosion>> BALEFIRE_EXPLOSION =
            ENTITY_TYPES.register("balefire_explosion",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntityBalefireExplosion>of(
                                    com.hbm_m.entity.logic.EntityBalefireExplosion::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("balefire_explosion"));

    // Gerald/Horizons orbital strike meteor (see com.hbm_m.satellite.SatelliteHorizons)
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.TomEntity>> TOM_METEOR =
            ENTITY_TYPES.register("tom_meteor",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.TomEntity>of(com.hbm_m.entity.projectile.TomEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(512)
                            .updateInterval(1)
                            .build("tom_meteor"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.TomBlastEntity>> TOM_BLAST =
            ENTITY_TYPES.register("tom_blast",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.TomBlastEntity>of(com.hbm_m.entity.logic.TomBlastEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("tom_blast"));

    // Длительная сущность ядерного взрыва MK5 (Fat Man и другие мощные боеприпасы)
    public static final RegistrySupplier<EntityType<EntityNukeExplosionMK5>> NUKE_MK5 =
            ENTITY_TYPES.register("nuke_mk5",
                    () -> EntityType.Builder.<EntityNukeExplosionMK5>of(EntityNukeExplosionMK5::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("nuke_mk5"));

    public static final RegistrySupplier<EntityType<EntityCloudFleija>> CLOUD_FLEIJA =
            ENTITY_TYPES.register("cloud_fleija",
                    () -> EntityType.Builder.<EntityCloudFleija>of(EntityCloudFleija::new, MobCategory.MISC)
                            .sized(20.0F, 40.0F)
                            .clientTrackingRange(512)
                            .updateInterval(1)
                            .build("cloud_fleija"));

    /** Original EntityCloudSolinium (dort versehentlich ebenfalls "entity_cloud_rainbow"), Reichweite 1000 Bloecke. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityCloudSolinium>> CLOUD_SOLINIUM =
            ENTITY_TYPES.register("entity_cloud_solinium",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityCloudSolinium>of(com.hbm_m.entity.effect.EntityCloudSolinium::new, MobCategory.MISC)
                            .sized(20.0F, 40.0F)
                            .fireImmune()
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("entity_cloud_solinium"));

    public static final RegistrySupplier<EntityType<EntityFalloutRain>> NUKE_FALLOUT_RAIN =
            ENTITY_TYPES.register("nuke_fallout_rain",
                    () -> EntityType.Builder.<EntityFalloutRain>of(EntityFalloutRain::new, MobCategory.MISC)
                            .sized(4.0F, 20.0F)
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .fireImmune().build("nuke_fallout_rain"));

    public static final RegistrySupplier<EntityType<FallingBlockEntity>> FALLING_SELLAFIT_ENTITY_TYPE = ENTITY_TYPES.register("falling_sellafit",
            () -> EntityType.Builder.<FallingBlockEntity>of(FallingBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F) // Размеры сущности, обычно для блока 1x1
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("falling_sellafit"));

    public static final RegistrySupplier<EntityType<NoloEntity>> NOLO = ENTITY_TYPES.register("nolo",
            () -> EntityType.Builder.of(NoloEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(10)
                    .build("nolo"));

    /** 1:1 port of {@code EntityBomber}: the plane a bomb caller brings in. */
    /** Original EntityC130 (entity_c130): 8x4, Tracking 1000. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntityC130>> C130 =
            ENTITY_TYPES.register("entity_c130",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntityC130>of(
                                    com.hbm_m.entity.logic.EntityC130::new, MobCategory.MISC)
                            .sized(8.0F, 4.0F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_c130"));
    /** Original EntityParachuteCrate (entity_parachute_crate). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.item.EntityParachuteCrate>> PARACHUTE_CRATE =
            ENTITY_TYPES.register("entity_parachute_crate",
                    () -> EntityType.Builder.<com.hbm_m.entity.item.EntityParachuteCrate>of(
                                    com.hbm_m.entity.item.EntityParachuteCrate::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(64).updateInterval(1)
                            .build("entity_parachute_crate"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntityBomber>> BOMBER =
            ENTITY_TYPES.register("bomber",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntityBomber>of(
                                    com.hbm_m.entity.logic.EntityBomber::new, MobCategory.MISC)
                            .sized(8.0F, 4.0F)
                            
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("bomber"));

    /** 1:1 port of {@code EntityBombletZeta}: the bomber's free-falling payload. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBombletZeta>> BOMBLET_ZETA =
            ENTITY_TYPES.register("bomblet_zeta",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBombletZeta>of(
                                    com.hbm_m.entity.projectile.EntityBombletZeta::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("bomblet_zeta"));

    /** 1:1 port of {@code EntityUFO}: 15x4 flying fortress, 20000 HP. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityUFO>> UFO =
            ENTITY_TYPES.register("ufo",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityUFO>of(
                                    com.hbm_m.entity.mob.EntityUFO::new, MobCategory.MONSTER)
                            .sized(15.0F, 4.0F)
                            .fireImmune()
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("ufo"));

    /** 1:1 port of {@code EntityBOTPrimeHead}: the 15000 HP worm boss. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.botprime.EntityBOTPrimeHead>> BOT_PRIME_HEAD =
            ENTITY_TYPES.register("bot_prime_head",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.botprime.EntityBOTPrimeHead>of(
                                    com.hbm_m.entity.mob.botprime.EntityBOTPrimeHead::new, MobCategory.MONSTER)
                            .sized(3.0F, 3.0F)
                            .fireImmune()
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("bot_prime_head"));

    /** One of the head's 74 trailing segments; never spawned on its own. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.botprime.EntityBOTPrimeBody>> BOT_PRIME_BODY =
            ENTITY_TYPES.register("bot_prime_body",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.botprime.EntityBOTPrimeBody>of(
                                    com.hbm_m.entity.mob.botprime.EntityBOTPrimeBody::new, MobCategory.MONSTER)
                            .sized(2.0F, 2.0F)
                            .fireImmune()
                            .noSummon()
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("bot_prime_body"));

    /** 1:1 port of {@code EntityRADBeast}: 2x2 blaze-alike, leader variant at 360 HP. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityRADBeast>> RAD_BEAST =
            ENTITY_TYPES.register("rad_beast",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityRADBeast>of(
                                    com.hbm_m.entity.mob.EntityRADBeast::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .fireImmune()
                            .clientTrackingRange(8)
                            .build("rad_beast"));


    // ═══ Glyphiden (1:1 EntityMappings.addMob) ═══
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphid>> GLYPHID =
            ENTITY_TYPES.register("glyphid",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphid>of(com.hbm_m.entity.mob.glyphid.EntityGlyphid::new, MobCategory.MONSTER)
                            .sized(1.75F, 1.0F)
                            .clientTrackingRange(10)
                            .build("glyphid"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler>> GLYPHID_BRAWLER =
            ENTITY_TYPES.register("glyphid_brawler",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler::new, MobCategory.MONSTER)
                            .sized(2.0F, 1.125F)
                            .clientTrackingRange(10)
                            .build("glyphid_brawler"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth>> GLYPHID_BEHEMOTH =
            ENTITY_TYPES.register("glyphid_behemoth",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth::new, MobCategory.MONSTER)
                            .sized(2.5F, 1.5F)
                            .clientTrackingRange(10)
                            .build("glyphid_behemoth"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidBrenda>> GLYPHID_BRENDA =
            ENTITY_TYPES.register("glyphid_brenda",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidBrenda>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidBrenda::new, MobCategory.MONSTER)
                            .sized(2.5F, 1.75F).fireImmune()
                            .clientTrackingRange(10)
                            .build("glyphid_brenda"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidBombardier>> GLYPHID_BOMBARDIER =
            ENTITY_TYPES.register("glyphid_bombardier",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidBombardier>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidBombardier::new, MobCategory.MONSTER)
                            .sized(1.75F, 1.0F)
                            .clientTrackingRange(10)
                            .build("glyphid_bombardier"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster>> GLYPHID_BLASTER =
            ENTITY_TYPES.register("glyphid_blaster",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster::new, MobCategory.MONSTER)
                            .sized(2.0F, 1.125F)
                            .clientTrackingRange(10)
                            .build("glyphid_blaster"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidScout>> GLYPHID_SCOUT =
            ENTITY_TYPES.register("glyphid_scout",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidScout>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidScout::new, MobCategory.MONSTER)
                            .sized(1.25F, 0.75F)
                            .clientTrackingRange(10)
                            .build("glyphid_scout"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear>> GLYPHID_NUCLEAR =
            ENTITY_TYPES.register("glyphid_nuclear",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear::new, MobCategory.MONSTER)
                            .sized(2.5F, 1.75F).fireImmune()
                            .clientTrackingRange(10)
                            .build("glyphid_nuclear"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger>> GLYPHID_DIGGER =
            ENTITY_TYPES.register("glyphid_digger",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger>of(com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger::new, MobCategory.MONSTER)
                            .sized(1.75F, 1.0F)
                            .clientTrackingRange(10)
                            .build("glyphid_digger"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityParasiteMaggot>> PARASITE_MAGGOT =
            ENTITY_TYPES.register("parasite_maggot",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityParasiteMaggot>of(com.hbm_m.entity.mob.EntityParasiteMaggot::new, MobCategory.MONSTER)
                            .sized(0.3F, 0.7F)
                            .clientTrackingRange(10)
                            .build("parasite_maggot"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EntityWaypoint>> WAYPOINT =
            ENTITY_TYPES.register("waypoint",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EntityWaypoint>of(com.hbm_m.entity.logic.EntityWaypoint::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).fireImmune().noSummon()
                            .clientTrackingRange(16).updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("waypoint"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityAcidBomb>> ACID_BOMB =
            ENTITY_TYPES.register("acid_bomb",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityAcidBomb>of(com.hbm_m.entity.projectile.EntityAcidBomb::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64).updateInterval(1)
                            .build("acid_bomb"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityChemical>> CHEMICAL =
            ENTITY_TYPES.register("chemthrower_splash",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityChemical>of(com.hbm_m.entity.projectile.EntityChemical::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).fireImmune()
                            .clientTrackingRange(64).updateInterval(1)
                            .build("chemthrower_splash"));

    /** 1:1 port of {@code EntityMaskMan}: 2x5 boss, 1000 HP. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityMaskMan>> MASKMAN =
            ENTITY_TYPES.register("maskman",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityMaskMan>of(
                                    com.hbm_m.entity.mob.EntityMaskMan::new, MobCategory.MONSTER)
                            .sized(2.0F, 5.0F)
                            .fireImmune()
                            .clientTrackingRange(16)
                            .build("maskman"));

    public static final RegistrySupplier<EntityType<EntityCreeperTainted>> ENTITY_MOB_TAINTED_CREEPER =
            ENTITY_TYPES.register("entity_mob_tainted_creeper",
                    () -> EntityType.Builder.of(EntityCreeperTainted::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(8)
                            .build("entity_mob_tainted_creeper"));

    public static final RegistrySupplier<EntityType<EntityCreeperVolatile>> ENTITY_MOB_VOLATILE_CREEPER =
            ENTITY_TYPES.register("entity_mob_volatile_creeper",
                    () -> EntityType.Builder.of(EntityCreeperVolatile::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(8)
                            .build("entity_mob_volatile_creeper"));

    public static final RegistrySupplier<EntityType<EntityCreeperGold>> ENTITY_MOB_GOLD_CREEPER =
            ENTITY_TYPES.register("entity_mob_gold_creeper",
                    () -> EntityType.Builder.of(EntityCreeperGold::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(8)
                            .build("entity_mob_gold_creeper"));

    public static final RegistrySupplier<EntityType<EntityCreeperNuclear>> ENTITY_MOB_NUCLEAR_CREEPER =
            ENTITY_TYPES.register("entity_mob_nuclear_creeper",
                    () -> EntityType.Builder.of(EntityCreeperNuclear::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(8)
                            .build("entity_mob_nuclear_creeper"));

    public static final RegistrySupplier<EntityType<EntityCreeperPhosgene>> ENTITY_MOB_PHOSGENE_CREEPER =
            ENTITY_TYPES.register("entity_mob_phosgene_creeper",
                    () -> EntityType.Builder.of(EntityCreeperPhosgene::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(8)
                            .build("entity_mob_phosgene_creeper"));

    public static final RegistrySupplier<EntityType<EntityMist>> ENTITY_MIST =
            ENTITY_TYPES.register("entity_mist",
                    () -> EntityType.Builder.<EntityMist>of(EntityMist::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("entity_mist"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.conveyor.MovingConveyorItemEntity>> MOVING_CONVEYOR_ITEM =
            ENTITY_TYPES.register("moving_conveyor_item",
                    () -> EntityType.Builder.<com.hbm_m.entity.conveyor.MovingConveyorItemEntity>of(
                                    com.hbm_m.entity.conveyor.MovingConveyorItemEntity::new, MobCategory.MISC)
                            .sized(0.375F, 0.375F)
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("moving_conveyor_item"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.conveyor.MovingConveyorPackageEntity>> MOVING_CONVEYOR_PACKAGE =
            ENTITY_TYPES.register("moving_conveyor_package",
                    () -> EntityType.Builder.<com.hbm_m.entity.conveyor.MovingConveyorPackageEntity>of(
                                    com.hbm_m.entity.conveyor.MovingConveyorPackageEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("moving_conveyor_package"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.drone.EntityDeliveryDrone>> DELIVERY_DRONE =
            ENTITY_TYPES.register("delivery_drone",
                    () -> EntityType.Builder.<com.hbm_m.entity.drone.EntityDeliveryDrone>of(
                                    com.hbm_m.entity.drone.EntityDeliveryDrone::new, MobCategory.MISC)
                            .sized(0.75F, 0.75F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("delivery_drone"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.drone.EntityRequestDrone>> REQUEST_DRONE =
            ENTITY_TYPES.register("request_drone",
                    () -> EntityType.Builder.<com.hbm_m.entity.drone.EntityRequestDrone>of(
                                    com.hbm_m.entity.drone.EntityRequestDrone::new, MobCategory.MISC)
                            .sized(0.75F, 0.75F)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            //? if forge || neoforge {
                            .setShouldReceiveVelocityUpdates(false)
                            //?}
                            .build("request_drone"));

    public static final RegistrySupplier<EntityType<ZirnoxDebrisEntity>> ZIRNOX_DEBRIS =
            ENTITY_TYPES.register("zirnox_debris",
                    () -> EntityType.Builder.<ZirnoxDebrisEntity>of(ZirnoxDebrisEntity::new, MobCategory.MISC)
                            .sized(0.75F, 0.5F)
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("zirnox_debris"));

    /** 1:1-Port von {@code EntityCog}: das Zahnrad aus einem geplatzten Stirlingmotor. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.CogEntity>> COG =
            ENTITY_TYPES.register("cog",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.CogEntity>of(
                                    com.hbm_m.entity.projectile.CogEntity::new, MobCategory.MISC)
                            // Original: setSize(1F, 1F).
                            .sized(1F, 1F)
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("cog"));

    /** 1:1-Port von {@code EntitySawblade}: das Blatt aus einem ueberdrehten Saegewerk. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.SawbladeEntity>> SAWBLADE =
            ENTITY_TYPES.register("sawblade",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.SawbladeEntity>of(
                                    com.hbm_m.entity.projectile.SawbladeEntity::new, MobCategory.MISC)
                            .sized(1F, 1F)
                            .clientTrackingRange(63)
                            .updateInterval(1)
                            .build("sawblade"));

    // pile_debris: Original-SEDNA-BulletConfig PileCoreBlockEntity.pile_debris (EntityBulletBaseMK4), keine eigene Entity.

    /** Original EntityTorpedo (entity_torpedo, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityTorpedo>> TORPEDO =
            ENTITY_TYPES.register("entity_torpedo",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityTorpedo>of(
                                    com.hbm_m.entity.projectile.EntityTorpedo::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1).fireImmune()
                            .build("entity_torpedo"));
    /** Original EntityDuchessGambit (entity_duchessgambit, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityDuchessGambit>> DUCHESS_GAMBIT =
            ENTITY_TYPES.register("entity_duchessgambit",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityDuchessGambit>of(
                                    com.hbm_m.entity.projectile.EntityDuchessGambit::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1).fireImmune()
                            .build("entity_duchessgambit"));
    /** Original EntityBuilding (entity_falling_building, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntityBuilding>> BUILDING =
            ENTITY_TYPES.register("entity_falling_building",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntityBuilding>of(
                                    com.hbm_m.entity.projectile.EntityBuilding::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1).fireImmune()
                            .build("entity_falling_building"));
    /** Original EntityFogFX (entity_nuclear_fog, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.EntityFogFX>> FOG_FX =
            ENTITY_TYPES.register("entity_nuclear_fog",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.EntityFogFX>of(
                                    com.hbm_m.entity.effect.EntityFogFX::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_nuclear_fog"));
    /** Original EntityWastePearl (entity_waste_pearl, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.grenades.EntityWastePearl>> WASTE_PEARL =
            ENTITY_TYPES.register("entity_waste_pearl",
                    () -> EntityType.Builder.<com.hbm_m.entity.grenades.EntityWastePearl>of(
                                    com.hbm_m.entity.grenades.EntityWastePearl::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_waste_pearl"));
    /** Original EntitySiegeLaser (entity_ntm_siege_laser, Reichweite 1000). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.projectile.EntitySiegeLaser>> SIEGE_LASER =
            ENTITY_TYPES.register("entity_ntm_siege_laser",
                    () -> EntityType.Builder.<com.hbm_m.entity.projectile.EntitySiegeLaser>of(
                                    com.hbm_m.entity.projectile.EntitySiegeLaser::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1)
                            .build("entity_ntm_siege_laser"));
    /** Original EntityUndeadSoldier (addMob entity_ntm_undead_soldier: Reichweite 80, Intervall 3). */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.EntityUndeadSoldier>> UNDEAD_SOLDIER =
            ENTITY_TYPES.register("entity_ntm_undead_soldier",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.EntityUndeadSoldier>of(
                                    com.hbm_m.entity.mob.EntityUndeadSoldier::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).clientTrackingRange(5).updateInterval(3)
                            .build("entity_ntm_undead_soldier"));

    /** 1:1 EntitySiegeCraft / EntitySiegeTunneler - im Original ohne Eintrag in EntityMappings, im Port per /summon. */
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.siege.EntitySiegeCraft>> SIEGE_CRAFT =
            ENTITY_TYPES.register("entity_ntm_siege_craft",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.siege.EntitySiegeCraft>of(
                                    com.hbm_m.entity.mob.siege.EntitySiegeCraft::new, MobCategory.MONSTER)
                            .sized(7.0F, 1.0F).fireImmune().clientTrackingRange(16).updateInterval(1)
                            .build("entity_ntm_siege_craft"));
    public static final RegistrySupplier<EntityType<com.hbm_m.entity.mob.siege.EntitySiegeTunneler>> SIEGE_TUNNELER =
            ENTITY_TYPES.register("entity_ntm_siege_tunneler",
                    () -> EntityType.Builder.<com.hbm_m.entity.mob.siege.EntitySiegeTunneler>of(
                                    com.hbm_m.entity.mob.siege.EntitySiegeTunneler::new, MobCategory.MONSTER)
                            .sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(1)
                            .build("entity_ntm_siege_tunneler"));

    public static void init() {
        ENTITY_TYPES.register();
        //? if fabric {
        /*net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                NOLO.get(), NoloEntity.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                ENTITY_MOB_TAINTED_CREEPER.get(), EntityCreeperTainted.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                ENTITY_MOB_VOLATILE_CREEPER.get(), EntityCreeperVolatile.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                ENTITY_MOB_PHOSGENE_CREEPER.get(), EntityCreeperPhosgene.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                ENTITY_MOB_GOLD_CREEPER.get(), EntityCreeperGold.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                ENTITY_MOB_NUCLEAR_CREEPER.get(), EntityCreeperNuclear.createAttributes());
        net.minecraft.world.entity.SpawnRestriction.register(
                NOLO.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoloEntity::checkNoloSpawnRules);
        *///?}
    }
}
