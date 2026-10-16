package com.hbm_m.handler;

import com.hbm_m.armormod.item.ItemModGasmask;
import com.hbm_m.item.ModItems;

/**
 * Регистрация защиты предметов по классам опасностей.
 * Порт {@link com.hbm.util.ArmorUtil.register()} (1.7.10).
 */
public final class ArmorRegistryInit {

    private ArmorRegistryInit() {
    }

    public static void init() {
        // Фильтры: полный пакет — базовый и комбо; mono только от CO и крупной пыли;
        // тряпичные — только от крупной пыли.
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER.get(),
                HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE,
                HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_COMBO.get(),
                HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE,
                HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA,
                HazardClass.GAS_MONOXIDE);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_MONO.get(),
                HazardClass.PARTICLE_COARSE, HazardClass.GAS_MONOXIDE);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_RAG.get(),
                HazardClass.PARTICLE_COARSE);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_PISS.get(),
                HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);

        // Маски без фильтра: сами защищают только от крупной пыли.
        ArmorRegistry.register(ModItems.MASK_RAG.get(), HazardClass.PARTICLE_COARSE);
        ArmorRegistry.register(ModItems.MASK_PISS.get(),
                HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);
    }
}
