package com.hbm_m.item.special;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemSoyuz} (missile_soyuz): die Sojus-Rakete fuer die Startrampe. Die drei Metas des Originals (Skins
 * Original / Luna Space Center / Post War) sind hier drei Gegenstaende; der Skin bestimmt die Texturen der Rakete.
 */
public class ItemSoyuz extends Item {

    public final int skin;

    public ItemSoyuz(int skin, Properties properties) {
        super(properties.stacksTo(1).rarity(skin == 0 ? Rarity.UNCOMMON : skin == 1 ? Rarity.RARE : Rarity.EPIC));
        this.skin = skin;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Skin:"));
        switch (skin) {
            case 0 -> list.add(Component.literal("Original").withStyle(ChatFormatting.GOLD));
            case 1 -> list.add(Component.literal("Luna Space Center").withStyle(ChatFormatting.BLUE));
            case 2 -> list.add(Component.literal("Post War").withStyle(ChatFormatting.GREEN));
        }
    }

    /** {@code new ItemStack(missile_soyuz, 1, skin)}. */
    public static ItemStack forSkin(int skin) {
        return new ItemStack(switch (skin) {
            case 1 -> ModItems.MISSILE_SOYUZ_LUNA.get();
            case 2 -> ModItems.MISSILE_SOYUZ_POSTWAR.get();
            default -> ModItems.MISSILE_SOYUZ.get();
        });
    }

    /** {@code getType()} der Rampe: Skin der eingelegten Rakete oder -1. */
    public static int skinOf(ItemStack stack) {
        return stack.getItem() instanceof ItemSoyuz s ? s.skin : -1;
    }
}
