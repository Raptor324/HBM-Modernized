package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.armormod.item.JetpackFueledBase;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.inventory.fluid.FluidType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ModEventHandlerClient.getBars/onHUDRenderBar} fuer den Jetpack-Treibstoff: je Ruestungsplatz eine Zeile
 * ueber der Ruestungsanzeige, Balken in Treibstofffarbe; Jetpacks auch als Ruestungsmod. Die Power-Armor-Ladebalken
 * zeichnet weiterhin {@code powerarmor.overlay.OverlayPowerArmor}.
 */
public final class OverlayJetpackFuel {

    private OverlayJetpackFuel() { }

    private static final EquipmentSlot[] SLOTS = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };

    private record Bar(float value, int color) { }

    private static void addBars(List<Bar> bars, ItemStack stack) {
        if (stack.getItem() instanceof JetpackFueledBase jetpack && jetpack.maxFuel > 0) {
            float fuel = (float) JetpackFueledBase.getFuel(stack) / jetpack.maxFuel;
            bars.add(new Bar(fuel, FluidType.forFluid(jetpack.fuel.get()).getColor()));
        }
    }

    /** @return Anzahl gezeichneter Zeilen (fuer leftHeight += 4 je Zeile). */
    public static int render(GuiGraphics g, int screenWidth, int screenHeight, int leftHeight) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        List<List<Bar>> barsList = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            List<Bar> bars = new ArrayList<>();
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                addBars(bars, stack);
                if (ArmorModificationHelper.hasMods(stack)) {
                    for (ItemStack mod : ArmorModificationHelper.pryMods(stack)) {
                        if (!mod.isEmpty()) addBars(bars, mod);
                    }
                }
            }
            barsList.add(bars);
        }

        int left = screenWidth / 2 - 91;
        int rows = 0;

        for (List<Bar> bars : barsList) {
            if (bars.isEmpty()) continue;

            int top = screenHeight - leftHeight + 7;

            for (int i = 0; i < bars.size(); i++) {
                float val = bars.get(i).value();
                int hstart, hend;

                if (i == 0) {
                    hstart = left;
                    hend = hstart + (bars.size() == 1 ? 81 : 40);
                } else {
                    int bl = (int) Math.ceil(40F / (bars.size() - 1));
                    hstart = left + 41 + bl * (i - 1);
                    hend = i == bars.size() - 1 ? left + 81 : hstart + bl;
                    if (i != 1) hstart += 1;
                }

                g.fill(hstart, top - 1, hend, top + 2, 0xFF404040);
                int valx = (int) (hstart + (hend - hstart - 1) * val);
                g.fill(hstart + 1, top, valx, top + 1, 0xFF000000 | (bars.get(i).color() & 0xFFFFFF));
            }

            leftHeight += 4;
            rows++;
        }
        return rows;
    }

    //? if forge {
    public static final net.minecraftforge.client.gui.overlay.IGuiOverlay OVERLAY = (gui, g, partialTick, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        int rows = render(g, w, h, gui.leftHeight);
        gui.leftHeight += rows * 4;
    };
    //?}
}
