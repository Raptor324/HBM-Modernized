package com.hbm_m.client.overlay;

// GUI для плавильной печи. Показывает прогресс плавки, уровень топлива и индикатор работы печи.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.BlastFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIBlastFurnace extends AbstractContainerScreen<BlastFurnaceMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/blast_furnace/blast_furnace_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/blast_furnace/arrow.png");
    private static final ResourceLocation FUEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/blast_furnace/fuel_progress.png");
    private static final ResourceLocation LIGHT_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/blast_furnace/light_on.png");
    private static final ResourceLocation LIGHT_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/blast_furnace/light_off.png");

    public GUIBlastFurnace(BlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 6;
        this.titleLabelY = 12;
        this.inventoryLabelX = 6;
        this.inventoryLabelY = 78;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Рисуем основную текстуру GUI
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
        renderFuelBar(guiGraphics, x, y);
        renderLight(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isCrafting()) {
            // Стрелка прогресса между входными слотами и выходным
            guiGraphics.blit(ARROW_TEXTURE, x + 101, y + 41, 0, 0,
                    menu.getScaledProgress(), 16, 24, 16);
        }
    }

    private void renderFuelBar(GuiGraphics guiGraphics, int x, int y) {
        if(menu.hasFuel()) {
            int fuelHeight = menu.getScaledFuelProgress();
            // Индикатор топлива возле топливного слота
            guiGraphics.blit(FUEL_TEXTURE, x + 43, y + 25 - fuelHeight + 51,
                    0, 52 - fuelHeight, 18, fuelHeight, 18, 51);
        }
    }

    private void renderLight(GuiGraphics guiGraphics, int x, int y) {
        // Индикатор работы печи
        if(menu.hasFuel()) {
            guiGraphics.blit(LIGHT_ON_TEXTURE, x + 63, y + 45, 0, 0, 14, 13, 14, 13);
        } else {
            guiGraphics.blit(LIGHT_OFF_TEXTURE, x + 63, y + 45, 0, 0, 14, 13, 14, 13);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}