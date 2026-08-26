package com.hbm_m.client;

import com.hbm_m.client.overlay.DoorAnimationDelayHelper;
import com.hbm_m.client.missile.track.MissileTrackClient;
import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
import com.hbm_m.client.render.culling.InstancedRenderFrame;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.event.HazardTooltipHandler;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.helper.ParticleEffectClient;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//?} elif neoforge {
/*@EventBusSubscriber(modid = RefStrings.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
*///?}
@SuppressWarnings({"UnstableApiUsage", "removal"})
public class ClientModEvents {

    private static boolean initialized = false;
    
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Версионно-зависимая регистрация тултипов: на 1.20.1 Architectury даёт 3-параметровый
        // callback (stack, lines, flag), на 1.21.1+ — 4-параметровый с Item.TooltipContext.
        // Тело вынесено в handleItemTooltip, чтобы логика не дублировалась.
        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register((stack, lines, flag) ->
                handleItemTooltip(stack, Minecraft.getInstance().level, lines, flag));
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, lines, context, flag) ->
                handleItemTooltip(stack, context.level(), lines, flag));
        *///?}

        ClientTickEvent.CLIENT_POST.register(client -> {
            com.hbm_m.client.overlay.OverlayInfoToast.tick();
            DoorAnimationDelayHelper.processQueue();
            DoorChunkInvalidationHelper.processPendingInvalidations();
            ShaderCompatibilityDetector.processPendingChunkInvalidation();
            ClientRenderHandler.onClientTickEnd();
            if (client.player != null) {
                ParticleEffectClient.tickRadiationAura(client.player);
            }
        });

        //? if fabric {
        /*WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            ModClothConfig cfg = ModClothConfig.get();
            if (!cfg.enableOcclusionCulling) {
                OcclusionCullingHelper.captureBlockEntityPassFrustum(null);
            } else {
                OcclusionCullingHelper.captureBlockEntityPassFrustum(context.frustum());
            }
        });

        // РЕГРЕССИЯ-СТОП: instanced flush ТОЛЬКО здесь. END — не рисовать batch (грязные texture units).
        WorldRenderEvents.AFTER_BLOCK_ENTITIES.register(context -> {
            var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            InstancedRenderFrame.presentAfterBlockEntities(context.projectionMatrix(), cameraPos);
        });

        WorldRenderEvents.END.register(context -> {
            InstancedRenderFrame.flushDeferredPresent();
        });

        WorldRenderEvents.LAST.register(context -> {
            com.hbm_m.client.render.shader.IrisRenderBatch.closePersistentIfActive();
        });
        *///?}
    }

    /**
     * Центральный обработчик предметных тултипов. Вызывается из версионно-зависимой
     * регистрации {@code ClientTooltipEvent.ITEM} (см. {@link #init()}).
     *
     * <p>Сначала делегирует в {@link ITooltipProvider} — версионно-независимый механизм
     * тултипов предмета (замена переопределению {@code Item.appendHoverText}). Затем,
     * для не-брони, добавляет hazard-тултипы и список тегов.
     */
    private static void handleItemTooltip(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        if (stack.isEmpty()) return;

        // Версионно-независимые предметные тултипы.
        if (stack.getItem() instanceof ITooltipProvider provider) {
            provider.appendHbmTooltip(stack, level, lines, flag);
        }

        // Hazard-тултипы и теги не применяются к броне (у неё свой обработчик).
        if (stack.getItem() instanceof ArmorItem) {
            return;
        }

        HazardTooltipHandler.appendHazardTooltips(stack, Minecraft.getInstance().player, lines);

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
    }

    //? if forge || neoforge {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // РЕГРЕССИЯ СТОП: ПРЕДОТВРАЩЕНИЕ УТЕЧКИ SHADOW PASS:
        // Если в системе остался активен батч теней, но сам проход теней уже завершен (мы в основном кадре),
        // немедленно закрываем его. Это восстановит оригинальный VAO, фазу Iris и очистит шейдер до того,
        // как начнется отрисовка неба, ландшафта, энтити, обводки блоков и руки игрока.
        if (IrisRenderBatch.isActive()) {
            var activeBatch = IrisRenderBatch.active();
            if (activeBatch.isShadowPass() && !ShaderCompatibilityDetector.isRenderingShadowPass()) {
                IrisRenderBatch.closePersistentIfActive();
            }
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            com.hbm_m.client.compat.dh.DhClientState.onAfterSky();
            // Захват чистой ванильной проекции кадра (FOV/zoom/bob) — из неё
            // строится проекция дальнего И ближнего NT-проходов.
            Minecraft mcCapture = Minecraft.getInstance();
            //? if < 1.21.1 {
            com.hbm_m.client.compat.dh.DhClientCompat.captureVanillaProjection(mcCapture.getFrameTime());
            //?} else {
            /*com.hbm_m.client.compat.dh.DhClientCompat.captureVanillaProjection(
                    mcCapture.getTimer().getGameTimeDeltaPartialTick(true));
            *///?}
            MissileTrackClient.beginRenderFrame();
            logShadowBerDiagnostics();
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            ModClothConfig cfg = ModClothConfig.get();
            Minecraft mc = Minecraft.getInstance();
            var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            var frustum = cfg.enableOcclusionCulling ? event.getFrustum() : null;
            InstancedRenderFrame.onBeforeBlockEntities(
                    event.getProjectionMatrix(), cameraPos, frustum);

            // При активном DH ракеты (обе дистанции) рисует EngineHandler на
            // AFTER_WEATHER — единый painter-порядок far->near с NT-частицами.
            // Здесь остаётся только путь без DH (виртуализация дальних треков).
            if (!com.hbm_m.client.compat.dh.DhClientState.isActive()) {
                //? if < 1.21.1 {
                MissileTrackWorldRender.render(mc.getFrameTime(), event.getPoseStack());
                //?} else {
                /*// 1.21.1: getPartialTick() удалён — частичное время тика через DeltaTracker.Timer.
                MissileTrackWorldRender.render(mc.getTimer().getGameTimeDeltaPartialTick(true), event.getPoseStack());
                *///?}
            }
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            Minecraft mc = Minecraft.getInstance();
            var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            ClientRenderHandler.onRenderWorldLate(
                    mc.renderBuffers().bufferSource(),
                    event.getPoseStack(),
                    cameraPos);
            InstancedRenderFrame.presentAfterBlockEntities(event.getProjectionMatrix(), cameraPos);
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            InstancedRenderFrame.onRenderSliceEnd();
            com.hbm_m.client.compat.dh.DhClientState.onAfterLevel();
        }
    }

    /**
     * Instanced flush — только {@link com.hbm_m.client.render.culling.InstancedRenderFrame#presentAfterBlockEntities}
     * на {@code AFTER_BLOCK_ENTITIES}. {@code RenderTickEvent.END} / отложенный flush → белые модели.
     */
    //?}

    // ── Диагностика shadow pass (1.21.1: машины не отбрасывают теней) ──
    /** Предыдущее значение счётчика; логируем только первое значение и смены 0↔N. */
    private static int lastLoggedShadowBerCount = -2;

    private static void logShadowBerDiagnostics() {
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            return;
        }
        int count = com.hbm_m.client.render.AbstractPartBasedRenderer.drainShadowBerInvocations();
        boolean zero = count == 0;
        boolean wasZero = lastLoggedShadowBerCount == 0;
        if (lastLoggedShadowBerCount == -2 || zero != wasZero) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM] Iris shadow pass: {} block-entity renderer invocations last frame{}",
                    count,
                    zero ? " — BERs are NOT called in the shadow pass (empty shadow BE list / terrain-mod interop), shadows impossible"
                         : " — shadow draw path active");
            lastLoggedShadowBerCount = count;
        }
    }
}