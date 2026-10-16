package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineHeatexMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.SetHeatexControlC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port of {@code GUIHeaterHeatex} (1.7.10 Original).
 * Texture 176x204; two tanks 16x52 rendered at (44,88)/(116,88) with info regions at (44,36)/(116,36);
 * two borderless green EditBoxes ("amountToCool" at 73/31, "tickDelay" at 73/49) that send the
 * control packet on EVERY change (original: per-keystroke NBTControlPacket via textboxKeyTyped).
 */
public class GUIMachineHeatex extends GuiInfoScreen<MachineHeatexMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_heatex.png");

    private final MachineHeatexBlockEntity be;

    // Оригинальные координаты (в пикселях относительно левого верхнего угла GUI)
    private static final int TANK_HOT_RENDER_X = 44;
    private static final int TANK_COLD_RENDER_X = 116;
    private static final int TANK_RENDER_Y = 88;
    private static final int TANK_INFO_Y = 36;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;

    private static final int FIELD_X = 73;
    private static final int FIELD_CYCLES_Y = 31;
    private static final int FIELD_DELAY_Y = 49;
    private static final int FIELD_W = 30;
    private static final int FIELD_H = 10;

    private EditBox fieldCycles;
    private EditBox fieldDelay;

    public GUIMachineHeatex(MachineHeatexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.be = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();

        // amountToCool — оригинал: (73, 31), 30x10, зелёный текст, без фона, макс 5 символов
        this.fieldCycles = new EditBox(this.font, this.leftPos + FIELD_X, this.topPos + FIELD_CYCLES_Y, FIELD_W, FIELD_H, Component.literal("Amount per cycle"));
        initText(this.fieldCycles);
        this.fieldCycles.setValue(String.valueOf(be.getAmountToCool()));
        this.fieldCycles.setResponder(text -> {
            int cyc = Math.max(safeParseInt(text), 1);
            SetHeatexControlC2SPacket.sendToServer(be.getBlockPos(), cyc, null);
        });
        this.addRenderableWidget(this.fieldCycles);

        // tickDelay — оригинал: (73, 49), 30x10
        this.fieldDelay = new EditBox(this.font, this.leftPos + FIELD_X, this.topPos + FIELD_DELAY_Y, FIELD_W, FIELD_H, Component.literal("Cycle tick delay"));
        initText(this.fieldDelay);
        this.fieldDelay.setValue(String.valueOf(be.getTickDelay()));
        this.fieldDelay.setResponder(text -> {
            int delay = Math.max(safeParseInt(text), 1);
            SetHeatexControlC2SPacket.sendToServer(be.getBlockPos(), null, delay);
        });
        this.addRenderableWidget(this.fieldDelay);
    }

    /** Оригинал initText(): зелёный текст, без фоновой рамки, макс 5 символов. */
    private static void initText(EditBox field) {
        field.setTextColor(0x00FF00);
        field.setBordered(false);
        field.setMaxLength(5);
    }

    /** Аналог NumberUtils.toInt (invalid -> 0). */
    private static int safeParseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (be != null) {
            // Оригинал renderTank: (44, 88) и (116, 88), 16x52
            be.getHotTank().renderTank(guiGraphics, leftPos + TANK_HOT_RENDER_X, topPos + TANK_RENDER_Y, TANK_W, TANK_H);
            be.getColdTank().renderTank(guiGraphics, leftPos + TANK_COLD_RENDER_X, topPos + TANK_RENDER_Y, TANK_W, TANK_H);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (be != null) {
            // Оригинал renderTankInfo: области (44, 36) и (116, 36), 16x52
            be.getHotTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    leftPos + TANK_HOT_RENDER_X, topPos + TANK_INFO_Y, TANK_W, TANK_H);
            be.getColdTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    leftPos + TANK_COLD_RENDER_X, topPos + TANK_INFO_Y, TANK_W, TANK_H);
        }

        // Оригинальные tooltip-области текстовых полей: (70, 26) и (70, 44), 36x18
        if (isInRegion(mouseX, mouseY, 70, 26, 36, 18)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Amount per cycle"), mouseX, mouseY);
        }
        if (isInRegion(mouseX, mouseY, 70, 44, 36, 18)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Cycle tick delay"), mouseX, mouseY);
        }
    }

    private boolean isInRegion(int mouseX, int mouseY, int x, int y, int w, int h) {
        int gx = mouseX - leftPos;
        int gy = mouseY - topPos;
        return gx >= x && gx < x + w && gy >= y && gy < y + h;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // EditBox.tick() удалён на 1.21.1 (мигание курсора теперь handled-by-render).
        //? if < 1.21.1 {
        this.fieldCycles.tick();
        this.fieldDelay.tick();
        //?}
    }
}
