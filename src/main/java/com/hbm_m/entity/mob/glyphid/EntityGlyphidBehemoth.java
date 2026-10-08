package com.hbm_m.entity.mob.glyphid;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.entity.projectile.EntityChemical;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityGlyphidBehemoth}: speit alle sechs Sekunden sechs Sekunden lang Schwefelsaeure, stirbt in einer Saeurewolke. */
public class EntityGlyphidBehemoth extends EntityGlyphid {

    public EntityGlyphidBehemoth(EntityType<? extends EntityGlyphidBehemoth> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getBehemoth());
    }

    @Override
    public String getSkinName() {
        return "glyphid_behemoth";
    }

    @Override
    public double getGlyphidScale() {
        return 1.5D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBehemoth;
    }

    public int timer = 120;
    int breathTime = 0;

    @Override
    public void tick() {
        super.tick();
        Entity e = this.getEntityToAttack();
        if (e == null) {
            timer = 120;
            breathTime = 0;
        } else {
            if (breathTime > 0) {
                if (!isSwingInProgress) {
                    this.swingItem();
                }
                acidAttack();
                setYRot(yRotO);
                breathTime--;
            } else if (--timer <= 0) {
                breathTime = 120;
                timer = 120;
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), level());
            mist.setFluidType(FluidType.forFluid(ModFluids.SULFURIC_ACID.getSource()));
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 4);
            mist.setDuration(120);
            level().addFreshEntity(mist);
        }
    }

    public void acidAttack() {
        if (!level().isClientSide && entityToAttack instanceof LivingEntity && this.distanceTo(entityToAttack) < 20) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 6));
            EntityChemical chem = new EntityChemical(level(), this, 0, 0, 0);
            chem.setFluid(ModFluids.SULFURIC_ACID.getSource());
            level().addFreshEntity(chem);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        this.spawnAtLocation(ItemFluidTank.make(ModItems.GLYPHID_GLAND.get(), ModFluids.SULFURIC_ACID.getSource(), 1), 1);
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        // amount < 5 ? 5 : amount < 10 ? 3 : 2;
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.15, 2), 100);
    }

    @Override
    public int swingDuration() {
        return 100;
    }
}
