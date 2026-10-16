package com.hbm_m.blockentity.machines;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineAutocrafterMenu;
import com.hbm_m.util.SimpleCraftingContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Autocrafter - Port von {@code TileEntityMachineAutocrafter} (1.7.10 Original). Craftet
 * fortlaufend jedes registrierte VANILLA-3x3-Rezept (shaped/shapeless), sobald die 9 Gitter-Slots
 * passen und genug Energie vorhanden ist - matcht ueber das echte {@link RecipeType#CRAFTING}-
 * Rezeptsystem dieses Ports (kein eigenes Rezeptformat), analog zum Original, das {@code
 * CraftingManager.getRecipeList()} durchsucht.
 * <p>
 * SCOPE-Entscheidung: Das Original trennt einen separaten "Vorlage"-Filter-Raster (Slots 0-8,
 * mit pro-Slot umschaltbarem Filter-Modus exact/wildcard/Ore-Dictionary-Tag/Bedrock-Grade via
 * {@code ModulePatternMatcher}) vom eigentlichen Fertigungs-Raster (Slots 10-18) UND erlaubt
 * Rezept-Durchblaettern per Rechtsklick, wenn mehrere registrierte Rezepte auf dieselbe Vorlage
 * passen. Diese Vorlage-/Filter-/Auswahl-Ebene wird NICHT uebernommen - stattdessen ist das 3x3-
 * Gitter direkt das Fertigungs-Raster (wie ein staendig laufender Crafting-Tisch): das jeweils
 * ERSTE passende Rezept wird sofort gefertigt. Das ist eine deutliche UX-Vereinfachung, deckt aber
 * die eigentliche Kernmechanik ("beliebiges Vanilla-3x3-Rezept automatisch fertigen, mit Strom
 * statt Handarbeit") vollstaendig ab.
 */
public class MachineAutocrafterBlockEntity extends BaseMachineBlockEntity {

    private static final int GRID_START = 0;
    private static final int GRID_SIZE = 9;
    public static final int SLOT_OUTPUT = 9;
    public static final int SLOT_BATTERY = 10;
    private static final int SLOT_COUNT = 11;

    private static final long MAX_POWER = 10_000L;
    private static final long CONSUMPTION = 100L;

    public MachineAutocrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOCRAFTER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAutocrafterBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level);
    }

    private void serverTick(Level level) {
        chargeFromBatterySlot(SLOT_BATTERY);
        if (getEnergyStored() < CONSUMPTION) return;

        SimpleCraftingContainer grid = buildGrid();
        // 1.21.1: getRecipeFor/assemble требуют CraftingInput — хелперы RecipeHooks.
        Optional<CraftingRecipe> recipeOpt = com.hbm_m.platform.recipe.RecipeHooks.getCraftingRecipeFor(level, grid);
        if (recipeOpt.isEmpty()) return;

        CraftingRecipe recipe = recipeOpt.get();
        ItemStack result = com.hbm_m.platform.recipe.RecipeHooks.assembleCrafting(recipe, grid, level);
        if (result.isEmpty()) return;

        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!outSlot.isEmpty()) {
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(outSlot, result)) return;
            if (outSlot.getCount() + result.getCount() > outSlot.getMaxStackSize()) return;
        }

        //? if < 1.21.1 {
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid);
        //?} else {
        /*NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid.toCraftingInput());
        *///?}
        for (int i = 0; i < GRID_SIZE; i++) {
            inventory.getStackInSlot(GRID_START + i).shrink(1);

            ItemStack leftover = remaining.get(i);
            if (!leftover.isEmpty()) {
                ItemStack slotStack = inventory.getStackInSlot(GRID_START + i);
                if (slotStack.isEmpty()) {
                    inventory.setStackInSlot(GRID_START + i, leftover);
                } else if (level != null) {
                    net.minecraft.world.level.block.Block.popResource(level, worldPosition, leftover);
                }
            }
        }

        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            outSlot.grow(result.getCount());
        }

        setEnergyStored(getEnergyStored() - CONSUMPTION);
        setChanged();
        sendUpdateToClient();
    }

    private SimpleCraftingContainer buildGrid() {
        NonNullList<ItemStack> items = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < GRID_SIZE; i++) {
            items.set(i, inventory.getStackInSlot(GRID_START + i));
        }
        return new SimpleCraftingContainer(items, 3, 3);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.autocrafter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_OUTPUT) return false;
        return slot >= GRID_START && slot < GRID_START + GRID_SIZE;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAutocrafterMenu(containerId, playerInventory, this);
    }
}
