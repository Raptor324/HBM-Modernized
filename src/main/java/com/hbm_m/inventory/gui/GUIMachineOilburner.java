package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import com.hbm_m.client.GuiCompat;
import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.menu.MachineOilburnerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ToggleOilburnerC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Порт {@code GUIOilburner} (1.7.10 Original). Также используется вариантом
 * {@code oilburner_hp} ({@code gui_oilburner_hp.png}, выбирается по блоку BE).
 *
 * <p>Порт 1:1: тепловая колонна (116,17..69), пламя при isOn (70,54) 35×14,
 * иконка огня (79,34) 18×18 при isOn + есть топливо и FT_Flammable, бак
 * (44,17) 16×52, кнопка вкл/выкл (80,54) 16×14 → {@link ToggleOilburnerC2SPacket}
 * (оригинальный NBTControlPacket "toggle"), тултипы горения "N mB/t" / "N TU/t".
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

        // Heat column, ported 1:1 from GUIOilburner:
        // drawTexturedModalRect(guiLeft + 116, guiTop + 69 - i, 194, 52 - i, 16, i), i = heat*52/max.
        int heat = oilburner.getHeatStored();
        int maxHeat = oilburner.getMaxHeatStored();
        int i = maxHeat > 0 ? heat * 52 / maxHeat : 0;
        if (i > 0) {
            guiGraphics.blit(texture, this.leftPos + 116, this.topPos + 69 - i, 194, 52 - i, 16, i);
        }

        // Flame overlay while switched on + fire icon when it can actually burn.
        if (oilburner.isOn()) {
            guiGraphics.blit(texture, this.leftPos + 70, this.topPos + 54, 210, 0, 35, 14);

            FT_Flammable trait = com.hbm_m.inventory.fluid.FluidType.getTrait(oilburner.getOilTank().getTankType(), FT_Flammable.class);
            if (oilburner.getOilTank().getFill() > 0 && trait != null) {
                guiGraphics.blit(texture, this.leftPos + 79, this.topPos + 34, 176, 0, 18, 18);
            }
        }

        // Oil tank fluid render, ported from diFurnace.tank.renderTank(guiLeft + 44, guiTop + 69, zLevel, 16, 52)
        // (в порте renderTank принимает верхний левый угол: y = 69 - 52 = 17).
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
            // Heat column tooltip.
            int heat = oilburner.getHeatStored();
            int maxHeat = oilburner.getMaxHeatStored();
            List<Component> heatTooltip = new ArrayList<>();
            heatTooltip.add(Component.literal(
                    String.format(Locale.US, "%,d", Math.min(heat, maxHeat)) + " / " + String.format(Locale.US, "%,d", maxHeat) + " TU"));
            drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                    this.leftPos + 116, this.topPos + 17, 16, 52,
                    mouseX, mouseY, heatTooltip.toArray(new Component[0]));

            // Burn info tooltip: "setting mB/t" / "(heatEnergy/1000)*setting TU/t".
            FT_Flammable trait = com.hbm_m.inventory.fluid.FluidType.getTrait(oilburner.getOilTank().getTankType(), FT_Flammable.class);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Toggle button region (80,54) 16x14 — порт mouseClicked из GUIOilburner.
        if (this.leftPos + 80 <= mouseX && this.leftPos + 80 + 16 > mouseX
                && this.topPos + 54 < mouseY && this.topPos + 54 + 14 >= mouseY) {
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            ToggleOilburnerC2SPacket.sendToServer(oilburner.getBlockPos());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
