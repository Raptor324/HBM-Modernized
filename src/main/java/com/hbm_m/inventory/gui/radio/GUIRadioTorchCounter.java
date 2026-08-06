package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchCounterBlockEntity;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.radio.RadioTorchCounterMenu;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Port of {@code GUICounterTorch} (1.7.10 Original). 3 filter slots, each with its own channel field. */
public class GUIRadioTorchCounter extends AbstractContainerScreen<RadioTorchCounterMenu> {

    private final BlockPos pos;
    private final RadioTorchCounterBlockEntity counter;

    private final EditBox[] channelBoxes = new EditBox[3];
    private Checkbox pollingBox;

    public GUIRadioTorchCounter(RadioTorchCounterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.counter = menu.getBlockEntity();
        this.pos = counter.getBlockPos();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        for (int i = 0; i < 3; i++) {
            EditBox box = new EditBox(this.font, left + 30 + i * 30, top + 40, 26, 16, Component.literal("Channel " + i));
            box.setMaxLength(15);
            box.setValue(counter.channel[i] != null ? counter.channel[i] : "");
            channelBoxes[i] = box;
            addRenderableWidget(box);
        }

        pollingBox = new Checkbox(left + 10, top + 62, 100, 18, Component.literal("Polling"), counter.polling);
        addRenderableWidget(pollingBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(left + 48, top + 84, 80, 20).build());
    }

    private void save() {
        CompoundTag data = new CompoundTag();
        data.putBoolean("polling", pollingBox.selected());
        for (int i = 0; i < 3; i++) data.putString("channel" + i, channelBoxes[i].getValue());
        RadioTorchControlPacket.sendToServer(pos, data);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < 3; i++) {
            Slot slot = this.menu.slots.get(i);
            String mode = counter.getMatcher().getMode(i);
            if (mode != null && isHoveringSlot(slot, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font,
                        java.util.List.of(Component.literal("§cRight click to change"),
                                Component.literal(ModulePatternMatcher.getLabel(mode))),
                        mouseX, mouseY);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 3; i++) {
            Slot slot = this.menu.slots.get(i);
            if (isHoveringSlot(slot, (int) mouseX, (int) mouseY) && button == 1 && slot.hasItem()) {
                counter.nextFilterMode(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringSlot(Slot slot, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= slot.x && localX < slot.x + 16 && localY >= slot.y && localY < slot.y + 16;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }
}
