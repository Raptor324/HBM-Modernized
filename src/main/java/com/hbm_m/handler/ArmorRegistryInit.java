package com.hbm_m.handler;

import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.world.item.Item;

/**
 * 1:1 {@code com.hbm.util.ArmorUtil.register()} (1.7.10): Schutz von Filtern, Masken, Brillen und Helmen
 * gegen Gefahrenklassen, plus alles, was FSB-Ruestungen ueber {@code setHazardClass} anmelden.
 */
public final class ArmorRegistryInit {

    private ArmorRegistryInit() {
    }

    public static void init() {
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER.get(), HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_MONO.get(), HazardClass.PARTICLE_COARSE, HazardClass.GAS_MONOXIDE);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_COMBO.get(), HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA, HazardClass.GAS_MONOXIDE);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_RAG.get(), HazardClass.PARTICLE_COARSE);
        ArmorRegistry.register(ModItems.GAS_MASK_FILTER_PISS.get(), HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);

        ArmorRegistry.register(ModItems.GAS_MASK.get(), HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.register(ModItems.GAS_MASK_M65.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.MASK_RAG.get(), HazardClass.PARTICLE_COARSE);
        ArmorRegistry.register(ModItems.MASK_PISS.get(), HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);

        ArmorRegistry.register(ModItems.GOGGLES.get(), HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.register(ModItems.ASHGLASSES.get(), HazardClass.LIGHT, HazardClass.SAND);

        ArmorRegistry.register(ModItems.ATTACHMENT_MASK.get(), HazardClass.SAND);

        ArmorRegistry.register(ModItems.ASBESTOS_HELMET.get(), HazardClass.SAND, HazardClass.LIGHT);

        ArmorRegistry.register(ModItems.HAZMAT_HELMET.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.HAZMAT_HELMET_RED.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.HAZMAT_HELMET_GREY.get(), HazardClass.SAND);
        ArmorRegistry.register(ModItems.HAZMAT_PAA_HELMET.get(), HazardClass.LIGHT, HazardClass.SAND);
        ArmorRegistry.register(ModItems.LIQUIDATOR_HELMET.get(), HazardClass.LIGHT, HazardClass.SAND);

        ArmorRegistry.register(ModItems.SCHRABIDIUM_HELMET.get(), ArmorUtil.FULL_PACKAGE);
        ArmorRegistry.register(ModItems.EUPHEMIUM_HELMET.get(), ArmorUtil.FULL_PACKAGE);

        for (Map.Entry<Item, HazardClass[]> pair : ArmorUtil.external) {
            ArmorRegistry.register(pair.getKey(), pair.getValue());
        }
    }
}
