package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.inventory.menu.MachineMiningDrillMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.MiningDrillToggleC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIMachineExcavator} (1.7.10, vom User bereitgestellt) auf GuiGraphics.
 * Alle Koordinaten/Source-Rechtecke sind direkt aus dem Original uebernommen - {@code gui_mining_drill.png}
 * ist wie eine klassische Vanilla-Textur aufgebaut (xSize=242, ySize=204), inklusive einer
 * "versteckten" Sprite-Region rechts/unterhalb von (204,96) fuer Zustands-Overlays (Schalter-an,
 * Lampen gruen/rot-blinkend, Blitz-Symbol, fehlendes-Drillbit-Warnsymbol, Energie-Fuellbalken).
 */
public class GUIMachineMiningDrill extends GuiInfoScreen<MachineMiningDrillMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_mining_drill.png");

    private record ToggleButton(String key, int x) {}

    private static final ToggleButton[] TOGGLES = {
            new ToggleButton("drill", 6),
            new ToggleButton("crusher", 30),
            new ToggleButton("walling", 54),
            new ToggleButton("veinminer", 78),
            new ToggleButton("silktouch", 102),
    };

    private final MachineMiningDrillBlockEntity miningDrill;

    public GUIMachineMiningDrill(MachineMiningDrillMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.miningDrill = menu.getBlockEntity();
        this.imageWidth = 242;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 242, 96);
        guiGraphics.blit(TEXTURE, this.leftPos + 33, this.topPos + 104, 33, 104, 176, 100);

        long energy = miningDrill.getEnergyStored();
        long maxEnergy = Math.max(1L, miningDrill.getMaxEnergyStored());
        int barHeight = (int) (52L * energy / maxEnergy);
        if (barHeight > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 220, this.topPos + 70 - barHeight, 229, 156 - barHeight, 16, barHeight);
        }

        if (energy > miningDrill.getEnergyPerTick()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 224, this.topPos + 4, 239, 156, 9, 12);
        }

        boolean blink = System.currentTimeMillis() % 1000 < 500;
        if (!miningDrill.hasDrillbitInstalled() && blink) {
            guiGraphics.blit(TEXTURE, this.leftPos + 171, this.topPos + 74, 209, 154, 18, 18);
        }

        drawToggle(guiGraphics, 0, miningDrill.enableDrill,
                miningDrill.hasDrillbitInstalled() && energy >= miningDrill.getEnergyPerTick(), blink);
        drawToggle(guiGraphics, 1, miningDrill.enableCrusher, true, blink);
        drawToggle(guiGraphics, 2, miningDrill.enableWalling, true, blink);
        drawToggle(guiGraphics, 3, miningDrill.enableVeinMiner, miningDrill.canVeinMine(), blink);
        drawToggle(guiGraphics, 4, miningDrill.enableSilkTouch, miningDrill.canSilkTouch(), blink);
    }

    private void drawToggle(GuiGraphics guiGraphics, int index, boolean enabled, boolean lampOk, boolean blink) {
        int x = TOGGLES[index].x();
        if (!enabled) return;

        guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + 42, 209, 114, 20, 40);
        if (lampOk) {
            guiGraphics.blit(TEXTURE, this.leftPos + x + 5, this.topPos + 5, 209, 104, 10, 10);
        } else if (blink) {
            guiGraphics.blit(TEXTURE, this.leftPos + x + 5, this.topPos + 5, 219, 104, 10, 10);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ToggleButton toggle : TOGGLES) {
            if (isPointInRect(toggle.x(), 42, 20, 40, (int) mouseX, (int) mouseY)) {
                playClickSound();
                MiningDrillToggleC2SPacket.sendToServer(miningDrill.getBlockPos(), toggle.key());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 41, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
            220, 18, 16, 52,
            miningDrill.getEnergyStored(), miningDrill.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
            6, 42, 96, 40,
            this.leftPos + 6, this.topPos + 42,
                Component.literal("Progress:"),
                Component.literal("   " + miningDrill.getProgress() + " / " + miningDrill.getMaxProgress()
                        + "   |   Depth: " + miningDrill.getDrillDepth()));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
