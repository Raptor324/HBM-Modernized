package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachinePUREXMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PurexRecipe;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUIMachinePUREX}: drei Eingangstanks links, Ausgangstank rechts, Energiesaeule, Fortschrittsbalken,
 * zwei LEDs, Rezeptknopf mit Icon und Rezept-Tooltip, halbtransparente Geister der Rezepteingaenge.
 */
public class GUIMachinePUREX extends GuiInfoScreen<MachinePUREXMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_purex.png");

    private final MachinePUREXBlockEntity purex;

    public GUIMachinePUREX(MachinePUREXMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.purex = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (purex != null) {
            for (int i = 0; i < 3; i++) {
                purex.inputTanks[i].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 8 + i * 18, topPos + 18, 16, 52);
            }
            purex.outputTanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 116, topPos + 36, 16, 52);

            drawElectricityInfo(g, mouseX, mouseY, 152, 18, 16, 61, purex.getEnergyStored(), purex.getMaxEnergyStored());

            if (isPointInRect(7, 125, 18, 18, mouseX, mouseY)) {
                PurexRecipe recipe = purex.getSelectedRecipe();
                if (recipe != null) {
                    g.renderTooltip(this.font, buildRecipeTooltip(recipe), Optional.empty(), mouseX, mouseY);
                } else {
                    g.renderTooltip(this.font, Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
                }
            }
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (purex != null && isPointInRect(7, 125, 18, 18, (int) x, (int) y) && this.minecraft != null) {
            this.minecraft.setScreen(new GUIScreenRecipeSelector(purex.getBlockPos(), purex.getSelectedRecipeId(), this));
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
        if (purex == null) return; // тайл может отсутствовать в реплее Flashback

        long maxPower = Math.max(1, purex.getMaxEnergyStored());
        int p = (int) (purex.getEnergyStored() * 61 / maxPower);
        g.blit(TEXTURE, leftPos + 152, topPos + 79 - p, 176, 61 - p, 16, p);

        double progress = purex.getProgressFraction();
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            g.blit(TEXTURE, leftPos + 62, topPos + 126, 176, 61, j, 16);
        }

        PurexRecipe recipe = purex.getSelectedRecipe();

        // linke LED
        if (purex.didProcess) {
            g.blit(TEXTURE, leftPos + 51, topPos + 121, 195, 0, 3, 6);
        } else if (recipe != null) {
            g.blit(TEXTURE, leftPos + 51, topPos + 121, 192, 0, 3, 6);
        }

        // rechte LED
        if (purex.didProcess) {
            g.blit(TEXTURE, leftPos + 56, topPos + 121, 195, 0, 3, 6);
        } else if (recipe != null && purex.getEnergyStored() >= recipe.getPowerConsumption()) {
            g.blit(TEXTURE, leftPos + 56, topPos + 121, 192, 0, 3, 6);
        }

        ItemStack icon = ItemStack.EMPTY;
        if (recipe != null && this.minecraft != null && this.minecraft.level != null) icon = recipe.getResultItem(this.minecraft.level.registryAccess());
        if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
        g.renderItem(icon, leftPos + 8, topPos + 126);

        if (recipe != null) {
            for (int i = 0; i < recipe.getItemInputs().size() && i < 3; i++) {
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

        for (int i = 0; i < 3; i++) {
            purex.inputTanks[i].renderTank(g, leftPos + 8 + i * 18, topPos + 18, 16, 52);
        }
        purex.outputTanks[0].renderTank(g, leftPos + 116, topPos + 36, 16, 52);
    }

    private List<Component> buildRecipeTooltip(PurexRecipe recipe) {
        List<Component> lines = new ArrayList<>();

        ItemStack icon = this.minecraft != null && this.minecraft.level != null ? recipe.getResultItem(this.minecraft.level.registryAccess()) : ItemStack.EMPTY;
        if (!icon.isEmpty()) {
            lines.add(icon.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        } else if (purex.getSelectedRecipeId() != null) {
            lines.add(Component.literal(purex.getSelectedRecipeId().toString()).withStyle(ChatFormatting.YELLOW));
        }

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
                .append(Component.literal(recipe.getPowerConsumption() + " HE/t")).withStyle(ChatFormatting.RED));

        lines.add(Component.empty());
        lines.add(Component.translatable("gui.recipe.input").withStyle(ChatFormatting.BOLD));
        for (var in : recipe.getItemInputs()) {
            ItemStack[] variants = in.ingredient().getItems();
            String name = variants.length == 0 ? "?" : variants[0].getHoverName().getString();
            lines.add(Component.literal("  " + in.count() + "x " + name).withStyle(ChatFormatting.GRAY));
        }
        for (var fin : recipe.getFluidInputs()) {
            lines.add(Component.literal("  " + fin.amount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(FluidLocalization.nameFromFluidId(fin.fluidId()).copy().withStyle(ChatFormatting.GRAY)));
        }

        lines.add(Component.translatable("gui.recipe.output").withStyle(ChatFormatting.BOLD));
        for (ItemStack out : recipe.getItemOutputs()) {
            if (out.isEmpty()) continue;
            lines.add(Component.literal("  " + out.getCount() + "x ").withStyle(ChatFormatting.GRAY).append(out.getHoverName()));
        }
        for (dev.architectury.fluid.FluidStack out : recipe.getFluidOutputs()) {
            if (out.isEmpty()) continue;
            lines.add(Component.literal("  " + out.getAmount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(out.getFluid()))));
        }

        return lines;
    }

    @Nullable
    public MachinePUREXBlockEntity getPurex() { return purex; }
}
