package com.hbm_m.inventory.gui;

import com.hbm_m.api.fluids.ConnectionPriority;
import com.hbm_m.blockentity.machines.FluidPumpBlockEntity;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 1:1-Port von {@code GUIPump} (1.7.10): Durchsatzfeld, Druckknopf (0-5 PU) und Prioritaetsknopf
 * nebeneinander auf Hoehe 100; beim Schliessen gehen alle drei Werte per NBTControlPacket an die Pumpe.
 */
public class GUIPump extends Screen {

    protected final FluidPumpBlockEntity pump;

    private EditBox textPlacementPriority;
    private Button buttonPressure;
    private Button buttonPriority;
    private int pressure;
    private int priority;

    public GUIPump(FluidPumpBlockEntity pump) {
        super(Component.empty());
        this.pump = pump;
        this.pressure = pump.getAllTanks()[0].getPressure();
        this.priority = pump.priority.ordinal();
    }

    /** Client: aus {@code FluidPumpBlock#use} geoeffnet. */
    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof FluidPumpBlockEntity pump) {
            mc.setScreen(new GUIPump(pump));
        }
    }

    @Override
    protected void init() {
        textPlacementPriority = new EditBox(this.font, this.width / 2 - 150, 100, 90, 20, Component.empty());
        textPlacementPriority.setMaxLength(5);
        textPlacementPriority.setValue("" + pump.bufferSize);
        addRenderableWidget(textPlacementPriority);

        buttonPressure = Button.builder(Component.literal(pressure + " PU"), b -> {
            this.pressure++;
            if (pressure > 5) pressure = 0;
            buttonPressure.setMessage(Component.literal(pressure + " PU"));
        }).bounds(this.width / 2 - 50, 100, 90, 20).build();
        addRenderableWidget(buttonPressure);

        buttonPriority = Button.builder(Component.literal(ConnectionPriority.values()[priority].name()), b -> {
            this.priority++;
            if (priority >= ConnectionPriority.values().length) priority = 0;
            buttonPriority.setMessage(Component.literal(ConnectionPriority.values()[priority].name()));
        }).bounds(this.width / 2 + 50, 100, 90, 20).build();
        addRenderableWidget(buttonPriority);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTicks);

        g.drawString(this.font, "Throughput:", this.width / 2 - 150, 80, 0xA0A0A0);
        g.drawString(this.font, "(max. 10,000mB)", this.width / 2 - 150, 90, 0xA0A0A0);
        g.drawString(this.font, "Pressure:", this.width / 2 - 50, 80, 0xA0A0A0);
        g.drawString(this.font, "Priority:", this.width / 2 + 50, 80, 0xA0A0A0);

        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putByte("pressure", (byte) pressure);
        data.putByte("priority", (byte) priority);
        try { data.putInt("capacity", Integer.parseInt(textPlacementPriority.getValue())); } catch (Exception ignored) { }
        NBTControlPacket.sendToServer(pump.getBlockPos(), data);
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textPlacementPriority != null && textPlacementPriority.isFocused() && keyCode != 256) {
            return textPlacementPriority.keyPressed(keyCode, scanCode, modifiers) || textPlacementPriority.canConsumeInput();
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
