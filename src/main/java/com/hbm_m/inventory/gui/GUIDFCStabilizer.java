package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.dfc.DFCStabilizerBlockEntity;
import com.hbm_m.inventory.menu.DFCStabilizerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.DFCStabilizerWattsC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 1:1-Port von {@code GUICoreStabilizer} (1.7.10), 176x166.
 *
 * <p>Links der Energiebalken bei (35, 17), oben der Linsenplatz. In der Mitte das Eingabefeld bei
 * (75, 57), rechts daneben bei (124, 52) der Knopf, der die eingetippte Wattzahl uebernimmt.</p>
 *
 * <p>Die Wattzahl kostet in der <b>vierten Potenz</b>: eins kostet eine Einheit je Tick, hundert
 * kosten hundert Millionen. Der Hinweis am Knopf nennt darum immer den tatsaechlichen Verbrauch.</p>
 */
public class GUIDFCStabilizer extends AbstractContainerScreen<DFCStabilizerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/dfc/gui_stabilizer.png");

    private static final int FIELD_X = 75;
    private static final int FIELD_Y = 57;
    private static final int FIELD_W = 29;
    private static final int FIELD_H = 12;

    private static final int BUTTON_X = 124;
    private static final int BUTTON_Y = 52;

    private static final int POWER_X = 35;
    private static final int POWER_Y = 17;
    private static final int POWER_W = 16;
    private static final int POWER_H = 52;

    private EditBox wattsField;

    public GUIDFCStabilizer(DFCStabilizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();

        wattsField = new EditBox(this.font, leftPos + FIELD_X, topPos + FIELD_Y, FIELD_W, FIELD_H,
                Component.translatable("gui.hbm_m.dfc.watts"));
        wattsField.setTextColor(0xFFFFFF);
        wattsField.setBordered(false);
        wattsField.setMaxLength(3);
        wattsField.setValue(String.valueOf(menu.getWatts()));
        wattsField.setFilter(s -> s.isEmpty() || s.matches("\\d{1,3}"));

        addRenderableWidget(wattsField);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Energiebalken, von unten gefuellt.
        int fill = menu.getPowerPermille() * POWER_H / 1000;
        if (fill > 0) {
            guiGraphics.fill(leftPos + POWER_X, topPos + POWER_Y + POWER_H - fill,
                    leftPos + POWER_X + POWER_W, topPos + POWER_Y + POWER_H, 0xFFE8C33F);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isOver(POWER_X, POWER_W, POWER_Y, POWER_H, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.literal(menu.getBlockEntity().getEnergyStored() + " / "
                            + DFCStabilizerBlockEntity.MAX_POWER + " HE")), mouseX, mouseY);
        }

        if (isOver(BUTTON_X, 18, BUTTON_Y, 18, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.dfc.watts.apply"),
                    Component.translatable("gui.hbm_m.dfc.watts.current", menu.getWatts()),
                    Component.translatable("gui.hbm_m.dfc.watts.draw",
                            menu.getBlockEntity().getDemand())), mouseX, mouseY);
        }
    }

    /** Original: der Knopf uebernimmt die eingetippte Zahl, auf 1 bis 100 begrenzt. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOver(BUTTON_X, 18, BUTTON_Y, 18, (int) mouseX, (int) mouseY)) {
            try {
                int watts = Math.max(1, Math.min(100, Integer.parseInt(wattsField.getValue())));
                wattsField.setValue(String.valueOf(watts));

                if (minecraft != null) {
                    minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                DFCStabilizerWattsC2SPacket.send(menu.getBlockEntity().getBlockPos(), watts);
            } catch (NumberFormatException ignored) {
                // Original: bei ungueltiger Eingabe passiert schlicht nichts.
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Solange das Eingabefeld den Zeiger hat, darf "e" die Oberflaeche nicht schliessen.
        if (wattsField != null && wattsField.isFocused() && keyCode != 256) {
            return wattsField.keyPressed(keyCode, scanCode, modifiers) || wattsField.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isOver(int x, int w, int y, int h, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
