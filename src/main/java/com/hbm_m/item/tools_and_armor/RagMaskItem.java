package com.hbm_m.item.tools_and_armor;

import com.hbm_m.lib.RefStrings;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Тряпичные маски (mask_rag / mask_piss). Порт {@link com.hbm.items.armor.ModArmor}
 * с материалом HBM_RAGS (1.7.10): обычный шлем без фильтра, собственный слой брони
 * {@code textures/armor/rag_damp.png} / {@code rag_piss.png}; защита регистрируется
 * напрямую в {@link com.hbm_m.handler.ArmorRegistryInit} (PARTICLE_COARSE [+ GAS_LUNG]).
 * В отличие от противогазов фильтр не устанавливается и HUD-оверлея нет.
 */
public class RagMaskItem extends ArmorItem {

    private final String armorTexture;

    public RagMaskItem(boolean piss, Properties properties) {
        super(ModArmorMaterialsAccess.holder(ModArmorMaterials.GAS_MASK), Type.HELMET, properties);
        this.armorTexture = RefStrings.MODID + ":textures/armor/" + (piss ? "rag_piss.png" : "rag_damp.png");
    }

    //? if < 1.21.1 {
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return armorTexture;
    }
    //?} else {
    /*@Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity,
            EquipmentSlot slot, net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
        return net.minecraft.resources.ResourceLocation.parse(armorTexture);
    }*///?}
}
