package com.hbm_m.client.weapon;

//? if forge {
import com.hbm_m.lib.RefStrings;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-Ereignisse des Waffensystems, siehe {@link GunClientHooks}. */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GunClientEventsForge {

    private GunClientEventsForge() { }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (GunClientHooks.renderHand(event.getPartialTick())) event.setCanceled(true);
        } else {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!GunClientHooks.holdingGun()) return;
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            if (GunClientHooks.renderCrosshair(event.getGuiGraphics())) event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) GunClientHooks.onClientTickEnd();
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        event.setNewFovModifier(GunClientHooks.modifyFov(event.getNewFovModifier()));
    }
}
//?} elif neoforge {
/*import com.hbm_m.lib.RefStrings;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/^* NeoForge-Gegenstueck der Forge-Ereignisse des Waffensystems (gleiches Verhalten), siehe {@link GunClientHooks}. ^/
@EventBusSubscriber(modid = RefStrings.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GunClientEventsForge {

    private GunClientEventsForge() { }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (GunClientHooks.renderHand(event.getPartialTick())) event.setCanceled(true);
        } else {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (!GunClientHooks.holdingGun()) return;
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            if (GunClientHooks.renderCrosshair(event.getGuiGraphics())) event.setCanceled(true);
        } else if (VanillaGuiLayers.HOTBAR.equals(event.getName())) {
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GunClientHooks.onClientTickEnd();
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!GunClientHooks.holdingGun()) return;
        event.setNewFovModifier(GunClientHooks.modifyFov(event.getNewFovModifier()));
    }
}
*///?}
