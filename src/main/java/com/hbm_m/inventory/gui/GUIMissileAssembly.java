package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

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

        // DRAW MISSILE: liegend, um die Hochachse kreisend, auf 8 Slots Breite skaliert
        if (menu.blockEntity == null) return;
        com.hbm_m.item.missile.MissileStruct missile = menu.blockEntity.getStruct();
        double height = com.hbm_m.client.render.util.MissilePronter.guiHeight(missile);

        var ps = gui.pose();
        ps.pushPose();
        ps.translate(x + 88, y + 98, 100);
        ps.mulPose(com.mojang.math.Axis.YN.rotationDegrees(System.currentTimeMillis() / 10 % 360));

        double size = 8 * 18;
        float scale = (float) (size / Math.max(height, 6));

        ps.translate(height / 2 * scale, 0, 0);
        ps.scale(scale, scale, scale);

        ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));
        ps.scale(-1, -1, -1);

        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        RenderSystem.disableCull();
        com.hbm_m.client.render.util.MissilePronter.prontMissile(missile, ps, gui.bufferSource(), net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        gui.flush();
        RenderSystem.enableCull();
        com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        ps.popPose();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, gui, mouseX, mouseY, delta);
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, 115, 35, 18, 18)) {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            // тайл может отсутствовать в реплее Flashback
            if (menu.blockEntity == null) return super.mouseClicked(mouseX, mouseY, button);
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
