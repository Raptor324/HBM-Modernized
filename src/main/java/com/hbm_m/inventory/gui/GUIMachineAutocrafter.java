package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineAutocrafterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineAutocrafterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 1:1 {@code GUIAutocrafter}: Energiesaeule aus der Textur, Filtermodus-Hinweis ueber den Vorlagenplaetzen
 * ("Right click to change" + Modus) und Rezeptzaehler ueber dem Vorschauplatz.
 */
public class GUIMachineAutocrafter extends GuiInfoScreen<MachineAutocrafterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_autocrafter.png");

    private final MachineAutocrafterBlockEntity blockEntity;

    public GUIMachineAutocrafter(MachineAutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 240;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (blockEntity != null) {
            drawElectricityInfo(g, mouseX, mouseY, 17, 45, 16, 52, blockEntity.getEnergyStored(), blockEntity.getMaxEnergyStored());

            if (this.menu.getCarried().isEmpty()) {
                for (int i = 0; i < 9; ++i) {
                    Slot slot = this.menu.slots.get(i);
                    String mode = blockEntity.getMatcher().getMode(i);
                    if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && mode != null) {
                        g.renderComponentTooltip(this.font, List.of(
                                Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                                Component.literal(ModulePatternMatcher.getLabel(mode))), mouseX, mouseY - 30);
                    }
                }

                Slot slot = this.menu.slots.get(9);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.hasItem()) {
                    g.renderComponentTooltip(this.font, List.of(
                            Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal((blockEntity.getRecipeIndex() + 1) + " / " + blockEntity.getRecipeCount()).withStyle(ChatFormatting.YELLOW)),
                            mouseX, mouseY - 30);
                }
            }
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (blockEntity == null) return; // тайл может отсутствовать в реплее Flashback

        int i = (int) (blockEntity.getEnergyStored() * 52 / Math.max(1, blockEntity.getMaxEnergyStored()));
        g.blit(TEXTURE, leftPos + 17, topPos + 97 - i, 176, 52 - i, 16, i);
    }
}
