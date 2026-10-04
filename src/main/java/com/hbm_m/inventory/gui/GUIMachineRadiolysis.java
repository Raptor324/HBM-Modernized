package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity;
import com.hbm_m.inventory.menu.MachineRadiolysisMenu;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIRadiolysis}: 230x166, Energiesaeule links, schmaler Eingangstank, zwei kleine Ausgangstanks und links
 * drei Info-Felder (Beschreibung, aktuelle Hitze, Liste der Pellets mit Leistung).
 */
public class GUIMachineRadiolysis extends GuiInfoScreen<MachineRadiolysisMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_radiolysis.png");

    private final MachineRadiolysisBlockEntity radiolysis;

    public GUIMachineRadiolysis(MachineRadiolysisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.radiolysis = menu.getBlockEntity();
        this.imageWidth = 230;
        this.imageHeight = 166;
    }

    /** Original {@code I18nUtil.resolveKeyArray}: Zeilen am {@code $} trennen. */
    private static Component[] lines(String key, Object... args) {
        String[] split = Component.translatable(key, args).getString().split("\\$");
        Component[] out = new Component[split.length];
        for (int i = 0; i < split.length; i++) out[i] = Component.literal(split[i]);
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (radiolysis != null) {
            var tanks = radiolysis.getTanks();
            tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 61, topPos + 17, 8, 52);
            tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 87, topPos + 17, 12, 16);
            tanks[2].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 87, topPos + 53, 12, 16);

            drawElectricityInfo(g, mouseX, mouseY, 8, 17, 16, 34, radiolysis.getEnergyStored(), radiolysis.getMaxEnergyStored());

            drawCustomInfoStat(g, mouseX, mouseY, -16, 16, 16, 16, leftPos - 8, topPos + 16 + 16, lines("desc.gui.radiolysis.desc"));
            drawCustomInfoStat(g, mouseX, mouseY, -16, 16 + 18, 16, 16, leftPos - 8, topPos + 16 + 18 + 16, lines("desc.gui.rtg.heat", radiolysis.getHeat()));

            List<Component> pelletText = new ArrayList<>();
            pelletText.add(Component.translatable("desc.gui.rtg.pellets"));
            for (ItemRTGPellet pellet : ItemRTGPellet.PELLETS) {
                pelletText.add(Component.translatable("desc.gui.rtg.pelletPower", Component.translatable(pellet.getDescriptionId()), pellet.getHeat() * 10));
            }
            drawCustomInfoStat(g, mouseX, mouseY, -16, 16 + 36, 16, 16, leftPos - 8, topPos + 16 + 36 + 16, pelletText.toArray(new Component[0]));
        }

        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 88 - font.width(title) / 2, 6, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (radiolysis == null) return; // тайл может отсутствовать в реплее Flashback

        int i = (int) (radiolysis.getEnergyStored() * 34L / Math.max(1L, radiolysis.getMaxEnergyStored()));
        g.blit(TEXTURE, leftPos + 8, topPos + 51 - i, 240, 34 - i, 16, i, 256, 256);

        var tanks = radiolysis.getTanks();
        tanks[0].renderTank(g, leftPos + 61, topPos + 17, 8, 52);
        for (int j = 0; j < 2; j++) {
            tanks[j + 1].renderTank(g, leftPos + 87, topPos + 17 + j * 36, 12, 16);
        }

        drawInfoPanel(g, -16, 16, PanelType.LARGE_BLUE_STAR);
        drawInfoPanel(g, -16, 16 + 18, PanelType.LARGE_BLUE_INFO);
        drawInfoPanel(g, -16, 16 + 36, PanelType.LARGE_GREEN_INFO);
    }
}
