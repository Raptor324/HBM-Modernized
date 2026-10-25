package com.hbm_m.entity.mob.glyphid;

import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityGlyphidBlaster}: schwerer Bombardier, zehn Kugeln mit 15 Schaden. */
public class EntityGlyphidBlaster extends EntityGlyphidBombardier {

    public EntityGlyphidBlaster(EntityType<? extends EntityGlyphidBlaster> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getBlaster());
    }

    @Override
    public String getSkinName() {
        return "glyphid_blaster";
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBlaster;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    public float getBombDamage() {
        return 15F;
    }

    @Override
    public int getBombCount() {
        return 10;
    }

    @Override
    public float getSpreadMult() {
        return 0.5F;
    }

    @Override
    public double getV0() {
        return 1.25D;
    }
}
