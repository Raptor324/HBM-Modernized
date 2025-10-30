package com.hbm_m.entity;

import com.hbm_m.entity.grenades.GrenadeProjectileEntity;
import com.hbm_m.main.MainRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADEIF_PROJECTILE =
        ENTITY_TYPES.register("grenadeif_projectile",
            () -> EntityType.Builder.<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .build("grenadeif_projectile"));
}
