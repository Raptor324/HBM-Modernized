package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineFluidTankMenu;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.FluidTankModePacket;
import com.hbm_m.network.ModPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import dev.architectury.fluid.FluidStack;

public class GUIMachineFluidTank extends AbstractContainerScreen<MachineFluidTankMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/storage/gui_tank.png");
     *///?} else {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/storage/gui_tank.png");
    //?}


    private final int tankX = 71;
    private final int tankY = 17;
    private final int tankWidth = 34;
    private final int tankHeight = 52;
    private final int tankCapacity = 256000;

    private static final int MODE_BUTTON_X = 151;
    private static final int MODE_BUTTON_Y = 34;
    private static final int MODE_BUTTON_SIZE = 18;

    /**
     * Строка спрайта в {@code gui_tank.png} (U=176, каждая высотой MODE_BUTTON_SIZE).
     * В атласе первый и третий спрайты когда‑то совпали с перепутанными подписями режимов;
     * логика BE: 0 — только вывод, 2 — только ввод — поэтому для иконки меняем местами 0↔2.
     */
    private static int modeButtonSpriteRow(int logicalMode) {
        return switch (logicalMode) {
            case 0 -> 2;
            case 2 -> 0;
            default -> logicalMode;
        };
    }

    public GUIMachineFluidTank(MachineFluidTankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + tankX, this.topPos + tankY, tankWidth, tankHeight);
        renderModeButtonTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int mode = menu.getMode();
        int spriteRow = modeButtonSpriteRow(mode);
        guiGraphics.blit(TEXTURE, this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y,
                176, spriteRow * MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE);

        getTank().renderTank(guiGraphics, this.leftPos + tankX, this.topPos + tankY, tankWidth, tankHeight);
    }

    private FluidTank getTank() {
        FluidStack synced = menu.getFluid();
        Fluid type = synced.isEmpty() ? menu.getTankTypeFluid() : synced.getFluid();
        FluidTank dummy = new FluidTank(type, tankCapacity);

        int amount = synced.isEmpty() ? 0 : (int) Math.min(Integer.MAX_VALUE, synced.getAmount());
        if (amount > 0 && type != null && type != Fluids.EMPTY) {
            dummy.setFluid(type, amount);
        }
        dummy.withPressure(menu.getPressure());
        return dummy;
    }

    private void renderModeButtonTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, mouseX, mouseY)) {
            int mode = menu.getMode();
            String key = "gui.hbm_m.fluid_tank.mode." + mode;
            guiGraphics.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, (int) mouseX, (int) mouseY)) {
            ModPacketHandler.sendToServer(ModPacketHandler.FLUID_TANK_MODE,
                    new FluidTankModePacket(menu.blockEntity.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w &&
                mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}