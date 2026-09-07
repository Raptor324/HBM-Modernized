package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.fusion.FusionBreederBlockEntity;
import com.hbm_m.inventory.menu.MachineFusionBreederMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1-Port von {@code GUIFusionBreeder} (1.7.10). */
public class GUIMachineFusionBreeder extends GuiInfoScreen<MachineFusionBreederMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_breeder.png");
    *///?} else {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_breeder.png");
    //?}

    public GUIMachineFusionBreeder(MachineFusionBreederMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    private FusionBreederBlockEntity be() {
        return menu.getBlockEntity();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        FusionBreederBlockEntity be = be();

        int p = (int) Math.ceil(be.progress * 42 / FusionBreederBlockEntity.CAPACITY);
        if (p > 0) guiGraphics.blit(TEXTURE, this.leftPos + 67, this.topPos + 48, 176, 0, Math.min(p, 42), 10);

        double gauge = 1D - Math.pow(Math.E, -be.neutronEnergySync * 10 / FusionBreederBlockEntity.CAPACITY);
        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 88, this.topPos + 32, gauge, 5, 2, 1, 0xA00000);

        be.tanks[0].renderTank(guiGraphics, this.leftPos + 26, this.topPos + 18, 16, 52);
        be.tanks[1].renderTank(guiGraphics, this.leftPos + 134, this.topPos + 18, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 35, this.imageHeight - 93, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FusionBreederBlockEntity be = be();

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 79, 23, 18, 18, mouseX, mouseY,
                Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal((int) Math.ceil(be.neutronEnergySync) + " flux/t")
                                .withStyle(ChatFormatting.WHITE)));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 67, 46, 42, 14, mouseX, mouseY,
                Component.literal((int) Math.ceil(be.progress) + " / "
                        + (int) Math.ceil(FusionBreederBlockEntity.CAPACITY) + " flux"));

        be.tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 26, this.topPos + 18, 16, 52);
        be.tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 134, this.topPos + 18, 16, 52);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
