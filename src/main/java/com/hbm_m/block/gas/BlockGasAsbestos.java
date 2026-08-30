package com.hbm_m.block.gas;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.world.entity.LivingEntity;

/**
 * Асбестовая пыль в воздухе: +1 asbestos за тик контакта (неизлечимо без лекарств),
 * защищённый фильтр изнашивается. Порт {@link com.hbm.blocks.gas.BlockGasAsbestos} (1.7.10).
 */
public class BlockGasAsbestos extends BlockGasBase {

    public BlockGasAsbestos() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        if (!ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            HbmLivingProps.incrementAsbestos(living, 1);
        } else {
            damageWornFilter(living);
        }
    }
}
