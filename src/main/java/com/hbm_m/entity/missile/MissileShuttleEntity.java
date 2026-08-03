package com.hbm_m.entity.missile;

import com.hbm_m.explosion.MissileWarheadEffects;
import com.hbm_m.item.ModItems;

import api.hbm_m.entity.IRadarDetectable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Шаттл-ракета (корпус shuttle.obj).
 */
public class MissileShuttleEntity extends MissileBaseEntity {

    public MissileShuttleEntity(EntityType<? extends MissileShuttleEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_TIER3;
    }

    @Override
    protected List<ItemStack> getDebris() {
        return List.of(
                new ItemStack(ModItems.SCRAP.get(), 8),
                new ItemStack(ModItems.SCRAP.get(), 2),
                new ItemStack(ModItems.SCRAP.get(), 1),
                new ItemStack(net.minecraft.world.level.block.Blocks.GLASS_PANE, 2));
    }

    @Override
    protected ItemStack getDebrisRareDrop() {
        return new ItemStack(ModItems.MISSILE_GENERIC.get());
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (level().isClientSide) {
            return;
        }
        // 1.7.10 EntityMissileShuttle: ExplosionNT(NOSOUND+NOPARTICLE, 20F, res 64) + rbmkmush
        // + уникальный звук robin_explosion (4.0F, pitch (1.0+(rand-rand)*0.2)*0.7). Звук здесь НЕ
        // дублируется: warheadShuttle → composeEffectLarge → ExplosionCreator уже воспроизводит
        // отложенный explosionLarge (near/far) с задержкой dist/8.575 (скорость звука) в пределах
        // soundRange=350. Оригинальный robin_explosion недоступен (нет .ogg ассета в Modernized),
        // поэтому используется общий explosionLarge. Прежний прямой EXPLOSION_LARGE_NEAR давал
        // «двойной бум» (немедленный + отложенный).
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            MissileWarheadEffects.warheadShuttle(this, server, pos);
        }
    }
}
