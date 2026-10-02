package com.hbm_m.entity.effect;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.ExplosionNukeGeneric;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityModFX} des Originals: als Entity (nicht als Partikel) gefuehrte Gaswolke, die sich selbst
 * durch die Welt schiebt und dabei ihre Umgebung vergiftet. Die vier Wolken des Originals
 * ({@code EntityChlorineFX}, {@code EntityCloudFX}, {@code EntityPinkCloudFX}, {@code EntityOrangeFX}) sind die
 * inneren Klassen; gezeichnet werden sie vom {@code MultiCloudRenderer}.
 */
public abstract class EntityModFX extends Entity {

    public int particleAge;
    public int maxAge;

    protected EntityModFX(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    /** {@code EntityModFX(world, x, y, z, mx, my, mz)}: zufaellige Grundbewegung, danach die Werte der Unterklasse. */
    protected EntityModFX(EntityType<?> type, Level level, double x, double y, double z, double mx, double my, double mz) {
        this(type, level);
        setPos(x, y, z);
        xo = x; yo = y; zo = z;
        double motionX = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double motionY = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        double motionZ = (float) (Math.random() * 2.0D - 1.0D) * 0.4F;
        float f = (float) (Math.random() + Math.random() + 1.0D) * 0.15F;
        float f1 = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX = motionX / f1 * f * 0.4000000059604645D;
        motionY = motionY / f1 * f * 0.4000000059604645D + 0.10000000149011612D;
        motionZ = motionZ / f1 * f * 0.4000000059604645D;
        // Unterklassen: *0.1 und die uebergebene Bewegung dazu
        setDeltaMovement(motionX * 0.10000000149011612D + mx, motionY * 0.10000000149011612D + my, motionZ * 0.10000000149011612D + mz);
    }

    @Override
    //? if < 1.21.1 {
    protected void defineSynchedData() { }
    //?} else {
    /*protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override protected void readAdditionalSaveData(CompoundTag nbt) { particleAge = nbt.getShort("age"); }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { nbt.putShort("age", (short) particleAge); }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 25000; }

    //? if < 1.21.1 {
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
    //?}

    /** {@code Block.isNormalCube()}: undurchsichtiger Vollblock, der kein Signal liefert. */
    protected static boolean isNormalCube(Level w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        return s.isSolidRender(w, p) && !s.isSignalSource();
    }

    /** Position wie im Original ueber {@code (int)} abgeschnitten. */
    protected BlockPos intPos() { return new BlockPos((int) getX(), (int) getY(), (int) getZ()); }

    /** Gemeinsamer Ablauf von Chlor-, Wolken- und Pink-Wolken-FX. */
    protected void gasTick(int minAge, int ageRand, boolean serverKillOnly) {
        xo = getX(); yo = getY(); zo = getZ();

        if (maxAge < minAge) maxAge = random.nextInt(ageRand) + minAge;

        affect();

        particleAge++;
        if (particleAge >= maxAge) discard();

        Vec3 m = getDeltaMovement().scale(0.7599999785423279D);
        if (onGround()) m = new Vec3(m.x * 0.699999988079071D, m.y, m.z * 0.699999988079071D);

        BlockPos bp = BlockPos.containing(getX(), getY(), getZ());
        if (level().isRaining() && level().canSeeSky(bp)) m = m.add(0, -0.01, 0);

        double subdivisions = 4;
        double x = getX(), y = getY(), z = getZ();
        for (int i = 0; i < subdivisions; i++) {
            x += m.x / subdivisions;
            y += m.y / subdivisions;
            z += m.z / subdivisions;
            BlockPos p = new BlockPos((int) x, (int) y, (int) z);
            if (isNormalCube(level(), p)) {
                if ((!serverKillOnly || !level().isClientSide) && random.nextInt(5) != 0) discard();
                x -= m.x / subdivisions;
                y -= m.y / subdivisions;
                z -= m.z / subdivisions;
                m = Vec3.ZERO;
            }
            BlockPos q = new BlockPos((int) x, (int) y, (int) z);
            if (this instanceof PinkCloud && level().getBlockState(q).is(ModBlocks.RADIOREC.get())) {
                discard();
                level().setBlock(q, ModBlocks.BROADCASTER_PC.get().defaultBlockState(), 2);
            }
        }
        setPos(x, y, z);
        setDeltaMovement(m);
    }

    protected void affect() { }

    // ================================================================================================

    /** 1:1 {@code EntityChlorineFX}: 700-800 Ticks, vergiftet (auf beiden Seiten, wie im Original) alle 50 Ticks im Umkreis 2. */
    public static class Chlorine extends EntityModFX {
        public Chlorine(EntityType<? extends Chlorine> t, Level l) { super(t, l); }
        public Chlorine(Level l, double x, double y, double z, double mx, double my, double mz) { super(ModEntities.CHLORINE_FX.get(), l, x, y, z, mx, my, mz); }
        @Override public void tick() { super.tick(); gasTick(700, 101, false); }
        @Override protected void affect() {
            if (!level().isClientSide && random.nextInt(50) == 0) ExplosionChaos.poison(level(), (int) getX(), (int) getY(), (int) getZ(), 2);
        }
    }

    /** 1:1 {@code EntityCloudFX}: 900-1200 Ticks, {@code ExplosionChaos.c}. */
    public static class Cloud extends EntityModFX {
        public Cloud(EntityType<? extends Cloud> t, Level l) { super(t, l); }
        public Cloud(Level l, double x, double y, double z, double mx, double my, double mz) { super(ModEntities.CLOUD_FX.get(), l, x, y, z, mx, my, mz); }
        @Override public void tick() { super.tick(); gasTick(900, 301, true); }
        @Override protected void affect() {
            if (!level().isClientSide && random.nextInt(50) == 0) ExplosionChaos.c(level(), (int) getX(), (int) getY(), (int) getZ(), 2);
        }
    }

    /** 1:1 {@code EntityPinkCloudFX}: 900-1200 Ticks, {@code ExplosionChaos.pc}; macht aus einem Radioempfaenger den verseuchten Sender. */
    public static class PinkCloud extends EntityModFX {
        public PinkCloud(EntityType<? extends PinkCloud> t, Level l) { super(t, l); }
        public PinkCloud(Level l, double x, double y, double z, double mx, double my, double mz) { super(ModEntities.PINK_CLOUD_FX.get(), l, x, y, z, mx, my, mz); }
        @Override public void tick() { super.tick(); gasTick(900, 301, false); }
        @Override protected void affect() {
            if (!level().isClientSide && random.nextInt(50) == 0) ExplosionChaos.pc(level(), (int) getX(), (int) getY(), (int) getZ(), 2);
        }
    }

    /** 1:1 {@code EntityOrangeFX} (Agent Orange): faellt, entlaubt beim Aufschlag 3x3x3 und wird nie gespeichert. */
    public static class Orange extends EntityModFX {
        public Orange(EntityType<? extends Orange> t, Level l) { super(t, l); }
        public Orange(Level l, double x, double y, double z, double mx, double my, double mz) { super(ModEntities.ORANGE_FX.get(), l, x, y, z, mx, my, mz); }

        @Override
        public void tick() {
            super.tick();
            xo = getX(); yo = getY(); zo = getZ();
            if (maxAge < 900) maxAge = random.nextInt(301) + 900;
            if (!level().isClientSide && random.nextInt(50) == 0) ExplosionChaos.poison(level(), (int) getX(), (int) getY(), (int) getZ(), 2);
            particleAge++;
            if (particleAge >= maxAge) discard();

            Vec3 m = getDeltaMovement().scale(0.86D).add(0, -0.1, 0);
            double x = getX(), y = getY(), z = getZ();
            for (int i = 0; i < 4; i++) {
                x += m.x / 4; y += m.y / 4; z += m.z / 4;
                BlockPos p = new BlockPos((int) x, (int) y, (int) z);
                if (!level().getBlockState(p).isAir()) {
                    discard();
                    for (int a = -1; a < 2; a++) for (int b = -1; b < 2; b++) for (int c = -1; c < 2; c++) {
                        BlockPos q = p.offset(a, b, c);
                        if (level().getBlockState(q).is(Blocks.GRASS_BLOCK)) level().setBlock(q, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                        else ExplosionNukeGeneric.solinium(level(), q);
                    }
                }
            }
            setPos(x, y, z);
            setDeltaMovement(m);
        }

        @Override public boolean shouldBeSaved() { return false; }
        @Override protected void readAdditionalSaveData(CompoundTag nbt) { super.readAdditionalSaveData(nbt); discard(); }
    }
}
