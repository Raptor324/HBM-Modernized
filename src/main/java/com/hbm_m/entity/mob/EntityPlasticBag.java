package com.hbm_m.entity.mob;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.item.EntityItemBuoyant;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityPlasticBag} ("Copy-pasted shit from the squid class"): treibt wie ein Tintenfisch durchs Meer,
 * jeder Treffer laesst sie als schwimmende Plastiktuete zerfallen. Spawnt nur zwischen Y 45 und 63 (1/10).
 */
public class EntityPlasticBag extends WaterAnimal {

    public float rotation;
    public float prevRotation;
    private float randomMotionSpeed;
    private float rotationVelocity;
    private float randomMotionVecX;
    private float randomMotionVecY;
    private float randomMotionVecZ;

    public EntityPlasticBag(EntityType<? extends EntityPlasticBag> type, Level world) {
        super(type, world);
        this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        if (!level().isClientSide) {
            this.discard();
            this.spawnAtLocation(ModItems.PLASTIC_BAG.get(), 1);
        }

        return true;
    }

    /** Original {@code entityDropItem}: als {@link EntityItemBuoyant}, Aufhebeverzoegerung 10. */
    @Nullable
    @Override
    public ItemEntity spawnAtLocation(@NotNull ItemStack stack, float offset) {
        if (!stack.isEmpty() && !level().isClientSide) {
            EntityItemBuoyant entityitem = new EntityItemBuoyant(level(), getX(), getY() + (double) offset, getZ(), stack);
            entityitem.setPickUpDelay(10);
            if (this.captureDrops() != null) {
                this.captureDrops().add(entityitem);
            } else {
                level().addFreshEntity(entityitem);
            }
            return entityitem;
        } else {
            return null;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    /** Original: Wasser im um 0.6 nach unten verkleinerten Kasten. */
    @Override
    public boolean isInWater() {
        AABB bb = this.getBoundingBox().inflate(0.0D, -0.6D, 0.0D);
        int minX = Mth.floor(bb.minX), maxX = Mth.ceil(bb.maxX), minY = Mth.floor(bb.minY), maxY = Mth.ceil(bb.maxY), minZ = Mth.floor(bb.minZ), maxZ = Mth.ceil(bb.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; x++)
            for (int y = minY; y < maxY; y++)
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    var fluid = level().getFluidState(pos);
                    if (fluid.is(FluidTags.WATER) && y + fluid.getHeight(level(), pos) >= bb.minY) return true;
                }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.prevRotation = this.rotation;
        this.rotation += this.rotationVelocity;

        if (this.rotation > ((float) Math.PI * 2F)) {
            this.rotation -= ((float) Math.PI * 2F);

            if (this.random.nextInt(10) == 0) {
                this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
            }
        }

        Vec3 m = getDeltaMovement();
        if (this.isInWater()) {
            float f;

            if (this.rotation < (float) Math.PI) {
                f = this.rotation / (float) Math.PI;

                if ((double) f > 0.75D) {
                    this.randomMotionSpeed = 0.1F;
                }
            } else {
                this.randomMotionSpeed *= 0.999F;
            }

            if (!level().isClientSide) {
                m = new Vec3(this.randomMotionVecX * this.randomMotionSpeed, this.randomMotionVecY * this.randomMotionSpeed, this.randomMotionVecZ * this.randomMotionSpeed);
                this.setDeltaMovement(m);
            }

            double f2 = Math.sqrt(m.x * m.x + m.z * m.z);
            this.yBodyRot += (-((float) Math.atan2(m.x, m.z)) * 180.0F / (float) Math.PI - this.yBodyRot) * 0.1F;
            this.setYRot(this.yBodyRot);
            this.setXRot((float) (Math.atan2(m.y, f2) * 180.0D / Math.PI));
        } else {
            if (!level().isClientSide) {
                this.setDeltaMovement(0.0D, (m.y - 0.08D) * 0.98D, 0.0D);
            }
        }
    }

    @Override
    public void travel(@NotNull Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    protected void customServerAiStep() {
        this.noActionTime++;

        if (this.noActionTime > 100) {
            this.randomMotionVecX = this.randomMotionVecY = this.randomMotionVecZ = 0.0F;
        } else if (this.random.nextInt(50) == 0 || !this.wasTouchingWater || this.randomMotionVecX == 0.0F && this.randomMotionVecY == 0.0F && this.randomMotionVecZ == 0.0F) {
            float f = this.random.nextFloat() * (float) Math.PI * 2.0F;
            this.randomMotionVecX = Mth.cos(f) * 0.2F;
            this.randomMotionVecY = -0.1F + this.random.nextFloat() * 0.2F;
            this.randomMotionVecZ = Mth.sin(f) * 0.2F;
        }
    }

    /** Original {@code getCanSpawnHere}: nur zwischen Y 45 und 63, 1/10. */
    public static boolean checkBagSpawnRules(EntityType<EntityPlasticBag> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return pos.getY() > 45 && pos.getY() < 63 && random.nextInt(10) == 0 && WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, reason, pos, random);
    }
}
