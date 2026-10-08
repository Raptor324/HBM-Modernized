package com.hbm_m.inventory.gui.radio;

import java.util.List;

import com.hbm_m.blockentity.network.radio.RadioTorchBaseBlockEntity;
import com.hbm_m.blockentity.network.radio.RadioTorchSenderBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code GUIScreenRadioTorch} (Sender/Empfaenger): Kanalfeld, Schalter "Eigene Zuordnung", "Abfrage",
 * "Speichern"; mit eigener Zuordnung die 16 Zuordnungsfelder auf der Original-Textur.
 */
public class GUIRadioTorchSimple extends Screen {

    public static final int MAX_CHANNEL_LENGTH = 15;

    protected static final ResourceLocation textureSender = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_sender.png");
    protected static final ResourceLocation textureReceiver = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_receiver.png");

    private final BlockPos pos;
    protected final RadioTorchBaseBlockEntity radio;
    protected final ResourceLocation texture;
    protected int xSize = 256;
    protected int ySize = 204;
    protected int guiLeft;
    protected int guiTop;
    protected EditBox frequency;
    protected final EditBox[] remap = new EditBox[16];

    public GUIRadioTorchSimple(BlockPos pos, RadioTorchBaseBlockEntity radio, Component title) {
        super(Component.translatable(radio instanceof RadioTorchSenderBlockEntity ? "container.rttySender" : "container.rttyReceiver"));
        this.pos = pos;
        this.radio = radio;
        this.texture = radio instanceof RadioTorchSenderBlockEntity ? textureSender : textureReceiver;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        int oX = 4;
        int oY = 4;
        int in = radio instanceof RadioTorchSenderBlockEntity ? 18 : 0;

        this.frequency = new EditBox(this.font, guiLeft + 25 + oX, guiTop + 17 + oY, 90 - oX * 2, 14, Component.empty());
        this.frequency.setTextColor(0x00ff00);
        this.frequency.setTextColorUneditable(0x00ff00);
        this.frequency.setBordered(false);
        this.frequency.setMaxLength(MAX_CHANNEL_LENGTH);
        this.frequency.setValue(radio.channel == null ? "" : radio.channel);
        addWidget(this.frequency);

        for (int i = 0; i < 16; i++) {
            this.remap[i] = new EditBox(this.font, guiLeft + 7 + (130 * (i / 8)) + oX + in, guiTop + 53 + (18 * (i % 8)) + oY, 90 - oX * 2, 14, Component.empty());
            this.remap[i].setTextColor(0x00ff00);
            this.remap[i].setTextColorUneditable(0x00ff00);
            this.remap[i].setBordered(false);
            this.remap[i].setMaxLength(32);
            this.remap[i].setValue(radio.mapping[i] == null ? "" : radio.mapping[i]);
            addWidget(this.remap[i]);
        }
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, f);

        if (radio.customMap) {
            g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
            g.blit(texture, guiLeft + 137, guiTop + 17, 0, 204, 18, 18);
            if (radio.polling) g.blit(texture, guiLeft + 173, guiTop + 17, 0, 222, 18, 18);
            for (int j = 0; j < 16; j++) this.remap[j].render(g, x, y, f);
        } else {
            g.blit(texture, guiLeft, guiTop, 0, 0, xSize, 35);
            g.blit(texture, guiLeft, guiTop + 35, 0, 197, xSize, 7);
            if (radio.polling) g.blit(texture, guiLeft + 173, guiTop + 17, 0, 222, 18, 18);
        }

        this.frequency.render(g, x, y, f);

        String name = this.title.getString();
        g.drawString(this.font, name, this.guiLeft + this.xSize / 2 - this.font.width(name) / 2, this.guiTop + 6, 4210752, false);

        if (inRect(x, y, 137)) g.renderComponentTooltip(this.font, List.of(Component.literal(radio.customMap ? "Custom Mapping" : "Redstone Passthrough")), x, y);
        if (inRect(x, y, 173)) g.renderComponentTooltip(this.font, List.of(Component.literal(radio.polling ? "Polling" : "State Change")), x, y);
        if (inRect(x, y, 209)) g.renderComponentTooltip(this.font, List.of(Component.literal("Save Settings")), x, y);
    }

    private boolean inRect(double x, double y, int left) {
        return guiLeft + left <= x && guiLeft + left + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y;
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean focusHit = false;
        this.frequency.setFocused(this.frequency.mouseClicked(x, y, i));
        focusHit |= this.frequency.isFocused();

        if (radio.customMap) {
            for (int j = 0; j < 16; j++) {
                this.remap[j].setFocused(this.remap[j].mouseClicked(x, y, i));
                focusHit |= this.remap[j].isFocused();
            }
        }

        if (inRect(x, y, 137)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("customMap", !radio.customMap);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (inRect(x, y, 173)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", !radio.polling);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (inRect(x, y, 209)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putString("channel", this.frequency.getValue());
            for (int j = 0; j < 16; j++) data.putString("mapping" + j, this.remap[j].getValue());
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        return focusHit || super.mouseClicked(x, y, i);
    }

    private void click() {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (this.frequency.charTyped(c, mods)) return true;
        if (radio.customMap) for (int j = 0; j < 16; j++) if (this.remap[j].charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != 256) {
            if (this.frequency.isFocused() && this.frequency.keyPressed(key, scan, mods)) return true;
            if (radio.customMap) for (int j = 0; j < 16; j++) if (this.remap[j].isFocused() && this.remap[j].keyPressed(key, scan, mods)) return true;
            if (this.frequency.isFocused()) return true;
            if (radio.customMap) for (int j = 0; j < 16; j++) if (this.remap[j].isFocused()) return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scan)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
