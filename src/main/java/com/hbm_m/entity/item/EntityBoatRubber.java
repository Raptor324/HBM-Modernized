package com.hbm_m.entity.item;

import java.util.List;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.entity.item.EntityBoatRubber}: Schlauchboot mit eigener Physik (nicht die des Vanilla-Boots).
 * Der Server rechnet, gelenkt wird ueber die Eingaben des Fahrers (vor/zurueck beschleunigt, seitwaerts dreht 3 Grad
 * pro Tick); Beschleunigungsfaktor 0.07 bis 0.5, Hoechstgeschwindigkeit 0.5 Bloecke/Tick. Zerbricht bei mehr als
 * 40 Schaden oder einem Sturz ueber 5 Bloecke und laesst dann {@code boat_rubber} fallen.
 * <p>
 * Im Original liegt {@code posY} in der Mitte der Hitbox ({@code yOffset = height / 2}); hier ist die Position wie
 * ueblich die Unterkante, alle Hoehen sind entsprechend um 0.3 verschoben.
 */
public class EntityBoatRubber extends Entity {

    private static final EntityDataAccessor<Integer> TIME_SINCE_HIT = SynchedEntityData.defineId(EntityBoatRubber.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORWARD_DIRECTION = SynchedEntityData.defineId(EntityBoatRubber.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE_TAKEN = SynchedEntityData.defineId(EntityBoatRubber.class, EntityDataSerializers.FLOAT);

    private double speedMultiplier = 0.07D;
    private int boatPosRotationIncrements;
    private double boatX;
    private double boatY;
    private double boatZ;
    private double boatYaw;
    private double boatPitch;

    public float prevRenderYaw;

    public EntityBoatRubber(EntityType<? extends EntityBoatRubber> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    public EntityBoatRubber(Level world, double x, double y, double z) {
        this(ModEntities.BOAT_RUBBER.get(), world);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TIME_SINCE_HIT, 0);
        this.entityData.define(FORWARD_DIRECTION, 1);
        this.entityData.define(DAMAGE_TAKEN, 0.0F);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    /** Original getCollisionBox(entity) = entity.boundingBox: das Boot ist fuer andere Wesen fest. */
    @Override
    public boolean canCollideWith(Entity entity) {
        return Boat.canVehicleCollide(this, entity);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    /** Original getMountedYOffset() = -0.3 (von der Mitte aus) - hier von der Unterkante. */
    @Override
    public double getPassengersRidingOffset() {
        return -0.15D;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level().isClientSide && !this.isRemoved()) {
            this.setForwardDirection(-this.getForwardDirection());
            this.setTimeSinceHit(10);
            this.setDamageTaken(this.getDamageTaken() + amount * 10.0F);
            this.markHurt();
            boolean hitByCreative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;

            if (hitByCreative || this.getDamageTaken() > 40.0F) {
                this.ejectPassengers();

                if (!hitByCreative) {
                    this.dropBoat();
                }

                this.discard();
            }
            return true;
        } else {
            return true;
        }
    }

    @Override
    public void animateHurt(float yaw) {
        this.setForwardDirection(-this.getForwardDirection());
        this.setTimeSinceHit(10);
        this.setDamageTaken(this.getDamageTaken() * 11.0F);
    }

    /** Original setPositionAndRotation2 (isBoatEmpty ist beim Schlauchboot immer true). */
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interp, boolean teleport) {
        this.boatPosRotationIncrements = interp;
        this.boatX = x;
        this.boatY = y;
        this.boatZ = z;
        this.boatYaw = yaw;
        this.boatPitch = pitch;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getTimeSinceHit() > 0) {
            this.setTimeSinceHit(this.getTimeSinceHit() - 1);
        }

        if (this.getDamageTaken() > 0.0F) {
            this.setDamageTaken(this.getDamageTaken() - 1.0F);
        }

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        byte b0 = 5;
        double d0 = 0.0D;
        AABB bb = this.getBoundingBox();

        for (int i = 0; i < b0; ++i) {
            double d1 = bb.minY + (bb.maxY - bb.minY) * (double) (i + 0) / (double) b0 - 0.125D;
            double d3 = bb.minY + (bb.maxY - bb.minY) * (double) (i + 1) / (double) b0 - 0.125D;

            if (isAABBInWater(new AABB(bb.minX, d1, bb.minZ, bb.maxX, d3, bb.maxZ))) {
                d0 += 1.0D / (double) b0;
            }
        }

        Vec3 motion = this.getDeltaMovement();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;

        if (this.level().isClientSide) {
            if (this.boatPosRotationIncrements > 0) {
                double x = this.getX() + (this.boatX - this.getX()) / (double) this.boatPosRotationIncrements;
                double y = this.getY() + (this.boatY - this.getY()) / (double) this.boatPosRotationIncrements;
                double z = this.getZ() + (this.boatZ - this.getZ()) / (double) this.boatPosRotationIncrements;
                double yaw = Mth.wrapDegrees(this.boatYaw - (double) this.getYRot());
                this.setYRot((float) ((double) this.getYRot() + yaw / (double) this.boatPosRotationIncrements));
                this.setXRot((float) ((double) this.getXRot() + (this.boatPitch - (double) this.getXRot()) / (double) this.boatPosRotationIncrements));
                --this.boatPosRotationIncrements;
                this.setPos(x, y, z);
            } else {
                double x = this.getX() + motionX;
                double y = this.getY() + motionY;
                double z = this.getZ() + motionZ;
                this.setPos(x, y, z);

                if (this.onGround()) {
                    motionX *= 0.5D;
                    motionY *= 0.5D;
                    motionZ *= 0.5D;
                }

                this.setDeltaMovement(motionX * 0.99D, motionY * 0.95D, motionZ * 0.99D);
            }
        } else {
            if (d0 < 1.0D) {
                double d2 = d0 * 2.0D - 1.0D;
                motionY += 0.04D * d2;
            } else {
                if (motionY < 0.0D) {
                    motionY /= 2.0D;
                }

                motionY += 0.007000000216066837D;
            }

            double prevSpeedSq = Math.sqrt(motionX * motionX + motionZ * motionZ);
            this.hasImpulse = false;
            Entity rider = this.getFirstPassenger();

            if (rider instanceof LivingEntity living) {
                if (living.zza != 0 || living.xxa != 0) {
                    Vec3 dir = new Vec3(0, 0, 1).yRot((float) -((this.getYRot() + 90) * Math.PI / 180D));
                    motionX += dir.x * this.speedMultiplier * living.zza * 0.05D;
                    motionZ += dir.z * this.speedMultiplier * living.zza * 0.05D;
                    float prevYaw = this.getYRot();
                    this.setYRot(this.getYRot() - living.xxa * 3);
                    Vec3 newMotion = new Vec3(motionX, 0, motionZ).yRot((float) (-(this.getYRot() - prevYaw) * Math.PI / 180D));
                    motionX = newMotion.x;
                    motionZ = newMotion.z;
                }
            } else {
                motionX *= 0.95D;
                motionY *= 0.95D;
                motionZ *= 0.95D;
            }

            double speedSq = Math.sqrt(motionX * motionX + motionZ * motionZ);

            if (speedSq > 0.5D) {
                double d4 = 0.5D / speedSq;
                motionX *= d4;
                motionZ *= d4;
                speedSq = 0.5D;
            }

            if (speedSq > prevSpeedSq && this.speedMultiplier < 0.5D) {
                this.speedMultiplier += (0.5D - this.speedMultiplier) / 50.0D;

                if (this.speedMultiplier > 0.5D) {
                    this.speedMultiplier = 0.5D;
                }
            } else {
                this.speedMultiplier -= (this.speedMultiplier - 0.07D) / 35.0D;

                if (this.speedMultiplier < 0.07D) {
                    this.speedMultiplier = 0.07D;
                }
            }

            boolean clearedCollision = false;

            for (int index = 0; index < 4; ++index) {
                int x = Mth.floor(this.getX() + ((double) (index % 2) - 0.5D) * 0.8D);
                int z = Mth.floor(this.getZ() + ((double) (index / 2) - 0.5D) * 0.8D);

                for (int yOff = 0; yOff < 2; ++yOff) {
                    // Original: floor(posY) mit posY = Mitte der Hitbox
                    int y = Mth.floor(this.getY() + 0.3D) + yOff;
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = this.level().getBlockState(pos);

                    if (state.is(Blocks.SNOW)) {
                        this.level().removeBlock(pos, false);
                        clearedCollision = true;
                    } else if (state.is(Blocks.LILY_PAD)) {
                        this.level().destroyBlock(pos, true);
                        clearedCollision = true;
                    }
                }
            }

            if (this.onGround()) {
                motionX *= 0.5D;
                motionY *= 0.5D;
                motionZ *= 0.5D;
            }

            this.setDeltaMovement(motionX, motionY, motionZ);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (clearedCollision) this.horizontalCollision = false;
            motion = this.getDeltaMovement();

            if (this.horizontalCollision && prevSpeedSq > 0.2D) {
                this.setDeltaMovement(motion.x * 0.25D, motion.y * 0.25D, motion.z * 0.25D);
            } else {
                this.setDeltaMovement(motion.x * 0.99D, motion.y * 0.95D, motion.z * 0.99D);
            }

            this.setXRot(0.0F);

            if (!(this.getFirstPassenger() instanceof LivingEntity)) {
                double yaw = this.getYRot();
                double deltaX = this.xo - this.getX();
                double deltaZ = this.zo - this.getZ();

                if (deltaX * deltaX + deltaZ * deltaZ > 0.001D) {
                    yaw = (double) ((float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI));
                }

                double rotationSpeed = Mth.wrapDegrees(yaw - (double) this.getYRot());

                if (rotationSpeed > 20.0D) {
                    rotationSpeed = 20.0D;
                }

                if (rotationSpeed < -20.0D) {
                    rotationSpeed = -20.0D;
                }

                this.setYRot((float) ((double) this.getYRot() + rotationSpeed));
            }

            this.setRot(this.getYRot(), this.getXRot());

            List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2D, 0.0D, 0.2D));

            for (Entity entity : list) {
                if (!this.hasPassenger(entity) && entity.isPushable() && (entity instanceof EntityBoatRubber || entity instanceof Boat)) {
                    entity.push(this);
                }
            }
        }

        double moX = this.xo - this.getX();
        double moZ = this.zo - this.getZ();
        double prevSpeedSq = Math.sqrt(moX * moX + moZ * moZ);

        if (prevSpeedSq > 0.2625D && this.level().isClientSide) {
            double cosYaw = Math.cos(this.getYRot() * Math.PI / 180.0D);
            double sinYaw = Math.sin(this.getYRot() * Math.PI / 180.0D);

            for (double j = 0; j < 1.0D + prevSpeedSq * 60.0D; ++j) {
                double offset = (double) (this.random.nextFloat() * 2.0F - 1.0F);
                double side = (double) (this.random.nextInt(2) * 2 - 1) * 0.7D;
                double magX;
                double magZ;
                // Original: posY - 0.125 mit posY = Mitte der Hitbox
                double py = this.getY() + 0.3D - 0.125D;

                if (this.random.nextBoolean()) {
                    magX = this.getX() - cosYaw * offset * 0.8D + sinYaw * side;
                    magZ = this.getZ() - sinYaw * offset * 0.8D - cosYaw * side;
                } else {
                    magX = this.getX() + cosYaw + sinYaw * offset * 0.7D;
                    magZ = this.getZ() + sinYaw - cosYaw * offset * 0.7D;
                }
                this.level().addParticle(ParticleTypes.SPLASH, magX, py, magZ, moX, 0.1, moZ);
            }
        }
    }

    /** World.isAABBInMaterial(bb, Material.water) inkl. Fuellhoehe. */
    private boolean isAABBInWater(AABB bb) {
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.floor(bb.maxX + 1.0D);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.floor(bb.maxY + 1.0D);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.floor(bb.maxZ + 1.0D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    pos.set(x, y, z);
                    FluidState fluid = this.level().getFluidState(pos);
                    if (fluid.is(FluidTags.WATER) && (double) y + fluid.getHeight(this.level(), pos) >= bb.minY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (this.hasPassenger(passenger)) {
            double offX = Math.cos((double) this.getYRot() * Math.PI / 180.0D) * 0.4D;
            double offZ = Math.sin((double) this.getYRot() * Math.PI / 180.0D) * 0.4D;
            move.accept(passenger, this.getX() + offX, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + offZ);

            if (passenger instanceof Player player) {
                player.yBodyRot = Mth.wrapDegrees(this.getYRot() + 90F);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        Entity rider = this.getFirstPassenger();

        if (rider instanceof Player && rider != player) {
            return InteractionResult.SUCCESS;
        } else {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
    }

    /** Original updateFallState: Sturz ueber 5 Bloecke zerstoert das Boot. */
    @Override
    protected void checkFallDamage(double fall, boolean onGround, BlockState state, BlockPos pos) {
        if (onGround) {
            if (this.fallDistance > 5.0F) {
                this.causeFallDamage(this.fallDistance, 1.0F, this.damageSources().fall());

                if (!this.level().isClientSide && !this.isRemoved()) {
                    this.discard();
                    this.dropBoat();
                }

                this.fallDistance = 0.0F;
            }
        } else if (!this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && fall < 0.0D) {
            this.fallDistance = (float) ((double) this.fallDistance - fall);
        }
    }

    public void dropBoat() {
        this.spawnAtLocation(ModItems.BOAT_RUBBER.get(), 0);
    }

    public void setDamageTaken(float amount) {
        this.entityData.set(DAMAGE_TAKEN, amount);
    }

    public float getDamageTaken() {
        return this.entityData.get(DAMAGE_TAKEN);
    }

    public void setTimeSinceHit(int time) {
        this.entityData.set(TIME_SINCE_HIT, time);
    }

    public int getTimeSinceHit() {
        return this.entityData.get(TIME_SINCE_HIT);
    }

    public void setForwardDirection(int dir) {
        this.entityData.set(FORWARD_DIRECTION, dir);
    }

    public int getForwardDirection() {
        return this.entityData.get(FORWARD_DIRECTION);
    }
}
