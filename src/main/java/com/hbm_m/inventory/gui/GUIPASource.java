package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.albion.PASourceBlockEntity;
import com.hbm_m.blockentity.machines.albion.PAState;
import com.hbm_m.inventory.menu.PASourceMenu;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIPASource}: Lampen fuer Kuehlung (44, 16) und Energie (44, 41), Zustandsbalken (45, 73)
 * in der Zustandsfarbe, Zustandsname darueber, Info-Knopf (105, 16) und Abbrechen-Knopf (105, 28).
 */
public class GUIPASource extends GUIPABase<PASourceMenu> {

    public GUIPASource(PASourceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_source.png", 8, 134, 136);
    }

    private PASourceBlockEntity source() {
        return (PASourceBlockEntity) be();
    }

    private PAState state() {
        PAState[] states = PAState.values();
        int ordinal = menu.getStateOrdinal();
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : PAState.IDLE;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean handled = super.mouseClicked(x, y, button);
        if (leftPos + 105 <= x && leftPos + 105 + 10 > x && topPos + 28 < y && topPos + 28 + 10 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("cancel", true);
            NBTControlPacket.sendToServer(source().getBlockPos(), data);
            return true;
        }
        return handled;
    }

    @Override
    protected void renderIndicators(GuiGraphics g, int mouseX, int mouseY) {
        if (heatOk()) g.blit(texture, leftPos + 44, topPos + 16, 176, 8, 8, 8);
        if (be().getEnergyStored() >= PASourceBlockEntity.getUsage()) g.blit(texture, leftPos + 44, topPos + 41, 176, 8, 8, 8);

        // Original: glColor4f(rot, gruen, blau) mit Werten 0-255 - jeder Kanal > 0 wird voll (wie dort)
        int color = state().color;
        float red = Math.min(1F, (color & 0xff0000) >> 16);
        float green = Math.min(1F, (color & 0x00ff00) >> 8);
        float blue = Math.min(1F, color & 0x0000ff);
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        g.blit(texture, leftPos + 45, topPos + 73, 176, 52, 68, 14);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawTitle(g, -9, 4, 0xffffff);
        super.renderLabels(g, mouseX, mouseY);

        String state = Component.translatable("pa." + state().name().toLowerCase(Locale.US)).getString();
        g.drawString(this.font, state, 79 - this.font.width(state) / 2, 76, state().color, false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> info = new ArrayList<>();
        info.add(Component.literal(ChatFormatting.BLUE + "Last momentum: " + ChatFormatting.RESET + String.format(Locale.US, "%,d", menu.getLastSpeed())));
        for (Component line : resolveKeyArray("pa." + state().name().toLowerCase(Locale.US) + ".desc")) {
            info.add(Component.literal(ChatFormatting.YELLOW + line.getString()));
        }
        this.drawCustomInfoStat(g, mouseX, mouseY, 105, 16, 10, 10, mouseX, mouseY, info.toArray(new Component[0]));
        this.drawCustomInfoStat(g, mouseX, mouseY, 105, 28, 10, 10, mouseX, mouseY,
                Component.literal(ChatFormatting.RED + "Cancel operation"));
    }
}
