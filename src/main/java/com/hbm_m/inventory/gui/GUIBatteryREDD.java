package com.hbm_m.inventory.gui;

import java.math.BigInteger;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.BatteryREDDBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.BatteryREDDMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateBatteryC2SPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIBatteryREDD}: 176x181, Ladung und Aenderung pro Sekunde in halber Schriftgroesse, drei Schaltknoepfe. */
public class GUIBatteryREDD extends AbstractContainerScreen<BatteryREDDMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_battery_redd.png");

    public GUIBatteryREDD(BatteryREDDMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 181;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, pt);
        super.render(g, mouseX, mouseY, pt);
        this.renderTooltip(g, mouseX, mouseY);
    }

    private boolean checkClick(double x, double y, int left, int top, int w, int h) {
        return x >= leftPos + left && x < leftPos + left + w && y >= topPos + top && y < topPos + top + h;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        BatteryREDDBlockEntity battery = menu.battery;
        if (battery != null) {
            int id = -1;
            if (checkClick(x, y, 133, 16, 18, 18)) id = 0;
            if (checkClick(x, y, 133, 52, 18, 18)) id = 1;
            if (checkClick(x, y, 152, 35, 16, 16)) id = 2;
            if (id >= 0) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_BATTERY, new UpdateBatteryC2SPacket(battery.getBlockPos(), id));
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

        BatteryREDDBlockEntity battery = menu.battery;
        if (battery == null) return;

        g.pose().pushPose();
        g.pose().scale(0.5F, 0.5F, 1F);

        String label = String.format(Locale.US, "%,d", battery.power) + " HE";
        g.drawString(this.font, label, 242 - this.font.width(label), 45, 0x00ff00, false);

        String deltaText = String.format(Locale.US, "%,d", battery.delta) + " HE/s";
        int comp = battery.delta.compareTo(BigInteger.ZERO);
        Component delta;
        if (comp > 0) delta = Component.literal("+" + deltaText).withStyle(ChatFormatting.GREEN);
        else if (comp < 0) delta = Component.literal(deltaText).withStyle(ChatFormatting.RED);
        else delta = Component.literal("+" + deltaText).withStyle(ChatFormatting.YELLOW);
        g.drawString(this.font, delta, 242 - this.font.width(delta), 65, 0x00ff00, false);

        g.pose().popPose();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        BatteryREDDBlockEntity battery = menu.battery;
        if (battery == null) return;
        g.blit(TEXTURE, leftPos + 133, topPos + 16, 176, 52 + battery.redLow * 18, 18, 18);
        g.blit(TEXTURE, leftPos + 133, topPos + 52, 176, 52 + battery.redHigh * 18, 18, 18);
        g.blit(TEXTURE, leftPos + 152, topPos + 35, 194, 52 + battery.priority.ordinal() * 16 - 16, 16, 16);
    }
}
