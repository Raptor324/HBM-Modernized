package com.hbm_m.entity;

import com.hbm_m.main.MainRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// ModEntities.java
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MainRegistry.MOD_ID);

    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADE_PROJECTILE =
            ENTITY_TYPES.register("grenade_projectile",
                    () -> EntityType.Builder
                            .<GrenadeProjectileEntity>of(GrenadeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenade_projectile"));

    public static final RegistryObject<EntityType<GrenadeheProjectileEntity>> GRENADEHE_PROJECTILE =
            ENTITY_TYPES.register("grenadehe_projectile",
                    () -> EntityType.Builder
                            .<GrenadeheProjectileEntity>of(GrenadeheProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenadehe_projectile"));

    public static final RegistryObject<EntityType<GrenadefireProjectileEntity>> GRENADEFIRE_PROJECTILE =
            ENTITY_TYPES.register("grenadefire_projectile",
                    () -> EntityType.Builder
                            .<GrenadefireProjectileEntity>of(GrenadefireProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenadefire_projectile"));

    public static final RegistryObject<EntityType<GrenadesmartProjectileEntity>> GRENADESMART_PROJECTILE =
            ENTITY_TYPES.register("grenadesmart_projectile",
                    () -> EntityType.Builder
                            .<GrenadesmartProjectileEntity>of(GrenadesmartProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenadesmart_projectile"));

    public static final RegistryObject<EntityType<GrenadeslimeProjectileEntity>> GRENADESLIME_PROJECTILE =
            ENTITY_TYPES.register("grenadeslime_projectile",
                    () -> EntityType.Builder
                            .<GrenadeslimeProjectileEntity>of(GrenadeslimeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .build("grenadeslime_projectile"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}


