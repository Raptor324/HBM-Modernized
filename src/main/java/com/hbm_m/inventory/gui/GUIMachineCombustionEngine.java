package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.menu.MachineCombustionEngineMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.MachineControlC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Порт {@code GUICombustionEngine} (1.7.10): кнопка зажигания (79,13 / 35x15, оверлей
 * из текстуры (192,0)), драг-слайдер дросселя 0..30 (трек (79,38) 36x8, ползунок из
 * (192,15) 4x8, расход = setting * 2 mB/t), индикатор комплекта поршней (80,51) из
 * (176,52+i*12), превью выработки HE/t. Позиции и регионы текстуры 1:1 с оригиналом.
 */
public class GUIMachineCombustionEngine extends AbstractContainerScreen<MachineCombustionEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_combustion.png");

    private final MachineCombustionEngineBlockEntity blockEntity;

    private int setting = 0;
    private boolean isMouseLocked = false;

    public GUIMachineCombustionEngine(MachineCombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = imageHeight - 96 + 2;
        if (blockEntity != null) {
            this.setting = blockEntity.setting;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            int x = leftPos;
            int y = topPos;

            // Комплект поршней: индикатор (80,51) 25x12 из (176, 52 + i*12).
            ItemStack piston = blockEntity.getInventory().getStackInSlot(MachineCombustionEngineBlockEntity.SLOT_PISTON);
            int pistonIdx = MachineCombustionEngineBlockEntity.pistonIndex(piston.getItem());
            if (pistonIdx >= 0) {
                guiGraphics.blit(TEXTURE, x + 80, y + 51, 176, 52 + pistonIdx * 12, 25, 12);
            }

            // Ползунок дросселя: (79 + setting*32/30, 38) 4x8 из (192,15).
            guiGraphics.blit(TEXTURE, x + 79 + (this.setting * 32 / 30), y + 38, 192, 15, 4, 8);

            // Оверлей зажигания (79,13) 35x15 из (192,0).
            if (blockEntity.isOn) {
                guiGraphics.blit(TEXTURE, x + 79, y + 13, 192, 0, 35, 15);
            }

            // Бар энергии: (143, 69-i) 16xi из (176, 52-i), i = power*53/maxPower.
            int i = (int) (blockEntity.getEnergyStored() * 53L / Math.max(blockEntity.getMaxEnergyStored(), 1L));
            if (i > 53) i = 53;
            if (i > 0) {
                guiGraphics.blit(TEXTURE, x + 143, y + 69 - i, 176, 52 - i, 16, i);
            }

            // Бак (35,69) 16x52, окраска по цвету топлива (аналог renderTank).
            var tank = blockEntity.getTank();
            if (tank.getFill() > 0) {
                int color = 0xFF000000 | FluidType.forFluid(tank.getTankType()).getColor();
                int fluidH = tank.getFill() * 52 / tank.getMaxFill();
                guiGraphics.fill(x + 35, y + 17 + (52 - fluidH), x + 51, y + 69, color);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (blockEntity == null) return;
        int x = leftPos;
        int y = topPos;

        // Тултип расхода: (setting * 2) / 10 мБ/т — у трека дросселя.
        if (isMouseLocked || isPointInArea(mouseX, mouseY, x + 80, y + 38, 34, 8)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(((setting * 2) / 10D) + "mB/t"),
                    Mth.clamp(mouseX, x + 80, x + 114), Mth.clamp(mouseY, y + 38, y + 46));
        }

        // Превью выработки HE/t при установленном комплекте поршней (79,50 35x14).
        ItemStack piston = blockEntity.getInventory().getStackInSlot(MachineCombustionEngineBlockEntity.SLOT_PISTON);
        if (MachineCombustionEngineBlockEntity.isPistonSet(piston.getItem())) {
            double power = 0;
            var tank = blockEntity.getTank();
            FT_Combustible trait = FluidType.getTrait(tank.getTankType(), FT_Combustible.class);
            if (trait != null) {
                double eff = MachineCombustionEngineBlockEntity.pistonEfficiency(piston.getItem(), trait.getGrade());
                power = setting * 0.2 * trait.getCombustionEnergy() / 1_000D * eff;
            }
            drawCustomPowerPreview(guiGraphics, mouseX, mouseY, x + 79, y + 50, 35, 14, power);
        }

        // Тултип "Ignition" над кнопкой зажигания (79,13 35x15).
        if (isPointInArea(mouseX, mouseY, x + 79, y + 13, 35, 15)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.hbm_m.engine.ignition"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (blockEntity != null) {
            // Кнопка зажигания: (89,13,16x14).
            if (mouseX >= leftPos + 89 && mouseX < leftPos + 89 + 16
                    && mouseY >= topPos + 13 && mouseY < topPos + 13 + 14) {
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        SoundEvents.UI_BUTTON_CLICK, 1.0F));
                CompoundTag data = new CompoundTag();
                data.putBoolean("turnOn", true);
                MachineControlC2SPacket.send(blockEntity.getBlockPos(), data);
            }

            // Захват дросселя: трек (79,38) 36x8.
            if (mouseX >= leftPos + 79 && mouseX < leftPos + 79 + 36
                    && mouseY >= topPos + 38 && mouseY < topPos + 38 + 8) {
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        SoundEvents.UI_BUTTON_CLICK, 1.0F));
                isMouseLocked = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isMouseLocked && blockEntity != null) {
            int newSetting = Mth.clamp((int) ((mouseX - leftPos - 81) * 30 / 32), 0, 30);

            if (this.setting != newSetting) {
                this.setting = newSetting;
                CompoundTag data = new CompoundTag();
                data.putInt("setting", setting);
                MachineControlC2SPacket.send(blockEntity.getBlockPos(), data);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseLocked) {
            isMouseLocked = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private static boolean isPointInArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Аналог drawCustomInfoStat с жёлтым HE/t-превью (оригинал жёлтым цветом). */
    private void drawCustomPowerPreview(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                        int x, int y, int width, int height, double power) {
        if (isPointInArea(mouseX, mouseY, x, y, width, height)) {
            guiGraphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal(String.format(java.util.Locale.US, "%,d", (int) power) + " HE/t")
                            .withStyle(ChatFormatting.YELLOW),
                    Component.literal(String.format(java.util.Locale.US, "%,d", (int) (power * 20)) + " HE/s")
                            .withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
    }
}
