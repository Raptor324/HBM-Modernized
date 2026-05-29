package com.hbm_m.entity.mob;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.MissileWarheadEffects;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Ядерный крипер — радиационная аура, мини-ядерный / полный взрыв при детонации.
 * Порт {@link com.hbm.entity.mob.EntityCreeperNuclear} (1.7.10).
 *
 * <p>Взрыв перехватывается в {@link com.hbm_m.mixin.CreeperMixin}.</p>
 */
public class EntityCreeperNuclear extends Creeper {

    private static final VarHandle MAX_SWELL;

    static {
        try {
            var creeperLookup = MethodHandles.privateLookupIn(Creeper.class, MethodHandles.lookup());
            MAX_SWELL = creeperLookup.findVarHandle(Creeper.class, "maxSwell", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public EntityCreeperNuclear(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        MAX_SWELL.set(this, 75);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isDeadOrDying()) {
            return false;
        }

        if (source.is(ModDamageTypes.RADIATION) || source.is(ModDamageTypes.MUD_POISONING)) {
            if (this.isAlive()) {
                this.heal(amount);
            }
            return false;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            AABB aura = this.getBoundingBox().inflate(5.0D);
            for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, aura, e -> e != this)) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.25F);
            }
        }

        super.tick();

        if (this.isAlive() && this.getHealth() < this.getMaxHealth() && this.tickCount % 10 == 0) {
            this.heal(1.0F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        this.spawnAtLocation(new ItemStack(Blocks.TNT));
        // coin_creeper, NUKE_STANDARD ammo, bossCreeper — после порта соответствующих систем
    }

    /** Взрыв (оригинал {@code func_146077_cc} / ExplosionNukeSmall). Вызывается из {@link com.hbm_m.mixin.CreeperMixin}. */
    public void nuclearExplode() {
        if (this.level().isClientSide()) {
            return;
        }

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        boolean griefing = this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);

        this.dead = true;

        if (this.isPowered()) {
            sendMukeParticle(this.level(), x, y + 0.5, z);
            playMukeSound(this.level(), x, y + 0.5, z);

            if (griefing) {
                EntityNukeExplosionMK5.start(this.level(), 50, x, y, z);
            } else {
                ExplosionNukeGeneric.dealDamage(this.level(), x, y + 0.5, z, 100.0D);
            }
        } else if (griefing) {
            explodeNukeSmall(this.level(), x, y + 0.5, z, 20.0F, 55.0F, 3.0F, false);
        } else {
            explodeNukeSmall(this.level(), x, y + 0.5, z, 0.0F, 45.0F, 2.0F, true);
        }

        this.discard();
    }

    /** Логика {@link com.hbm.explosion.ExplosionNukeSmall#explode} для PARAMS_MEDIUM / PARAMS_SAFE. */
    private static void explodeNukeSmall(Level level, double x, double y, double z,
                                         float blastRadius, float killRadius, float radiationLevel, boolean safe) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        sendMukeParticle(level, x, y, z);
        playMukeSound(level, x, y, z);

        MissileWarheadEffects.spawnShrapnelBurst(serverLevel, x, y, z, 25);

        if (!safe && blastRadius > 0.0F) {
            serverLevel.explode(null, x, y, z, blastRadius, Level.ExplosionInteraction.MOB);
        }

        if (killRadius > 0.0F) {
            ExplosionNukeGeneric.dealDamage(level, x, y, z, killRadius);
        }

        float radMod = radiationLevel / 3.0F;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) < 4) {
                    ChunkRadiationManager.incrementRad(
                            level,
                            (int) Math.floor(x + i * 16),
                            (int) Math.floor(y),
                            (int) Math.floor(z + j * 16),
                            50.0F / (Math.abs(i) + Math.abs(j) + 1) * radMod);
                }
            }
        }
    }

    private static void sendMukeParticle(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putString("type", "nuke");
        data.putDouble("posX", x);
        data.putDouble("posY", y);
        data.putDouble("posZ", z);
        IParticleCreator.sendPacket(serverLevel, x, y, z, 250, data);
    }

    private static void playMukeSound(Level level, double x, double y, double z) {
        SoundEvent sound = ModSounds.MUKE_EXPLOSION.orElse(null);
        if (sound != null) {
            level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.HOSTILE, 15.0F, 1.0F);
        }
    }
}
