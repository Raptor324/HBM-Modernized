package com.hbm_m.entity.projectile;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityCloudFleijaRainbow;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityB92Beam} (entity_beam_bomb): Strahl der B92. Fliegt ohne Schwerkraft geradeaus, lebt 100 Ticks.
 * Trifft er ein Lebewesen, zuendet er eine FLEIJA-Explosion (Radius 10) samt Regenbogenwolke - und fliegt weiter
 * wie im Original (kein setDead beim Entitaetstreffer). Ein Blocktreffer merkt sich den Block, im naechsten Tick
 * folgt Explosion und Ende; ebenso im Wasser.
 */
public class EntityB92Beam extends Entity {

    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    /** Port: im Original liegt (-1,-1,-1) ausserhalb der Welt (Luft); in 1.20 ist Y -1 Fels - daher Merker. */
    private boolean hasTile;
    @Nullable public Entity shootingEntity;
    private int ticksInAir;

    public EntityB92Beam(EntityType<? extends EntityB92Beam> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    /** Original-Konstruktor (World, EntityLivingBase, float): Abschuss aus Augenhoehe in Blickrichtung. */
    public static EntityB92Beam shoot(Level level, LivingEntity shooter, float speed) {
        EntityB92Beam beam = new EntityB92Beam(ModEntities.B92_BEAM.get(), level);
        beam.shootingEntity = shooter;
        float yaw = shooter.getYRot();
        float pitch = shooter.getXRot();
        double x = shooter.getX() - Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
        double y = shooter.getY() + shooter.getEyeHeight() - 0.10000000149011612D;
        double z = shooter.getZ() - Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
        beam.moveTo(x, y, z, yaw, pitch);
        double mx = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double mz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double my = -Mth.sin(pitch / 180.0F * (float) Math.PI);
        beam.setThrowableHeading(mx, my, mz, speed * 1.5F, 1.0F);
        return beam;
    }

    /** 1:1 setThrowableHeading: normieren, kleine Streuung, Geschwindigkeit, Winkel aus der Bewegung. */
    public void setThrowableHeading(double x, double y, double z, float speed, float inaccuracy) {
        float f2 = Mth.sqrt((float) (x * x + y * y + z * z));
        x /= f2;
        y /= f2;
        z /= f2;
        x += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
        y += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
        z += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
        x *= speed;
        y *= speed;
        z *= speed;
        this.setDeltaMovement(x, y, z);
        float f3 = Mth.sqrt((float) (x * x + z * z));
        this.setYRot((float) (Math.atan2(x, z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(y, f3) * 180.0D / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 100) this.discard();

        Vec3 motion = this.getDeltaMovement();
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
            this.yRotO = this.getYRot();
        }

        BlockPos tile = new BlockPos(this.xTile, this.yTile, this.zTile);
        if (this.hasTile && !this.level().getBlockState(tile).isAir()) {
            this.discard();
            explode();
        }

        ++this.ticksInAir;
        Vec3 start = this.position();
        Vec3 end = start.add(motion);
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        HitResult hit = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
        if (hit != null) end = hit.getLocation();

        Entity entity = null;
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().expandTowards(motion).inflate(1.0D));
        double d0 = 0.0D;
        for (Entity e : list) {
            if (e.isPickable() && (e != this.shootingEntity || this.ticksInAir >= 5)) {
                AABB box = e.getBoundingBox().inflate(0.3F);
                var clip = box.clip(start, end);
                if (clip.isPresent()) {
                    double d1 = start.distanceTo(clip.get());
                    if (d1 < d0 || d0 == 0.0D) {
                        entity = e;
                        d0 = d1;
                    }
                }
            }
        }

        boolean entityHit = entity != null;
        if (entityHit && entity instanceof Player target) {
            if (target.getAbilities().invulnerable || this.shootingEntity instanceof Player shooter && !shooter.canHarmPlayer(target)) {
                entityHit = false;
                hit = null;
            }
        }

        if (entityHit) {
            explode();
        } else if (hit instanceof BlockHitResult bh) {
            this.xTile = bh.getBlockPos().getX();
            this.yTile = bh.getBlockPos().getY();
            this.zTile = bh.getBlockPos().getZ();
            this.hasTile = true;
        }

        double nx = this.getX() + motion.x, ny = this.getY() + motion.y, nz = this.getZ() + motion.z;
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

        if (this.isInWater()) {
            this.discard();
            explode();
        }

        this.setPos(nx, ny, nz);
        this.checkInsideBlocks();
    }

    private void explode() {
        if (!this.level().isClientSide) {
            EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(this.level(), this.getX(), this.getY(), this.getZ(), 10);
            if (!ex.isRemoved()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                        100.0F, this.level().random.nextFloat() * 0.1F + 0.9F);
                this.level().addFreshEntity(ex);
                EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(ModEntities.CLOUD_FLEIJA_RAINBOW.get(), this.level(), 10);
                cloud.setPos(this.getX(), this.getY(), this.getZ());
                this.level().addFreshEntity(cloud);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.xTile = nbt.getShort("xTile");
        this.yTile = nbt.getShort("yTile");
        this.zTile = nbt.getShort("zTile");
        this.hasTile = nbt.getBoolean("hasTile");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putShort("xTile", (short) this.xTile);
        nbt.putShort("yTile", (short) this.yTile);
        nbt.putShort("zTile", (short) this.zTile);
        nbt.putBoolean("hasTile", this.hasTile);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 4096.0D * 10.0D;
    }

    //? if < 1.21.1 {
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?}
}
