package com.hbm_m.powerarmor;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.AttributeOps;

import com.google.common.collect.Multimap;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.powerarmor.overlay.FSBHelmetOverlay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.ArmorLiquidator}: schwerer Bleianzug (kein Rueckstoss, langsamer, dunkle Sicht). */
public class ArmorLiquidator extends ModArmorFSB {

    private final ResourceLocation hazmatBlur = ResourceLocation.tryParse("hbm_m:textures/misc/overlay_dark.png");

    public ArmorLiquidator(ModArmorMaterials material, Type type, Properties properties, String texture) {
        super(material, type, properties, texture);
    }

    //? if < 1.21.1 {
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
    //?} else {
    /*@Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(net.minecraft.world.item.ItemStack stack) {
        return com.hbm_m.platform.AttributeHooks.fromSlots(this::hbmSlotModifiers);
    }

    private Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> hbmSlotModifiers(EquipmentSlot slot) {
        Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> base =
                com.hbm_m.platform.AttributeHooks.forSlot(super.getDefaultAttributeModifiers(), slot);
    *///?}
        if (slot != this.getEquipmentSlot()) return base;
        return ArmorAttributes.with(base,
                java.util.Map.entry(Attributes.KNOCKBACK_RESISTANCE, PlatformHooks.attributeModifier(ArmorAttributes.fixed(this.getType()), "Armor modifier", 100D, AttributeOps.ADDITION)),
                java.util.Map.entry(Attributes.MOVEMENT_SPEED, PlatformHooks.attributeModifier(ArmorAttributes.fixed(this.getType()), "Armor modifier", -0.1D, AttributeOps.MULTIPLY_BASE)));
    }

    /** Original getArmorModel: die Liquidatorhaube nutzt das M65-Modell (GasMaskLayer). */
    @Override
    public boolean isObjArmor() {
        return this == com.hbm_m.item.ModItems.LIQUIDATOR_HELMET.get();
    }

    @Override
    public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTicks) {
        FSBHelmetOverlay.render(hazmatBlur, width, height);
    }
}
