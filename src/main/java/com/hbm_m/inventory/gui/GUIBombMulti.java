package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.BombMultiBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.BombMultiMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Экран многоцелевой бомбы — 1:1 {@code GUIBombMulti}: текстура bombGeneric
 * (в порте — байт-в-байт та же bomb_generic.png), фон 176x166 и значок
 * совместимости модификаторов (слоты 2 и 5) в правом верхнем углу (124, 34).
 */
@OnlyIn(Dist.CLIENT)
public class GUIBombMulti extends GuiInfoScreen<BombMultiMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/bomb_generic.png");

    private final BombMultiBlockEntity be;

    public GUIBombMulti(BombMultiMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        // Оригинал GUIBombMulti.drawGuiContainerBackgroundLayer: при совпадающих
        // модификаторах — иконка их типа (строки 0-5), при разных — иконка «не совпадает»
        // (строка 7). При пустых модификаторах (оба 0) иконка не рисуется.
        if (be != null) { // тайл может отсутствовать в реплее Flashback
            int type2 = be.return2type();
            int type5 = be.return5type();
            if (type2 == type5) {
                if (type2 >= 1 && type2 <= 6) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 124, this.topPos + 34, 176, (type2 - 1) * 18, 18, 18);
                }
            } else {
                guiGraphics.blit(TEXTURE, this.leftPos + 124, this.topPos + 34, 176, 7 * 18, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
