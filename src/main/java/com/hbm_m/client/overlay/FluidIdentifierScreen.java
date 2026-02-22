package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.custom.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.FluidIdentifierControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * GUI for selecting primary/secondary fluid in the fluid identifier.
 * LMB: set primary. RMB: set secondary.
 */
@OnlyIn(Dist.CLIENT)
public class FluidIdentifierScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_fluid.png");

    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 54;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_COUNT = 9;

    private final Player player;
    private int guiLeft;
    private int guiTop;
    private EditBox searchBox;
    private List<ModFluids.FluidEntry> searchResults = new ArrayList<>();
    private String primaryName = "none";
    private String secondaryName = "none";

    public FluidIdentifierScreen(Player player) {
        super(Component.translatable("gui.hbm_m.fluid_identifier"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() == ModItems.FLUID_IDENTIFIER.get()) {
            primaryName = FluidIdentifierItem.getTypeName(held, true);
            secondaryName = FluidIdentifierItem.getTypeName(held, false);
        }

        searchBox = new EditBox(font, guiLeft + 46, guiTop + 11, 86, 12, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue("");
        searchBox.setResponder(s -> updateSearch());
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        updateSearch();
    }

    @Override
    public void tick() {
        super.tick();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != ModItems.FLUID_IDENTIFIER.get()) {
            onClose();
        }
    }

    private void updateSearch() {
        String query = searchBox != null ? searchBox.getValue() : "";
        searchResults = HbmFluidRegistry.search(query);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, X_SIZE, Y_SIZE);

        if (searchBox != null && searchBox.isFocused()) {
            guiGraphics.blit(TEXTURE, guiLeft + 43, guiTop + 7, 166, 54, 90, 18);
        }

        for (int k = 0; k < SLOT_COUNT && k < searchResults.size(); k++) {
            ModFluids.FluidEntry entry = searchResults.get(k);
            Fluid fluid = entry.getSource();
            String name = HbmFluidRegistry.getFluidName(fluid);
            int color = ModFluids.getTintColor(name);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            guiGraphics.setColor(r / 255F, g / 255F, b / 255F, 1.0F);
            guiGraphics.blit(TEXTURE, guiLeft + 12 + k * SLOT_SIZE, guiTop + 31, 12 + k * SLOT_SIZE, 56, 8, 14);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            int overlayV;
            if (name.equals(primaryName) && name.equals(secondaryName)) {
                overlayV = 36;
            } else if (name.equals(primaryName)) {
                overlayV = 0;
            } else if (name.equals(secondaryName)) {
                overlayV = 18;
            } else {
                overlayV = -1;
            }
            if (overlayV >= 0) {
                guiGraphics.blit(TEXTURE, guiLeft + 7 + k * SLOT_SIZE, guiTop + 29, 176, overlayV, SLOT_SIZE, SLOT_SIZE);
            }
        }

        if (searchBox != null) {
            searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (int k = 0; k < SLOT_COUNT && k < searchResults.size(); k++) {
            int slotX = guiLeft + 7 + k * SLOT_SIZE;
            int slotY = guiTop + 29;
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                ModFluids.FluidEntry entry = searchResults.get(k);
                Fluid fluid = entry.getSource();
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(fluid.getFluidType().getDescriptionId()));
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                break;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (int k = 0; k < SLOT_COUNT && k < searchResults.size(); k++) {
            int slotX = guiLeft + 7 + k * SLOT_SIZE;
            int slotY = guiTop + 29;
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                String name = HbmFluidRegistry.getFluidName(searchResults.get(k).getSource());
                if (minecraft != null && minecraft.getSoundManager() != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                if (button == 0) {
                    primaryName = name;
                    FluidIdentifierControlPacket.send(name, null);
                } else if (button == 1) {
                    secondaryName = name;
                    FluidIdentifierControlPacket.send(null, name);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        if (searchBox != null && searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
