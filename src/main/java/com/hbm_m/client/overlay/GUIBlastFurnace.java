package com.hbm_m.client.overlay;

// GUI для плавильной печи. Показывает прогресс плавки, уровень топлива и индикатор работы печи.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.BlastFurnaceMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class GUIBlastFurnace extends AbstractContainerScreen<BlastFurnaceMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/GUIDiFurnace.png");
    public GUIBlastFurnace(BlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 0;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
        int progressWidth = menu.getScaledProgress() + 1;
        guiGraphics.blit(GUI_TEXTURE, x + 101, y + 35, 176, 14, progressWidth, 17);
    }

    private void renderFuelBar(GuiGraphics guiGraphics, int x, int y) {
        if(menu.hasFuel()) {
            int fuelHeight = menu.getScaledFuelProgress();
            if (fuelHeight > 0) {
                guiGraphics.blit(GUI_TEXTURE, x + 44, y + 70 - fuelHeight,
                        201, 53 - fuelHeight, 16, fuelHeight);
            }
        }
    }

    private void renderLight(GuiGraphics guiGraphics, int x, int y) {
        if(menu.hasFuel() && (menu.isCrafting() || menu.getScaledProgress() > 0)) {
            guiGraphics.blit(GUI_TEXTURE, x + 63, y + 37, 176, 0, 14, 14);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderSlotDirectionTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    private void renderSlotDirectionTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }

        // slot order: 0 -> upper input, 1 -> lower input, 2 -> fuel
        int teSlotOffset = this.menu.getMachineSlotOffset();
        Slot fuelSlot = this.menu.slots.get(teSlotOffset);
        Slot upperSlot = this.menu.slots.get(teSlotOffset + 1);
        Slot lowerSlot = this.menu.slots.get(teSlotOffset + 2);

        Slot[] slots = new Slot[] { upperSlot, lowerSlot, fuelSlot };

        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];
            if (slot != null && !slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                Component direction = Component.translatable("direction.hbm_m." + menu.getConfiguredDirectionForSlot(i).getName());
                Component tooltip = Component.translatable("gui.hbm_m.blast_furnace.accepts", direction)
                        .withStyle(ChatFormatting.YELLOW);
                guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }
}