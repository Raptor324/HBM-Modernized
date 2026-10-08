//? if forge || neoforge {
package com.hbm_m.powerarmor.overlay;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.ModArmorFSB;
import com.hbm_m.powerarmor.ModArmorFSBPowered;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?}

/**
 * 1:1 {@code ArmorHEV.handleOverlay}/{@code renderOverlay}: Mit dem HEV-Anzug ersetzen Zahlen die
 * Herz- und Ruestungsleiste (+Gesundheit x5, ||Ladung, Strahlungsbalken, RAD/s).
 */
//? if forge {
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
*///?}
public final class HEVHudOverlay {

    private HEVHudOverlay() {}

    private static long lastSurvey;
    private static float prevResult;
    private static float lastResult;

    @SubscribeEvent
    //? if forge {
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
    //?} else {
    /*public static void onOverlay(RenderGuiLayerEvent.Pre event) {
    *///?}
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (ModArmorFSB.hasFSBArmorIgnoreCharge(player) && plate.getItem() == ModItems.HEV_PLATE.get()) {

            //? if forge {
            if (event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            //?} else {
            /*if (VanillaGuiLayers.ARMOR_LEVEL.equals(event.getName())) {
            *///?}
                event.setCanceled(true);
                return;
            }

            //? if forge {
            if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
                event.setCanceled(true);
                renderOverlay(event.getGuiGraphics(), event.getWindow().getGuiScaledHeight(), player);
            //?} else {
            /*if (VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName())) {
                event.setCanceled(true);
                renderOverlay(event.getGuiGraphics(), Minecraft.getInstance().getWindow().getGuiScaledHeight(), player);
            *///?}
            }
        }
    }

    private static void renderOverlay(GuiGraphics g, int scaledHeight, Player player) {

        float in = HbmLivingProps.getRadiation(player);

        float radiation = lastResult - prevResult;

        if (System.currentTimeMillis() >= lastSurvey + 1000) {
            lastSurvey = System.currentTimeMillis();
            prevResult = lastResult;
            lastResult = in;
        }

        var font = Minecraft.getInstance().font;
        var pose = g.pose();
        pose.pushPose();

        double scale = 2D;
        pose.scale((float) scale, (float) scale, (float) scale);

        int hX = (int) (8 / scale);
        int hY = (int) ((scaledHeight - 18 - 2) / scale);

        int healthColor = player.getHealth() * 5 > 15 ? 0xff8000 : 0xff0000;
        g.drawString(font, "+" + (int) (player.getHealth() * 5), hX, hY, healthColor, false);

        double c = 0D;
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (EquipmentSlot slot : slots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof ModArmorFSBPowered item)
                c += (double) item.getCharge(armor) / (double) item.getMaxCharge(armor);
        }

        int aX = (int) (70 / scale);
        int aY = (int) ((scaledHeight - 18 - 2) / scale);

        int armorColor = c * 25 > 15 ? 0xff8000 : 0xff0000;
        g.drawString(font, "||" + (int) (c * 25), aX, aY, armorColor, false);

        StringBuilder rad = new StringBuilder("\u2622 [");

        for (int i = 0; i < 10; i++) {

            if (in / 100 > i) {
                int mid = (int) (in - i * 100);

                if (mid < 33)
                    rad.append("..");
                else if (mid < 67)
                    rad.append("|.");
                else
                    rad.append("||");
            } else {
                rad.append(" ");
            }
        }

        rad.append("]");

        int rX = (int) (8 / scale);
        int rY = (int) ((scaledHeight - 40) / scale);

        int radColor = in < 800 ? 0xff8000 : 0xff0000;
        g.drawString(font, rad.toString(), rX, rY, radColor, false);

        pose.popPose();

        if (radiation > 0) {

            int dX = 32;
            int dY = scaledHeight - 55;

            String delta = "" + Math.round(radiation);

            if (radiation > 1000)
                delta = ">1000";
            else if (radiation < 1)
                delta = "<1";

            g.drawString(font, delta + " RAD/s", dX, dY, 0xFF0000, false);
        }
    }
}
//?}
