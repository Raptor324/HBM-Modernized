package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.fusion.FusionKlystronBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.inventory.menu.MachineFusionKlystronMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.SetKlystronOutputC2SPacket;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIFusionKlystron} (1.7.10). Das Textfeld schickt bei jeder Aenderung den
 * neuen Zielwert an den Server - genau wie das {@code NBTControlPacket} des Originals.
 */
public class GUIMachineFusionKlystron extends GuiInfoScreen<MachineFusionKlystronMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_klystron.png");
    *///?} else {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_klystron.png");
    //?}

    private EditBox field;

    public GUIMachineFusionKlystron(MachineFusionKlystronMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 200;
    }

    private FusionKlystronBlockEntity be() {
        return menu.getBlockEntity();
    }

    @Override
    protected void init() {
        super.init();

        this.field = new EditBox(this.font, this.leftPos + 84, this.topPos + 22, 102, 12, Component.empty());
        this.field.setTextColor(0x00FF00);
        this.field.setTextColorUneditable(0x00FF00);
        this.field.setBordered(false);
        this.field.setMaxLength(16);
        this.field.setValue(Long.toString(be().outputTarget));
        this.field.setResponder(this::onFieldChanged);
        this.addRenderableWidget(this.field);
    }

    /** Original: {@code keyTyped} - fuehrende Null abschneiden, leeres Feld als 0 behandeln. */
    private void onFieldChanged(String text) {
        if (text.startsWith("0") && text.length() > 1) {
            this.field.setValue(text.substring(1));
            return;
        }
        if (text.isEmpty()) {
            this.field.setValue("0");
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) return;
        }
        long num;
        try {
            num = Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return;
        }
        SetKlystronOutputC2SPacket.sendToServer(be().getBlockPos(), num);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        FusionKlystronBlockEntity be = be();

        long maxPower = be.getMaxEnergyStored();
        if (maxPower > 0) {
            int p = (int) (be.getEnergyStored() * 52L / maxPower);
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 70 - p, 194, 52 - p, 16, p);
        }

        double outputGauge = be.outputTarget <= 0 ? 0 : ((double) be.output / (double) be.outputTarget);
        double airGauge = (double) be.compair.getFill() / (double) be.compair.getMaxFill();
        double powerGauge = FusionTorusBlockEntity.getSpeedScaled(be.getMaxEnergyStored(), be.getEnergyStored());

        // Strom-LED
        if (powerGauge >= 0.5 && be.output > 0) guiGraphics.blit(TEXTURE, this.leftPos + 160, this.topPos + 71, 210, 8, 8, 8);
        else if (powerGauge > 0) guiGraphics.blit(TEXTURE, this.leftPos + 160, this.topPos + 71, 210, 0, 8, 8);
        // Kuehl-LED
        if (airGauge >= 0.5 && be.output > 0) guiGraphics.blit(TEXTURE, this.leftPos + 170, this.topPos + 71, 210, 8, 8, 8);
        else if (airGauge > 0) guiGraphics.blit(TEXTURE, this.leftPos + 170, this.topPos + 71, 210, 0, 8, 8);
        // Betriebs-LED
        if (be.output >= be.outputTarget && be.output > 0) guiGraphics.blit(TEXTURE, this.leftPos + 180, this.topPos + 71, 210, 8, 8, 8);
        else if (be.output > 0) guiGraphics.blit(TEXTURE, this.leftPos + 180, this.topPos + 71, 210, 0, 8, 8);

        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 52, this.topPos + 80, outputGauge, 5, 2, 1, 0xA00000);
        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 88, this.topPos + 80, airGauge, 5, 2, 1, 0xA00000);
        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 124, this.topPos + 80, powerGauge, 5, 2, 1, 0xA00000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, 115 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 35, this.imageHeight - 93, 0x404040, false);

        FusionKlystronBlockEntity be = be();
        String result = "= " + EnergyFormatter.format(be.outputTarget) + "KyU";
        if (be.outputTarget == FusionKlystronBlockEntity.MAX_OUTPUT) result += " (max)";
        guiGraphics.drawString(this.font, result, 183 - this.font.width(result), 40, 0x00FF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FusionKlystronBlockEntity be = be();

        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 52, be.getEnergyStored(), be.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 43, 71, 18, 18, mouseX, mouseY,
                Component.literal("<- ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(EnergyFormatter.format(be.output) + "KyU / "
                                + EnergyFormatter.format(be.outputTarget) + "KyU").withStyle(ChatFormatting.WHITE)));

        be.compair.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 76, this.topPos + 71, 18, 18);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 115, 71, 18, 18, mouseX, mouseY,
                Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(EnergyFormatter.format(be.output) + "HE / "
                                + EnergyFormatter.format(be.outputTarget) + "HE").withStyle(ChatFormatting.WHITE)));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Das Textfeld soll Tippen abfangen, ohne dass "E" das GUI schliesst.
        if (this.field != null && this.field.isFocused() && keyCode != 256) {
            return this.field.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.field != null && this.field.isFocused()) {
            return this.field.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
