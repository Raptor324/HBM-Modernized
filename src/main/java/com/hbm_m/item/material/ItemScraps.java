package com.hbm_m.item.material;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemScraps} als Bruecke: im Original ein Metadaten-Item (Schaden = Material-ID) mit NBT {@code amount}
 * und {@code liquid}; im Port je Material ein {@code scraps_<material>}-Item ({@link ScrapItem}) mit demselben NBT.
 */
public final class ItemScraps {

    private static Map<Item, NTMMaterial> itemToMat;
    private static Map<NTMMaterial, Item> matToItem;

    private ItemScraps() { }

    private static synchronized void build() {
        if (itemToMat != null) return;
        Map<String, NTMMaterial> byKey = new HashMap<>();
        for (NTMMaterial mat : Mats.orderedList) {
            for (String name : mat.names) byKey.putIfAbsent(name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT), mat);
            for (String name : mat.names) byKey.putIfAbsent(name.toLowerCase(Locale.ROOT), mat);
            for (String alias : mat.portIds) byKey.putIfAbsent(alias, mat);
        }

        Map<Item, NTMMaterial> i2m = new IdentityHashMap<>();
        Map<NTMMaterial, Item> m2i = new IdentityHashMap<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(RefStrings.MODID) || !id.getPath().startsWith("scraps_")) continue;
            NTMMaterial mat = byKey.get(id.getPath().substring("scraps_".length()));
            if (mat == null) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            i2m.put(item, mat);
            m2i.putIfAbsent(mat, item);
        }
        matToItem = m2i;
        itemToMat = i2m;
    }

    @Nullable
    public static NTMMaterial materialOf(ItemStack stack) {
        build();
        return itemToMat.get(stack.getItem());
    }

    public static boolean isScrap(ItemStack stack) {
        return materialOf(stack) != null;
    }

    public static boolean isLiquid(ItemStack stack) {
        return PlatformHooks.contains(stack, "liquid") && PlatformHooks.getBoolean(stack, "liquid");
    }

    @Nullable
    public static MaterialStack getMats(ItemStack stack) {
        NTMMaterial mat = materialOf(stack);
        if (mat == null) return null;

        int amount = MaterialShapes.INGOT.q(1);
        if (PlatformHooks.contains(stack, "amount")) {
            amount = PlatformHooks.getInt(stack, "amount");
        }
        return new MaterialStack(mat, amount);
    }

    public static ItemStack create(MaterialStack stack) {
        return create(stack, false);
    }

    public static ItemStack create(MaterialStack stack, boolean liquid) {
        build();
        if (stack.material == null) return ItemStack.EMPTY;
        Item item = matToItem.get(stack.material);
        if (item == null) return ItemStack.EMPTY;
        ItemStack scrap = new ItemStack(item);
        PlatformHooks.editItemTag(scrap, t -> {
            t.putInt("amount", stack.amount);
            if (liquid) t.putBoolean("liquid", true);
        });
        return scrap;
    }
}
