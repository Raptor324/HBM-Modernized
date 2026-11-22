package com.hbm_m.util;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompletionTracker {
    private static final Map<ResourceLocation, CompletionStatus> STATUS_MAP = new ConcurrentHashMap<>();

    /**
     * Helper method to mark an item or block with a completion status in one line during registration.
     * Example:
     * public static final RegistrySupplier<Item> MY_ITEM = CompletionTracker.mark(CompletionStatus.WIP, ITEMS.register("my_item", ...));
     */
    public static <T> RegistrySupplier<T> mark(CompletionStatus status, RegistrySupplier<T> supplier) {
        STATUS_MAP.put(supplier.getId(), status);
        return supplier;
    }

    public static CompletionStatus getStatus(ResourceLocation id) {
        return STATUS_MAP.get(id);
    }
}
