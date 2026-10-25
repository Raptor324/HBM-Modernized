package com.hbm_m.inventory.gui.radio;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
import com.hbm_m.blockentity.network.radio.RadioTorchReaderBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RadioTorchControlPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code GUIScreenRadioTorchReader}: acht Zeilen Kanal + Wertname, Abfrage, Speichern; das Fragezeichen (29,17)
 * listet die lesbaren Werte des Blocks, an dem der Leser haengt.
 */
public class GUIRadioTorchReader extends RttyScreenBase {

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_reader.png");

    private final BlockPos pos;
    public final RadioTorchReaderBlockEntity rtty;
    protected final EditBox[] frequencies = new EditBox[8];
    protected final EditBox[] names = new EditBox[8];

    public GUIRadioTorchReader(BlockPos pos, RadioTorchReaderBlockEntity rtty) {
        super(Component.translatable("container.rttyReader"), 256, 204);
        this.pos = pos;
        this.rtty = rtty;
    }

    @Override
    protected void init() {
        super.init();

        int oX = 4;
        int oY = 4;

        for (int i = 0; i < 8; i++) {
            this.frequencies[i] = field(guiLeft + 25 + oX, guiTop + 53 + i * 18 + oY, 72 - oX * 2, 14, GUIRadioTorchSimple.MAX_CHANNEL_LENGTH, rtty.channels[i]);
            this.names[i] = field(guiLeft + 119 + oX, guiTop + 53 + i * 18 + oY, 126 - oX * 2, 14, 25, rtty.names[i]);
        }
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, f);

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
        if (rtty.polling) g.blit(texture, guiLeft + 173, guiTop + 17, 0, 204, 18, 18);

        drawFields(g, x, y, f);

        String name = this.title.getString();
        g.drawString(this.font, name, this.guiLeft + this.xSize / 2 - this.font.width(name) / 2, this.guiTop + 6, 4210752, false);

        if (in(x, y, 173, 17, 18, 18)) tip(g, x, y, rtty.polling ? "Polling" : "State Change");
        if (in(x, y, 209, 17, 18, 18)) tip(g, x, y, "Save Settings");
        if (in(x, y, 29, 17, 18, 18) && rtty.getLevel() != null) {
            Direction dir = rtty.getBlockState().hasProperty(RadioTorchBaseBlock.FACING) ? rtty.getBlockState().getValue(RadioTorchBaseBlock.FACING) : Direction.DOWN;
            if (com.hbm_m.api.redstoneoverradio.IRORInfo.resolve(rtty.getLevel(), rtty.getBlockPos().relative(dir)) instanceof IRORValueProvider prov) {
                String[] info = prov.getFunctionInfo();
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("Readable values:"));
                for (String s : info) {
                    if (s.startsWith(IRORValueProvider.PREFIX_VALUE))
                        lines.add(Component.literal(s.substring(4)).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                g.renderComponentTooltip(this.font, lines, x, y);
            }
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean hit = clickFields(x, y, i);

        if (in(x, y, 173, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", !rtty.polling);
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        if (in(x, y, 209, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            for (int j = 0; j < 8; j++) data.putString("channel" + j, this.frequencies[j].getValue());
            for (int j = 0; j < 8; j++) data.putString("name" + j, this.names[j].getValue());
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        return hit || super.mouseClicked(x, y, i);
    }
}
