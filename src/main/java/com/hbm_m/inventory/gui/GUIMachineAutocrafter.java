package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineAutocrafterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineAutocrafterMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - Energieanzeige als eigenes Fuellrechteck-Overlay. Die Vorlagen-/Filter-
 * Raster-Anzeige des Originals entfaellt (siehe Klassenkommentar in
 * {@link MachineAutocrafterBlockEntity}).
 * Zusaetzlich (aus FFA): Filtermodus-Hinweis ueber den Vorlageplaetzen ("Right click to change"
 * + Modus) und Rezeptzaehler ueber dem Vorschauplatz.
 */
@OnlyIn(Dist.CLIENT)
public class GUIMachineAutocrafter extends GuiInfoScreen<MachineAutocrafterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_autocrafter.png");

    private final MachineAutocrafterBlockEntity blockEntity;

    public GUIMachineAutocrafter(MachineAutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        // Original: 176x240 - zwei Gitter uebereinander brauchen die Hoehe.
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            long energy = blockEntity.getEnergyStored();
            long maxEnergy = blockEntity.getMaxEnergyStored();
            if (energy > 0 && maxEnergy > 0) {
                int h = (int) (energy * 34L / maxEnergy);
                guiGraphics.fill(x + 17, y + 71 - h, x + 29, y + 71, 0xFF3080FF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (blockEntity != null) {
            if (this.menu.getCarried().isEmpty()) {
                for (int i = 0; i < 9; ++i) {
                    Slot slot = this.menu.slots.get(i);
                    String mode = blockEntity.getMatcher().getMode(i);
                    if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && mode != null) {
                        guiGraphics.renderComponentTooltip(this.font, List.of(
                                Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                                Component.literal(ModulePatternMatcher.getLabel(mode))), mouseX, mouseY - 30);
                    }
                }

                Slot slot = this.menu.slots.get(9);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.hasItem()) {
                    guiGraphics.renderComponentTooltip(this.font, List.of(
                            Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal((blockEntity.getRecipeIndex() + 1) + " / " + blockEntity.getRecipeCount()).withStyle(ChatFormatting.YELLOW)),
                            mouseX, mouseY - 30);
                }
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
