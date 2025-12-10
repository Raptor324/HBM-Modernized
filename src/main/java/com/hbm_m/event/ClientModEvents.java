package com.hbm_m.event;

// import com.hbm_m.client.render.DoorDebugRenderer;
import com.hbm_m.client.render.DoorRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.MachinePressRenderer;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.ImmediateFallbackRenderer;
import com.hbm_m.client.render.shader.RenderPathManager;
// Обработчик событий клиента, добавляющий подсказки к предметам (опасности, OreDict теги).
// Подсказки показываются при наведении на предмет в инвентаре.
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.DoorPartAABBRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientModEvents {

    private static int memoryCleanupCounter = 0;
    private static final int MEMORY_CLEANUP_INTERVAL = 1200;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        
        if (stack.isEmpty() || stack.getItem() instanceof ArmorItem) {
            return;
        }
        
        // Логика для опасностей 
        HazardTooltipHandler.appendHazardTooltips(event.getItemStack(), event.getToolTip());

        // Логика для OreDict тегов 
        boolean hasTags = event.getItemStack().getTags().findAny().isPresent();
        if (hasTags) {
            if (Screen.hasShiftDown()) {
                event.getToolTip().add(Component.empty());
                event.getToolTip().add(Component.translatable("tooltip.hbm_m.tags").withStyle(ChatFormatting.GRAY));
                event.getItemStack().getTags()
                    .map(TagKey::location)
                    .sorted(ResourceLocation::compareTo)
                    .forEach(location -> {
                        event.getToolTip().add(
                            Component.literal("  - " + location.toString())
                                     .withStyle(ChatFormatting.DARK_GRAY)
                        );
                    });
            } else {
                event.getToolTip().add(
                    Component.translatable("tooltip.hbm_m.hold_shift_for_details")
                             .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                );
            }
        }
    }

    // Управляем батчами для ВСЕХ машин
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            MachineAdvancedAssemblerRenderer.flushInstancedBatches();
            DoorRenderer.flushInstancedBatches();
            MachinePressRenderer.flushInstancedBatches();
            
            // НОВОЕ: Завершаем immediate рендер батчи
            ImmediateFallbackRenderer.endBatch();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Очистка старого кеша culling в начале тика
            OcclusionCullingHelper.onFrameStart();
            
        } else if (event.phase == TickEvent.Phase.END) {
            // Проверяем render path только в конце тика
            RenderPathManager.checkAndUpdate();
            
            // Периодическая очистка памяти immediate рендера
            memoryCleanupCounter++;
            if (memoryCleanupCounter >= MEMORY_CLEANUP_INTERVAL) {
                memoryCleanupCounter = 0;
                
                // Очищаем кеши рендереров
                DoorRenderer.clearAllCaches();
                
                // Очищаем глобальные кеши
                GlobalMeshCache.clearAll();
                DoorPartAABBRegistry.clear();
                
                // Принудительно очищаем состояние Tesselator
                ImmediateFallbackRenderer.forceReset();
                
                // Вызываем сборку мусора если нужно
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                
                // Если используется больше 80% памяти - запускаем GC
                if (usedMemory > maxMemory * 0.8) {
                    MainRegistry.LOGGER.debug("Memory usage high ({}%), triggering GC",
                            (usedMemory * 100) / maxMemory);
                    System.gc();
                }
            }
        }
    }
}