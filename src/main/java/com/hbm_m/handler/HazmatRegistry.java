package com.hbm_m.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.item.ModItems;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Радиационное сопротивление брони. Порт {@link com.hbm.handler.HazmatRegistry} (1.7.10).
 */
public final class HazmatRegistry {

    public static final double HELMET = 0.2D;
    public static final double CHEST = 0.4D;
    public static final double LEGS = 0.3D;
    public static final double BOOTS = 0.1D;

    public static final List<ExternalEntry> external = new ArrayList<>();

    private static final Map<Item, Double> ENTRIES = new HashMap<>();

    private HazmatRegistry() {
    }

    public record ExternalEntry(Item item, double resistance) {
    }

    public static void registerHazmat(Item item, double resistance) {
        ENTRIES.put(item, resistance);
    }

    public static double getResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0D;
        }
        double cladding = getCladding(stack);
        Double value = ENTRIES.get(stack.getItem());
        if (value != null) {
            return value + cladding;
        }
        return cladding;
    }

    public static float getCladding(ItemStack stack) {
        return 0F;
    }

    public static float getResistance(Player player) {
        float res = 0F;
        for (ItemStack stack : player.getArmorSlots()) {
            res += (float) getResistance(stack);
        }
        return res;
    }

    public static void registerHazmats() {
        if (!ENTRIES.isEmpty()) {
            return;
        }
        initDefault();
    }

    /** {@link com.hbm.handler.HazmatRegistry#initDefault()} */
    public static void initDefault() {
        for (ExternalEntry entry : external) {
            registerHazmat(entry.item(), entry.resistance());
        }

        double iron = 0.0225D;
        double gold = 0.0225D;
        double steel = 0.045D;
        double titanium = 0.045D;
        double alloy = 0.07D;
        double cobalt = 0.125D;

        double hazYellow = 0.6D;
        double paa = 1.7D;
        double liquidator = 2.4D;
        double security = 0.825D;
        double star = 1D;
        double cmb = 1.3D;

        registerSet(ModItems.HAZMAT_HELMET, ModItems.HAZMAT_CHESTPLATE, ModItems.HAZMAT_LEGGINGS, ModItems.HAZMAT_BOOTS, hazYellow);
        registerSet(ModItems.LIQUIDATOR_HELMET, ModItems.LIQUIDATOR_CHESTPLATE, ModItems.LIQUIDATOR_LEGGINGS, ModItems.LIQUIDATOR_BOOTS, liquidator);
        registerSet(ModItems.PAA_HELMET, ModItems.PAA_CHESTPLATE, ModItems.PAA_LEGGINGS, ModItems.PAA_BOOTS, paa);
        registerSet(ModItems.SECURITY_HELMET, ModItems.SECURITY_CHESTPLATE, ModItems.SECURITY_LEGGINGS, ModItems.SECURITY_BOOTS, security);
        registerSet(ModItems.STARMETAL_HELMET, ModItems.STARMETAL_CHESTPLATE, ModItems.STARMETAL_LEGGINGS, ModItems.STARMETAL_BOOTS, star);
        registerSet(ModItems.T51_HELMET, ModItems.T51_CHESTPLATE, ModItems.T51_LEGGINGS, ModItems.T51_BOOTS, star);
        registerSet(ModItems.AJR_HELMET, ModItems.AJR_CHESTPLATE, ModItems.AJR_LEGGINGS, ModItems.AJR_BOOTS, cmb);
        registerSet(ModItems.BISMUTH_HELMET, ModItems.BISMUTH_CHESTPLATE, ModItems.BISMUTH_LEGGINGS, ModItems.BISMUTH_BOOTS, cmb);
        registerSet(ModItems.STEEL_HELMET, ModItems.STEEL_CHESTPLATE, ModItems.STEEL_LEGGINGS, ModItems.STEEL_BOOTS, steel);
        registerSet(ModItems.TITANIUM_HELMET, ModItems.TITANIUM_CHESTPLATE, ModItems.TITANIUM_LEGGINGS, ModItems.TITANIUM_BOOTS, titanium);
        registerSet(ModItems.COBALT_HELMET, ModItems.COBALT_CHESTPLATE, ModItems.COBALT_LEGGINGS, ModItems.COBALT_BOOTS, cobalt);
        registerSet(ModItems.ALLOY_HELMET, ModItems.ALLOY_CHESTPLATE, ModItems.ALLOY_LEGGINGS, ModItems.ALLOY_BOOTS, alloy);

        registerHazmat(Items.IRON_HELMET, iron * HELMET);
        registerHazmat(Items.IRON_CHESTPLATE, iron * CHEST);
        registerHazmat(Items.IRON_LEGGINGS, iron * LEGS);
        registerHazmat(Items.IRON_BOOTS, iron * BOOTS);

        registerHazmat(Items.GOLDEN_HELMET, gold * HELMET);
        registerHazmat(Items.GOLDEN_CHESTPLATE, gold * CHEST);
        registerHazmat(Items.GOLDEN_LEGGINGS, gold * LEGS);
        registerHazmat(Items.GOLDEN_BOOTS, gold * BOOTS);

        registerHazmat(Items.DIAMOND_HELMET, 0.05D);
        registerHazmat(Items.DIAMOND_CHESTPLATE, 0.25D);
        registerHazmat(Items.DIAMOND_LEGGINGS, 0.1D);
        registerHazmat(Items.DIAMOND_BOOTS, 0.025D);

        registerHazmat(Items.NETHERITE_HELMET, 0.1D);
        registerHazmat(Items.NETHERITE_CHESTPLATE, 0.45D);
        registerHazmat(Items.NETHERITE_LEGGINGS, 0.2D);
        registerHazmat(Items.NETHERITE_BOOTS, 0.05D);
    }

    private static void registerSet(
            RegistrySupplier<Item> helmet,
            RegistrySupplier<Item> chest,
            RegistrySupplier<Item> legs,
            RegistrySupplier<Item> boots,
            double materialCoeff) {
        registerIfPresent(helmet, materialCoeff * HELMET);
        registerIfPresent(chest, materialCoeff * CHEST);
        registerIfPresent(legs, materialCoeff * LEGS);
        registerIfPresent(boots, materialCoeff * BOOTS);
    }

    private static void registerIfPresent(RegistrySupplier<Item> supplier, double resistance) {
        Item item = supplier.orElse(null);
        if (item != null) {
            registerHazmat(item, resistance);
        }
    }
}
