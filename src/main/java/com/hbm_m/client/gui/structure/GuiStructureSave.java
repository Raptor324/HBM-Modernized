package com.hbm_m.client.gui.structure;

import com.hbm_m.blockentity.generic.WandStructureBlockEntity;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** 1:1 {@code BlockWandStructure.GuiStructureSave}: Name, Groesse X/Y/Z und SAVE-Knopf; Daten gehen beim Schliessen raus. */
public class GuiStructureSave extends Screen {

    private final WandStructureBlockEntity tile;

    private EditBox textName;

    private EditBox textSizeX;
    private EditBox textSizeY;
    private EditBox textSizeZ;

    private boolean saveOnClose = false;

    public GuiStructureSave(WandStructureBlockEntity tile) {
        super(Component.literal("Structure Save"));
        this.tile = tile;
    }

    @Override
    protected void init() {
        textName = new EditBox(font, width / 2 - 150, 50, 300, 20, Component.empty());
        textName.setMaxLength(256);
        textName.setValue(tile.name);

        textSizeX = new EditBox(font, width / 2 - 150, 100, 50, 20, Component.empty());
        textSizeX.setValue("" + tile.sizeX);
        textSizeY = new EditBox(font, width / 2 - 100, 100, 50, 20, Component.empty());
        textSizeY.setValue("" + tile.sizeY);
        textSizeZ = new EditBox(font, width / 2 - 50, 100, 50, 20, Component.empty());
        textSizeZ.setValue("" + tile.sizeZ);

        addRenderableWidget(textName);
        addRenderableWidget(textSizeX);
        addRenderableWidget(textSizeY);
        addRenderableWidget(textSizeZ);

        addRenderableWidget(Button.builder(Component.literal("SAVE"), b -> {
            saveOnClose = true;
            onClose();
        }).bounds(width / 2 - 150, 150, 300, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        CompoundTag data = tile.writeSettings();

        data.putString("name", textName.getValue());

        try { data.putInt("sizeX", Integer.parseInt(textSizeX.getValue())); } catch (Exception ex) { }
        try { data.putInt("sizeY", Integer.parseInt(textSizeY.getValue())); } catch (Exception ex) { }
        try { data.putInt("sizeZ", Integer.parseInt(textSizeZ.getValue())); } catch (Exception ex) { }

        if (saveOnClose) data.putBoolean("save", true);

        NBTControlPacket.sendToServer(tile.getBlockPos(), data);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
