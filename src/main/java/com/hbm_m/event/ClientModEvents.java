package com.hbm_m.event;

// import com.hbm_m.client.render.DoorDebugRenderer;
import com.hbm_m.client.overlay.DoorAnimationDelayHelper;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.implementations.DoorRenderer;
import com.hbm_m.client.render.implementations.MachineAdvancedAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineAssemblerRenderer;
import com.hbm_m.client.render.implementations.MachineChemicalPlantRenderer;
import com.hbm_m.client.render.implementations.MachineHydraulicFrackiningTowerRenderer;
import com.hbm_m.client.render.implementations.MachinePressRenderer;
import com.hbm_m.client.render.shader.IrisExtendedShaderAccess;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
// Обработчик событий клиента, добавляющий подсказки к предметам (опасности, OreDict теги).
// Подсказки показываются при наведении на предмет в инвентаре.
import com.hbm_m.lib.RefStrings;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
                // Tear down any IrisRenderBatch opened during BER (e.g. Chemical Plant
                // Slider/Spinner + instanced Base/Frame). flushBatchIris does its own
                // shader.apply/clear; leaving ACTIVE set after clear() poisons the next
                // frame with "No active program" / matrix uniform errors.
                IrisRenderBatch.closePersistentIfActive();
                MachineAdvancedAssemblerRenderer.flushInstancedBatches(event);
                MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(event);
                MachineAssemblerRenderer.flushInstancedBatches(event);
                DoorRenderer.flushInstancedBatches(event);
                MachinePressRenderer.flushInstancedBatches(event);
                MachineChemicalPlantRenderer.flushInstancedBatches(event);
            }
            // Bump the per-pass shader-lookup cache in IrisExtendedShaderAccess so
            // the next frame re-resolves the shader from the (possibly rebuilt)
            // pipeline instead of returning a stale instance. Within a single
            // frame all draws share the cached shader - this is the single
            // largest CPU saving on the per-part Iris path (~8.78% per profiler
            // trace).
            IrisExtendedShaderAccess.tickPass();
            // Bump the per-frame light-sample cache so the next frame resamples
            // (BUT shadow pass + main pass of the SAME frame still share one
            // sample - fired here, after both passes have drained their BE
            // dispatches). Saves ~17% on dense multiblock scenes by collapsing
            // 11×N redundant 6-block lookups down to N per frame.
            LightSampleCache.onFrameStart();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            // Safety net for the IrisRenderBatch persistent-shadow path. The normal
            // teardown happens inside the first main-pass begin() that detects the
            // pass change. But if the main pass renders ZERO of our BlockEntities
            // (e.g. player turned away and main-camera frustum culled them all
            // while the shadow camera still captured them), no begin(false, …)
            // ever fires and the persistent batch would leak into the next frame -
            // poisoning its first draw with stale shadow-pass uniforms. Closing
            // here guarantees a clean slate at the start of every frame.
            IrisRenderBatch.closePersistentIfActive();
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
            // Инвалидация чанков при смене шейдера (вне render loop - избегаем краша Sodium)
            ShaderCompatibilityDetector.processPendingChunkInvalidation();
        }
    }
}