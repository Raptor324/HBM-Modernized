package com.hbm_m.block.custom.machines.armormod.item;

// Это мод, который обеспечивает защиту от радиации при установке на броню.
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.List;


public class ItemModRadProtection extends ItemArmorMod {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");
    private final float protectionValue;

    public ItemModRadProtection(Properties pProperties, int type, float protectionValue) {
        super(pProperties, type);
        this.protectionValue = protectionValue;
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.rad_protection.value",
                        "+" + DECIMAL_FORMAT.format(this.protectionValue))
                .withStyle(ChatFormatting.YELLOW));
    }

    public float getProtectionValue() {
        return this.protectionValue;
    }
}