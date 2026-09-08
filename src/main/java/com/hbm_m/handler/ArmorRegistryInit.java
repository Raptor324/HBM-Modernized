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

        // Everything below was missing: the port only registered the filters and the two rag masks,
        // so the gas masks themselves, the goggles and every helmet protected against nothing.
        // Straight from the original ArmorUtil.register().
        ArmorRegistry.register(ModItems.GAS_MASK.get(), HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.register(ModItems.GAS_MASK_M65.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.ATTACHMENT_MASK.get(), HazardClass.SAND);

        ArmorRegistry.register(ModItems.GOGGLES.get(), HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.register(ModItems.ASHGLASSES.get(), HazardClass.LIGHT, HazardClass.SAND);

        ArmorRegistry.register(ModItems.ASBESTOS_HELMET.get(), HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.register(ModItems.HAZMAT_HELMET.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.HAZMAT_HELMET_RED.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.HAZMAT_HELMET_GREY.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.HAZMAT_PAA_HELMET.get(), HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.register(ModItems.LIQUIDATOR_HELMET.get(), HazardClass.LIGHT, HazardClass.SAND);

        ArmorRegistry.register(ModItems.SCHRABIDIUM_HELMET.get(), FULL_PACKAGE);
        ArmorRegistry.register(ModItems.EUPHEMIUM_HELMET.get(), FULL_PACKAGE);
    }

    /** The original's ArmorUtil.FULL_PACKAGE: everything a head slot can shield against. */
    private static final HazardClass[] FULL_PACKAGE = {
            HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG,
            HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE,
            HazardClass.LIGHT, HazardClass.SAND
    };
}
