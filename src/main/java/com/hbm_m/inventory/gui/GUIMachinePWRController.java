package com.hbm_m.inventory.gui;

import java.util.Locale;

import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.inventory.menu.PWRControllerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code GUIPWR}: Textfeld fuer die Steuerstabstellung, Bestaetigen ueber den Knopf rechts daneben. */
public class GUIMachinePWRController extends GuiInfoScreen<PWRControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_pwr.png");

    private final PWRControllerBlockEntity controller;
    private EditBox field;

    public GUIMachinePWRController(PWRControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.controller = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 188;
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(this.font, leftPos + 57, topPos + 63, 30, 8, Component.empty());
        this.field.setTextColor(0x00ff00);
        this.field.setTextColorUneditable(0x008000);
        this.field.setBordered(false);
        this.field.setMaxLength(3);
        String text = controller == null ? "" : (100 - controller.rodTarget) + "";
        this.field.setValue(text.length() > 3 ? text.substring(0, 3) : text);
        this.addRenderableWidget(this.field);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (controller != null) {
            drawCustomInfoStat(g, x, y, 115, 31, 18, 18, x, y, Component.literal("Core: " + String.format(Locale.US, "%,d", controller.coreHeat) + " / " + String.format(Locale.US, "%,d", controller.coreHeatCapacity) + " TU"));
            drawCustomInfoStat(g, x, y, 151, 31, 18, 18, x, y, Component.literal("Hull: " + String.format(Locale.US, "%,d", controller.hullHeat) + " / " + String.format(Locale.US, "%,d", PWRControllerBlockEntity.hullHeatCapacityBase) + " TU"));

            drawCustomInfoStat(g, x, y, 52, 31, 36, 18, x, y, Component.literal(((int) (controller.progress * 100 / controller.processTime)) + "%"));
            drawCustomInfoStat(g, x, y, 52, 53, 54, 4, x, y, Component.literal("Control rod level: " + (100 - (Math.round(controller.rodLevel * 100) / 100)) + "%"));

            if (controller.typeLoaded != -1 && controller.amountLoaded > 0) {
                ItemStack display = new ItemStack(PWRControllerBlockEntity.freshFuelItemFor(PWRControllerBlockEntity.fuelType(controller.typeLoaded)));
                if (leftPos + 88 <= x && leftPos + 88 + 18 > x && topPos + 4 < y && topPos + 4 + 18 >= y) g.renderTooltip(this.font, display, x, y);
            }

            controller.tanks[0].renderTankInfo(g, font, x, y, leftPos + 8, topPos + 5, 16, 52);
            controller.tanks[1].renderTankInfo(g, font, x, y, leftPos + 26, topPos + 5, 16, 52);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        if (controller == null) return;

        float scale = 1.25F;
        String flux = String.format(Locale.US, "%,.1f", controller.flux);
        g.pose().pushPose();
        g.pose().scale(1 / scale, 1 / scale, 1);
        g.drawString(this.font, flux, (int) (165 * scale - this.font.width(flux)), (int) (64 * scale), 0x00ff00, false);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (controller == null) return;

        if (controller.hullHeat > PWRControllerBlockEntity.hullHeatCapacityBase * 0.8 || controller.coreHeat > controller.coreHeatCapacity * 0.8)
            g.blit(TEXTURE, leftPos + 147, topPos, 176, 14, 26, 26);

        int p = (int) (controller.progress * 33 / controller.processTime);
        g.blit(TEXTURE, leftPos + 54, topPos + 33, 176, 0, p, 14);

        int c = (int) (controller.rodLevel * 52 / 100);
        g.blit(TEXTURE, leftPos + 53, topPos + 54, 176, 40, c, 2);

        GuiGaugeNeedle.draw(g, leftPos + 124, topPos + 40, (double) controller.coreHeat / (double) controller.coreHeatCapacity, 5, 2, 1, 0x7F0000);
        GuiGaugeNeedle.draw(g, leftPos + 160, topPos + 40, (double) controller.hullHeat / (double) PWRControllerBlockEntity.hullHeatCapacityBase, 5, 2, 1, 0x7F0000);

        if (controller.typeLoaded != -1 && controller.amountLoaded > 0) {
            ItemStack display = new ItemStack(PWRControllerBlockEntity.freshFuelItemFor(PWRControllerBlockEntity.fuelType(controller.typeLoaded)));
            drawFuelStack(g, display, leftPos + 89, topPos + 5, ChatFormatting.YELLOW + "" + controller.amountLoaded + "/" + controller.rodCount);
        }

        // Original: renderTank(x, yUnten, z, 16, 52)
        controller.tanks[0].renderTank(g, leftPos + 8, topPos + 57 - 52, 16, 52);
        controller.tanks[1].renderTank(g, leftPos + 26, topPos + 57 - 52, 16, 52);
    }

    /** Original: {@code drawItemStack} mit halb grosser Beschriftung unter dem Gegenstand. */
    private void drawFuelStack(GuiGraphics g, ItemStack stack, int x, int y, String label) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 32.0F);
        g.renderItem(stack, x, y);
        g.pose().scale(0.5F, 0.5F, 0.5F);
        int w = this.font.width(label);
        int lx = (x + w / 4) * 2;
        int ly = (y + 15) * 2;
        g.pose().translate(0.0F, 0.0F, 200.0F);
        g.drawString(this.font, label, lx + 19 - 2 - w, ly + 6 + 3, 0xFFFFFF, true);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (controller != null && leftPos + 88 <= mouseX && leftPos + 88 + 18 > mouseX && topPos + 58 < mouseY && topPos + 58 + 18 >= mouseY) {
            try {
                int level = (int) Mth.clamp(Double.parseDouble(field.getValue()), 0, 100);
                field.setValue(level + "");

                CompoundTag control = new CompoundTag();
                control.putInt("control", 100 - level);
                NBTControlPacket.sendToServer(controller.getBlockPos(), control);
                playClickSound();
            } catch (NumberFormatException ignored) {
                // Original: NumberUtils.isNumber - sonst passiert nichts
            }
            return true;
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.field.isFocused() && this.field.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (this.field.isFocused() && keyCode != 256) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
