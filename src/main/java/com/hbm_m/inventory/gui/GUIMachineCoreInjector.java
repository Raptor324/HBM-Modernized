package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCoreInjectorMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Direktport der Slot-/Tank-Koordinaten aus {@code GUICoreInjector} (1.7.10 Original).
 *  Die beiden Crafting-Slotpaare des Originals (Fusionsbrennstab-Rezeptur) entfallen,
 *  weil das BlockEntity diese Crafting-Logik noch nicht implementiert - siehe
 *  {@link MachineCoreInjectorMenu}. */
public class GUIMachineCoreInjector extends GuiInfoScreen<MachineCoreInjectorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/dfc/gui_injector.png");

    private final MachineCoreInjectorBlockEntity be;

    private static final int TANK_DEUTERIUM_X = 44;
    private static final int TANK_TRITIUM_X = 116;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;

    public GUIMachineCoreInjector(MachineCoreInjectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.be = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        FluidTank deuterium = be.getTank(MachineCoreInjectorBlockEntity.TANK_DEUTERIUM);
        FluidTank tritium = be.getTank(MachineCoreInjectorBlockEntity.TANK_TRITIUM);
        if (deuterium != null) {
            deuterium.renderTank(guiGraphics, leftPos + TANK_DEUTERIUM_X, topPos + TANK_Y, TANK_W, TANK_H);
        }
        if (tritium != null) {
            tritium.renderTank(guiGraphics, leftPos + TANK_TRITIUM_X, topPos + TANK_Y, TANK_W, TANK_H);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FluidTank deuterium = be.getTank(MachineCoreInjectorBlockEntity.TANK_DEUTERIUM);
        FluidTank tritium = be.getTank(MachineCoreInjectorBlockEntity.TANK_TRITIUM);
        if (deuterium != null && isPointInRect(TANK_DEUTERIUM_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            deuterium.renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_DEUTERIUM_X, topPos + TANK_Y, TANK_W, TANK_H);
        }
        if (tritium != null && isPointInRect(TANK_TRITIUM_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            tritium.renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_TRITIUM_X, topPos + TANK_Y, TANK_W, TANK_H);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
