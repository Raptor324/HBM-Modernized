package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.NukeFstbmbMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран бомбы бейлфайра: слоты, таймер и кнопка запуска отсчёта.
 */
public class GUINukeFstbmb extends GuiInfoScreen<NukeFstbmbMenu> {

    private static final ResourceLocation TEXTURE =
            //? if < 1.21.1 {
            new ResourceLocation("hbm_m", "textures/gui/weapon/fstbmb_schematic.png");
            //?} else {
            /*ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/gui/weapon/fstbmb_schematic.png");
             *///?}

    private final NukeFstbmbBlockEntity be;
    private Button startButton;

    public GUINukeFstbmb(NukeFstbmbMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.startButton = Button.builder(Component.translatable("gui.hbm_m.nuke_fstbmb.start"), b -> {
                    if (!be.started && be.isReady()) {
                        be.startCountdown();
                    }
                })
                .bounds(this.leftPos + 8, this.topPos + 60, 60, 18)
                .build();
        addRenderableWidget(startButton);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (be != null && be.started) { // тайл может отсутствовать в реплее Flashback
            int mins = be.timer / 1200;
            int secs = (be.timer / 20) % 60;
            String time = mins + ":" + (secs < 10 ? "0" : "") + secs;
            guiGraphics.drawString(this.font,
                    Component.translatable("gui.hbm_m.nuke_fstbmb.timer", time).withStyle(net.minecraft.ChatFormatting.RED),
                    this.leftPos + 74, this.topPos + 64, 4210752, false);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        // тайл может отсутствовать в реплее Flashback
        if (be != null && be.isReady()) {
            guiGraphics.blit(TEXTURE, this.leftPos + imageWidth - 42, this.topPos + 6, 176, 48, 16, 16);
        }

        this.drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_BLUE_INFO);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
