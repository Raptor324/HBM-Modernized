package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.blockentity.machines.MachineSuperComputerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineSuperComputerMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SuperComputerRecipe;
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
 * 1:1 {@code GUIMachineSuperComputer}: drei Eingangstanks links, Ausgangstank rechts, Energiesaeule, Fortschrittsbalken,
 * zwei LEDs, Rezeptknopf mit Icon und Rezept-Tooltip, halbtransparente Geister der Rezepteingaenge.
 */
public class GUIMachineSuperComputer extends GuiInfoScreen<MachineSuperComputerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_supercomputer.png");

    private final MachineSuperComputerBlockEntity computer;

    public GUIMachineSuperComputer(MachineSuperComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.computer = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 211;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (computer != null) {
            computer.inputTanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 8, topPos + 54, 52, 16);
            computer.outputTanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 80, topPos + 54, 52, 16);

            drawElectricityInfo(g, mouseX, mouseY, 152, 18, 16, 61, computer.getEnergyStored(), computer.getMaxEnergyStored());

            if (isPointInRect(7, 80, 18, 18, mouseX, mouseY)) {
                SuperComputerRecipe recipe = computer.getSelectedRecipe();
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
        if (computer != null && isPointInRect(7, 80, 18, 18, (int) x, (int) y) && this.minecraft != null) {
            this.minecraft.setScreen(new GUIScreenRecipeSelector(computer.getBlockPos(), computer.getSelectedRecipeId(), this));
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
        if (computer == null) return; // тайл может отсутствовать в реплее Flashback

        long maxPower = Math.max(1, computer.getMaxEnergyStored());
        int p = (int) (computer.getEnergyStored() * 61 / maxPower);
        g.blit(TEXTURE, leftPos + 152, topPos + 79 - p, 176, 61 - p, 16, p);

        double progress = computer.getProgressFraction();
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            g.blit(TEXTURE, leftPos + 62, topPos + 81, 176, 61, j, 16);
        }

        SuperComputerRecipe recipe = computer.getSelectedRecipe();

        // linke LED
        if (computer.didProcess) {
            g.blit(TEXTURE, leftPos + 51, topPos + 76, 195, 0, 3, 6);
        } else if (recipe != null) {
            g.blit(TEXTURE, leftPos + 51, topPos + 76, 192, 0, 3, 6);
        }

        // rechte LED
        if (computer.didProcess) {
            g.blit(TEXTURE, leftPos + 56, topPos + 76, 195, 0, 3, 6);
        } else if (recipe != null && computer.getEnergyStored() >= recipe.getPowerConsumption()) {
            g.blit(TEXTURE, leftPos + 56, topPos + 76, 192, 0, 3, 6);
        }

        ItemStack icon = ItemStack.EMPTY;
        if (recipe != null && this.minecraft != null && this.minecraft.level != null) icon = recipe.getResultItem(this.minecraft.level.registryAccess());
        if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
        g.renderItem(icon, leftPos + 8, topPos + 81);

        if (recipe != null) {
            for (int i = 0; i < recipe.getItemInputs().size() && i < 3; i++) {
                Slot slot = this.menu.slots.get(2 + i);
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

        computer.inputTanks[0].renderTank(g, leftPos + 8, topPos + 54, 52, 16, 1);
        computer.outputTanks[0].renderTank(g, leftPos + 80, topPos + 54, 52, 16, 1);
    }

    private List<Component> buildRecipeTooltip(SuperComputerRecipe recipe) {
        List<Component> lines = new ArrayList<>();

        ItemStack icon = this.minecraft != null && this.minecraft.level != null ? recipe.getResultItem(this.minecraft.level.registryAccess()) : ItemStack.EMPTY;
        if (!icon.isEmpty()) {
            lines.add(icon.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        } else if (computer.getSelectedRecipeId() != null) {
            lines.add(Component.literal(computer.getSelectedRecipeId().toString()).withStyle(ChatFormatting.YELLOW));
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
        addChanceLines(recipe, lines);
        for (dev.architectury.fluid.FluidStack out : recipe.getFluidOutputs()) {
            if (out.isEmpty()) continue;
            lines.add(Component.literal("  " + out.getAmount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(out.getFluid()))));
        }

        return lines;
    }

    /** Zufallsausgaben mit Wahrscheinlichkeit (Original {@code ChanceOutputMulti} im Rezept-Tooltip). */
    public static void addChanceLines(SuperComputerRecipe recipe, List<Component> lines) {
        for (var group : recipe.getChanceOutputs()) {
            int total = 0;
            for (var w : group) total += Math.max(0, w.weight());
            if (total <= 0) continue;
            for (var w : group) {
                if (w.stack().isEmpty()) continue;
                lines.add(Component.literal("  " + (w.weight() * 100 / total) + "% " + w.stack().getCount() + "x ")
                        .withStyle(ChatFormatting.GRAY).append(w.stack().getHoverName()));
            }
        }
    }

    @Nullable
    public MachineSuperComputerBlockEntity getComputer() { return computer; }
}
