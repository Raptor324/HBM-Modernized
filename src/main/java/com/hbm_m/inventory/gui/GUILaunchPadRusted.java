package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import java.util.Random;

import com.hbm_m.client.GuiCompat;
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

import java.util.Random;

/**
 * GUI ржавой пусковой площадки — порт {@code GUILaunchPadRusted} (1.7.10):
 * индикаторы наличия кода и ключа, 8-значный код запуска (детерминирован
 * координатами пада: {@code Random(x*131071 + z)}), статус ракеты и её превью.
 *
 * Отличие от оригинала: кнопка Release не нужна — ракета загружается и
 * выгружается предметом через слот 0 (в 1.7.10 пад заряжался только
 * генерацией силоса).
 */
public class GUILaunchPadRusted extends GuiInfoScreen<LaunchPadRustedMenu> {

    private static final ResourceLocation TEXTURE =
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_rusted.png");

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

        // 1.7.10: индикаторы кода и ключа
        boolean hasCodes = be.getInventory().getStackInSlot(LaunchPadRustedBlockEntity.SLOT_LAUNCH_CODE)
                .is(ModItems.LAUNCH_CODE.get());
        boolean hasKey = be.getInventory().getStackInSlot(LaunchPadRustedBlockEntity.SLOT_LAUNCH_KEY)
                .is(ModItems.LAUNCH_KEY.get());

        if (hasCodes) guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 32, 192, 0, 6, 8);
        if (hasKey) guiGraphics.blit(TEXTURE, this.leftPos + 139, this.topPos + 32, 192, 0, 6, 8);

        // 1.7.10: 8-значный код, детерминированный координатами пада
        if (hasCodes && hasKey && be.isMissileValid()) {
            Random rand = new Random(be.getBlockPos().getX() * 131_071L + be.getBlockPos().getZ());
            int launchCodes = rand.nextInt(100_000_000);

            for (int i = 0; i < 8; i++) {
                int magnitude = (int) Math.pow(10, i);
                int digit = (launchCodes % (magnitude * 10)) / magnitude;
                guiGraphics.blit(TEXTURE, this.leftPos + 109 + 6 * i, this.topPos + 85, 192 + 6 * digit, 8, 6, 8);
            }
        }

        GUILaunchPadLarge.renderMissilePreview(guiGraphics, be, this.leftPos, this.topPos);

        GUILaunchPadLarge.renderStatusLabel(guiGraphics, be, this.leftPos, this.topPos);
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
