package com.hbm_m.creativetabs;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;



/**
 * Порт {@link com.hbm.creativetabs.NukeTab} — иконка и фон креативной вкладки бомб.
 */

public final class NukeTab {
    /** Оригинал: {@code setBackgroundImageName("nuke.png")} → {@code textures/gui/container/creative_inventory/nuke.png}. */
    public static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/tab_nuke.png");
    private NukeTab() {
    }

    public static Item getTabIconItem() {
        if (ModBlocks.NUKE_FAT_MAN.isPresent()) {
            return ModBlocks.NUKE_FAT_MAN.get().asItem();
        }
        return Items.IRON_PICKAXE;
    }

    /** Фон вкладки: Forge 1.20.1 — {@code withBackgroundLocation}; NeoForge 1.21+ — {@code backgroundTexture}. */
    public static void applyBackgroundTexture(CreativeModeTab.Builder builder) {
        //? if forge {
        builder.withBackgroundLocation(BACKGROUND_TEXTURE);
        //?}

        //? if neoforge {
        /*builder.backgroundTexture(BACKGROUND_TEXTURE);
        *///?}

    }

}


