package com.hbm_m.item.block;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.RedBarrelBlock;
import com.hbm_m.block.machines.MachineMassStorageBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

/**
 * 1:1-Port von {@code ItemBlockLore}: feste Zusatzzeilen fuer Fass, Meteoritenbatterie und Diamantkies sowie die
 * Seltenheit (Diamantkies selten, Euphemiumbloecke episch). Hier zentral fuer alle Block-Items, weil der Port
 * dafuer keine eigenen Item-Klassen registriert; haengt auch {@link ItemBlockBlastInfo} und den
 * Massenspeicher-Tooltip ({@code BlockMassStorage.addInformation}) an.
 */
public final class ItemBlockLore {

    private ItemBlockLore() {}

    /** Original {@code getRarity}, nach Registriername (wird beim Anlegen des Block-Items abgefragt). */
    public static Rarity getRarity(String name) {
        if ("gravel_diamond".equals(name)) return Rarity.RARE;
        if ("block_euphemium".equals(name) || "block_euphemium_cluster".equals(name)) return Rarity.EPIC;
        return null;
    }

    public static void appendTooltip(ItemStack stack, List<Component> list) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();

        // Original ItemBlockBase: zuerst die Infos des Blocks selbst
        if (block instanceof MachineMassStorageBlock storage) {
            storage.addInformation(stack, list);
        }

        if (block instanceof RedBarrelBlock) {
            list.add(Component.literal("Static fluid barrel"));
        }

        if (block == ModBlocks.METEOR_BATTERY.get()) {
            list.add(Component.literal("Provides infinite charge to tesla coils"));
        }

        if (block == ModBlocks.GRAVEL_DIAMOND.get()) {
            list.add(Component.literal("There is some kind of joke here,"));
            list.add(Component.literal("but I can't quite tell what it is."));
            list.add(Component.literal(""));
            list.add(Component.literal("Update, 2020-07-04:"));
            list.add(Component.literal("We deny any implications of a joke on"));
            list.add(Component.literal("the basis that it was so severely unfunny"));
            list.add(Component.literal("that people started stabbing their eyes out."));
            list.add(Component.literal(""));
            list.add(Component.literal("Update, 2020-17-04:"));
            list.add(Component.literal("As it turns out, \"Diamond Gravel\" was"));
            list.add(Component.literal("never really a thing, rendering what might"));
            list.add(Component.literal("have been a joke as totally nonsensical."));
            list.add(Component.literal("We apologize for getting your hopes up with"));
            list.add(Component.literal("this non-joke that hasn't been made."));
            list.add(Component.literal(""));
            list.add(Component.literal("i added an item for a joke that isn't even here, what am i, stupid? can't even tell the difference between gravel and a gavel, how did i not forget how to breathe yet?"));
        }

        ItemBlockBlastInfo.appendTooltip(block, list);
    }
}
