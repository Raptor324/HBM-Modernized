package com.hbm_m.entity;

import com.hbm_m.entity.mob.EntityCreeperGold;
import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.entity.mob.EntityCreeperPhosgene;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.entity.mob.EntityCreeperVolatile;
import com.hbm_m.entity.mob.NoloEntity;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.Supplier;

/**
 * Единая (без loader-дублирования) регистрация атрибутов и спавн-плейсментов мобов
 * через Architectury {@link EntityAttributeRegistry} / {@link SpawnPlacementsRegistry}.
 * Обе реализации буферизуют записи и применяют их на соответствующем событии шины мода,
 * поэтому вызов из общего init безопасен на Forge и NeoForge.
 */
public final class ModEntityEvents {

    public static void init() {
        // --- Атрибуты ---
        EntityAttributeRegistry.register(ModEntities.NOLO, NoloEntity::createAttributes);
        EntityAttributeRegistry.register(ModEntities.UFO, com.hbm_m.entity.mob.EntityUFO::createAttributes);
        EntityAttributeRegistry.register(ModEntities.BOT_PRIME_HEAD, com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase::createAttributes);
        EntityAttributeRegistry.register(ModEntities.BOT_PRIME_BODY, com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase::createAttributes);
        EntityAttributeRegistry.register(ModEntities.RAD_BEAST, com.hbm_m.entity.mob.EntityRADBeast::createAttributes);
        EntityAttributeRegistry.register(ModEntities.MASKMAN, com.hbm_m.entity.mob.EntityMaskMan::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_TAINTED_CREEPER, EntityCreeperTainted::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, EntityCreeperVolatile::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_GOLD_CREEPER, EntityCreeperGold::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER, EntityCreeperNuclear::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, EntityCreeperPhosgene::createAttributes);

        // --- Спавн-плейсменты (в оригинале везде Operation.REPLACE; у кастомных мобов
        // предзарегистрированных placements нет, поэтому семантика эквивалентна) ---
        spawnPlacement(ModEntities.NOLO, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NoloEntity::checkNoloSpawnRules);
        spawnPlacement(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCreeperVolatile::checkVolatileSpawnRules);
        spawnPlacement(ModEntities.ENTITY_MOB_GOLD_CREEPER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCreeperGold::checkGoldSpawnRules);
        spawnPlacement(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCreeperPhosgene::checkPhosgeneSpawnRules);
    }

    private static <T extends Mob> void spawnPlacement(Supplier<? extends EntityType<T>> type,
                                                       Heightmap.Types heightmapType,
                                                       SpawnPlacements.SpawnPredicate<T> predicate) {
        // 1.21 переименовал SpawnPlacements.Type в интерфейс SpawnPlacementTypes.
        //? if < 1.21.1 {
        SpawnPlacementsRegistry.register(type, net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND, heightmapType, predicate);
        //?} else {
        /*SpawnPlacementsRegistry.register(type, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, heightmapType, predicate);
        *///?}
    }

    private ModEntityEvents() {
    }
}
