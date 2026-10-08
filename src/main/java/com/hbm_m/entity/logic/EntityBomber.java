package com.hbm_m.entity.logic;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.EntityBombletZeta;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBomber}: fliegt 100 Bloecke vor dem Ziel auf Hoehe +50 ein und wirft zwischen {@code bombStart}
 * und {@code bombStop} alle {@code bombRate} Ticks ab. Ladungen: 0 Teppich, 1 Napalm, 2 Chlor, 3 Agent Orange
 * (Giftwolke am Flugzeug), 4 Atombombe, 5 Stinger (wirft nichts ab), 6 Boxcar-Raketen, 7 Giftwolke am Boden ("PC").
 * Das Aussehen (Dornier 1-4, B-29 5-8) wird zufaellig gewaehlt; manche Ladungen legen es fest.
 */
public class EntityBomber extends EntityPlaneBase {

    /** Datawatcher 16: Flugzeugaussehen. */
    private static final EntityDataAccessor<Byte> STYLE = SynchedEntityData.defineId(EntityBomber.class, EntityDataSerializers.BYTE);

    /* This was probably the dumbest fucking way that I could have handled this. Not gonna change it now, be glad I made a superclass at all. */
    int bombStart = 75;
    int bombStop = 125;
    int bombRate = 3;
    int type = 0;

    public EntityBomber(EntityType<? extends EntityBomber> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STYLE, (byte) 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STYLE, (byte) 0);
    }
    *///?}

    public int getPlaneType() { return this.entityData.get(STYLE); }
    private void setStyle(int i) { this.entityData.set(STYLE, (byte) i); }

    /** This sucks balls. Too bad! */
    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            com.hbm_m.client.sound.ChopperSoundClient.tickPlane(this, getPlaneType() <= 4 ? "hbm:entity.bomberSmallLoop" : "hbm:entity.bomberLoop");
        }

        if (!level().isClientSide && this.health > 0 && this.tickCount > bombStart && this.tickCount < bombStop && this.tickCount % bombRate == 0) {

            Level world = level();
            if (type == 3) {
                world.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                com.hbm_m.explosion.ExplosionChaos.spawnPoisonCloud(world, getX(), getY() - 1F, getZ(), 10, 0.5, 3);

            } else if (type == 5) {

            } else if (type == 6) {
                world.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, HbmSoundsNT.get("hbm:weapon.missileTakeOff"), SoundSource.NEUTRAL, 10.0F, 0.9F + random.nextFloat() * 0.2F);
                com.hbm_m.entity.projectile.EntityBoxcar rocket = new com.hbm_m.entity.projectile.EntityBoxcar(world);
                rocket.setPos(getX() + random.nextDouble() - 0.5, getY() - random.nextDouble(), getZ() + random.nextDouble() - 0.5);
                world.addFreshEntity(rocket);

            } else if (type == 7) {
                world.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                com.hbm_m.explosion.ExplosionChaos.spawnPoisonCloud(world, getX(), world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) getX(), (int) getZ()) + 2, getZ(), 10, 1, 2);

            } else {
                world.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, HbmSoundsNT.get("hbm:entity.bombWhistle"), SoundSource.NEUTRAL, 10.0F, 0.9F + random.nextFloat() * 0.2F);
                Vec3 m = getDeltaMovement();
                EntityBombletZeta zeta = EntityBombletZeta.create(world, getX() + random.nextDouble() - 0.5, getY() - random.nextDouble(), getZ() + random.nextDouble() - 0.5, type);
                if (type == 0) {
                    zeta.setDeltaMovement(m.x + random.nextGaussian() * 0.15, 0, m.z + random.nextGaussian() * 0.15);
                } else {
                    zeta.setDeltaMovement(m.x, 0, m.z);
                }
                zeta.updateRotation();
                world.addFreshEntity(zeta);
            }
        }
    }

    public void fac(Level world, double x, double y, double z) {

        Vec3 vector = new Vec3(world.random.nextDouble() - 0.5, 0, world.random.nextDouble() - 0.5).normalize();
        double mult = com.hbm_m.config.ModClothConfig.get().enableBomberShortMode ? 1 : 2;
        vector = new Vec3(vector.x * mult, 0, vector.z * mult);

        this.moveTo(x - vector.x * 100, y + 50, z - vector.z * 100, 0.0F, 0.0F);
        this.loadNeighboringChunks((int) (x / 16), (int) (z / 16));

        this.setDeltaMovement(vector.x, 0.0D, vector.z);

        this.rotation();

        int i = 1;
        int rand = world.random.nextInt(7);

        switch (rand) {
            case 0, 1 -> i = 1;
            case 2, 3 -> i = 2;
            case 4 -> i = 5;
            case 5 -> i = 6;
            case 6 -> i = 7;
        }

        if (world.random.nextInt(100) == 0) {
            rand = world.random.nextInt(4);
            switch (rand) {
                case 0 -> i = 0;
                case 1 -> i = 3;
                case 2 -> i = 4;
                case 3 -> i = 8;
            }
        }

        setStyle(i);
    }

    private static EntityBomber make(Level world, double x, double y, double z, int start, int stop, int rate, int type) {
        EntityBomber bomber = new EntityBomber(ModEntities.BOMBER.get(), world);
        bomber.timer = 200;
        bomber.bombStart = start;
        bomber.bombStop = stop;
        bomber.bombRate = rate;
        bomber.fac(world, x, y, z);
        bomber.type = type;
        return bomber;
    }

    public static EntityBomber carpet(Level world, double x, double y, double z) { return make(world, x, y, z, 50, 100, 2, 0); }
    public static EntityBomber napalm(Level world, double x, double y, double z) { return make(world, x, y, z, 50, 100, 5, 1); }
    public static EntityBomber chlorine(Level world, double x, double y, double z) { return make(world, x, y, z, 50, 100, 4, 2); }
    public static EntityBomber orange(Level world, double x, double y, double z) { return make(world, x, y, z, 75, 125, 1, 3); }

    public static EntityBomber aBomb(Level world, double x, double y, double z) {
        EntityBomber bomber = make(world, x, y, z, 60, 70, 65, 4);
        int i = switch (world.random.nextInt(3)) {
            case 0 -> 5;
            case 1 -> 6;
            default -> 7;
        };
        if (world.random.nextInt(100) == 0) i = 8;
        bomber.setStyle(i);
        return bomber;
    }

    public static EntityBomber stinger(Level world, double x, double y, double z) {
        EntityBomber bomber = make(world, x, y, z, 50, 150, 10, 5);
        bomber.setStyle(4);
        return bomber;
    }

    public static EntityBomber boxcar(Level world, double x, double y, double z) {
        EntityBomber bomber = make(world, x, y, z, 50, 150, 10, 6);
        bomber.setStyle(6);
        return bomber;
    }

    public static EntityBomber pc(Level world, double x, double y, double z) {
        EntityBomber bomber = make(world, x, y, z, 75, 125, 1, 7);
        bomber.setStyle(6);
        return bomber;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        bombStart = nbt.getInt("bombStart");
        bombStop = nbt.getInt("bombStop");
        bombRate = Math.max(1, nbt.getInt("bombRate"));
        type = nbt.getInt("type");
        setStyle(nbt.getByte("style"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("bombStart", bombStart);
        nbt.putInt("bombStop", bombStop);
        nbt.putInt("bombRate", bombRate);
        nbt.putInt("type", type);
        nbt.putByte("style", (byte) getPlaneType());
    }
}
