package com.hbm_m.entity.mob;

import com.hbm_m.platform.PlatformHooks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * BOW. 1:1 {@code EntityQuackos}: die 25-fache Ente. Unverwundbar, laesst sich nicht toeten (Gesundheit bleibt
 * voll, serverseitiges Entfernen wird verweigert), reitbar, faellt sie unter Y -30, kehrt sie bei Y 256 zurueck.
 * Nur Erbsen ({@code despawn}) vertreiben sie: 150 Pilzwolken und 3 goldene Eier.
 */
public class EntityQuackos extends EntityDuck implements IDespawnable {

    private boolean despawning = false;

    public EntityQuackos(EntityType<? extends Chicken> type, Level world) {
        super(type, world);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HbmSoundsNT.get("hbm:entity.megaquacc");
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return HbmSoundsNT.get("hbm:entity.megaquacc");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundsNT.get("hbm:entity.megaquacc");
    }

    @Override
    public Chicken getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob mate) {
        return ModEntities.QUACKOS.get().create(level);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return true;
    }

    /** prank'd */
    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (level().isClientSide || despawning || reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER || reason == RemovalReason.CHANGED_DIMENSION)
            super.remove(reason);
    }

    /** prank'd */
    @Override
    public void setHealth(float f) {
        super.setHealth(this.getMaxHealth());
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult res = super.mobInteract(player, hand);
        if (res.consumesAction()) return res;

        if (!level().isClientSide && (this.getFirstPassenger() == null || this.getFirstPassenger() == player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void despawn() {

        if (!level().isClientSide) {
            for (int i = 0; i < 150; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "bf");
                double px = getX() + random.nextDouble() * 20 - 10, py = getY() + random.nextDouble() * 25, pz = getZ() + random.nextDouble() * 20 - 10;
                for (ServerPlayer p : ((ServerLevel) level()).players()) {
                    if (p.distanceToSqr(getX(), getY(), getZ()) < 150 * 150) com.hbm_m.network.AuxParticlePacket.sendTo(p, data, px, py, pz);
                }
            }

            this.spawnAtLocation(ModItems.SPAWN_DUCK.get(), 3);
        }
        this.despawning = true;
        this.discard();
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move) {
        float f = Mth.sin(this.yBodyRot * (float) Math.PI / 180.0F);
        float f1 = Mth.cos(this.yBodyRot * (float) Math.PI / 180.0F);
        float f2 = 0.1F;
        float f3 = 0.0F;
        move.accept(passenger, getX() + (double) (f2 * f), getY() + (double) (this.getBbHeight() - 0.125F) + PlatformHooks.getMyRidingOffset(passenger) + (double) f3, getZ() - (double) (f2 * f1));

        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = this.yBodyRot;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide && getY() < -30) {
            this.setPos(getX() + random.nextGaussian() * 30, 256, getZ() + random.nextGaussian() * 30);
        }
    }

    @Override
    public void die(@NotNull DamageSource sourceOrRatherLackThereof) { }

    //? if < 1.21.1 {
    @Override
    public boolean canBeLeashed(@NotNull Player player) {
    //?} else {
    /*@Override
    public boolean canBeLeashed() {
    *///?}
        return false;
    }

    // ─── Bossleiste (Original: BossStatus aus dem Renderer, ohne Nebel) ──────

    private final net.minecraft.server.level.ServerBossEvent bossEvent =
            new net.minecraft.server.level.ServerBossEvent(this.getDisplayName(),
                    net.minecraft.world.BossEvent.BossBarColor.PURPLE,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }
}
