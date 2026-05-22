package com.hbm_m.client;



import com.hbm_m.client.overlay.DoorAnimationDelayHelper;

import com.hbm_m.client.render.DoorChunkInvalidationHelper;

import com.hbm_m.client.render.culling.InstancedRenderFrame;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import com.hbm_m.config.ModClothConfig;

import com.hbm_m.event.HazardTooltipHandler;

import dev.architectury.event.events.client.ClientTickEvent;

import dev.architectury.event.events.client.ClientTooltipEvent;



import com.hbm_m.lib.RefStrings;

//? if forge {

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.client.event.RenderLevelStageEvent;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

//?}

import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;

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



            HazardTooltipHandler.appendHazardTooltips(stack, lines);



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

            DoorAnimationDelayHelper.processQueue();

            DoorChunkInvalidationHelper.processPendingInvalidations();

            ShaderCompatibilityDetector.processPendingChunkInvalidation();

            ClientRenderHandler.onClientTickEnd();

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



    //? if forge {

    @SubscribeEvent

    public static void onRenderLevelStage(RenderLevelStageEvent event) {

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            ModClothConfig cfg = ModClothConfig.get();
            Minecraft mc = Minecraft.getInstance();
            var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            var frustum = cfg.enableOcclusionCulling ? event.getFrustum() : null;
            InstancedRenderFrame.onBeforeBlockEntities(
                    event.getProjectionMatrix(), cameraPos, frustum);

            return;

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

        }

    }



    /**

     * Instanced flush — только {@link com.hbm_m.client.render.culling.InstancedRenderFrame#presentAfterBlockEntities}
     * на {@code AFTER_BLOCK_ENTITIES}. {@code RenderTickEvent.END} / отложенный flush → белые модели.

     */

    //?}

}


