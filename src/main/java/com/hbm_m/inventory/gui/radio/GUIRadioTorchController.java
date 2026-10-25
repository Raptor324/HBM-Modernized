package com.hbm_m.inventory.gui.radio;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.redstoneoverradio.IRORInfo;
import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
import com.hbm_m.blockentity.network.radio.RadioTorchControllerBlockEntity;
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

/** 1:1 {@code GUIScreenRadioTorchController}: Kanal, Funktionsliste (137,17), Abfrage, Speichern. */
public class GUIRadioTorchController extends RttyScreenBase {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_controller.png");

    private final BlockPos pos;
    protected final RadioTorchControllerBlockEntity rtty;
    protected EditBox frequency;

    public GUIRadioTorchController(BlockPos pos, RadioTorchControllerBlockEntity rtty) {
        super(Component.translatable("container.rttyController"), 256, 42);
        this.pos = pos;
        this.rtty = rtty;
    }

    @Override
    protected void init() {
        super.init();
        int oX = 4;
        int oY = 4;
        this.frequency = field(guiLeft + 25 + oX, guiTop + 17 + oY, 90 - oX * 2, 14, GUIRadioTorchSimple.MAX_CHANNEL_LENGTH, rtty.channel);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, f);

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
        if (rtty.polling) g.blit(texture, guiLeft + 173, guiTop + 17, 0, 42, 18, 18);

        drawFields(g, x, y, f);

        String name = this.title.getString();
        g.drawString(this.font, name, this.guiLeft + this.xSize / 2 - this.font.width(name) / 2, this.guiTop + 6, 4210752, false);

        if (in(x, y, 173, 17, 18, 18)) tip(g, x, y, rtty.polling ? "Polling" : "State Change");
        if (in(x, y, 209, 17, 18, 18)) tip(g, x, y, "Save Settings");
        if (in(x, y, 137, 17, 18, 18) && rtty.getLevel() != null) {
            Direction dir = rtty.getBlockState().hasProperty(RadioTorchBaseBlock.FACING) ? rtty.getBlockState().getValue(RadioTorchBaseBlock.FACING) : Direction.DOWN;
            if (IRORInfo.resolve(rtty.getLevel(), rtty.getBlockPos().relative(dir)) instanceof IRORInfo prov) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("Usable functions:"));
                for (String s : prov.getFunctionInfo()) {
                    if (s.startsWith(IRORInfo.PREFIX_FUNCTION))
                        lines.add(Component.literal(s.substring(4)).withStyle(ChatFormatting.AQUA));
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
            data.putString("channel", this.frequency.getValue());
            RadioTorchControlPacket.sendToServer(pos, data);
            return true;
        }

        return hit || super.mouseClicked(x, y, i);
    }
}
