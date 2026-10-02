package com.hbm_m.inventory.gui;

import com.hbm_m.item.satellite.ItemSatInterface;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.FluidIdentifierControlPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUIScreenSatCoord}: drei Felder X/Y/Z; nur mit Verbindung ("connected") bedienbar, der Knopf bei
 * (133,52) schickt die Koordinaten (Y darf leer sein) und schliesst das Fenster.
 */
public class GUIScreenSatCoord extends Screen {

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/satellites/gui_sat_coord.png");
    protected int xSize = 176;
    protected int ySize = 126;
    protected int guiLeft;
    protected int guiTop;
    protected final ItemStack device;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public GUIScreenSatCoord(ItemStack device) {
        super(Component.empty());
        this.device = device;
    }

    public static void open(ItemStack device) {
        Minecraft.getInstance().setScreen(new GUIScreenSatCoord(device));
    }

    private boolean connected() {
        return device.hasTag() && device.getTag().getBoolean(ItemSatInterface.KEY_NBT_CONNECTED);
    }

    private EditBox field(int y) {
        EditBox f = new EditBox(this.font, guiLeft + 66, guiTop + y, 48, 12, Component.empty());
        f.setTextColor(-1);
        f.setTextColorUneditable(-1);
        f.setBordered(false);
        f.setMaxLength(7);
        this.addWidget(f);
        return f;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.xField = field(21);
        this.yField = field(56);
        this.zField = field(92);
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return !s.isEmpty();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!connected()) return false;

        boolean handled = super.mouseClicked(mx, my, button);
        xField.setFocused(xField.isMouseOver(mx, my));
        yField.setFocused(yField.isMouseOver(mx, my));
        zField.setFocused(zField.isMouseOver(mx, my));

        if (mx >= this.guiLeft + 133 && mx < this.guiLeft + 133 + 18 && my >= this.guiTop + 52 && my < this.guiTop + 52 + 18) {
            if (isNumber(xField.getValue()) && isNumber(zField.getValue())) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                CompoundTag data = new CompoundTag();
                data.putInt("x", (int) Double.parseDouble(xField.getValue()));
                data.putInt("z", (int) Double.parseDouble(zField.getValue()));
                if (isNumber(yField.getValue())) data.putInt("y", (int) Double.parseDouble(yField.getValue()));
                FluidIdentifierControlPacket.sendControl(data);
                this.onClose();
                return true;
            }
        }
        return handled;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        this.renderBackground(g);

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
        if (xField.isFocused()) g.blit(texture, guiLeft + 61, guiTop + 16, 0, 126, 54, 18);
        if (yField.isFocused()) g.blit(texture, guiLeft + 61, guiTop + 52, 0, 126, 54, 18);
        if (zField.isFocused()) g.blit(texture, guiLeft + 61, guiTop + 88, 0, 126, 54, 18);

        if (connected()) {
            g.blit(texture, guiLeft + 120, guiTop + 17, 194, 0, 7, 7);
            g.blit(texture, guiLeft + 120, guiTop + 25, 194, 0, 7, 7);
        }

        xField.render(g, mouseX, mouseY, f);
        yField.render(g, mouseX, mouseY, f);
        zField.render(g, mouseX, mouseY, f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
