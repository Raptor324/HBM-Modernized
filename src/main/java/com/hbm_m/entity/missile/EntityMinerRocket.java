package com.hbm_m.entity.missile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.entity.missile.EntityMinerRocket}: die Frachtrakete der Bergbausatelliten. Modus 0 sinkt mit
 * 0.75/Tick bis auf die Andockstation, Modus 1 entlaedt 100 Ticks lang (Schockwellen), Modus 2 steigt auf und
 * verschwindet ueber Y 300. Trifft sie beim Sinken auf etwas anderes als die Station, explodiert sie.
 */
public class EntityMinerRocket extends Entity {

    //0 landing, 1 unloading, 2 lifting
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(EntityMinerRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SAT = SynchedEntityData.defineId(EntityMinerRocket.class, EntityDataSerializers.INT);

    public int timer = 0;

    public EntityMinerRocket(EntityType<? extends EntityMinerRocket> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityMinerRocket(Level world) {
        this(ModEntities.MINER_ROCKET.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MODE, 0);
        this.entityData.define(SAT, 0);
    }

    public int getMode() { return entityData.get(MODE); }
    public void setMode(int mode) { entityData.set(MODE, mode); }
    public int getSat() { return entityData.get(SAT); }
    public void setSat(int freq) { entityData.set(SAT, freq); }

    @Override
    public void tick() {
        double motionY = 0;
        if (getMode() == 0) motionY = -0.75;
        if (getMode() == 1) motionY = 0;
        if (getMode() == 2) motionY = 1;

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.setPos(getX(), getY() + motionY, getZ());
        this.setRot(0.0F, 0.0F);

        if (getMode() == 0 && level().getBlockState(new BlockPos((int) (getX() - 0.5), (int) (getY() - 0.5), (int) (getZ() - 0.5))).is(ModBlocks.SAT_DOCK.get())) {
            setMode(1);
            this.setPos(getX(), (int) getY(), getZ());
        } else if (!level().getBlockState(new BlockPos((int) (getX() - 0.5), (int) (getY() + 1), (int) (getZ() - 0.5))).isAir() && !level().isClientSide && getMode() != 1) {
            this.discard();
            ExplosionLarge.explodeFire(level(), getX() - 0.5, getY(), getZ() - 0.5, 10F, true, false, true);
        }

        if (getMode() == 1) {
            if (!level().isClientSide && tickCount % 4 == 0)
                ExplosionLarge.spawnShock(level(), getX(), getY(), getZ(), 1 + random.nextInt(3), 1 + random.nextGaussian());

            timer++;

            if (timer > 100) {
                setMode(2);
            }
        }

        if (getMode() != 1 && !level().isClientSide && tickCount % 2 == 0) {
            ParticleUtil.spawnGasFlame(level(), getX(), getY() - 0.5, getZ(), 0.0, -1.0, 0.0);
        }

        if (getMode() == 2 && getY() > 300)
            this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        setMode(nbt.getInt("mode"));
        setSat(nbt.getInt("sat"));
        timer = nbt.getInt("timer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("mode", getMode());
        nbt.putInt("sat", getSat());
        nbt.putInt("timer", timer);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
