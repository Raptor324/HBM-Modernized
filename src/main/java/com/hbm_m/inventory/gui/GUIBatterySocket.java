package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.inventory.menu.BatterySocketMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateBatteryC2SPacket;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class GUIBatterySocket extends AbstractContainerScreen<BatterySocketMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            new ResourceLocation(RefStrings.MODID, "textures/gui/storage/gui_battery_socket.png");
            //?} else {
                        /*ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_battery_socket.png");
            *///?}


    public GUIBatterySocket(BatterySocketMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 181;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -9999;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        renderEnergyBar(graphics, x, y);
        renderButtons(graphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String titleText = title.getString();
        graphics.drawString(font, titleText, (imageWidth - font.width(titleText)) / 2, 6, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    private void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        long energy = menu.getEnergy();
        long maxEnergy = menu.getMaxEnergy();
        if (maxEnergy <= 0) return;
        if (energy > Long.MAX_VALUE / 100) {
            energy /= 100;
            maxEnergy /= 100;
        }
        maxEnergy = Math.max(1, maxEnergy);
        int totalHeight = 52;
        int barHeight = (int) (totalHeight * ((double) energy / (double) maxEnergy));
        barHeight = Math.min(totalHeight, barHeight);
        graphics.blit(TEXTURE, x + 62, y + 69 - barHeight, 176, 52 - barHeight, 34, barHeight);
    }

    /**
     * В png-атласе кадры режимов для 0/1 идут в другом порядке:
     * 0 (both) и 1 (input) визуально были перепутаны местами.
     * Ремапаем только UV, не меняя смысл режимов.
     */
    private int getVForMode(int mode) {
        return switch (mode) {
            case 0 -> 70;  // both
            case 1 -> 52;  // input
            case 2 -> 88;  // output
            case 3 -> 106; // locked
            default -> 70;
        };
    }

    private void renderButtons(GuiGraphics graphics, int x, int y) {
        int low = menu.getModeOnNoSignal();
        graphics.blit(TEXTURE, x + 106, y + 16, 176, getVForMode(low), 18, 18);
        int high = menu.getModeOnSignal();
        graphics.blit(TEXTURE, x + 106, y + 52, 176, getVForMode(high), 18, 18);
        int p = menu.getPriorityOrdinal();
        graphics.blit(TEXTURE, x + 125, y + 35, 194, 52 + p * 16 - 16, 16, 16);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (isMouseOver(mouseX, mouseY, 62, 69 - 52, 34, 52)) {
            List<Component> tooltip = new ArrayList<>();
            long energy = menu.getEnergy();
            long maxEnergy = menu.getMaxEnergy();
            long delta = menu.getEnergyDelta();
            tooltip.add(Component.literal(EnergyFormatter.format(energy) + " / " + EnergyFormatter.format(maxEnergy) + " HE"));
            String deltaText = (delta >= 0 ? "+" : "") + EnergyFormatter.formatRate(delta);
            ChatFormatting deltaColor = delta > 0 ? ChatFormatting.GREEN : (delta < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW);
            tooltip.add(Component.literal(deltaText).withStyle(deltaColor));
            long deltaPerSecond = delta * 20;
            tooltip.add(Component.literal((deltaPerSecond >= 0 ? "+" : "") + EnergyFormatter.formatWithUnit(deltaPerSecond, "HE/s")).withStyle(deltaColor));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
        if (isMouseOver(mouseX, mouseY, 125, 35, 16, 16)) {
            int ord = menu.getPriorityOrdinal();
            String key = "gui.hbm_m.battery.priority." + ord;
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(key));
            tooltip.add(Component.translatable("gui.hbm_m.battery.priority.recommended").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(key + ".desc").withStyle(ChatFormatting.GRAY));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
        if (isMouseOver(mouseX, mouseY, 106, 16, 18, 18)) {
            renderRedstoneTooltip(graphics, mouseX, mouseY, menu.getModeOnNoSignal(), "no_signal");
        }
        if (isMouseOver(mouseX, mouseY, 106, 52, 18, 18)) {
            renderRedstoneTooltip(graphics, mouseX, mouseY, menu.getModeOnSignal(), "with_signal");
        }
    }

    private void renderRedstoneTooltip(GuiGraphics graphics, int mouseX, int mouseY, int mode, String conditionKey) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.hbm_m.battery.condition." + conditionKey));
        String modeKey = switch (mode) {
            case 0 -> "both";
            case 1 -> "input";
            case 2 -> "output";
            case 3 -> "locked";
            default -> "both";
        };
        tooltip.add(Component.translatable("gui.hbm_m.battery.mode." + modeKey).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gui.hbm_m.battery.mode." + modeKey + ".desc").withStyle(ChatFormatting.GRAY));
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMouseOver(mouseX, mouseY, 106, 16, 18, 18)) {
                playClick();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_BATTERY,
                    new UpdateBatteryC2SPacket(menu.blockEntity.getBlockPos(), 0));
                return true;
            }
            if (isMouseOver(mouseX, mouseY, 106, 52, 18, 18)) {
                playClick();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_BATTERY,
                    new UpdateBatteryC2SPacket(menu.blockEntity.getBlockPos(), 1));
                return true;
            }
            if (isMouseOver(mouseX, mouseY, 125, 35, 16, 16)) {
                playClick();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_BATTERY,
                    new UpdateBatteryC2SPacket(menu.blockEntity.getBlockPos(), 2));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX <= leftPos + x + w && mouseY >= topPos + y && mouseY <= topPos + y + h;
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
