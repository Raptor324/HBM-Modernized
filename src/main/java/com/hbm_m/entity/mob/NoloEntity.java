package com.hbm_m.entity.mob;

import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class NoloEntity extends Fox {

    public NoloEntity(EntityType<? extends Fox> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Fox.createAttributes();
    }

    @SuppressWarnings("unchecked")
    public static boolean checkNoloSpawnRules(EntityType<NoloEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Fox.checkFoxSpawnRules((EntityType<Fox>) (EntityType<?>) entityType, level, spawnType, pos, random);
    }

    @Override
    public Fox getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return ModEntities.NOLO.get().create(serverLevel);
    }
}