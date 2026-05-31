package com.hbm_m.entity.mob;

import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;

/**
 * Заражённый крипер — самолечение, взрыв с порчей вместо обычного.
 * Порт {@link com.hbm.entity.mob.EntityCreeperTainted} (1.7.10).
 *
 * <p>Взрыв перехватывается в {@link com.hbm_m.mixin.CreeperMixin} (private {@code explodeCreeper()}).</p>
 */
public class EntityCreeperTainted extends Creeper {

    private static final EntityDataAccessor<Boolean> DATA_IS_POWERED;

    static {
        try {
            var creeperLookup = MethodHandles.privateLookupIn(Creeper.class, MethodHandles.lookup());
            @SuppressWarnings("unchecked")
            EntityDataAccessor<Boolean> powered = (EntityDataAccessor<Boolean>) creeperLookup
                    .findStaticVarHandle(Creeper.class, "DATA_IS_POWERED", EntityDataAccessor.class)
                    .get();
            DATA_IS_POWERED = powered;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public EntityCreeperTainted(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive() && this.getHealth() < this.getMaxHealth() && this.tickCount % 10 == 0) {
            this.heal(1.0F);
        }
    }

    /** Взрыв порчи (оригинал {@code func_146077_cc}). Вызывается из {@link com.hbm_m.mixin.CreeperMixin}. */
    public void taintedExplode() {
        if (this.level().isClientSide) {
            return;
        }

        this.dead = true;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0F, false, Level.ExplosionInteraction.MOB);

        if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            spreadTaint(this.isPowered());
        }

        this.spawnLingeringCloud();
        this.discard();
    }

    /** Копия {@link Creeper#spawnLingeringCloud()} — private в ваниле. */
    private void spawnLingeringCloud() {
        Collection<MobEffectInstance> collection = this.getActiveEffects();
        if (!collection.isEmpty()) {
            AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            cloud.setRadius(2.5F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setDuration(cloud.getDuration() / 2);
            cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());

            for (MobEffectInstance effect : collection) {
                cloud.addEffect(new MobEffectInstance(effect));
            }

            this.level().addFreshEntity(cloud);
        }
    }

    private void spreadTaint(boolean powered) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        boolean trails = ModClothConfig.get().taintTrails;
        int count = powered ? 255 : 85;
        int radius = powered ? 7 : 3;

        for (int i = 0; i < count; i++) {
            int x = this.random.nextInt(radius * 2 + 1) + (int) this.getX() - radius;
            int y = this.random.nextInt(radius * 2 + 1) + (int) this.getY() - radius;
            int z = this.random.nextInt(radius * 2 + 1) + (int) this.getZ() - radius;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!BlockTaint.canBeReplacedByTaint(level, pos, state)) {
                continue;
            }

            int age;
            if (trails) {
                age = this.random.nextInt(3) + (powered ? 0 : 4);
            } else if (powered) {
                age = this.random.nextInt(3) + 5;
            } else {
                age = this.random.nextInt(6) + 10;
            }
            level.setBlock(pos, BlockTaint.stateWithAge(age), 2);
        }
    }

    /** Замена обычного крипера при контакте с блоком taint (сервер). */
    public static void convertFromCreeper(Creeper creeper) {
        if (creeper.level().isClientSide || creeper instanceof EntityCreeperTainted) {
            return;
        }
        Level level = creeper.level();
        EntityCreeperTainted tainted = ModEntities.ENTITY_MOB_TAINTED_CREEPER.get().create(level);
        if (tainted == null) {
            return;
        }
        tainted.moveTo(creeper.getX(), creeper.getY(), creeper.getZ(), creeper.getYRot(), creeper.getXRot());
        if (creeper.isPowered()) {
            tainted.getEntityData().set(DATA_IS_POWERED, true);
        }
        creeper.discard();
        level.addFreshEntity(tainted);
    }
}
