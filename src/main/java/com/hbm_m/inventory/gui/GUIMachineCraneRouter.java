package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneRouterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class GUIMachineCraneRouter extends GuiInfoScreen<MachineCraneRouterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_crane_router.png");

    private final MachineCraneRouterBlockEntity router;

    public GUIMachineCraneRouter(MachineCraneRouterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.router = menu.getBlockEntity();
        this.imageWidth = 256;
        this.imageHeight = 201;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 256, 93);
        guiGraphics.blit(TEXTURE, this.leftPos + 39, this.topPos + 93, 39, 93, 176, 108);

        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                int side = col * 3 + row;
                int mode = router.getMode(side);
                guiGraphics.blit(TEXTURE, this.leftPos + 7 + col * 222, this.topPos + 16 + row * 26, 238, 93 + mode * 18, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 5, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 47, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                int side = col * 3 + row;
                if (isHovering(7 + col * 222, 16 + row * 26, 18, 18, mouseX, mouseY)) {
                    guiGraphics.renderComponentTooltip(this.font, modeTooltip(router.getMode(side)), mouseX, mouseY);
                }
            }
        }

        for (int i = 0; i < MachineCraneRouterBlockEntity.INVENTORY_SIZE; i++) {
            Slot slot = this.menu.slots.get(i);
            int side = i / MachineCraneRouterBlockEntity.SLOTS_PER_SIDE;
            int local = i % MachineCraneRouterBlockEntity.SLOTS_PER_SIDE;
            String mode = router.getMatcher(side).getMode(local);
            if (mode != null && isHoveringSlot(slot, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font,
                        java.util.List.of(Component.literal("§cRight click to change"),
                                Component.literal(ModulePatternMatcher.getLabel(mode))),
                        mouseX, mouseY);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private java.util.List<Component> modeTooltip(int mode) {
        return switch (mode) {
            case 1 -> java.util.List.of(Component.literal("WHITELIST"), Component.literal("Route if filter matches"));
            case 2 -> java.util.List.of(Component.literal("BLACKLIST"), Component.literal("Route if filter doesn't match"));
            case 3 -> java.util.List.of(Component.literal("WILDCARD"), Component.literal("Route if no other route is valid"));
            default -> java.util.List.of(Component.literal("OFF"));
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                int side = col * 3 + row;
                if (isHovering(7 + col * 222, 16 + row * 26, 18, 18, (int) mouseX, (int) mouseY)) {
                    router.nextTargetMode(side);
                    return true;
                }
            }
        }

        for (int i = 0; i < MachineCraneRouterBlockEntity.INVENTORY_SIZE; i++) {
            Slot slot = this.menu.slots.get(i);
            if (isHoveringSlot(slot, (int) mouseX, (int) mouseY) && button == 1 && slot.hasItem()) {
                router.nextFilterMode(i);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringSlot(Slot slot, int mouseX, int mouseY) {
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
