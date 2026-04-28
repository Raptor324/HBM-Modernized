package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.menu.MachineChemicalPlantMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
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
import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.platform.ModFluidTank;
import dev.architectury.fluid.FluidStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

/**
 * GUI для Chemical Plant - порт с 1.7.10.
 * UV-координаты и layout соответствуют оригинальному GUIMachineChemicalPlant.
 */
public class GUIMachineChemicalPlant extends AbstractContainerScreen<MachineChemicalPlantMenu> {

    //? if fabric && < 1.21.1 {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/processing/gui_chemplant.png");
    //?} else {
        /*private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_chemplant.png");
    *///?}


    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 34;
    private static final int TANK_CAPACITY = 24_000;

    public GUIMachineChemicalPlant(MachineChemicalPlantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        long energyStored = menu.getBlockEntity().getEnergyStored();
        long maxEnergy = menu.getBlockEntity().getMaxEnergyStored();
        if (maxEnergy > 0) {
            int p = (int) (energyStored * 61L / maxEnergy);
            if (p > 61) p = 61;
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - p, 176, 61 - p, 16, p);
        }

        int progress = menu.getProgress();
        if (progress > 0) {
            int maxProgress = menu.getMaxProgress();
            if (maxProgress > 0) {
                int j = (int) Math.ceil(70.0 * progress / maxProgress);
                guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, j, 16);
            }
        }

        boolean hasRecipe = menu.getBlockEntity().getSelectedRecipeId() != null;
        boolean didProcess = menu.getBlockEntity().getDidProcess();
        boolean canProcess = hasRecipe && energyStored >= 100;

        if (didProcess) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (hasRecipe) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (canProcess) {
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        for (int i = 0; i < 3; i++) {
            renderFluidTank(guiGraphics, menu.getBlockEntity().getInputTanks()[i],
                    this.leftPos + 8 + i * 18, this.topPos + 52);
            renderFluidTank(guiGraphics, menu.getBlockEntity().getOutputTanks()[i],
                    this.leftPos + 80 + i * 18, this.topPos + 52);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TEXTURE);

        renderGhostInputs(guiGraphics);
    }

    private void renderFluidTank(GuiGraphics guiGraphics, ModFluidTank tank, int x, int y) {
        Fluid fluid = tank.getStoredFluid();
        int amountMb = tank.getFluidAmountMb();
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY || amountMb <= 0) return;

        int pixelHeight = (int) ((long) amountMb * TANK_HEIGHT / TANK_CAPACITY);
        if (pixelHeight == 0 && amountMb > 0) pixelHeight = 1;
        if (pixelHeight > TANK_HEIGHT) pixelHeight = TANK_HEIGHT;

        int color = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = 1.0F;

        ResourceLocation png = FluidGuiRendering.guiTexturePngForFluid(fluid, FluidStack.create(fluid, (long) amountMb));
        if (png == null) return;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);

        FluidGuiRendering.renderTiledFluid(guiGraphics, png, x, y + TANK_HEIGHT - pixelHeight, TANK_WIDTH, pixelHeight);

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);

        if (this.minecraft != null && this.minecraft.screen == this) {
            if (menu.getBlockEntity().getSelectedRecipeId() != null) {
            } else {
                guiGraphics.renderItem(new ItemStack(ModItems.TEMPLATE_FOLDER.get()), 8, 126);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isMouseOver(mouseX, mouseY, 152, 18, 16, 61)) {
            long energy = menu.getBlockEntity().getEnergyStored();
            long maxEnergy = menu.getBlockEntity().getMaxEnergyStored();
            guiGraphics.renderTooltip(this.font,
                    Component.literal(EnergyFormatter.format(energy) + " / " + EnergyFormatter.format(maxEnergy) + " HE")
                            .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
        }

        for (int i = 0; i < 3; i++) {
            if (isMouseOver(mouseX, mouseY, 8 + i * 18, 18, 16, 34)) {
                ModFluidTank tank = menu.getBlockEntity().getInputTanks()[i];
                renderTankTooltip(guiGraphics, tank, mouseX, mouseY);
                return;
            }
            if (isMouseOver(mouseX, mouseY, 80 + i * 18, 18, 16, 34)) {
                ModFluidTank tank = menu.getBlockEntity().getOutputTanks()[i];
                renderTankTooltip(guiGraphics, tank, mouseX, mouseY);
                return;
            }
        }

        if (isMouseOver(mouseX, mouseY, 7, 125, 18, 18)) {
            ChemicalPlantRecipe recipe = getSelectedRecipe();
            if (recipe != null) {
                guiGraphics.renderTooltip(this.font, buildRecipeTooltip(recipe), Optional.empty(), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font,
                        Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                        mouseX, mouseY);
            }
        }
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, ModFluidTank tank, int mouseX, int mouseY) {
        Fluid stored = tank.getStoredFluid();
        int amountMb = tank.getFluidAmountMb();
        FluidStack fluid = (stored == net.minecraft.world.level.material.Fluids.EMPTY || amountMb <= 0)
                ? FluidStack.empty()
                : FluidStack.create(stored, (long) amountMb);
        List<Component> tooltip = new ArrayList<>();
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("gui.hbm_m.fluid.empty"));
        } else {
            tooltip.add(dev.architectury.hooks.fluid.FluidStackHooks.getName(fluid));
            tooltip.add(Component.literal(fluid.getAmount() + " / " + TANK_CAPACITY + " mB"));
        }
        guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver((int) mouseX, (int) mouseY, 7, 125, 18, 18)) {
            openRecipeSelector();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openRecipeSelector() {
        if (this.minecraft == null) return;
        ResourceLocation currentRecipe = menu.getBlockEntity().getSelectedRecipeId();
        this.minecraft.setScreen(new GUIScreenRecipeSelector(
                menu.getBlockEntity().getBlockPos(),
                currentRecipe,
                (net.minecraft.client.gui.screens.Screen) this));
    }

    private @NotNull ChemicalPlantRecipe getSelectedRecipe() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        ResourceLocation id = menu.getBlockEntity().getSelectedRecipeId();
        if (id == null) return null;
        return this.minecraft.level.getRecipeManager()
                .byKey(id)
                .filter(r -> r instanceof ChemicalPlantRecipe)
                .map(r -> (ChemicalPlantRecipe) r)
                .orElse(null);
    }

    private List<Component> buildRecipeTooltip(ChemicalPlantRecipe recipe) {
        List<Component> lines = new ArrayList<>();

        ItemStack icon = recipe.getResultItem(this.minecraft != null && this.minecraft.level != null
                ? this.minecraft.level.registryAccess()
                : null);
        if (!icon.isEmpty()) {
            lines.add(icon.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.literal(recipe.getId().toString()).withStyle(ChatFormatting.YELLOW));
        }

        String pool = recipe.getBlueprintPool();
        if (pool != null && !pool.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.hbm_m.recipe_from_group").withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal("  " + pool).withStyle(ChatFormatting.GOLD));
        }

        lines.add(Component.empty());
        lines.add(
                Component.translatable("gui.recipe.duration")
                        .append(": ")
                        .append(Component.literal(String.format(java.util.Locale.ROOT, "%.1fs", recipe.getDuration() / 20.0)))
                        .withStyle(ChatFormatting.RED)
        );
        lines.add(
                Component.translatable("gui.recipe.consumption")
                        .append(": ")
                        .append(Component.literal(recipe.getPowerConsumption() + " HE/t"))
                        .withStyle(ChatFormatting.RED)
        );

        lines.add(Component.empty());
        lines.add(Component.translatable("gui.recipe.input").withStyle(ChatFormatting.BOLD));
        for (var in : recipe.getItemInputs()) {
            ItemStack[] variants = in.ingredient().getItems();
            String name = variants.length == 0 ? "?" : variants[0].getHoverName().getString();
            lines.add(Component.literal("  " + in.count() + "x " + name).withStyle(ChatFormatting.GRAY));
        }
        for (var fin : recipe.getFluidInputs()) {
            lines.add(Component.literal("  " + fin.amount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(Component.literal(fin.fluidId().toString()).withStyle(ChatFormatting.GRAY)));
        }

        lines.add(Component.translatable("gui.recipe.output").withStyle(ChatFormatting.BOLD));
        for (ItemStack out : recipe.getItemOutputs()) {
            if (out.isEmpty()) continue;
            lines.add(Component.literal("  " + out.getCount() + "x ").withStyle(ChatFormatting.GRAY)
                    .append(out.getHoverName()));
        }
        for (dev.architectury.fluid.FluidStack out : recipe.getFluidOutputs()) {
            if (out.isEmpty()) continue;
            lines.add(Component.literal("  " + out.getAmount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(dev.architectury.hooks.fluid.FluidStackHooks.getName(out)));
        }

        return lines;
    }

    private void renderGhostInputs(GuiGraphics guiGraphics) {
        ChemicalPlantRecipe recipe = getSelectedRecipe();
        if (recipe == null) return;

        // Входные слоты 4..6 в menu соответствуют solid inputs
        for (int i = 0; i < recipe.getItemInputs().size() && i < 3; i++) {
            int slotIndex = 4 + i;
            if (slotIndex >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(slotIndex);
            if (slot.hasItem()) continue;

            var in = recipe.getItemInputs().get(i);
            ItemStack[] variants = in.ingredient().getItems();
            if (variants.length == 0) continue;

            ItemStack ghost = variants[0].copy();
            ghost.setCount(in.count());

            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 100);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 0.5f);

            guiGraphics.renderItem(ghost, x, y);
            if (ghost.getCount() > 1) {
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                guiGraphics.renderItemDecorations(this.font, ghost, x, y);
            }

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}
