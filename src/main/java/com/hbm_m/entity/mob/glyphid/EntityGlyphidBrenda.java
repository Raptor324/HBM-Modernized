package com.hbm_m.entity.mob.glyphid;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityGlyphidBrenda}: feuerfeste Koenigin, platzt beim Tod in eine Pheromonwolke und zwoelf Glyphiden. */
public class EntityGlyphidBrenda extends EntityGlyphid {

    public EntityGlyphidBrenda(EntityType<? extends EntityGlyphidBrenda> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getBrenda());
    }

    @Override
    public String getSkinName() {
        return "glyphid_brenda";
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrenda;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        // amount < 5 ? 5 : amount < 10 ? 3 : 2;
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.getHealth() <= 0.0F) {
            EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), level());
            mist.setFluidType(FluidType.forFluid(ModFluids.PHEROMONE.getSource()));
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(14, 6);
            mist.setDuration(80);
            level().addFreshEntity(mist);

            for (int i = 0; i < 12; ++i) {
                EntityGlyphid glyphid = new EntityGlyphid(ModEntities.GLYPHID.get(), level());
                glyphid.moveTo(this.getX(), this.getY() + 0.5D, this.getZ(), random.nextFloat() * 360.0F, 0.0F);
                this.level().addFreshEntity(glyphid);
                glyphid.move(MoverType.SELF, new Vec3(random.nextGaussian(), 0, random.nextGaussian()));
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextInt(3) == 0) this.spawnAtLocation(ItemFluidTank.make(ModItems.GLYPHID_GLAND.get(), ModFluids.PHEROMONE.getSource(), 1), 1);
    }
}
