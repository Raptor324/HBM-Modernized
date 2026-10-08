package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineElectrolyserFluidMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIElectrolyserFluid}: Tanks (42/96/116, 18), Strom (186,18), Fortschritt (62,26); der Knopf (8,82) wechselt
 * zur Metallseite.
 */
public class GUIElectrolyserFluid extends GuiInfoScreen<MachineElectrolyserFluidMenu> {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_electrolyser_fluid.png");
    private final MachineElectrolyserBlockEntity electrolyser;

    public GUIElectrolyserFluid(MachineElectrolyserFluidMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.electrolyser = menu.getBlockEntity();

        this.imageWidth = 210;
        this.imageHeight = 204;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        if (electrolyser != null) {
            electrolyser.tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 42, topPos + 18, 16, 52);
            electrolyser.tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 96, topPos + 18, 16, 52);
            electrolyser.tanks[2].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);

            this.drawElectricityInfo(g, mouseX, mouseY, 186, 18, 16, 89, electrolyser.getPower(), MachineElectrolyserBlockEntity.maxPower);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        if (electrolyser != null && leftPos + 8 <= x && leftPos + 8 + 54 > x && topPos + 82 < y && topPos + 82 + 12 >= y) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean("sgm", true);
            NBTControlPacket.sendToServer(electrolyser.getBlockPos(), data);
            return true;
        }
        return super.mouseClicked(x, y, i);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, (this.imageWidth / 2 - this.font.width(this.title) / 2) - 16, 7, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (electrolyser == null) return;

        int p = (int) (electrolyser.getPower() * 89 / MachineElectrolyserBlockEntity.maxPower);
        g.blit(texture, leftPos + 186, topPos + 107 - p, 210, 89 - p, 16, p, 256, 256);

        if (electrolyser.getPower() >= electrolyser.usageFluid)
            g.blit(texture, leftPos + 190, topPos + 4, 226, 40, 9, 12, 256, 256);

        int e = electrolyser.progressFluid * 41 / Math.max(electrolyser.processFluidTime, 1);
        g.blit(texture, leftPos + 62, topPos + 26, 226, 0, 12, e, 256, 256);

        electrolyser.tanks[0].renderTank(g, leftPos + 42, topPos + 18, 16, 52);
        electrolyser.tanks[1].renderTank(g, leftPos + 96, topPos + 18, 16, 52);
        electrolyser.tanks[2].renderTank(g, leftPos + 116, topPos + 18, 16, 52);
    }
}
