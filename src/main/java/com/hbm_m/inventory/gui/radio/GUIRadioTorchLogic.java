package com.hbm_m.inventory.gui.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchLogicBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code GUIScreenRadioTorchLogic}: Kanal, Reihenfolge, Abfrage, Speichern; 16 Regeln mit Bedingungssymbol
 * (Klick oder Mausrad schaltet 0-9 weiter) und Vergleichswert. Bedingungen werden erst mit "Speichern" uebernommen.
 */
public class GUIRadioTorchLogic extends RttyScreenBase {

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_logic_receiver.png");

    private final BlockPos pos;
    protected final RadioTorchLogicBlockEntity logic;
    protected EditBox frequency;
    protected final EditBox[] map = new EditBox[16];
    protected final int[] conditions = new int[16];

    public GUIRadioTorchLogic(BlockPos pos, RadioTorchLogicBlockEntity logic) {
        super(Component.translatable("container.rttyLogic"), 256, 204);
        this.pos = pos;
        this.logic = logic;
    }

    @Override
    protected void init() {
        super.init();

        int oX = 4;
        int oY = 4;

        this.frequency = field(guiLeft + 25 + oX, guiTop + 17 + oY, 90 - oX * 2, 14, GUIRadioTorchSimple.MAX_CHANNEL_LENGTH, logic.channel);

        for (int i = 0; i < 16; i++) {
            this.map[i] = field(guiLeft + 7 + (130 * (i / 8)) + oX + 18, guiTop + 53 + (18 * (i % 8)) + oY, 54 - oX * 2, 14, 15, logic.mapping[i]);
            this.conditions[i] = logic.conditions[i];
        }
    }

    private boolean inCond(double x, double y, int j) {
        return guiLeft + 7 + (130 * (j / 8)) <= x && guiLeft + 7 + 18 + (130 * (j / 8)) > x
                && guiTop + 53 + (18 * (j % 8)) <= y && guiTop + 53 + 18 + (18 * (j % 8)) > y;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, f);

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
        if (logic.descending) g.blit(texture, guiLeft + 137, guiTop + 17, 0, 204, 18, 18);
        if (logic.polling) g.blit(texture, guiLeft + 173, guiTop + 17, 0, 222, 18, 18);

        for (int i = 0; i < 16; i++) {
            if (logic.mapping[i] == null || logic.mapping[i].isEmpty()) {
                if (this.conditions[i] != 0)
                    g.blit(texture, guiLeft + 7 + (130 * (i / 8)), guiTop + 53 + (18 * (i % 8)), 18 + this.conditions[i] * 18, 222, 18, 18);
            } else {
                g.blit(texture, guiLeft + 7 + (130 * (i / 8)), guiTop + 53 + (18 * (i % 8)), 18 + this.conditions[i] * 18, 204, 18, 18);
                g.blit(texture, guiLeft + 85 + (130 * (i / 8)), guiTop + 57 + (18 * (i % 8)), 198, 204, 14, 10);
            }
        }

        drawFields(g, x, y, f);

        String name = this.title.getString();
        g.drawString(this.font, name, this.guiLeft + this.xSize / 2 - this.font.width(name) / 2, this.guiTop + 6, 4210752, false);

        if (in(x, y, 137, 17, 18, 18)) tip(g, x, y, logic.descending ? "Descending Order" : "Ascending Order");
        if (in(x, y, 173, 17, 18, 18)) tip(g, x, y, logic.polling ? "Polling" : "State Change");
        if (in(x, y, 209, 17, 18, 18)) tip(g, x, y, "Save Settings");
        for (int j = 0; j < 16; j++) {
            if (inCond(x, y, j)) {
                tip(g, x, y, Component.translatable("desc.gui.rttyLogic.cond" + this.conditions[j]));
                break;
            }
        }
    }

    /** Original "easy selection": Mausrad ueber einem Bedingungssymbol schaltet vor/zurueck. */
    @Override
    //? if < 1.21.1 {
    public boolean mouseScrolled(double x, double y, double scroll) {
    //?} else {
    /*public boolean mouseScrolled(double x, double y, double scrollX, double scroll) {
    *///?}
        for (int j = 0; j < 16; j++) {
            if (inCond(x, y, j)) {
                if (scroll > 0) this.conditions[j] = (this.conditions[j] + 1) % 10;
                if (scroll < 0) this.conditions[j] = (this.conditions[j] + 9) % 10;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean hit = clickFields(x, y, i);

        if (in(x, y, 137, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("descending", !logic.descending);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (in(x, y, 173, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", !logic.polling);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (in(x, y, 209, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putString("channel", this.frequency.getValue());
            for (int j = 0; j < 16; j++) data.putString("mapping" + j, this.map[j].getValue());
            for (int j = 0; j < 16; j++) data.putInt("cond" + j, this.conditions[j]);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        for (int j = 0; j < 16; j++) {
            if (inCond(x, y, j)) {
                click();
                this.conditions[j] = (this.conditions[j] + 1) % 10;
                return true;
            }
        }

        return hit || super.mouseClicked(x, y, i);
    }
}
