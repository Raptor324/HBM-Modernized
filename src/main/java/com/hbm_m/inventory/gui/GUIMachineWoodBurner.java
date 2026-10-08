package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineWoodBurnerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * 1:1 {@code GUIMachineWoodBurner} (176x186): Ein/Aus (53/17), Umschalter fest/fluessig (46/37),
 * Brennzeitbalken (17/18) bzw. im Fluessigbetrieb Holzoeltank (80/18), Energie (143/18, 34 hoch),
 * Brennwert-Tooltip ueber dem leeren Brennstoffslot.
 */
public class GUIMachineWoodBurner extends GuiInfoScreen<MachineWoodBurnerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_wood_burner_alt.png");

    public GUIMachineWoodBurner(MachineWoodBurnerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    private MachineWoodBurnerBlockEntity burner() {
        return menu.blockEntity;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, gui, mouseX, mouseY, delta);
        super.render(gui, mouseX, mouseY, delta);

        MachineWoodBurnerBlockEntity burner = burner();
        // тайл может отсутствовать в реплее Flashback
        if (burner != null) {
            drawElectricityInfo(gui, mouseX, mouseY, 143, 18, 16, 34, menu.getEnergyLong(), menu.getMaxEnergyLong());

            // Original: Brennwert-Boni ueber dem leeren Brennstoffslot (Menue-Slot 0)
            if (menu.getCarried().isEmpty()) {
                Slot slot = menu.slots.get(0);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && !slot.hasItem()) {
                    List<Component> bonuses = MachineWoodBurnerBlockEntity.burnModule.getDesc();
                    if (!bonuses.isEmpty()) gui.renderComponentTooltip(this.font, bonuses, mouseX, mouseY);
                }
            }

            if (burner.liquidBurn) burner.tank.renderTankInfo(gui, this.font, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);

            if (!burner.liquidBurn && inRect(mouseX, mouseY, 16, 17, 8, 54)) {
                gui.renderComponentTooltip(this.font, List.of(Component.literal((menu.getBurnTime() / 20) + "s")), mouseX, mouseY);
            }

            if (inRect(mouseX, mouseY, 53, 17, 16, 15)) {
                gui.renderComponentTooltip(this.font, List.of(menu.isEnabled()
                        ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                        : Component.literal("OFF").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            }
        }

        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean result = super.mouseClicked(x, y, button);
        MachineWoodBurnerBlockEntity burner = burner();
        if (burner == null) return result;

        if (inRect(x, y, 53, 17, 16, 15)) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", false);
            NBTControlPacket.sendToServer(burner.getBlockPos(), data);
            return true;
        }

        if (inRect(x, y, 46, 37, 30, 14)) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("switch", false);
            NBTControlPacket.sendToServer(burner.getBlockPos(), data);
            return true;
        }
        return result;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        gui.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        MachineWoodBurnerBlockEntity burner = burner();
        if (burner == null) return;

        if (burner.liquidBurn) {
            gui.blit(TEXTURE, leftPos + 16, topPos + 17, 176, 52, 60, 54);
            gui.blit(TEXTURE, leftPos + 79, topPos + 17, 176, 106, 36, 54);
        }

        if (menu.isEnabled()) {
            gui.blit(TEXTURE, leftPos + 53, topPos + 17, 196, 0, 16, 15);
        }

        long max = menu.getMaxEnergyLong();
        int p = max > 0 ? (int) (menu.getEnergyLong() * 34 / max) : 0;
        if (p > 0) gui.blit(TEXTURE, leftPos + 143, topPos + 52 - p, 176, 52 - p, 16, p);

        if (menu.getMaxBurnTime() > 0 && !burner.liquidBurn) {
            int b = menu.getBurnTime() * 52 / menu.getMaxBurnTime();
            if (b > 0) gui.blit(TEXTURE, leftPos + 17, topPos + 70 - b, 192, 52 - b, 4, b);
        }

        if (burner.liquidBurn) burner.tank.renderTank(gui, leftPos + 80, topPos + 18, 16, 52);
    }

    /** Original: {@code guiLeft + x <= mx && guiLeft + x + w > mx && guiTop + y < my && guiTop + y + h >= my}. */
    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return leftPos + x <= mx && leftPos + x + w > mx && topPos + y < my && topPos + y + h >= my;
    }
}
