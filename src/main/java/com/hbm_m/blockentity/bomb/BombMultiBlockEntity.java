package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.BombMultiMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityBombMulti}: 2x3-Raster, TNT in den Slots 0, 1, 3, 4, Module in 2 und 5 (Schiesspulver, TNT,
 * Streuladung, Feuerpulver, Giftpulver, Gaspellet). Keine Automatisierung.
 */
public class BombMultiBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 6;

    public BombMultiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOMB_MULTI_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.bomb_multi");
    }

    /** Original {@code isItemValidForSlot}: false (nur fuer Trichter). */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    /** Original {@code isLoaded}. */
    @Override
    public boolean isReady() {
        return slots.get(0).is(Items.TNT) && slots.get(1).is(Items.TNT)
                && slots.get(3).is(Items.TNT) && slots.get(4).is(Items.TNT);
    }

    private static Item fire() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "fire_powder"));
    }

    private static int type(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.GUNPOWDER)) return 1;
        if (stack.is(Items.TNT)) return 2;
        if (stack.is(ModItems.PELLET_CLUSTER.get())) return 3;
        if (stack.is(fire())) return 4;
        if (stack.is(ModItems.POWDER_POISON.get())) return 5;
        if (stack.is(ModItems.PELLET_GAS.get())) return 6;
        return 0;
    }

    public int return2type() {
        return type(slots.get(2));
    }

    public int return5type() {
        return type(slots.get(5));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BombMultiMenu(id, inventory, this);
    }
}
