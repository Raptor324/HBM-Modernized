package com.hbm_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.main.Polaroid;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.ItemCustomLore}: Tooltip aus {@code <descriptionId>.desc} (bzw.
 * {@code .desc.P11} bei Polaroid 11), Zeilen mit {@code $} getrennt; optionale Seltenheit und
 * Verzauberungsglanz ({@code setEffect}).
 */
public class ItemCustomLore extends Item implements ITooltipProvider {

    protected Rarity rarity;
    protected boolean hasEffect = false;

    public ItemCustomLore(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        addLore(this.getDescriptionId(), tooltip);
    }

    /** Die Lore-Logik als statische Hilfe, damit auch Nicht-Unterklassen sie nutzen koennen. */
    public static void addLore(String unlocalized, List<Component> tooltip) {
        String p11Key = unlocalized + ".desc.P11";
        boolean p11 = net.minecraft.locale.Language.getInstance().has(p11Key);
        String key = (Polaroid.id() == 11 && p11) ? p11Key : unlocalized + ".desc";
        String loc = net.minecraft.locale.Language.getInstance().getOrDefault(key);
        if (loc.equals(key)) return;
        for (String s : loc.split("\\$")) tooltip.add(Component.literal(s));
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return this.rarity != null ? rarity : super.getRarity(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasEffect || super.isFoil(stack);
    }

    public ItemCustomLore setRarity(Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public ItemCustomLore setEffect() {
        this.hasEffect = true;
        return this;
    }
}
