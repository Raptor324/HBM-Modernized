package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidLocalization;
import net.minecraft.core.registries.BuiltInRegistries;
import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.inventory.menu.MachineFusionTorusMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.FusionRecipe;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.architectury.fluid.FluidStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code GUIFusionTorus} (1.7.10). Alle Koordinaten, Balken, LEDs und Zeiger sind
 * unveraendert uebernommen; die Rezeptauswahl laeuft ueber den bereits vorhandenen
 * {@link GUIScreenRecipeSelector}.
 */
public class GUIMachineFusionTorus extends GuiInfoScreen<MachineFusionTorusMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_torus.png");
    *///?} else {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/reactors/gui_fusion_torus.png");
    //?}

    public GUIMachineFusionTorus(MachineFusionTorusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 244;
    }

    private FusionTorusBlockEntity be() {
        return menu.getBlockEntity();
    }

    @Nullable
    private FusionRecipe recipe() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        return be().getRecipe(this.minecraft.level);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        FusionTorusBlockEntity be = be();
        FusionRecipe recipe = recipe();

        long maxPower = be.getMaxEnergyStored();
        if (maxPower > 0) {
            int p = (int) (be.getEnergyStored() * 62L / maxPower);
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 80 - p, 230, 62 - p, 16, p);
        }

        if (be.progress > 0) {
            int j = (int) Math.ceil(70 * be.progress);
            guiGraphics.blit(TEXTURE, this.leftPos + 98, this.topPos + 81, 0, 244, j, 6);
        }

        if (be.bonus > 0) {
            int j = (int) Math.min(Math.ceil(70 * be.bonus), 70);
            guiGraphics.blit(TEXTURE, this.leftPos + 98, this.topPos + 91, 0, 250, j, 6);
        }

        // Strom-LED
        if (recipe != null && be.getEnergyStored() >= recipe.getPower()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 160, this.topPos + 115, 246, 14, 8, 8);
        }
        // Kuehl-LED
        int heat = (int) Math.ceil(be.temperature);
        if (heat <= 123) guiGraphics.blit(TEXTURE, this.leftPos + 170, this.topPos + 115, 246, 14, 8, 8);
        // Plasma-LED
        if (be.didProcess) guiGraphics.blit(TEXTURE, this.leftPos + 180, this.topPos + 115, 246, 14, 8, 8);

        // linke LED
        if (be.didProcess) {
            guiGraphics.blit(TEXTURE, this.leftPos + 87, this.topPos + 76, 249, 0, 3, 6);
        } else if (recipe != null) {
            guiGraphics.blit(TEXTURE, this.leftPos + 87, this.topPos + 76, 246, 0, 3, 6);
        }

        // rechte LED
        if (be.didProcess) {
            guiGraphics.blit(TEXTURE, this.leftPos + 92, this.topPos + 76, 249, 0, 3, 6);
        } else if (recipe != null) {
            guiGraphics.blit(TEXTURE, this.leftPos + 92, this.topPos + 76, 246, 0, 3, 6);
        }

        double inputGauge = recipe == null || recipe.getIgnitionTemp() <= 0 ? 0
                : Math.min((double) be.klystronEnergySync / (double) recipe.getIgnitionTemp(), 1.5) / 1.5D;
        double outputGauge = recipe == null || recipe.getOutputTemp() <= 0 ? 0
                : Math.min((double) be.plasmaEnergy / (double) recipe.getOutputTemp(), 1);

        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 52, this.topPos + 124, inputGauge, 5, 2, 1, 0xA00000);
        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 88, this.topPos + 124, outputGauge, 5, 2, 1, 0xA00000);
        GuiGaugeNeedle.draw(guiGraphics, this.leftPos + 124, this.topPos + 124, be.fuelConsumption, 5, 2, 1, 0xA00000);

        // Rezeptauswahl-Knopf
        ItemStack icon = ItemStack.EMPTY;
        if (recipe != null && this.minecraft != null && this.minecraft.level != null) {
            icon = recipe.getResultItem(this.minecraft.level.registryAccess());
        }
        if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
        guiGraphics.renderItem(icon, this.leftPos + 44, this.topPos + 81);

        be.tanks[0].renderTank(guiGraphics, this.leftPos + 44, this.topPos + 18, 16, 52);
        be.tanks[1].renderTank(guiGraphics, this.leftPos + 62, this.topPos + 18, 16, 52);
        be.tanks[2].renderTank(guiGraphics, this.leftPos + 80, this.topPos + 18, 16, 52);
        be.tanks[3].renderTank(guiGraphics, this.leftPos + 152, this.topPos + 18, 16, 52);

        be.coolantTanks[0].renderTank(guiGraphics, this.leftPos + 188, this.topPos + 46, 16, 52);
        be.coolantTanks[1].renderTank(guiGraphics, this.leftPos + 206, this.topPos + 46, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, 106 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 35, this.imageHeight - 93, 0x404040, false);

        guiGraphics.drawString(this.font, Component.literal("/123K").withStyle(ChatFormatting.AQUA),
                136 + 54, 32, 0x404040, false);

        int heat = (int) Math.ceil(be().temperature);
        Component label = Component.literal(heat + "K")
                .withStyle(heat > 123 ? ChatFormatting.RED : ChatFormatting.AQUA);
        guiGraphics.drawString(this.font, label, 166 + 54 - this.font.width(label), 22, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FusionTorusBlockEntity be = be();
        FusionRecipe recipe = recipe();

        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 62, be.getEnergyStored(), be.getMaxEnergyStored());

        be.tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 44, this.topPos + 18, 16, 52);
        be.tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 62, this.topPos + 18, 16, 52);
        be.tanks[2].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 80, this.topPos + 18, 16, 52);
        be.tanks[3].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 152, this.topPos + 18, 16, 52);
        be.coolantTanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 188, this.topPos + 46, 16, 52);
        be.coolantTanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 206, this.topPos + 46, 16, 52);

        if (recipe != null) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 43, 115, 18, 18, mouseX, mouseY,
                    Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(EnergyFormatter.format(be.klystronEnergySync) + "KyU / "
                                    + EnergyFormatter.format(recipe.getIgnitionTemp()) + "KyU")
                                    .withStyle(ChatFormatting.WHITE)));

            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 79, 115, 18, 18, mouseX, mouseY,
                    Component.literal("<- ").withStyle(ChatFormatting.RED)
                            .append(Component.literal(EnergyFormatter.format(be.plasmaEnergy) + "TU / "
                                    + EnergyFormatter.format(recipe.getOutputTemp()) + "TU")
                                    .withStyle(ChatFormatting.WHITE)));

            List<FluidStack> inputs = recipe.getFluidInputs();
            List<Component> lines = new ArrayList<>(inputs.size());
            for (FluidStack in : inputs) {
                int consumption = (int) Math.ceil(in.getAmount() * be.fuelConsumption);
                lines.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(consumption + "mB/t ").withStyle(ChatFormatting.WHITE))
                        .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(in.getFluid()))));
            }
            if (!lines.isEmpty()) {
                drawCustomInfoStat(guiGraphics, mouseX, mouseY, 115, 115, 18, 18, mouseX, mouseY,
                        lines.toArray(new Component[0]));
            }
        } else {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 43, 115, 18, 18, mouseX, mouseY,
                    Component.literal("0KyU / 0KyU"));
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 79, 115, 18, 18, mouseX, mouseY,
                    Component.literal("0TU / 0TU"));
        }

        if (isPointInRect(43, 80, 18, 18, mouseX, mouseY) && recipe == null) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.hbm_m.recipe.set_recipe").withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isPointInRect(43, 80, 18, 18, (int) mouseX, (int) mouseY)) {
            openRecipeSelector();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openRecipeSelector() {
        if (this.minecraft == null || menu.getBlockEntity() == null) return;
        this.minecraft.setScreen(new GUIScreenRecipeSelector(
                menu.getBlockEntity().getBlockPos(),
                menu.getBlockEntity().getSelectedRecipeId(),
                this));
    }
}
