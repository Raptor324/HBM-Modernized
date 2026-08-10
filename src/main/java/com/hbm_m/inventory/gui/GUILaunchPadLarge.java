package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUILaunchPadLarge extends GuiInfoScreen<LaunchPadLargeMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_large.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_large.png");
            //?}

    private static final int TANK_X = 125;
    private static final int TANK_Y = 88;
    private static final int ENERGY_TANK_X = 107;
    private static final int OXIDIZER_TANK_X = 143;
    private static final int TANK_W = 16;
    private static final int TANK_H = 52;
    private static final int TANK_TOP_Y = TANK_Y - TANK_H;

    public GUILaunchPadLarge(LaunchPadLargeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 236;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        LaunchPadBaseBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }

        int fuel = be.getFuelState();
        int oxidizer = be.getOxidizerState();

        if (fuel == 1) {
            guiGraphics.blit(TEXTURE, this.leftPos + 130, this.topPos + 23, 192, 0, 6, 8);
        } else if (fuel == -1) {
            guiGraphics.blit(TEXTURE, this.leftPos + 130, this.topPos + 23, 198, 0, 6, 8);
        }
        if (oxidizer == 1) {
            guiGraphics.blit(TEXTURE, this.leftPos + 148, this.topPos + 23, 192, 0, 6, 8);
        } else if (oxidizer == -1) {
            guiGraphics.blit(TEXTURE, this.leftPos + 148, this.topPos + 23, 198, 0, 6, 8);
        }
        long energy = menu.getEnergyLong();
        long maxEnergy = menu.getMaxEnergyLong();

        if (be.isMissileValid()) {
            int iconU = energy >= 75_000L ? 192 : 198;
            guiGraphics.blit(TEXTURE, this.leftPos + 112, this.topPos + 23, iconU, 0, 6, 8);
        }
        if (maxEnergy > 0 && energy > 0) {
            int height = (int) (energy * TANK_H / maxEnergy);
            if (height > 0) {
                guiGraphics.blit(TEXTURE,
                        this.leftPos + ENERGY_TANK_X,
                        this.topPos + TANK_Y - height,
                        176,
                        TANK_H - height,
                        TANK_W,
                        height);
            }
        }

        FluidTank[] tanks = be.getTanks();
        tanks[0].renderTank(guiGraphics, this.leftPos + TANK_X, this.topPos + TANK_TOP_Y, TANK_W, TANK_H);
        tanks[1].renderTank(guiGraphics, this.leftPos + OXIDIZER_TANK_X, this.topPos + TANK_TOP_Y, TANK_W, TANK_H);

        //? if forge {
        renderMissilePreview(guiGraphics, be);
        //?}

        renderStatusLabel(guiGraphics, be);
    }

    //? if forge {
    private void renderMissilePreview(GuiGraphics guiGraphics, LaunchPadBaseBlockEntity be) {
        ItemStack missileStack = be.getMissilePreviewStack();
        if (missileStack.isEmpty() || !(missileStack.getItem() instanceof MissileItem missileItem)) {
            return;
        }

        float scale = resolvePreviewScale(missileItem, missileStack);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + 70.0F, this.topPos + 120.0F, 100.0F);

        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(90.0F));
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().scale(-8.0F, -8.0F, -8.0F);

        guiGraphics.pose().pushPose();
        Lighting.setupForFlatItems();
        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(75.0F));
        guiGraphics.pose().popPose();

        var bufferSource = guiGraphics.bufferSource();
        com.hbm_m.client.render.missile.MissileRenderHelper.drawBakedQuads(
                guiGraphics.pose(), bufferSource, LightTexture.FULL_BRIGHT, missileStack);
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.solid());

        Lighting.setupFor3DItems();
        guiGraphics.pose().popPose();
    }
    //?}

    private static float resolvePreviewScale(MissileItem missile, ItemStack stack) {
        float scale = switch (missile.formFactor) {
            case ABM -> 1.45F;
            case MICRO -> 2.5F;
            case V2 -> 1.75F;
            case STEALTH -> 1.125F;
            case STRONG -> 1.375F;
            case HUGE -> 0.925F;
            case ATLAS -> 0.875F;
            case OTHER -> 1.0F;
        };
        return scale;
    }

    private void renderStatusLabel(GuiGraphics guiGraphics, LaunchPadBaseBlockEntity be) {
        String key;
        float textScale;
        int color;
        switch (be.getState()) {
            case LaunchPadBaseBlockEntity.STATE_LOADING -> {
                key = "gui.launchPad.loading";
                textScale = 0.6F;
                color = 0xFFFF8000;
            }
            case LaunchPadBaseBlockEntity.STATE_READY -> {
                key = "gui.launchPad.ready";
                textScale = 0.8F;
                color = 0xFF00FF00;
            }
            default -> {
                key = "gui.launchPad.notReady";
                textScale = 0.5F;
                color = 0xFFFF0000;
            }
        }

        Component text = Component.translatable(key);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + 34.0F, this.topPos + 107.0F, 0.0F);
        guiGraphics.pose().scale(textScale, textScale, 1.0F);
        int w = this.font.width(text);
        guiGraphics.drawString(this.font, text, -w / 2, -this.font.lineHeight / 2, color, false);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font,
                this.title,
                this.imageWidth / 2 - this.font.width(this.title) / 2,
                4,
                0x404040,
                false);

        guiGraphics.drawString(this.font,
                this.playerInventoryTitle,
                8,
                this.imageHeight - 96 + 2,
                0x404040,
                false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        LaunchPadBaseBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            drawElectricityInfo(guiGraphics, mouseX, mouseY,
                    this.leftPos + ENERGY_TANK_X, this.topPos + TANK_TOP_Y, TANK_W, TANK_H,
                    menu.getEnergyLong(), menu.getMaxEnergyLong());

            FluidTank[] tanks = be.getTanks();
            tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + TANK_X, this.topPos + TANK_TOP_Y, TANK_W, TANK_H);
            tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + OXIDIZER_TANK_X, this.topPos + TANK_TOP_Y, TANK_W, TANK_H);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
