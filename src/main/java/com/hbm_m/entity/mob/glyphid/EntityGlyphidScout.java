package com.hbm_m.entity.mob.glyphid;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.MobConfig;
import com.hbm_m.entity.logic.EntityWaypoint;
import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.world.feature.GlyphidHive;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityGlyphidScout}: sucht im Umkreis von 45 Bloecken (Terraforming mit Nuklearglyphide: 60) einen
 * Bauplatz, holt Verstaerkung und baut dort nach fuenf Takten ein neues Nest. Auf Basalt wird ein grosses Nest
 * angestrebt.
 */
public class EntityGlyphidScout extends EntityGlyphid {

    boolean hasTarget = false;
    int timer;
    int scoutingRange = 45;
    int minDistanceToHive = 8;
    boolean useLargeHive = false;
    float largeHiveChance = MobConfig.largeHiveChance();

    public EntityGlyphidScout(EntityType<? extends EntityGlyphidScout> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getScout());
    }

    //extreme measures for anti-scout bullying
    @Override
    public boolean doHurtTarget(Entity victum) {
        if (super.doHurtTarget(victum) && victum instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 10 * 20, 3));
            return true;
        }
        return false;
    }

    @Override
    public String getSkinName() {
        return "glyphid_scout";
    }

    @Override
    public double getGlyphidScale() {
        return 0.75D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsScout;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount, 2), 100);
    }

    @Override
    public void tick() {
        super.tick();

        //Updates to check whether the player still exists, important to make sure it wont stop doing work
        if (entityToAttack != null && tickCount % 60 == 0) {
            setEntityToAttack(findPlayerToAttack());
        }

        if ((getCurrentTask() != TASK_BUILD_HIVE || getCurrentTask() != TASK_TERRAFORM) && taskWaypoint == null) {

            if (MobConfig.rampantGlyphidGuidance() && PollutionHandler.targetCoords != null) {

                if (!hasTarget) {
                    Vec3 dirVec = playerBaseDirFinder(new Vec3(getX(), getY(), getZ()), getPlayerTargetDirection());

                    EntityWaypoint target = new EntityWaypoint(level());
                    target.moveTo(dirVec.x, dirVec.y, dirVec.z, 0, 0);
                    target.maxAge = 300;
                    target.radius = 6;
                    target.setWaypointType(TASK_BUILD_HIVE);
                    level().addFreshEntity(target);
                    hasTarget = true;

                    setCurrentTask(TASK_RETREAT_FOR_REINFORCEMENTS, target);
                }

                if (super.isAtDestination()) {
                    setCurrentTask(TASK_BUILD_HIVE, null);
                    hasTarget = false;
                }

            } else {
                setCurrentTask(TASK_BUILD_HIVE, null);
            }
        }

        if (getCurrentTask() == TASK_BUILD_HIVE || getCurrentTask() == TASK_TERRAFORM) {

            if (!level().isClientSide && !hasTarget) {

                //Check for whether a big man johnson is nearby, this makes the scout switch into its terraforming task
                if (scoutingRange != 60 && hasNuclearGlyphidNearby()) {
                    setCurrentTask(TASK_TERRAFORM, null);
                }

                if (expandHive()) {
                    this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 180 * 20, 1));
                    hasTarget = true;
                }
            }

            //fixes edge case where glyphids have no task and yet hasTarget is true
            if (taskWaypoint == null && hasTarget) {
                hasTarget = false;
            }

            if (getCurrentTask() == TASK_TERRAFORM && super.isAtDestination() && canBuildHiveHere()) {
                communicate(TASK_TERRAFORM, taskWaypoint);
            }

            if (tickCount % 10 == 0 && isAtDestination()) {
                timer++;

                if (!level().isClientSide && canBuildHiveHere()) {

                    if (timer == 1) {

                        EntityWaypoint additional = new EntityWaypoint(level());
                        additional.moveTo(getX(), getY(), getZ(), 0, 0);
                        additional.setWaypointType(TASK_IDLE);

                        // First, go home and get reinforcements
                        EntityWaypoint home = new EntityWaypoint(level());
                        home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
                        home.setAdditionalWaypoint(additional);
                        home.moveTo(homeX, homeY, homeZ, 0, 0);
                        home.maxAge = 1200;
                        home.radius = 6;

                        level().addFreshEntity(home);

                        this.taskWaypoint = home;
                        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 * 20, 10));
                        communicate(TASK_RETREAT_FOR_REINFORCEMENTS, taskWaypoint);

                    } else if (timer >= 5) {

                        level().explode(this, getX(), getY(), getZ(), 5F, false, Level.ExplosionInteraction.NONE);
                        GlyphidHive.generateSmall(level(), Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ()), random, this.getSubtype() != TYPE_NORMAL, false);
                        this.discard();

                    } else {
                        communicate(TASK_FOLLOW, taskWaypoint);
                    }
                }
            }
        }
    }

    /** Returns true if the position is far enough away from other hives. Also resets the task if unsuccessful. */
    public boolean canBuildHiveHere() {
        int length = useLargeHive ? 16 : 8;

        for (int i = 0; i < 8; i++) {

            float angle = (float) Math.toRadians(360D / 16 * i);
            Vec3 rot = new Vec3(0, 0, length).yRot(angle);
            Vec3 pos = new Vec3(this.getX(), this.getY() + 1, this.getZ());
            Vec3 nextPos = new Vec3(this.getX() + rot.x, this.getY() + 1, this.getZ() + rot.z);
            BlockHitResult mop = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (mop.getType() == HitResult.Type.BLOCK) {

                BlockState block = level().getBlockState(mop.getBlockPos());

                if (block.is(ModBlocks.GLYPHID_BASE.get())) {
                    setCurrentTask(TASK_IDLE, null);
                    hasTarget = false;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isAtDestination() {
        return this.getCurrentTask() == TASK_BUILD_HIVE && super.isAtDestination();
    }

    public boolean hasNuclearGlyphidNearby() {
        int radius = 8;

        AABB bb = new AABB(this.getX() - radius, this.getY() - radius, this.getZ() - radius, this.getX() + radius, this.getY() + radius, this.getZ() + radius);

        List<Entity> bugs = level().getEntities(this, bb);

        for (Entity e : bugs) {
            if (e instanceof EntityGlyphidNuclear) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean expandHive() {

        int nestX = random.nextInt((homeX + scoutingRange) - (homeX - scoutingRange)) + (homeX - scoutingRange);
        int nestZ = random.nextInt((homeZ + scoutingRange) - (homeZ - scoutingRange)) + (homeZ - scoutingRange);
        int nestY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, nestX, nestZ);
        BlockPos below = new BlockPos(nestX, nestY - 1, nestZ);
        BlockState b = level().getBlockState(below);

        boolean distanceCheck = new Vec3(nestX - homeX, nestY - homeY, nestZ - homeZ).length() > minDistanceToHive;

        if (distanceCheck && !b.isAir() && b.isRedstoneConductor(level(), below) && !b.is(ModBlocks.GLYPHID_BASE.get())) {

            if (b.is(ModBlocks.BASALT.get())) {
                useLargeHive = true;
                largeHiveChance /= 2;
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 3));
            }

            if (!level().isClientSide) {
                EntityWaypoint nest = new EntityWaypoint(level());
                nest.setWaypointType(getCurrentTask());
                nest.radius = 5;

                if (useLargeHive)
                    nest.setHighPriority();

                nest.moveTo(nestX, nestY, nestZ, 0, 0);
                level().addFreshEntity(nest);

                taskWaypoint = nest;

                // updates the task coordinates
                setCurrentTask(getCurrentTask(), taskWaypoint);
                communicate(TASK_BUILD_HIVE, taskWaypoint);
            }
            return true;
        }

        return false;
    }

    @Override
    public void carryOutTask() {
        if (!level().isClientSide && taskWaypoint == null) {
            switch (getCurrentTask()) {
                case TASK_INITIATE_RETREAT:

                    this.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 4));

                    //then, come back later
                    EntityWaypoint additional = new EntityWaypoint(level());
                    additional.moveTo(getX(), getY(), getZ(), 0, 0);
                    additional.setWaypointType(0);

                    //First, go home and get reinforcements
                    EntityWaypoint home = new EntityWaypoint(level());
                    home.setWaypointType(2);
                    home.setAdditionalWaypoint(additional);
                    home.setHighPriority();
                    home.radius = 6;
                    home.moveTo(homeX, homeY, homeZ, 0, 0);
                    level().addFreshEntity(home);

                    communicate(4, home);
                    break;

                //terraforming task, only used if a big man johnson is near the scout
                case TASK_TERRAFORM:
                    scoutingRange = 60;
                    minDistanceToHive = 20;
                    break;
                default:
                    break;
            }
        }
        super.carryOutTask();
    }

    @Override
    public boolean useExtendedTargeting() {
        return false;
    }

    @Nullable
    @Override
    protected Entity findPlayerToAttack() {
        if (this.hasEffect(MobEffects.BLINDNESS)) return null;
        //no extended targeting, and a low attack distance, ensures the scouts are focused in expanding, and not in chasing the player
        return closestVulnerablePlayer(10);
    }

    ///RAMPANT MODE STUFFS

    /** Finds the direction from the bug's location to the target and adds it to their current coord
     * Used as a performant way to make scouts expand toward the player's spawn point
     * @return An adjusted direction vector, to be added into the bug's current position for it to path in the required direction**/
    public static Vec3 playerBaseDirFinder(Vec3 currentLocation, Vec3 target) {
        Vec3 dirVec = target.subtract(currentLocation).normalize();
        return new Vec3(
                currentLocation.x + dirVec.x * 10,
                currentLocation.y + dirVec.y * 10,
                currentLocation.z + dirVec.z * 10
        );
    }

    protected Vec3 getPlayerTargetDirection() {
        Player player = level().getNearestPlayer(this, 300);
        if (player != null) {
            return new Vec3(player.getX(), player.getY(), player.getZ());
        }
        return PollutionHandler.targetCoords;
    }

    /** Vanilla implementation, minus the RNG */
    public boolean isValidLightLevel() {
        BlockPos pos = BlockPos.containing(this.getX(), this.getBoundingBox().minY, this.getZ());
        int darken = level().isThundering() ? 10 : level().getSkyDarken();
        int light = level().getMaxLocalRawBrightness(pos, darken);
        return light <= 7;
    }
}
