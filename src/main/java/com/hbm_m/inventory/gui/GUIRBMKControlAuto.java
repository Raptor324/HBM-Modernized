package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.RBMKControlAutoMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RBMKControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port of {@code GUIRBMKControlAuto} (1.7.10 Original), rendered with the real
 * {@code gui_rbmk_control_auto.png} texture. Like the original, this is purely
 * a settings/status screen (no machine item slots) for the automated control
 * rod's level/heat curve.
 *
 * <p>Value edits mutate the {@link RBMKControlAutoBlockEntity} fields directly for
 * immediate client-side feedback, then send an {@code RBMKControlPacket} so the
 * change actually reaches the server (a purely client-side mutation is invisible to
 * the server and gets overwritten on the next sync - this bit both this GUI and the
 * manual {@link GUIRBMKControl} sibling, where it manifested as the rod level being
 * stuck at 0% and unselectable).
 *
 * <p>Not restored from the original: the power icon/tooltip (the modernized
 * {@code RBMKControlBlockEntity} hierarchy has no {@code power}/{@code maxPower}/
 * {@code isPowered()} fields), matching the same limitation already documented
 * on {@link GUIRBMKControl}.
 */
public class GUIRBMKControlAuto extends GuiInfoScreen<RBMKControlAutoMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_control_auto.png");

    private final RBMKControlAutoBlockEntity be;
    private final EditBox[] fields = new EditBox[4];

    public GUIRBMKControlAuto(RBMKControlAutoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < 4; i++) {
            EditBox box = new EditBox(this.font, leftPos + 30, topPos + 27 + 11 * i, 26, 9, Component.empty());
            box.setBordered(false);
            box.setTextColor(0xFFFFFFFF);
            box.setMaxLength(i < 2 ? 3 : 4);
            fields[i] = box;
            this.addRenderableWidget(box);
        }

        fields[0].setValue(String.valueOf((int) be.levelUpper));
        fields[1].setValue(String.valueOf((int) be.levelLower));
        fields[2].setValue(String.valueOf((int) be.heatUpper));
        fields[3].setValue(String.valueOf((int) be.heatLower));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Rod-level bar (same formula as the manual RBMKControl GUI).
        int height = (int) (56 * (1.0 - be.level));
        if (height > 0) {
            g.blit(TEXTURE, leftPos + 124, topPos + 29, 176, 56 - height, 8, height);
        }

        // Interpolation-function icon (Linear / Quadratic / Inverse Quadratic).
        int f = be.function.ordinal();
        g.blit(TEXTURE, leftPos + 59, topPos + 27, 184, f * 19, 26, 19);

        for (EditBox box : fields) {
            box.render(g, mx, my, partial);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mx, my, partial);
        super.render(g, mx, my, partial);

        drawCustomInfoStat(g, mx, my, 124, 29, 16, 56, mx, my,
                Component.literal((int) (be.level * 100) + "%"));

        drawCustomInfoStat(g, mx, my, 58, 26, 28, 19, mx, my,
                Component.literal("Function: " + functionLabel(be.function)));
        drawCustomInfoStat(g, mx, my, 61, 48, 22, 10, mx, my,
                Component.literal("Select linear interpolation"));
        drawCustomInfoStat(g, mx, my, 61, 59, 22, 10, mx, my,
                Component.literal("Select quadratic interpolation"));
        drawCustomInfoStat(g, mx, my, 61, 70, 22, 10, mx, my,
                Component.literal("Select inverse quadratic interpolation"));

        drawCustomInfoStat(g, mx, my, 28, 26, 30, 10, mx, my,
                Component.literal("Level at max heat"), Component.literal("Should be smaller than level at min heat"));
        drawCustomInfoStat(g, mx, my, 28, 37, 30, 10, mx, my,
                Component.literal("Level at min heat"), Component.literal("Should be larger than level at max heat"));
        drawCustomInfoStat(g, mx, my, 28, 48, 30, 10, mx, my,
                Component.literal("Max heat"), Component.literal("Must be larger than min heat"));
        drawCustomInfoStat(g, mx, my, 28, 59, 30, 10, mx, my,
                Component.literal("Min heat"), Component.literal("Must be smaller than max heat"));
        drawCustomInfoStat(g, mx, my, 28, 70, 30, 10, mx, my,
                Component.literal("Save parameters"));

        renderTooltip(g, mx, my);
    }

    private static String functionLabel(RBMKControlAutoBlockEntity.RBMKFunction function) {
        return switch (function) {
            case LINEAR -> "Linear";
            case QUAD_UP -> "Quadratic";
            case QUAD_DOWN -> "Inverse Quadratic";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (isPointInRect(28, 70, 30, 10, (int) mouseX, (int) mouseY)) {
            playClickSound();
            saveFields();
            return true;
        }

        for (int k = 0; k < 3; k++) {
            if (isPointInRect(61, 48 + k * 11, 22, 10, (int) mouseX, (int) mouseY)) {
                playClickSound();
                be.function = RBMKControlAutoBlockEntity.RBMKFunction.values()[k];
                RBMKControlPacket.sendSetFunction(be.getBlockPos(), k);
                return true;
            }
        }

        return handled;
    }

    private void saveFields() {
        double[] vals = new double[4];
        for (int k = 0; k < 4; k++) {
            double clamp = k < 2 ? 100 : 9999;
            String text = fields[k].getValue();
            double parsed;
            try {
                parsed = Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                parsed = 0;
            }
            int value = (int) net.minecraft.util.Mth.clamp(parsed, 0, clamp);
            fields[k].setValue(String.valueOf(value));
            vals[k] = value;
        }

        be.levelUpper = vals[0];
        be.levelLower = vals[1];
        be.heatUpper = vals[2];
        be.heatLower = vals[3];
        RBMKControlPacket.sendSetParams(be.getBlockPos(), vals[0], vals[1], vals[2], vals[3]);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : fields) {
            if (box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : fields) {
            if (box.isFocused() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
}
