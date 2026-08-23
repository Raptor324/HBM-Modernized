package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineChemicalFactoryMenu;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * GUI для Chemical Factory — 4 параллельные линии Chemical Plant в одном мультиблоке.
 * Использует существующую заглушечную текстуру gui_chemical_factory.png (256x256);
 * рецепты подбираются автоматически на каждой линии (см. {@link com.hbm_m.module.machine.MachineModuleChemFactoryLane}),
 * поэтому GUI не содержит blueprint-селектора рецептов, в отличие от Chemical Plant.
 */
public class GUIMachineChemicalFactory extends AbstractContainerScreen<MachineChemicalFactoryMenu> {

    private static final int LANE_COUNT = 4;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_chemical_factory.png");

    public GUIMachineChemicalFactory(MachineChemicalFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        long energyStored = menu.getEnergyStored();
        long maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0) {
            int p = (int) (energyStored * 60L / maxEnergy);
            if (p > 60) p = 60;
            guiGraphics.fill(this.leftPos + 224, this.topPos + 78 - p, this.leftPos + 240, this.topPos + 78, 0xFFB00000);
        }

        for (int lane = 0; lane < LANE_COUNT; lane++) {
            int rowY = this.topPos + 10 + lane * 40;
            int progress = menu.getLaneProgress(lane);
            int maxProgress = menu.getLaneMaxProgress(lane);
            if (progress > 0 && maxProgress > 0) {
                int w = (int) Math.ceil(50.0 * progress / maxProgress);
                guiGraphics.fill(this.leftPos + 64, rowY + 6, this.leftPos + 64 + w, rowY + 12, 0xFF3CB043);
            }

            if (this.minecraft != null && this.minecraft.level != null) {
                ChemicalPlantRecipe recipe = getLaneRecipe(lane);
                if (recipe != null) {
                    ItemStack icon = recipe.getResultItem(this.minecraft.level.registryAccess());
                    if (!icon.isEmpty()) {
                        guiGraphics.renderItem(icon, this.leftPos + 156, rowY);
                    }
                }
            }
        }

        if (menu.getBlockEntity() != null) {
            for (int i = 0; i < 3; i++) {
                for (int lane = 0; lane < LANE_COUNT; lane++) {
                    int rowY = this.topPos + 10 + lane * 40;
                    menu.getBlockEntity().getInputTanks()[lane * 3 + i].renderTank(guiGraphics, this.leftPos + 62 + i * 5, rowY, 4, 34);
                    menu.getBlockEntity().getOutputTanks()[lane * 3 + i].renderTank(guiGraphics, this.leftPos + 176 + i * 5, rowY, 4, 34);
                }
            }

            menu.getBlockEntity().getWaterTank().renderTank(guiGraphics, this.leftPos + 244, this.topPos + 10, 6, 60);
            menu.getBlockEntity().getSpentSteamTank().renderTank(guiGraphics, this.leftPos + 250, this.topPos + 10, 6, 60);
        }

        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TEXTURE);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 106 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isMouseOver(mouseX, mouseY, 224, 18, 16, 60)) {
            long energy = menu.getEnergyStored();
            long maxEnergy = menu.getMaxEnergyStored();
            guiGraphics.renderTooltip(this.font,
                    Component.literal(EnergyFormatter.format(energy) + " / " + EnergyFormatter.format(maxEnergy) + " HE")
                            .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
        }

        if (menu.getBlockEntity() != null) {
            for (int i = 0; i < 3; i++) {
                for (int lane = 0; lane < LANE_COUNT; lane++) {
                    int rowY = this.topPos + 10 + lane * 40;
                    menu.getBlockEntity().getInputTanks()[lane * 3 + i].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 62 + i * 5, rowY, 4, 34);
                    menu.getBlockEntity().getOutputTanks()[lane * 3 + i].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 176 + i * 5, rowY, 4, 34);
                }
            }
            menu.getBlockEntity().getWaterTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 244, this.topPos + 10, 6, 60);
            menu.getBlockEntity().getSpentSteamTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 250, this.topPos + 10, 6, 60);
        }
    }

    @Nullable
    private ChemicalPlantRecipe getLaneRecipe(int lane) {
        if (menu.getBlockEntity() == null) return null; // тайл может отсутствовать в реплее Flashback
        var lanes = menu.getBlockEntity().getLanes();
        if (lane < 0 || lane >= lanes.length) return null;
        return lanes[lane].peekRecipe();
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}
