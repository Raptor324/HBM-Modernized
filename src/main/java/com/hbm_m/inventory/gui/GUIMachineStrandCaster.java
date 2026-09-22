package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.menu.MachineStrandCasterMenu;
import com.hbm_m.item.material.ScrapItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Порт {@code GUIMachineStrandCaster} (1.7.10): 176×214; столб расплава
 * рисуется текстурой GUI (u=176) с тинтом цвета материала, блендингом
 * SRC_ALPHA/ONE и оверлеем 0.3; тултип состава (порт drawStackInfo);
 * баки воды и отработанного пара 16×24 через {@code FluidTank.renderTank}.
 */
public class GUIMachineStrandCaster extends GuiInfoScreen<MachineStrandCasterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_strand_caster.png");

    private static final int BAR_X = 17, BAR_ANCHOR_Y = 93, BAR_W = 34, BAR_MAX_H = 79, BAR_TEX_ANCHOR_V = 89;
    private static final int INFO_X = 16, INFO_Y = 17, INFO_W = 36, INFO_H = 81;
    // Оригинальный FluidTankNTM.renderTank(x, y, ...) якорит бак по НИЖНЕЙ грани
    // (вода: низ y=38 → 14..38; пар: низ y=89 → 65..89). Наш renderTank рисует ВНИЗ
    // от верхней грани — передаём верхнюю координату (14/65), иначе баки уезжают на
    // 24px ниже своих рамок («инвертированный» рендер).
    private static final int TANK_X = 82, WATER_TANK_TOP = 14, STEAM_TANK_TOP = 65, TANK_W = 16, TANK_H = 24;
    private static final int WATER_INFO_Y = 14, STEAM_INFO_Y = 65;

    private final MachineStrandCasterMenu menu;

    public GUIMachineStrandCaster(MachineStrandCasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.menu = menu;
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        MachineStrandCasterBlockEntity be = menu.blockEntity;
        if (be == null) return;

        // Столб расплава (порт ветки amount != 0 из drawGuiContainerBackgroundLayer)
        if (be.amount != 0 && be.getCapacity() > 0 && be.type != null) {
            int targetHeight = Math.min(be.amount * BAR_MAX_H / be.getCapacity(), 92);

            int color = be.type.color;
            float r = ((color >> 16) & 0xFF) / 255F;
            float gr = ((color >> 8) & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

            RenderSystem.setShaderColor(r, gr, b, 1f);
            g.blit(TEXTURE, this.leftPos + BAR_X, this.topPos + BAR_ANCHOR_Y - targetHeight,
                    176, BAR_TEX_ANCHOR_V - targetHeight, BAR_W, targetHeight);
            RenderSystem.setShaderColor(1F, 1F, 1F, 0.3F);
            g.blit(TEXTURE, this.leftPos + BAR_X, this.topPos + BAR_ANCHOR_Y - targetHeight,
                    176, BAR_TEX_ANCHOR_V - targetHeight, BAR_W, targetHeight);

            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        // Баки воды и отработанного пара (порт renderTank (82, низ 38)/(82, низ 89) 16×24)
        be.getWaterTank().renderTank(g, this.leftPos + TANK_X, this.topPos + WATER_TANK_TOP, TANK_W, TANK_H);
        be.getSteamTank().renderTank(g, this.leftPos + TANK_X, this.topPos + STEAM_TANK_TOP, TANK_W, TANK_H);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Порт drawStackInfo: "Empty" красным либо "Материал: количество". */
    private void drawStackInfo(GuiGraphics g, double mouseX, double mouseY) {
        MachineStrandCasterBlockEntity be = menu.blockEntity;
        List<Component> list = new ArrayList<>();
        if (be == null || be.type == null) {
            list.add(Component.translatable("gui.hbm_m.crucible.empty").withStyle(ChatFormatting.RED));
        } else {
            boolean shift = hasShiftDown();
            int quanta = (int) ((long) be.amount * ScrapItem.QUANTA_PER_INGOT / MaterialStack.MB_PER_INGOT);
            list.add(Component.literal("").withStyle(ChatFormatting.YELLOW)
                    .append(GUIScreenRecipeSelector.materialNameOf(new MaterialStack(be.type, 0)))
                    .append(Component.literal(": " + ScrapItem.formatAmount(quanta, shift).getString())));
        }
        drawCustomInfoStat(g, (int) mouseX, (int) mouseY,
                INFO_X, INFO_Y, INFO_W, INFO_H, (int) mouseX, (int) mouseY,
                list.toArray(new Component[0]));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        MachineStrandCasterBlockEntity be = menu.blockEntity;
        if (be == null) return;

        drawStackInfo(g, mouseX, mouseY);

        // Порт renderTankInfo (82,14)/(82,65) 16×24
        be.getWaterTank().renderTankInfo(g, this.font, mouseX, mouseY,
                this.leftPos + TANK_X, this.topPos + WATER_INFO_Y, TANK_W, TANK_H);
        be.getSteamTank().renderTankInfo(g, this.font, mouseX, mouseY,
                this.leftPos + TANK_X, this.topPos + STEAM_INFO_Y, TANK_W, TANK_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, delta);
        super.render(g, mouseX, mouseY, delta);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 4, 0xFFFFFF, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }
}
