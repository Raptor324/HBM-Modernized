package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.inventory.menu.MachineCoreEmitterMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.CoreEmitterControlC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUICoreEmitter} (1.7.10), 176x166.
 *
 * <p>Links der Cryogeltank bei (8, 17), daneben der Energiebalken bei (26, 17). In der Mitte das
 * Eingabefeld bei (57, 57) mit dem Fuellbalken darueber bei (53, 45), rechts daneben bei (97, 52)
 * der Knopf, der die Zahl uebernimmt, und bei (133, 52) der An/Aus-Schalter.</p>
 */
public class GUIMachineCoreEmitter extends GuiInfoScreen<MachineCoreEmitterMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/dfc/gui_emitter.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/dfc/gui_emitter.png");
            //?}

    private static final int FIELD_X = 57;
    private static final int FIELD_Y = 57;
    private static final int FIELD_W = 29;
    private static final int FIELD_H = 12;

    private static final int APPLY_X = 97;
    private static final int APPLY_Y = 52;
    private static final int TOGGLE_X = 133;
    private static final int TOGGLE_Y = 52;

    private final MachineCoreEmitterBlockEntity emitter;
    private EditBox wattsField;

    public GUIMachineCoreEmitter(MachineCoreEmitterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.emitter = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        wattsField = new EditBox(this.font, leftPos + FIELD_X, topPos + FIELD_Y, FIELD_W, FIELD_H,
                Component.translatable("gui.hbm_m.dfc.watts"));
        wattsField.setTextColor(0xFFFFFF);
        wattsField.setBordered(false);
        wattsField.setMaxLength(3);
        wattsField.setValue(String.valueOf(emitter != null ? emitter.getWatts() : 1));
        wattsField.setFilter(s -> s.isEmpty() || s.matches("\\d{1,3}"));

        addRenderableWidget(wattsField);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (emitter == null) return; // тайл может отсутствовать в реплее Flashback

        // Original: der Rahmen erscheint nur, solange das Feld den Zeiger hat.
        if (wattsField != null && wattsField.isFocused()) {
            guiGraphics.blit(TEXTURE, leftPos + 53, topPos + 53, 210, 4, 34, 16);
        }

        if (emitter.isOn()) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + TOGGLE_Y, 192, 0, 18, 18);
        }

        // Der Fuellbalken ueber dem Feld zeigt die eingestellte Wattzahl.
        guiGraphics.blit(TEXTURE, leftPos + 53, topPos + 45, 210, 0, emitter.getWatts() * 34 / 100, 4);

        int energy = (int) (emitter.getEnergyStored() * 52L / Math.max(emitter.getMaxEnergyStored(), 1L));
        if (energy > 52) energy = 52;
        if (energy > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 69 - energy, 176, 52 - energy, 16, energy);
        }

        emitter.getCoolantTank().renderTank(guiGraphics, this.leftPos + 8, this.topPos + 17, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);

        if (emitter != null) {
            guiGraphics.drawString(this.font, "Output: " + emitter.getOutput() + "Spk", 50, 30, 0xFF7F7F, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (emitter != null) { // тайл может отсутствовать в реплее Flashback
            emitter.getCoolantTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 8, this.topPos + 17, 16, 52);

            drawElectricityInfo(guiGraphics, mouseX, mouseY,
                    26, 17, 16, 52,
                    emitter.getEnergyStored(), emitter.getMaxEnergyStored());
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (emitter == null) return super.mouseClicked(mouseX, mouseY, button);

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Original: der Knopf uebernimmt die eingetippte Zahl, auf 1 bis 100 begrenzt.
        if (isOver(APPLY_X, APPLY_Y, mx, my)) {
            try {
                int watts = Math.max(1, Math.min(100, Integer.parseInt(wattsField.getValue())));
                wattsField.setValue(String.valueOf(watts));
                playClick();
                CoreEmitterControlC2SPacket.sendWatts(emitter.getBlockPos(), watts);
            } catch (NumberFormatException ignored) {
                // Original: bei ungueltiger Eingabe passiert schlicht nichts.
            }
            return true;
        }

        if (isOver(TOGGLE_X, TOGGLE_Y, mx, my)) {
            playClick();
            CoreEmitterControlC2SPacket.sendToggle(emitter.getBlockPos());
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

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean isOver(int x, int y, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= x && localX < x + 18 && localY >= y && localY < y + 18;
    }
}
