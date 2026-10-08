package com.hbm_m.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBurrowingBase}: Basis fuer "grabende" Wesen - kein Fallverhalten, keine Kollision (noClip) und eine
 * einfache Bewegung mit unterschiedlichem Luftwiderstand im Boden und in der Luft.
 */
public abstract class EntityBurrowingBase extends PathfinderMob {

    protected float airDrag;
    protected float airDragY;
    protected float groundDrag;
    protected float groundDragY;

    protected EntityBurrowingBase(EntityType<? extends EntityBurrowingBase> type, Level world) {
        super(type, world);
        this.noPhysics = true;
        this.airDrag = 0.995F;
        this.airDragY = 0.997F;
        this.groundDrag = 0.98F;
        this.groundDragY = 0.995F;
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = true;
    }

    /** Grabende Wesen drehen sich mit der Bewegung, daher Augenhoehe in der Mitte. */
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {

        if (this.isInvulnerableTo(source) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)) {
            return false;
        }

        return super.hurt(source, amount);
    }

    /** Kein Fallschaden und kein Fallzustand. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) { }

    /**
     * Original {@code moveEntityWithHeading}: Bewegung nach Blickrichtung aufaddieren, bewegen, dann je nach Lage
     * (im Boden/in der Luft) bremsen.
     */
    @Override
    public void travel(Vec3 travelVector) {

        float drag = this.groundDrag;
        float dragY = this.groundDragY;

        if (!isInsideOpaqueBlock() && !isInWater() && !isInLava()) {
            drag = this.airDrag;
            dragY = this.airDragY;
        }

        this.moveRelative(0.02F, travelVector);
        this.move(MoverType.SELF, this.getDeltaMovement());

        Vec3 m = this.getDeltaMovement();
        this.setDeltaMovement(m.x * drag, m.y * dragY, m.z * drag);
    }

    /** Keine Leitern. */
    @Override
    public boolean onClimbable() {
        return false;
    }

    /** Ob das Wesen durch die Luft fliegen kann. */
    public boolean canFly() {
        return false;
    }

    /** Ob senkrechte Bewegung moeglich ist (sonst wirkt die Schwerkraft). */
    protected boolean canSupportMovement() {
        return isInsideOpaqueBlock() || canFly();
    }

    /**
     * {@code isEntityInsideOpaqueBlock} der 1.7.10 - Vanillas {@code isInWall()} liefert bei noPhysics immer false.
     * Prueft acht Punkte um die Augenhoehe auf volle Bloecke.
     */
    public boolean isInsideOpaqueBlock() {
        for (int i = 0; i < 8; ++i) {
            double dx = ((float) ((i >> 0) % 2) - 0.5F) * this.getBbWidth() * 0.8F;
            double dy = ((float) ((i >> 1) % 2) - 0.5F) * 0.1F;
            double dz = ((float) ((i >> 2) % 2) - 0.5F) * this.getBbWidth() * 0.8F;
            BlockPos pos = BlockPos.containing(this.getX() + dx, this.getEyeY() + dy, this.getZ() + dz);
            BlockState state = this.level().getBlockState(pos);
            if (state.isRedstoneConductor(this.level(), pos)) return true;
        }
        return false;
    }
}
