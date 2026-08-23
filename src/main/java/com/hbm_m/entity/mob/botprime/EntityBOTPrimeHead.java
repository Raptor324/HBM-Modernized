package com.hbm_m.entity.mob.botprime;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityBOTPrimeHead}.
 *
 * <p>The head owns the health pool and the seventy-four body segments, which it spawns on first
 * placement. When it has no target it patrols a 100x60x100 box around wherever it was spawned;
 * when it does, it dives at the target - but only from below, surfacing and re-burrowing as it
 * goes, which is what {@code wasNearGround} tracks.</p>
 */
public class EntityBOTPrimeHead extends EntityBOTPrimeBase {

    /** {@code for(int i = 0; i < 74; i++)} - the length of the worm. */
    public static final int SEGMENT_COUNT = 74;

    private boolean spawnedSegments;

    public EntityBOTPrimeHead(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
        this.wasNearGround = false;
        this.attackRange = 150.0D;
        this.maxSpeed = 1.0D;
        this.fallSpeed = 0.006D;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean getIsHead() {
        return true;
    }

    @Override
    public float getAttackStrength(@NotNull Entity target) {
        return 1000F;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (super.hurt(source, amount)) {
            this.dmgCooldown = 10;
            return true;
        }
        return false;
    }

    /** {@code onSpawnWithEgg}: the head brings its own body with it. */
    public void spawnSegments() {
        if (this.spawnedSegments || this.level().isClientSide) return;
        this.spawnedSegments = true;

        this.setHeadID(this.getId());
        BlockPos pos = this.blockPosition();

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            EntityBOTPrimeBody body = ModEntities.BOT_PRIME_BODY.get().create(this.level());
            if (body == null) continue;
            body.setPartNumber(i);
            body.setPos(pos.getX(), pos.getY(), pos.getZ());
            body.setHeadID(this.getId());
            this.level().addFreshEntity(body);
        }

        this.spawnPoint = pos;
    }

    @Override
    public void tick() {
        super.tick();

        // The head cannot spawn its tail from the constructor - it has no id and no level yet.
        if (!this.level().isClientSide && !this.spawnedSegments) {
            spawnSegments();
        }

        // The head points along its own motion vector rather than at anything.
        var motion = this.getDeltaMovement();
        float flat = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, flat) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    @Override
    protected void customServerAiStep() {
        updateActionState();
        super.customServerAiStep();
        updateHeadMovement();
        updateBossBar();

        // Regenerates slowly while hunting, fast while left alone.
        if (this.getHealth() < this.getMaxHealth() && this.tickCount % 6 == 0) {
            if (this.targetedEntity != null) {
                this.heal(1.0F);
            } else if (this.getLastHurtByMobTimestamp() + 100 < this.tickCount) {
                this.heal(4.0F);
            }
        }

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < this.attackRange * this.attackRange
                && canSeeThroughNonSolids(this.targetedEntity)) {
            this.attackCounter++;
            if (this.attackCounter == 30) {
                laserAttack(this.targetedEntity, true);
                this.attackCounter = 0;
            }
        } else {
            this.attackCounter = 0;
        }
    }

    /**
     * {@code updateHeadMovement}: steers toward the waypoint on a jittery cooldown, sinks while
     * buried, and picks a new patrol point when it has nothing to chase.
     */
    protected void updateHeadMovement() {
        double dx = this.waypointX - this.getX();
        double dy = this.waypointY - this.getY();
        double dz = this.waypointZ - this.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown += this.random.nextInt(5) + 2;
            double dist = Math.sqrt(distSq);

            var motion = this.getDeltaMovement();
            if (motion.lengthSqr() < this.maxSpeed && dist > 1.0E-4D) {
                // Buried, it steers eight times more sluggishly - it has rock to push through.
                if (!isCourseTraversable()) dist *= 8.0D;

                var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
                double moverSpeed = speedAttr != null ? speedAttr.getBaseValue() : 0.15D;
                this.setDeltaMovement(motion.add(
                        dx / dist * moverSpeed, dy / dist * moverSpeed, dz / dist * moverSpeed));
            }
        }

        if (!isCourseTraversable()) {
            this.setDeltaMovement(this.getDeltaMovement().subtract(0, this.fallSpeed, 0));
        }

        if (this.dmgCooldown > 0) this.dmgCooldown--;
        this.aggroCooldown--;

        if (this.getTarget() != null) {
            if (this.aggroCooldown <= 0) {
                this.targetedEntity = this.getTarget();
                this.aggroCooldown = 20;
            }
        } else if (this.targetedEntity == null) {
            this.waypointX = this.spawnPoint.getX() - 50 + this.random.nextInt(100);
            this.waypointY = this.spawnPoint.getY() - 30 + this.random.nextInt(60);
            this.waypointZ = this.spawnPoint.getZ() - 50 + this.random.nextInt(100);
        }

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < this.attackRange * this.attackRange) {
            if (this.wasNearGround || this.canFly) {
                // Surfaced: go straight for them, and occasionally dive again.
                this.waypointX = this.targetedEntity.getX();
                this.waypointY = this.targetedEntity.getY();
                this.waypointZ = this.targetedEntity.getZ();

                if (this.random.nextInt(80) == 0 && this.getY() > this.surfaceY && !isCourseTraversable()) {
                    this.wasNearGround = false;
                }
            } else {
                // Buried: track them from deep down until it is low enough to surface.
                this.waypointX = this.targetedEntity.getX();
                this.waypointY = 10.0D;
                this.waypointZ = this.targetedEntity.getZ();

                if (this.getY() < 15.0D) this.wasNearGround = true;
            }
        }
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (this.level().isClientSide) return;

        // 200 blocks, and the coin goes straight into the inventory rather than on the floor -
        // the original does not trust you to find it wherever the head happened to die.
        ModAdvancements.grantNearby(this, 200D, ModAdvancements.BOSS_WORM);
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(200D))) {
            ItemStack coin = new ItemStack(ModItems.COIN_WORM.get());
            if (!player.getInventory().add(coin)) player.drop(coin, false);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("spawnX", this.spawnPoint.getX());
        tag.putInt("spawnY", this.spawnPoint.getY());
        tag.putInt("spawnZ", this.spawnPoint.getZ());
        tag.putBoolean("spawnedSegments", this.spawnedSegments);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.spawnPoint = new BlockPos(tag.getInt("spawnX"), tag.getInt("spawnY"), tag.getInt("spawnZ"));
        // Without this a reloaded head would spawn a second full-length body every time.
        this.spawnedSegments = tag.getBoolean("spawnedSegments");
    }

    /** Used by the spawn egg path so the tail exists the moment the head does. */
    public static void spawnWithBody(ServerLevel level, BlockPos pos) {
        EntityBOTPrimeHead head = ModEntities.BOT_PRIME_HEAD.get().create(level);
        if (head == null) return;
        head.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(head);
        head.spawnSegments();
    }

    // ─── Boss bar ────────────────────────────────────────────────────────────
    // The original calls BossStatus.setBossStatus from its renderer every frame, which is how
    // 1.7.10 did boss bars. 1.20 has a real server-side ServerBossEvent instead, so the bar is
    // driven from the entity and correctly disappears when it dies or unloads.

    private final net.minecraft.server.level.ServerBossEvent bossEvent =
            new net.minecraft.server.level.ServerBossEvent(this.getDisplayName(),
                    net.minecraft.world.BossEvent.BossBarColor.PURPLE,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);

    @Override
    public void startSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@org.jetbrains.annotations.Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    /** Keeps the bar in step with the health bar; call once per server tick. */
    protected void updateBossBar() {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

}
