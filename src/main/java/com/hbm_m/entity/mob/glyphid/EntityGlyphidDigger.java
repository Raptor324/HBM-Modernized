package com.hbm_m.entity.mob.glyphid;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.entity.projectile.RubbleEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityGlyphidDigger}: reisst alle sechs Sekunden einen Faecher Boden auf und schleudert ihn als Truemmer. */
public class EntityGlyphidDigger extends EntityGlyphid {

    protected Entity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;

    public EntityGlyphidDigger(EntityType<? extends EntityGlyphidDigger> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getDigger());
    }

    @Override
    public String getSkinName() {
        return "glyphid_digger";
    }

    @Override
    public double getGlyphidScale() {
        return 1.3D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsDigger;
    }

    public int timer = 0;

    @Override
    public void tick() {
        super.tick();
        Entity e = this.getEntityToAttack();
        if (e != null && this.isAlive()) {

            this.lastX = e.getX();
            this.lastY = e.getY();
            this.lastZ = e.getZ();

            if (--timer <= 0) {
                groundSlam();
                timer = 120;
            }
        }
    }

    /** {@code Library.getBlockPosInPath}. */
    private static List<int[]> getBlockPosInPath(int x, int y, int z, int length, Vec3 vec0) {
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i <= length; i++) {
            list.add(new int[] { (int) (x + (vec0.x * i)), y, (int) (z + (vec0.z * i)), i });
        }
        return list;
    }

    /** Mainly composed of crusty old power fist code, with some touch ups **/
    public void groundSlam() {
        if (!level().isClientSide && entityToAttack instanceof LivingEntity && this.distanceTo(entityToAttack) < 30) {
            Entity e = this.getEntityToAttack();
            boolean topAttack = false;

            int l = 6;
            float part = -1F / 16F;

            int bugX = (int) getX();
            int bugY = (int) getY();
            int bugZ = (int) getZ();

            Vec3 vec0 = getViewVector(1.0F);
            List<int[]> list = getBlockPosInPath(bugX, bugY, bugZ, l, vec0);

            for (int i = 0; i < 8; i++) {
                vec0 = vec0.yRot(part);
                list.addAll(getBlockPosInPath(bugX, bugY - 1, bugZ, l, vec0));
            }

            double velX = e.getX() - lastX;
            double velY = e.getY() - lastY;
            double velZ = e.getZ() - lastZ;

            if (this.lastTarget != e) {
                velX = velY = velZ = 0;
            }

            if (this.distanceTo(e) > 20) {
                topAttack = true;
            }

            int prediction = 60;
            Vec3 delta = new Vec3(e.getX() - getX() + velX * prediction, (e.getY() + e.getBbHeight() / 2) - (getY() + 1) + velY * prediction, e.getZ() - getZ() + velZ * prediction);
            double len = delta.length();
            if (len < 3) return;
            double targetYaw = -Math.atan2(delta.x, delta.z);

            double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            double y = delta.y;
            double v0 = 1.2;
            double v02 = v0 * v0;
            double g = 0.03D;
            double upperLower = topAttack ? 1 : -1;
            double targetPitch = Math.atan((v02 + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02)) * upperLower) / (g * x));
            Vec3 fireVec = null;
            if (!Double.isNaN(targetPitch)) {

                fireVec = new Vec3(v0, 0, 0).zRot((float) -targetPitch).yRot((float) -(targetYaw + Math.PI * 0.5));
            }

            float concrete = ModBlocks.CONCRETE.get().getExplosionResistance();

            for (int[] ints : list) {
                BlockPos pos = new BlockPos(ints[0], ints[1], ints[2]);
                BlockState b = level().getBlockState(pos);
                float k = b.getExplosionResistance(level(), pos, null);

                if (k < concrete && b.isRedstoneConductor(level(), pos) && level().getBlockEntity(pos) == null) {

                    RubbleEntity rubble = RubbleEntity.create(level(), pos.getX() + 0.5F, pos.getY() + 2, pos.getZ() + 0.5F, b);

                    if (fireVec != null) {
                        // Original EntityRubble.setThrowableHeading
                        double mx = fireVec.x, my = fireVec.y, mz = fireVec.z;
                        double tl = Math.sqrt(mx * mx + my * my + mz * mz);
                        float inacc = random.nextFloat();
                        mx = (mx / tl + level().random.nextGaussian() * 0.0075D * inacc) * v0;
                        my = (my / tl + level().random.nextGaussian() * 0.0075D * inacc) * v0;
                        mz = (mz / tl + level().random.nextGaussian() * 0.0075D * inacc) * v0;
                        rubble.setDeltaMovement(mx, my, mz);
                    }

                    level().addFreshEntity(rubble);
                    level().removeBlock(pos, false);
                }
            }
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    protected boolean canDig() {
        return true;
    }
}
