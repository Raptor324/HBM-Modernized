package com.hbm_m.item.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemFELCrystal}: Laserkristalle des FEL mit Wellenlaenge. */
public class ItemFELCrystal extends Item implements ITooltipProvider {

    public EnumWavelengths wavelength = EnumWavelengths.NULL;

    public ItemFELCrystal(EnumWavelengths wavelength, Properties properties) {
        super(properties.stacksTo(1));
        this.wavelength = wavelength;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (stack.getItem() == ModItems.LASER_CRYSTAL_DIGAMMA.get()) {
            list.add(Component.literal("THERADIANCEOFATHOUSANDSUNS").withStyle(ChatFormatting.OBFUSCATED));
        } else {
            list.add(Component.translatable(this.getDescriptionId() + ".desc"));
        }
        list.add(Component.translatable(wavelength.name).withStyle(wavelength.textColor)
                .append(Component.literal(" - ").withStyle(wavelength.textColor))
                .append(Component.translatable(wavelength.wavelengthRange).withStyle(wavelength.textColor)));
    }

    public enum EnumWavelengths {
        NULL("la creatura", "6 dollar", 0x010101, 0x010101, ChatFormatting.WHITE), //why do you exist?
        IR("wavelengths.name.ir", "wavelengths.waveRange.ir", 0xBB1010, 0xCC4040, ChatFormatting.RED),
        VISIBLE("wavelengths.name.visible", "wavelengths.waveRange.visible", 0, 0, ChatFormatting.GREEN),
        UV("wavelengths.name.uv", "wavelengths.waveRange.uv", 0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA),
        GAMMA("wavelengths.name.gamma", "wavelengths.waveRange.gamma", 0x150560, 0xEF00FF, ChatFormatting.LIGHT_PURPLE),
        DRX("wavelengths.name.drx", "wavelengths.waveRange.drx", 0xFF0000, 0xFF0000, ChatFormatting.DARK_RED);

        public final String name;
        public final String wavelengthRange;
        public final int renderedBeamColor;
        public final int guiColor;
        public final ChatFormatting textColor;

        EnumWavelengths(String name, String wavelength, int color, int guiColor, ChatFormatting textColor) {
            this.name = name;
            this.wavelengthRange = wavelength;
            this.renderedBeamColor = color;
            this.guiColor = guiColor;
            this.textColor = textColor;
        }
    }
}
