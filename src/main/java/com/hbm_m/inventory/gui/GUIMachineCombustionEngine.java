package com.hbm_m.inventory.gui;

import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.menu.MachineCombustionEngineMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUICombustionEngine}: Zuendknopf, Drosselregler (bei gehaltener Maus folgt er der Maus), Leistungsanzeige
 * mit Kolbensymbol, Tank und Energiebalken.
 */
public class GUIMachineCombustionEngine extends GuiInfoScreen<MachineCombustionEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_combustion.png");

    private final MachineCombustionEngineBlockEntity engine;
    private int setting = 0;
    private boolean isMouseLocked = false;

    public GUIMachineCombustionEngine(MachineCombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.engine = menu.getBlockEntity();
        if (engine != null) this.setting = engine.setting;
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);
        if (engine == null) return; // тайл может отсутствовать в реплее Flashback

        if (!isMouseLocked) {
            drawElectricityInfo(g, x, y, 143, 17, 16, 52, engine.getEnergyStored(), MachineCombustionEngineBlockEntity.maxPower);
            engine.tank.renderTankInfo(g, this.font, x, y, leftPos + 35, topPos + 17, 16, 52);
        }

        if (isMouseLocked || (leftPos + 80 <= x && leftPos + 80 + 34 > x && topPos + 38 < y && topPos + 38 + 8 >= y)) {
            g.renderTooltip(font, Component.literal(((setting * 2) / 10D) + "mB/t"),
                    Mth.clamp(x, leftPos + 80, leftPos + 114), Mth.clamp(y, topPos + 38, topPos + 46));
        }

        ItemStack piston = engine.getInventory().getStackInSlot(MachineCombustionEngineBlockEntity.SLOT_PISTON);
        if (MachineCombustionEngineBlockEntity.pistonType(piston.getItem()) >= 0) {
            double power = 0;
            FT_Combustible trait = FluidType.getTrait(engine.tank.getTankType(), FT_Combustible.class);
            if (trait != null) {
                power = setting * 0.2 * trait.getCombustionEnergy() / 1_000D * MachineCombustionEngineBlockEntity.efficiency(piston, engine.tank.getTankType());
            }
            drawCustomInfoStat(g, x, y, 79, 50, 35, 14, x, y,
                    Component.literal(String.format(Locale.US, "%,d", (int) (power)) + " HE/t").withStyle(ChatFormatting.YELLOW),
                    Component.literal(String.format(Locale.US, "%,d", (int) (power * 20)) + " HE/s").withStyle(ChatFormatting.YELLOW));
        }

        drawCustomInfoStat(g, x, y, 79, 13, 35, 15, x, y, Component.literal("Ignition"));

        if (isMouseLocked) {

            int setting = (x - leftPos - 81) * 30 / 32;
            setting = Mth.clamp(setting, 0, 30);

            if (this.setting != setting) {
                this.setting = setting;
                com.hbm_m.network.CombustionEngineControlC2SPacket.sendThrottle(engine.getBlockPos(), setting);
            }
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (engine != null) {
            int x = (int) mx, y = (int) my;
            if (leftPos + 89 <= x && leftPos + 89 + 16 > x && topPos + 13 < y && topPos + 13 + 14 >= y) {
                playClickSound();
                com.hbm_m.network.CombustionEngineControlC2SPacket.sendToggle(engine.getBlockPos());
            }

            if (leftPos + 79 <= x && leftPos + 79 + 36 > x && topPos + 38 < y && topPos + 38 + 8 >= y) {
                playClickSound();
                isMouseLocked = true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (isMouseLocked && (button == 0 || button == 1)) {
            isMouseLocked = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (engine == null) return;

        ItemStack piston = engine.getInventory().getStackInSlot(MachineCombustionEngineBlockEntity.SLOT_PISTON);
        int i = MachineCombustionEngineBlockEntity.pistonType(piston.getItem());
        if (i >= 0) {
            g.blit(TEXTURE, leftPos + 80, topPos + 51, 176, 52 + i * 12, 25, 12);
        }

        g.blit(TEXTURE, leftPos + 79 + (setting * 32 / 30), topPos + 38, 192, 15, 4, 8);

        if (engine.isOn) {
            g.blit(TEXTURE, leftPos + 79, topPos + 13, 192, 0, 35, 15);
        }

        int p = (int) (engine.getEnergyStored() * 53 / MachineCombustionEngineBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 143, topPos + 69 - p, 176, 52 - p, 16, p);

        engine.tank.renderTank(g, leftPos + 35, topPos + 69, 16, 52);
    }
}
