package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineTurbofanMenu;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * 1:1-порт {@code GUIMachineTurbofan} (1.7.10): 176x203, энергетический бар
 * (UV 192, снизу вверх), иконки форсажа над слотом улучшений (атлас x=176),
 * круглый датчик крови при showBlood (13 кадров), бак отрисовывается
 * {@code FluidTank.renderTank} (текстура жидкости, тайлинг 16x16 - как в оригинале).
 */
@OnlyIn(Dist.CLIENT)
public class GUIMachineTurbofan extends AbstractContainerScreen<MachineTurbofanMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "textures/gui/generators/gui_turbofan.png");
    private static final ResourceLocation SMALL_ROUND =
            ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "textures/gui/gauges/small_round.png");

    private static final int TANK_X = 35;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 34;
    private static final int TANK_H = 52;

    private static final int POWER_X = 143;
    private static final int POWER_BOTTOM = 69;
    private static final int POWER_H = 52;

    private final MachineTurbofanBlockEntity turbofan;

    public GUIMachineTurbofan(MachineTurbofanMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.turbofan = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        // тайл может отсутствовать в реплее Flashback
        if (turbofan == null) return;

        // Энергетический бар: 1:1-оригинальные UV (176+16, 52-i).
        long maxPower = turbofan.getMaxEnergyStored();
        int power = maxPower > 0 ? (int) (turbofan.getEnergyStored() * POWER_H / maxPower) : 0;
        power = Mth.clamp(power, 0, POWER_H);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, x + POWER_X, y + POWER_BOTTOM - power, 192, POWER_H - power, 16, power, 256, 256);
        }

        // Иконки форсажа: кадр min(afterburner, 6) - 1 из атласа (176, кадр*16).
        if (turbofan.getAfterburner() > 0) {
            int frame = Math.min(turbofan.getAfterburner(), 6) - 1;
            guiGraphics.blit(TEXTURE, x + 98, y + 44, 176, frame * 16, 16, 16, 256, 256);
        }

        // Датчик крови: 13 кадров 18x18, показывается после первой добытой крови.
        if (turbofan.isShowingBlood()) {
            var blood = turbofan.getBloodTank();
            int frame = blood.getMaxFill() <= 0 ? 0
                    : Mth.clamp((int) Math.round(12.0D * blood.getFill() / blood.getMaxFill()), 0, 12);
            guiGraphics.blit(SMALL_ROUND, x + 97, y + 16, 0, frame * 18, 18, 18, 18, 234);
        }

        // Бак керосина: 1:1 renderTank (текстура жидкости, тайлинг 16x16).
        turbofan.getTank().renderTank(guiGraphics, x + TANK_X, y + TANK_Y, TANK_W, TANK_H);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 43 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // тайл может отсутствовать в реплее Flashback
        if (turbofan != null) {
            // Тултипы баков рисует сам FluidTank (оригинальный renderTankInfo).
            turbofan.getTank().renderTankInfo(guiGraphics, font, mouseX, mouseY,
                    leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H);
            if (turbofan.isShowingBlood() && isPointInRect(97, 16, 18, 18, mouseX, mouseY)) {
                var blood = turbofan.getBloodTank();
                guiGraphics.renderTooltip(font, Component.literal(blood.getFill() + " / " + blood.getMaxFill() + " mB"), mouseX, mouseY);
            }
            if (isPointInRect(POWER_X, 17, 16, POWER_H, mouseX, mouseY)) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.hbm_m.energy",
                        EnergyFormatter.format(turbofan.getEnergyStored()), EnergyFormatter.format(turbofan.getMaxEnergyStored())), mouseX, mouseY);
            }
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private boolean isPointInRect(int relX, int relY, int width, int height, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        return x >= relX && x < relX + width && y >= relY && y < relY + height;
    }
}
