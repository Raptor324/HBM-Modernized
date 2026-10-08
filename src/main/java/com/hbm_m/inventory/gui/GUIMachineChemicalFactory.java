package com.hbm_m.inventory.gui;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.inventory.menu.MachineChemicalFactoryMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ChemicalPlantRecipe;
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
 * 1:1 {@code GUIMachineChemicalFactory} (248x216): je Modul Rezeptknopf (74,19+i*22) mit Auswahlfenster,
 * Fortschrittsbalken, zwei LEDs, Geister-Eingaenge, je drei Ein-/Ausgangstanks; Kuehlkreis rechts unten.
 */
public class GUIMachineChemicalFactory extends AbstractContainerScreen<MachineChemicalFactoryMenu> {

    private static final int LANE_COUNT = MachineChemicalFactoryBlockEntity.LANE_COUNT;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_chemical_factory.png");

    public GUIMachineChemicalFactory(MachineChemicalFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 216;
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
        MachineChemicalFactoryBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        for (int i = 0; i < 3; i++) for (int j = 0; j < LANE_COUNT; j++) {
            be.inputTanks[i + j * 3].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 60 + i * 5, topPos + 20 + j * 22, 3, 16);
            be.outputTanks[i + j * 3].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 189 + i * 5, topPos + 20 + j * 22, 3, 16);
        }

        be.water.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 224, topPos + 125, 7, 52);
        be.lps.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 233, topPos + 125, 7, 52);

        if (isIn(mouseX, mouseY, 224, 18, 16, 68)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(EnergyFormatter.format(menu.getEnergyStored()) + " / " + EnergyFormatter.format(menu.getMaxEnergyStored()) + " HE"),
                    mouseX, mouseY);
        }

        for (int i = 0; i < LANE_COUNT; i++) {
            if (leftPos + 74 <= mouseX && leftPos + 74 + 18 > mouseX && topPos + 19 + i * 22 < mouseY && topPos + 19 + i * 22 + 18 >= mouseY) {
                ChemicalPlantRecipe recipe = getRecipe(i);
                if (recipe != null) {
                    guiGraphics.renderTooltip(this.font, GUIMachineChemicalPlant.buildRecipeTooltip(recipe), Optional.empty(), mouseX, mouseY);
                } else {
                    guiGraphics.renderTooltip(this.font, Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < LANE_COUNT; i++) {
            if (isIn((int) mouseX, (int) mouseY, 74, 19 + i * 22, 18, 18) && this.minecraft != null && menu.getBlockEntity() != null) {
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
        guiGraphics.drawString(this.font, name, 106 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 26, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, 248, 116);
        guiGraphics.blit(TEXTURE, leftPos + 18, topPos + 116, 18, 116, 230, 100);

        long power = menu.getEnergyStored();
        long maxPower = menu.getMaxEnergyStored();
        if (maxPower > 0) {
            int p = (int) (power * 68 / maxPower);
            guiGraphics.blit(TEXTURE, leftPos + 224, topPos + 86 - p, 0, 184 - p, 16, p);
        }

        for (int i = 0; i < LANE_COUNT; i++) {
            int prog = menu.getLaneProgress(i);
            int max = menu.getLaneMaxProgress(i);
            if (prog > 0 && max > 0) {
                int j = (int) Math.ceil(22D * prog / max);
                guiGraphics.blit(TEXTURE, leftPos + 113, topPos + 29 + i * 22, 0, 216, j, 6);
            }
        }

        MachineChemicalFactoryBlockEntity be = menu.getBlockEntity();

        for (int g = 0; g < LANE_COUNT; g++) {
            ChemicalPlantRecipe recipe = getRecipe(g);
            boolean did = menu.getLaneDidProcess(g);

            /// LEFT LED
            if (did) {
                guiGraphics.blit(TEXTURE, leftPos + 113, topPos + 21 + g * 22, 4, 222, 4, 4);
            } else if (recipe != null) {
                guiGraphics.blit(TEXTURE, leftPos + 113, topPos + 21 + g * 22, 0, 222, 4, 4);
            }

            /// RIGHT LED
            if (did) {
                guiGraphics.blit(TEXTURE, leftPos + 121, topPos + 21 + g * 22, 4, 222, 4, 4);
            } else if (recipe != null && power >= recipe.getPowerConsumption() && be != null && be.canCool()) {
                guiGraphics.blit(TEXTURE, leftPos + 121, topPos + 21 + g * 22, 0, 222, 4, 4);
            }
        }

        for (int g = 0; g < LANE_COUNT; g++) {
            ChemicalPlantRecipe recipe = getRecipe(g);

            ItemStack icon = recipe != null && this.minecraft != null && this.minecraft.level != null
                    ? recipe.getResultItem(this.minecraft.level.registryAccess()) : ItemStack.EMPTY;
            if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
            guiGraphics.renderItem(icon, leftPos + 75, topPos + 20 + g * 22);

            if (recipe != null) {
                for (int i = 0; i < recipe.getItemInputs().size() && i < 3; i++) {
                    Slot slot = this.menu.slots.get(MachineChemicalFactoryBlockEntity.inputSlot(g, i));
                    if (slot.hasItem()) continue;
                    var in = recipe.getItemInputs().get(i);
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
            for (int i = 0; i < 3; i++) for (int j = 0; j < LANE_COUNT; j++) {
                be.inputTanks[i + j * 3].renderTank(guiGraphics, leftPos + 60 + i * 5, topPos + 20 + j * 22, 3, 16);
                be.outputTanks[i + j * 3].renderTank(guiGraphics, leftPos + 189 + i * 5, topPos + 20 + j * 22, 3, 16);
            }
            be.water.renderTank(guiGraphics, leftPos + 224, topPos + 125, 7, 52);
            be.lps.renderTank(guiGraphics, leftPos + 233, topPos + 125, 7, 52);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Nullable
    private ChemicalPlantRecipe getRecipe(int lane) {
        if (this.minecraft == null || this.minecraft.level == null || menu.getBlockEntity() == null) return null;
        ResourceLocation id = menu.getBlockEntity().getSelectedRecipeId(lane);
        if (id == null) return null;
        return RecipeHooks.getRecipeByKey(this.minecraft.level.getRecipeManager(), id)
                .filter(r -> r instanceof ChemicalPlantRecipe)
                .map(r -> (ChemicalPlantRecipe) r)
                .orElse(null);
    }

    private boolean isIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }
}
