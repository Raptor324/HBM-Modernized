package com.hbm_m.client.overlay;

import com.hbm_m.block.entity.custom.machines.MachineShredderBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineShredderMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineShredder extends GuiInfoScreen<MachineShredderMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_shredder.png");

    // Progress Bar constants (как в оригинале)
    private static final int PROGRESS_X = 63; // Relative X position on the GUI for the arrow
    private static final int PROGRESS_Y = 89; // Relative Y position on the GUI for the arrow
    private static final int PROGRESS_WIDTH = 34; // Actual width of the arrow texture
    private static final int PROGRESS_HEIGHT = 18; // Actual height of the arrow texture
    private static final int PROGRESS_TEXTURE_X = 176; // Source X on texture for the arrow
    private static final int PROGRESS_TEXTURE_Y = 54; // Source Y on texture for the arrow

    // Energy Bar constants (как в оригинале)
    private static final int ENERGY_BAR_X = 8; // Relative X position on the GUI
    private static final int ENERGY_BAR_BASE_Y = 106; // Base Y position (как в оригинале: guiTop + 106)
    private static final int ENERGY_BAR_WIDTH = 16; // Width of the energy bar
    private static final int ENERGY_BAR_HEIGHT = 88; // Height of the energy bar
    private static final int ENERGY_TEXTURE_X = 176; // Source X on texture for the energy fill
    private static final int ENERGY_TEXTURE_BASE_Y = 160; // Base Y in texture (как в оригинале: 160 - i)

    // Blade rendering constants (как в оригинале)
    private static final int BLADE_LEFT_X = 43;
    private static final int BLADE_LEFT_Y = 71;
    private static final int BLADE_RIGHT_X = 79;
    private static final int BLADE_RIGHT_Y = 71;
    private static final int BLADE_WIDTH = 18;
    private static final int BLADE_HEIGHT = 18;
    private static final int BLADE_TEXTURE_X_LEFT = 176; // Source X for left blade
    private static final int BLADE_TEXTURE_X_RIGHT = 194; // Source X for right blade
    private static final int BLADE_TEXTURE_Y_GOOD = 0; // Good condition (state 1)
    private static final int BLADE_TEXTURE_Y_WORN = 18; // Worn condition (state 2)
    private static final int BLADE_TEXTURE_Y_BROKEN = 36; // Broken condition (state 3)

    // Error panel constants (как в оригинале)
    private static final int ERROR_PANEL_X = -16; // Relative to GUI left
    private static final int ERROR_PANEL_Y = 36;
    private static final int ERROR_PANEL_WIDTH = 16;
    private static final int ERROR_PANEL_HEIGHT = 16;


    public GUIMachineShredder(MachineShredderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;  // GUI width
        this.imageHeight = 233; // GUI height
        this.inventoryLabelY = this.imageHeight - 94; // Position of "Inventory" label
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Render GUI background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Render energy bar (как в оригинале)
        renderEnergyBar(guiGraphics, x, y);

        // Render progress bar (как в оригинале)
        renderProgressArrow(guiGraphics, x, y);

        // Render blades (как в оригинале)
        renderBlades(guiGraphics, x, y);

        // Render error panel if needed (как в оригинале)
        renderErrorPanel(guiGraphics);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        int progressScaled = menu.getScaledProgress(PROGRESS_WIDTH);
        if (progressScaled > 0) {
            guiGraphics.blit(TEXTURE,
                    x + PROGRESS_X, y + PROGRESS_Y,
                    PROGRESS_TEXTURE_X, PROGRESS_TEXTURE_Y,
                    progressScaled + 1, PROGRESS_HEIGHT);
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        
        if (power > 0 && maxPower > 0) {
            int energyScaled = (int) (power * ENERGY_BAR_HEIGHT / maxPower);
            if (energyScaled > 0) {
                // Как в оригинале: guiLeft + 8, guiTop + 106 - i, 176, 160 - i, 16, i
                guiGraphics.blit(TEXTURE,
                        x + ENERGY_BAR_X, y + ENERGY_BAR_BASE_Y - energyScaled,
                        ENERGY_TEXTURE_X, ENERGY_TEXTURE_BASE_Y - energyScaled,
                        ENERGY_BAR_WIDTH, energyScaled);
            }
        }
    }

    private void renderBlades(GuiGraphics guiGraphics, int x, int y) {
        MachineShredderBlockEntity blockEntity = menu.getBlockEntity();
        
        // Render left blade
        int gearLeft = blockEntity.getGearLeft();
        if (gearLeft != 0) {
            int textureY = switch (gearLeft) {
                case 1 -> BLADE_TEXTURE_Y_GOOD;
                case 2 -> BLADE_TEXTURE_Y_WORN;
                case 3 -> BLADE_TEXTURE_Y_BROKEN;
                default -> BLADE_TEXTURE_Y_GOOD;
            };
            guiGraphics.blit(TEXTURE,
                    x + BLADE_LEFT_X, y + BLADE_LEFT_Y,
                    BLADE_TEXTURE_X_LEFT, textureY,
                    BLADE_WIDTH, BLADE_HEIGHT);
        }
        
        // Render right blade
        int gearRight = blockEntity.getGearRight();
        if (gearRight != 0) {
            int textureY = switch (gearRight) {
                case 1 -> BLADE_TEXTURE_Y_GOOD;
                case 2 -> BLADE_TEXTURE_Y_WORN;
                case 3 -> BLADE_TEXTURE_Y_BROKEN;
                default -> BLADE_TEXTURE_Y_GOOD;
            };
            guiGraphics.blit(TEXTURE,
                    x + BLADE_RIGHT_X, y + BLADE_RIGHT_Y,
                    BLADE_TEXTURE_X_RIGHT, textureY,
                    BLADE_WIDTH, BLADE_HEIGHT);
        }
    }

    private void renderErrorPanel(GuiGraphics guiGraphics) {
        if (shouldShowBladeWarning()) {
            drawInfoPanel(guiGraphics, ERROR_PANEL_X, ERROR_PANEL_Y, PanelType.LARGE_RED_EXCLAMATION);
        }
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MachineShredderBlockEntity blockEntity = menu.getBlockEntity();

        drawElectricityInfo(
                guiGraphics,
                mouseX, mouseY,
                ENERGY_BAR_X,
                ENERGY_BAR_BASE_Y - ENERGY_BAR_HEIGHT,
                ENERGY_BAR_WIDTH,
                ENERGY_BAR_HEIGHT,
                menu.getEnergyLong(),
                menu.getMaxEnergyLong());

        if (shouldShowBladeWarning()) {
            drawCustomInfoStat(
                    guiGraphics,
                    mouseX, mouseY,
                    ERROR_PANEL_X, ERROR_PANEL_Y,
                    ERROR_PANEL_WIDTH, ERROR_PANEL_HEIGHT,
                    mouseX, mouseY,
                    Component.translatable("gui.hbm_m.shredder.blade_warning.title"),
                    Component.translatable("gui.hbm_m.shredder.blade_warning.desc"));
        }
    }

    private boolean shouldShowBladeWarning() {
        MachineShredderBlockEntity blockEntity = menu.getBlockEntity();
        int gearLeft = blockEntity.getGearLeft();
        int gearRight = blockEntity.getGearRight();
        return (gearLeft == 0 || gearLeft == 3) || (gearRight == 0 || gearRight == 3);
    }
}