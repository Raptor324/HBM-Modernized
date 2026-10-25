package com.hbm_m.client.gui.structure;

import com.hbm_m.blockentity.generic.WandJigsawBlockEntity;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 1:1 {@code BlockWandJigsaw.GuiWandJigsaw}: Zielpool, Name, Zielname, Auswahl-/Platzierungsprioritaet und
 * Gelenkart; beim Schliessen gehen die Werte per {@code NBTControlPacket} an den Server.
 */
public class GuiWandJigsaw extends Screen {

    private final WandJigsawBlockEntity jigsaw;

    private EditBox textPool;
    private EditBox textName;
    private EditBox textTarget;

    private EditBox textSelectionPriority;
    private EditBox textPlacementPriority;

    private Button jointToggle;
    private boolean rollable;

    public GuiWandJigsaw(WandJigsawBlockEntity jigsaw) {
        super(Component.literal("Jigsaw"));
        this.jigsaw = jigsaw;
    }

    @Override
    protected void init() {
        textPool = box(width / 2 - 150, 50, 300, jigsaw.pool);
        textName = box(width / 2 - 150, 100, 140, jigsaw.name);
        textTarget = box(width / 2 + 10, 100, 140, jigsaw.target);
        textSelectionPriority = box(width / 2 - 150, 150, 90, "" + jigsaw.selectionPriority);
        textPlacementPriority = box(width / 2 - 40, 150, 90, "" + jigsaw.placementPriority);

        rollable = jigsaw.isRollable;
        jointToggle = addRenderableWidget(Button.builder(Component.literal(rollable ? "Rollable" : "Aligned"), b -> {
            rollable = !rollable;
            b.setMessage(Component.literal(rollable ? "Rollable" : "Aligned"));
        }).bounds(width / 2 + 60, 150, 90, 20).build());
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setMaxLength(256);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        com.hbm_m.client.GuiCompat.renderBackground(this, graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(font, "Target pool:", width / 2 - 150, 37, 0xA0A0A0);
        graphics.drawString(font, "Name:", width / 2 - 150, 87, 0xA0A0A0);
        graphics.drawString(font, "Target name:", width / 2 + 10, 87, 0xA0A0A0);
        graphics.drawString(font, "Selection priority:", width / 2 - 150, 137, 0xA0A0A0);
        graphics.drawString(font, "Placement priority:", width / 2 - 40, 137, 0xA0A0A0);
        graphics.drawString(font, "Joint type:", width / 2 + 60, 137, 0xA0A0A0);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putInt("selection", jigsaw.selectionPriority);
        data.putInt("placement", jigsaw.placementPriority);
        data.put("block", net.minecraft.nbt.NbtUtils.writeBlockState(jigsaw.replaceBlock));

        data.putString("pool", textPool.getValue());
        data.putString("name", textName.getValue());
        data.putString("target", textTarget.getValue());

        try { data.putInt("selection", Integer.parseInt(textSelectionPriority.getValue())); } catch (Exception ex) { }
        try { data.putInt("placement", Integer.parseInt(textPlacementPriority.getValue())); } catch (Exception ex) { }

        data.putBoolean("roll", rollable);

        NBTControlPacket.sendToServer(jigsaw.getBlockPos(), data);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
