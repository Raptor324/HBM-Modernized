package com.hbm_m.handler;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

//? if forge {
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * 1:1 {@code com.hbm.handler.FuelHandler}: Brennzeiten der NTM-Brennstoffe - wie im Original fuer alle Oefen, auch
 * die Vanilla-Oefen ({@code IFuelHandler}). Metadaten-Varianten (Briketts, Asche, Koks) sind im Port eigene Items.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = com.hbm_m.lib.RefStrings.MODID)
//?} elif neoforge {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = com.hbm_m.lib.RefStrings.MODID)
*///?}
public final class FuelHandler {

    private static final int SINGLE = 200;
    private static final Map<ResourceLocation, Integer> BURN = new HashMap<>();

    static {
        put("solid_fuel", SINGLE * 16);
        put("solid_fuel_presto", SINGLE * 40);
        put("solid_fuel_presto_triplet", SINGLE * 200);
        put("solid_fuel_bf", SINGLE * 160);
        put("solid_fuel_presto_bf", SINGLE * 400);
        put("solid_fuel_presto_triplet_bf", SINGLE * 2000);
        put("rocket_fuel", SINGLE * 32);

        put("biomass", SINGLE * 2);
        put("biomass_compressed", SINGLE * 4);
        put("coal_powder", SINGLE * 8);
        put("scrap", SINGLE / 4);
        put("dust", SINGLE / 8);
        put("block_scrap", SINGLE * 2);
        put("fire_powder", 6400);
        put("lignite", 1200);
        put("lignite_powder", 1200);
        // Original: coke (alle drei Sorten) und block_coke
        put("coal_coke", SINGLE * 16);
        put("lignite_coke", SINGLE * 16);
        put("coke_petroleum", SINGLE * 16);
        put("block_coke_coal", SINGLE * 160);
        put("block_coke_lignite", SINGLE * 160);
        put("block_coke_petroleum", SINGLE * 160);
        put("book_guide", SINGLE);
        put("coal_infernal", 4800);
        put("coal_eternal", SINGLE * 16);
        put("crystal_coal", 6400);
        put("sawdust_powder", SINGLE / 2);

        // Original: briquette Meta 0 Kohle, 1 Braunkohle, 2 Holz
        put("coal_briquette", SINGLE * 10);
        put("lignite_briquette", SINGLE * 8);
        put("sawdust_briquette", SINGLE * 2);

        // Original: powder_ash Meta 0 Holz, 1 Kohle, 2 Sonstiges, 3 Flugasche, 4 Russ
        put("ash_wood", SINGLE / 2);
        put("ash_coal", SINGLE);
        put("ash_misc", SINGLE / 2);
        put("ash_fly", SINGLE);
        put("ash_soot", SINGLE / 2);
    }

    private FuelHandler() {}

    private static void put(String id, int ticks) {
        BURN.put(ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, id), ticks);
    }

    /** Original {@code getBurnTime}: 0, wenn der Gegenstand kein NTM-Brennstoff ist. */
    public static int getBurnTime(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        Integer t = BURN.get(BuiltInRegistries.ITEM.getKey(fuel.getItem()));
        return t == null ? 0 : t;
    }

    /** Original {@code FuelHandler.getBurnTimeFromCache} = {@code TileEntityFurnace.getItemBurnTime} samt NTM-Brennstoffen. */
    public static int getBurnTimeFromCache(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        //? if forge {
        return net.minecraftforge.common.ForgeHooks.getBurnTime(stack, net.minecraft.world.item.crafting.RecipeType.SMELTING);
        //?} else {
        /*int own = getBurnTime(stack);
        return own > 0 ? own : AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        *///?}
    }

    //? if forge {
    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        int t = getBurnTime(event.getItemStack());
        if (t > 0) event.setBurnTime(t);
    }
    //?} elif neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void onFuelBurnTime(net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent event) {
        int t = getBurnTime(event.getItemStack());
        if (t > 0) event.setBurnTime(t);
    }
    *///?}

    /** Nur damit der Import in der Nicht-Forge-Variante nicht verwaist. */
    @SuppressWarnings("unused")
    private static int vanilla(ItemStack stack) {
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }
}
