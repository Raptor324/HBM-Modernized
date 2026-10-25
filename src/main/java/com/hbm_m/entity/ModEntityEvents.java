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
        EntityAttributeRegistry.register(ModEntities.GLYPHID, com.hbm_m.entity.mob.glyphid.EntityGlyphid::createAttributes);
        EntityAttributeRegistry.register(ModEntities.HUNTER_CHOPPER, com.hbm_m.entity.mob.EntityHunterChopper::createAttributes);
        EntityAttributeRegistry.register(ModEntities.DUCK, net.minecraft.world.entity.animal.Chicken::createAttributes);
        EntityAttributeRegistry.register(ModEntities.FBI, com.hbm_m.entity.mob.EntityFBI::createAttributes);
        EntityAttributeRegistry.register(ModEntities.FBI_DRONE, com.hbm_m.entity.mob.EntityFBIDrone::createAttributes);
        EntityAttributeRegistry.register(ModEntities.QUACKOS, net.minecraft.world.entity.animal.Chicken::createAttributes);
        EntityAttributeRegistry.register(ModEntities.PIGEON, com.hbm_m.entity.mob.EntityPigeon::createAttributes);
        EntityAttributeRegistry.register(ModEntities.PLASTIC_BAG, com.hbm_m.entity.mob.EntityPlasticBag::createAttributes);
        EntityAttributeRegistry.register(ModEntities.TEST_DUMMY, com.hbm_m.entity.mob.EntityDummy::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GHOST, com.hbm_m.entity.mob.EntityGhost::createAttributes);
        EntityAttributeRegistry.register(ModEntities.BLOCK_SPIDER, com.hbm_m.entity.mob.EntityBlockSpider::createAttributes);
        EntityAttributeRegistry.register(ModEntities.CYBER_CRAB, com.hbm_m.entity.mob.EntityCyberCrab::createAttributes);
        EntityAttributeRegistry.register(ModEntities.TESLA_CRAB, com.hbm_m.entity.mob.EntityTeslaCrab::createAttributes);
        EntityAttributeRegistry.register(ModEntities.TAINT_CRAB, com.hbm_m.entity.mob.EntityTaintCrab::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_BRAWLER, com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_BEHEMOTH, com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_BRENDA, com.hbm_m.entity.mob.glyphid.EntityGlyphidBrenda::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_BOMBARDIER, com.hbm_m.entity.mob.glyphid.EntityGlyphidBombardier::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_BLASTER, com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_SCOUT, com.hbm_m.entity.mob.glyphid.EntityGlyphidScout::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_NUCLEAR, com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear::createAttributes);
        EntityAttributeRegistry.register(ModEntities.GLYPHID_DIGGER, com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger::createAttributes);
        EntityAttributeRegistry.register(ModEntities.PARASITE_MAGGOT, com.hbm_m.entity.mob.EntityParasiteMaggot::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_TAINTED_CREEPER, EntityCreeperTainted::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, EntityCreeperVolatile::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_GOLD_CREEPER, EntityCreeperGold::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER, EntityCreeperNuclear::createAttributes);
        EntityAttributeRegistry.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, EntityCreeperPhosgene::createAttributes);
        EntityAttributeRegistry.register(ModEntities.UNDEAD_SOLDIER, com.hbm_m.entity.mob.EntityUndeadSoldier::createAttributes);
        EntityAttributeRegistry.register(ModEntities.SIEGE_CRAFT, com.hbm_m.entity.mob.siege.EntitySiegeCraft::createAttributes);
        EntityAttributeRegistry.register(ModEntities.SIEGE_TUNNELER, com.hbm_m.entity.mob.siege.EntitySiegeTunneler::createAttributes);

        // --- Спавн-плейсменты (в оригинале везде Operation.REPLACE; у кастомных мобов
        // предзарегистрированных placements нет, поэтому семантика эквивалентна) ---
        spawnPlacement(ModEntities.NOLO, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NoloEntity::checkNoloSpawnRules);
        spawnPlacement(ModEntities.UNDEAD_SOLDIER, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
        spawnPlacement(ModEntities.PIGEON, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
        spawnPlacement(ModEntities.PLASTIC_BAG, true, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, com.hbm_m.entity.mob.EntityPlasticBag::checkBagSpawnRules);
        spawnPlacement(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCreeperVolatile::checkVolatileSpawnRules);
        spawnPlacement(ModEntities.ENTITY_MOB_GOLD_CREEPER, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCreeperGold::checkGoldSpawnRules);
        spawnPlacement(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, false, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityCreeperPhosgene::checkPhosgeneSpawnRules);
    }

    private static <T extends Mob> void spawnPlacement(Supplier<? extends EntityType<T>> type,
                                                       boolean inWater,
                                                       Heightmap.Types heightmapType,
                                                       SpawnPlacements.SpawnPredicate<T> predicate) {
        // 1.21 переименовал SpawnPlacements.Type в интерфейс SpawnPlacementTypes.
        //? if < 1.21.1 {
        SpawnPlacementsRegistry.register(type, inWater ? net.minecraft.world.entity.SpawnPlacements.Type.IN_WATER : net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND, heightmapType, predicate);
        //?} else {
        /*SpawnPlacementsRegistry.register(type, inWater ? net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER : net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, heightmapType, predicate);
        *///?}
    }

    /** Original addSpawn: Pilzinseln (BiomeGenMushroomIsland) bekommen keine natuerlichen Spawns. */
    private static boolean notMushroom(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos) {
        if (reason != net.minecraft.world.entity.MobSpawnType.NATURAL && reason != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) return true;
        return !level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS);
    }

    private ModEntityEvents() {
    }
}
