package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineFlareStackBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.menu.MachineFlareStackMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIMachineGasFlare}: Ventil- und Zuendschalter per Klick, Tank- und Energieanzeige. */
public class GUIMachineFlareStack extends GuiInfoScreen<MachineFlareStackMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_flare_stack.png");

    private final MachineFlareStackBlockEntity flare;

    public GUIMachineFlareStack(MachineFlareStackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.flare = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (flare != null) {
            drawCustomInfoStat(g, mouseX, mouseY, 79, 16, 35, 10, mouseX, mouseY, Component.translatable("flare.valve"));
            drawCustomInfoStat(g, mouseX, mouseY, 79, 50, 35, 14, mouseX, mouseY, Component.translatable("flare.ignition"));

            flare.tank.renderTankInfo(g, this.font, mouseX, mouseY, this.leftPos + 35, this.topPos + 69 - 52, 16, 52);
            drawElectricityInfo(g, mouseX, mouseY, 143, 69 - 52, 16, 52, flare.getEnergyStored(), MachineFlareStackBlockEntity.maxPower);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (flare != null) {
            int lx = (int) x - this.leftPos;
            int ly = (int) y - this.topPos;
            if (89 <= lx && 89 + 16 > lx && 16 < ly && 16 + 10 >= ly) {
                playClickSound();
                CompoundTag data = new CompoundTag();
                data.putBoolean("valve", true);
                com.hbm_m.network.NBTControlPacket.sendToServer(flare.getBlockPos(), data);
                return true;
            } else if (89 <= lx && 89 + 16 > lx && 50 < ly && 50 + 14 >= ly) {
                playClickSound();
                CompoundTag data = new CompoundTag();
                data.putBoolean("dial", true);
                com.hbm_m.network.NBTControlPacket.sendToServer(flare.getBlockPos(), data);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (flare == null) return; // тайл может отсутствовать в реплее Flashback

        int j = (int) flare.getPowerScaled(52);
        g.blit(TEXTURE, this.leftPos + 143, this.topPos + 69 - j, 176, 94 - j, 16, j);

        if (flare.isOn) g.blit(TEXTURE, this.leftPos + 79, this.topPos + 15, 176, 0, 35, 10);
        if (flare.doesBurn) g.blit(TEXTURE, this.leftPos + 79, this.topPos + 49, 176, 10, 35, 14);

        if (flare.isOn && flare.doesBurn && flare.tank.getFill() > 0 && FluidType.forFluid(flare.tank.getTankType()).hasTrait(FT_Flammable.class))
            g.blit(TEXTURE, this.leftPos + 88, this.topPos + 29, 176, 24, 18, 18);

        flare.tank.renderTank(g, this.leftPos + 35, this.topPos + 17, 16, 52);
    }
}
