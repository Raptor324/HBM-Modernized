package com.hbm_m.inventory.gui;

import com.hbm_m.api.energy.CableDiodeBlockEntity;
import com.hbm_m.blockentity.machines.NetworkSeparatorBlockEntity;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 1:1 {@code GUIDiode}: Durchsatzfeld und Prioritaetsknopf, beim Schliessen per {@code NBTControlPacket} gesendet. */
@OnlyIn(Dist.CLIENT)
public class GUIDiode extends Screen {

    protected final BlockPos pos;
    private final long initialLimit;
    private EditBox textThroughput;
    private Button buttonPriority;
    private int priority;

    public GUIDiode(CableDiodeBlockEntity diode) {
        super(Component.empty());
        this.pos = diode.getBlockPos();
        this.initialLimit = diode.limit;
        this.priority = diode.priority.ordinal();
    }

    public GUIDiode(NetworkSeparatorBlockEntity separator) {
        super(Component.empty());
        this.pos = separator.getBlockPos();
        this.initialLimit = separator.configuredLimit;
        this.priority = separator.priority.ordinal();
    }

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be instanceof CableDiodeBlockEntity diode) {
            mc.setScreen(new GUIDiode(diode));
        } else if (be instanceof NetworkSeparatorBlockEntity separator) {
            mc.setScreen(new GUIDiode(separator));
        }
    }

    @Override
    protected void init() {
        textThroughput = new EditBox(font, this.width / 2 - 150, 100, 90, 20, Component.empty());
        textThroughput.setMaxLength(11);
        textThroughput.setValue("" + initialLimit);
        addRenderableWidget(textThroughput);
        buttonPriority = Button.builder(Component.literal(IEnergyReceiver.Priority.values()[priority].name()), b -> {
            this.priority++;
            if (priority >= IEnergyReceiver.Priority.values().length) priority = 0;
            b.setMessage(Component.literal(IEnergyReceiver.Priority.values()[priority].name()));
        }).bounds(this.width / 2 + 20, 100, 90, 20).build();
        addRenderableWidget(buttonPriority);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTicks);
        g.drawString(font, "Throughput:", this.width / 2 - 150, 80, 0xA0A0A0);
        g.drawString(font, "(max. 10,000,000,000 HE)", this.width / 2 - 150, 90, 0xA0A0A0);
        g.drawString(font, "Priority:", this.width / 2 + 20, 80, 0xA0A0A0);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putByte("priority", (byte) priority);
        try { data.putLong("limit", Long.parseLong(textThroughput.getValue())); } catch (Exception ignored) {}
        NBTControlPacket.sendToServer(pos, data);
        super.removed();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (textThroughput.keyPressed(key, scan, mods)) return true;
        if (key == 256 || (!textThroughput.isFocused() && minecraft != null && minecraft.options.keyInventory.matches(key, scan))) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean isPauseScreen() { return false; }
}
