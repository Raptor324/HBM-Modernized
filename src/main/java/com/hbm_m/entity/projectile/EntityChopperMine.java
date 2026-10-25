package com.hbm_m.entity.projectile;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityChopperMine}: Abwurfmine des Kampfhubschraubers (Kasten 12x12). Faellt beschleunigt, bremst
 * seitlich; explodiert (Staerke 5, ohne Feuer) bei Spielerberuehrung, nach 100 Ticks oder in einem Block.
 */
public class EntityChopperMine extends Entity {

    public int timer = 0;
    @Nullable public Entity shooter;

    public EntityChopperMine(EntityType<? extends EntityChopperMine> type, Level world) {
        super(type, world);
    }

    public EntityChopperMine(Level world, double x, double y, double z, double moX, double moY, double moZ, Entity shooter) {
        this(ModEntities.CHOPPER_MINE.get(), world);
        this.setPos(x, y, z);
        this.setDeltaMovement(moX, moY, moZ);
        this.shooter = shooter;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() { }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) { }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        Vec3 vec31 = position();
        Vec3 vec3 = position().add(motion);
        HitResult hit = level().clip(new ClipContext(vec31, vec3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.MISS) {
            vec3 = hit.getLocation();
        }

        Entity entity = null;
        List<Entity> list = level().getEntities(this, this.getBoundingBox().expandTowards(motion).inflate(1.0D, 1.0D, 1.0D));
        double d0 = 0.0D;

        for (Entity entity1 : list) {

            if (entity1.isPickable() && (entity1 != this.shooter)) {
                float f1 = 0.3F;
                AABB axisalignedbb1 = entity1.getBoundingBox().inflate(f1, f1, f1);
                var intercept = axisalignedbb1.clip(vec31, vec3);

                if (intercept.isPresent()) {
                    double d1 = vec31.distanceTo(intercept.get());

                    if (d1 < d0 || d0 == 0.0D) {
                        entity = entity1;
                        d0 = d1;
                    }
                }
            }
        }

        if (entity instanceof Player && !level().isClientSide) {
            level().explode(shooter, getX(), getY(), getZ(), 5F, false, Level.ExplosionInteraction.MOB);
            this.discard();
        }

        if (!level().isClientSide) {
            var ev = HbmSoundsNT.get("hbm:misc.nullMine");
            if (ev != null) level().playSound(null, getX(), getY(), getZ(), ev, SoundSource.HOSTILE, 10.0F, 1F);
        }

        if (timer >= 100 || !level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
            if (!level().isClientSide) {
                level().explode(shooter, getX(), getY(), getZ(), 5F, false, Level.ExplosionInteraction.MOB);
                this.discard();
            }
        }

        double mx = motion.x, my = motion.y, mz = motion.z;
        if (my > -0.85)
            my -= 0.05;

        mx *= 0.9;
        mz *= 0.9;

        this.setDeltaMovement(mx, my, mz);
        this.setPos(getX() + mx, getY() + my, getZ() + mz);

        timer++;

        if (level().isClientSide) com.hbm_m.client.sound.ChopperSoundClient.tickMine(this);
    }
}
