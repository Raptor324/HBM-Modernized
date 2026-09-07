package com.hbm_m.inventory.gui;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm_m.inventory.menu.MachineFusionPlasmaForgeMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PlasmaForgeRecipe;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code GUIMachinePlasmaForge} (1.7.10) - Koordinaten, Balken, LEDs und Zeiger
 * unveraendert; die Rezeptauswahl laeuft ueber {@link GUIScreenRecipeSelector}.
 */
public class GUIMachineFusionPlasmaForge extends GuiInfoScreen<MachineFusionPlasmaForgeMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_plasmaforge.png");
    *///?} else {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_plasmaforge.png");
    //?}

    public GUIMachineFusionPlasmaForge(MachineFusionPlasmaForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 244;
    }

    private FusionPlasmaForgeBlockEntity be() {
        return menu.getBlockEntity();
    }

    @Nullable
    private PlasmaForgeRecipe recipe() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        return be().getRecipe(this.minecraft.level);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        FusionPlasmaForgeBlockEntity be = be();
        PlasmaForgeRecipe recipe = recipe();

        long maxPower = be.getMaxEnergyStored();
        if (maxPower > 0) {
            int p = (int) (be.getEnergyStored() * 62L / maxPower);
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 80 - p, 176, 62 - p, 16, p);
        }

        if (be.progress > 0) {
            int j = (int) Math.ceil(70 * be.progress);
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 81, 176, 62, j, 16);
        }

        // linke LED
        if (be.didProcess) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 76, 195, 0, 3, 6);
        } else if (recipe != null) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 76, 192, 0, 3, 6);
        }

        // rechte LED
        if (be.didProcess) {
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 76, 195, 0, 3, 6);
        } else if (recipe != null && be.getEnergyStored() >= recipe.getPower()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 76, 192, 0, 3, 6);
        }

        double inputGauge = recipe == null || recipe.getIgnitionTemp() <= 0 ? 0
                : Math.min((double) be.plasmaEnergySync / (double) recipe.getIgnitionTemp(), 1.5) / 1.5D;
        double boosterGauge = be.maxBooster <= 0 ? 0 : (double) be.booster / (double) be.maxBooster;

        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 34, this.topPos + 124, inputGauge, 5, 2, 1, 0xA00000);
        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 70, this.topPos + 124, boosterGauge, 5, 2, 1, 0xA00000);

        ItemStack icon = ItemStack.EMPTY;
        if (recipe != null && this.minecraft != null && this.minecraft.level != null) {
            icon = recipe.getResultItem(this.minecraft.level.registryAccess());
        }
        if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
        guiGraphics.renderItem(icon, this.leftPos + 8, this.topPos + 81);

        // Geisterbilder der Rezepteingaben in leeren Slots
        if (recipe != null) {
            List<PlasmaForgeRecipe.CountedIngredient> inputs = recipe.getItemInputs();
            for (int i = 0; i < Math.min(inputs.size(), FusionPlasmaForgeBlockEntity.SLOT_INPUT_COUNT); i++) {
                Slot slot = this.menu.slots.get(FusionPlasmaForgeBlockEntity.SLOT_INPUT_FIRST + i);
                if (slot.hasItem()) continue;
                ItemStack[] matching = inputs.get(i).ingredient().getItems();
                if (matching.length == 0) continue;
                ItemStack ghost = matching[(int) ((System.currentTimeMillis() / 1000) % matching.length)].copy();
                ghost.setCount(inputs.get(i).count());
                guiGraphics.renderItem(ghost, this.leftPos + slot.x, this.topPos + slot.y);
                guiGraphics.renderItemDecorations(this.font, ghost, this.leftPos + slot.x, this.topPos + slot.y);
            }
        }

        be.inputTank.renderTank(guiGraphics, this.leftPos + 80, this.topPos + 18, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FusionPlasmaForgeBlockEntity be = be();
        PlasmaForgeRecipe recipe = recipe();

        be.inputTank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 80, this.topPos + 18, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 18, 16, 62, be.getEnergyStored(), be.getMaxEnergyStored());

        if (recipe != null) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 25, 115, 18, 18, mouseX, mouseY,
                    Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(EnergyFormatter.format(be.plasmaEnergySync) + "TU / "
                                    + EnergyFormatter.format(recipe.getIgnitionTemp()) + "TU")
                                    .withStyle(ChatFormatting.WHITE)));
        } else {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 25, 115, 18, 18, mouseX, mouseY,
                    Component.literal("0TU / 0TU"));
        }

        if (isPointInRect(7, 80, 18, 18, mouseX, mouseY) && recipe == null) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.hbm_m.recipe.set_recipe").withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPointInRect(7, 80, 18, 18, (int) mouseX, (int) mouseY)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new GUIScreenRecipeSelector(
                        be().getBlockPos(), be().getSelectedRecipeId(), this));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
