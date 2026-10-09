package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.MaskOfInfamy}. */
public class MaskOfInfamy extends ArmorItem {

    public MaskOfInfamy(ModArmorMaterials material, Type type, Properties properties) {
        super(ModArmorMaterialsAccess.holder(material), type, properties);
    }

    //? if forge {
    @Override
    //?}
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "hbm_m:textures/armor/maskofinfamy.png";
    }

    //? if neoforge {
    /*/^* NeoForge: Textur ueber die String-Variante (1.20.1 Forge {@code getArmorTexture(.., String type)}). ^/
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
        String tex = this.getArmorTexture(stack, entity, slot, (String) null);
        return tex == null ? null : net.minecraft.resources.ResourceLocation.parse(tex);
    }
    *///?}
}
