package com.hbm_m.entity;

import com.hbm_m.entity.effect.EntityFalloutRain;
import com.hbm_m.entity.grenades.*;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.entity.mob.NoloEntity;
import com.hbm_m.entity.missile.MissileABMEntity;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.entity.missile.MissileTestEntity;
import com.hbm_m.entity.missile.MissileShuttleEntity;
import com.hbm_m.entity.missile.MissileStealthEntity;
import com.hbm_m.entity.missile.MissileTier0;
import com.hbm_m.entity.missile.MissileTier1;
import com.hbm_m.entity.missile.MissileTier2;
import com.hbm_m.entity.missile.MissileTier3;
import com.hbm_m.entity.missile.MissileTier4;
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
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(256)
                            .updateInterval(3)
                            .build("missile_test"));

    public static final RegistrySupplier<EntityType<MissileABMEntity>> MISSILE_ABM =
            ENTITY_TYPES.register("missile_abm",
                    () -> EntityType.Builder.<MissileABMEntity>of(MissileABMEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(256)
                            .updateInterval(3)
                            .build("missile_abm"));

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
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("cluster_rocket"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.logic.EmpPulseEntity>> EMP_PULSE =
            ENTITY_TYPES.register("emp_pulse",
                    () -> EntityType.Builder.<com.hbm_m.entity.logic.EmpPulseEntity>of(
                                    com.hbm_m.entity.logic.EmpPulseEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(20)
                            .build("emp_pulse"));

    public static final RegistrySupplier<EntityType<com.hbm_m.entity.effect.BlackHoleEntity>> BLACK_HOLE =
            ENTITY_TYPES.register("black_hole",
                    () -> EntityType.Builder.<com.hbm_m.entity.effect.BlackHoleEntity>of(
                                    com.hbm_m.entity.effect.BlackHoleEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("black_hole"));

    /** Tracking range in chunks; server multiplies by 16 for blocks (see ChunkMap.TrackedEntity). */
    private static final int MISSILE_TRACKING_CHUNKS = 512;

    private static <T extends MissileBaseEntity> EntityType.Builder<T> missileBuilder(EntityType.EntityFactory<T> factory) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(1.5F, 1.5F)
                .clientTrackingRange(MISSILE_TRACKING_CHUNKS)
                .updateInterval(1);
    }

    // Длительная сущность ядерного взрыва MK5 (Fat Man и другие мощные боеприпасы)
    public static final RegistrySupplier<EntityType<EntityNukeExplosionMK5>> NUKE_MK5 =
            ENTITY_TYPES.register("nuke_mk5",
                    () -> EntityType.Builder.<EntityNukeExplosionMK5>of(EntityNukeExplosionMK5::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("nuke_mk5"));

    public static final RegistrySupplier<EntityType<EntityFalloutRain>> NUKE_FALLOUT_RAIN =
            ENTITY_TYPES.register("nuke_fallout_rain",
                    () -> EntityType.Builder.<EntityFalloutRain>of(EntityFalloutRain::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(3)
                            .updateInterval(2)
                            .build("nuke_fallout_rain"));

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

    public static void init() {
        ENTITY_TYPES.register();
        //? if fabric {
        /*net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                NOLO.get(), NoloEntity.createAttributes());
        net.minecraft.world.entity.SpawnRestriction.register(
                NOLO.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoloEntity::checkNoloSpawnRules);
        *///?}
    }
}
