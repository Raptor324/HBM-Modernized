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
        event.put(ModEntities.ENTITY_MOB_TAINTED_CREEPER.get(), EntityCreeperTainted.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(), EntityCreeperVolatile.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(), EntityCreeperGold.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get(), EntityCreeperNuclear.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(), EntityCreeperPhosgene.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.NOLO.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoloEntity::checkNoloSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityCreeperVolatile::checkVolatileSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityCreeperGold::checkGoldSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityCreeperPhosgene::checkPhosgeneSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
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

@EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NOLO.get(), NoloEntity.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_TAINTED_CREEPER.get(), EntityCreeperTainted.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(), EntityCreeperVolatile.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(), EntityCreeperGold.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER.get(), EntityCreeperNuclear.createAttributes().build());
        event.put(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(), EntityCreeperPhosgene.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // 1.21: SpawnPlacements.Type удалён. У NeoForge-события есть перегрузка
        // register(EntityType, SpawnPlacementType, Heightmap.Types, SpawnPredicate, Operation).
        event.register(ModEntities.NOLO.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoloEntity::checkNoloSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_VOLATILE_CREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityCreeperVolatile::checkVolatileSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_GOLD_CREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityCreeperGold::checkGoldSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityCreeperPhosgene::checkPhosgeneSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
*///?}
