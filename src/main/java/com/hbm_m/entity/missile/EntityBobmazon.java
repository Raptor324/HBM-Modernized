package com.hbm_m.entity.missile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityBobmazon}: die Lieferrakete. Faellt mit 4 x 0.5 Bloecken pro Tick; trifft sie auf einen Block,
 * gibt es Partikel und den alten Explosionsknall, die Ware wird 2 Bloecke hoeher abgesetzt.
 */
public class EntityBobmazon extends Entity {

    private static final EntityDataAccessor<Integer> DATA_16 = SynchedEntityData.defineId(EntityBobmazon.class, EntityDataSerializers.INT);

    public ItemStack payload = ItemStack.EMPTY;

    public EntityBobmazon(EntityType<? extends EntityBobmazon> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityBobmazon(Level world) {
        this(ModEntities.BOBMAZON.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_16, 0);
    }

    @Override
    public void tick() {

        double motionY = -0.5;

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();

        for (int i = 0; i < 4; i++) {

            BlockPos check = new BlockPos((int) (getX() - 0.5), (int) (getY() + 1), (int) (getZ() - 0.5));
            if (!level().getBlockState(check).isAir() && !level().isClientSide && entityData.get(DATA_16) != 1) {
                ExplosionLarge.spawnParticles(level(), getX(), getY() + 1, getZ(), 50);

                level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:entity.oldExplosion"), SoundSource.NEUTRAL, 10.0F, 0.5F + this.random.nextFloat() * 0.1F);

                if (!payload.isEmpty()) {
                    ItemEntity pack = new ItemEntity(level(), getX(), getY() + 2, getZ(), payload);
                    pack.setDeltaMovement(0, pack.getDeltaMovement().y, 0);
                    level().addFreshEntity(pack);
                }

                this.discard();

                break;
            }

            this.setPos(getX(), getY() + motionY, getZ());
        }

        if (level().isClientSide) {

            CompoundTag data = new CompoundTag();
            data.putString("type", "exhaust");
            data.putString("mode", "meteor");
            data.putInt("count", 1);
            data.putDouble("width", 0);
            data.putDouble("posX", getX());
            data.putDouble("posY", getY() + 1);
            data.putDouble("posZ", getZ());

            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.payload = ItemStack.of(nbt.getCompound("payload"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        CompoundTag nbt1 = new CompoundTag();
        payload.save(nbt1);
        nbt.put("payload", nbt1);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }
}
