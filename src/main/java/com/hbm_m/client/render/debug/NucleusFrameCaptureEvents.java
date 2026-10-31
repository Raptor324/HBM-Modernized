package com.hbm_m.client.render.debug;

import com.hbm_m.lib.RefStrings;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

// Imports above are rewritten to net.neoforged.* by the !loader_packages replacements
// in stonecutter.gradle.kts. Only classes with DIFFERENT names on the two loaders
// (overlay/layer API, tick/frame API, bus annotation) are stonecutter-guarded below.
//? if forge {
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
//?} elif neoforge {
/*import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//?} elif neoforge {
/*@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
*///?}
public final class NucleusFrameCaptureEvents {

    private NucleusFrameCaptureEvents() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_F9) {
            if (Minecraft.getInstance().screen == null) {
                NucleusFrameCapture.requestCapture(Screen.hasShiftDown());
            }
        }
    }

    // Frame boundaries: Forge splits RenderTickEvent by phase, NeoForge has Pre/Post events.
    //? if forge {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            NucleusFrameCapture.onFrameStart();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            NucleusFrameCapture.onFrameEnd();
        }
    }
    //?} elif neoforge {
    /*@SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderFramePre(RenderFrameEvent.Pre event) {
        NucleusFrameCapture.onFrameStart();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        NucleusFrameCapture.onFrameEnd();
    }
    *///?}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!NucleusFrameCapture.isCapturing()) return;
        // RenderLevelStageEvent.Stage.name is private - toString() is the only safe option.
        NucleusFrameCapture.recordEvent("STAGE_" + event.getStage().toString(),
                "RenderLevelStageEvent", null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHandPre(RenderHandEvent event) {
        if (!NucleusFrameCapture.isCapturing()) return;
        NucleusFrameCapture.recordEvent("HAND_RENDER_PRE",
                "hand=" + event.getHand() + " item=" + event.getItemStack().getItem(), null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderHandPost(RenderHandEvent event) {
        if (!NucleusFrameCapture.isCapturing()) return;
        NucleusFrameCapture.recordEvent("HAND_RENDER_POST",
                "hand=" + event.getHand() + " canceled=" + event.isCanceled(), null);
    }

    // GUI overlays: Forge uses RenderGuiOverlayEvent + VanillaGuiOverlay types, NeoForge uses
    // RenderGuiLayerEvent + VanillaGuiLayers ResourceLocations. Both are reduced to the overlay
    // id path and dispatched to the shared handlers below.
    //? if forge {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        guiOverlayPre(event.getOverlay().id().getPath());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        guiOverlayPost(event.getOverlay().id().getPath());
    }
    //?} elif neoforge {
    /*@SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        guiOverlayPre(event.getName().getPath());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onGuiLayerPost(RenderGuiLayerEvent.Post event) {
        guiOverlayPost(event.getName().getPath());
    }
    *///?}

    /** Shared overlay-id dispatch. "camera_overlays" is the 1.21.1 layer that draws the vignette. */
    private static void guiOverlayPre(String idPath) {
        switch (idPath) {
            case "vignette", "camera_overlays" -> NucleusFrameCapture.inspectVignetteState(false);
            case "crosshair", "hotbar" -> {
                if (NucleusFrameCapture.isCapturing()) {
                    NucleusFrameCapture.recordEvent("GUI_OVERLAY_PRE_" + idPath.toUpperCase(Locale.ROOT), null, null);
                }
            }
        }
    }

    /** Shared overlay-id dispatch. "camera_overlays" is the 1.21.1 layer that draws the vignette. */
    private static void guiOverlayPost(String idPath) {
        switch (idPath) {
            case "vignette", "camera_overlays" -> NucleusFrameCapture.inspectVignetteState(true);
            case "crosshair", "hotbar" -> {
                if (NucleusFrameCapture.isCapturing()) {
                    NucleusFrameCapture.recordEvent("GUI_OVERLAY_POST_" + idPath.toUpperCase(Locale.ROOT), null, null);
                }
            }
        }
    }
}
