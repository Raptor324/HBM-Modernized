package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.explosion.CustomNukeExplosion;
import com.hbm_m.inventory.menu.NukeCustomMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityNukeCustom}: 27 Slots ohne Automatisierung. Die Stufenwerte werden jeden Tick (beidseitig, die
 * GUI liest sie) aus dem Inhalt berechnet; eine Fallschirmbaugruppe ({@code custom_fall}) macht daraus eine Fallbombe.
 */
public class NukeCustomBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 27;

    public float tnt;
    public float nuke;
    public float hydro;
    public float amat;
    public float dirty;
    public float schrab;
    public float euph;

    public NukeCustomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_CUSTOM_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_custom");
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    /** Original {@code isItemValidForSlot}: nichts per Trichter. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction direction) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction direction) {
        return new int[0];
    }

    /** Das Original zuendet auch leer (es zerstoert sich dann nur selbst). */
    @Override
    public boolean isReady() {
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NukeCustomBlockEntity be) {
        be.update();
    }

    /** {@code updateEntity}. */
    public void update() {
        CustomNukeExplosion.Values v = CustomNukeExplosion.compute(this.slots);
        this.tnt = v.tnt;
        this.nuke = v.nuke;
        this.hydro = v.hydro;
        this.amat = v.amat;
        this.dirty = v.dirty;
        this.schrab = v.schrab;
        this.euph = v.euph;
    }

    public CustomNukeExplosion.Values values() {
        CustomNukeExplosion.Values v = new CustomNukeExplosion.Values();
        v.tnt = tnt;
        v.nuke = nuke;
        v.hydro = hydro;
        v.amat = amat;
        v.dirty = dirty;
        v.schrab = schrab;
        v.euph = euph;
        return v;
    }

    public float getNukeAdj() { return values().getNukeAdj(); }
    public float getHydroAdj() { return values().getHydroAdj(); }
    public float getAmatAdj() { return values().getAmatAdj(); }
    public float getSchrabAdj() { return values().getSchrabAdj(); }

    public boolean isFalling() {

        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && stack.is(ModItems.CUSTOM_FALL.get()))
                return true;
        }

        return false;
    }

    public void clearSlots() {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return AABB.ofSize(worldPosition.getCenter(), 64, 64, 64);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeCustomMenu(id, inventory, this);
    }
}
