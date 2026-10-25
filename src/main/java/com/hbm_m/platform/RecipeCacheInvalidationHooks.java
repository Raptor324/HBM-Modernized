package com.hbm_m.platform;

import com.hbm_m.recipe.index.ModRecipeIndex;

/**
 * Серверная инвалидация кэша {@link ModRecipeIndex}:
 * инвалидирует кэш в начале цикла перезагрузки датапаков (AddReloadListenerEvent — сброс перед чтением),
 * при синхронизации датапаков с клиентом (OnDatapackSyncEvent — после применения рецептов на сервере)
 * и при обновлении тегов (TagsUpdatedEvent — изменение membership тега без пересборки рецептов,
 * так как byIngredient-индекс строится по снапшоту тегов).
 *
 * ВАЖНО: RecipesUpdatedEvent НЕ используется — он КЛИЕНТСКИЙ на обеих версиях
 * (forge 1.20.1: net.minecraftforge.client.event; neoforge 1.21.1: net.neoforged.neoforge.client.event).
 * Все кэши ModRecipeIndex живут на сервере, поэтому здесь только серверные события.
 */
public final class RecipeCacheInvalidationHooks {

    private static volatile boolean registered;

    private RecipeCacheInvalidationHooks() {}

    public static void register() {
        if (registered) return;
        registered = true;
        //? if forge {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(RecipeCacheInvalidationHooks.class);
        //?} else {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(RecipeCacheInvalidationHooks.class);
         *///?}
    }

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onAddReloadListeners(net.minecraftforge.event.AddReloadListenerEvent event) {
        ModRecipeIndex.invalidate();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent event) {
        ModRecipeIndex.invalidate();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onTagsUpdated(net.minecraftforge.event.TagsUpdatedEvent event) {
        ModRecipeIndex.invalidate();
    }
    //?} else {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void onAddReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        ModRecipeIndex.invalidate();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        ModRecipeIndex.invalidate();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onTagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event) {
        ModRecipeIndex.invalidate();
    }
    *///?}
}
