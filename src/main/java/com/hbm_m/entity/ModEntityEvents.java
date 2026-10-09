package com.hbm_m.entity;

//? if forge {
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.entity.mob.EntityCreeperGold;
import com.hbm_m.entity.mob.EntityCreeperPhosgene;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.entity.mob.EntityCreeperVolatile;
import com.hbm_m.entity.mob.NoloEntity;
import com.hbm_m.main.MainRegistry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NOLO.get(), NoloEntity.createAttributes().build());
        event.put(ModEntities.UFO.get(), com.hbm_m.entity.mob.EntityUFO.createAttributes().build());
        event.put(ModEntities.BOT_PRIME_HEAD.get(), com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase.createAttributes().build());
        event.put(ModEntities.BOT_PRIME_BODY.get(), com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase.createAttributes().build());
        event.put(ModEntities.RAD_BEAST.get(), com.hbm_m.entity.mob.EntityRADBeast.createAttributes().build());
        event.put(ModEntities.MASKMAN.get(), com.hbm_m.entity.mob.EntityMaskMan.createAttributes().build());
        event.put(ModEntities.GLYPHID.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphid.createAttributes().build());
        event.put(ModEntities.HUNTER_CHOPPER.get(), com.hbm_m.entity.mob.EntityHunterChopper.createAttributes().build());
        event.put(ModEntities.DUCK.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(ModEntities.FBI.get(), com.hbm_m.entity.mob.EntityFBI.createAttributes().build());
        event.put(ModEntities.FBI_DRONE.get(), com.hbm_m.entity.mob.EntityFBIDrone.createAttributes().build());
        event.put(ModEntities.QUACKOS.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(ModEntities.PIGEON.get(), com.hbm_m.entity.mob.EntityPigeon.createAttributes().build());
        event.put(ModEntities.PLASTIC_BAG.get(), com.hbm_m.entity.mob.EntityPlasticBag.createAttributes().build());
        event.put(ModEntities.TEST_DUMMY.get(), com.hbm_m.entity.mob.EntityDummy.createAttributes().build());
        event.put(ModEntities.GHOST.get(), com.hbm_m.entity.mob.EntityGhost.createAttributes().build());
        event.put(ModEntities.BLOCK_SPIDER.get(), com.hbm_m.entity.mob.EntityBlockSpider.createAttributes().build());
        event.put(ModEntities.CYBER_CRAB.get(), com.hbm_m.entity.mob.EntityCyberCrab.createAttributes().build());
        event.put(ModEntities.TESLA_CRAB.get(), com.hbm_m.entity.mob.EntityTeslaCrab.createAttributes().build());
        event.put(ModEntities.TAINT_CRAB.get(), com.hbm_m.entity.mob.EntityTaintCrab.createAttributes().build());
        event.put(ModEntities.GLYPHID_BRAWLER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler.createAttributes().build());
        event.put(ModEntities.GLYPHID_BEHEMOTH.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth.createAttributes().build());
        event.put(ModEntities.GLYPHID_BRENDA.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBrenda.createAttributes().build());
        event.put(ModEntities.GLYPHID_BOMBARDIER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBombardier.createAttributes().build());
        event.put(ModEntities.GLYPHID_BLASTER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster.createAttributes().build());
        event.put(ModEntities.GLYPHID_SCOUT.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidScout.createAttributes().build());
        event.put(ModEntities.GLYPHID_NUCLEAR.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear.createAttributes().build());
        event.put(ModEntities.GLYPHID_DIGGER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger.createAttributes().build());
        event.put(ModEntities.PARASITE_MAGGOT.get(), com.hbm_m.entity.mob.EntityParasiteMaggot.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_TAINTED_CREEPER.get(), EntityCreeperTainted.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(), EntityCreeperVolatile.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(), EntityCreeperGold.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get(), EntityCreeperNuclear.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(), EntityCreeperPhosgene.createAttributes().build());
        event.put(ModEntities.UNDEAD_SOLDIER.get(), com.hbm_m.entity.mob.EntityUndeadSoldier.createAttributes().build());
        event.put(ModEntities.SIEGE_CRAFT.get(), com.hbm_m.entity.mob.siege.EntitySiegeCraft.createAttributes().build());
        event.put(ModEntities.SIEGE_TUNNELER.get(), com.hbm_m.entity.mob.siege.EntitySiegeTunneler.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
        // Original BiomeGenNoMansLand: Untote Soldaten spawnen als Monster auf festem Boden (ohne Lichtpruefung)
        event.register(ModEntities.UNDEAD_SOLDIER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        // Original addSpawn: Tauben (Ebenen), Plastiktueten (Ozean, Y 45-63)
        event.register(ModEntities.PIGEON.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.PLASTIC_BAG.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.hbm_m.entity.mob.EntityPlasticBag::checkBagSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.NOLO.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoloEntity::checkNoloSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> notMushroom(l, r, p) && EntityCreeperVolatile.checkVolatileSpawnRules(t, l, r, p, rnd),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> notMushroom(l, r, p) && EntityCreeperGold.checkGoldSpawnRules(t, l, r, p, rnd),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> notMushroom(l, r, p) && EntityCreeperPhosgene.checkPhosgeneSpawnRules(t, l, r, p, rnd),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    /** Original addSpawn: Pilzinseln (BiomeGenMushroomIsland) bekommen keine natuerlichen Spawns. */
    private static boolean notMushroom(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos) {
        if (reason != net.minecraft.world.entity.MobSpawnType.NATURAL && reason != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) return true;
        return !level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS);
    }

    private ModEntityEvents() {
    }
}
 //?}

//? if neoforge {
/*import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;

import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.entity.mob.EntityCreeperGold;
import com.hbm_m.entity.mob.EntityCreeperPhosgene;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.entity.mob.EntityCreeperVolatile;
import com.hbm_m.entity.mob.NoloEntity;
import com.hbm_m.main.MainRegistry;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

// Gleiche Attribute/Spawn-Regeln wie die Forge-Fassung (1.21: SpawnPlacements.Type -> SpawnPlacementTypes).
@EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NOLO.get(), NoloEntity.createAttributes().build());
        event.put(ModEntities.UFO.get(), com.hbm_m.entity.mob.EntityUFO.createAttributes().build());
        event.put(ModEntities.BOT_PRIME_HEAD.get(), com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase.createAttributes().build());
        event.put(ModEntities.BOT_PRIME_BODY.get(), com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase.createAttributes().build());
        event.put(ModEntities.RAD_BEAST.get(), com.hbm_m.entity.mob.EntityRADBeast.createAttributes().build());
        event.put(ModEntities.MASKMAN.get(), com.hbm_m.entity.mob.EntityMaskMan.createAttributes().build());
        event.put(ModEntities.GLYPHID.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphid.createAttributes().build());
        event.put(ModEntities.HUNTER_CHOPPER.get(), com.hbm_m.entity.mob.EntityHunterChopper.createAttributes().build());
        event.put(ModEntities.DUCK.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(ModEntities.FBI.get(), com.hbm_m.entity.mob.EntityFBI.createAttributes().build());
        event.put(ModEntities.FBI_DRONE.get(), com.hbm_m.entity.mob.EntityFBIDrone.createAttributes().build());
        event.put(ModEntities.QUACKOS.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(ModEntities.PIGEON.get(), com.hbm_m.entity.mob.EntityPigeon.createAttributes().build());
        event.put(ModEntities.PLASTIC_BAG.get(), com.hbm_m.entity.mob.EntityPlasticBag.createAttributes().build());
        event.put(ModEntities.TEST_DUMMY.get(), com.hbm_m.entity.mob.EntityDummy.createAttributes().build());
        event.put(ModEntities.GHOST.get(), com.hbm_m.entity.mob.EntityGhost.createAttributes().build());
        event.put(ModEntities.BLOCK_SPIDER.get(), com.hbm_m.entity.mob.EntityBlockSpider.createAttributes().build());
        event.put(ModEntities.CYBER_CRAB.get(), com.hbm_m.entity.mob.EntityCyberCrab.createAttributes().build());
        event.put(ModEntities.TESLA_CRAB.get(), com.hbm_m.entity.mob.EntityTeslaCrab.createAttributes().build());
        event.put(ModEntities.TAINT_CRAB.get(), com.hbm_m.entity.mob.EntityTaintCrab.createAttributes().build());
        event.put(ModEntities.GLYPHID_BRAWLER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler.createAttributes().build());
        event.put(ModEntities.GLYPHID_BEHEMOTH.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth.createAttributes().build());
        event.put(ModEntities.GLYPHID_BRENDA.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBrenda.createAttributes().build());
        event.put(ModEntities.GLYPHID_BOMBARDIER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBombardier.createAttributes().build());
        event.put(ModEntities.GLYPHID_BLASTER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster.createAttributes().build());
        event.put(ModEntities.GLYPHID_SCOUT.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidScout.createAttributes().build());
        event.put(ModEntities.GLYPHID_NUCLEAR.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear.createAttributes().build());
        event.put(ModEntities.GLYPHID_DIGGER.get(), com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger.createAttributes().build());
        event.put(ModEntities.PARASITE_MAGGOT.get(), com.hbm_m.entity.mob.EntityParasiteMaggot.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_TAINTED_CREEPER.get(), EntityCreeperTainted.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(), EntityCreeperVolatile.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(), EntityCreeperGold.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get(), EntityCreeperNuclear.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(), EntityCreeperPhosgene.createAttributes().build());
        event.put(ModEntities.UNDEAD_SOLDIER.get(), com.hbm_m.entity.mob.EntityUndeadSoldier.createAttributes().build());
        event.put(ModEntities.SIEGE_CRAFT.get(), com.hbm_m.entity.mob.siege.EntitySiegeCraft.createAttributes().build());
        event.put(ModEntities.SIEGE_TUNNELER.get(), com.hbm_m.entity.mob.siege.EntitySiegeTunneler.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Original BiomeGenNoMansLand: Untote Soldaten spawnen als Monster auf festem Boden (ohne Lichtpruefung)
        event.register(ModEntities.UNDEAD_SOLDIER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Original addSpawn: Tauben (Ebenen), Plastiktueten (Ozean, Y 45-63)
        event.register(ModEntities.PIGEON.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.PLASTIC_BAG.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.hbm_m.entity.mob.EntityPlasticBag::checkBagSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.NOLO.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoloEntity::checkNoloSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> notMushroom(l, r, p) && EntityCreeperVolatile.checkVolatileSpawnRules(t, l, r, p, rnd),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> notMushroom(l, r, p) && EntityCreeperGold.checkGoldSpawnRules(t, l, r, p, rnd),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, l, r, p, rnd) -> notMushroom(l, r, p) && EntityCreeperPhosgene.checkPhosgeneSpawnRules(t, l, r, p, rnd),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // Original addSpawn: Pilzinseln (BiomeGenMushroomIsland) bekommen keine natuerlichen Spawns.
    private static boolean notMushroom(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos) {
        if (reason != net.minecraft.world.entity.MobSpawnType.NATURAL && reason != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) return true;
        return !level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS);
    }

    private ModEntityEvents() {
    }
}
*///?}
