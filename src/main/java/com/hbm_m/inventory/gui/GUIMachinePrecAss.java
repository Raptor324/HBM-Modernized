package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachinePrecAssMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PrecAssRecipe;
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
 * 1:1 {@code GUIMachinePrecAss}: 3x3 Ein- und Ausgaenge, liegende Tanks darunter, Energiesaeule, Fortschrittsbalken,
 * zwei LEDs, Rezeptknopf mit Symbol und Rezept-Tooltip, halbtransparente Geister der Rezepteingaenge.
 */
public class GUIMachinePrecAss extends GuiInfoScreen<MachinePrecAssMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_precass.png");

    private final MachinePrecAssBlockEntity assembler;

    public GUIMachinePrecAss(MachinePrecAssMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.assembler = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (assembler != null) {
            assembler.inputTank.renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 8, topPos + 99, 52, 16);
            assembler.outputTank.renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 80, topPos + 99, 52, 16);
            drawElectricityInfo(g, mouseX, mouseY, 152, 18, 16, 61, assembler.getEnergyStored(), assembler.getMaxEnergyStored());

            if (isPointInRect(7, 125, 18, 18, mouseX, mouseY)) {
                PrecAssRecipe recipe = assembler.getSelectedRecipe();
                if (recipe != null) {
                    g.renderTooltip(this.font, buildTooltip(recipe), Optional.empty(), mouseX, mouseY);
                } else {
                    g.renderTooltip(this.font, Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
                }
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (assembler != null && isPointInRect(7, 125, 18, 18, (int) x, (int) y) && this.minecraft != null) {
            this.minecraft.setScreen(new GUIScreenRecipeSelector(assembler.getBlockPos(), assembler.getSelectedRecipeId(), this));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String name = this.title.getString();
        g.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (assembler == null) return;

        long maxPower = Math.max(1, assembler.getMaxEnergyStored());
        int p = (int) (assembler.getEnergyStored() * 61 / maxPower);
        g.blit(TEXTURE, leftPos + 152, topPos + 79 - p, 176, 61 - p, 16, p);

        double progress = assembler.getProgressFraction();
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            g.blit(TEXTURE, leftPos + 62, topPos + 126, 176, 61, j, 16);
        }

        PrecAssRecipe recipe = assembler.getSelectedRecipe();

        /// LEFT LED
        if (assembler.didProcess) g.blit(TEXTURE, leftPos + 51, topPos + 121, 195, 0, 3, 6);
        else if (recipe != null) g.blit(TEXTURE, leftPos + 51, topPos + 121, 192, 0, 3, 6);

        /// RIGHT LED
        if (assembler.didProcess) g.blit(TEXTURE, leftPos + 56, topPos + 121, 195, 0, 3, 6);
        else if (recipe != null && assembler.getEnergyStored() >= recipe.getPower()) g.blit(TEXTURE, leftPos + 56, topPos + 121, 192, 0, 3, 6);

        ItemStack icon = recipe != null ? recipe.getResultItemSafe() : ItemStack.EMPTY;
        if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
        g.renderItem(icon, leftPos + 8, topPos + 126);

        if (recipe != null) {
            for (int i = 0; i < recipe.getItemInputs().size() && i < 9; i++) {
                Slot slot = this.menu.slots.get(4 + i);
                if (slot.hasItem()) continue;
                var in = recipe.getItemInputs().get(i);
                ItemStack[] variants = in.ingredient().getItems();
                if (variants.length == 0) continue;
                ItemStack ghost = variants[(int) ((System.currentTimeMillis() / 1000) % variants.length)].copy();
                ghost.setCount(in.count());
                GhostItemRenderUtil.renderTranslucent(g, ghost, leftPos + slot.x, topPos + slot.y, 0.5f);
                if (ghost.getCount() > 1) g.renderItemDecorations(this.font, ghost, leftPos + slot.x, topPos + slot.y);
            }
        }

        assembler.inputTank.renderTank(g, leftPos + 8, topPos + 99, 52, 16, 1);
        assembler.outputTank.renderTank(g, leftPos + 80, topPos + 99, 52, 16, 1);
    }

    private List<Component> buildTooltip(PrecAssRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        ItemStack icon = recipe.getResultItemSafe();
        if (!icon.isEmpty()) lines.add(icon.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        appendRecipeLines(recipe, lines);
        return lines;
    }

    /** {@code GenericRecipe.print}: Dauer, Verbrauch, Eingaenge und Ausgaben mit ihren Wahrscheinlichkeiten. */
    public static void appendRecipeLines(PrecAssRecipe recipe, List<Component> lines) {
        String pool = recipe.getBlueprintPool();
        if (pool != null && !pool.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.hbm_m.recipe_from_group").withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal("  " + pool).withStyle(ChatFormatting.GOLD));
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.recipe.duration").append(": ")
                .append(Component.literal(String.format(java.util.Locale.ROOT, "%.1fs", recipe.getDuration() / 20.0))).withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("gui.recipe.consumption").append(": ")
                .append(Component.literal(recipe.getPower() + " HE/t")).withStyle(ChatFormatting.RED));

        lines.add(Component.empty());
        lines.add(Component.translatable("gui.recipe.input").withStyle(ChatFormatting.BOLD));
        for (var in : recipe.getItemInputs()) {
            ItemStack[] variants = in.ingredient().getItems();
            Component name = variants.length == 0 ? Component.literal("?") : variants[0].getHoverName();
            lines.add(Component.literal("  " + in.count() + "x ").append(name).withStyle(ChatFormatting.GRAY));
        }
        for (var fin : recipe.getFluidInputs()) {
            lines.add(Component.literal("  " + fin.amount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(FluidLocalization.nameFromFluidId(fin.fluidId()).copy().withStyle(ChatFormatting.GRAY)));
        }

        lines.add(Component.translatable("gui.recipe.output").withStyle(ChatFormatting.BOLD));
        for (PrecAssRecipe.OutputGroup group : recipe.getItemOutputs()) {
            int total = 0;
            for (var e : group.entries()) total += e.weight();
            for (var e : group.entries()) {
                float chance = group.entries().size() == 1 ? group.chance() : (total > 0 ? (float) e.weight() / total : 0F);
                String pct = chance >= 1F ? "" : " (" + Math.round(chance * 1000F) / 10F + "%)";
                lines.add(Component.literal("  " + e.stack().getCount() + "x ").append(e.stack().getHoverName()).append(pct).withStyle(ChatFormatting.GRAY));
            }
        }
        for (var fout : recipe.getFluidOutputs()) {
            lines.add(Component.literal("  " + fout.amount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(FluidLocalization.nameFromFluidId(fout.fluidId()).copy().withStyle(ChatFormatting.GRAY)));
        }
    }
}
