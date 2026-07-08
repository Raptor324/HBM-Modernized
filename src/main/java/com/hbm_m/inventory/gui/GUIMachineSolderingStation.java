package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.inventory.menu.MachineSolderingStationMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.SolderingStationControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineSolderingStation extends GuiInfoScreen<MachineSolderingStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_soldering_station.png");

    private final MachineSolderingStationBlockEntity solderer;

    public GUIMachineSolderingStation(MachineSolderingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.solderer    = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 204;
    }

    // ─── Background ───────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Collision Prevention indicator (texture at 192, 14 in GUI sheet)
        if (solderer.collisionPrevention)
            g.blit(TEXTURE, leftPos + 5, topPos + 66, 192, 14, 10, 10);

        // Power bar
        int power = (int)(solderer.getEnergyStored() * 52L / Math.max(solderer.getMaxEnergyStored(), 1L));
        power = Math.min(power, 52);
        if (power > 0)
            g.blit(TEXTURE, leftPos + 152, topPos + 70 - power, 176, 52 - power, 16, power);

        // Progress bar (original y = 28)
        int progress = solderer.getProgressScaled(33);
        if (progress > 0)
            g.blit(TEXTURE, leftPos + 72, topPos + 28, 192, 0, progress, 14);

        // Powered indicator
        if (solderer.getEnergyStored() > 0)
            g.blit(TEXTURE, leftPos + 156, topPos + 4, 176, 52, 9, 12);

        drawInfoPanel(g, 78, 67, PanelType.SMALL_BLUE_INFO);
    }

    // ─── Labels ───────────────────────────────────────────────────────────────

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component title = this.title;
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    // ─── Full render (tooltips) ───────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        drawElectricityInfo(g, mouseX, mouseY, 152, 18, 16, 52,
                solderer.getEnergyStored(), solderer.getMaxEnergyStored());

        drawCustomInfoStat(g, mouseX, mouseY,
                leftPos + 78, topPos + 67, 8, 8, leftPos + 78, topPos + 67,
                Component.literal("Progress:"),
                Component.literal("   " + solderer.getProgress() + " / " + solderer.getMaxProgress()));

        // Collision Prevention button tooltip
        drawCustomInfoStat(g, mouseX, mouseY,
                leftPos + 5, topPos + 66, 10, 10, mouseX, mouseY,
                Component.literal("Recipe Collision Prevention: "
                        + (solderer.collisionPrevention
                                ? ChatFormatting.GREEN + "ON"
                                : ChatFormatting.RED   + "OFF")),
                Component.literal("Prevents no-fluid recipes from being processed"),
                Component.literal("when fluid is present."));

        this.renderTooltip(g, mouseX, mouseY);
    }

    // ─── Mouse click — toggle Collision Prevention ────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int bx = leftPos + 5, by = topPos + 66;
        if (mx >= bx && mx < bx + 10 && my > by && my <= by + 10) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            SolderingStationControlPacket.sendToServer(solderer.getBlockPos());
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }
}
