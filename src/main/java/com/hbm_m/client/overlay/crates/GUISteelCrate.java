package com.hbm_m.client.overlay.crates;

import com.hbm_m.menu.SteelCrateMenu;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * GUI экран для Steel Crate (54 слота) с серым оттенком
 * Добавлены звуки открытия и закрытия
 */
public class GUISteelCrate extends AbstractContainerScreen<SteelCrateMenu> {

    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    private boolean soundPlayed = false;
    private final Random random = new Random();

    public GUISteelCrate(SteelCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;

    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(CHEST_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        guiGraphics.setColor(0.6f, 0.6f, 0.6f, 1.0f);  // Серый оттенок поверх
        guiGraphics.blit(CHEST_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 291) { // ESC или E
            playCloseSound();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    private void playCloseSound() {
        LocalPlayer player = this.minecraft.player;
        if (player != null && ModSounds.CRATE_CLOSE.isPresent()) {
            float pitch = 1.0f + (random.nextFloat() * 0.2f - 0.1f);
            this.minecraft.level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    ModSounds.CRATE_CLOSE.get(), player.getSoundSource(), 0.6F, pitch);
        }
    }
}
