package com.hbm_m.inventory.gui;

import com.hbm_m.main.MainRegistry;
import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.NukeFstbmbMenu;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUINukeFstbmb} (176x222): Ei-/Batterie-Overlays, Startfeld (142,35) solange nicht scharf,
 * Timer-Eingabefeld in Sekunden (94,40; 1-999) und rote Restzeitanzeige (nur mit Batterie).
 */
public class GUINukeFstbmb extends GuiInfoScreen<NukeFstbmbMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/weapon/fstbmb_schematic.png");

    private final NukeFstbmbBlockEntity be;
    private EditBox timer;

    public GUINukeFstbmb(NukeFstbmbMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.timer = new EditBox(this.font, this.leftPos + 94, this.topPos + 40, 29, 12, Component.empty());
        this.timer.setTextColor(0xff0000);
        this.timer.setTextColorUneditable(0x800000);
        this.timer.setBordered(false);
        this.timer.setMaxLength(3);
        this.timer.setValue(String.valueOf(be != null ? be.timer / 20 : 0));
        this.timer.setResponder(text -> {
            if (be == null) return;
            try {
                int j = Mth.clamp((int) Double.parseDouble(text), 1, 999);
                send(j, 1);
            } catch (NumberFormatException ignored) { }
        });
        addRenderableWidget(this.timer);
    }

    private void send(int value, int meta) {
        CompoundTag data = new CompoundTag();
        data.putInt("value", value);
        data.putInt("meta", meta);
        NBTControlPacket.sendToServer(be.getBlockPos(), data);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean result = super.mouseClicked(x, y, button);

        if (be != null && !be.started) {
            if (leftPos + 142 <= x && leftPos + 142 + 18 > x && topPos + 35 < y && topPos + 35 + 18 >= y) {
                send(0, 0);
            }
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.timer != null && this.timer.isFocused() && keyCode != 256) {
            this.timer.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        if (be == null) return; // тайл может отсутствовать в реплее Flashback

        if (be.hasEgg())
            guiGraphics.blit(TEXTURE, leftPos + 19, topPos + 90, 176, 0, 30, 16);

        int battery = be.getBattery();

        if (battery == 1)
            guiGraphics.blit(TEXTURE, leftPos + 88, topPos + 93, 176, 16, 18, 10);
        else if (battery == 2)
            guiGraphics.blit(TEXTURE, leftPos + 88, topPos + 93, 194, 16, 18, 10);

        if (be.started)
            guiGraphics.blit(TEXTURE, leftPos + 142, topPos + 35, 176, 26, 18, 18);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

        if (be != null && be.hasBattery()) {
            String timer = be.getMinutes() + ":" + be.getSeconds();
            float scale = 0.75F;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.drawString(this.font, timer, (int) ((69 - this.font.width(timer) / 2) * (1 / scale)), (int) (95.5 * (1 / scale)), 0xff0000, false);
            guiGraphics.pose().popPose();
        }
    }
}
