package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import com.hbm_m.client.GuiCompat;
import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.menu.MachineOilburnerMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port of {@code GUIOilburner} (1.7.10 Original). Also used for the {@code oilburner_hp} variant
 * (uses {@code gui_oilburner_hp.png}, chosen by looking at which block the block entity belongs to).
 * <p>
 * SCOPE-Vereinfachung: kein manueller On/Off-Toggle-Button mehr (Original: Mausklick bei (80,54)
 * sendete ein {@code NBTControlPacket} um {@code isOn} umzuschalten) - {@link MachineOilburnerBlockEntity}
 * ersetzt das durch ein Redstone-Signal (siehe {@code serverTick}), es gibt daher kein clientseitig
 * togglebares Flag mehr. Die Flammen-/Fuellstand-Anzeige nutzt stattdessen {@code isBurning()}.
 */
public class GUIMachineOilburner extends GuiInfoScreen<MachineOilburnerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_oilburner.png");
    private static final ResourceLocation TEXTURE_HP =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_oilburner_hp.png");

    private final MachineOilburnerBlockEntity oilburner;
    private final ResourceLocation texture;

    public GUIMachineOilburner(MachineOilburnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.oilburner = menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        this.texture = this.oilburner != null && this.oilburner.getBlockState().is(com.hbm_m.block.ModBlocks.OILBURNER_HP.get()) ? TEXTURE_HP : TEXTURE;
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Heat bar (right side tank-style indicator), ported 1:1 from GUIOilburner.
        int heat = oilburner.getHeatStored();
        int maxHeat = oilburner.getMaxHeatStored();
        int i = maxHeat > 0 ? heat * 52 / maxHeat : 0;
        if (i > 0) {
            guiGraphics.blit(texture, this.leftPos + 116, this.topPos + 69 - i, 194, 52 - i, 16, i);
        }

        if (oilburner.isBurning()) {
            guiGraphics.blit(texture, this.leftPos + 70, this.topPos + 54, 210, 0, 35, 14);

            FT_Flammable trait = FluidType.getTrait(oilburner.getOilTank().getStoredFluid(), FT_Flammable.class);
            if (oilburner.getOilTank().getFluidAmountMb() > 0 && trait != null) {
                guiGraphics.blit(texture, this.leftPos + 79, this.topPos + 34, 176, 0, 18, 18);
            }
        }

        // Oil tank fluid render, ported from diFurnace.tank.renderTank(...).
        oilburner.getOilTank().renderTank(guiGraphics, this.leftPos + 44, this.topPos + 17, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (oilburner != null) {
        int heat = oilburner.getHeatStored();
        int maxHeat = oilburner.getMaxHeatStored();
        List<Component> heatTooltip = new ArrayList<>();
        heatTooltip.add(Component.literal(
                String.format(Locale.US, "%,d", Math.min(heat, maxHeat)) + " / " + String.format(Locale.US, "%,d", maxHeat) + " TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 116, this.topPos + 17, 16, 52,
                mouseX, mouseY, heatTooltip.toArray(new Component[0]));

        FT_Flammable trait = FluidType.getTrait(oilburner.getOilTank().getStoredFluid(), FT_Flammable.class);
        if (trait != null) {
            int setting = oilburner.getSetting();
            List<Component> burnTooltip = new ArrayList<>();
            burnTooltip.add(Component.literal(setting + " mB/t"));
            burnTooltip.add(Component.literal(
                    String.format(Locale.US, "%,d", (int) (trait.getHeatEnergy() / 1000L) * setting) + " TU/t"));
            drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                    this.leftPos + 79, this.topPos + 34, 18, 18,
                    mouseX, mouseY, burnTooltip.toArray(new Component[0]));
        }

        oilburner.getOilTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 44, this.topPos + 17, 16, 52);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
