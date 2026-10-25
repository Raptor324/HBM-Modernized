package com.hbm_m.entity.projectile;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** 1:1 {@code EntityAcidBomb}: Saeurekugel der Bombardier- und Blaster-Glyphiden; Glyphiden bleiben unberuehrt. */
public class EntityAcidBomb extends EntityThrowableNT implements net.minecraft.world.entity.projectile.ItemSupplier {

    /** Original: {@code RenderSnowball(Items.slime_ball)}. */
    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL);
    }


    public float damage = 1.5F;

    public EntityAcidBomb(EntityType<? extends EntityAcidBomb> type, Level world) {
        super(type, world);
    }

    public EntityAcidBomb(Level world, double x, double y, double z) {
        super(ModEntities.ACID_BOMB.get(), world, x, y, z);
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (level().isClientSide) return;

        if (mop instanceof EntityHitResult ehr) {

            if (!(ehr.getEntity() instanceof EntityGlyphid)) {
                ehr.getEntity().hurt(getDamage(ModDamageTypes.ACID_PLAYER), damage);
                this.discard();
            }
        }

        if (mop.getType() == HitResult.Type.BLOCK)
            this.discard();
    }

    @Override
    public double getGravityVelocity() {
        return 0.04D;
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.damage = nbt.getFloat("damage");
    }
}
