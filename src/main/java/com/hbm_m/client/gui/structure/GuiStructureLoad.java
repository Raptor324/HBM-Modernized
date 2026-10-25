package com.hbm_m.client.gui.structure;

import java.io.File;
import java.io.FileFilter;
import java.util.function.Consumer;

import com.hbm_m.blockentity.generic.WandStructureBlockEntity;
import com.hbm_m.network.NBTControlPacket;
import com.hbm_m.world.gen.nbt.NBTStructure;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 1:1 {@code BlockWandStructure.GuiStructureLoad}: Namensfeld (zugleich Filter), Dateiliste der .nbt-Dateien im
 * Strukturverzeichnis ({@code GuiFileList}) und LOAD-Knopf.
 */
public class GuiStructureLoad extends Screen {

    private final WandStructureBlockEntity tile;

    private EditBox textName;

    private GuiFileList fileList;

    private boolean loadOnClose = false;

    private static String nameFilter = "";
    private static final FileFilter structureFilter = file -> {
        if (!file.isFile() || !file.getName().endsWith(".nbt")) return false;
        return nameFilter.isEmpty() || file.getName().contains(nameFilter);
    };

    public GuiStructureLoad(WandStructureBlockEntity tile) {
        super(Component.literal("Structure Load"));
        this.tile = tile;
    }

    @Override
    protected void init() {
        textName = new EditBox(font, width / 2 - 150, 50, 300, 20, Component.empty());
        textName.setMaxLength(256);
        textName.setValue(tile.name);
        nameFilter = tile.name;
        textName.setResponder(text -> {
            if (!suppressFilter && !nameFilter.equals(text)) {
                nameFilter = text;
                rebuildList();
            }
        });

        rebuildList();

        addRenderableWidget(textName);

        addRenderableWidget(Button.builder(Component.literal("LOAD"), b -> {
            loadOnClose = true;
            onClose();
        }).bounds(width / 2 - 150, height - 70, 300, 20).build());
    }

    private void rebuildList() {
        if (fileList != null) removeWidget(fileList);
        File[] files = NBTStructure.getStructureDirectory().listFiles(structureFilter);
        fileList = new GuiFileList(minecraft, files != null ? files : new File[0], this::selectFile, nameFilter, width, height, 70, height - 90, 16);
        addWidget(fileList);
    }

    public void selectFile(File file) {
        String fileName = file.getName();
        // Original setzt nur den Text, die Liste wird erst beim Tippen neu gefiltert
        suppressFilter = true;
        textName.setValue(fileName.substring(0, fileName.length() - 4));
        suppressFilter = false;
    }

    private boolean suppressFilter = false;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        com.hbm_m.client.GuiCompat.renderBackground(this, graphics, mouseX, mouseY, partialTicks);
        fileList.render(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        fileList.select(textName.getValue());
        return handled;
    }

    @Override
    public void removed() {
        CompoundTag data = tile.writeSettings();

        data.putString("name", textName.getValue());

        if (loadOnClose) data.putBoolean("load", true);

        NBTControlPacket.sendToServer(tile.getBlockPos(), data);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 1:1 {@code GuiFileList}: Dateiliste, Klick waehlt die Datei aus. */
    public static class GuiFileList extends ObjectSelectionList<GuiFileList.Row> {

        public GuiFileList(Minecraft mc, File[] files, Consumer<File> onSelect, String nameFilter, int width, int height, int top, int bottom, int slotHeight) {
            //? if < 1.21.1 {
            super(mc, width, height, top, bottom, slotHeight);
            //?} else {
            /*super(mc, width, bottom - top, top, slotHeight);
            *///?}

            for (File file : files) {
                Row row = new Row(this, file, onSelect);
                addEntry(row);
                if (file.getName().equals(nameFilter + ".nbt")) {
                    setSelected(row);
                }
            }
        }

        public void select(String nameFilter) {
            for (Row row : children()) {
                if (row.file.getName().equals(nameFilter + ".nbt")) {
                    setSelected(row);
                    return;
                }
            }
        }

        public static class Row extends ObjectSelectionList.Entry<Row> {

            private final GuiFileList list;
            private final File file;
            private final Consumer<File> onSelect;

            public Row(GuiFileList list, File file, Consumer<File> onSelect) {
                this.list = list;
                this.file = file;
                this.onSelect = onSelect;
            }

            @Override
            public void render(GuiGraphics graphics, int id, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                graphics.drawString(Minecraft.getInstance().font, file.getName(), x + 20, y + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                list.setSelected(this);
                onSelect.accept(file);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(file.getName());
            }
        }
    }
}
