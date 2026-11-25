package com.hbm_m.creativetabs;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.ItemCustomMissile;
import com.hbm_m.item.missile.MissilePartItems;
import com.hbm_m.platform.StackNbt;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Порт {@link com.hbm.creativetabs.MissileTab} — иконка вкладки и доп. предметы (кастомные ракеты).
 */
public final class MissileTab {

    private MissileTab() {
    }

    public static Item getTabIconItem() {
        if (ModItems.MISSILE_NUCLEAR.isPresent()) {
            return ModItems.MISSILE_NUCLEAR.get();
        }
        return Items.IRON_PICKAXE;
    }

    private static Item part(String id) {
        return MissilePartItems.get(id).get();
    }

    private static ItemStack customMissile(String chip, String warhead, String fuselage, @Nullable String stability, String thruster, String name, ChatFormatting color) {
        ItemStack stack = ItemCustomMissile.buildMissile(
                part(chip),
                part(warhead),
                part(fuselage),
                stability != null ? part(stability) : null,
                part(thruster)
        );
        return StackNbt.setCustomName(stack, Component.literal(name).withStyle(color));
    }

    /**
     * Дополнительные стеки вкладки (оригинал: {@code displayAllReleventItems} + {@code ItemCustomMissile}).
     */
    public static void appendExtraItems(Consumer<ItemStack> output) {
        // 205: Lil Bub
        output.accept(customMissile("mp_c_3", "mp_warhead_10_he", "mp_fuselage_10_kerosene", "mp_stability_10_flat", "mp_thruster_10_kerosene", "Lil Bub", ChatFormatting.DARK_PURPLE));
        // 206: Long Boy
        output.accept(customMissile("mp_c_3", "mp_warhead_10_incendiary", "mp_fuselage_10_long_solid", "mp_stability_10_space", "mp_thruster_10_solid", "Long Boy", ChatFormatting.DARK_PURPLE));
        // 207: Uncle Kim
        output.accept(customMissile("mp_c_3", "mp_warhead_10_nuclear", "mp_fuselage_10_15_kerosene", "mp_stability_15_flat", "mp_thruster_15_kerosene", "Uncle Kim", ChatFormatting.DARK_PURPLE));
        // 208: Trotty's Toy Rocket
        output.accept(customMissile("mp_c_3", "mp_warhead_10_nuclear_large", "mp_fuselage_10_15_balefire", "mp_stability_15_flat", "mp_thruster_15_balefire_large", "Trotty's Toy Rocket", ChatFormatting.GREEN));
        // 209: Stealthy Shark
        output.accept(customMissile("mp_c_3", "mp_warhead_15_nuclear_shark", "mp_fuselage_15_kerosene_camo", "mp_stability_15_thin", "mp_thruster_15_kerosene_triple", "Stealthy Shark", ChatFormatting.DARK_PURPLE));
        // 210: Polite Lad
        output.accept(customMissile("mp_c_3", "mp_warhead_15_he", "mp_fuselage_15_kerosene_polite", "mp_stability_15_thin", "mp_thruster_15_kerosene_dual", "Polite Lad", ChatFormatting.DARK_PURPLE));
        // 211: NERV's Leftover Missile
        output.accept(customMissile("mp_c_3", "mp_warhead_15_n2", "mp_fuselage_15_solid_desh", "mp_stability_15_thin", "mp_thruster_15_solid_hexdecuple", "NERV's Leftover Missile", ChatFormatting.DARK_PURPLE));
        // 212: Auntie Blackjack
        output.accept(customMissile("mp_c_5", "mp_warhead_15_boxcar", "mp_fuselage_15_kerosene_blackjack", "mp_stability_15_thin", "mp_thruster_15_kerosene", "Auntie Blackjack", ChatFormatting.RED));
        // 213: Hightower Missile
        output.accept(customMissile("mp_c_4", "mp_warhead_15_balefire", "mp_fuselage_15_20_kerosene_magnusson", null, "mp_thruster_20_kerosene", "Hightower Missile", ChatFormatting.GREEN));
    }
}
