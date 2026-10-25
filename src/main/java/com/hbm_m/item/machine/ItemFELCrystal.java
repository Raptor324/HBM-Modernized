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

    /**
     * Wellenlaengen des FEL. {@link #tier} ersetzt die Ordinalzahl des Originals in allen Rechnungen (FEL-Strom
     * {@code 3^tier}, Strahl {@code 2^tier}, SILEX-Rezeptstufe/-Tempo, GUI-Frequenz) und haelt die HBM-Werte fest.
     * <p>Space-API (W-R4b-4): {@code XRAY} (NTM Space, {@code laser_crystal_iron} des Addons) steht wie dort zwischen UV
     * und GAMMA. Im Space-Zweig verschiebt es GAMMA/DRX um eine Ordinalzahl (doppelter Strom, andere SILEX-Stufen) - das
     * wuerde HBM ohne Addon veraendern. Deshalb behalten GAMMA/DRX ihre Stufen 4/5, XRAY rechnet mit der Space-Stufe 4
     * (Strom/Tempo exakt wie im Original), darf aber wie dort nur Rezepte bis UV ({@link #maxRecipeTier} 3) bearbeiten.
     */
    public enum EnumWavelengths {
        NULL("la creatura", "6 dollar", 0x010101, 0x010101, ChatFormatting.WHITE, 0, 0), //why do you exist?
        IR("wavelengths.name.ir", "wavelengths.waveRange.ir", 0xBB1010, 0xCC4040, ChatFormatting.RED, 1, 1),
        VISIBLE("wavelengths.name.visible", "wavelengths.waveRange.visible", 0, 0, ChatFormatting.GREEN, 2, 2),
        UV("wavelengths.name.uv", "wavelengths.waveRange.uv", 0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA, 3, 3),
        XRAY("wavelengths.name.xray", "wavelengths.waveRange.xray", 0xF8E6A7, 0xF8E6A7, ChatFormatting.GOLD, 4, 3),
        GAMMA("wavelengths.name.gamma", "wavelengths.waveRange.gamma", 0x150560, 0xEF00FF, ChatFormatting.LIGHT_PURPLE, 4, 4),
        DRX("wavelengths.name.drx", "wavelengths.waveRange.drx", 0xFF0000, 0xFF0000, ChatFormatting.DARK_RED, 5, 5);

        public final String name;
        public final String wavelengthRange;
        public final int renderedBeamColor;
        public final int guiColor;
        public final ChatFormatting textColor;
        /** Rechenstufe (HBM: bisherige Ordinalzahl). */
        public final int tier;
        /** Hoechste SILEX-Rezeptstufe ({@code SilexRecipe.laser}), die diese Wellenlaenge bearbeiten darf. */
        public final int maxRecipeTier;

        EnumWavelengths(String name, String wavelength, int color, int guiColor, ChatFormatting textColor, int tier, int maxRecipeTier) {
            this.name = name;
            this.wavelengthRange = wavelength;
            this.renderedBeamColor = color;
            this.guiColor = guiColor;
            this.textColor = textColor;
            this.tier = tier;
            this.maxRecipeTier = maxRecipeTier;
        }
    }
}
