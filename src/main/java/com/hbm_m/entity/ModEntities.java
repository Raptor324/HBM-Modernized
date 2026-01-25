package com.hbm_m.entity;

import com.hbm_m.entity.grenades.*;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MainRegistry.MOD_ID);



    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADE_PROJECTILE =
        ENTITY_TYPES.register("grenade_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenade_projectile"));

    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADEHE_PROJECTILE =
        ENTITY_TYPES.register("grenadehe_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadehe_projectile"));

    public static final RegistryObject<EntityType<AirstrikeEntity>> AIRSTRIKE_ENTITY =
            ENTITY_TYPES.register("airstrike",
                    () -> EntityType.Builder.<AirstrikeEntity>of(AirstrikeEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.0F)
                            .build("airstrike")
            );
    public static final RegistryObject<EntityType<AirstrikeNukeEntity>> AIRSTRIKE_NUKE_ENTITY =
            ENTITY_TYPES.register("airstrikenuke",
                    () -> EntityType.Builder.<AirstrikeNukeEntity>of(AirstrikeNukeEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.0F)
                            .build("airstrikenuke"));

    public static final RegistryObject<EntityType<AirstrikeAgentEntity>> AIRSTRIKE_AGENT_ENTITY =
            ENTITY_TYPES.register("airstrikeagent",
                    () -> EntityType.Builder.<AirstrikeAgentEntity>of(AirstrikeAgentEntity::new, MobCategory.MISC)
                            .sized(2.0F, 1.0F)
                            .build("airstrikeagent"));

    public static final RegistryObject<EntityType<AirBombProjectileEntity>> AIRBOMB_PROJECTILE =
            ENTITY_TYPES.register("airbomb_projectile",
                    () -> EntityType.Builder.<AirBombProjectileEntity>of(AirBombProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("airbomb_projectile"));
    public static final RegistryObject<EntityType<AirNukeBombProjectileEntity>> AIRNUKEBOMB_PROJECTILE =
            ENTITY_TYPES.register("airnukebomb_projectile",
                    () -> EntityType.Builder.<AirNukeBombProjectileEntity>of(AirNukeBombProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("airnukebomb_projectile"));
    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADEFIRE_PROJECTILE =
        ENTITY_TYPES.register("grenadefire_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadefire_projectile"));

    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADESMART_PROJECTILE =
        ENTITY_TYPES.register("grenadesmart_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadesmart_projectile"));

    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADESLIME_PROJECTILE =
        ENTITY_TYPES.register("grenadeslime_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadeslime_projectile"));

    public static final RegistryObject<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("grenade_if_projectile"));

    public static final RegistryObject<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_FIRE_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_fire_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenade_if_fire_projectile"));

    public static final RegistryObject<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_SLIME_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_slime_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("grenade_if_slime_projectile"));

    public static final RegistryObject<EntityType<GrenadeIfProjectileEntity>> GRENADE_IF_HE_PROJECTILE =
            ENTITY_TYPES.register("grenade_if_he_projectile",
                    () -> EntityType.Builder.<GrenadeIfProjectileEntity>of(GrenadeIfProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenade_if_he_projectile"));


    public static final RegistryObject<EntityType<GrenadeNucProjectileEntity>> GRENADE_NUC_PROJECTILE =
            ENTITY_TYPES.register("grenade_nuc_projectile",
                    () -> EntityType.Builder.<GrenadeNucProjectileEntity>of(GrenadeNucProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("grenade_nuc_projectile"));
    public static final RegistryObject<EntityType<FallingBlockEntity>> FALLING_SELLAFIT_ENTITY_TYPE = ENTITY_TYPES.register("falling_sellafit",
            () -> EntityType.Builder.<FallingBlockEntity>of(FallingBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F) // Размеры сущности, обычно для блока 1x1
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("falling_sellafit"));
}
