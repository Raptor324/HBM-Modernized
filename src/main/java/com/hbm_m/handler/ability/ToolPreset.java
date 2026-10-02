package com.hbm_m.handler.ability;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 1:1 {@code com.hbm.handler.ability.ToolPreset}. */
public class ToolPreset {
    public IToolAreaAbility areaAbility = IToolAreaAbility.NONE;
    public int areaAbilityLevel = 0;
    public IToolHarvestAbility harvestAbility = IToolHarvestAbility.NONE;
    public int harvestAbilityLevel = 0;

    public ToolPreset() {
    }

    public ToolPreset(IToolAreaAbility areaAbility, IToolHarvestAbility harvestAbility) {
        this.areaAbility = areaAbility;
        this.harvestAbility = harvestAbility;
    }

    public ToolPreset(IToolAreaAbility areaAbility, int areaAbilityLevel, IToolHarvestAbility harvestAbility, int harvestAbilityLevel) {
        this.areaAbility = areaAbility;
        this.areaAbilityLevel = areaAbilityLevel;
        this.harvestAbility = harvestAbility;
        this.harvestAbilityLevel = harvestAbilityLevel;
    }

    public MutableComponent getMessage() {
        if (isNone()) {
            return Component.literal("[Tool ability deactivated]").withStyle(ChatFormatting.GOLD);
        }

        boolean hasArea = areaAbility != IToolAreaAbility.NONE;
        boolean hasHarvest = harvestAbility != IToolHarvestAbility.NONE;

        MutableComponent builder = Component.literal("[Enabled ");

        if (hasArea) {
            builder.append(Component.translatable(areaAbility.getName()));
            builder.append(areaAbility.getExtension(areaAbilityLevel));
        }

        if (hasArea && hasHarvest) {
            builder.append(" + ");
        }

        if (hasHarvest) {
            builder.append(Component.translatable(harvestAbility.getName()));
            builder.append(harvestAbility.getExtension(harvestAbilityLevel));
        }

        return builder.withStyle(ChatFormatting.YELLOW);
    }

    public boolean isNone() {
        return areaAbility == IToolAreaAbility.NONE && harvestAbility == IToolHarvestAbility.NONE;
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("area", areaAbility.getName());
        nbt.putInt("areaLevel", areaAbilityLevel);
        nbt.putString("harvest", harvestAbility.getName());
        nbt.putInt("harvestLevel", harvestAbilityLevel);
    }

    public void readFromNBT(CompoundTag nbt) {
        areaAbility = IToolAreaAbility.getByName(nbt.getString("area"));
        areaAbilityLevel = nbt.getInt("areaLevel");
        harvestAbility = IToolHarvestAbility.getByName(nbt.getString("harvest"));
        harvestAbilityLevel = nbt.getInt("harvestLevel");

        areaAbilityLevel = Math.min(areaAbilityLevel, areaAbility.levels() - 1);
        harvestAbilityLevel = Math.min(harvestAbilityLevel, harvestAbility.levels() - 1);
    }

    public void restrictTo(AvailableAbilities availableAbilities) {
        int maxAreaLevel = availableAbilities.maxLevel(areaAbility);

        if (maxAreaLevel == -1) {
            areaAbility = IToolAreaAbility.NONE;
            areaAbilityLevel = 0;
        } else if (areaAbilityLevel > maxAreaLevel) {
            areaAbilityLevel = maxAreaLevel;
        } else if (areaAbilityLevel < 0) {
            areaAbilityLevel = 0;
        }

        if (!areaAbility.allowsHarvest(areaAbilityLevel)) {
            harvestAbility = IToolHarvestAbility.NONE;
            harvestAbilityLevel = 0;
        }

        int maxHarvestLevel = availableAbilities.maxLevel(harvestAbility);

        if (maxHarvestLevel == -1) {
            harvestAbility = IToolHarvestAbility.NONE;
            harvestAbilityLevel = 0;
        } else if (harvestAbilityLevel > maxHarvestLevel) {
            harvestAbilityLevel = maxHarvestLevel;
        } else if (harvestAbilityLevel < 0) {
            harvestAbilityLevel = 0;
        }
    }
}
