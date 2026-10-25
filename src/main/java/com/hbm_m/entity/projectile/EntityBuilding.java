package com.hbm_m.entity.projectile;

import java.util.List;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.entity.projectile.EntityBuilding}: das vom Himmel fallende Gebaeude (.50 BMG "Building").
 * 100 Wolkenpartikel beim ersten Tick, faellt mit bis zu 1.5 Bloecken/Tick; beim Aufschlag alter Explosionsklang,
 * Partikel, fuenf Schockwellen, 1000 Schaden ("building") im Umkreis 8 und 250 Ziegel-Truemmer.
 */
public class EntityBuilding extends Entity {

    public EntityBuilding(EntityType<? extends EntityBuilding> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityBuilding(Level world) {
        this(ModEntities.BUILDING.get(), world);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        // Original ueberschreibt onUpdate vollstaendig (kein Entity-Grundtick)
        if (level() instanceof ServerLevel server && this.tickCount == 1) {
            for (int i = 0; i < 100; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "bf");
                IParticleCreator.sendPacket(server,
                        getX() + (random.nextDouble() - 0.5) * 15,
                        getY() + (random.nextDouble() - 0.5) * 15,
                        getZ() + (random.nextDouble() - 0.5) * 15, 150, data);
            }
        }

        this.xOld = this.xo = getX();
        this.yOld = this.yo = getY();
        this.zOld = this.zo = getZ();
        this.setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);

        double motionY = getDeltaMovement().y - 0.03;
        if (motionY < -1.5) motionY = -1.5;
        this.setDeltaMovement(getDeltaMovement().x, motionY, getDeltaMovement().z);

        // Original: (int) posX usw. (Richtung null gerundet)
        if (!this.level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
            SoundEvent boom = HbmSoundsNT.get("hbm:entity.oldExplosion");
            if (boom != null) this.level().playSound(null, getX(), getY(), getZ(), boom, SoundSource.PLAYERS, 10000.0F, 0.5F + this.random.nextFloat() * 0.1F);
            this.discard();
            ExplosionLarge.spawnParticles(level(), getX(), getY() + 3, getZ(), 150);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 6);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 4);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);

            List<Entity> list = level().getEntities((Entity) null, new AABB(getX() - 8, getY() - 8, getZ() - 8, getX() + 8, getY() + 8, getZ() + 8));

            for (Entity e : list) e.hurt(ModDamageSources.create(level(), ModDamageTypes.BUILDING), 1000);

            for (int i = 0; i < 250; i++) {

                Vec3NT vec = new Vec3NT(1, 0, 0);
                vec.rotateAroundZ((float) (-random.nextFloat() * Math.PI / 2));
                vec.rotateAroundY((float) (random.nextFloat() * Math.PI * 2));

                // Original spawnt auch clientseitig; im Port nur der Server (Truemmer sind synchronisierte Entities)
                if (!level().isClientSide) {
                    RubbleEntity rubble = RubbleEntity.create(level(), getX(), getY() + 3, getZ(), Blocks.BRICKS.defaultBlockState());
                    rubble.setDeltaMovement(vec.xCoord, vec.yCoord, vec.zCoord);
                    level().addFreshEntity(rubble);
                }
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    //? if < 1.21.1 {
    @Override protected void defineSynchedData() { }
    //?} else {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }
}
