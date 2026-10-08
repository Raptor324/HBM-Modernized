package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;
import com.hbm_m.inventory.menu.MachineHeatexMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIHeaterHeatex}: Tanks, dazu die Felder "Menge je Zyklus" und "Zyklusverzoegerung" (NBTControlPacket). */
public class GUIMachineHeatex extends GuiInfoScreen<MachineHeatexMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_heatex.png");

    private final MachineHeatexBlockEntity heater;
    private EditBox fieldCycles;
    private EditBox fieldDelay;

    public GUIMachineHeatex(MachineHeatexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.heater = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        this.fieldCycles = new EditBox(this.font, leftPos + 73, topPos + 31, 30, 10, Component.empty());
        initText(this.fieldCycles);
        this.fieldCycles.setValue(String.valueOf(heater != null ? heater.amountToCool : 24_000));
        this.fieldCycles.setResponder(s -> send("toCool", s));

        this.fieldDelay = new EditBox(this.font, leftPos + 73, topPos + 49, 30, 10, Component.empty());
        initText(this.fieldDelay);
        this.fieldDelay.setValue(String.valueOf(heater != null ? heater.tickDelay : 1));
        this.fieldDelay.setResponder(s -> send("delay", s));

        this.addRenderableWidget(fieldCycles);
        this.addRenderableWidget(fieldDelay);
    }

    protected void initText(EditBox field) {
        field.setTextColor(0x00ff00);
        field.setTextColorUneditable(0x00ff00);
        field.setBordered(false);
        field.setMaxLength(5);
    }

    private void send(String key, String text) {
        if (heater == null) return;
        int v;
        try { v = Integer.parseInt(text.trim()); } catch (NumberFormatException e) { v = 0; }
        CompoundTag data = new CompoundTag();
        data.putInt(key, Math.max(v, 1));
        com.hbm_m.network.NBTControlPacket.sendToServer(heater.getBlockPos(), data);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((fieldCycles.isFocused() || fieldDelay.isFocused()) && keyCode != 256) {
            return (fieldCycles.isFocused() ? fieldCycles : fieldDelay).keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (heater != null) {
            heater.tanks[0].renderTank(guiGraphics, leftPos + 44, topPos + 36, 16, 52);
            heater.tanks[1].renderTank(guiGraphics, leftPos + 116, topPos + 36, 16, 52);
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
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (heater != null) {
            heater.tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 44, topPos + 36, 16, 52);
            heater.tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 116, topPos + 36, 16, 52);
        }

        if (leftPos + 70 <= mouseX && leftPos + 70 + 36 > mouseX && topPos + 26 < mouseY && topPos + 26 + 18 >= mouseY) {
            guiGraphics.renderTooltip(this.font, Component.literal("Amount per cycle"), mouseX, mouseY);
        }
        if (leftPos + 70 <= mouseX && leftPos + 70 + 36 > mouseX && topPos + 44 < mouseY && topPos + 44 + 18 >= mouseY) {
            guiGraphics.renderTooltip(this.font, Component.literal("Cycle tick delay"), mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
