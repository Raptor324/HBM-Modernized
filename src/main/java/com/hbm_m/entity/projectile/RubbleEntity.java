package com.hbm_m.entity.projectile;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import com.hbm_m.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Обломки блоков, засасываемые чёрной дырой (порт {@code com.hbm.entity.projectile.EntityRubble}).
 */
public class RubbleEntity extends Entity {

    private static final EntityDataAccessor<BlockState> BLOCK_STATE =
            SynchedEntityData.defineId(RubbleEntity.class, EntityDataSerializers.BLOCK_STATE);

    private static final double GRAVITY = 0.03D;

    public RubbleEntity(EntityType<? extends RubbleEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public static RubbleEntity create(Level level, double x, double y, double z, BlockState state) {
        RubbleEntity rubble = new RubbleEntity(ModEntities.RUBBLE.get(), level);
        rubble.setPos(x, y, z);
        rubble.setBlockState(state);
        return rubble;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK_STATE, Blocks.STONE.defaultBlockState());
    }

    public BlockState getBlockState() {
        return this.entityData.get(BLOCK_STATE);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_STATE, state == null || state.isAir() ? Blocks.STONE.defaultBlockState() : state);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, motion.y - GRAVITY, motion.z);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onImpact(hit);
            return;
        }

        motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
    }

    private boolean canHitEntity(Entity entity) {
        return entity.isAlive() && entity != this;
    }

    private void onImpact(HitResult hit) {
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target.isAlive()) {
                target.hurt(ModDamageSources.rubble(this.level()), 15F);
            }
        }

        if (this.tickCount > 2) {
            BlockState state = this.getBlockState();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.DEBRIS.get(), SoundSource.BLOCKS, 1.5F, 1.0F);

            if (this.level() instanceof ServerLevel serverLevel) {
                BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
                serverLevel.sendParticles(particle, this.getX(), this.getY(), this.getZ(),
                        12, 0.2D, 0.2D, 0.2D, 0.05D);
            }

            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND)) {
            this.setBlockState(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("BlockState")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(this.getBlockState()));
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
