package com.hbm_m.inventory.gui;

import org.apache.commons.lang3.math.NumberUtils;

import com.hbm_m.blockentity.machines.albion.PADipoleBlockEntity;
import com.hbm_m.inventory.menu.PADipoleMenu;
import com.hbm_m.item.machine.ItemPACoil;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUIPADipole}: drei Richtungsschalter (62, 29/43/57) mit Kompassnadeln (blau = Blickrichtung
 * des Spielers, rot = Ausgangsrichtung), Schwellenfeld (47, 77) und Lampen fuer Energie (83, 54),
 * Kuehlung (93, 54) und Spule (103, 54).
 */
public class GUIPADipole extends GUIPABase<PADipoleMenu> {

    private EditBox threshold;

    public GUIPADipole(PADipoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_dipole.png", 8, 134, 136);
    }

    private PADipoleBlockEntity dipole() {
        return (PADipoleBlockEntity) be();
    }

    @Override
    protected void init() {
        super.init();
        // Original: GuiTextField(guiLeft + 47, guiTop + 77, 66, 8), gruen, ohne Hintergrund, max. 9 Zeichen
        threshold = new EditBox(this.font, leftPos + 47, topPos + 77, 66, 8, Component.empty());
        threshold.setTextColor(0x00ff00);
        threshold.setTextColorUneditable(0x00ff00);
        threshold.setBordered(false);
        threshold.setMaxLength(9);
        threshold.setValue("" + dipole().getThreshold());
        threshold.setResponder(text -> {
            if (text.startsWith("0") && text.length() > 1) { threshold.setValue(text.substring(1)); return; }
            if (text.isEmpty()) { threshold.setValue("0"); return; }
            if (NumberUtils.isDigits(text)) {
                CompoundTag data = new CompoundTag();
                data.putInt("threshold", NumberUtils.toInt(text));
                NBTControlPacket.sendToServer(dipole().getBlockPos(), data);
            }
        });
        addRenderableWidget(threshold);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (threshold != null && threshold.isFocused() && keyCode != 256) {
            return threshold.keyPressed(keyCode, scanCode, modifiers) || threshold.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean handled = super.mouseClicked(x, y, button);
        String[] keys = { "lower", "upper", "redstone" };
        for (int i = 0; i < 3; i++) {
            int by = 29 + i * 14;
            if (leftPos + 62 <= x && leftPos + 62 + 12 > x && topPos + by < y && topPos + by + 12 >= y) {
                playClickSound();
                CompoundTag data = new CompoundTag();
                data.putBoolean(keys[i], true);
                NBTControlPacket.sendToServer(dipole().getBlockPos(), data);
                return true;
            }
        }
        return handled;
    }

    /** Original {@code dirLower} usw. als Zahl 0-3 (N, O, S, W). */
    private static int dirIndex(Direction d) {
        return switch (d) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    @Override
    protected void renderIndicators(GuiGraphics g, int mouseX, int mouseY) {
        if (heatOk()) g.blit(texture, leftPos + 93, topPos + 54, 176, 8, 8, 8);
        ItemStack coil = be().getInventory().getStackInSlot(PADipoleBlockEntity.SLOT_COIL);
        if (!coil.isEmpty() && coil.getItem() instanceof ItemPACoil) g.blit(texture, leftPos + 103, topPos + 54, 176, 8, 8, 8);
        if (be().getEnergyStored() >= PADipoleBlockEntity.getUsage()) g.blit(texture, leftPos + 83, topPos + 54, 176, 8, 8, 8);

        // Original: Kompassnadeln, 6 Pixel lang, 3 Pixel breit
        float yaw = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 0F;
        addLine(g, 68, 35, 0xff8080ff, 180);
        addLine(g, 68, 35, 0xffff0000, yaw - dirIndex(dipole().getDirLower()) * 90);
        addLine(g, 68, 49, 0xff8080ff, 180);
        addLine(g, 68, 49, 0xffff0000, yaw - dirIndex(dipole().getDirUpper()) * 90);
        addLine(g, 68, 63, 0xff8080ff, 180);
        addLine(g, 68, 63, 0xffff0000, yaw - dirIndex(dipole().getDirRedstone()) * 90);
    }

    /** Original {@code addLine}: Vektor (0, 6) um die Z-Achse gedreht, ab (x, y). */
    private void addLine(GuiGraphics g, int x, int y, int color, float yaw) {
        g.pose().pushPose();
        g.pose().translate(leftPos + x, topPos + y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(yaw));
        g.fill(-1, 0, 2, 6, color);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawTitle(g, -9, 6, 4210752);
        super.renderLabels(g, mouseX, mouseY);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Direction[] dirs = { dipole().getDirLower(), dipole().getDirUpper(), dipole().getDirRedstone() };
        for (int i = 0; i < 3; i++) {
            this.drawCustomInfoStat(g, mouseX, mouseY, 62, 29 + i * 14, 12, 12, mouseX, mouseY,
                    Component.literal(ChatFormatting.BLUE + "Player orientation"),
                    Component.literal(ChatFormatting.RED + "Output orientation:"),
                    Component.literal(dirs[i].name()));
        }
    }
}
