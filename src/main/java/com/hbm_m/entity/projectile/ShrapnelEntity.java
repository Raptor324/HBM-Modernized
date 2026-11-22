package com.hbm_m.entity.projectile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorSetBlock;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Порт {@code com.hbm.entity.projectile.EntityShrapnel} (1.7.10). Режимы — байт
 * синхронизации (ориг. dataWatcher 16): 0 — обычная шрапнель, 1 — огненный след,
 * 2 — вулканический сгусток, 3 — watz (грязь), 4 — радиоактивный вулканический.
 *
 * Вулканические сгустки (используются ядром вулкана) при падении ставят лаву
 * над точкой удара + куб монооксида 3×3×3, при подъёме — лавовый взрыв 7F.
 * Сущности не сохраняются ({@code writeToNBTOptional} оригинала) — после
 * загрузки чанка исчезают.
 */
public class ShrapnelEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Byte> DATA_MODE =
            SynchedEntityData.defineId(ShrapnelEntity.class, EntityDataSerializers.BYTE);

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_TRAIL = 1;
    public static final int MODE_VOLCANO = 2;
    public static final int MODE_WATZ = 3;
    public static final int MODE_RAD_VOLCANO = 4;

    public ShrapnelEntity(EntityType<? extends ShrapnelEntity> type, Level level) {
        super(type, level);
    }

    public ShrapnelEntity(EntityType<? extends ShrapnelEntity> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
    }

    public void setTrail(boolean b) {
        this.entityData.set(DATA_MODE, (byte) (b ? MODE_TRAIL : MODE_DEFAULT));
    }

    public void setVolcano(boolean b) {
        this.entityData.set(DATA_MODE, (byte) (b ? MODE_VOLCANO : MODE_DEFAULT));
    }

    public void setWatz(boolean b) {
        this.entityData.set(DATA_MODE, (byte) (b ? MODE_WATZ : MODE_DEFAULT));
    }

    public void setRadVolcano(boolean b) {
        this.entityData.set(DATA_MODE, (byte) (b ? MODE_RAD_VOLCANO : MODE_DEFAULT));
    }

    public int getShrapnelMode() {
        return this.entityData.get(DATA_MODE);
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        // 1.20.1: defineSynchedData() is abstract in Entity - no super call.

var defs = com.hbm_m.platform.EntityDataHooks.sink(this.entityData);
        defs.define(DATA_MODE, (byte) MODE_DEFAULT);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // 1.21.1: defineSynchedData(Builder) is abstract in Entity - no super call.

var defs = com.hbm_m.platform.EntityDataHooks.sink(builder);
        defs.define(DATA_MODE, (byte) MODE_DEFAULT);
    }
    *///?}

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 1.7.10 {@code writeToNBTOptional = false}: шрапнель не переживает выгрузку. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // 1.7.10: режим trail (байт 1) рисует пламя на клиенте
        if (this.level().isClientSide && this.getShrapnelMode() == MODE_TRAIL) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        Level level = this.level();

        // 1.7.10: попадание в сущность — 15 урона шрапнелью
        if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            living.hurt(level.damageSources().thrown(this, this.getOwner()), 15.0F);
        }

        if (this.tickCount <= 5) {
            return;
        }

        int mode = this.getShrapnelMode();
        BlockPos hitPos = BlockPos.containing(result.getLocation());

        if (level.isClientSide) {
            // 1.7.10: обычная шрапнель отсыпает частицы лавы
            if (mode != MODE_VOLCANO && mode != MODE_RAD_VOLCANO && mode != MODE_WATZ) {
                for (int i = 0; i < 5; i++) {
                    level.addParticle(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
                }
            }
            return;
        }

        this.discard();

        if (mode == MODE_VOLCANO || mode == MODE_RAD_VOLCANO) {
            BlockState lava = (mode == MODE_VOLCANO
                    ? ModBlocks.VOLCANIC_LAVA_BLOCK
                    : ModBlocks.RAD_LAVA_BLOCK).get().defaultBlockState();
            double motionY = this.getDeltaMovement().y;

            if (motionY < -0.2D) {
                // Лава на точке удара + куб монооксида 3×3×3
                BlockPos above = hitPos.above();
                if (level.getBlockState(above).canBeReplaced()) {
                    level.setBlock(above, lava, 3);
                }
                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos p = hitPos.offset(x, y, z);
                            if (level.getBlockState(p).isAir()) {
                                level.setBlock(p, ModBlocks.GAS_MONOXIDE.get().defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }

            if (motionY > 0) {
                // 1.7.10: ExplosionNT 7F NODROP+LAVA_V/LAVA_R+NOSOUND+NOHURT
                ExplosionVNT explosion = new ExplosionVNT(level, hitPos.getX() + 0.5D, hitPos.getY() + 0.5D, hitPos.getZ() + 0.5D, 7.0F);
                explosion.setBlockAllocator(new BlockAllocatorStandard(16));
                explosion.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorSetBlock(() -> lava)));
                explosion.explode();
            }
        } else if (mode == MODE_WATZ) {
            // 1.7.10 ставит HBM mud_block над точкой удара; mud_block в этот порт не входил.
        }

        level.playSound(null, hitPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
