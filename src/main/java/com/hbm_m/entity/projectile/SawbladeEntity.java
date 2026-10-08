package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntitySawblade}: das Blatt, das aus einem ueberdrehten Saegewerk fliegt. Verhaelt sich exakt wie das
 * Stirling-Zahnrad ({@link CogEntity}: toetet, prallt ab, sprengt, bleibt liegen) und gibt aufgehoben ein
 * {@code sawblade} zurueck.
 */
public class SawbladeEntity extends CogEntity {

    public SawbladeEntity(EntityType<? extends CogEntity> type, Level level) {
        super(type, level);
    }

    public static SawbladeEntity create(Level level, double x, double y, double z, Direction facing) {
        SawbladeEntity blade = new SawbladeEntity(ModEntities.SAWBLADE.get(), level);
        blade.setPos(x, y, z);
        blade.setOrientationValue(facing.ordinal());
        return blade;
    }

    @Override
    protected ItemStack pickupStack() {
        return new ItemStack(ModItems.SAWBLADE.get());
    }
}
