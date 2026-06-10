package com.hbm_m.entity.effect;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.RubbleEntity;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Чёрная дыра (порт {@code com.hbm.entity.effect.EntityBlackHole}).
 */
public class BlackHoleEntity extends Entity {

    protected static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);

    protected boolean breaksBlocks = true;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public BlackHoleEntity setSize(float size) {
        this.entityData.set(SIZE, size);
        return this;
    }

    public BlackHoleEntity noBreak() {
        this.breaksBlocks = false;
        return this;
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    @Override
    public void tick() {
        super.tick();

        float size = this.entityData.get(SIZE);
        Level level = this.level();

        if (this.breaksBlocks && !level.isClientSide) {
            for (int k = 0; k < size * 2; k++) {
                double phi = this.random.nextDouble() * (Math.PI * 2);
                double costheta = this.random.nextDouble() * 2 - 1;
                double theta = Math.acos(costheta);
                double x = Math.sin(theta) * Math.cos(phi);
                double y = Math.sin(theta) * Math.sin(phi);
                double z = Math.cos(theta);

                Vec3 vec = new Vec3(x, y, z);
                int length = (int) Math.ceil(size * 15);

                for (int i = 0; i < length; i++) {
                    int x0 = (int) (this.getX() + (vec.x * i));
                    int y0 = (int) (this.getY() + (vec.y * i));
                    int z0 = (int) (this.getZ() + (vec.z * i));
                    BlockPos toChange = new BlockPos(x0, y0, z0);
                    BlockState state = level.getBlockState(toChange);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!state.getFluidState().isEmpty()) {
                        level.setBlock(toChange, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    RubbleEntity rubble = RubbleEntity.create(level, x0 + 0.5D, y0, z0 + 0.5D, state);
                    level.addFreshEntity(rubble);
                    level.setBlock(toChange, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }

        double range = size * 15;
        List<Entity> entities = level.getEntities(
                null,
                new AABB(getX() - range, getY() - range, getZ() - range, getX() + range, getY() + range, getZ() + range));

        for (Entity entity : entities) {
            if (entity instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
            }

            if (entity instanceof FallingBlockEntity falling && !level.isClientSide && falling.tickCount > 1) {
                BlockState fallingState = falling.getBlockState();
                RubbleEntity rubble = RubbleEntity.create(level, falling.getX(), falling.getY(), falling.getZ(), fallingState);
                rubble.setDeltaMovement(falling.getDeltaMovement());
                level.addFreshEntity(rubble);
                falling.discard();
                continue;
            }

            Vec3 toEntity = new Vec3(getX() - entity.getX(), getY() - entity.getY(), getZ() - entity.getZ());
            double dist = toEntity.length();
            if (dist > range) {
                continue;
            }

            toEntity = toEntity.normalize();
            if (!(entity instanceof ItemEntity)) {
                toEntity = toEntity.yRot((float) Math.toRadians(15));
            }

            double speed = 0.1D;
            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    toEntity.x * speed,
                    toEntity.y * speed * 2,
                    toEntity.z * speed
            ));

            if (entity instanceof BlackHoleEntity) {
                continue;
            }

            if (dist < size * 1.5) {
                entity.hurt(ModDamageSources.blackHole(level), 1000F);

                if (!(entity instanceof LivingEntity)) {
                    entity.discard();
                }

                if (!level.isClientSide && entity instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getItem();
                    if (stack.is(ModItems.PELLET_ANTIMATTER.get()) || stack.is(ModItems.FLAME_PONY.get())) {
                        this.discard();
                        level.explode(null, this.getX(), this.getY(), this.getZ(), 5.0F, true, Level.ExplosionInteraction.BLOCK);
                        return;
                    }
                }
            }
        }

        this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.99D, 0.99D, 0.99D));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SIZE, 0.5F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SIZE, tag.getFloat("Size"));
        this.breaksBlocks = tag.getBoolean("BreaksBlocks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Size", this.entityData.get(SIZE));
        tag.putBoolean("BreaksBlocks", this.breaksBlocks);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 25000.0 * 25000.0;
    }
}
