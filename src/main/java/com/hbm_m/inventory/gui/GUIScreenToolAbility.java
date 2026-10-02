package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.handler.ability.AvailableAbilities;
import com.hbm_m.handler.ability.IBaseAbility;
import com.hbm_m.handler.ability.IToolAreaAbility;
import com.hbm_m.handler.ability.IToolHarvestAbility;
import com.hbm_m.handler.ability.ToolPreset;
import com.hbm_m.item.tool.ItemToolAbility;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.FluidIdentifierControlPacket;
import com.hbm_m.network.InfoToastPacket;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.inventory.gui.GUIScreenToolAbility}: Preset-Editor der Faehigkeitswerkzeuge. */
public class GUIScreenToolAbility extends Screen {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/tool/gui_tool_ability.png");

    protected int guiLeft;
    protected int guiTop;
    protected int xSize;
    protected int ySize;
    protected int insetWidth;

    public static class AbilityInfo {
        public IBaseAbility ability;
        public int textureU, textureV;

        public AbilityInfo(IBaseAbility ability, int textureU, int textureV) {
            this.ability = ability;
            this.textureU = textureU;
            this.textureV = textureV;
        }
    }

    public static final List<AbilityInfo> abilitiesArea = new ArrayList<>();
    public static final List<AbilityInfo> abilitiesHarvest = new ArrayList<>();

    static {
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.NONE, 0, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.RECURSION, 32, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.HAMMER, 64, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.HAMMER_FLAT, 96, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.EXPLOSION, 128, 91));

        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.NONE, 0, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.SILK, 32, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.LUCK, 64, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.SMELTER, 96, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.SHREDDER, 128, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.CENTRIFUGE, 160, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.CRYSTALLIZER, 192, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.MERCURY, 224, 107));
    }

    protected ItemStack toolStack;
    protected AvailableAbilities availableAbilities;
    protected ItemToolAbility.Configuration config;

    protected int hoverIdxHarvest = -1;
    protected int hoverIdxArea = -1;
    protected int hoverIdxExtraBtn = -1;

    public GUIScreenToolAbility(AvailableAbilities availableAbilities) {
        super(Component.empty());

        this.availableAbilities = availableAbilities;

        this.xSize = 186; // Note: increased dynamically
        this.ySize = 76;

        this.insetWidth = 20 * Math.max(abilitiesArea.size() - 4, abilitiesHarvest.size() - 8);
        this.xSize += insetWidth;
    }

    @Override
    protected void init() {
        this.toolStack = this.minecraft.player.getMainHandItem();

        if (this.toolStack.isEmpty() || !(this.toolStack.getItem() instanceof ItemToolAbility)) {
            this.minecraft.setScreen(null);
            return;
        }

        this.config = ((ItemToolAbility) this.toolStack.getItem()).getConfiguration(this.toolStack);

        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        if (config == null) return;
        this.renderBackground(g);

        // Draw window background
        drawStretchedRect(g, guiLeft, guiTop, 0, 0, xSize, xSize - insetWidth, ySize, 74, 87);

        // Draw the switches
        ToolPreset activePreset = config.getActivePreset();
        hoverIdxArea = drawSwitches(g, abilitiesArea, activePreset.areaAbility, activePreset.areaAbilityLevel, guiLeft + 15, guiTop + 25, mouseX, mouseY);
        hoverIdxHarvest = drawSwitches(g, abilitiesHarvest, activePreset.harvestAbility, activePreset.harvestAbilityLevel, guiLeft + 15, guiTop + 45, mouseX, mouseY);

        // Draw preset indicator
        drawNumber(g, config.currentPreset + 1, guiLeft + insetWidth + 115, guiTop + 25);
        drawNumber(g, config.presets.size(), guiLeft + insetWidth + 149, guiTop + 25);

        // Draw extra buttons hover highlights
        int extraBtnsX = guiLeft + xSize - 86;

        hoverIdxExtraBtn = -1;
        for (int i = 0; i < 7; ++i) {
            if (isInAABB(mouseX, mouseY, extraBtnsX + i * 11, guiTop + 11, 9, 9)) {
                hoverIdxExtraBtn = i;
                g.blit(TEXTURE, extraBtnsX + i * 11, guiTop + 11, 193 + i * 9, 0, 9, 9);
            }
        }

        // Draw tooltip
        Component tooltipValue = null;

        if (hoverIdxArea != -1) {
            int level = 0;
            if (abilitiesArea.get(hoverIdxArea).ability == activePreset.areaAbility) {
                level = activePreset.areaAbilityLevel;
            }
            tooltipValue = abilitiesArea.get(hoverIdxArea).ability.getFullName(level);
        } else if (hoverIdxHarvest != -1) {
            int level = 0;
            if (abilitiesHarvest.get(hoverIdxHarvest).ability == activePreset.harvestAbility) {
                level = activePreset.harvestAbilityLevel;
            }
            tooltipValue = abilitiesHarvest.get(hoverIdxHarvest).ability.getFullName(level);
        } else if (hoverIdxExtraBtn != -1) {
            switch (hoverIdxExtraBtn) {
                case 0 -> tooltipValue = Component.literal("Reset all presets");
                case 1 -> tooltipValue = Component.literal("Delete current preset");
                case 2 -> tooltipValue = Component.literal("Add new preset");
                case 3 -> tooltipValue = Component.literal("Select first preset");
                case 4 -> tooltipValue = Component.literal("Next preset");
                case 5 -> tooltipValue = Component.literal("Previous preset");
                case 6 -> tooltipValue = Component.literal("Close window");
            }
        }

        if (tooltipValue != null && !tooltipValue.getString().isEmpty()) {
            int tooltipWidth = Math.max(6, font.width(tooltipValue));
            int tooltipX = guiLeft + xSize / 2 - tooltipWidth / 2;
            int tooltipY = guiTop + ySize + 1 + 4;
            drawStretchedRect(g, tooltipX - 5, tooltipY - 4, 0, 76, tooltipWidth + 10, 186, 15, 3, 3);
            g.drawString(font, tooltipValue, tooltipX, tooltipY, 0xffffffff, false);
        }
    }

    protected void drawStretchedRect(GuiGraphics g, int x, int y, int u, int v, int realWidth, int width, int height, int keepLeft, int keepRight) {
        int midWidth = width - keepLeft - keepRight;
        int realMidWidth = realWidth - keepLeft - keepRight;
        g.blit(TEXTURE, x, y, u, v, keepLeft, height);
        for (int i = 0; i < realMidWidth; i += midWidth) {
            g.blit(TEXTURE, x + keepLeft + i, y, u + keepLeft, v, Math.min(midWidth, realMidWidth - i), height);
        }
        g.blit(TEXTURE, x + keepLeft + realMidWidth, y, u + keepLeft + midWidth, v, keepRight, height);
    }

    protected int drawSwitches(GuiGraphics g, List<AbilityInfo> abilities, IBaseAbility selectedAbility, int selectedLevel, int x, int y, int mouseX, int mouseY) {
        int hoverIdx = -1;

        for (int i = 0; i < abilities.size(); ++i) {
            AbilityInfo abilityInfo = abilities.get(i);
            boolean available = abilityAvailable(abilityInfo.ability);
            boolean selected = abilityInfo.ability == selectedAbility;

            // Draw switch
            g.blit(TEXTURE, x + 20 * i, y, abilityInfo.textureU + (available ? 16 : 0), abilityInfo.textureV, 16, 16);

            // Draw level LEDs
            if (abilityInfo.ability.levels() > 1) {
                int level = 0;

                if (selected) {
                    level = selectedLevel + 1;
                }

                // Note: only visual effect for the LEDs
                int maxLevel = 5;

                if (level > 10 || level < 0) {
                    // All-red LEDs for invalid levels
                    level = -1;
                }

                g.blit(TEXTURE, x + 20 * i + 17, y + 1, 188 + level * 2, maxLevel * 14, 2, 14);
            }

            boolean isHovered = isInAABB(mouseX, mouseY, x + 20 * i, y, 16, 16);

            if (isHovered) {
                hoverIdx = i;
            }

            if (selected) {
                // Draw selection highlight
                g.blit(TEXTURE, x + 20 * i - 1, y - 1, 220, 9, 18, 18);
            } else if (available && isHovered) {
                // Draw hover highlight
                g.blit(TEXTURE, x + 20 * i - 1, y - 1, 238, 9, 18, 18);
            }
        }

        return hoverIdx;
    }

    protected void drawNumber(GuiGraphics g, int number, int x, int y) {
        number += 100; // Against accidental negatives
        drawDigit(g, (number / 10) % 10, x, y);
        drawDigit(g, number % 10, x + 12, y);
    }

    protected void drawDigit(GuiGraphics g, int digit, int x, int y) {
        g.blit(TEXTURE, x, y, digit * 10, 123, 10, 15);
    }

    private boolean isInAABB(double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }

    private boolean abilityAvailable(IBaseAbility ability) {
        if (!availableAbilities.supportsAbility(ability)) {
            return false;
        }

        ToolPreset activePreset = config.getActivePreset();
        return !(ability instanceof IToolHarvestAbility && ability != IToolHarvestAbility.NONE && !activePreset.areaAbility.allowsHarvest(activePreset.areaAbilityLevel));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (config == null) return false;
        if (scroll < 0) doPrevPreset(true);
        if (scroll > 0) doNextPreset(true);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (config == null) return false;
        ToolPreset activePreset = config.getActivePreset();

        // Process switches
        Object[] clickResult;

        clickResult = handleSwitchesClicked(abilitiesArea, activePreset.areaAbility, activePreset.areaAbilityLevel, hoverIdxArea);
        activePreset.areaAbility = (IToolAreaAbility) clickResult[0];
        activePreset.areaAbilityLevel = (Integer) clickResult[1];

        clickResult = handleSwitchesClicked(abilitiesHarvest, activePreset.harvestAbility, activePreset.harvestAbilityLevel, hoverIdxHarvest);
        activePreset.harvestAbility = (IToolHarvestAbility) clickResult[0];
        activePreset.harvestAbilityLevel = (Integer) clickResult[1];

        if (!activePreset.areaAbility.allowsHarvest(activePreset.areaAbilityLevel)) {
            activePreset.harvestAbility = IToolHarvestAbility.NONE;
            activePreset.harvestAbilityLevel = 0;
        }

        // Process extra buttons
        if (hoverIdxExtraBtn != -1) {
            switch (hoverIdxExtraBtn) {
                case 0 -> doResetPresets();
                case 1 -> doDelPreset();
                case 2 -> doAddPreset();
                case 3 -> doZeroPreset();
                case 4 -> doNextPreset(false);
                case 5 -> doPrevPreset(false);
                case 6 -> doClose();
            }

            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
        }

        // Allow quick-closing
        if (!isInAABB(mouseX, mouseY, guiLeft, guiTop, xSize, ySize)) {
            doClose();
        }

        return true;
    }

    protected Object[] handleSwitchesClicked(List<AbilityInfo> abilities, IBaseAbility selectedAbility, int selectedLevel, int hoverIdx) {
        if (hoverIdx != -1) {
            IBaseAbility hoveredAbility = abilities.get(hoverIdx).ability;
            boolean available = abilityAvailable(hoveredAbility);

            if (available) {
                int availableLevels = availableAbilities.maxLevel(hoveredAbility) + 1;

                if (hoveredAbility != selectedAbility || availableLevels > 1) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.TOOL_TECH_BOOP.get(), 2F));
                }

                if (hoveredAbility == selectedAbility) {
                    selectedLevel = (selectedLevel + 1) % availableLevels;
                } else {
                    selectedLevel = 0;
                }

                selectedAbility = hoveredAbility;
            }
        }

        return new Object[] { selectedAbility, selectedLevel };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            doClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void doResetPresets() {
        config.reset(availableAbilities);
    }

    protected void doDelPreset() {
        if (config.presets.size() <= 1) {
            return;
        }
        config.presets.remove(config.currentPreset);
        config.currentPreset = Math.min(config.currentPreset, config.presets.size() - 1);
    }

    protected void doAddPreset() {
        if (config.presets.size() >= 99) {
            return;
        }

        config.presets.add(config.currentPreset + 1, new ToolPreset());
        config.currentPreset += 1;
    }

    protected void doZeroPreset() {
        config.currentPreset = 0;
    }

    protected void doNextPreset(boolean bound) {
        if (bound) {
            if (config.currentPreset < config.presets.size() - 1) {
                config.currentPreset += 1;
            }
        } else {
            config.currentPreset = (config.currentPreset + 1) % config.presets.size();
        }
    }

    protected void doPrevPreset(boolean bound) {
        if (bound) {
            if (config.currentPreset > 0) {
                config.currentPreset -= 1;
            }
        } else {
            config.currentPreset = (config.currentPreset + config.presets.size() - 1) % config.presets.size();
        }
    }

    protected void doClose() {
        // A bit messy, but I suppose it works
        ((ItemToolAbility) this.toolStack.getItem()).setConfiguration(toolStack, config);
        FluidIdentifierControlPacket.sendControl(this.toolStack.getTag());

        this.minecraft.setScreen(null);

        OverlayInfoToast.show(config.getActivePreset().getMessage(), 20, InfoToastPacket.ID_TOOLABILITY, 0xFFFFFF);

        this.minecraft.level.playLocalSound(this.minecraft.player.getX(), this.minecraft.player.getY(), this.minecraft.player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, config.getActivePreset().isNone() ? 0.75F : 1.25F, false);
    }
}
