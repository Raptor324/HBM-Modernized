package com.hbm_m.entity.missile;

import com.hbm_m.explosion.MissileWarheadEffects;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import api.hbm.entity.IRadarDetectable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
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
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            MissileWarheadEffects.warheadShuttle(this, server, pos);
        }
        level().playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                ModSounds.EXPLOSION_LARGE_NEAR.get(), SoundSource.PLAYERS, 4.0F,
                0.7F + level().random.nextFloat() * 0.2F);
    }
}
