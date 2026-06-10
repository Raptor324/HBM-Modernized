package com.hbm_m.entity.mob;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.mixin.CreeperAccessor;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Фосгеновый крипер — быстрый поджиг, взрыв и облако фосгена.
 * Порт {@link com.hbm.entity.mob.EntityCreeperPhosgene} (1.7.10).
 *
 * <p>Взрыв перехватывается в {@link com.hbm_m.mixin.CreeperMixin}.</p>
 */
public class EntityCreeperPhosgene extends Creeper {

    public EntityCreeperPhosgene(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        ((CreeperAccessor) this).hbm_m$setMaxSwell(20);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount -= 4.0F;
        }
        if (amount <= 0.0F) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @SuppressWarnings("unchecked")
    public static boolean checkPhosgeneSpawnRules(
            EntityType<EntityCreeperPhosgene> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (!Creeper.checkMonsterSpawnRules(
                (EntityType<Creeper>) (EntityType<?>) entityType, level, spawnType, pos, random)) {
            return false;
        }
        if (level instanceof Level world && world.dimension() != Level.OVERWORLD) {
            return false;
        }
        return true;
    }

    /** Взрыв + облако фосгена (оригинал {@code func_146077_cc}). Вызывается из {@link com.hbm_m.mixin.CreeperMixin}. */
    public void phosgeneExplode() {
        if (this.level().isClientSide) {
            return;
        }

        this.dead = true;
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        this.level().explode(
                this,
                x,
                y + this.getBbHeight() / 2.0,
                z,
                2.0F,
                false,
                Level.ExplosionInteraction.MOB);

        EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), this.level());
        mist.setPos(x, y, z);
        mist.setFluidType(FluidType.forFluid(ModFluids.PHOSGENE.getSource()));
        mist.setArea(10.0F, 5.0F);
        mist.setDuration(150);
        this.level().addFreshEntity(mist);

        this.discard();
    }
}
