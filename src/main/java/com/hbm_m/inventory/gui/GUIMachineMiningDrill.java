package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMiningDrillMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIMachineExcavator} (242x204): fuenf Kippschalter (Bohrer, Brecher, Wand, Adernabbau, Behutsamkeit)
 * mit Statuslampen, Energiebalken + Betriebslampe, blinkender Bohrkopf-Hinweis, Saeuretank.
 */
public class GUIMachineMiningDrill extends GuiInfoScreen<MachineMiningDrillMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_mining_drill.png");

    private static final String[] TOGGLES = { "drill", "crusher", "walling", "veinminer", "silktouch" };

    private final MachineMiningDrillBlockEntity drill;

    public GUIMachineMiningDrill(MachineMiningDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.drill = menu.getBlockEntity();
        this.imageWidth = 242;
        this.imageHeight = 204;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (drill != null) {
            for (int i = 0; i < 5; i++) {
                drawCustomInfoStat(g, x, y, 6 + i * 24, 42, 20, 40, x, y, Component.translatable("excavator." + TOGGLES[i]));
            }

            drawElectricityInfo(g, x, y, 220, 18, 16, 52, drill.getEnergyStored(), MachineMiningDrillBlockEntity.maxPower);
            drill.tank.renderTankInfo(g, this.font, x, y, leftPos + 202, topPos + 18, 16, 52);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean handled = super.mouseClicked(mx, my, button);
        int x = (int) mx;
        int y = (int) my;

        String toggle = null;

        for (int i = 0; i < 5; i++) {
            int bx = leftPos + 6 + i * 24;
            if (bx <= x && bx + 20 > x && topPos + 42 < y && topPos + 42 + 40 >= y) toggle = TOGGLES[i];
        }

        if (toggle != null && drill != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(com.hbm_m.sound.HbmSoundsNT.get("hbm:block.leverLarge"), 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean(toggle, true);
            NBTControlPacket.sendToServer(drill.getBlockPos(), data);
            return true;
        }

        return handled;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, 8 + 33, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, 242, 96);
        g.blit(TEXTURE, leftPos + 33, topPos + 104, 33, 104, 176, 100);
        if (drill == null) return;

        int i = (int) (drill.getEnergyStored() * 52 / MachineMiningDrillBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 220, topPos + 70 - i, 229, 156 - i, 16, i);

        if (drill.getEnergyStored() > drill.getPowerConsumption()) {
            g.blit(TEXTURE, leftPos + 224, topPos + 4, 239, 156, 9, 12);
        }

        boolean blink = System.currentTimeMillis() % 1000 < 500;

        if (drill.getInstalledDrill() == null && blink) {
            g.blit(TEXTURE, leftPos + 171, topPos + 74, 209, 154, 18, 18);
        }

        if (drill.enableDrill) {
            g.blit(TEXTURE, leftPos + 6, topPos + 42, 209, 114, 20, 40);
            if (drill.getInstalledDrill() != null && drill.getEnergyStored() >= drill.getPowerConsumption()) g.blit(TEXTURE, leftPos + 11, topPos + 5, 209, 104, 10, 10);
            else if (blink) g.blit(TEXTURE, leftPos + 11, topPos + 5, 219, 104, 10, 10);
        }

        if (drill.enableCrusher) {
            g.blit(TEXTURE, leftPos + 30, topPos + 42, 209, 114, 20, 40);
            g.blit(TEXTURE, leftPos + 35, topPos + 5, 209, 104, 10, 10);
        }

        if (drill.enableWalling) {
            g.blit(TEXTURE, leftPos + 54, topPos + 42, 209, 114, 20, 40);
            g.blit(TEXTURE, leftPos + 59, topPos + 5, 209, 104, 10, 10);
        }

        if (drill.enableVeinMiner) {
            g.blit(TEXTURE, leftPos + 78, topPos + 42, 209, 114, 20, 40);
            if (drill.canVeinMine()) g.blit(TEXTURE, leftPos + 83, topPos + 5, 209, 104, 10, 10);
            else if (blink) g.blit(TEXTURE, leftPos + 83, topPos + 5, 219, 104, 10, 10);
        }

        if (drill.enableSilkTouch) {
            g.blit(TEXTURE, leftPos + 102, topPos + 42, 209, 114, 20, 40);
            if (drill.canSilkTouch()) g.blit(TEXTURE, leftPos + 107, topPos + 5, 209, 104, 10, 10);
            else if (blink) g.blit(TEXTURE, leftPos + 107, topPos + 5, 219, 104, 10, 10);
        }

        drill.tank.renderTank(g, leftPos + 202, topPos + 18, 16, 52);
    }
}
