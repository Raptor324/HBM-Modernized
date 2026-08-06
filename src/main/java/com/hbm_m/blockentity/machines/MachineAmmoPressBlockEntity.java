package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineAmmoPressMenu;
import com.hbm_m.recipe.AmmoPressRecipe;
import com.hbm_m.recipe.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ammo Press - Port von {@code TileEntityMachineAmmoPress} (1.7.10 Original). Anders als die
 * Press/E-Press/Conveyor-Press-Familie (Stempel-Item + Rohmaterial -> Blech) ist dies eine reine
 * 3x3-Raster-Rezept-Maschine ohne Energie/Treibstoff: sobald die 9 Eingangs-Slots exakt einem
 * {@link AmmoPressRecipe} entsprechen, wird sofort (und wiederholt, solange Material reicht)
 * gefertigt - siehe {@link AmmoPressRecipe} fuer die Rezept-Form.
 * <p>
 * SCOPE-Entscheidung: Das Original laesst den Spieler im GUI aus einer durchsuchbaren Liste ALLER
 * bekannten Rezepte eine "Ziel"-Auswahl treffen (rein kosmetisch/Hinweis-Zweck - das eigentliche
 * Craften matcht ohnehin unabhaengig davon, welches Rezept exakt passt); diese Rezeptauswahl-Liste
 * wurde nicht uebernommen, das Craften funktioniert automatisch bei Slot-Uebereinstimmung (wie bei
 * jeder anderen automatischen Rezept-Maschine in diesem Port). Ebenso nicht uebernommen: die
 * animierte Press-Kolben-3D-Bewegung (Original-Renderer) - {@link #animTicks} haelt nur einen
 * kurzen Fortschritts-Flash-Zustand fuers GUI.
 */
public class MachineAmmoPressBlockEntity extends BaseMachineBlockEntity {

    private static final int GRID_SIZE = AmmoPressRecipe.GRID_SIZE;
    public static final int SLOT_OUTPUT = GRID_SIZE;
    private static final int SLOT_COUNT = GRID_SIZE + 1;

    private static final int ANIM_DURATION = 20;

    private int animTicks = 0;

    public MachineAmmoPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMMO_PRESS_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAmmoPressBlockEntity be) {
        if (level.isClientSide()) {
            if (be.animTicks > 0) be.animTicks--;
            return;
        }
        be.serverTick(level);
    }

    private void serverTick(Level level) {
        AmmoPressRecipe recipe = findRecipe(level);
        if (recipe == null) return;

        ItemStack output = recipe.getOutput();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!outSlot.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(outSlot, output)) return;
            if (outSlot.getCount() + output.getCount() > outSlot.getMaxStackSize()) return;
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            inventory.getStackInSlot(i).shrink(1);
        }
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        } else {
            outSlot.grow(output.getCount());
        }

        animTicks = ANIM_DURATION;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 1.4F);

        setChanged();
        sendUpdateToClient();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private AmmoPressRecipe findRecipe(Level level) {
        NonNullList<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        boolean anyItem = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            grid.set(i, stack);
            if (!stack.isEmpty()) anyItem = true;
        }
        if (!anyItem) return null;

        RecipeType type = (RecipeType) AmmoPressRecipe.Type.INSTANCE;
        List<AmmoPressRecipe> recipes = level.getRecipeManager().getAllRecipesFor(type);
        for (AmmoPressRecipe recipe : recipes) {
            if (recipe.matchesGrid(grid)) return recipe;
        }
        return null;
    }

    public int getAnimTicks() {
        return animTicks;
    }

    public boolean isPressing() {
        return animTicks > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("anim_ticks", animTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        animTicks = tag.getInt("anim_ticks");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.ammo_press");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != SLOT_OUTPUT;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAmmoPressMenu(containerId, playerInventory, this);
    }
}
