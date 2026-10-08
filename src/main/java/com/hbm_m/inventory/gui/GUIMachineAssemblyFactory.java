package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineAssemblyFactoryBlockEntity;
import com.hbm_m.inventory.menu.MachineAssemblyFactoryMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUIMachineAssemblyFactory} (256x240): vier Felder im 2x2-Raster (Versatz 109/56), je Rezeptknopf
 * (6,53) mit Auswahlfenster, Fortschrittsbalken (37 px), zwei LEDs, Geister-Eingaenge, Ein-/Ausgangstank;
 * Strom (234,18) und Kuehlkreis rechts unten.
 */
public class GUIMachineAssemblyFactory extends AbstractContainerScreen<MachineAssemblyFactoryMenu> {

    private static final int LANE_COUNT = MachineAssemblyFactoryBlockEntity.LANE_COUNT;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_assembly_factory.png");

    public GUIMachineAssemblyFactory(MachineAssemblyFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 240;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        MachineAssemblyFactoryBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        for (int j = 0; j < LANE_COUNT; j++) {
            be.inputTanks[j].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 105 + (j % 2) * 109, topPos + 20 + (j / 2) * 56, 5, 32);
            be.outputTanks[j].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 105 + (j % 2) * 109, topPos + 54 + (j / 2) * 56, 5, 16);
        }

        be.water.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 232, topPos + 149, 7, 52);
        be.lps.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 241, topPos + 149, 7, 52);

        if (isIn(mouseX, mouseY, 234, 18, 16, 92)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(EnergyFormatter.format(menu.getEnergyStored()) + " / " + EnergyFormatter.format(menu.getMaxEnergyStored()) + " HE"),
                    mouseX, mouseY);
        }

        for (int i = 0; i < LANE_COUNT; i++) {
            int bx = 6 + (i % 2) * 109;
            int by = 53 + (i / 2) * 56;
            if (leftPos + bx <= mouseX && leftPos + bx + 18 > mouseX && topPos + by < mouseY && topPos + by + 18 >= mouseY) {
                AssemblerRecipe recipe = getRecipe(i);
                if (recipe != null) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(recipe.getResultItemSafe().getHoverName());
                    com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(recipe, tooltip);
                    guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
                } else {
                    guiGraphics.renderTooltip(this.font, Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < LANE_COUNT; i++) {
            if (isIn((int) mouseX, (int) mouseY, 6 + (i % 2) * 109, 53 + (i / 2) * 56, 18, 18) && this.minecraft != null && menu.getBlockEntity() != null) {
                this.minecraft.setScreen(new GUIScreenRecipeSelector(menu.getBlockEntity().getBlockPos(),
                        menu.getBlockEntity().getSelectedRecipeId(i), this, i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 113 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 33, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, 256, 140);
        guiGraphics.blit(TEXTURE, leftPos + 25, topPos + 140, 25, 140, 231, 100);

        long power = menu.getEnergyStored();
        long maxPower = menu.getMaxEnergyStored();
        if (maxPower > 0) {
            int p = (int) (power * 92 / maxPower);
            guiGraphics.blit(TEXTURE, leftPos + 234, topPos + 110 - p, 0, 232 - p, 16, p);
        }

        for (int i = 0; i < LANE_COUNT; i++) {
            int prog = menu.getLaneProgress(i);
            int max = menu.getLaneMaxProgress(i);
            if (prog > 0 && max > 0) {
                int j = (int) Math.ceil(37D * prog / max);
                guiGraphics.blit(TEXTURE, leftPos + 45 + (i % 2) * 109, topPos + 63 + (i / 2) * 56, 0, 240, j, 6);
            }
        }

        MachineAssemblyFactoryBlockEntity be = menu.getBlockEntity();

        for (int g = 0; g < LANE_COUNT; g++) {
            AssemblerRecipe recipe = getRecipe(g);
            boolean did = menu.getLaneDidProcess(g);

            /// LEFT LED
            if (did) {
                guiGraphics.blit(TEXTURE, leftPos + 45 + (g % 2) * 109, topPos + 55 + (g / 2) * 56, 4, 236, 4, 4);
            } else if (recipe != null) {
                guiGraphics.blit(TEXTURE, leftPos + 45 + (g % 2) * 109, topPos + 55 + (g / 2) * 56, 0, 236, 4, 4);
            }

            /// RIGHT LED
            if (did) {
                guiGraphics.blit(TEXTURE, leftPos + 53 + (g % 2) * 109, topPos + 55 + (g / 2) * 56, 4, 236, 4, 4);
            } else if (recipe != null && power >= recipe.getPowerConsumption() && be != null && be.canCool()) {
                guiGraphics.blit(TEXTURE, leftPos + 53 + (g % 2) * 109, topPos + 55 + (g / 2) * 56, 0, 236, 4, 4);
            }
        }

        for (int g = 0; g < LANE_COUNT; g++) {
            AssemblerRecipe recipe = getRecipe(g);

            ItemStack icon = recipe != null ? recipe.getResultItemSafe() : ItemStack.EMPTY;
            if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
            guiGraphics.renderItem(icon, leftPos + 7 + (g % 2) * 109, topPos + 54 + (g / 2) * 56);

            if (recipe != null) {
                var inputs = recipe.getInputDisplaySlots();
                for (int i = 0; i < inputs.size() && i < 12; i++) {
                    Slot slot = this.menu.slots.get(MachineAssemblyFactoryBlockEntity.inputSlot(g, i));
                    if (slot.hasItem()) continue;
                    var in = inputs.get(i);
                    ItemStack[] variants = in.ingredient().getItems();
                    if (variants.length == 0) continue;
                    // Original extractForCyclingDisplay(20)
                    ItemStack ghost = variants[(int) ((System.currentTimeMillis() / 1000) % variants.length)].copy();
                    ghost.setCount(in.count());
                    GhostItemRenderUtil.renderTranslucent(guiGraphics, ghost, leftPos + slot.x, topPos + slot.y, 0.5f);
                    if (ghost.getCount() > 1) guiGraphics.renderItemDecorations(this.font, ghost, leftPos + slot.x, topPos + slot.y);
                }
            }
        }

        if (be != null) {
            for (int j = 0; j < LANE_COUNT; j++) {
                be.inputTanks[j].renderTank(guiGraphics, leftPos + 105 + (j % 2) * 109, topPos + 20 + (j / 2) * 56, 5, 32);
                be.outputTanks[j].renderTank(guiGraphics, leftPos + 105 + (j % 2) * 109, topPos + 54 + (j / 2) * 56, 5, 16);
            }
            be.water.renderTank(guiGraphics, leftPos + 232, topPos + 149, 7, 52);
            be.lps.renderTank(guiGraphics, leftPos + 241, topPos + 149, 7, 52);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Nullable
    private AssemblerRecipe getRecipe(int lane) {
        MachineAssemblyFactoryBlockEntity be = menu.getBlockEntity();
        return be != null ? be.getRecipe(lane) : null;
    }

    private boolean isIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }
}
