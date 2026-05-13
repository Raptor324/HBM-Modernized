package com.hbm_m.event;

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
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
// Обработчик событий клиента, добавляющий подсказки к предметам (опасности, OreDict теги).
// Подсказки показываются при наведении на предмет в инвентаре.
//? if fabric {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}
//? if forge {
import com.hbm_m.lib.RefStrings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//?}
@SuppressWarnings("UnstableApiUsage")
public class ClientModEvents {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> {
            if (stack.isEmpty() || stack.getItem() instanceof ArmorItem) {
                return;
            }

            // Логика для опасностей
            HazardTooltipHandler.appendHazardTooltips(stack, lines);

            // Логика для OreDict тегов
            boolean hasTags = stack.getTags().findAny().isPresent();
            if (hasTags) {
                if (Screen.hasShiftDown()) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable("tooltip.hbm_m.tags").withStyle(ChatFormatting.GRAY));
                    stack.getTags()
                            .map(TagKey::location)
                            .sorted(ResourceLocation::compareTo)
                            .forEach(location -> {
                                lines.add(
                                        Component.literal("  - " + location.toString())
                                                .withStyle(ChatFormatting.DARK_GRAY)
                                );
                            });
                } else {
                    lines.add(
                            Component.translatable("tooltip.hbm_m.hold_shift_for_details")
                                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    );
                }
            }
        });

        ClientTickEvent.CLIENT_POST.register(client -> {
            // Обработка задержки анимации дверей (перед переключением на baked model)
            DoorAnimationDelayHelper.processQueue();
            // Инвалидация чанков дверей при смене состояния (baked model после открытия/закрытия)
            DoorChunkInvalidationHelper.processPendingInvalidations();
            // Инвалидация чанков при смене шейдера (вне render loop - избегаем краша Sodium)
            ShaderCompatibilityDetector.processPendingChunkInvalidation();
        });

        //? if fabric {
        /*net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof com.hbm_m.client.tooltip.CrateContentsTooltipComponent c) {
                return new com.hbm_m.client.tooltip.CrateContentsTooltipComponentRenderer(c);
            }
            return null;
        });
        *///?}

        //? if fabric {
        /*WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            // Late safety net: close any persistent batch left open during BER.
            // Instanced flush is done earlier (BEFORE_BLOCK_OUTLINE) so solid VBO
            // doesn't render after translucent (which breaks depth/outline).
            IrisRenderBatch.closePersistentIfActive();
        });

        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            if (ModClothConfig.useInstancedBatching()) {
                // Best Fabric equivalent to Forge AFTER_BLOCK_ENTITIES:
                // block entities have been dispatched (instances collected) and
                // we're still before outline + translucent passes.
                IrisRenderBatch.closePersistentIfActive();
                org.joml.Matrix4f proj = context.projectionMatrix();
                MachineAdvancedAssemblerRenderer.flushInstancedBatches(proj);
                MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(proj);
                MachineAssemblerRenderer.flushInstancedBatches(proj);
                DoorRenderer.flushInstancedBatches(proj);
                MachinePressRenderer.flushInstancedBatches(proj);
                MachineChemicalPlantRenderer.flushInstancedBatches(proj);
            }
            IrisExtendedShaderAccess.tickPass();
            LightSampleCache.onFrameStart();
            OcclusionCullingHelper.onFrameStart();
            return true;
        });

        WorldRenderEvents.LAST.register(context -> {
            // Safety net for the IrisRenderBatch persistent-shadow path. The normal
            // teardown happens inside the first main-pass begin() that detects the
            // pass change. But if the main pass renders ZERO of our BlockEntities
            // (e.g. player turned away and main-camera frustum culled them all
            // while the shadow camera still captured them), no begin(false, …)
            // ever fires and the persistent batch would leak into the next frame -
            // poisoning its first draw with stale shadow-pass uniforms. Closing
            // here guarantees a clean slate at the start of every frame.
            IrisRenderBatch.closePersistentIfActive();
        });
        *///?}
    }

    // Управляем батчами для ВСЕХ машин
    //? if forge {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            if (ModClothConfig.useInstancedBatching()) {
                // Tear down any IrisRenderBatch opened during BER (e.g. Chemical Plant
                // Slider/Spinner + instanced Base/Frame). flushBatchIris does its own
                // shader.apply/clear; leaving ACTIVE set after clear() poisons the next
                // frame with "No active program" / matrix uniform errors.
                IrisRenderBatch.closePersistentIfActive();
                org.joml.Matrix4f proj = event.getProjectionMatrix();
                MachineAdvancedAssemblerRenderer.flushInstancedBatches(proj);
                MachineHydraulicFrackiningTowerRenderer.flushInstancedBatches(proj);
                MachineAssemblerRenderer.flushInstancedBatches(proj);
                DoorRenderer.flushInstancedBatches(proj);
                MachinePressRenderer.flushInstancedBatches(proj);
                MachineChemicalPlantRenderer.flushInstancedBatches(proj);
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
            // Кэш окклюжена тоже должен жить кадр, а не тик (иначе при FPS>TPS
            // одна и та же видимость переиспользуется несколько рендер-кадров
            // и даёт заметный flicker на BER-only моделях).
            OcclusionCullingHelper.onFrameStart();
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
    //?}
}