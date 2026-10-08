package com.hbm_m.inventory.gui.radio;

import java.util.Random;

import com.hbm_m.blockentity.network.RadioTelexBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code GUIScreenRadioTelex}: fuenf Sendezeilen mit Sondertasten (Glocke, Drucken, Loeschen, Format, Pause),
 * Sende-/Empfangskanal, Senden/Puffer loeschen, Empfang drucken/loeschen und das Oszilloskop des gesendeten Zeichens.
 */
public class GUIRadioTelex extends RttyScreenBase {

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_telex.png");

    private final BlockPos pos;
    protected final RadioTelexBlockEntity telex;
    protected EditBox txFrequency;
    protected EditBox rxFrequency;
    protected boolean textFocus = false;

    protected final String[] txBuffer;
    protected int cursorPos = 0;

    public GUIRadioTelex(BlockPos pos, RadioTelexBlockEntity tile) {
        super(Component.empty(), 256, 244);
        this.pos = pos;
        this.telex = tile;
        this.txBuffer = new String[tile.txBuffer.length];

        for (int i = 0; i < txBuffer.length; i++) {
            this.txBuffer[i] = tile.txBuffer[i];
        }

        for (int i = 4; i > 0; i--) {
            if (!txBuffer[i].isEmpty()) {
                cursorPos = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.txFrequency = field(guiLeft + 29, guiTop + 110, 90, 14, 10, telex.txChannel);
        this.rxFrequency = field(guiLeft + 29, guiTop + 224, 90, 14, 10, telex.rxChannel);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);

        drawFields(g, mouseX, mouseY, f);

        for (int line = 0; line < 5; line++) {
            String text = txBuffer[line];
            int y = 11 + 14 * line;

            String format = ChatFormatting.RESET + "";

            for (int index = 0; index < text.length(); index++) {
                int x = 11 + 7 * index;
                char c = text.charAt(index);
                x += (7 - this.font.width(String.valueOf(c))) / 2;
                if (c == '§' && text.length() > index + 1) {
                    format = "§" + text.charAt(index + 1);
                    x -= 3;
                }
                String glyph = format + c;
                if (c == '\u0007') glyph = ChatFormatting.RED + "B";
                if (c == '\u000c') glyph = ChatFormatting.RED + "P";
                if (c == '\u007f') glyph = ChatFormatting.RED + "<";
                if (c == '\u0016') glyph = ChatFormatting.RED + "W";
                g.drawString(this.font, glyph, guiLeft + x, guiTop + y, 0x00ff00, false);
            }

            if (System.currentTimeMillis() % 1000 < 500 && this.textFocus) {
                int x = Math.max(11 + 7 * (text.length() - 1) + 7, 11);
                if (this.cursorPos == line) {
                    g.drawString(this.font, "|", guiLeft + x, guiTop + y, 0x00ff00, false);
                }
            }
        }

        for (int line = 0; line < 5; line++) {
            String text = telex.rxBuffer[line];
            int y = 145 + 14 * line;

            String format = ChatFormatting.RESET + "";

            int x = 11;

            for (int index = 0; index < text.length(); index++) {

                char c = text.charAt(index);
                x += (7 - this.font.width(String.valueOf(c))) / 2;
                if (c == '§' && text.length() > index + 1) {
                    format = "§" + text.charAt(index + 1);
                    c = ' ';
                } else if (c == '§') {
                    c = ' ';
                } else if (index > 0 && text.charAt(index - 1) == '§') {
                    c = ' ';
                    x -= 14;
                }
                String glyph = format + c;
                g.drawString(this.font, glyph, guiLeft + x, guiTop + y, 0x00ff00, false);
                x += 7;
            }
        }

        // Oszilloskop: gesendetes Zeichen als Zufallskurve (Original: GL_LINES, Breite 3)
        Random rand = new Random(telex.sendingChar);
        double offset = 0;
        for (int i = 0; i < 48; i++) {
            double y0 = guiTop + 93.5 + offset;
            if (telex.sendingChar != ' ' && i > 4 && i < 43) offset = rand.nextGaussian() * 7; else offset = 0;
            offset = Mth.clamp(offset, -7D, 7D);
            double y1 = guiTop + 93.5 + offset;
            int top = (int) Math.floor(Math.min(y0, y1)) - 1;
            int bottom = (int) Math.ceil(Math.max(y0, y1)) + 1;
            g.fill(guiLeft + 199 + i, top, guiLeft + 199 + i + 1, bottom, 0xFF00FF00);
        }

        if (in(mouseX, mouseY, 7, 85, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GOLD + "BELL", "Plays a bell when this character is received");
        if (in(mouseX, mouseY, 27, 85, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GOLD + "PRINT", "Forces recipient to print message after transmission ends");
        if (in(mouseX, mouseY, 47, 85, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GOLD + "CLEAR SCREEN", "Wipes message buffer when this character is received");
        if (in(mouseX, mouseY, 67, 85, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GOLD + "FORMAT", "Inserts format character for message formatting");
        if (in(mouseX, mouseY, 87, 85, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GOLD + "PAUSE", "Pauses message transmission for one second");

        if (in(mouseX, mouseY, 127, 105, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GREEN + "SAVE ID");
        if (in(mouseX, mouseY, 147, 105, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.YELLOW + "SEND MESSAGE");
        if (in(mouseX, mouseY, 167, 105, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.RED + "DELETE MESSAGE BUFFER");

        if (in(mouseX, mouseY, 127, 219, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.GREEN + "SAVE ID");
        if (in(mouseX, mouseY, 147, 219, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.AQUA + "PRINT MESSAGE");
        if (in(mouseX, mouseY, 167, 219, 18, 18)) tip(g, mouseX, mouseY, ChatFormatting.RED + "CLEAR SCREEN");
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        clickFields(x, y, i);

        this.textFocus = guiLeft + 7 <= x && guiLeft + 7 + 242 > x && guiTop + 7 < y && guiTop + 7 + 74 >= y;

        char character = '\0';
        String cmd = null;

        /* special characters */
        if (in(x, y, 7, 85, 18, 18)) character = '\u0007'; // bell
        if (in(x, y, 27, 85, 18, 18)) character = '\u000c'; // form feed
        if (in(x, y, 47, 85, 18, 18)) character = '\u007f'; // delete
        if (in(x, y, 67, 85, 18, 18)) character = '§'; // minecraft formatting character
        if (in(x, y, 87, 85, 18, 18)) character = '\u0016'; // synchronous idle

        if (in(x, y, 127, 105, 18, 18) || in(x, y, 127, 219, 18, 18)) cmd = "sve"; // save channel
        if (in(x, y, 147, 105, 18, 18)) cmd = "snd"; // send message in TX buffer
        if (in(x, y, 167, 105, 18, 18)) { // delete message in TX buffer
            cmd = "rxdel";
            for (int j = 0; j < 5; j++) this.txBuffer[j] = "";
            CompoundTag data = new CompoundTag();
            for (int j = 0; j < 5; j++) data.putString("tx" + j, this.txBuffer[j]);
            RadioTorchControlPacket.sendToServer(pos, data);
        }
        if (in(x, y, 147, 219, 18, 18)) cmd = "rxprt"; // print message in RX buffer
        if (in(x, y, 167, 219, 18, 18)) cmd = "rxcls"; // delete message in RX buffer

        if (cmd != null) {
            click();
            CompoundTag data = new CompoundTag();
            data.putString("cmd", cmd);

            if ("snd".equals(cmd)) {
                for (int j = 0; j < 5; j++) data.putString("tx" + j, this.txBuffer[j]);
            }

            if ("sve".equals(cmd)) {
                data.putString("txChan", this.txFrequency.getValue());
                data.putString("rxChan", this.rxFrequency.getValue());
            }

            RadioTorchControlPacket.sendToServer(pos, data);
        }

        if (character != '\0') {
            click();
            setTextFocus();
            submitChar(character);
        }
        return true;
    }

    protected void setTextFocus() {
        this.textFocus = true;
        this.txFrequency.setFocused(false);
        this.rxFrequency.setFocused(false);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (this.txFrequency.isFocused() || this.rxFrequency.isFocused()) return super.charTyped(c, mods);
        if (this.textFocus && SharedConstants.isAllowedChatCharacter(c)) {
            submitChar(c);
            return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.txFrequency.isFocused() || this.rxFrequency.isFocused()) return super.keyPressed(key, scan, mods);

        if (this.textFocus) {
            if (key == 256) {
                this.textFocus = false;
                return true;
            }
            if (key == 265) this.cursorPos--; // UP
            if (key == 264) this.cursorPos++; // DOWN
            this.cursorPos = Mth.clamp(cursorPos, 0, 4);

            if (key == 259 && this.txBuffer[cursorPos].length() > 0) { // BACKSPACE
                this.txBuffer[cursorPos] = this.txBuffer[cursorPos].substring(0, this.txBuffer[cursorPos].length() - 1);
            }
            if (this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scan)) return true;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    protected void submitChar(char c) {
        String line = this.txBuffer[cursorPos];

        if (line.length() < RadioTelexBlockEntity.lineWidth) {
            this.txBuffer[cursorPos] = line + c;
        }
    }

    @Override
    public void removed() {
        super.removed();
        CompoundTag data = new CompoundTag();
        for (int j = 0; j < 5; j++) data.putString("tx" + j, this.txBuffer[j]);
        RadioTorchControlPacket.sendToServer(pos, data);
    }
}
