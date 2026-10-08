package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import java.util.Random;

import com.hbm_m.blockentity.machines.LaunchPadRustedBlockEntity;
import com.hbm_m.inventory.menu.LaunchPadRustedMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUILaunchPadRusted}: Knopf "Release Missile", Anzeigen fuer Codes/Schluessel, achtstelliger
 * Startcode (aus der Position gewuerfelt) und die eingebaute Rakete als Vorschau.
 */
public class GUILaunchPadRusted extends GuiInfoScreen<LaunchPadRustedMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_rusted.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_rusted.png");
            //?}


    public GUILaunchPadRusted(LaunchPadRustedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 236;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        LaunchPadRustedBlockEntity be = menu.getBlockEntity();
        if (be != null && leftPos + 26 <= x && leftPos + 26 + 16 > x && topPos + 36 < y && topPos + 36 + 16 >= y) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean("release", true);
            NBTControlPacket.sendToServer(be.getBlockPos(), data);
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        LaunchPadRustedBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }

        boolean hasCodes = be.getInventory().getStackInSlot(1).getItem() == ModItems.LAUNCH_CODE.get();
        boolean hasKey = be.getInventory().getStackInSlot(2).getItem() == ModItems.LAUNCH_KEY.get();

        if (hasCodes) guiGraphics.blit(TEXTURE, leftPos + 121, topPos + 32, 192, 0, 6, 8);
        if (hasKey) guiGraphics.blit(TEXTURE, leftPos + 139, topPos + 32, 192, 0, 6, 8);

        if (hasCodes && hasKey && be.missileLoaded) {
            Random rand = new Random(be.getBlockPos().getX() * 131_071 + be.getBlockPos().getZ());
            int launchCodes = rand.nextInt(100_000_000);

            for (int i = 0; i < 8; i++) {
                int magnitude = (int) Math.pow(10, i);
                int digit = (launchCodes % (magnitude * 10)) / magnitude;
                guiGraphics.blit(TEXTURE, leftPos + 109 + 6 * i, topPos + 85, 192 + 6 * digit, 8, 6, 8);
            }
        }

        //? if forge || neoforge {
        if (be.missileLoaded) {
            ItemStack missile = new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(this.leftPos + 70.0F, this.topPos + 120.0F, 100.0F);
            guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(90.0F));
            float scale = 0.875F;
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.pose().scale(-8.0F, -8.0F, -8.0F);
            Lighting.setupForFlatItems();

            var bufferSource = guiGraphics.bufferSource();
            com.hbm_m.client.render.missile.MissileRenderHelper.drawBakedQuads(
                    guiGraphics.pose(), bufferSource, LightTexture.FULL_BRIGHT, missile);
            bufferSource.endBatch(net.minecraft.client.renderer.RenderType.solid());

            Lighting.setupFor3DItems();
            guiGraphics.pose().popPose();
        }
        //?}
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font,
                title,
                this.imageWidth / 2 - this.font.width(title) / 2,
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
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 26, 36, 16, 16, mouseX, mouseY,
                Component.literal("Release Missile").withStyle(ChatFormatting.YELLOW),
                Component.literal("Missile is locked in lauch position,"),
                Component.literal("releasing may cause damage to the missile."),
                Component.literal("Damaged missile can not be put back"),
                Component.literal("into launching position."));
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
