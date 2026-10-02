package com.hbm_m.inventory.gui;

import java.util.List;
import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.menu.MachineAnnihilatorMenu;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.AnnihilatorPoolC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** 1:1 {@code GUIMachineAnnihilator}: Poolname als gruenes Textfeld, Monitor-Tooltip mit dem Zaehlerstand. */
public class GUIMachineAnnihilator extends GuiInfoScreen<MachineAnnihilatorMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_annihilator.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_annihilator.png");
            //?}

    private final MachineAnnihilatorBlockEntity annihilator;
    private EditBox pool;

    public GUIMachineAnnihilator(MachineAnnihilatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.annihilator = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 208;
    }

    @Override
    protected void init() {
        super.init();
        if (annihilator == null) return; // тайл может отсутствовать в реплее Flashback

        this.pool = new EditBox(font, leftPos + 31, topPos + 85, 80, 8, Component.empty());
        this.pool.setTextColor(0x00ff00);
        this.pool.setTextColorUneditable(0x00ff00);
        this.pool.setBordered(false);
        this.pool.setMaxLength(20);
        this.pool.setValue("" + annihilator.pool);
        this.pool.setResponder(text -> AnnihilatorPoolC2SPacket.sendToServer(annihilator.getBlockPos(), text));
        addRenderableWidget(this.pool);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.pool != null && this.pool.isFocused() && keyCode != 256) {
            this.pool.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        ItemStack monitor = annihilator == null ? ItemStack.EMPTY : annihilator.getInventory().getStackInSlot(MachineAnnihilatorBlockEntity.SLOT_MONITOR);
        if (!monitor.isEmpty() && isPointInRect(151, 35, 18, 18, x, y)) {
            Component name = monitor.getHoverName();
            if (monitor.getItem() instanceof FluidIdentifierItem) {
                Fluid type = FluidIdentifierItem.resolvePrimaryForTank(monitor);
                if (type != null) name = FluidType.forFluid(type).getLocalizedName();
            }
            g.renderComponentTooltip(font, List.of(name.copy().append(":"), Component.literal(String.format(Locale.US, "%,d", annihilator.monitorBigInt))), x, y);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component name = this.title;
        g.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
