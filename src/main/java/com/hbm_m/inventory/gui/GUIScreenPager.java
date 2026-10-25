package com.hbm_m.inventory.gui;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.item.tool.ItemRTTYPager;
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

/** 1:1 {@code GUIScreenPager}: Kanal eingeben (max. 10 Zeichen, gruene Schrift), Knopf uebernimmt. */
public class GUIScreenPager extends Screen {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_rtty_pager.png");

    public ItemStack pager;
    public String startingChannel;
    protected int xSize = 184;
    protected int ySize = 42;
    protected int guiLeft;
    protected int guiTop;
    protected EditBox chan;

    public GUIScreenPager(ItemStack pager) {
        super(Component.translatable("container.rttyPager"));
        this.pager = pager;
        if (pager != null && StackNbt.has(pager)) {
            startingChannel = StackNbt.read(pager).getString(ItemRTTYPager.KEY_CHANNEL);
        }
        if (startingChannel == null) this.startingChannel = "";
    }

    public static void open(ItemStack pager) {
        Minecraft.getInstance().setScreen(new GUIScreenPager(pager));
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        int oX = 4;
        int oY = 4;

        chan = new EditBox(this.font, guiLeft + 27 + oX, guiTop + 19 + oY, 90 - oX * 2, 14, Component.empty());
        // GUIScreenRBMKKeyPad.setupTextFieldStandard(chan, 10, startingChannel)
        chan.setTextColor(0x00ff00);
        chan.setTextColorUneditable(0x00ff00);
        chan.setBordered(false);
        chan.setMaxLength(10);
        chan.setValue(startingChannel != null ? startingChannel : "");
        this.addWidget(chan);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);
        chan.render(g, mouseX, mouseY, f);
        Component name = Component.translatable("container.rttyPager");
        g.drawString(font, name, this.guiLeft + this.xSize / 2 - font.width(name) / 2, this.guiTop + 6, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double x, double y, int b) {
        if (guiLeft + 137 <= x && guiLeft + 137 + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putString(ItemRTTYPager.KEY_CHANNEL, this.chan.getValue());
            FluidIdentifierControlPacket.sendControl(data);
            return true;
        }
        return super.mouseClicked(x, y, b);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.chan.isFocused() && key != 256 && this.chan.keyPressed(key, scan, mods)) return true;
        if (key == 256 || (!this.chan.isFocused() && this.minecraft.options.keyInventory.matches(key, scan))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
