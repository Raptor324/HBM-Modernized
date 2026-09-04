package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.client.GuiCompat;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * GUI для Chemical Factory — порт 1.7.10 {@code GUIMachineChemicalFactory}.
 *
 * <p>Размер 248×216, текстура {@code textures/gui/processing/gui_chemical_factory.png}
 * (фон рисуется двумя кусками, как в оригинале: 0,0,248,116 и 18,116,230,100).
 * На каждую из 4 линий: полоса прогресса, два LED-индикатора (левый — есть рецепт/работает,
 * правый — хватает энергии и охлаждения), иконка рецепта-кнопка (клик — селектор рецепта),
 * ghost-предметы входов; колонки жидкостных баков по 3 входных + 3 выходных на линию,
 * справа — шкала энергии и баки воды/спент-стима.</p>
 */
public class GUIMachineChemicalFactory extends AbstractContainerScreen<MachineChemicalFactoryMenu> {

    private static final int LANE_COUNT = 4;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_chemical_factory.png");

    public GUIMachineChemicalFactory(MachineChemicalFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 216;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Фон двумя кусками — как drawTexturedModalRect в оригинале
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 248, 116);
        guiGraphics.blit(TEXTURE, this.leftPos + 18, this.topPos + 116, 18, 116, 230, 100);

        // Шкала энергии: заполнение снизу вверх из полосы (0,116)-(16,184)
        long energyStored = menu.getEnergyStored();
        long maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0) {
            int p = (int) (energyStored * 68L / maxEnergy);
            if (p > 68) p = 68;
            if (p > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 224, this.topPos + 86 - p, 0, 184 - p, 16, p);
            }
        }

        for (int lane = 0; lane < LANE_COUNT; lane++) {
            ChemicalPlantRecipe recipe = getLaneRecipe(lane);
            boolean didProcess = menu.getLaneDidProcess(lane);

            // Полоса прогресса (0, 216), максимум 22×6
            int progress = menu.getLaneProgress(lane);
            int maxProgress = menu.getLaneMaxProgress(lane);
            if (progress > 0 && maxProgress > 0) {
                int j = (int) Math.ceil(22.0 * progress / maxProgress);
                if (j > 22) j = 22;
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 29 + lane * 22, 0, 216, j, 6);
            }

            // ЛЕВЫЙ LED: горит при обработке, тусклый если рецепт есть
            if (didProcess) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 21 + lane * 22, 4, 222, 4, 4);
            } else if (recipe != null) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 21 + lane * 22, 0, 222, 4, 4);
            }

            // ПРАВЫЙ LED: горит при обработке, тусклый если есть рецепт + энергия + охлаждение
            if (didProcess) {
                guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 21 + lane * 22, 4, 222, 4, 4);
            } else if (recipe != null && energyStored >= recipe.getPowerConsumption() && menu.getCanCool()) {
                guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 21 + lane * 22, 0, 222, 4, 4);
            }

            // Иконка рецепта-кнопка (75, 20+lane*22); без рецепта — папка шаблонов
            if (this.minecraft != null && this.minecraft.level != null) {
                ItemStack icon = recipe != null ? recipe.getResultItem(this.minecraft.level.registryAccess()) : ItemStack.EMPTY;
                if (icon.isEmpty()) icon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
                guiGraphics.renderItem(icon, this.leftPos + 75, this.topPos + 20 + lane * 22);
            }

            renderGhostInputs(guiGraphics, lane, recipe);
        }

        renderTanks(guiGraphics);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TEXTURE);
    }

    private void renderTanks(GuiGraphics guiGraphics) {
        if (menu.getBlockEntity() == null) return; // тайл может отсутствовать в реплее Flashback

        for (int i = 0; i < 3; i++) {
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                int y = this.topPos + 20 + lane * 22;
                menu.getBlockEntity().getInputTanks()[lane * 3 + i].renderTank(guiGraphics, this.leftPos + 60 + i * 5, y, 3, 16);
                menu.getBlockEntity().getOutputTanks()[lane * 3 + i].renderTank(guiGraphics, this.leftPos + 189 + i * 5, y, 3, 16);
            }
        }

        menu.getBlockEntity().getWaterTank().renderTank(guiGraphics, this.leftPos + 224, this.topPos + 125, 7, 52);
        menu.getBlockEntity().getSpentSteamTank().renderTank(guiGraphics, this.leftPos + 233, this.topPos + 125, 7, 52);
    }

    /**
     * Ghost-предметы входов выбранного рецепта: цикличная смена вариантов тега (1 с, как
     * extractForCyclingDisplay(20) в оригинале) + полупрозрачный квадрат слота из текстуры
     * (аналог zLevel=300-оверлея оригинала).
     */
    private void renderGhostInputs(GuiGraphics guiGraphics, int lane, @Nullable ChemicalPlantRecipe recipe) {
        if (recipe == null) return;

        List<ChemicalPlantRecipe.CountedIngredient> inputs = recipe.getItemInputs();
        for (int i = 0; i < inputs.size() && i < 3; i++) {
            int slotIndex = 5 + lane * 7 + i;
            if (slotIndex >= this.menu.slots.size()) break;
            Slot slot = this.menu.slots.get(slotIndex);
            if (slot.hasItem()) continue;

            var in = inputs.get(i);
            ItemStack[] variants = in.ingredient().getItems();
            if (variants.length == 0) continue;
            int cycleIndex = (int) ((System.currentTimeMillis() / 1000) % variants.length);
            ItemStack ghost = variants[cycleIndex].copy();
            ghost.setCount(in.count());

            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;

            // setShaderColor(alpha) не влияет на 3D-блоки — общая утилита умножает альфу на уровне вершин.
            GhostItemRenderUtil.renderTranslucent(guiGraphics, ghost, x, y, 0.5f);
            if (ghost.getCount() > 1) {
                guiGraphics.renderItemDecorations(this.font, ghost, x, y);
            }

            // Полупрозрачный квадрат пустого слота поверх — текстура хранит слоты в тех же координатах
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
            guiGraphics.blit(TEXTURE, x, y, slot.x, slot.y, 16, 16);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 106 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 26, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (menu.getBlockEntity() == null) return; // тайл может отсутствовать в реплее Flashback

        // Энергия
        if (isMouseOver(mouseX, mouseY, 224, 18, 16, 68)) {
            long energy = menu.getEnergyStored();
            long maxEnergy = menu.getMaxEnergyStored();
            guiGraphics.renderTooltip(this.font,
                    Component.literal(EnergyFormatter.format(energy) + " / " + EnergyFormatter.format(maxEnergy) + " HE")
                            .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
        }

        // Баки линий: 3 входных + 3 выходных на линию
        for (int i = 0; i < 3; i++) {
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                int y = this.topPos + 20 + lane * 22;
                menu.getBlockEntity().getInputTanks()[lane * 3 + i].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 60 + i * 5, y, 3, 16);
                menu.getBlockEntity().getOutputTanks()[lane * 3 + i].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 189 + i * 5, y, 3, 16);
            }
        }
        // Вода / спент-стим
        menu.getBlockEntity().getWaterTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 224, this.topPos + 125, 7, 52);
        menu.getBlockEntity().getSpentSteamTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 233, this.topPos + 125, 7, 52);

        // Кнопка рецепта: тултип рецепта либо подсказка "set recipe"
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (!isMouseOver(mouseX, mouseY, 74, 19 + lane * 22, 18, 18)) continue;
            ChemicalPlantRecipe recipe = getLaneRecipe(lane);
            if (recipe != null) {
                guiGraphics.renderTooltip(this.font, buildRecipeTooltip(recipe), Optional.empty(), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font,
                        Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                        mouseX, mouseY);
            }
            break;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (isMouseOver((int) mouseX, (int) mouseY, 74, 19 + lane * 22, 18, 18)) {
                openRecipeSelector(lane);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openRecipeSelector(int lane) {
        if (this.minecraft == null || menu.getBlockEntity() == null) return; // тайл может отсутствовать в реплее Flashback
        ResourceLocation currentRecipe = menu.getBlockEntity().getSelectedRecipeId(lane);
        this.minecraft.setScreen(new GUIScreenRecipeSelector(
                menu.getBlockEntity().getBlockPos(),
                lane,
                currentRecipe,
                (net.minecraft.client.gui.screens.Screen) this));
    }

    @Nullable
    private ChemicalPlantRecipe getLaneRecipe(int lane) {
        if (this.minecraft == null || this.minecraft.level == null || menu.getBlockEntity() == null) return null;
        ResourceLocation id = menu.getBlockEntity().getSelectedRecipeId(lane);
        if (id == null) return null;
        return RecipeHooks.getRecipeByKey(this.minecraft.level.getRecipeManager(), id)
                .filter(r -> r instanceof ChemicalPlantRecipe)
                .map(r -> (ChemicalPlantRecipe) r)
                .orElse(null);
    }

    private List<Component> buildRecipeTooltip(ChemicalPlantRecipe recipe) {
        List<Component> lines = new ArrayList<>();

        ItemStack icon = this.minecraft != null && this.minecraft.level != null
                ? recipe.getResultItem(this.minecraft.level.registryAccess())
                : ItemStack.EMPTY;
        if (!icon.isEmpty()) {
            lines.add(icon.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        } else {
            // На 1.20.1 id лежит на Recipe (getId()); на 1.21.1 — на RecipeHolder.
            //? if < 1.21.1 {
            lines.add(Component.literal(recipe.getId().toString()).withStyle(ChatFormatting.YELLOW));
            //?} else {
            /*lines.add(Component.literal(
                    RecipeHooks.recipeId(
                            this.minecraft.level.getRecipeManager(),
                            ChemicalPlantRecipe.Type.INSTANCE,
                            recipe).toString()).withStyle(ChatFormatting.YELLOW));
            *///?}
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
            lines.add(Component.literal("  " + fin.getAmount() + "mB ").withStyle(ChatFormatting.BLUE)
                    .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(fin.getFluid())).copy().withStyle(ChatFormatting.GRAY)));
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

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}
