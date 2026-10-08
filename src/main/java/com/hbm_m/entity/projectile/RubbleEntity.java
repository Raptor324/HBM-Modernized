package com.hbm_m.entity.projectile;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Обломки блоков, засасываемые чёрной дырой (порт {@code com.hbm.entity.projectile.EntityRubble}).
 * 1:1 {@code extends EntityThrowableNT}: Schwerkraft 0.03, kein Luftwiderstand (getAirDrag 1),
 * im Wasser 0.8; trifft fuer 15 rubble-Schaden und zerfaellt ab dem dritten Tick beim Aufprall.
 */
public class RubbleEntity extends EntityThrowableNT {

    private static final EntityDataAccessor<BlockState> BLOCK_STATE =
            SynchedEntityData.defineId(RubbleEntity.class, EntityDataSerializers.BLOCK_STATE);

    public RubbleEntity(EntityType<? extends RubbleEntity> type, Level level) {
        super(type, level);
    }

    public static RubbleEntity create(Level level, double x, double y, double z, BlockState state) {
        RubbleEntity rubble = new RubbleEntity(ModEntities.RUBBLE.get(), level);
        rubble.setPos(x, y, z);
        rubble.setBlockState(state);
        return rubble;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(BLOCK_STATE, Blocks.STONE.defaultBlockState());
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_STATE, Blocks.STONE.defaultBlockState());
    }
    *///?}

    public BlockState getBlockState() {
        return this.entityData.get(BLOCK_STATE);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_STATE, state == null || state.isAir() ? Blocks.STONE.defaultBlockState() : state);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            target.hurt(ModDamageSources.rubble(this.level()), 15F);
        }

        if (this.tickCount > 2) {
            this.discard();

            if (this.level() instanceof ServerLevel serverLevel) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.DEBRIS.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                // 1:1 Original: ParticleBurstPacket (volle Blockzerfallspartikel), Umkreis 50
                com.hbm_m.network.ParticleBurstPacket.sendAround(serverLevel, this.getX(), this.getY(), this.getZ(), 50,
                        (int) Math.floor(this.getX()), (int) this.getY(), (int) Math.floor(this.getZ()), this.getBlockState());
            }
        }
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND)) {
            this.setBlockState(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("BlockState")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("BlockState", NbtUtils.writeBlockState(this.getBlockState()));
    }
}
