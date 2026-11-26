package com.hbm_m.client;

//? if forge {
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Haengt {@link Ka52Client} an die Forge-Ereignisse fuer Kamera, Sichtfeld und HUD. */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
public final class Ka52ClientEventsForge {

    private Ka52ClientEventsForge() {}

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity viewer = minecraft.getCameraEntity();
        if (!Ka52Client.isPiloting(viewer)) {
            return;
        }
        float partial = (float) event.getPartialTick();
        boolean firstPerson = minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
        event.setRoll(event.getRoll() + Ka52Client.cameraRoll(viewer, partial, firstPerson));
        event.setPitch(event.getPitch() + Ka52Client.cameraPitchShake(viewer, partial));
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (Ka52Client.isPiloting(Minecraft.getInstance().getCameraEntity())) {
            event.setNewFovModifier(event.getNewFovModifier() * Ka52Client.fovModifier());
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Ka52Client.renderHud(event.getGuiGraphics(), event.getPartialTick());
        PantsirClient.renderHud(event.getGuiGraphics());
    }
}
//?}
