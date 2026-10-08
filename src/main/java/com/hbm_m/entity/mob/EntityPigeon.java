package com.hbm_m.entity.mob;

import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.mob.ai.FlyingCreatureGoals;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityPigeon}: Taube mit Lauf-/Flugzustand. Fliegt zufaellig auf (oder bei Angriff/Feuer), haelt sich
 * hoechstens 10 Bloecke ueber dem Boden, frisst Brot und wird dick; dicke Tauben duengen im Flug den Boden unter sich
 * (1/50, Wollpartikel) und werden wieder schlank (1/10). Ein Treffer ueber doppelter Maximalgesundheit zerreisst sie
 * in 10 Federn. Kein Fallschaden, keine Laute.
 */
public class EntityPigeon extends PathfinderMob implements IFlyingCreature {

    private static final EntityDataAccessor<Byte> FLYING = SynchedEntityData.defineId(EntityPigeon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FAT = SynchedEntityData.defineId(EntityPigeon.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float dest;
    public float prevDest;
    public float prevFallTime;
    public float offGroundTimer = 1.0F;

    public EntityPigeon(EntityType<? extends EntityPigeon> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    protected void registerGoals() {
        Predicate<Mob> noFlyCondition = x -> ((EntityPigeon) x).getFlyingState() == IFlyingCreature.STATE_WALKING;
        this.goalSelector.addGoal(0, new FlyingCreatureGoals.StartFlying(this, this));
        this.goalSelector.addGoal(0, new FlyingCreatureGoals.StopFlying(this, this));
        this.goalSelector.addGoal(1, new FlyingCreatureGoals.SwimmingConditional(this, noFlyCondition));
        this.goalSelector.addGoal(2, new FlyingCreatureGoals.EatBread(this, 0.4D));
        this.goalSelector.addGoal(5, new FlyingCreatureGoals.WanderConditional(this, 0.2D, noFlyCondition));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        if (amount >= this.getMaxHealth() * 2 && !level().isClientSide) {
            this.discard();

            for (int i = 0; i < 10; i++) {
                Vec3 vec = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();

                ItemEntity feather = new ItemEntity(level(), getX() + vec.x, getY() + getBbHeight() / 2D + vec.y, getZ() + vec.z, new ItemStack(Items.FEATHER));
                feather.setDeltaMovement(vec.x * 0.5, vec.y * 0.5, vec.z * 0.5);
                level().addFreshEntity(feather);
            }

            return true;
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, (byte) 0);
        this.entityData.define(FAT, (byte) 0);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    /** Original {@code dropFewItems}: 0-2 (+ Pluenderung) Federn und ein (gebratenes) Huhn, dick drei. */
    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int j = this.random.nextInt(3) + this.random.nextInt(1 + looting);

        for (int k = 0; k < j; ++k) {
            this.spawnAtLocation(Items.FEATHER, 1);
        }

        if (this.isOnFire()) {
            this.spawnAtLocation(new ItemStack(Items.COOKED_CHICKEN, this.isFat() ? 3 : 1));
        } else {
            this.spawnAtLocation(new ItemStack(Items.CHICKEN, this.isFat() ? 3 : 1));
        }
    }

    @Override
    public int getFlyingState() {
        return this.entityData.get(FLYING);
    }

    @Override
    public void setFlyingState(int state) {
        this.entityData.set(FLYING, (byte) state);
    }

    public boolean isFat() {
        return this.entityData.get(FAT) == 1;
    }

    public void setFat(boolean fat) {
        this.entityData.set(FAT, (byte) (fat ? 1 : 0));
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
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.getFlyingState() == IFlyingCreature.STATE_FLYING) {
            int height = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(getX()), Mth.floor(getZ()));

            boolean ceil = getY() - height > 10;

            double motionY = this.getRandom().nextGaussian() * 0.05 + (ceil ? 0 : 0.04) + (this.isInWater() ? 0.2 : 0);

            if (onGround()) motionY = Math.abs(motionY) + 0.1D;

            Vec3 m = getDeltaMovement();
            this.setDeltaMovement(m.x, motionY, m.z);

            if (this.getRandom().nextInt(20) == 0) this.setYRot(this.getYRot() + (float) (this.getRandom().nextGaussian() * 30));

            if (this.isFat() && this.getRandom().nextInt(50) == 0) {

                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "sweat");
                nbt.putInt("count", 3);
                nbt.putInt("block", Block.getId(Blocks.WHITE_WOOL.defaultBlockState()));
                nbt.putInt("entity", getId());
                for (ServerPlayer p : ((ServerLevel) level()).players()) {
                    if (p.distanceToSqr(getX(), getY(), getZ()) < 50 * 50) com.hbm_m.network.AuxParticlePacket.sendTo(p, nbt, 0, 0, 0);
                }

                int x = Mth.floor(getX());
                int y = Mth.floor(getY()) - 1;
                int z = Mth.floor(getZ());

                for (int i = 0; i < 25; i++) {
                    BlockPos pos = new BlockPos(x, y - i, z);
                    if (fertilize((ServerLevel) level(), pos)) {
                        level().levelEvent(1505, pos, 0);
                        break;
                    }
                }

                if (this.getRandom().nextInt(10) == 0) {
                    this.setFat(false);
                }
            }

        } else if (!this.onGround() && getDeltaMovement().y < 0.0D) {
            Vec3 m = getDeltaMovement();
            this.setDeltaMovement(m.x, m.y * 0.8D, m.z);
        }
    }

    /** {@code ItemFertilizer.fertilize(..., force = true)}: waechst garantiert, wenn wachstumsfaehig. */
    private static boolean fertilize(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock growable) {
            if (growable.isValidBonemealTarget(world, pos, state, false)) {
                growable.performBonemeal(world, world.random, pos, state);
                return true;
            }
        }
        return false;
    }

    /** Original {@code moveForward = 1.5F} im Flug. */
    @Override
    public void travel(@NotNull Vec3 input) {
        if (this.getFlyingState() == IFlyingCreature.STATE_FLYING) input = new Vec3(input.x, input.y, 1.5F);
        super.travel(input);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.prevFallTime = this.fallTime;
        this.prevDest = this.dest;
        this.dest = (float) ((double) this.dest + (double) (this.onGround() ? -1 : 4) * 0.3D);

        if (this.dest < 0.0F) {
            this.dest = 0.0F;
        }

        if (this.dest > 1.0F) {
            this.dest = 1.0F;
        }

        if (!this.onGround() && this.offGroundTimer < 1.0F) {
            this.offGroundTimer = 1.0F;
        }

        this.offGroundTimer = (float) ((double) this.offGroundTimer * 0.9D);

        if (!this.onGround() && getDeltaMovement().y < 0.0D) {
            Vec3 m = getDeltaMovement();
            this.setDeltaMovement(m.x, m.y * 0.6D, m.z);
        }

        this.fallTime += this.offGroundTimer * 2.0F;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) { }
}
