package com.hbm_m.client.overlay.crates;

import com.hbm_m.menu.IronCrateMenu;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * GUI экран для Iron Crate (36 слотов: 4 ряда × 9 колонок)
 */
public class GUIIronCrate extends AbstractContainerScreen<IronCrateMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    private boolean soundPlayed = false;

    public GUIIronCrate(IronCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;

        // Проигрываем звук открытия при инициализации экрана
        if (!soundPlayed) {
            playOpenSound();
            soundPlayed = true;
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Рисуем верхнюю часть (заголовок + 3 ряда)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, 71);
        // Рисуем 4-й ряд крейта
        guiGraphics.blit(TEXTURE, x, y + 71, 0, 53, imageWidth, 18);
        // Рисуем инвентарь игрока (3 ряда + хотбар)
        guiGraphics.blit(TEXTURE, x, y + 89, 0, 125, imageWidth, 97);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // ✅ ИСПРАВЛЕНО: Правильная сигнатура для Minecraft 1.20.1 Forge
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Проверяем клавишу инвентаря (E) или Escape
        if (keyCode == 256 || keyCode == 291) { // 256 = ESC, 291 = E (Inventory)
            playCloseSound();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Проигрывает звук открытия ящика
     */
    private void playOpenSound() {
        LocalPlayer player = this.minecraft.player;
        if (player != null && ModSounds.CRATE_OPEN.isPresent()) {
            SoundEvent soundEvent = ModSounds.CRATE_OPEN.get();
            this.minecraft.level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    soundEvent, player.getSoundSource(), 0.6F, 1.0F);
        }
    }

    /**
     * Проигрывает звук закрытия ящика
     */
    private void playCloseSound() {
        LocalPlayer player = this.minecraft.player;
        if (player != null && ModSounds.CRATE_CLOSE.isPresent()) {
            SoundEvent soundEvent = ModSounds.CRATE_CLOSE.get();
            this.minecraft.level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    soundEvent, player.getSoundSource(), 0.6F, 1.0F);
        }
    }
}
