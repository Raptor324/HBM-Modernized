package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.machines.RadioRecBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code GUIRadioRec}: Kanal (max. 10 Zeichen), Speichern (137,17), Ein/Aus (173,17). */
public class GUIRadioRec extends RttyScreenBase {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_radio.png");

    private final BlockPos pos;
    protected final RadioRecBlockEntity radio;
    protected EditBox frequency;

    public GUIRadioRec(BlockPos pos, RadioRecBlockEntity radio) {
        super(Component.translatable("container.radiorec"), 220, 42);
        this.pos = pos;
        this.radio = radio;
    }

    @Override
    protected void init() {
        super.init();
        int oX = 4;
        int oY = 4;
        this.frequency = field(guiLeft + 25 + oX, guiTop + 17 + oY, 90 - oX * 2, 14, 10, radio.channel);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, f);

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
        if (this.radio.isOn) g.blit(texture, guiLeft + 173, guiTop + 17, 0, 42, 18, 18);

        drawFields(g, x, y, f);

        String name = this.title.getString();
        g.drawString(this.font, name, this.guiLeft + this.xSize / 2 - this.font.width(name) / 2, this.guiTop + 6, 4210752, false);

        if (in(x, y, 137, 17, 18, 18)) tip(g, x, y, "Save Settings");
        if (in(x, y, 173, 17, 18, 18)) tip(g, x, y, "Toggle");
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean hit = clickFields(x, y, i);

        if (in(x, y, 137, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putString("channel", this.frequency.getValue());
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (in(x, y, 173, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("isOn", !radio.isOn);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        return hit || super.mouseClicked(x, y, i);
    }
}
