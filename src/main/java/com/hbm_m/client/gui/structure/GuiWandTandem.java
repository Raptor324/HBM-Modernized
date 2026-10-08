package com.hbm_m.client.gui.structure;

import com.hbm_m.blockentity.generic.WandTandemBlockEntity;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** 1:1 {@code BlockWandTandem.GuiWandTandem}: Zielpool, Zielname und Gelenkart. */
public class GuiWandTandem extends Screen {

    private final WandTandemBlockEntity jigsaw;

    private EditBox textPool;
    private EditBox textTarget;

    private boolean rollable;

    public GuiWandTandem(WandTandemBlockEntity jigsaw) {
        super(Component.literal("Tandem"));
        this.jigsaw = jigsaw;
    }

    @Override
    protected void init() {
        textPool = new EditBox(font, width / 2 - 150, 50, 300, 20, Component.empty());
        textPool.setMaxLength(256);
        textPool.setValue(jigsaw.pool);
        addRenderableWidget(textPool);

        textTarget = new EditBox(font, width / 2 + 10, 100, 140, 20, Component.empty());
        textTarget.setMaxLength(256);
        textTarget.setValue(jigsaw.target);
        addRenderableWidget(textTarget);

        rollable = jigsaw.isRollable;
        addRenderableWidget(Button.builder(Component.literal(rollable ? "Rollable" : "Aligned"), b -> {
            rollable = !rollable;
            b.setMessage(Component.literal(rollable ? "Rollable" : "Aligned"));
        }).bounds(width / 2 + 60, 150, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        com.hbm_m.client.GuiCompat.renderBackground(this, graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(font, "Target pool:", width / 2 - 150, 37, 0xA0A0A0);
        graphics.drawString(font, "Target name:", width / 2 + 10, 87, 0xA0A0A0);
        graphics.drawString(font, "Joint type:", width / 2 + 60, 137, 0xA0A0A0);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.put("block", net.minecraft.nbt.NbtUtils.writeBlockState(jigsaw.replaceBlock));
        data.putString("pool", textPool.getValue());
        data.putString("target", textTarget.getValue());
        data.putBoolean("roll", rollable);

        NBTControlPacket.sendToServer(jigsaw.getBlockPos(), data);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
