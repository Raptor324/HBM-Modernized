package com.hbm_m.inventory.gui;

import com.hbm_m.block.machines.MachineElectricFurnaceBlock;
import com.hbm_m.inventory.menu.MachineElectricFurnaceMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code GUIMachineElectricFurnace} (1.7.10): Energiebalken (152, 18-52), Flammen bei
 * laufendem Ofen, Fortschrittspfeil (43, 36), Upgrade-Info (115, 19).
 */
public class GUIMachineElectricFurnace extends GuiInfoScreen<MachineElectricFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_electric_furnace.png");

    public GUIMachineElectricFurnace(MachineElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 52 - 34, 16, 34,
                menu.getEnergyLong(), menu.getMaxEnergyLong());

        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 115, 19, 8, 8, mouseX, mouseY,
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"));

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(font, name, 70 - font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        long max = menu.getMaxEnergyLong();
        if (menu.getEnergyLong() > 0 && max > 0) {
            int p = (int) (menu.getEnergyLong() * 34 / max);
            guiGraphics.blit(TEXTURE, leftPos + 152, topPos + 52 - p, 176, 64 - p, 16, p);
        }

        // Original: Flammen, solange der Block "machine_electric_furnace_on" ist (hier: LIT)
        BlockState state = menu.blockEntity.getBlockState();
        if (state.hasProperty(MachineElectricFurnaceBlock.LIT) && state.getValue(MachineElectricFurnaceBlock.LIT)) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 20, 192, 12, 18, 16);
            guiGraphics.blit(TEXTURE, leftPos + 46, topPos + 47, 192, 28, 18, 16);
        }

        int p = menu.getCookProgressScaled(28);
        guiGraphics.blit(TEXTURE, leftPos + 43, topPos + 36, 176, 0, p, 12);

        this.drawInfoPanel(guiGraphics, 115, 19, PanelType.SMALL_BLUE_STAR);
    }
}
