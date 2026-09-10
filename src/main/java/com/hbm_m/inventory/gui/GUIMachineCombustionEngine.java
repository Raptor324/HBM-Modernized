package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCombustionEngineMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur - Tank-/Energieanzeigen als eigene
 * Fuellrechteck-Overlays.
 *
 * <p>Die beiden Bedienelemente sind 1:1 uebernommen: der <b>Zuendknopf</b> oben und der
 * <b>Drosselregler</b> darunter, der sich von null bis dreissig ziehen laesst. Die Drossel
 * bestimmt direkt den Durchsatz - man stellt den Motor auf den Verbrauch ein, den man wirklich
 * abnimmt, statt ihn volllaufen und abschalten zu lassen.</p>
 */
public class GUIMachineCombustionEngine extends AbstractContainerScreen<MachineCombustionEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_combustion.png");

    private final MachineCombustionEngineBlockEntity blockEntity;

    /**
     * Waehrend des Ziehens zeigt der Schieber sofort die neue Stellung, ohne auf die Antwort des
     * Servers zu warten - sonst haengt er sichtbar hinterher.
     */
    private int dragThrottle = -1;

    public GUIMachineCombustionEngine(MachineCombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            var tank = blockEntity.getTank();
            int fuelH = tank.getMaxFill() > 0 ? tank.getFill() * 52 / tank.getMaxFill() : 0;
            if (fuelH > 0) guiGraphics.fill(x + 35, y + 17 + (52 - fuelH), x + 51, y + 69, 0xFF804000);

            long max = blockEntity.getMaxEnergyStored();
            long energy = blockEntity.getEnergyStored();
            int energyH = max > 0 ? (int) (energy * 52L / max) : 0;
            if (energyH > 0) guiGraphics.fill(x + 143, y + 17 + (52 - energyH), x + 159, y + 69, 0xFFFF3020);

            // Original: der gedrueckte Zuendknopf wird aus (192,0) ueberblendet.
            if (blockEntity.isOn()) {
                guiGraphics.blit(TEXTURE, x + 79, y + 13, 192, 0, 35, 15);
            }

            // Original: der Schieber sitzt bei 79 + setting * 32 / 30.
            int knob = 79 + (throttle() * 32 / MachineCombustionEngineBlockEntity.MAX_THROTTLE);
            guiGraphics.blit(TEXTURE, x + knob, y + 38, 192, 15, 4, 8);
        }
    }

    private int throttle() {
        if (dragThrottle >= 0) return dragThrottle;
        return blockEntity != null ? blockEntity.getSetting() : 0;
    }

    /** Original: {@code setting = (x - guiLeft - 81) * 30 / 32}, geklemmt auf null bis dreissig. */
    private int throttleAt(double mouseX) {
        int raw = (int) ((mouseX - leftPos - 81) * MachineCombustionEngineBlockEntity.MAX_THROTTLE / 32);
        return Math.max(0, Math.min(MachineCombustionEngineBlockEntity.MAX_THROTTLE, raw));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (blockEntity != null) {
            // Original-Trefferflaeche des Zuendknopfs: (89,13), 16 x 14.
            if (isHovering(89, 13, 16, 14, mouseX, mouseY)) {
                com.hbm_m.network.CombustionEngineControlC2SPacket.sendToggle(blockEntity.getBlockPos());
                playClick();
                return true;
            }

            // Original-Trefferflaeche des Reglers: (79,38), 36 x 8.
            if (isHovering(79, 38, 36, 8, mouseX, mouseY)) {
                dragThrottle = throttleAt(mouseX);
                com.hbm_m.network.CombustionEngineControlC2SPacket.sendThrottle(
                        blockEntity.getBlockPos(), dragThrottle);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragThrottle >= 0 && blockEntity != null) {
            int next = throttleAt(mouseX);
            if (next != dragThrottle) {
                dragThrottle = next;
                com.hbm_m.network.CombustionEngineControlC2SPacket.sendThrottle(
                        blockEntity.getBlockPos(), next);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragThrottle = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
