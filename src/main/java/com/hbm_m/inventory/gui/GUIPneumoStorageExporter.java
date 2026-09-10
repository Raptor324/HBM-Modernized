package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;
import com.hbm_m.inventory.menu.PneumoStorageExporterMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PneumoStorageControlC2SPacket;
import com.hbm_m.network.PneumoStorageControlC2SPacket.Control;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 1:1-Port von {@code GUIPneumoStorageExporter} (1.7.10), 176x185.
 *
 * <p>Links die neun Anforderungsvorlagen, rechts die Ausgabe. Am rechten Rand drei Schalter:
 * (142, 16) fortlaufend oder auf Redstoneflanke, (142, 34) die Betriebsart und (142, 52) die
 * Herkunft der Filter. Steht die auf Funk, legt sich eine Blende ueber die Vorlagenplaetze - sie
 * werden dann nicht mehr gelesen.</p>
 */
public class GUIPneumoStorageExporter extends AbstractContainerScreen<PneumoStorageExporterMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/storage/gui_pneumatic_exporter.png");

    private static final int BTN_CONTINUOUS_X = 142;
    private static final int BTN_CONTINUOUS_Y = 16;
    private static final int BTN_MODE_X = 142;
    private static final int BTN_MODE_Y = 34;
    private static final int BTN_ROR_X = 142;
    private static final int BTN_ROR_Y = 52;

    public GUIPneumoStorageExporter(PneumoStorageExporterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 185;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Original: der Knopf zeigt die Zusatzgrafik nur im Redstonebetrieb.
        if (!menu.isContinuous()) {
            guiGraphics.blit(TEXTURE, leftPos + BTN_CONTINUOUS_X, topPos + BTN_CONTINUOUS_Y,
                    imageWidth, 0, 18, 18);
        }
        // Original: nur die beiden strengen Betriebsarten bekommen eine Grafik.
        if (menu.getRequestMode() == PneumoStorageExporterBlockEntity.MODE_FULL_STACK) {
            guiGraphics.blit(TEXTURE, leftPos + BTN_MODE_X, topPos + BTN_MODE_Y,
                    imageWidth + 18, 0, 18, 18);
        }
        if (menu.getRequestMode() == PneumoStorageExporterBlockEntity.MODE_FULL_REQUEST) {
            guiGraphics.blit(TEXTURE, leftPos + BTN_MODE_X, topPos + BTN_MODE_Y,
                    imageWidth + 18, 18, 18, 18);
        }

        if (menu.isRorMode()) {
            guiGraphics.blit(TEXTURE, leftPos + BTN_ROR_X, topPos + BTN_ROR_Y, imageWidth, 18, 18, 18);
            // Die Blende ueber den neun Vorlagenplaetzen.
            guiGraphics.blit(TEXTURE, leftPos + 14, topPos + 14, 77, 14, 58, 58);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isOver(BTN_CONTINUOUS_X, BTN_CONTINUOUS_Y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable(
                    menu.isContinuous() ? "gui.hbm_m.pneumo.export.continuous"
                                        : "gui.hbm_m.pneumo.export.redstone")), mouseX, mouseY);
        }

        if (isOver(BTN_MODE_X, BTN_MODE_Y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable(switch (menu.getRequestMode()) {
                case PneumoStorageExporterBlockEntity.MODE_FULL_STACK -> "gui.hbm_m.pneumo.export.mode.stack";
                case PneumoStorageExporterBlockEntity.MODE_FULL_REQUEST -> "gui.hbm_m.pneumo.export.mode.request";
                default -> "gui.hbm_m.pneumo.export.mode.amap";
            })), mouseX, mouseY);
        }

        if (isOver(BTN_ROR_X, BTN_ROR_Y, mouseX, mouseY)) {
            java.util.List<Component> lines = new java.util.ArrayList<>();

            if (!menu.isRorMode()) {
                lines.add(Component.translatable("gui.hbm_m.pneumo.export.filter.manual"));
            } else {
                lines.add(Component.translatable("gui.hbm_m.pneumo.export.filter.ror"));

                // Original: je Platz "Item #id with Meta m xN" oder "None".
                for (int i = 0; i < PneumoStorageExporterBlockEntity.REQUEST_SLOTS; i++) {
                    int id = menu.getRorFilter(i, 0);
                    int meta = menu.getRorFilter(i, 1);
                    int amount = menu.getRorFilter(i, 2);

                    lines.add(id != 0 && amount > 0
                            ? Component.translatable("gui.hbm_m.pneumo.export.filter.slot",
                                    i + 1, describe(id), meta, amount)
                            : Component.translatable("gui.hbm_m.pneumo.export.filter.slot.none", i + 1));
                }
            }

            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /** Der Registriername zur Gegenstandsnummer - lesbarer als die Zahl des Originals. */
    private static String describe(int id) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(id);
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (isOver(BTN_CONTINUOUS_X, BTN_CONTINUOUS_Y, mx, my)) {
            PneumoStorageControlC2SPacket.send(menu.getBlockEntity().getBlockPos(), Control.EXPORT_CONTINUOUS);
            return true;
        }
        if (isOver(BTN_MODE_X, BTN_MODE_Y, mx, my)) {
            PneumoStorageControlC2SPacket.send(menu.getBlockEntity().getBlockPos(), Control.EXPORT_MODE);
            return true;
        }
        if (isOver(BTN_ROR_X, BTN_ROR_Y, mx, my)) {
            PneumoStorageControlC2SPacket.send(menu.getBlockEntity().getBlockPos(), Control.EXPORT_ROR);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOver(int x, int y, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + 18 && localY >= y && localY < y + 18;
    }
}
