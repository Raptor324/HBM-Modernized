package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MissileAssemblyMenu;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.BuildMissilePacket;
import com.hbm_m.network.ModPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMissileAssembly extends AbstractContainerScreen<MissileAssemblyMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/gui_missile_assembly.png");

    public GUIMissileAssembly(MissileAssemblyMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (menu.chipState() == 1) gui.blit(TEXTURE, x + 13, y + 23, 194, 0, 6, 8);
        if (menu.warheadState() == 1) gui.blit(TEXTURE, x + 31, y + 23, 194, 0, 6, 8);
        if (menu.fuselageState() == 1) gui.blit(TEXTURE, x + 49, y + 23, 194, 0, 6, 8);
        if (menu.stabilityState() == 1) gui.blit(TEXTURE, x + 67, y + 23, 194, 0, 6, 8);
        if (menu.stabilityState() == 0) gui.blit(TEXTURE, x + 67, y + 23, 200, 0, 6, 8);
        if (menu.thrusterState() == 1) gui.blit(TEXTURE, x + 85, y + 23, 194, 0, 6, 8);

        if (menu.canBuild()) {
            gui.blit(TEXTURE, x + 115, y + 35, 176, 0, 18, 18);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.canBuild() && isMouseOver(mouseX, mouseY, 115, 35, 18, 18)) {
            ModPacketHandler.sendToServer(ModPacketHandler.BUILD_MISSILE,
                    new BuildMissilePacket(menu.blockEntity.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX
                && mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY;
    }
}
