package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.sound.BroadcastSoundInstance;
import com.hbm_m.sound.ClientSoundBootstrap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Port of {@code TileEntityBroadcaster} (1.7.10 Original) - "Corrupted Broadcaster". Purely
 * environmental hazard prop, no GUI/inventory: every tick it scans a radius around itself and
 * applies nausea within 25 blocks, plus scaling damage within 15 blocks. Additionally plays a
 * permanent, client-side looping ambient sound (one of 3 variants, picked randomly per block/load)
 * for as long as the block exists and is loaded, using the same looping-sound infrastructure as
 * the other machines ({@link com.hbm_m.sound.ClientSoundManager}).
 */
public class BroadcasterPcBlockEntity extends BlockEntity {

    private static final double CONFUSION_RADIUS = 25.0D;
    private static final double DAMAGE_RADIUS = 15.0D;
    private static final int CONFUSION_DURATION = 300;
    private static final float MAX_DAMAGE = 10.0F;

    public BroadcasterPcBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BROADCASTER_PC_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BroadcasterPcBlockEntity be) {
        if (level.isClientSide) {
            ClientSoundBootstrap.updateSound(be, true, () -> new BroadcastSoundInstance(pos));
            return;
        }

        AABB box = new AABB(pos).inflate(CONFUSION_RADIUS);
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            double dist = entity.position().distanceTo(new net.minecraft.world.phys.Vec3(cx, cy, cz));
            if (dist > CONFUSION_RADIUS) continue;

            if (!entity.hasEffect(MobEffects.CONFUSION) || entity.getEffect(MobEffects.CONFUSION).getDuration() < 60) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CONFUSION_DURATION, 0));
            }

            if (dist <= DAMAGE_RADIUS) {
                float damage = (float) ((DAMAGE_RADIUS - dist) / DAMAGE_RADIUS * MAX_DAMAGE);
                if (damage > 0) {
                    entity.hurt(ModDamageSources.broadcast(level), damage);
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.stopSound(level, worldPosition);
        }
    }
}
