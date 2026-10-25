package com.hbm_m.entity.mob.glyphid;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityWaypoint;
import com.hbm_m.entity.mob.EntityParasiteMaggot;
import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code EntityGlyphidNuclear} ("Big Man Johnson"): zieht beim Tod 100 Ticks lang die Spaeher zurueck, piept und
 * explodiert dann mit Staerke 25 (Vulkanlava-Truemmer; befallen: 15-20 Maden statt Kraterschaeden).
 */
public class EntityGlyphidNuclear extends EntityGlyphid {

    public int deathTicks;

    public EntityGlyphidNuclear(EntityType<? extends EntityGlyphidNuclear> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getNuclear());
    }

    @Override
    public String getSkinName() {
        return "glyphid_nuclear";
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsNuclear;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount % 20 == 0) {
            if (isAtDestination() && getCurrentTask() == TASK_FOLLOW) {
                setCurrentTask(TASK_IDLE, null);
            }

            if (getCurrentTask() == TASK_BUILD_HIVE && getLastHurtByMob() == null) {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10 * 20, 3));
            }

            if (getCurrentTask() == TASK_TERRAFORM) {
                this.setHealth(0);
            }
        }
    }

    /** Communicates only with glyphid scouts, unlike the super implementation which does the opposite */
    @Override
    public void communicate(int task, @Nullable EntityWaypoint waypoint) {
        int radius = waypoint != null ? waypoint.radius : 4;

        AABB bb = new AABB(this.getX() - radius, this.getY() - radius, this.getZ() - radius, this.getX() + radius, this.getY() + radius, this.getZ() + radius);

        List<Entity> bugs = level().getEntities(this, bb);
        for (Entity e : bugs) {
            if (e instanceof EntityGlyphidScout scout) {
                if (scout.getCurrentTask() != task) {
                    scout.setCurrentTask(task, waypoint);
                }
            }
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    public boolean doesInfectedSpawnMaggots() {
        return false;
    }

    public boolean hasWaypoint = false;

    /** Original {@code onDeathUpdate}. */
    @Override
    protected void tickDeath() {
        ++this.deathTicks;

        if (!hasWaypoint) {
            // effectively causes neighboring EntityGlyphidScout to retreat
            communicate(TASK_INITIATE_RETREAT, null);
            hasWaypoint = true;
        }

        if (deathTicks == 90) {
            int radius = 8;
            AABB bb = new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ()).inflate(radius);

            List<Entity> bugs = level().getEntities(this, bb);
            for (Entity e : bugs) {
                if (e instanceof EntityGlyphid) {
                    addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 20, 6));
                    addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 15 * 20, 1));
                }
            }
        }

        if (this.deathTicks == 100) {

            if (!level().isClientSide) {
                ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 25, this);

                if (this.getSubtype() == TYPE_INFECTED) {
                    int j = 15 + this.random.nextInt(6);
                    for (int k = 0; k < j; ++k) {
                        float f = ((float) (k % 2) - 0.5F) * 0.5F;
                        float f1 = ((float) (k / 2) - 0.5F) * 0.5F;
                        EntityParasiteMaggot maggot = new EntityParasiteMaggot(ModEntities.PARASITE_MAGGOT.get(), level());
                        maggot.moveTo(this.getX() + f, this.getY() + 0.5D, this.getZ() + f1, this.random.nextFloat() * 360.0F, 0.0F);
                        maggot.setDeltaMovement(f, 0, f1);
                        maggot.hurtMarked = true;
                        level().addFreshEntity(maggot);
                    }

                } else {
                    vnt.setBlockAllocator(new BlockAllocatorStandard(24));
                    vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorDebris(ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState())).setNoDrop());
                }

                vnt.setEntityProcessor(new EntityProcessorStandard());
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.mukeExplosion"), SoundSource.HOSTILE, 15.0F, 1.0F);

                CompoundTag data = new CompoundTag();
                data.putString("type", "muke");
                // if the FX type is "muke", apply random BF effect
                if (com.hbm_m.main.Polaroid.id() == 11 || random.nextInt(100) == 0) {
                    data.putBoolean("balefire", true);
                }
                com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) level(), getX(), getY() + 0.5, getZ(), 250, data);
            }

            this.remove(RemovalReason.KILLED);
        } else {
            if (!level().isClientSide && this.deathTicks % 10 == 0) {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.fstbmbPing"), SoundSource.HOSTILE, 5.0F, 1.0F);
            }
        }
    }
}
