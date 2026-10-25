package com.hbm_m.inventory.gui.radio;

import java.util.List;

import com.hbm_m.blockentity.network.radio.RadioTorchCounterBlockEntity;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.radio.RadioTorchCounterMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 1:1 {@code GUICounterTorch}: drei Kanalfelder zu den drei Musterplaetzen, Abfrage (193,8), Speichern (193,30). */
public class GUIRadioTorchCounter extends AbstractContainerScreen<RadioTorchCounterMenu> {

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_counter.png");

    private final BlockPos pos;
    protected final RadioTorchCounterBlockEntity counter;
    protected final EditBox[] frequency = new EditBox[3];

    public GUIRadioTorchCounter(RadioTorchCounterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.translatable("container.rttyCounter"));
        this.counter = menu.getBlockEntity();
        this.pos = counter.getBlockPos();
        this.imageWidth = 218;
        this.imageHeight = 238;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < 3; i++) {
            this.frequency[i] = new EditBox(this.font, leftPos + 29, topPos + 21 + 44 * i, 86, 14, Component.empty());
            this.frequency[i].setTextColor(0x00ff00);
            this.frequency[i].setTextColorUneditable(0x00ff00);
            this.frequency[i].setBordered(false);
            this.frequency[i].setMaxLength(10);
            this.frequency[i].setValue(counter.channel[i] == null ? "" : counter.channel[i]);
            addWidget(this.frequency[i]);
        }
    }

    private boolean in(double x, double y, int left, int top) {
        return leftPos + left <= x && leftPos + left + 18 > x && topPos + top < y && topPos + top + 18 >= y;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (in(x, y, 193, 8)) g.renderComponentTooltip(this.font, List.of(Component.literal(counter.polling ? "Polling" : "State Change")), x, y);
        if (in(x, y, 193, 30)) g.renderComponentTooltip(this.font, List.of(Component.literal("Save Settings")), x, y);

        if (this.menu.getCarried().isEmpty()) {
            for (int i = 0; i < 3; ++i) {
                Slot slot = this.menu.slots.get(i);
                String mode = counter.getMatcher().getMode(i);
                if (this.isHovering(slot.x, slot.y, 16, 16, x, y) && mode != null) {
                    g.renderComponentTooltip(this.font, List.of(Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal(ModulePatternMatcher.getLabel(mode))), x, y - 30);
                }
            }
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean hit = false;
        for (EditBox f : frequency) {
            boolean on = f.mouseClicked(x, y, i);
            f.setFocused(on);
            hit |= on;
        }

        if (in(x, y, 193, 8)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", !counter.polling);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (in(x, y, 193, 30)) {
            click();
            CompoundTag data = new CompoundTag();
            for (int j = 0; j < 3; j++) data.putString("channel" + j, this.frequency[j].getValue());
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        return hit || super.mouseClicked(x, y, i);
    }

    private void click() {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean charTyped(char c, int mods) {
        for (EditBox f : frequency) if (f.isFocused() && f.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != 256) {
            for (EditBox f : frequency) {
                if (f.isFocused()) {
                    f.keyPressed(key, scan, mods);
                    return true;
                }
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
        String name = this.title.getString();
        g.drawString(this.font, name, 184 / 2 - this.font.width(name) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 16, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (counter.polling) {
            g.blit(texture, leftPos + 193, topPos + 8, 218, 0, 18, 18);
        }

        for (int i = 0; i < 3; i++) this.frequency[i].render(g, x, y, interp);
    }
}
