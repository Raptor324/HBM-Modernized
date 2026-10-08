package com.hbm_m.block.machines;

import com.hbm_m.blockentity.machines.MachineFoundryBaseBlockEntity;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.material.ItemScraps;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;

/** Gemeinsame Handgriffe der Giesserei-Bloecke des Originals (Schaufel leert aus, Abbau gibt den Inhalt als Schrott). */
public final class FoundryBlockUtil {

    private FoundryBlockUtil() { }

    /** Original: {@code ItemTool} mit Werkzeugklasse "shovel". */
    public static boolean isShovel(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ShovelItem || stack.is(ItemTags.SHOVELS));
    }

    /** Original {@code onBlockActivated} mit Schaufel: Inhalt als Schrott an den Spieler, sonst daneben fallen lassen. */
    public static void shovelOut(Level world, BlockPos pos, Player player, MachineFoundryBaseBlockEntity cast, double maxY) {
        if (cast.amount > 0 && cast.type != null) {
            ItemStack scrap = ItemScraps.create(new MaterialStack(cast.type, cast.amount));
            if (!player.getInventory().add(scrap)) {
                world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + maxY, pos.getZ() + 0.5, scrap));
            } else {
                player.inventoryMenu.broadcastChanges();
            }
            cast.amount = 0;
            cast.type = null;
            cast.markForUpdate();
        }
    }

    /** Original {@code breakBlock}: Restmaterial faellt als Schrott heraus. */
    public static void dropContents(Level world, BlockPos pos, MachineFoundryBaseBlockEntity cast, double maxY) {
        if (cast.amount > 0 && cast.type != null) {
            ItemStack scrap = ItemScraps.create(new MaterialStack(cast.type, cast.amount));
            if (!scrap.isEmpty()) world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + maxY, pos.getZ() + 0.5, scrap));
            cast.amount = 0; //just for safety
        }
    }
}
