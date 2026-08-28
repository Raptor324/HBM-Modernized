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
        // KHR_debug-колбэк (первый кадр, render thread): ловит сообщения драйвера
        // о битом GL-состоянии в момент проблемного вызова. Диагностика
        // «чёрного экрана» трек-рендера — см. GlDebugProbe.
        com.hbm_m.client.render.GlDebugProbe.enableOnce();
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

        // ВАТЧДОГ program-desync (Oculus без пака): если RS-шейдер числится, а
        // фактическая GL-программа = 0 (сырой сброс), сбросить кеш — иначе ваниль
        // пропустит честный бинд и кадр перестанет перерисовываться до появления
        // любого другого честного apply (симптом «чёрный экран после смерти ракеты,
        // лечится только появлением машин»). Вызывается на КАЖДОЙ стадии кадра.
        com.hbm_m.client.render.shader.ShaderBindResync.enforceGlProgramConsistency();
        // ВОССТАНОВЛЕНИЕ ТЕКСТУРНЫХ ЮНИТОВ: px.pad.* показала, что к фазе BER
        // физические units=[0/0/0] при «живом» кеше — ванильные дро но-опятся и
        // рисуют чёрное до первого block_lit (пусковая лечит кадр). Первое наше
        // событие в кадре — AFTER_SKY: восстанавливаем троицу до энтити/BER.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            com.hbm_m.client.render.shader.ShaderBindResync.restoreVanillaTextureBindings();
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            com.hbm_m.client.render.FrameStateProbe.snap("px.sky");
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
            com.hbm_m.client.render.FrameStateProbe.snap("px.entities");

            ModClothConfig cfg = ModClothConfig.get();
            Minecraft mc = Minecraft.getInstance();
            var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            var frustum = cfg.enableOcclusionCulling ? event.getFrustum() : null;
            InstancedRenderFrame.onBeforeBlockEntities(
                    event.getProjectionMatrix(), cameraPos, frustum);

            // РЕГРЕССИЯ-СТОП (двойная отрисовка): меши ракет рисуются ТОЛЬКО
            // в EngineHandler на AFTER_WEATHER (renderFiltered) — единый
            // painter-порядок far->near с NT-частицами для обоих случаев
            // (DH активен / нет). Второй проход здесь давал фантомную копию
            // под другой проекцией и удвоенные draw/GL-ошибки.
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            Minecraft mc = Minecraft.getInstance();
            var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            ClientRenderHandler.onRenderWorldLate(
                    mc.renderBuffers().bufferSource(),
                    event.getPoseStack(),
                    cameraPos);
            com.hbm_m.client.render.FrameStateProbe.snap("px.be");
            InstancedRenderFrame.presentAfterBlockEntities(event.getProjectionMatrix(), cameraPos);
            com.hbm_m.client.render.FrameStateProbe.snap("px.be2");
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            com.hbm_m.client.render.FrameStateProbe.snap("px.afterlevel");
            com.hbm_m.client.render.shader.ShaderBindResync.forceIrisDepthColorEnabled();
            InstancedRenderFrame.onRenderSliceEnd();
            com.hbm_m.client.compat.dh.DhClientState.onAfterLevel();
        }
    }

    // ── Слепая зона FrameStateProbe: рука и GUI ─────────────────────────────
    // px.*-зонды останавливаются на AFTER_LEVEL, а «полностью чёрный экран»
    // (GUI жив, рука пропала) может рисоваться именно после него. Эти снапы
    // замеряют пиксели на входе в проход руки и в GUI-фазу.
    //? if forge {
    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        com.hbm_m.client.render.FrameStateProbe.snap("px.hand");
    }

    @SubscribeEvent
    public static void onRenderGuiPre(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
        // Виньетка использует multiply-блендинг (ZERO/ONE_MINUS_SRC_COLOR) и
        // молча ломается при расхождении кеша факторов блендинга с физикой
        // (GlStateManager._blendFuncSeparate но-опится при «совпадении»).
        // Форсируем честный блендинг ДО Gui.render.
        com.hbm_m.client.render.shader.ShaderBindResync.forceHonestBlendState();
        com.hbm_m.client.render.FrameStateProbe.snap("px.gui.pre");
        com.hbm_m.client.render.FrameStateProbe.snapGuiEffects();
    }

    // Бисекция GUI-оверлеев: на Pre каждого оверлея читаем центральный пиксель;
    // при провале яркости логируем id оверлея, зачернившего кадр (см. GuiOverlayBisectProbe).
    @SubscribeEvent
    public static void onGuiOverlayPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        com.hbm_m.client.render.GuiOverlayBisectProbe.onOverlayPre(event.getOverlay().id());
    }

    @SubscribeEvent
    public static void onRenderGuiPost(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        com.hbm_m.client.render.FrameStateProbe.snap("px.gui.post");
        com.hbm_m.client.render.GuiOverlayBisectProbe.resetFrame();
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        com.hbm_m.client.render.FrameStateProbe.snap("px.hand");
    }

    @SubscribeEvent
    public static void onRenderGuiPre(net.neoforged.neoforge.client.event.RenderGuiEvent.Pre event) {
        com.hbm_m.client.render.FrameStateProbe.snap("px.gui.pre");
    }

    @SubscribeEvent
    public static void onRenderGuiPost(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        com.hbm_m.client.render.FrameStateProbe.snap("px.gui.post");
    }
    *///?}

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