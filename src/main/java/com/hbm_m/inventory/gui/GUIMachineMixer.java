package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.inventory.menu.MachineMixerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MixerRecipes;
import com.hbm_m.recipe.MixerRecipes.MixerRecipe;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIMixer}: Energie, zwei Eingangstanks, Ausgangstank, Fortschritt, Rezeptumschalter (71,17). */
public class GUIMachineMixer extends GuiInfoScreen<MachineMixerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_mixer.png");

    private final MachineMixerBlockEntity mixer;

    public GUIMachineMixer(MachineMixerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.mixer = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    private static Component fluidName(net.minecraft.world.level.material.Fluid f) {
        return FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(f));
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        this.drawElectricityInfo(g, x, y, 12, 18, 16, 52, menu.getPower(), MachineMixerBlockEntity.maxPower);

        this.drawCustomInfoStat(g, x, y, 152, 55, 8, 8, x, y,
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"),
                Component.translatable("desc.gui.upgrade.overdrive"));

        if (mixer != null) {
            MixerRecipe[] recipes = MixerRecipes.getOutput(mixer.tanks[2].getTankType());

            if (recipes != null && recipes.length > 1) {
                List<Component> label = new ArrayList<>();
                label.add(Component.literal("Current recipe (" + (menu.getRecipeIndex() + 1) + "/" + recipes.length + "):").withStyle(ChatFormatting.YELLOW));
                MixerRecipe recipe = recipes[menu.getRecipeIndex() % recipes.length];
                if (recipe.input1 != null) label.add(Component.literal("-").append(fluidName(recipe.input1.type())));
                if (recipe.input2 != null) label.add(Component.literal("-").append(fluidName(recipe.input2.type())));
                if (recipe.solidInput != null) label.add(Component.literal("-").append(recipe.solidInput.extractForCyclingDisplay(20).getHoverName()));
                label.add(Component.literal("Click to change!").withStyle(ChatFormatting.RED));
                this.drawCustomInfoStat(g, x, y, 71, 17, 12, 12, x, y, label.toArray(new Component[0]));
            }

            mixer.tanks[0].renderTankInfo(g, this.font, x, y, leftPos + 52, topPos + 18, 7, 52);
            mixer.tanks[1].renderTankInfo(g, this.font, x, y, leftPos + 61, topPos + 18, 7, 52);
            mixer.tanks[2].renderTankInfo(g, this.font, x, y, leftPos + 126, topPos + 18, 16, 52);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        if (mixer != null && leftPos + 71 <= x && leftPos + 71 + 12 > x && topPos + 17 < y && topPos + 17 + 12 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            com.hbm_m.network.NBTControlPacket.sendToServer(mixer.getBlockPos(), data);
            return true;
        }
        return super.mouseClicked(x, y, i);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String name = this.title.getString();
        g.drawString(this.font, name, this.imageWidth / 2 + 40 / 2 - this.font.width(name) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int i = (int) (menu.getPower() * 52 / MachineMixerBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 12, topPos + 70 - i, 176, 52 - i, 16, i);

        if (menu.getProcessTime() > 0 && menu.getProgress() > 0) {
            int j = menu.getProgress() * 52 / menu.getProcessTime();
            g.blit(TEXTURE, leftPos + 71, topPos + 31, 192, 0, j, 44);
        }

        if (mixer != null) {
            mixer.tanks[0].renderTank(g, leftPos + 52, topPos + 18, 7, 52);
            mixer.tanks[1].renderTank(g, leftPos + 61, topPos + 18, 7, 52);
            mixer.tanks[2].renderTank(g, leftPos + 126, topPos + 18, 16, 52);
        }

        this.drawInfoPanel(g, 152, 55, PanelType.SMALL_BLUE_STAR);
    }
}
