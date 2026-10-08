package com.hbm_m.hazard.transformer;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.item.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code HazardTransformerRadiationContainer}: Behaelter strahlen mit der Summe ihres Inhalts - Kisten
 * (Blockgegenstand mit Inhalt), Werkzeugkiste (volle Summe), Schutzbox ({@code BobMathUtil.squirt} der Summe) und
 * Plastiktuete (doppelte Summe).
 */
public class HazardTransformerRadiationContainer extends HazardTransformerBase {

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {

        boolean isCrate = stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof com.hbm_m.block.machines.crates.BaseCrateBlock;
        boolean isBox = stack.getItem() == ModItems.CONTAINMENT_BOX.get();
        boolean isBag = stack.getItem() == ModItems.PLASTIC_BAG.get();

        boolean isContainer = stack.getItem() == ModItems.TOOLBOX.get(); // For anything using the standard ItemInventory shit.

        if (!isCrate && !isBox && !isBag && !isContainer) return;
        if (!StackNbt.has(stack)) return;

        float radiation = 0;

        if (isContainer) {
            ItemStack[] items = HeldItemInventory.readStacksFromNBT(stack, 24); // Biggest container: toolbox; 24 slots.

            if (items != null)
                for (ItemStack held : items)
                    if (held != null)
                        radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
        }

        if (isCrate) {
            // Kisteninhalt: BlockEntityTag.inventory (ItemStackHandler, Liste "Items")
            CompoundTag beData = com.hbm_m.platform.BlockEntityItemData.read(stack);
            CompoundTag be = beData != null ? beData : new CompoundTag();
            ListTag list = be.getCompound("inventory").getList("Items", 10);

            for (int i = 0; i < list.size(); i++) {
                ItemStack held = StackNbt.parse(list.getCompound(i));

                if (!held.isEmpty()) {
                    radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
                }
            }
        }

        if (isBox) {

            ItemStack[] fromNBT = HeldItemInventory.readStacksFromNBT(stack, 20);
            if (fromNBT == null) return;

            for (ItemStack held : fromNBT) {
                if (held != null) {
                    radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
                }
            }

            radiation = (float) squirt(radiation);
        }

        if (isBag) {

            ItemStack[] fromNBT = HeldItemInventory.readStacksFromNBT(stack, 1);
            if (fromNBT == null) return;

            for (ItemStack held : fromNBT) {
                if (held != null) {
                    radiation += HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION) * held.getCount();
                }
            }

            radiation *= 2F;
        }

        if (radiation > 0) {
            entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
        }
    }

    /** BobMathUtil.squirt */
    private static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }
}
