package com.hbm_m.event;

// import com.hbm_m.client.render.DoorDebugRenderer;
import com.hbm_m.client.overlay.DoorAnimationDelayHelper;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
import com.hbm_m.client.render.DoorRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.MachineAssemblerRenderer;
import com.hbm_m.client.render.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.MachinePressRenderer;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
// Обработчик событий клиента, добавляющий подсказки к предметам (опасности, OreDict теги).
// Подсказки показываются при наведении на предмет в инвентаре.
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientModEvents {

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
            if (ModClothConfig.useInstancedBatching()) {
                MachineAdvancedAssemblerRenderer.flushInstancedBatches(event);
                MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(event);
                MachineAssemblerRenderer.flushInstancedBatches(event);
                DoorRenderer.flushInstancedBatches(event);
                MachinePressRenderer.flushInstancedBatches(event);
            }
        }
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Очистка старого кеша culling в начале тика
            OcclusionCullingHelper.onFrameStart();
            
        } else if (event.phase == TickEvent.Phase.END) {
            // Обработка задержки анимации дверей (перед переключением на baked model)
            DoorAnimationDelayHelper.processQueue();
            // Инвалидация чанков дверей при смене состояния (baked model после открытия/закрытия)
            DoorChunkInvalidationHelper.processPendingInvalidations();
            // Инвалидация чанков при смене шейдера (вне render loop — избегаем краша Sodium)
            ShaderCompatibilityDetector.processPendingChunkInvalidation();
        }
    }
}