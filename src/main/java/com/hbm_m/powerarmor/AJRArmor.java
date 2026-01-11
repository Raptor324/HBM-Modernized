package com.hbm_m.powerarmor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// AJR armor with full set bonus - provides damage resistance
public class AJRArmor extends ModArmorFSB {

    public AJRArmor(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties, "hbm_m:textures/armor/ajr_" + getArmorLayerName(type) + ".png");

        // Set up FSB properties for AJR armor
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2, false, false, true));
        this.setHasGeigerSound(false);
        this.setHasCustomGeiger(false);
        this.setHasHardLanding(false);
        this.enableVATS(false);
        this.enableThermalSight(false);
    }

    private static String getArmorLayerName(Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chest";
            case LEGGINGS -> "legs";
            case BOOTS -> "boots";
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (this.type == Type.CHESTPLATE && flag.isAdvanced()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Advanced Jointed Reinforcement").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Significantly increases damage resistance").withStyle(ChatFormatting.GRAY));
        }
    }
}
