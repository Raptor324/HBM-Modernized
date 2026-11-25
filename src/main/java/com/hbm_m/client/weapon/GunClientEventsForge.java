package com.hbm_m.client.weapon;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.hbm_m.client.render.weapon.bridge.ExternalGunRenderBridge;
//? if forge {
import com.hbm_m.lib.RefStrings;

import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-Ereignisse des Waffensystems, siehe {@link GunClientHooks}. */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GunClientEventsForge {

    private GunClientEventsForge() { }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        // Externe Renderer-Bridge (TACZ) uebernimmt die Erstperson-Darstellung (Waffe UND Haende):
        // HBM zeichnet nicht und bricht das Ereignis nicht ab, damit der externe Handler es nehmen kann.
        if (ExternalGunRenderBridge.get().isHandled(GunClientHooks.clientPlayer().getMainHandItem())) return;
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (GunClientHooks.renderHand(event.getPartialTick())) event.setCanceled(true);
        } else {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!GunClientHooks.holdingGun()) return;
        boolean external = ExternalGunRenderBridge.get().isHandled(GunClientHooks.clientPlayer().getMainHandItem());
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            // Externe Bridge besitzt die Fadenkreuz-Unterdrueckung (ADS) und das Zielfernrohr:
            // weder HBM-Fadenkreuz noch Vanilla-Fadenkreuz anfassen.
            if (!external && GunClientHooks.renderCrosshair(event.getGuiGraphics())) event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            // HUD-Komponenten (Munition/Haltbarkeit) bleiben; das Scope-Overlay skippt renderHotbarExtras selbst.
            GunClientHooks.renderHotbarExtras(event.getGuiGraphics());
        }
    }

    /** Original {@code HbmKeybinds}: mit Waffe in der Hand wird statt anzugreifen/Bloecke zu waehlen geschossen/gezielt. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!GunClientHooks.holdingGun()) return;
        if (event.isAttack() || event.isPickBlock()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Original {@code ModEventHandlerRenderer.onDrawHighlight}: der Bohrer zeichnet seinen eigenen Abbaubereich. */
    @SubscribeEvent
    public static void onBlockHighlight(net.minecraftforge.client.event.RenderHighlightEvent.Block event) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        var held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() == com.hbm_m.item.weapon.sedna.WeaponItems.gun("gun_drill")) {
            com.hbm_m.item.weapon.sedna.factory.XFactoryDrill.drawBlockHighlight(player, held, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource());
            event.setCanceled(true);
        }
    }

    /** Original {@code ModEventHandlerClient}: Rueckstoss, pro Frame integriert (siehe {@link GunClientHooks#onRenderFrame}). */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) GunClientHooks.onRenderFrame();
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        // Externe Bridge besitzt das ADS-FOV (CameraSetupEvent) - HBM-Zoom auslassen.
        if (ExternalGunRenderBridge.get().isHandled(GunClientHooks.clientPlayer().getMainHandItem())) return;
        event.setNewFovModifier(GunClientHooks.modifyFov(event.getNewFovModifier()));
    }
}
//?} elif neoforge {
/*import com.hbm_m.lib.RefStrings;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/^* NeoForge-Gegenstueck der Forge-Ereignisse des Waffensystems (gleiches Verhalten), siehe {@link GunClientHooks}. ^/
@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
public final class GunClientEventsForge {

    private GunClientEventsForge() { }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        // Externe Renderer-Bridge (TACZ) uebernimmt die Erstperson-Darstellung (Waffe UND Haende):
        // HBM zeichnet nicht und bricht das Ereignis nicht ab, damit der externe Handler es nehmen kann.
        if (ExternalGunRenderBridge.get().isHandled(GunClientHooks.clientPlayer().getMainHandItem())) return;
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (GunClientHooks.renderHand(event.getPartialTick())) event.setCanceled(true);
        } else {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (!GunClientHooks.holdingGun()) return;
        boolean external = ExternalGunRenderBridge.get().isHandled(GunClientHooks.clientPlayer().getMainHandItem());
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            // Externe Bridge besitzt die Fadenkreuz-Unterdrueckung (ADS) und das Zielfernrohr:
            // weder HBM-Fadenkreuz noch Vanilla-Fadenkreuz anfassen.
            if (!external && GunClientHooks.renderCrosshair(event.getGuiGraphics())) event.setCanceled(true);
        } else if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            // HUD-Komponenten (Munition/Haltbarkeit) bleiben; das Scope-Overlay skippt renderHotbarExtras selbst.
            GunClientHooks.renderHotbarExtras(event.getGuiGraphics());
        }
    }

    /^* Original {@code HbmKeybinds}: mit Waffe in der Hand wird statt anzugreifen/Bloecke zu waehlen geschossen/gezielt. ^/
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!GunClientHooks.holdingGun()) return;
        if (event.isAttack() || event.isPickBlock()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /^* Original {@code ModEventHandlerRenderer.onDrawHighlight}: der Bohrer zeichnet seinen eigenen Abbaubereich. ^/
    @SubscribeEvent
    public static void onBlockHighlight(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block event) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        var held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() == com.hbm_m.item.weapon.sedna.WeaponItems.gun("gun_drill")) {
            com.hbm_m.item.weapon.sedna.factory.XFactoryDrill.drawBlockHighlight(player, held,
                    event.getDeltaTracker().getGameTimeDeltaPartialTick(true), event.getPoseStack(), event.getMultiBufferSource());
            event.setCanceled(true);
        }
    }

    /* Original {@code ModEventHandlerClient}: Rueckstoss, pro Frame integriert (siehe {@link GunClientHooks#onRenderFrame}). ^/
    @SubscribeEvent
    public static void onRenderFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        GunClientHooks.onRenderFrame();
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        // Externe Bridge besitzt das ADS-FOV (CameraSetupEvent) - HBM-Zoom auslassen.
        if (ExternalGunRenderBridge.get().isHandled(GunClientHooks.clientPlayer().getMainHandItem())) return;
        event.setNewFovModifier(GunClientHooks.modifyFov(event.getNewFovModifier()));
    }
}
*///?}
